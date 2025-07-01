package com.martist.vitamove.workout.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.utils.DateUtils;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseMedia;
import com.martist.vitamove.workout.data.models.ExerciseType;
import com.martist.vitamove.workout.data.models.ProgramDay;
import com.martist.vitamove.workout.data.models.ProgramExercise;
import com.martist.vitamove.workout.data.models.ProgramType;
import com.martist.vitamove.workout.data.models.WorkoutPlan;
import com.martist.vitamove.workout.data.models.WorkoutProgram;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class SupabaseProgramRepository implements ProgramRepository {
    private static final String TAG = "SupabaseProgramRepo";
    private final SupabaseClient supabaseClient;
    private Context context;
    

    public interface AsyncCallback<T> {

        void onSuccess(T result);
        

        void onFailure(Exception error);
    }
    

    public SupabaseProgramRepository(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
    }
    

    public void setContext(Context context) {
        this.context = context;
    }
    
    @Override
    public List<WorkoutProgram> getAllPrograms() {

        

        String userId = null;
        if (context != null) {
            userId = ((VitaMoveApplication) context.getApplicationContext()).getCurrentUserId();
        }
        

        List<WorkoutProgram> allPrograms = new ArrayList<>();
        

        List<WorkoutProgram> publicPrograms = getPublicPrograms();
        if (publicPrograms != null && !publicPrograms.isEmpty()) {
            allPrograms.addAll(publicPrograms);

        }
        

        if (userId != null && !userId.isEmpty()) {
            try {
                List<WorkoutProgram> userPrograms = getUserPrograms(userId);
                if (userPrograms != null && !userPrograms.isEmpty()) {
                    allPrograms.addAll(userPrograms);

                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении программ пользователя: " + e.getMessage(), e);

            }
        }
        

        return allPrograms;
    }
    

    private List<WorkoutProgram> getPublicPrograms() {

        
        Map<String, WorkoutProgram> uniquePrograms = new HashMap<>();
        
        try {

            JSONArray results = supabaseClient.from("program_templates")
                .eq("is_public", true)
                .executeAndGetArray();
            
            if (results != null) {

                
                for (int i = 0; i < results.length(); i++) {
                    JSONObject programJson = results.getJSONObject(i);
                    WorkoutProgram program = parseTemplateToProgram(programJson);
                    

                    if (program != null) {

                        String key = program.getId();
                        

                        if (!uniquePrograms.containsKey(key)) {
                            uniquePrograms.put(key, program);
                        }
                    }
                }
            }
            

            return new ArrayList<>(uniquePrograms.values());
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении публичных программ: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    

    @Override
    public List<WorkoutProgram> getUserPrograms(String userId) throws Exception {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Невозможно загрузить пользовательские программы: userId пустой");
            return new ArrayList<>();
        }
        

        Map<String, WorkoutProgram> uniquePrograms = new HashMap<>();
        
        try {

            JSONArray authorPrograms = supabaseClient.from("program_templates")
                    .eq("author_id", userId)
                    .select("*")
                    .executeAndGetArray();
            

            if (authorPrograms != null) {

                
                for (int i = 0; i < authorPrograms.length(); i++) {
                    try {
                        JSONObject programJson = authorPrograms.getJSONObject(i);
                        WorkoutProgram program = parseTemplateToProgram(programJson);
                        

                        if (program != null) {

                            String key = program.getId();
                            if (!uniquePrograms.containsKey(key)) {

                                program.setType(ProgramType.USER_CREATED);

                                program.setUserId(userId);
                                uniquePrograms.put(key, program);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке программы: " + e.getMessage(), e);
                    }
                }
            }
            

            JSONArray userPrograms = supabaseClient.from("programs")
                    .eq("user_id", userId)
                    .select("*")
                    .executeAndGetArray();
            

            if (userPrograms != null) {

                
                for (int i = 0; i < userPrograms.length(); i++) {
                    try {
                        JSONObject programJson = userPrograms.getJSONObject(i);
                        

                        String templateId = programJson.optString("template_id", null);
                        if (templateId != null && !templateId.isEmpty()) {

                            JSONArray templateResults = supabaseClient.from("program_templates")
                                .eq("id", templateId)
                                .executeAndGetArray();
                            
                            if (templateResults != null && templateResults.length() > 0) {
                                JSONObject templateJson = templateResults.getJSONObject(0);
                                WorkoutProgram program = parseTemplateToProgram(templateJson);
                                
                                if (program != null) {

                                    program.setId(programJson.optString("id", program.getId()));
                                    program.setUserId(userId);
                                    program.setType(ProgramType.USER_CREATED);
                                    

                                    String key = program.getId();
                                    if (!uniquePrograms.containsKey(key)) {
                                        uniquePrograms.put(key, program);
                                    }
                                }
                            }
                        } else {

                            WorkoutProgram program = new WorkoutProgram();
                            program.setId(programJson.optString("id", UUID.randomUUID().toString()));
                            program.setName(programJson.optString("name", "Программа без названия"));
                            program.setDescription(programJson.optString("description", ""));
                            program.setDurationWeeks(programJson.optInt("duration_weeks", 4));
                            program.setDaysPerWeek(programJson.optInt("days_per_week", 3));
                            program.setType(ProgramType.USER_CREATED);
                            program.setUserId(userId);
                            

                            String key = program.getId();
                            if (!uniquePrograms.containsKey(key)) {
                                uniquePrograms.put(key, program);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке программы из таблицы programs: " + e.getMessage(), e);
                    }
                }
            }
            

            List<WorkoutProgram> result = new ArrayList<>(uniquePrograms.values());

            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении пользовательских программ: " + e.getMessage(), e);
            throw e;
        }
    }
    

    

    public void getAllProgramsAsync(com.martist.vitamove.utils.AsyncCallback<List<WorkoutProgram>> callback) {
        new Thread(() -> {
            try {
                List<WorkoutProgram> programs = getAllPrograms();

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(programs));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при асинхронном получении программ", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        }).start();
    }
    

    private WorkoutProgram parseTemplateToProgram(JSONObject json) {
        try {
            WorkoutProgram program = new WorkoutProgram();
            
            program.setId(json.optString("id", UUID.randomUUID().toString()));
            program.setName(json.optString("name", "Программа без названия"));
            program.setDescription(json.optString("description", ""));
            

            String imageUrl = "";
            if (json.has("image")) {
                imageUrl = json.optString("image", "");
            } else if (json.has("img_url")) {
                imageUrl = json.optString("img_url", "");
            } else if (json.has("image_path")) {
                imageUrl = json.optString("image_path", "");
            } else if (json.has("thumbnail") || json.has("thumbnail_url")) {
                imageUrl = json.has("thumbnail") ? json.optString("thumbnail", "") : json.optString("thumbnail_url", "");
            } else if (json.has("cover_image")) {
                imageUrl = json.optString("cover_image", "");
            }
            program.setImageUrl(imageUrl);
            

            String difficultyStr = json.optString("difficulty", "начинающий");
            program.setLevel(difficultyStr);
            

            String categoryStr = json.optString("category", "общая подготовка");
            program.setGoal(categoryStr);
            
            program.setDurationWeeks(json.optInt("duration_weeks", 4));
            program.setDaysPerWeek(json.optInt("days_per_week", 3));
            

            String programTypeStr = json.optString("type", "");
            program.setProgramType(programTypeStr);
            

            if (json.optBoolean("is_public", true)) {
                program.setType(ProgramType.OFFICIAL);
            } else {
                program.setType(ProgramType.USER_CREATED);
            }
            

            program.setAuthor(json.optString("author_id", ""));
            

            String createdAtStr = json.optString("created_at", "");
            String updatedAtStr = json.optString("updated_at", "");
            
            if (!createdAtStr.isEmpty()) {
                try {

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                    Date date = format.parse(createdAtStr);
                    program.setCreatedAt(date.getTime());
                } catch (Exception e) {

                }
            }
            
            if (!updatedAtStr.isEmpty()) {
                try {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                    Date date = format.parse(updatedAtStr);
                    program.setUpdatedAt(date.getTime());
                } catch (Exception e) {

                }
            }
            

            try {

                List<Integer> workoutDays = new ArrayList<>();
                
                if (json.has("workout_days") && !json.isNull("workout_days")) {
                    try {

                        JSONArray workoutDaysArray = json.getJSONArray("workout_days");
                        for (int i = 0; i < workoutDaysArray.length(); i++) {
                            workoutDays.add(workoutDaysArray.getInt(i));
                        }

                        


                        if (!workoutDays.isEmpty()) {

                            program.setDaysPerWeek(workoutDays.size());

                        }
                    } catch (Exception e) {


                        workoutDays = getDefaultWorkoutDays(program.getDaysPerWeek());

                    }
                } else {

                    workoutDays = getDefaultWorkoutDays(program.getDaysPerWeek());

                }
                
                program.setWorkoutDays(workoutDays);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при установке дней тренировок: " + e.getMessage());
            }
            
            return program;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при парсинге шаблона программы: " + e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public List<WorkoutProgram> filterPrograms(String goal, String level, int maxDuration) {

        
        List<WorkoutProgram> programs = new ArrayList<>();
        
        try {

            SupabaseClient.QueryBuilder queryBuilder = supabaseClient.from("program_templates");
            


            if (goal != null && !goal.isEmpty() && !goal.equalsIgnoreCase("все")) {
                queryBuilder.eq("category", goal);

            }
            

            if (level != null && !level.isEmpty() && !level.equalsIgnoreCase("все")) {
                queryBuilder.eq("difficulty", level);

            }
            

            if (maxDuration > 0) {
                queryBuilder.lte("duration_weeks", maxDuration);

            }
            

            JSONArray results = queryBuilder.executeAndGetArray();
            

            if (results != null && results.length() > 0) {
                for (int i = 0; i < results.length(); i++) {
                    JSONObject json = results.getJSONObject(i);
                    WorkoutProgram program = parseTemplateToProgram(json);
                    

                    boolean isDuplicate = false;
                    for (WorkoutProgram existingProgram : programs) {
                        if (existingProgram.getId().equals(program.getId())) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    
                    if (!isDuplicate) {
                            programs.add(program);
                        }
                    }
                    

                } else {

            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при фильтрации шаблонов программ: " + e.getMessage(), e);

            return new ArrayList<>();
        }
        
        return programs;
    }
    
    @Override
    public WorkoutProgram getProgramById(String programId) {
        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "ID программы пуст или null");
            return null;
        }
        

        
        try {

            JSONObject result = supabaseClient.from("program_templates")
                .eq("id", programId)
                .executeAndGetSingle();
            
            if (result != null) {
                WorkoutProgram program = parseTemplateToProgram(result);
                
                return program;
            } else {

                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении программы по ID " + programId + ": " + e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public WorkoutProgram getActiveProgram(String userId) {

        
        try {

            String realUserId = getRealUserId(userId);
            if (realUserId == null) {
                Log.e(TAG, "Не удалось получить реальный ID пользователя");
                return null;
            }
            

            JSONArray results = supabaseClient.from("programs")
                .eq("user_id", realUserId)
                .eq("is_active", true)
                .executeAndGetArray();
            
            if (results != null && results.length() > 0) {
                JSONObject programJson = results.getJSONObject(0);
                

                WorkoutProgram program = parseTemplateToProgram(programJson);
                program.setActive(true);



                List<Integer> savedWorkoutDays = loadWorkoutDaysFromPrefs(program.getId());
                if (savedWorkoutDays != null && !savedWorkoutDays.isEmpty()) {
                    program.setWorkoutDays(savedWorkoutDays);

                    program.setDaysPerWeek(savedWorkoutDays.size());

                } else {



                }

                return program;
            }
            

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении активной программы: " + e.getMessage(), e);
        return null;
        }
    }
    
    @Override
    public String startProgram(String userId, String programId, long startDate) {

        
        try {

            WorkoutProgram template = getProgramById(programId);
            if (template == null) {
                Log.e(TAG, "Ошибка при запуске программы: шаблон не найден (ID: " + programId + ")");
                return null;
            }
            

            String realUserId = getRealUserId(userId);
            if (realUserId == null) {
                Log.e(TAG, "Ошибка при запуске программы: не удалось получить реальный ID пользователя");
                return null;
            }
            



            List<Integer> userSelectedDays = loadWorkoutDaysFromPrefs(programId);

            

            if (userSelectedDays.isEmpty()) {
                userSelectedDays = template.getWorkoutDays();

            }
            

            JSONArray daysArray = new JSONArray();
            for (Integer day : userSelectedDays) {
                daysArray.put(day);
            }
            

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            Date date = new Date(startDate);
            


            JSONObject result = supabaseClient.rpc("create_program_from_template")
                .param("p_template_id", programId)
                .param("p_user_id", realUserId)
                .param("p_program_name", template.getName())
                .param("p_workout_days", daysArray)
                .param("p_start_date", dateFormat.format(date))
                .executeAndGetSingle();
            
            String newProgramId = result.getString("program_id");

            

            saveWorkoutDaysToPrefs(newProgramId, userSelectedDays);
            
            return newProgramId;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске программы: " + e.getMessage(), e);
            return null;
        }
    }
    

    public void saveWorkoutDaysToPrefs(String programId, List<Integer> workoutDays) {

        
        try {

            SharedPreferences prefs = context.getSharedPreferences("program_config_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            

            StringBuilder daysBuilder = new StringBuilder();
            for (int i = 0; i < workoutDays.size(); i++) {
                daysBuilder.append(workoutDays.get(i));
                if (i < workoutDays.size() - 1) {
                    daysBuilder.append(",");
                }
            }
            
            String daysString = daysBuilder.toString();

            
            editor.putString("config_workout_days_" + programId, daysString);
            editor.apply();
            

        } catch (Exception e) {
            Log.e(TAG, "!!! VITAMOVE_DEBUG: [SAVE WORKOUT DAYS] Ошибка при сохранении дней тренировок: " + e.getMessage(), e);
        }
    }



    private String parseDifficulty(String difficultyStr) {
        switch (difficultyStr.toLowerCase()) {
            case "intermediate": return "intermediate";
            case "advanced": return "advanced";
            default: return "beginner";
        }
    }
    

    

    private String getRealUserId(String defaultUserId) {
        try {

            VitaMoveApplication app = 
                (VitaMoveApplication) VitaMoveApplication.getContext();
            

            String appUserId = app.getCurrentUserId();
            
            if (appUserId != null && !appUserId.trim().isEmpty()) {


                return appUserId;
            } else {

                String hardcodedUserId = "fb5e69f1-c2dc-4572-95a8-cb54e155a3c9";

                return hardcodedUserId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении ID пользователя: " + e.getMessage(), e);

            return "fb5e69f1-c2dc-4572-95a8-cb54e155a3c9";
        }
    }

    
    @Override
    public List<ProgramDay> getProgramDays(String programId) {
        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "ID программы пуст или null");
            return new ArrayList<>();
        }
        

        
        List<ProgramDay> days = new ArrayList<>();
        
        try {

            JSONArray templateResults = supabaseClient.from("program_templates")
                .eq("id", programId)
                .executeAndGetArray();
                
            boolean isTemplate = templateResults != null && templateResults.length() > 0;
            
            if (isTemplate) {


                JSONArray templateDaysResults = supabaseClient.from("program_template_days")
                    .eq("template_id", programId)
                    .order("day_number", true)
                    .executeAndGetArray();
                    
                if (templateDaysResults != null && templateDaysResults.length() > 0) {
                    for (int i = 0; i < templateDaysResults.length(); i++) {
                        JSONObject dayJson = templateDaysResults.getJSONObject(i);
                        ProgramDay day = parseProgramTemplateDay(dayJson);
                            days.add(day);
                        }
                    

                } else {

                    }
                } else {


                JSONArray programDaysResults = supabaseClient.from("program_days")
                    .eq("program_id", programId)
                    .order("day_number", true)
                    .executeAndGetArray();
                    
                if (programDaysResults != null && programDaysResults.length() > 0) {
                    for (int i = 0; i < programDaysResults.length(); i++) {
                        JSONObject dayJson = programDaysResults.getJSONObject(i);
                        ProgramDay day = parseProgramDay(dayJson);
                        days.add(day);
                    }
                    

            } else {

                }
            }
            
            if (isTemplate) {

            }
            


            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении дней программы: " + e.getMessage(), e);
        }
        
        return days;
    }
    

    private ProgramDay parseProgramTemplateDay(JSONObject json) {
        try {
            ProgramDay day = new ProgramDay();
            day.setId(json.getString("id"));
            day.setProgramId(json.getString("template_id"));
            day.setDayNumber(json.getInt("day_number"));
            day.setName(json.optString("name", "День " + json.getInt("day_number")));
            day.setDescription(json.optString("description", ""));
            return day;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при парсинге дня шаблона программы: " + e.getMessage(), e);
            return null;
        }
    }
    

    private ProgramDay parseProgramDay(JSONObject json) {
        try {
            ProgramDay day = new ProgramDay();
            
            day.setId(json.getString("id"));
            

            if (json.has("program_id")) {
            day.setProgramId(json.getString("program_id"));
            } else if (json.has("template_id")) {
                day.setProgramId(json.getString("template_id"));
            } else {



                day.setProgramId("");
            }
            
            day.setDayNumber(json.getInt("day_number"));
            day.setName(json.getString("name"));
            day.setDescription(json.optString("description", ""));
            

            String focusArea = json.optString("focus_area", "");
            day.setFocusArea(focusArea);
            
            day.setEstimatedDurationMinutes(json.optInt("estimated_duration", 0));
            

            if (json.has("template_day_id") && !json.isNull("template_day_id")) {
                day.setTemplateDayId(json.getString("template_day_id"));
            }
            

            day.setRestBetweenExercisesSec(60);
            day.setCompleted(false);
            

            List<ProgramExercise> exercises = getProgramDayExercises(day.getId());
            day.setExercises(exercises);
            
            return day;
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при парсинге дня программы: " + e.getMessage(), e);
            return null;
        }
    }
    

    

    @Override
    public List<ProgramExercise> getProgramDayExercises(String dayId) {

        
        if (dayId == null || dayId.isEmpty()) {
            Log.e(TAG, "ID дня программы пустой, невозможно получить упражнения");
            return new ArrayList<>();
        }
        
        List<ProgramExercise> exercises = new ArrayList<>();
        
        try {

            boolean isTemplateDay = false;
            

            try {
                JSONArray templateDayCheck = supabaseClient.from("program_template_days")
                    .eq("id", dayId)
                    .limit(1)
                    .executeAndGetArray();
                
                isTemplateDay = templateDayCheck != null && templateDayCheck.length() > 0;

            } catch (Exception e) {


            }
            
            String tableName;
            String idFieldName;
            
            if (isTemplateDay) {

                tableName = "program_template_exercises";
                idFieldName = "day_id";

            } else {

                tableName = "program_exercises";
                idFieldName = "day_id";

            }
            

            JSONArray results = supabaseClient.from(tableName)
                .eq(idFieldName, dayId)
                .order("order_number", true)
                .executeAndGetArray();
                
            if (results != null && results.length() > 0) {
                for (int i = 0; i < results.length(); i++) {
                    JSONObject exerciseJson = results.getJSONObject(i);
                    ProgramExercise exercise = parseProgramExercise(exerciseJson, isTemplateDay);
                    

                    if (exercise != null && exercise.getExerciseId() != null && !exercise.getExerciseId().isEmpty()) {
                        try {

                            JSONArray exerciseData = supabaseClient.from("exercises")
                                .select("*")
                                .eq("id", exercise.getExerciseId())
                                .executeAndGetArray();
                            
                            if (exerciseData != null && exerciseData.length() > 0) {
                                Exercise baseExercise = parseExercise(exerciseData.getJSONObject(0));
                                exercise.setExercise(baseExercise);

                            } else {

                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при загрузке деталей упражнения: " + e.getMessage(), e);
                        }
                    }
                    
                    exercises.add(exercise);
                }
                

            } else {

            }
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении упражнений для дня программы: " + e.getMessage(), e);
        }
        
        return exercises;
    }
    

    private ProgramExercise parseProgramExercise(JSONObject json, boolean isTemplateExercise) {
        try {
            ProgramExercise exercise = new ProgramExercise();
            
            exercise.setId(json.getString("id"));
            

            if (isTemplateExercise) {

                if (json.has("template_day_id")) {
                    exercise.setProgramDayId(json.getString("template_day_id"));
                } else if (json.has("day_id")) {
                    exercise.setProgramDayId(json.getString("day_id"));
                }
            } else {

                if (json.has("day_id")) {
                    exercise.setProgramDayId(json.getString("day_id"));
                } else if (json.has("template_day_id")) {
                    exercise.setProgramDayId(json.getString("template_day_id"));
                }
            }
            

            if (exercise.getProgramDayId() == null || exercise.getProgramDayId().isEmpty()) {

            }
            
            exercise.setExerciseId(json.optString("exercise_id", ""));
            

            if (json.has("order_index")) {
                exercise.setOrderNumber(json.optInt("order_index", 0));
            } else {
                exercise.setOrderNumber(json.optInt("order_number", 0));
            }
            

            String repsRange = json.optString("reps_range", json.optString("target_reps", "10"));
            try {
                exercise.setTargetReps(Integer.parseInt(repsRange.split("-")[0].trim()));
            } catch (Exception e) {
                exercise.setTargetReps(10);
            }
            
            exercise.setTargetSets(json.optInt("sets", json.optInt("target_sets", 3)));
            
            String weightRange = json.optString("weight_range", json.optString("target_weight", "0"));
            try {
                exercise.setTargetWeight(Float.parseFloat(weightRange.split("-")[0].trim()));
            } catch (Exception e) {
                exercise.setTargetWeight(0f);
            }
            
            exercise.setNotes(json.optString("notes", ""));
            

            if (json.has("template_exercise_id") && !json.isNull("template_exercise_id")) {
                exercise.setTemplateExerciseId(json.getString("template_exercise_id"));
            } else {
                exercise.setTemplateExerciseId(null);
            }
            

            int restTime = 60;
            if (json.has("rest_seconds")) {
                restTime = json.optInt("rest_seconds", 60);
            } else if (json.has("rest_between_sets_sec")) {
                restTime = json.optInt("rest_between_sets_sec", 60);
            }
            exercise.setRestBetweenSetsSec(restTime);
            




            
            return exercise;
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при парсинге упражнения: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String createProgramExercise(String dayId, ProgramExercise exercise) {

        
        try {

            JSONObject exerciseData = new JSONObject();
            exerciseData.put("id", exercise.getId());
            exerciseData.put("day_id", dayId);
            exerciseData.put("exercise_id", exercise.getExerciseId());
            exerciseData.put("order_number", exercise.getOrderNumber());
            exerciseData.put("target_sets", exercise.getTargetSets());
            exerciseData.put("target_reps", String.valueOf(exercise.getTargetReps()));
            exerciseData.put("target_weight", String.valueOf(exercise.getTargetWeight()));
            exerciseData.put("rest_seconds", exercise.getRestBetweenSetsSec());
            exerciseData.put("notes", exercise.getNotes() != null ? exercise.getNotes() : "");
            


            String templateExerciseId = exercise.getTemplateExerciseId();
            



            if (templateExerciseId != null && !templateExerciseId.isEmpty()) {
                exerciseData.put("template_exercise_id", templateExerciseId);

            } else {

                exerciseData.put("template_exercise_id", JSONObject.NULL);

            }
            


            

            JSONObject checkResponse = null;
            try {
                checkResponse = supabaseClient.from("program_exercises")
                    .select("id")
                    .eq("id", exercise.getId())
                    .executeAndGetSingle();
            } catch (Exception e) {


            }
            
            JSONObject response;
            if (checkResponse != null && checkResponse.has("id")) {


                response = supabaseClient.from("program_exercises")
                    .update(exerciseData)
                    .eq("id", exercise.getId())
                    .executeAndGetSingle();
                    

                if (response == null) {


                    response = new JSONObject();
                    response.put("id", exercise.getId());
                }
            } else {

                try {
                    response = supabaseClient.insertRecord("program_exercises", exerciseData);
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("409")) {


                        response = supabaseClient.from("program_exercises")
                            .update(exerciseData)
                            .eq("id", exercise.getId())
                            .executeAndGetSingle();
                        

                        if (response == null) {


                            response = new JSONObject();
                            response.put("id", exercise.getId());
                        }
                    } else {

                        throw e;
                    }
                }
            }
            
            if (response != null) {

                return exercise.getId();
            } else {
                Log.e(TAG, "Ошибка при создании упражнения в Supabase: ответ null");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании упражнения: " + e.getMessage(), e);
            return null;
        }
    }
    

    
    @Override
    public boolean updateProgram(WorkoutProgram program) {

        
        try {

            JSONObject programData = new JSONObject();
            programData.put("name", program.getName());
            programData.put("description", program.getDescription());
            

            List<Integer> workoutDays = program.getWorkoutDays();
            if (workoutDays != null && !workoutDays.isEmpty()) {

                programData.put("days_per_week", workoutDays.size());

            } else {

                programData.put("days_per_week", program.getDaysPerWeek());

            }
            

            programData.put("type", program.getType().toString());
            



            

            supabaseClient.from("programs")
                .update(programData)
                .eq("id", program.getId())
                .executeUpdate();
            

            try {
                JSONArray results = supabaseClient.from("program_templates")
                    .eq("id", program.getId())
                    .executeAndGetArray();
                
                if (results != null && results.length() > 0) {

                    JSONObject templateData = new JSONObject();
                    templateData.put("name", program.getName());
                    templateData.put("description", program.getDescription());
                    templateData.put("days_per_week", programData.getInt("days_per_week"));
                    templateData.put("type", program.getType().toString());
                    

                    templateData.put("difficulty", program.getLevel());
                    

                    supabaseClient.from("program_templates")
                        .update(templateData)
                        .eq("id", program.getId())
                        .executeUpdate();

                }
            } catch (Exception e) {

            }
            

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении программы: " + e.getMessage(), e);
            return false;
        }
    }




    @Override
    public ProgramDay getProgramDayById(String dayId) {

        
        if (dayId == null || dayId.isEmpty()) {
            Log.e(TAG, "ID дня программы пустой, невозможно получить день программы");
            return null;
        }
        
        try {

            JSONArray templateResults = supabaseClient.from("program_template_days")
                .eq("id", dayId)
                .executeAndGetArray();
                
            if (templateResults != null && templateResults.length() > 0) {

                JSONObject dayJson = templateResults.getJSONObject(0);
                return parseProgramTemplateDay(dayJson);
            }
            

            JSONArray dayResults = supabaseClient.from("program_days")
                .eq("id", dayId)
                .executeAndGetArray();
                
            if (dayResults != null && dayResults.length() > 0) {

                JSONObject dayJson = dayResults.getJSONObject(0);
                return parseProgramDay(dayJson);
            }
            

            


            for (WorkoutProgram program : getAllPrograms()) {

                List<ProgramDay> days = getProgramDays(program.getId());
                

                for (ProgramDay day : days) {
                    if (day.getId().equals(dayId)) {

                        return day;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении дня программы по ID: " + e.getMessage(), e);
            


            for (WorkoutProgram program : getAllPrograms()) {

                List<ProgramDay> days = getProgramDays(program.getId());
                

                for (ProgramDay day : days) {
                    if (day.getId().equals(dayId)) {

                        return day;
                    }
                }
            }
            
            return null;
        }
    }


    
    @Override
    public void deactivateProgram(String programId) throws Exception {
        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "Невозможно деактивировать программу: programId пустой");
            throw new IllegalArgumentException("ID программы не может быть пустым");
        }
        

            
        try {

            JSONArray checkResults = supabaseClient.from("programs")
                .eq("id", programId)
                .executeAndGetArray();
                
            if (checkResults == null || checkResults.length() == 0) {

                return;
            }
            

            JSONObject programJson = checkResults.getJSONObject(0);
            String userId = programJson.optString("user_id", null);
            boolean isActive = programJson.optBoolean("is_active", false);
            

            
            if (!isActive) {

                return;
            }
            

            JSONObject updateData = new JSONObject();
            updateData.put("is_active", false);
            

            supabaseClient.from("programs")
                .eq("id", programId)
                .update(updateData)
                .executeUpdate();
                

            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при деактивации программы: " + e.getMessage(), e);
            throw new Exception("Ошибка при деактивации программы: " + e.getMessage(), e);
        }
    }




    private Exercise parseExercise(JSONObject json) {
        try {
            String id = json.getString("id");
            String name = json.getString("name");
            String description = json.optString("description", "");
            




            

            String difficultyStr = json.optString("difficulty", "beginner");
            String difficulty = parseDifficulty(difficultyStr);
            

            String typeStr = json.optString("type", "strength");
            ExerciseType exerciseType = ExerciseType.strength;
            try {
                exerciseType = ExerciseType.valueOf(typeStr.toLowerCase());
            } catch (Exception e) {

            }
            

            ExerciseMedia media = new ExerciseMedia();
            if (json.has("image_url") && !json.isNull("image_url")) {
                media.setPreviewImage(json.getString("image_url"));
            }
            if (json.has("video_url") && !json.isNull("video_url")) {
                media.setAnimationUrl(json.getString("video_url"));
            }
            

            int defaultSets = json.optInt("default_sets", 3);
            String defaultReps = json.optString("default_reps", "12");
            int defaultRestSeconds = json.optInt("default_rest_seconds", 60);
            float met = (float) json.optDouble("met", 5.0);
            

            return new Exercise.Builder()
                .id(id)
                .name(name)
                .description(description)
                .difficulty(difficulty)
                .exerciseType(String.valueOf(exerciseType))
                .media(media)
                .defaultSets(defaultSets)
                .defaultReps(defaultReps)
                .defaultRestSeconds(defaultRestSeconds)
                .met(met)
                .build();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при парсинге упражнения: " + e.getMessage(), e);
            return null;
        }
    }


    @Override
    public ProgramExercise getProgramExerciseById(String exerciseId) {

        
        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.e(TAG, "ID упражнения не может быть пустым");
            return null;
        }
        
        try {

            boolean isTemplateExercise = false;
            
            try {
                JSONArray templateExerciseCheck = supabaseClient.from("program_template_exercises")
                    .eq("id", exerciseId)
                    .limit(1)
                    .executeAndGetArray();
                    
                isTemplateExercise = templateExerciseCheck != null && templateExerciseCheck.length() > 0;

            } catch (Exception e) {


            }
            

            String tableName = isTemplateExercise ? "program_template_exercises" : "program_exercises";

            

            JSONArray results = supabaseClient.from(tableName)
                .eq("id", exerciseId)
                .executeAndGetArray();
                
            if (results != null && results.length() > 0) {
                JSONObject exerciseJson = results.getJSONObject(0);
                ProgramExercise exercise = parseProgramExercise(exerciseJson, isTemplateExercise);
                

                if (exercise != null && exercise.getExerciseId() != null && !exercise.getExerciseId().isEmpty()) {
                    try {
                        JSONArray exerciseData = supabaseClient.from("exercises")
                            .eq("id", exercise.getExerciseId())
                            .executeAndGetArray();
                            
                        if (exerciseData != null && exerciseData.length() > 0) {
                            Exercise baseExercise = parseExercise(exerciseData.getJSONObject(0));
                            exercise.setExercise(baseExercise);
                        }
                    } catch (Exception e) {

                    }
                }
                
                return exercise;
            } else {

                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении упражнения по ID: " + e.getMessage(), e);
            return null;
        }
    }


    private List<Integer> getDefaultWorkoutDays(int daysPerWeek) {
        List<Integer> workoutDays = new ArrayList<>();

        if (daysPerWeek == 3) {
            workoutDays.add(0);
            workoutDays.add(2);
            workoutDays.add(4);
        } else if (daysPerWeek == 4) {
            workoutDays.add(0);
            workoutDays.add(1);
            workoutDays.add(3);
            workoutDays.add(5);
        } else if (daysPerWeek == 5) {
            workoutDays.add(0);
            workoutDays.add(1);
            workoutDays.add(2);
            workoutDays.add(3);
            workoutDays.add(4);
        } else {

            for (int i = 0; i < daysPerWeek && i < 7; i++) {
                workoutDays.add(i);
            }
        }
        return workoutDays;
    }


    public List<Integer> loadWorkoutDaysFromPrefs(String programId) {

        
        try {

            SharedPreferences prefs = context.getSharedPreferences("program_config_prefs", Context.MODE_PRIVATE);
            String daysString = prefs.getString("config_workout_days_" + programId, null);
            

            
            if (daysString != null && !daysString.isEmpty()) {

                String[] daysArray = daysString.split(",");
                List<Integer> workoutDays = new ArrayList<>();
                
                for (String day : daysArray) {
                    workoutDays.add(Integer.parseInt(day.trim()));
                }
                

                return workoutDays;
            } else {

            }
            


            
            try {

                JSONArray plansResult = supabaseClient
                    .from("workout_plans")
                    .eq("program_id", programId)
                    .limit(30)
                    .executeAndGetArray();
                
                if (plansResult != null && plansResult.length() > 0) {

                    

                    Set<Integer> workoutDaysSet = new HashSet<>();
                    

                    for (int i = 0; i < plansResult.length(); i++) {
                        JSONObject plan = plansResult.getJSONObject(i);
                        

                        String plannedDateStr = plan.getString("planned_date");
                        long plannedDateMillis = parseIsoDateTime(plannedDateStr);
                        

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(plannedDateMillis);
                        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                        

                        int dayIndex = (dayOfWeek + 5) % 7;
                        
                        workoutDaysSet.add(dayIndex);
                    }
                    

                    if (!workoutDaysSet.isEmpty()) {
                        List<Integer> workoutDays = new ArrayList<>(workoutDaysSet);
                        Collections.sort(workoutDays);
                        

                        

                        saveWorkoutDaysToPrefs(programId, workoutDays);
                        
                        return workoutDays;
                    }
                }
                

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при попытке загрузить дни тренировок из workout_plans: " + e.getMessage(), e);
            }
            

            List<Integer> defaultDays = getDefaultWorkoutDays(3);

            return defaultDays;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке дней тренировок: " + e.getMessage(), e);
            List<Integer> defaultDays = getDefaultWorkoutDays(3);

            return defaultDays;
        }
    }
    

    private long parseIsoDateTime(String isoDateTimeString) throws Exception {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Date date = format.parse(isoDateTimeString);
            return date.getTime();
        } catch (Exception e) {
            try {

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                Date date = format.parse(isoDateTimeString);
                return date.getTime();
            } catch (Exception e2) {

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date date = format.parse(isoDateTimeString);
                return date.getTime();
            }
        }
    }
    

    public void getFullProgramAsync(String programId, com.martist.vitamove.utils.AsyncCallback<JSONObject> callback) {

        

        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "getFullProgramAsync: Invalid programId");

            new Handler(Looper.getMainLooper()).post(() -> 
                callback.onFailure(new IllegalArgumentException("Invalid programId"))
            );
                return;
            }
            

        new Thread(() -> {
            try {

                JSONObject programJson = supabaseClient.rpc("get_full_program")
                    .param("program_uuid", programId)
                    .executeAndGetSingle();


                if (programJson == null || programJson.length() == 0) {

                    throw new Exception("Программа не найдена или пуста.");
                }
                


                


                final JSONObject finalResult = programJson;
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(finalResult));

            } catch (Exception e) {
                Log.e(TAG, "getFullProgramAsync: Ошибка выполнения RPC get_full_program или обработки ответа", e);

                 new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        }).start();
    }



    public List<WorkoutPlan> getWorkoutPlansForProgram(String programId) throws Exception {

        List<WorkoutPlan> workoutPlans = new ArrayList<>();

        try {
            JSONArray results = supabaseClient.from("workout_plans")
                    .eq("program_id", programId)


                    .executeAndGetArray();

            if (results != null) {


                int logLimit = Math.min(results.length(), 3);
                if (results.length() > 0) {

                    for (int i = 0; i < logLimit; i++) {
                        try {

                        } catch (Exception jsonEx) {

                        }
                    }
                 } else {

                 }

                for (int i = 0; i < results.length(); i++) {
                    JSONObject planJson = results.getJSONObject(i);
                    WorkoutPlan plan = parseWorkoutPlanFromJson(planJson);
                    if (plan != null) {
                        workoutPlans.add(plan);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении планов тренировок из Supabase для программы ID: " + programId, e);
            throw e;
        }

        return workoutPlans;
    }


    private WorkoutPlan parseWorkoutPlanFromJson(JSONObject jsonObject) {
        if (jsonObject == null) return null;
        WorkoutPlan plan = new WorkoutPlan();
        try {
            plan.setId(jsonObject.optString("id"));
            plan.setUserId(jsonObject.optString("user_id"));
            plan.setName(jsonObject.optString("name"));
            plan.setProgramId(jsonObject.optString("program_id"));
            plan.setProgramDayId(jsonObject.optString("program_day_id"));
            plan.setStatus(jsonObject.optString("status"));
            plan.setNotes(jsonObject.optString("notes"));


            String plannedDateStr = jsonObject.optString("planned_date");
            String createdAtStr = jsonObject.optString("created_at");
            String updatedAtStr = jsonObject.optString("updated_at");

            plan.setPlannedDate(DateUtils.parseIsoDate(plannedDateStr));
            plan.setCreatedAt(DateUtils.parseIsoDate(createdAtStr));
            plan.setUpdatedAt(DateUtils.parseIsoDate(updatedAtStr));




            return plan;
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка парсинга даты в parseWorkoutPlanFromJson: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
             Log.e(TAG, "Ошибка парсинга WorkoutPlan из JSON: " + e.getMessage(), e);
             return null;
        }
    }


} 