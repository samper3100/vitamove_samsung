package com.martist.vitamove.workout.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.db.AppDatabase;
import com.martist.vitamove.db.dao.WorkoutDao;
import com.martist.vitamove.db.entity.ExerciseEntity;
import com.martist.vitamove.db.entity.ExerciseSetEntity;
import com.martist.vitamove.db.entity.UserWorkoutEntity;
import com.martist.vitamove.db.entity.WorkoutExerciseEntity;
import com.martist.vitamove.utils.Callback;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseMedia;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.UserWorkout;
import com.martist.vitamove.workout.data.models.WorkoutExercise;
import com.martist.vitamove.workout.data.models.WorkoutPlan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class SupabaseWorkoutRepository implements WorkoutRepository {
    private static final String TAG = "SupabaseWorkoutRepo";
    private final SupabaseClient supabaseClient;
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private final Context context;


    private final Map<String, Long> exercisesLoadedTimestamp = new HashMap<>();


    private final Map<String, Exercise> exerciseCache = new HashMap<>();
    private final Map<String, Long> exerciseCacheTimestamps = new HashMap<>();
    private static final long EXERCISE_CACHE_TIMEOUT_MS = 3600000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseWorkoutRepository(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
        this.context = VitaMoveApplication.getContext();
    }

    @Override
    public String createWorkout(String userId) throws Exception {
        try {
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "createWorkout: userId равен null или пуст");
                throw new IllegalArgumentException("ID пользователя не может быть пустым");
            }




            if (supabaseClient.getUserToken() == null) {
                Log.e(TAG, "createWorkout: Отсутствует токен авторизации");
                throw new Exception("Ошибка авторизации: токен отсутствует");
            }


            String currentTime = ISO_DATE_FORMAT.format(new Date());

            JSONObject workoutJson = new JSONObject()
                    .put("user_id", userId)
                    .put("name", "Тренировка " + LocalDateTime.now().toLocalDate())
                    .put("start_time", JSONObject.NULL)
                    .put("end_time", JSONObject.NULL)
                    .put("total_calories", 0)
                    .put("notes", "")
                    .put("created_at", currentTime)
                    .put("updated_at", currentTime);



            JSONArray result = supabaseClient.from("workouts")
                    .insert(workoutJson)
                    .executeAndGetArray();



            if (result.length() > 0) {
                String workoutId = result.getJSONObject(0).getString("id");

                return workoutId;
            }

            Log.e(TAG, "createWorkout: Пустой результат от сервера");
            throw new Exception("Не удалось создать тренировку: пустой ответ от сервера");
        } catch (SupabaseClient.TokenRefreshedException e) {

            handleTokenRefresh();
            return createWorkout(userId);
        } catch (Exception e) {
            Log.e(TAG, "createWorkout: Ошибка создания тренировки", e);
            throw new Exception("Ошибка создания тренировки: " + e.getMessage());
        }
    }


    @Override
    public String addExerciseToWorkout(String workoutId, String exerciseId, int orderNumber) throws Exception {
        try {

            if (!workoutExists(workoutId)) {
                throw new IllegalArgumentException("Тренировка не найдена");
            }


            String existingId = checkExerciseInWorkout(workoutId, exerciseId);
            if (existingId != null) {
                return existingId;
            }

            String currentTime = ISO_DATE_FORMAT.format(new Date());
            JSONObject exerciseJson = new JSONObject()
                    .put("workout_id", workoutId)
                    .put("exercise_id", exerciseId)
                    .put("order_number", orderNumber)
                    .put("created_at", currentTime);

            JSONArray result = supabaseClient.from("workout_exercises")
                    .insert(exerciseJson)
                    .executeAndGetArray();

            if (result.length() > 0) {
                String newId = result.getJSONObject(0).getString("id");

                return newId;
            }
            throw new Exception("Не удалось добавить упражнение");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка добавления упражнения: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String addSet(String workoutExerciseId, ExerciseSet set) throws Exception {
        try {
            JSONObject setJson = new JSONObject()
                    .put("workout_exercise_id", workoutExerciseId)
                    .put("set_number", set.getSetNumber())
                    .put("weight", set.getWeight())
                    .put("reps", set.getReps())
                    .put("is_completed", set.isCompleted())
                    .put("created_at", ISO_DATE_FORMAT.format(new Date()));


            if (set.getExerciseId() != null && !set.getExerciseId().isEmpty()) {
                setJson.put("exercise_id", set.getExerciseId());

            } else {

            }


            if (set.getDurationSeconds() != null) {
                setJson.put("duration_seconds", set.getDurationSeconds());

            }



            JSONArray result = supabaseClient.from("exercise_sets")
                    .insert(setJson)
                    .executeAndGetArray();

            if (result.length() > 0) {
                String newSetId = result.getJSONObject(0).getString("id");

                return newSetId;
            }
            throw new Exception("Не удалось добавить подход");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка добавления подхода: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void updateSet(String setId, ExerciseSet set) throws Exception {
        try {
            JSONObject setJson = new JSONObject()
                    .put("weight", set.getWeight())
                    .put("reps", set.getReps())
                    .put("is_completed", set.isCompleted());


            if (set.getExerciseId() != null && !set.getExerciseId().isEmpty()) {
                setJson.put("exercise_id", set.getExerciseId());

            }


            if (set.getDurationSeconds() != null) {
                setJson.put("duration_seconds", set.getDurationSeconds());

            }



            supabaseClient.from("exercise_sets")
                    .update(setJson)
                    .eq("id", setId)
                    .executeUpdate();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обновления подхода: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Exercise> getAllExercises() throws Exception {
        try {


            JSONArray exercisesArray = supabaseClient.from("exercises")
                    .select("*")

                    .executeAndGetArray();

            List<Exercise> exercises = new ArrayList<>();

            for (int i = 0; i < exercisesArray.length(); i++) {
                JSONObject exerciseJson = exercisesArray.getJSONObject(i);
                Exercise exercise = parseExerciseFromJson(exerciseJson);

                if (exercise != null) {
                    exercises.add(exercise);
                }
            }


            return exercises;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении упражнений: " + e.getMessage(), e);
            throw e;
        }
    }


    private Exercise parseExerciseFromJson(JSONObject json) {
        try {
            String id = json.getString("id");
            String name = json.getString("name");
            String description = json.optString("description", "");


            String difficulty = json.optString("difficulty", "Легкое");


            String exerciseType = json.optString("exercise_type", "Силовое");


            List<String> categories = new ArrayList<>();
            if (json.has("category") && !json.isNull("category")) {
                String categoryStr = json.optString("category", "");
                if (!categoryStr.isEmpty()) {
                    categories.add(categoryStr);
                }
            }


            if (json.has("categories") && !json.isNull("categories")) {
                try {
                    JSONArray categoriesArray = json.getJSONArray("categories");
                    for (int i = 0; i < categoriesArray.length(); i++) {
                        String category = categoriesArray.getString(i);
                        if (!categories.contains(category)) {
                            categories.add(category);
                        }
                    }
                } catch (Exception e) {

                    String categoriesStr = json.optString("categories", "");
                    if (!categoriesStr.isEmpty()) {

                        if (categoriesStr.startsWith("[") && categoriesStr.endsWith("]")) {
                            try {
                                JSONArray categoriesArray = new JSONArray(categoriesStr);
                                for (int i = 0; i < categoriesArray.length(); i++) {
                                    String category = categoriesArray.getString(i);
                                    if (!categories.contains(category)) {
                                        categories.add(category);
                                    }
                                }
                            } catch (Exception jsonEx) {

                                if (!categories.contains(categoriesStr)) {
                                    categories.add(categoriesStr);
                                }
                            }
                        } else {

                            if (!categories.contains(categoriesStr)) {
                                categories.add(categoriesStr);
                            }
                        }
                    }
                }
            }


            ExerciseMedia media = new ExerciseMedia();
            if (json.has("media") && !json.isNull("media")) {
                JSONObject mediaObj = json.getJSONObject("media");
                String animationUrl = mediaObj.optString("animation_url", null);
                String previewImage = mediaObj.optString("preview_image", null);
                media = new ExerciseMedia(previewImage, animationUrl, null);
            }


            int defaultSets = 3;
            String defaultReps = "12";
            int defaultRestSeconds = 60;


            List<String> equipmentRequired = new ArrayList<>();
            if (json.has("equipment_required") && !json.isNull("equipment_required")) {
                JSONArray equipmentArray = json.getJSONArray("equipment_required");
                for (int i = 0; i < equipmentArray.length(); i++) {
                    String equipmentStr = equipmentArray.getString(i);
                    equipmentRequired.add(equipmentStr);
                }
            }


            List<String> muscleGroups = new ArrayList<>();
            if (json.has("primary_muscles") && !json.isNull("primary_muscles")) {
                JSONArray muscleGroupsArray = json.getJSONArray("primary_muscles");
                for (int i = 0; i < muscleGroupsArray.length(); i++) {
                    String muscleGroupStr = muscleGroupsArray.getString(i);
                    muscleGroups.add(muscleGroupStr);
                }
            }


            List<String> secondaryMuscles = new ArrayList<>();
            if (json.has("secondary_muscles") && !json.isNull("secondary_muscles")) {
                JSONArray secondaryMusclesArray = json.getJSONArray("secondary_muscles");
                for (int i = 0; i < secondaryMusclesArray.length(); i++) {
                    String muscleGroupStr = secondaryMusclesArray.getString(i);
                    secondaryMuscles.add(muscleGroupStr);
                }
            }


            List<String> stabilizerMuscles = new ArrayList<>();
            if (json.has("stabilizer_muscles") && !json.isNull("stabilizer_muscles")) {
                JSONArray stabilizerMusclesArray = json.getJSONArray("stabilizer_muscles");
                for (int i = 0; i < stabilizerMusclesArray.length(); i++) {
                    String muscleGroupStr = stabilizerMusclesArray.getString(i);
                    stabilizerMuscles.add(muscleGroupStr);
                }
            }


            String instructions = json.optString("instructions", "");


            List<String> commonMistakes = new ArrayList<>();
            if (json.has("common_mistakes") && !json.isNull("common_mistakes")) {
                JSONArray mistakesArray = json.getJSONArray("common_mistakes");
                for (int i = 0; i < mistakesArray.length(); i++) {
                    commonMistakes.add(mistakesArray.getString(i));
                }
            }


            String contraindications = json.optString("contraindications", "");


            float met = 0f;
            if (json.has("met") && !json.isNull("met")) {
                met = (float) json.getDouble("met");
            }


            Exercise.Builder builder = new Exercise.Builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .difficulty(difficulty)
                    .exerciseType(exerciseType)
                    .categories(categories)
                    .muscleGroups(muscleGroups)
                    .secondaryMuscles(secondaryMuscles)
                    .stabilizerMuscles(stabilizerMuscles)
                    .equipmentRequired(equipmentRequired)
                    .met(met)
                    .instructions(instructions)
                    .commonMistakes(commonMistakes)
                    .contraindications(contraindications)
                    .media(media);

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при парсинге упражнения: " + e.getMessage(), e);
            return null;
        }
    }



    private boolean workoutExists(String workoutId) throws Exception {
        try {
            JSONArray result = supabaseClient.from("workouts")
                    .select("id")
                    .eq("id", workoutId)
                    .executeAndGetArray();
            return result.length() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования тренировки: " + e.getMessage());
            throw e;
        }
    }

    private String checkExerciseInWorkout(String workoutId, String exerciseId) throws Exception {
        try {
            JSONArray result = supabaseClient.from("workout_exercises")
                    .select("id")
                    .eq("workout_id", workoutId)
                    .eq("exercise_id", exerciseId)
                    .executeAndGetArray();

            if (result.length() > 0) {
                String existingId = result.getJSONObject(0).getString("id");


                return existingId;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования упражнения в тренировке: " + e.getMessage());
            throw e;
        }
    }


    private Exercise parseExercise(JSONObject json) throws Exception {
        String id = json.getString("id");
        String name = json.getString("name");
        String description = json.optString("description", "");


        String difficulty = json.optString("difficulty", "Легкое");


        String exerciseType = json.optString("exercise_type", "Силовое");


        List<String> categories = new ArrayList<>();
        if (json.has("category") && !json.isNull("category")) {
            String categoryStr = json.optString("category", "");
            if (!categoryStr.isEmpty()) {
                categories.add(categoryStr);
            }
        }


        if (json.has("categories") && !json.isNull("categories")) {
            try {
                JSONArray categoriesArray = json.getJSONArray("categories");
                for (int i = 0; i < categoriesArray.length(); i++) {
                    String category = categoriesArray.getString(i);
                    if (!categories.contains(category)) {
                        categories.add(category);
                    }
                }
            } catch (Exception e) {

                String categoriesStr = json.optString("categories", "");
                if (!categoriesStr.isEmpty()) {

                    if (categoriesStr.startsWith("[") && categoriesStr.endsWith("]")) {
                        try {
                            JSONArray categoriesArray = new JSONArray(categoriesStr);
                            for (int i = 0; i < categoriesArray.length(); i++) {
                                String category = categoriesArray.getString(i);
                                if (!categories.contains(category)) {
                                    categories.add(category);
                                }
                            }
                        } catch (Exception jsonEx) {

                            if (!categories.contains(categoriesStr)) {
                                categories.add(categoriesStr);
                            }
                        }
                    } else {

                        if (!categories.contains(categoriesStr)) {
                            categories.add(categoriesStr);
                        }
                    }
                }
            }
        }


        ExerciseMedia media = new ExerciseMedia();
        if (json.has("media") && !json.isNull("media")) {
            JSONObject mediaObj = json.getJSONObject("media");
            String animationUrl = mediaObj.optString("animation_url", null);
            String previewImage = mediaObj.optString("preview_image", null);
            media = new ExerciseMedia(previewImage, animationUrl, null);
        }


        int defaultSets = 3;
        String defaultReps = "12";
        int defaultRestSeconds = 60;


        List<String> equipmentRequired = new ArrayList<>();
        if (json.has("equipment_required") && !json.isNull("equipment_required")) {
            JSONArray equipmentArray = json.getJSONArray("equipment_required");
            for (int i = 0; i < equipmentArray.length(); i++) {
                String equipmentStr = equipmentArray.getString(i);
                equipmentRequired.add(equipmentStr);
            }
        }


        List<String> muscleGroups = new ArrayList<>();
        if (json.has("primary_muscles") && !json.isNull("primary_muscles")) {
            JSONArray muscleGroupsArray = json.getJSONArray("primary_muscles");
            for (int i = 0; i < muscleGroupsArray.length(); i++) {
                String muscleGroupStr = muscleGroupsArray.getString(i);
                muscleGroups.add(muscleGroupStr);
            }
        }


        List<String> secondaryMuscles = new ArrayList<>();
        if (json.has("secondary_muscles") && !json.isNull("secondary_muscles")) {
            JSONArray secondaryMusclesArray = json.getJSONArray("secondary_muscles");
            for (int i = 0; i < secondaryMusclesArray.length(); i++) {
                String muscleGroupStr = secondaryMusclesArray.getString(i);
                secondaryMuscles.add(muscleGroupStr);
            }
        }


        List<String> stabilizerMuscles = new ArrayList<>();
        if (json.has("stabilizer_muscles") && !json.isNull("stabilizer_muscles")) {
            JSONArray stabilizerMusclesArray = json.getJSONArray("stabilizer_muscles");
            for (int i = 0; i < stabilizerMusclesArray.length(); i++) {
                String muscleGroupStr = stabilizerMusclesArray.getString(i);
                stabilizerMuscles.add(muscleGroupStr);
            }
        }


        String instructions = json.optString("instructions", "");


        List<String> commonMistakes = new ArrayList<>();
        if (json.has("common_mistakes") && !json.isNull("common_mistakes")) {
            JSONArray mistakesArray = json.getJSONArray("common_mistakes");
            for (int i = 0; i < mistakesArray.length(); i++) {
                commonMistakes.add(mistakesArray.getString(i));
            }
        }


        String contraindications = json.optString("contraindications", "");


        float met = 0f;
        if (json.has("met") && !json.isNull("met")) {
            met = (float) json.getDouble("met");
        }


        Exercise.Builder builder = new Exercise.Builder();
        return builder.id(id)
                .name(name)
                .description(description)
                .difficulty(difficulty)
                .exerciseType(exerciseType)
                .categories(categories)
                .muscleGroups(muscleGroups)
                .secondaryMuscles(secondaryMuscles)
                .stabilizerMuscles(stabilizerMuscles)
                .equipmentRequired(equipmentRequired)
                .media(media)
                .defaultSets(defaultSets)
                .defaultReps(defaultReps)
                .defaultRestSeconds(defaultRestSeconds)
                .met(met)
                .instructions(instructions)
                .commonMistakes(commonMistakes)
                .contraindications(contraindications)
                .category(categories.isEmpty() ? "" : categories.get(0))
                .build();
    }


    public void deleteWorkout(String workoutId) throws Exception {
        try {
            supabaseClient.from("workouts")
                    .delete()
                    .eq("id", workoutId)
                    .executeDelete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting workout: " + e.getMessage());
            throw e;
        }
    }


    public void deleteUnfinishedWorkouts(String userId) throws Exception {
        try {
            supabaseClient.from("workouts")
                    .delete()
                    .eq("user_id", userId)
                    .is("end_time", "null")
                    .executeDelete();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при удалении незавершенных тренировок: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void updateWorkoutStartTime(String workoutId, long startTime) throws Exception {
        try {
            if (workoutId == null || workoutId.isEmpty()) {
                throw new IllegalArgumentException("ID тренировки не может быть пустым");
            }




            String startTimeFormatted = formatDateTimeForDb(startTime);

            JSONObject updateJson = new JSONObject()
                    .put("start_time", startTimeFormatted);


            supabaseClient.from("workouts")
                    .update(updateJson)
                    .eq("id", workoutId)
                    .executeUpdate();


        } catch (SupabaseClient.TokenRefreshedException e) {

            handleTokenRefresh();
            updateWorkoutStartTime(workoutId, startTime);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении времени начала тренировки: " + e.getMessage(), e);
            throw new Exception("Ошибка обновления времени начала тренировки: " + e.getMessage());
        }
    }

    @Override
    public void deleteSet(String setId) throws Exception {
        try {
            supabaseClient.from("exercise_sets")
                    .delete()
                    .eq("id", setId)
                    .executeDelete();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка удаления подхода: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<WorkoutPlan> getWorkoutPlansByDateRange(String userId, long startDate, long endDate) throws Exception {

        String startDateISO = formatDateTimeForDb(startDate);
        String endDateISO = formatDateTimeForDb(endDate);

        JSONArray jsonArray = supabaseClient.from("workout_plans")
                .select("*")
                .eq("user_id", userId)
                .gte("planned_date", startDateISO)
                .lte("planned_date", endDateISO)
                .executeAndGetArray();
        return parseWorkoutPlans(jsonArray);
    }


    private String formatDateTimeForDb(long timestamp) {
        try {
            Date date = new Date(timestamp);

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String result = isoFormat.format(date);



            return result;
        } catch (Exception e) {
            Log.e(TAG, "!!! VITAMOVE_DEBUG: Ошибка при форматировании даты: " + e.getMessage(), e);

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            return new SimpleDateFormat("yyyy-MM-dd'T'12:00:00.000'Z'", Locale.US).format(cal.getTime());
        }
    }


    public WorkoutPlan getWorkoutPlanById(String id) throws Exception {
        JSONArray jsonArray = supabaseClient.from("workout_plans")
                .select("*")
                .eq("id", id)
                .executeAndGetArray();
        if (jsonArray.length() > 0) {
            return parseWorkoutPlan(jsonArray.getJSONObject(0));
        } else {
            throw new Exception("Workout plan not found");
        }
    }


    private List<WorkoutPlan> parseWorkoutPlans(JSONArray jsonArray) throws JSONException {
        List<WorkoutPlan> workoutPlans = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            workoutPlans.add(parseWorkoutPlan(jsonObject));
        }
        return workoutPlans;
    }



    @Override
    public Exercise getExerciseById(String id) throws Exception {
        JSONArray jsonArray = supabaseClient.from("exercises")
                .select("*")
                .eq("id", id)
                .executeAndGetArray();
        if (jsonArray.length() > 0) {
            return parseExercise(jsonArray.getJSONObject(0));
        }
        return null;
    }


    @Override
    public List<ExerciseSet> getExerciseSetsHistoryById(String exerciseId) throws Exception {
        try {


            JSONArray result = supabaseClient.from("exercise_sets")
                    .select("*")
                    .eq("exercise_id", exerciseId)
                    .order("created_at", true)
                    .executeAndGetArray();

            List<ExerciseSet> sets = new ArrayList<>();

            for (int i = 0; i < result.length(); i++) {
                JSONObject setJson = result.getJSONObject(i);


                if (!setJson.getBoolean("is_completed")) {
                    continue;
                }

                ExerciseSet set = new ExerciseSet();
                set.setId(setJson.getString("id"));
                set.setSetNumber(setJson.getInt("set_number"));


                if (setJson.has("weight") && !setJson.isNull("weight")) {
                    set.setWeight((float) setJson.getDouble("weight"));
                }


                if (setJson.has("reps") && !setJson.isNull("reps")) {
                    set.setReps(setJson.getInt("reps"));
                }

                set.setCompleted(true);
                set.setExerciseId(exerciseId);


                if (setJson.has("workout_exercise_id") && !setJson.isNull("workout_exercise_id")) {
                    set.setWorkoutExerciseId(setJson.getString("workout_exercise_id"));
                }


                if (setJson.has("created_at") && !setJson.isNull("created_at")) {

                    String createdAtStr = setJson.getString("created_at");
                    try {

                        long timestamp = parseIsoDateTime(createdAtStr);
                        set.setCreatedAt(timestamp);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при парсинге даты создания: " + e.getMessage());

                        set.setCreatedAt(System.currentTimeMillis());
                    }
                } else {
                    set.setCreatedAt(System.currentTimeMillis());
                }

                sets.add(set);
            }




            return sets;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении истории подходов: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public UserWorkout getWorkoutById(String workoutId) throws Exception {
        if (workoutId == null || workoutId.isEmpty()) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: ID тренировки равен null или пуст");
            throw new IllegalArgumentException("ID тренировки не может быть пустым");
        }

        try {

            long startRequestTime = System.currentTimeMillis();


            JSONArray workoutArray = supabaseClient
                    .from("workouts")
                    .select("*")
                    .eq("id", workoutId)
                    .executeAndGetArray();

            long endRequestTime = System.currentTimeMillis();
            long requestDuration = endRequestTime - startRequestTime;




            if (workoutArray.length() == 0) {

                return null;
            }

            JSONObject workoutJson = workoutArray.getJSONObject(0);


            long startParseTime = System.currentTimeMillis();


            UserWorkout workout = new UserWorkout(
                    workoutJson.getString("id"),
                    workoutJson.getString("user_id"),
                    workoutJson.optString("name", "Тренировка"),
                    parseIsoDateTime(workoutJson.getString("start_time")),
                    workoutJson.has("end_time") && !workoutJson.isNull("end_time") ?
                            parseIsoDateTime(workoutJson.getString("end_time")) : null,
                    workoutJson.optInt("total_calories", 0),
                    workoutJson.optString("notes", ""),
                    workoutJson.has("program_id") && !workoutJson.isNull("program_id") ?
                            workoutJson.getString("program_id") : null,
                    workoutJson.has("program_day_number") && !workoutJson.isNull("program_day_number") ?
                            workoutJson.getInt("program_day_number") : 0,
                    workoutJson.has("program_day_id") && !workoutJson.isNull("program_day_id") ?
                            workoutJson.getString("program_day_id") : null,
                    workoutJson.has("plan_id") && !workoutJson.isNull("plan_id") ?
                            workoutJson.getString("plan_id") : null,
                    new ArrayList<>());





            loadWorkoutExercises(workout);

            long endParseTime = System.currentTimeMillis();
            long parseDuration = endParseTime - startParseTime;




            return workout;
        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при получении тренировки с СЕРВЕРА по ID: " + workoutId, e);
            throw new Exception("Не удалось загрузить тренировку: " + e.getMessage());
        }
    }


    @Override
    public void updateWorkoutPlan(WorkoutPlan plan) throws Exception {
        JSONObject planJson = new JSONObject();
        planJson.put("name", plan.getName());


        String plannedDateISO = formatDateTimeForDb(plan.getPlannedDate());
        planJson.put("planned_date", plannedDateISO);

        planJson.put("notes", plan.getNotes());


        String updatedAtISO = formatDateTimeForDb(System.currentTimeMillis());
        planJson.put("updated_at", updatedAtISO);


        if (plan.getProgramId() != null) {
            planJson.put("program_id", plan.getProgramId());
        }
        if (plan.getProgramDayId() != null) {
            planJson.put("program_day_id", plan.getProgramDayId());
        }


        if (plan.getStatus() != null) {
            planJson.put("status", plan.getStatus());
        }



        supabaseClient.from("workout_plans")
                .update(planJson)
                .eq("id", plan.getId())
                .executeUpdate();


        if (plan.getProgramId() != null) {
            List<WorkoutPlan> plans = new ArrayList<>();
            plans.add(plan);
            com.martist.vitamove.workout.data.cache.ProgramRoomCache.saveWorkoutPlans(plan.getProgramId(), plans);
        }
    }



    private long parseIsoDateTime(String isoDateTimeString) throws Exception {
        if (isoDateTimeString == null || isoDateTimeString.isEmpty()) {
            throw new Exception("Строка даты-времени не может быть пустой");
        }

        try {


            String normalizedDateString = isoDateTimeString;


            if (isoDateTimeString.contains(".") && isoDateTimeString.matches(".*\\.\\d{4,}.*")) {

                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})\\.(\\d{3})(\\d*)([+-]\\d{2}:\\d{2}|Z)"
                );
                java.util.regex.Matcher matcher = pattern.matcher(isoDateTimeString);

                if (matcher.matches()) {




                    normalizedDateString = matcher.group(1) + "." + matcher.group(2) + matcher.group(4);

                }
            }


            String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss"
            };

            Exception lastException = null;
            for (String format : formats) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = dateFormat.parse(normalizedDateString);
                    if (date != null) {
                        return date.getTime();
                    }
                } catch (java.text.ParseException e) {
                    lastException = e;

                }
            }


            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME;
                java.time.OffsetDateTime dateTime = java.time.OffsetDateTime.parse(normalizedDateString, formatter);
                return dateTime.toInstant().toEpochMilli();
            } catch (Exception e) {


            }


            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d+)([+-]\\d{2}:\\d{2}|Z)"
                );
                java.util.regex.Matcher matcher = pattern.matcher(isoDateTimeString);

                if (matcher.matches()) {
                    int year = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2)) - 1;
                    int day = Integer.parseInt(matcher.group(3));
                    int hour = Integer.parseInt(matcher.group(4));
                    int minute = Integer.parseInt(matcher.group(5));
                    int second = Integer.parseInt(matcher.group(6));

                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal.set(year, month, day, hour, minute, second);
                    cal.set(Calendar.MILLISECOND, 0);


                    String millisStr = matcher.group(7);
                    if (millisStr.length() > 3) {
                        millisStr = millisStr.substring(0, 3);
                    }
                    while (millisStr.length() < 3) {
                        millisStr += "0";
                    }
                    cal.set(Calendar.MILLISECOND, Integer.parseInt(millisStr));


                    String timezone = matcher.group(8);
                    if (timezone.equals("Z")) {

                    } else {

                        java.util.regex.Pattern tzPattern = java.util.regex.Pattern.compile("([+-])(\\d{2}):(\\d{2})");
                        java.util.regex.Matcher tzMatcher = tzPattern.matcher(timezone);
                        if (tzMatcher.matches()) {
                            String sign = tzMatcher.group(1);
                            int tzHour = Integer.parseInt(tzMatcher.group(2));
                            int tzMinute = Integer.parseInt(tzMatcher.group(3));
                            int offsetMillis = (tzHour * 60 + tzMinute) * 60 * 1000;
                            if (sign.equals("-")) {
                                offsetMillis = -offsetMillis;
                            }

                            cal.add(Calendar.MILLISECOND, -offsetMillis);
                        }
                    }

                    return cal.getTimeInMillis();
                }
            } catch (Exception e) {

            }


            if (lastException != null) {
                Log.e(TAG, "Все методы парсинга даты не сработали: " + isoDateTimeString, lastException);
                throw new Exception("Не удалось распарсить дату: " + isoDateTimeString);
            } else {
                throw new Exception("Неверный формат даты: " + isoDateTimeString);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при парсинге даты: " + isoDateTimeString, e);
            throw new Exception("Не удалось преобразовать строку даты: " + e.getMessage());
        }
    }


    private WorkoutPlan parseWorkoutPlan(JSONObject json) throws JSONException {


        WorkoutPlan workoutPlan = new WorkoutPlan();

        workoutPlan.setId(json.getString("id"));
        workoutPlan.setUserId(json.getString("user_id"));
        workoutPlan.setName(json.optString("name", "Тренировка"));


        if (json.has("planned_date") && !json.isNull("planned_date")) {
            try {
                String dateStr = json.getString("planned_date");



                Date date = null;
                Exception lastException = null;

                String[] formats = {
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        "yyyy-MM-dd'T'HH:mm:ssXXX",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'"
                };

                for (String formatStr : formats) {
                    try {
                        SimpleDateFormat format = new SimpleDateFormat(formatStr, Locale.US);
                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                        date = format.parse(dateStr);
                        if (date != null) {


                            break;
                        }
                    } catch (Exception e) {
                        lastException = e;
                    }
                }

                if (date != null) {
                    workoutPlan.setPlannedDate(date.getTime());

                } else {
                    Log.e(TAG, "!!! VITAMOVE_DEBUG: [PARSE] Не удалось распознать формат даты: " + dateStr, lastException);
                    workoutPlan.setPlannedDate(System.currentTimeMillis());

                }
            } catch (Exception e) {
                Log.e(TAG, "!!! VITAMOVE_DEBUG: [PARSE] Ошибка парсинга даты тренировки: " + e.getMessage(), e);
                workoutPlan.setPlannedDate(System.currentTimeMillis());
            }
        } else {

            workoutPlan.setPlannedDate(System.currentTimeMillis());
        }


        if (json.has("program_id") && !json.isNull("program_id")) {
            String programId = json.getString("program_id");
            workoutPlan.setProgramId(programId);

        } else {

        }

        if (json.has("program_day_id") && !json.isNull("program_day_id")) {
            String programDayId = json.getString("program_day_id");
            workoutPlan.setProgramDayId(programDayId);

        } else {

        }


        String status = json.optString("status", "planned");
        workoutPlan.setStatus(status);


        String notes = json.optString("notes", "");
        workoutPlan.setNotes(notes);


        workoutPlan.setCompleted("completed".equals(status));
        workoutPlan.setMissed("missed".equals(status));


        if (json.has("created_at") && !json.isNull("created_at")) {
            try {
                String createdAtStr = json.getString("created_at");
                workoutPlan.setCreatedAt(parseIsoDateTime(createdAtStr));

            } catch (Exception e) {
                Log.e(TAG, "!!! VITAMOVE_DEBUG: [PARSE] Ошибка парсинга created_at: " + e.getMessage(), e);
                workoutPlan.setCreatedAt(System.currentTimeMillis());
            }
        } else {
            workoutPlan.setCreatedAt(System.currentTimeMillis());
        }

        if (json.has("updated_at") && !json.isNull("updated_at")) {
            try {
                String updatedAtStr = json.getString("updated_at");
                workoutPlan.setUpdatedAt(parseIsoDateTime(updatedAtStr));

            } catch (Exception e) {
                Log.e(TAG, "!!! VITAMOVE_DEBUG: [PARSE] Ошибка парсинга updated_at: " + e.getMessage(), e);
                workoutPlan.setUpdatedAt(System.currentTimeMillis());
            }
        } else {
            workoutPlan.setUpdatedAt(System.currentTimeMillis());
        }



        return workoutPlan;
    }


    @Override
    public List<ExerciseSet> getExerciseSets(String workoutExerciseId) throws Exception {
        try {
            JSONArray result = supabaseClient.from("exercise_sets")
                    .select("*")
                    .eq("workout_exercise_id", workoutExerciseId)
                    .order("set_number", true)
                    .executeAndGetArray();

            List<ExerciseSet> sets = new ArrayList<>();
            for (int i = 0; i < result.length(); i++) {
                JSONObject setJson = result.getJSONObject(i);
                ExerciseSet set = new ExerciseSet();
                set.setId(setJson.getString("id"));
                set.setSetNumber(setJson.getInt("set_number"));
                set.setWeight(setJson.has("weight") ? (float) setJson.getDouble("weight") : null);
                set.setReps(setJson.has("reps") ? setJson.getInt("reps") : null);
                set.setCompleted(setJson.getBoolean("is_completed"));
                set.setWorkoutExerciseId(workoutExerciseId);


                if (setJson.has("exercise_id") && !setJson.isNull("exercise_id")) {
                    set.setExerciseId(setJson.getString("exercise_id"));

                }

                sets.add(set);
            }
            return sets;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения подходов: " + e.getMessage(), e);
            throw e;
        }
    }


    private void handleTokenRefresh() throws Exception {
        try {

            String newToken = supabaseClient.refreshAccessToken();



            SharedPreferences prefs = VitaMoveApplication.getContext()
                    .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("accessToken", newToken)
                    .putString("refreshToken", supabaseClient.getRefreshToken())
                    .putLong("tokenUpdateTime", System.currentTimeMillis())
                    .apply();


        } catch (SupabaseClient.AuthException e) {
            Log.e(TAG, "handleTokenRefresh: Ошибка аутентификации", e);


            SharedPreferences prefs = VitaMoveApplication.getContext()
                    .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .remove("accessToken")
                    .remove("refreshToken")
                    .remove("userId")
                    .apply();

            throw new Exception("Требуется повторная авторизация. Пожалуйста, войдите снова.", e);
        } catch (IOException e) {
            Log.e(TAG, "handleTokenRefresh: Ошибка обновления токена", e);


            if (e.getMessage() != null &&
                    (e.getMessage().contains("недействителен или истек") ||
                            e.getMessage().contains("token is not available") ||
                            e.getMessage().contains("Failed to refresh token"))) {

                SharedPreferences prefs = VitaMoveApplication.getContext()
                        .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .remove("accessToken")
                        .remove("refreshToken")
                        .remove("userId")
                        .apply();

                throw new Exception("Требуется повторная авторизация. Пожалуйста, войдите снова.", e);
            }

            throw e;
        } catch (Exception e) {
            Log.e(TAG, "handleTokenRefresh: Непредвиденная ошибка", e);
            throw new Exception("Ошибка при обновлении токена: " + e.getMessage(), e);
        }
    }


    @Override
    public String createWorkoutFromPlan(WorkoutPlan plan) throws Exception {


        WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
        String newWorkoutId = null;

        try {

            JSONObject rpcResult = supabaseClient.rpc("create_workout_from_plan_func")
                    .param("p_user_id", plan.getUserId())
                    .param("p_plan_id", plan.getId())
                    .executeAndGetSingle();


            if (rpcResult == null) {
                Log.e(TAG, "RPC вызов create_workout_from_plan_func вернул null объект JSON.");
                throw new Exception("SQL-функция create_workout_from_plan_func вернула неожиданный null.");
            }

            boolean success = rpcResult.optBoolean("success", false);
            newWorkoutId = rpcResult.optString("workout_id", null);


            if (!success || newWorkoutId == null || newWorkoutId.isEmpty() || newWorkoutId.equals("null")) {
                String errorMessage = rpcResult.optString("error", "SQL-функция не вернула действительный ID или сообщила об ошибке.");
                Log.e(TAG, "Ошибка выполнения SQL-функции create_workout_from_plan_func: " + errorMessage + " (Результат RPC: " + rpcResult + ")");
                throw new Exception(errorMessage);
            }





            UserWorkout createdWorkout = getWorkoutById(newWorkoutId);
            if (createdWorkout == null) {
                Log.e(TAG, "Не удалось загрузить детали тренировки ID: " + newWorkoutId + " после создания через RPC.");
                throw new Exception("Не удалось получить детали созданной тренировки.");
            }


            workoutDao.saveFullWorkout(createdWorkout);








            return newWorkoutId;

        } catch (JSONException jsonEx) {
            Log.e(TAG, "Ошибка парсинга JSON ответа от RPC функции create_workout_from_plan_func", jsonEx);
            throw new Exception("Ошибка обработки ответа от SQL-функции: " + jsonEx.getMessage(), jsonEx);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании тренировки из плана через SQL-функцию: " + plan.getId(), e);

            throw new Exception("Не удалось создать тренировку из плана (SQL): " + e.getMessage(), e);
        }
    }






    @Override
    public List<UserWorkout> getRecentWorkouts(String userId, int limit) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("ID пользователя не может быть пустым");
        }

        try {

            long currentTimeMillis = System.currentTimeMillis();
            

            JSONArray workoutsArray = supabaseClient
                    .from("workouts")
                    .select("*")
                    .eq("user_id", userId)
                    .order("start_time", false)
                    .executeAndGetArray();

            List<UserWorkout> workouts = new ArrayList<>();
            for (int i = 0; i < workoutsArray.length(); i++) {
                JSONObject workoutJson = workoutsArray.getJSONObject(i);


                if (!workoutJson.has("start_time") || workoutJson.isNull("start_time") ||
                        !workoutJson.has("end_time") || workoutJson.isNull("end_time")) {
                    continue;
                }

                long startTime = parseIsoDateTime(workoutJson.getString("start_time"));
                long endTime = parseIsoDateTime(workoutJson.getString("end_time"));


                if (endTime <= startTime) {
                    continue;
                }
                

                if (startTime > currentTimeMillis) {

                    continue;
                }

                UserWorkout workout = new UserWorkout(
                        workoutJson.getString("id"),
                        workoutJson.getString("user_id"),
                        workoutJson.optString("name", "Тренировка"),
                        startTime,
                        endTime,
                        workoutJson.optInt("total_calories", 0),
                        workoutJson.optString("notes", ""),
                        workoutJson.has("program_id") && !workoutJson.isNull("program_id") ?
                                workoutJson.getString("program_id") : null,
                        workoutJson.has("program_day_number") && !workoutJson.isNull("program_day_number") ?
                                workoutJson.getInt("program_day_number") : 0,
                        workoutJson.has("program_day_id") && !workoutJson.isNull("program_day_id") ?
                                workoutJson.getString("program_day_id") : null,
                        workoutJson.has("plan_id") && !workoutJson.isNull("plan_id") ?
                                workoutJson.getString("plan_id") : null,
                        new ArrayList<>());
                loadWorkoutExercises(workout);
                workouts.add(workout);
            }


            workouts.sort((w1, w2) -> Long.compare(w2.getStartTime(), w1.getStartTime()));
            



            if (workouts.size() > limit) {
                workouts = workouts.subList(0, limit);
            }


            return workouts;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении последних тренировок", e);
            throw new Exception("Не удалось загрузить последние тренировки: " + e.getMessage());
        }
    }

    @Override
    public WorkoutPlan getTodayWorkoutPlan(String userId) throws Exception {



        if (userId == null || userId.isEmpty()) {


            return null;
        }


        updatePastUncompletedPlans(userId);

        Calendar calendar = Calendar.getInstance();
        long todayStart = getDayStart(calendar.getTimeInMillis());
        long todayEnd = getDayEnd(calendar.getTimeInMillis());

        try {
            JSONArray result = supabaseClient.from("workout_plans")
                    .select("*")
                    .eq("user_id", userId)
                    .eq("status", "planned")
                    .gte("planned_date", formatDateTimeForDb(todayStart))
                    .lte("planned_date", formatDateTimeForDb(todayEnd))
                    .executeAndGetArray();

            if (result.length() > 0) {
                WorkoutPlan plan = parseWorkoutPlan(result.getJSONObject(0));

                return plan;
            } else {

                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении плана тренировки на сегодня: " + e.getMessage());
            throw e;
        }
    }


    private void updatePastUncompletedPlans(String userId) {
        try {

            Calendar calendar = Calendar.getInstance();
            long todayStart = getDayStart(calendar.getTimeInMillis());


            JSONArray pastPlans = supabaseClient.from("workout_plans")
                    .select("*")
                    .eq("user_id", userId)
                    .eq("status", "planned")
                    .lt("planned_date", formatDateTimeForDb(todayStart))
                    .executeAndGetArray();




            for (int i = 0; i < pastPlans.length(); i++) {
                JSONObject planJson = pastPlans.getJSONObject(i);
                String planId = planJson.getString("id");


                JSONObject updateData = new JSONObject();
                updateData.put("status", "skipped");

                supabaseClient.from("workout_plans")
                        .update(updateData)
                        .eq("id", planId)
                        .execute();


            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении статусов прошлых тренировок: " + e.getMessage(), e);

        }
    }


    private long getDayStart(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    private long getDayEnd(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    @Override
    public List<UserWorkout> getWorkoutHistory(String userId, long startDate, long endDate, int page, int pageSize) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("ID пользователя не может быть пустым");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Номер страницы не может быть отрицательным");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть положительным числом");
        }


        final int MAX_RETRIES = 3;
        int retries = 0;
        long retryDelayMs = 1000;
        Exception lastException = null;

        while (retries < MAX_RETRIES) {
            try {

                String startDateStr = formatDateTimeForDb(startDate);
                String endDateStr = formatDateTimeForDb(endDate);






                int offset = page * pageSize;


                JSONArray allWorkouts = supabaseClient.from("workouts")
                        .select("*")
                        .eq("user_id", userId)
                        .gte("start_time", startDateStr)
                        .lte("start_time", endDateStr)
                        .order("start_time", false)
                        .executeAndGetArray();


                JSONArray jsonArray = new JSONArray();
                int endIndex = Math.min(offset + pageSize, allWorkouts.length());

                for (int i = offset; i < endIndex; i++) {
                    try {
                        jsonArray.put(allWorkouts.getJSONObject(i));
                    } catch (JSONException e) {
                        Log.e(TAG, "Ошибка при обработке тренировки " + i, e);
                    }
                }




                List<UserWorkout> workouts = new ArrayList<>();
                List<String> workoutIds = new ArrayList<>();


                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject workoutJson = jsonArray.getJSONObject(i);

                    try {
                        UserWorkout workout = new UserWorkout(
                                workoutJson.getString("id"),
                                workoutJson.getString("user_id"),
                                workoutJson.optString("name", "Тренировка"),
                                parseIsoDateTime(workoutJson.getString("start_time")),
                                workoutJson.has("end_time") && !workoutJson.isNull("end_time") ?
                                        parseIsoDateTime(workoutJson.getString("end_time")) : null,
                                workoutJson.optInt("total_calories", 0),
                                workoutJson.optString("notes", ""),
                                workoutJson.has("program_id") && !workoutJson.isNull("program_id") ?
                                        workoutJson.getString("program_id") : null,
                                workoutJson.has("program_day_number") && !workoutJson.isNull("program_day_number") ?
                                        workoutJson.getInt("program_day_number") : 0,
                                workoutJson.has("program_day_id") && !workoutJson.isNull("program_day_id") ?
                                        workoutJson.getString("program_day_id") : null,
                                workoutJson.has("plan_id") && !workoutJson.isNull("plan_id") ?
                                        workoutJson.getString("plan_id") : null,
                                new ArrayList<>());

                        workouts.add(workout);
                        workoutIds.add(workout.getId());
                    } catch (Exception e) {
                        Log.e(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Ошибка при обработке тренировки " + i, e);

                    }
                }


                if (!workoutIds.isEmpty()) {
                    loadWorkoutExercisesBatch(workouts, workoutIds);
                }

                return workouts;
            } catch (Exception e) {
                lastException = e;
                retries++;

                String errorMessage = e.getMessage();
                Log.e(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Ошибка (попытка " + retries + "/" + MAX_RETRIES + "): " + errorMessage, e);


                boolean isNetworkError = errorMessage != null &&
                        (errorMessage.contains("Connection reset") ||
                                errorMessage.contains("timeout") ||
                                errorMessage.contains("network") ||
                                errorMessage.contains("connection"));

                if (!isNetworkError) {
                    break;
                }


                try {


                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }


        throw lastException != null ?
                lastException :
                new Exception("Не удалось получить историю тренировок");
    }


    private void loadWorkoutExercisesBatch(List<UserWorkout> workouts, List<String> workoutIds) throws Exception {
        try {

            Map<String, String> exerciseToWorkoutMap = new HashMap<>();
            Map<String, WorkoutExercise> exerciseMap = new HashMap<>();
            Map<String, List<WorkoutExercise>> workoutExercisesMap = new HashMap<>();
            List<String> exerciseIds = new ArrayList<>();
            List<String> workoutExerciseIds = new ArrayList<>();


            for (String workoutId : workoutIds) {
                JSONArray workoutExercisesArray = supabaseClient.from("workout_exercises")
                        .select("id, exercise_id, order_number, workout_id")
                        .eq("workout_id", workoutId)
                        .order("order_number", true)
                        .executeAndGetArray();


                for (int i = 0; i < workoutExercisesArray.length(); i++) {
                    JSONObject workoutExerciseJson = workoutExercisesArray.getJSONObject(i);
                    String workoutExerciseId = workoutExerciseJson.getString("id");
                    String exerciseId = workoutExerciseJson.getString("exercise_id");


                    WorkoutExercise workoutExercise = new WorkoutExercise();
                    workoutExercise.setId(workoutExerciseId);
                    workoutExercise.setOrderNumber(workoutExerciseJson.getInt("order_number"));


                    exerciseToWorkoutMap.put(workoutExerciseId, workoutId);
                    exerciseMap.put(workoutExerciseId, workoutExercise);
                    exerciseIds.add(exerciseId);
                    workoutExerciseIds.add(workoutExerciseId);


                    workoutExercisesMap.computeIfAbsent(workoutId, k -> new ArrayList<>())
                            .add(workoutExercise);
                }
            }




            Map<String, Exercise> exercisesById = new HashMap<>();
            for (String exerciseId : new HashSet<>(exerciseIds)) {
                JSONArray exerciseResult = supabaseClient.from("exercises")
                        .select("*")
                        .eq("id", exerciseId)
                        .executeAndGetArray();

                if (exerciseResult.length() > 0) {
                    Exercise exercise = parseExercise(exerciseResult.getJSONObject(0));
                    exercisesById.put(exercise.getId(), exercise);
                }
            }


            for (String workoutExerciseId : exerciseMap.keySet()) {
                WorkoutExercise workoutExercise = exerciseMap.get(workoutExerciseId);
                for (int i = 0; i < workoutExerciseIds.size(); i++) {
                    if (workoutExerciseIds.get(i).equals(workoutExerciseId)) {
                        String exerciseId = exerciseIds.get(i);
                        if (exercisesById.containsKey(exerciseId)) {
                            workoutExercise.setExercise(exercisesById.get(exerciseId));
                        }
                        break;
                    }
                }
            }


            Map<String, List<ExerciseSet>> setsByExerciseId = new HashMap<>();
            for (String workoutExerciseId : workoutExerciseIds) {
                JSONArray setsResult = supabaseClient.from("exercise_sets")
                        .select("*")
                        .eq("workout_exercise_id", workoutExerciseId)
                        .order("set_number", true)
                        .executeAndGetArray();

                List<ExerciseSet> sets = new ArrayList<>();
                for (int j = 0; j < setsResult.length(); j++) {
                    JSONObject setJson = setsResult.getJSONObject(j);
                    ExerciseSet set = new ExerciseSet();
                    set.setId(setJson.getString("id"));
                    set.setSetNumber(setJson.getInt("set_number"));
                    set.setWeight(setJson.has("weight") ? (float) setJson.getDouble("weight") : null);
                    set.setReps(setJson.has("reps") ? setJson.getInt("reps") : null);
                    set.setCompleted(setJson.getBoolean("is_completed"));
                    set.setWorkoutExerciseId(workoutExerciseId);


                    if (setJson.has("exercise_id") && !setJson.isNull("exercise_id")) {
                        set.setExerciseId(setJson.getString("exercise_id"));

                    }

                    sets.add(set);
                }

                setsByExerciseId.put(workoutExerciseId, sets);
            }


            for (String workoutExerciseId : exerciseMap.keySet()) {
                WorkoutExercise workoutExercise = exerciseMap.get(workoutExerciseId);
                List<ExerciseSet> sets = setsByExerciseId.getOrDefault(workoutExerciseId, new ArrayList<>());
                workoutExercise.setSetsCompleted(sets);
            }



            if (workouts != null) {
                for (UserWorkout workout : workouts) {
                    List<WorkoutExercise> exercises = workoutExercisesMap.getOrDefault(workout.getId(), new ArrayList<>());
                    workout.setExercises(exercises);


                }
            } else {



                try {
                    AppDatabase db = AppDatabase.getInstance(context);
                    WorkoutDao dao = db.workoutDao();


                    for (Exercise exercise : exercisesById.values()) {
                        ExerciseEntity exerciseEntity = new ExerciseEntity();
                        exerciseEntity.setId(exercise.getId());
                        exerciseEntity.setName(exercise.getName());
                        exerciseEntity.setDescription(exercise.getDescription());
                        exerciseEntity.setMuscleGroups(exercise.getMuscleGroups());
                        exerciseEntity.setDifficulty(exercise.getDifficulty());
                        exerciseEntity.setEquipmentRequired(exercise.getEquipmentRequired());

                        try {
                            db.exerciseDao().insertExercise(exerciseEntity);
                        } catch (Exception e) {


                        }
                    }


                    for (String workoutId : workoutExercisesMap.keySet()) {
                        List<WorkoutExercise> exercises = workoutExercisesMap.get(workoutId);
                        for (WorkoutExercise exercise : exercises) {
                            WorkoutExerciseEntity entity = new WorkoutExerciseEntity();
                            entity.setId(exercise.getId());
                            entity.setWorkoutId(workoutId);
                            entity.setBaseExerciseId(exercise.getExercise() != null ? exercise.getExercise().getId() : null);
                            entity.setOrderNumber(exercise.getOrderNumber());

                            try {
                                dao.insertWorkoutExercise(entity);
                            } catch (Exception e) {


                            }
                        }
                    }


                    for (String workoutExerciseId : setsByExerciseId.keySet()) {
                        List<ExerciseSet> sets = setsByExerciseId.get(workoutExerciseId);
                        List<ExerciseSetEntity> setEntities = new ArrayList<>();

                        for (ExerciseSet set : sets) {
                            ExerciseSetEntity entity = new ExerciseSetEntity();
                            entity.setId(set.getId());
                            entity.setWorkoutExerciseId(workoutExerciseId);
                            entity.setSetNumber(set.getSetNumber());
                            entity.setWeight(set.getWeight());
                            entity.setReps(set.getReps());
                            entity.setCompleted(set.isCompleted());
                            setEntities.add(entity);
                        }

                        try {
                            dao.insertExerciseSets(setEntities);
                        } catch (Exception e) {


                        }
                    }


                } catch (Exception e) {
                    Log.e(TAG, "loadWorkoutExercisesBatch: Ошибка при сохранении в Room: " + e.getMessage(), e);

                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при пакетной загрузке упражнений: " + e.getMessage(), e);
            throw e;
        }
    }



    @Override
    public void saveCompletedWorkout(UserWorkout workout) throws Exception {
        if (workout == null || workout.getId() == null || workout.getUserId() == null || workout.getEndTime() == null) {
            Log.e(TAG, "saveCompletedWorkout (finish only): Некорректные данные для завершения тренировки.");
            throw new IllegalArgumentException("Workout data is invalid for finishing workout record.");
        }
        String workoutId = workout.getId();
        String userId = workout.getUserId();


        try {

            JSONObject workoutUpdateJson = new JSONObject();
            workoutUpdateJson.put("user_id", userId);
            workoutUpdateJson.put("end_time", formatDateTimeForDb(workout.getEndTime()));
            workoutUpdateJson.put("total_calories", workout.getTotalCalories());
            workoutUpdateJson.put("notes", workout.getNotes());




            String currentUserIdFromToken = "UNKNOWN";
            try {
                String token = supabaseClient.getUserToken();
                if (token != null) {
                    String[] jwtParts = token.split("\\.");
                    if (jwtParts.length > 1) {
                        String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                        JSONObject jwtJson = new JSONObject(payload);
                        currentUserIdFromToken = jwtJson.optString("sub", "UNKNOWN");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка извлечения ID пользователя из токена: " + e.getMessage());
            }




            if (!currentUserIdFromToken.equals(userId)) {
                Log.e(TAG, "saveCompletedWorkout (finish only): КРИТИЧЕСКАЯ ОШИБКА! ID пользователя в тренировке (" + userId +
                        ") не совпадает с ID пользователя в текущем токене (" + currentUserIdFromToken + "). Обновление отменено.");
                throw new Exception("Ошибка безопасности: Попытка обновить тренировку другого пользователя.");
            }


            supabaseClient.from("workouts")
                    .update(workoutUpdateJson)
                    .eq("id", workoutId)
                    .executeUpdate();


        } catch (SupabaseClient.TokenRefreshedException e) {

            handleTokenRefresh();
            saveCompletedWorkout(workout);
        } catch (Exception e) {
            Log.e(TAG, "saveCompletedWorkout (finish only): Ошибка при завершении записи тренировки ID: " + workoutId + " в Supabase", e);

            throw new Exception("Ошибка синхронизации завершения тренировки с сервером: " + e.getMessage(), e);
        }
    }



    @Override
    public void loadWorkoutExercises(UserWorkout workout) throws Exception {
        try {
            String workoutId = workout.getId();


            if (exercisesLoadedTimestamp.containsKey(workoutId)) {
                long lastLoaded = exercisesLoadedTimestamp.get(workoutId);
                long currentTime = System.currentTimeMillis();

                if (currentTime - lastLoaded < 30000 && workout.getExercises() != null && !workout.getExercises().isEmpty()) {


                    return;
                }
            }


            try {
                AppDatabase db = AppDatabase.getInstance(context);
                WorkoutDao workoutDao = db.workoutDao();


                List<WorkoutExerciseEntity> localExercises = workoutDao.getExercisesForWorkout(workoutId);

                if (localExercises != null && !localExercises.isEmpty()) {
                    List<WorkoutExercise> exercises = new ArrayList<>();

                    for (WorkoutExerciseEntity exerciseEntity : localExercises) {
                        String exerciseId = exerciseEntity.getBaseExerciseId();
                        String workoutExerciseId = exerciseEntity.getId();


                        Exercise exercise = null;
                        if (exerciseCache.containsKey(exerciseId)) {
                            exercise = exerciseCache.get(exerciseId);
                        } else {

                            try {
                                com.martist.vitamove.db.entity.ExerciseEntity exerciseEntityObj =
                                        db.exerciseDao().getExerciseById(exerciseId);
                                if (exerciseEntityObj != null) {
                                    exercise = convertEntityToExercise(exerciseEntityObj);
                                }
                            } catch (Exception e) {

                            }
                        }


                        if (exercise == null) {


                            break;
                        }


                        List<ExerciseSetEntity> localSets = workoutDao.getSetsForExercise(workoutExerciseId);
                        List<ExerciseSet> sets = new ArrayList<>();

                        if (localSets != null && !localSets.isEmpty()) {
                            for (ExerciseSetEntity setEntity : localSets) {
                                sets.add(setEntity.toModel());
                            }
                        }


                        WorkoutExercise workoutExercise = exerciseEntity.toModel(exercise, sets);
                        exercises.add(workoutExercise);
                    }

                    if (exercises.size() == localExercises.size()) {

                        workout.setExercises(exercises);
                        exercisesLoadedTimestamp.put(workoutId, System.currentTimeMillis());


                        return;
                    }
                }


            } catch (Exception e) {


            }


            long startLoadTime = System.currentTimeMillis();


            JSONArray workoutExercises = supabaseClient.from("workout_exercises")
                    .select("id, exercise_id, order_number")
                    .eq("workout_id", workout.getId())
                    .order("order_number", true)
                    .executeAndGetArray();



            List<WorkoutExercise> exercises = new ArrayList<>();

            for (int i = 0; i < workoutExercises.length(); i++) {
                JSONObject workoutExerciseJson = workoutExercises.getJSONObject(i);
                String workoutExerciseId = workoutExerciseJson.getString("id");
                String exerciseId = workoutExerciseJson.getString("exercise_id");




                WorkoutExercise workoutExercise = new WorkoutExercise();
                workoutExercise.setId(workoutExerciseId);
                workoutExercise.setOrderNumber(workoutExerciseJson.getInt("order_number"));

                try {
                    long exerciseStartTime = System.currentTimeMillis();


                    Exercise exercise = null;
                    if (exerciseCache.containsKey(exerciseId) &&
                            (System.currentTimeMillis() - exerciseCacheTimestamps.getOrDefault(exerciseId, 0L) < EXERCISE_CACHE_TIMEOUT_MS)) {
                        exercise = exerciseCache.get(exerciseId);

                    } else {

                        JSONArray exerciseResult = supabaseClient.from("exercises")
                                .select("*")
                                .eq("id", exerciseId)
                                .executeAndGetArray();

                        long exerciseFetchTime = System.currentTimeMillis() - exerciseStartTime;

                        if (exerciseResult.length() > 0) {
                            exercise = parseExercise(exerciseResult.getJSONObject(0));

                            exerciseCache.put(exerciseId, exercise);
                            exerciseCacheTimestamps.put(exerciseId, System.currentTimeMillis());
                        }
                    }

                    if (exercise != null) {
                        workoutExercise.setExercise(exercise);

                        long setsStartTime = System.currentTimeMillis();


                        JSONArray setsResult = supabaseClient.from("exercise_sets")
                                .select("*")
                                .eq("workout_exercise_id", workoutExerciseId)
                                .order("set_number", true)
                                .executeAndGetArray();

                        long setsFetchTime = System.currentTimeMillis() - setsStartTime;



                        List<ExerciseSet> sets = new ArrayList<>();
                        for (int j = 0; j < setsResult.length(); j++) {
                            JSONObject setJson = setsResult.getJSONObject(j);
                            ExerciseSet set = new ExerciseSet();
                            set.setId(setJson.getString("id"));
                            set.setSetNumber(setJson.getInt("set_number"));
                            set.setWeight(setJson.has("weight") ? (float) setJson.getDouble("weight") : null);
                            set.setReps(setJson.has("reps") ? setJson.getInt("reps") : null);
                            set.setCompleted(setJson.getBoolean("is_completed"));
                            set.setWorkoutExerciseId(workoutExerciseId);


                            if (setJson.has("exercise_id") && !setJson.isNull("exercise_id")) {
                                set.setExerciseId(setJson.getString("exercise_id"));

                            }

                            sets.add(set);
                        }

                        workoutExercise.getSetsCompleted().addAll(sets);
                        exercises.add(workoutExercise);


                        try {
                            AppDatabase db = AppDatabase.getInstance(context);
                            WorkoutDao dao = db.workoutDao();


                            try {
                                ExerciseEntity exerciseEntity = new ExerciseEntity();
                                exerciseEntity.setId(exercise.getId());
                                exerciseEntity.setName(exercise.getName());
                                exerciseEntity.setDescription(exercise.getDescription());
                                exerciseEntity.setMuscleGroups(exercise.getMuscleGroups());
                                exerciseEntity.setDifficulty(exercise.getDifficulty());
                                exerciseEntity.setEquipmentRequired(exercise.getEquipmentRequired());
                                db.exerciseDao().insertExercise(exerciseEntity);
                            } catch (Exception e) {

                            }


                            WorkoutExerciseEntity workoutExerciseEntity = WorkoutExerciseEntity.fromModel(workoutExercise, workoutId);
                            dao.insertWorkoutExercise(workoutExerciseEntity);


                            List<ExerciseSetEntity> setEntities = new ArrayList<>();
                            for (ExerciseSet set : sets) {
                                setEntities.add(ExerciseSetEntity.fromModel(set, workoutExerciseId));
                            }
                            dao.insertExerciseSets(setEntities);
                        } catch (Exception e) {

                        }

                    } else {
                        Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Упражнение с ID " + exerciseId + " не найдено на СЕРВЕРЕ");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке данных упражнения с СЕРВЕРА, ID: " +
                            exerciseId + ", ошибка: " + e.getMessage(), e);

                    continue;
                }
            }

            workout.setExercises(exercises);


            exercisesLoadedTimestamp.put(workoutId, System.currentTimeMillis());



        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке упражнений: " + e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public void updateWorkoutPlanStatus(String planId, String newStatus) throws Exception {
        if (planId == null || planId.isEmpty()) {
            throw new IllegalArgumentException("Plan ID не может быть пустым");
        }

        try {
            JSONObject updateData = new JSONObject();
            updateData.put("status", newStatus);


            supabaseClient.from("workout_plans")
                    .eq("id", planId)
                    .update(updateData)
                    .executeUpdate();




            com.martist.vitamove.workout.data.cache.ProgramRoomCache.updateWorkoutPlanStatus(planId, newStatus);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении статуса WorkoutPlan ID: " + planId, e);
            throw new Exception("Не удалось обновить статус плана тренировки: " + e.getMessage(), e);
        }
    }


    @Override
    public void updateExerciseOrderNumber(String exerciseId, int orderNumber) throws Exception {
        try {
            if (exerciseId == null || exerciseId.isEmpty()) {
                throw new IllegalArgumentException("ID упражнения не может быть пустым");
            }



            JSONObject updateJson = new JSONObject()
                    .put("order_number", orderNumber);


            supabaseClient.from("workout_exercises")
                    .update(updateJson)
                    .eq("id", exerciseId)
                    .executeUpdate();


        } catch (SupabaseClient.TokenRefreshedException e) {

            handleTokenRefresh();
            updateExerciseOrderNumber(exerciseId, orderNumber);
        } catch (Exception e) {
            Log.e(TAG, "updateExerciseOrderNumber: Ошибка обновления порядкового номера упражнения", e);
            throw new Exception("Ошибка обновления порядкового номера упражнения: " + e.getMessage());
        }
    }



    private Exercise convertEntityToExercise(com.martist.vitamove.db.entity.ExerciseEntity entity) {
        if (entity == null) return null;

        return new Exercise.Builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .muscleGroups(entity.getMuscleGroups())
                .difficulty(entity.getDifficulty())
                .equipmentRequired(entity.getEquipmentRequired())
                .instructions(entity.getInstructions())
                .build();
    }


    public Float getLastWeightForExercise(String exerciseId) {
        try {
            if (exerciseId == null || exerciseId.isEmpty()) {
                Log.e(TAG, "getLastWeightForExercise: exerciseId равен null или пуст");
                return null;
            }


            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();


            Float lastWeight = workoutDao.getLastWeightForExercise(exerciseId);

            if (lastWeight != null && lastWeight > 0) {

                return lastWeight;
            } else {

                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastWeightForExercise: Ошибка при получении последнего веса: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Integer getLastRepsForExercise(String exerciseId) {
        try {
            if (exerciseId == null || exerciseId.isEmpty()) {
                Log.e(TAG, "getLastRepsForExercise: exerciseId равен null или пуст");
                return null;
            }


            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();


            Integer lastReps = workoutDao.getLastRepsForExercise(exerciseId);

            if (lastReps != null && lastReps > 0) {

                return lastReps;
            } else {

                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastRepsForExercise: Ошибка при получении последних повторений: " + e.getMessage(), e);
            return null;
        }
    }


    public void syncUserWorkouts(String userId, Runnable onComplete, Callback<Exception> onError) {
        if (userId == null || userId.isEmpty()) {
            if (onError != null) {
                onError.call(new IllegalArgumentException("ID пользователя не может быть пустым"));
            }
            return;
        }




        final int THREAD_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
        final ExecutorService parallelExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);



        executor.execute(() -> {
            try {

                Calendar calendar = Calendar.getInstance();
                long endDate = calendar.getTimeInMillis();
                calendar.add(Calendar.MONTH, -6);
                long startDate = calendar.getTimeInMillis();

                String formattedStartDate = formatDateTimeForDb(startDate);
                String formattedEndDate = formatDateTimeForDb(endDate);




                JSONArray workoutsArray = supabaseClient.from("workouts")
                        .select("*")
                        .eq("user_id", userId)
                        .gte("start_time", formattedStartDate)
                        .lte("start_time", formattedEndDate)
                        .order("start_time", false)
                        .executeAndGetArray();

                int totalWorkouts = workoutsArray.length();



                AppDatabase db = AppDatabase.getInstance(context);
                WorkoutDao dao = db.workoutDao();


                List<String> workoutIds = new ArrayList<>();
                List<UserWorkoutEntity> workoutEntities = new ArrayList<>();


                for (int i = 0; i < totalWorkouts; i++) {
                    JSONObject workoutJson = workoutsArray.getJSONObject(i);
                    String workoutId = workoutJson.getString("id");
                    workoutIds.add(workoutId);


                    UserWorkoutEntity entity = new UserWorkoutEntity();
                    entity.setId(workoutId);
                    entity.setUserId(userId);
                    entity.setName(workoutJson.getString("name"));
                    entity.setStartTime(parseIsoDateTime(workoutJson.getString("start_time")));


                    if (!workoutJson.isNull("end_time")) {
                        entity.setEndTime(parseIsoDateTime(workoutJson.getString("end_time")));
                    } else {
                        entity.setEndTime(null);
                    }

                    entity.setTotalCalories(workoutJson.optInt("total_calories", 0));
                    entity.setNotes(workoutJson.optString("notes", ""));


                    if (!workoutJson.isNull("program_id")) {
                        entity.setProgramId(workoutJson.getString("program_id"));
                    }

                    entity.setProgramDayNumber(workoutJson.optInt("program_day_number", 0));

                    if (!workoutJson.isNull("program_day_id")) {
                        entity.setProgramDayId(workoutJson.getString("program_day_id"));
                    }

                    if (!workoutJson.isNull("plan_id")) {
                        entity.setPlanId(workoutJson.getString("plan_id"));
                    }

                    workoutEntities.add(entity);
                }


                for (UserWorkoutEntity entity : workoutEntities) {
                    dao.insertWorkout(entity);
                }




                if (workoutIds.isEmpty()) {

                    if (onComplete != null) {
                        onComplete.run();
                    }
                    parallelExecutor.shutdown();
                    return;
                }


                List<String> priorityWorkoutIds = new ArrayList<>();
                int priorityCount = Math.min(3, workoutIds.size());
                for (int i = 0; i < priorityCount; i++) {
                    priorityWorkoutIds.add(workoutIds.get(i));
                }



                for (String workoutId : priorityWorkoutIds) {
                    try {
                        UserWorkout workout = getWorkoutById(workoutId);
                        if (workout != null) {

                            dao.saveFullWorkout(workout);

                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при загрузке приоритетной тренировки " + workoutId + ": " + e.getMessage());
                    }
                }


                workoutIds.removeAll(priorityWorkoutIds);


                final int BATCH_SIZE = Math.max(5, workoutIds.size() / THREAD_POOL_SIZE);
                List<List<String>> workoutBatches = new ArrayList<>();

                for (int i = 0; i < workoutIds.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, workoutIds.size());
                    workoutBatches.add(workoutIds.subList(i, endIndex));
                }


                final java.util.concurrent.atomic.AtomicInteger completedBatches = new java.util.concurrent.atomic.AtomicInteger(0);
                final int totalBatches = workoutBatches.size();




                for (List<String> batch : workoutBatches) {
                    parallelExecutor.submit(() -> {
                        try {

                            loadWorkoutExercisesBatch(null, batch);


                            int completed = completedBatches.incrementAndGet();




                            if (completed == totalBatches) {

                                if (onComplete != null) {
                                    onComplete.run();
                                }
                                parallelExecutor.shutdown();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при загрузке группы тренировок: " + e.getMessage(), e);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при синхронизации тренировок: " + e.getMessage(), e);
                if (onError != null) {
                    onError.call(e);
                }
                parallelExecutor.shutdown();
            }
        });
    }


}