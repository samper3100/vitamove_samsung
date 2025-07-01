package com.martist.vitamove.viewmodels;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.db.AppDatabase;
import com.martist.vitamove.db.dao.WorkoutDao;
import com.martist.vitamove.db.entity.ExerciseSetEntity;
import com.martist.vitamove.db.entity.UserWorkoutEntity;
import com.martist.vitamove.db.entity.WorkoutExerciseEntity;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.UserWorkout;
import com.martist.vitamove.workout.data.models.WorkoutExercise;
import com.martist.vitamove.workout.data.models.WorkoutPlan;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HistoryViewModel extends AndroidViewModel {
    private static final String TAG = "HistoryViewModel";
    private static final String PREF_NAME = "workout_history_cache";
    private static final String KEY_PREFIX_CACHE = "page_";
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private static final String KEY_WORKOUT_IDS = "workout_ids";
    
    private final WorkoutRepository workoutRepository;
    private final MutableLiveData<List<UserWorkout>> recentWorkouts;
    private final MutableLiveData<List<UserWorkout>> allWorkouts;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Integer> totalWorkouts;
    
    
    private final LruCache<String, List<UserWorkout>> workoutCache;
    private final Map<Long, UserWorkout> workoutsByDate;
    private final Map<Long, WorkoutPlan> plansByDate;
    
    
    private final int PAGE_SIZE = 10;
    private final boolean hasMoreWorkouts = true;
    private final int currentPage = 0;
    
    
    private final Executor executor;
    
    
    private final long startDate;
    private final long endDate;
    
    private static final int RECENT_WORKOUTS_LIMIT = 3;
    
    
    private static final long CACHE_EXPIRATION_TIME = 24 * 60 * 60 * 1000;
    
    
    private final SharedPreferences prefs;

    
    private final String userId;

    public HistoryViewModel(Application application) {
        super(application);
        workoutRepository = ((VitaMoveApplication) application).getWorkoutRepository();
        recentWorkouts = new MutableLiveData<>(new ArrayList<>());
        allWorkouts = new MutableLiveData<>(new ArrayList<>());
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>("");
        totalWorkouts = new MutableLiveData<>(0);
        
        
        userId = ((VitaMoveApplication) application).getCurrentUserId();
        
        
        workoutCache = new LruCache<>(30);
        workoutsByDate = new HashMap<>();
        plansByDate = new HashMap<>();
        
        
        
        prefs = application.getSharedPreferences(
            userId != null ? PREF_NAME + "_" + userId : PREF_NAME, 
            Application.MODE_PRIVATE
        );
        
        
        executor = Executors.newCachedThreadPool();
        
        
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, -6);
        startDate = calendar.getTimeInMillis();
        
        
        loadCachedData();
        
        
        loadRecentWorkouts();
    }

    
    private void loadCachedData() {
        long lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();
        
        
        if (currentTime - lastUpdateTime > CACHE_EXPIRATION_TIME) {
            
            
            prefs.edit().clear().apply();
            return;
        }
        
        try {
            
            String workoutIdsJson = prefs.getString(KEY_WORKOUT_IDS, null);
            if (workoutIdsJson != null) {
                JSONArray workoutIdsArray = new JSONArray(workoutIdsJson);
                
                
                for (int i = 0; i < workoutIdsArray.length(); i++) {
                    String workoutId = workoutIdsArray.getString(i);
                    String workoutJson = prefs.getString("workout_" + workoutId, null);
                    
                    if (workoutJson != null) {
                        UserWorkout workout = parseWorkoutFromJson(new JSONObject(workoutJson));
                        if (workout != null) {
                            
                            long normalizedDate = normalizeDate(workout.getStartTime());
                            workoutsByDate.put(normalizedDate, workout);
                        }
                    }
                }
                
                
                
                
                for (int i = 0; i < 30; i++) {
                    String pageKey = KEY_PREFIX_CACHE + i;
                    String pageWorkoutIdsJson = prefs.getString(pageKey, null);
                    
                    if (pageWorkoutIdsJson != null) {
                        JSONArray pageWorkoutIds = new JSONArray(pageWorkoutIdsJson);
                        List<UserWorkout> pageWorkouts = new ArrayList<>();
                        
                        for (int j = 0; j < pageWorkoutIds.length(); j++) {
                            String workoutId = pageWorkoutIds.getString(j);
                            
                            for (UserWorkout workout : workoutsByDate.values()) {
                                if (workout.getId().equals(workoutId)) {
                                    pageWorkouts.add(workout);
                                    break;
                                }
                            }
                        }
                        
                        if (!pageWorkouts.isEmpty()) {
                            workoutCache.put(getCacheKey(i), pageWorkouts);
                            
                            if (i == 0) {
                                
                                allWorkouts.postValue(new ArrayList<>(pageWorkouts));
                                
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при загрузке кэша: " + e.getMessage());
            
            prefs.edit().clear().apply();
        }
    }
    
    
    private void saveCacheData() {
        if (workoutsByDate.isEmpty()) return;
        
        try {
            SharedPreferences.Editor editor = prefs.edit();
            
            
            editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
            
            
            JSONArray workoutIdsArray = new JSONArray();
            for (UserWorkout workout : workoutsByDate.values()) {
                workoutIdsArray.put(workout.getId());
                
                
                JSONObject workoutJson = serializeWorkout(workout);
                editor.putString("workout_" + workout.getId(), workoutJson.toString());
            }
            editor.putString(KEY_WORKOUT_IDS, workoutIdsArray.toString());
            
            
            for (int i = 0; i < 30; i++) {
                List<UserWorkout> pageWorkouts = workoutCache.get(getCacheKey(i));
                if (pageWorkouts != null && !pageWorkouts.isEmpty()) {
                    JSONArray pageWorkoutIds = new JSONArray();
                    for (UserWorkout workout : pageWorkouts) {
                        pageWorkoutIds.put(workout.getId());
                    }
                    editor.putString(KEY_PREFIX_CACHE + i, pageWorkoutIds.toString());
                }
            }
            
            editor.apply();
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении кэша: " + e.getMessage());
        }
    }
    
    
    private JSONObject serializeWorkout(UserWorkout workout) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", workout.getId());
            json.put("userId", workout.getUserId());
            json.put("name", workout.getName());
            json.put("startTime", workout.getStartTime());
            json.put("endTime", workout.getEndTime());
            json.put("totalCalories", workout.getTotalCalories());
            json.put("notes", workout.getNotes());
            json.put("programId", workout.getProgramId());
            json.put("programDayNumber", workout.getProgramDayNumber());
            json.put("programDayId", workout.getProgramDayId());
            
            
            JSONArray exercisesArray = new JSONArray();
            for (WorkoutExercise exercise : workout.getExercises()) {
                JSONObject exerciseJson = new JSONObject();
                exerciseJson.put("id", exercise.getId());
                exerciseJson.put("orderNumber", exercise.getOrderNumber());
                
                
                if (exercise.getExercise() != null) {
                    JSONObject baseExerciseJson = new JSONObject();
                    baseExerciseJson.put("id", exercise.getExercise().getId());
                    baseExerciseJson.put("name", exercise.getExercise().getName());
                    baseExerciseJson.put("description", exercise.getExercise().getDescription());
                    exerciseJson.put("exercise", baseExerciseJson);
                }
                
                
                JSONArray setsArray = new JSONArray();
                if (exercise.getSetsCompleted() != null) {
                    for (ExerciseSet set : exercise.getSetsCompleted()) {
                        JSONObject setJson = new JSONObject();
                        setJson.put("id", set.getId());
                        setJson.put("setNumber", set.getSetNumber());
                        setJson.put("weight", set.getWeight());
                        setJson.put("reps", set.getReps());
                        setJson.put("completed", set.isCompleted());
                        setsArray.put(setJson);
                    }
                }
                exerciseJson.put("sets", setsArray);
                
                exercisesArray.put(exerciseJson);
            }
            json.put("exercises", exercisesArray);
            
            return json;
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при сериализации тренировки: " + e.getMessage());
            return new JSONObject();
        }
    }
    
    
    private UserWorkout parseWorkoutFromJson(JSONObject json) {
        try {
            String id = json.getString("id");
            String userId = json.getString("userId");
            String name = json.getString("name");
            long startTime = json.getLong("startTime");
            Long endTime = json.has("endTime") && !json.isNull("endTime") ? json.getLong("endTime") : null;
            int totalCalories = json.optInt("totalCalories", 0);
            String notes = json.optString("notes", "");
            String programId = json.has("programId") && !json.isNull("programId") ? json.getString("programId") : null;
            Integer programDayNumber = json.has("programDayNumber") && !json.isNull("programDayNumber") ? 
                json.getInt("programDayNumber") : null;
            String programDayId = json.has("programDayId") && !json.isNull("programDayId") ? 
                json.getString("programDayId") : null;
            String planId = json.has("planId") && !json.isNull("planId") ? json.getString("planId") : null;
                
            UserWorkout workout = new UserWorkout(id, userId, name, startTime, endTime, totalCalories, 
                notes, programId, programDayNumber != null ? programDayNumber : 0, programDayId, planId, new ArrayList<>());
            
            
            if (json.has("exercises")) {
                JSONArray exercisesArray = json.getJSONArray("exercises");
                for (int i = 0; i < exercisesArray.length(); i++) {
                    JSONObject exerciseJson = exercisesArray.getJSONObject(i);
                    WorkoutExercise exercise = new WorkoutExercise();
                    exercise.setId(exerciseJson.getString("id"));
                    exercise.setOrderNumber(exerciseJson.getInt("orderNumber"));
                    
                    
                    if (exerciseJson.has("exercise")) {
                        JSONObject baseExerciseJson = exerciseJson.getJSONObject("exercise");
                        Exercise baseExercise = new Exercise.Builder()
                            .id(baseExerciseJson.getString("id"))
                            .name(baseExerciseJson.getString("name"))
                            .description(baseExerciseJson.optString("description", ""))
                            .build();
                        exercise.setExercise(baseExercise);
                    }
                    
                    
                    List<ExerciseSet> sets = new ArrayList<>();
                    if (exerciseJson.has("sets")) {
                        JSONArray setsArray = exerciseJson.getJSONArray("sets");
                        for (int j = 0; j < setsArray.length(); j++) {
                            JSONObject setJson = setsArray.getJSONObject(j);
                            ExerciseSet set = new ExerciseSet();
                            set.setId(setJson.getString("id"));
                            set.setSetNumber(setJson.getInt("setNumber"));
                            set.setWeight(setJson.has("weight") && !setJson.isNull("weight") ? 
                                (float)setJson.getDouble("weight") : null);
                            set.setReps(setJson.has("reps") && !setJson.isNull("reps") ? 
                                setJson.getInt("reps") : null);
                            set.setCompleted(setJson.getBoolean("completed"));
                            sets.add(set);
                        }
                    }
                    exercise.setSetsCompleted(sets);
                    
                    workout.addExercise(exercise);
                }
            }
            
            return workout;
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при десериализации тренировки: " + e.getMessage());
            return null;
        }
    }


    
    public LiveData<List<UserWorkout>> getAllWorkouts() {
        return allWorkouts;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    

    
    public Map<Long, UserWorkout> getWorkoutsByDate() {
        return Collections.unmodifiableMap(workoutsByDate);
    }
    
    public Map<Long, WorkoutPlan> getPlansByDate() {
        return Collections.unmodifiableMap(plansByDate);
    }

    private void loadRecentWorkouts() {
        String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
        if (userId == null) return;

        isLoading.setValue(true);
        
        executor.execute(() -> {
            try {
                List<UserWorkout> workouts = workoutRepository.getRecentWorkouts(userId, RECENT_WORKOUTS_LIMIT);
                recentWorkouts.postValue(workouts);
                
                
                if (totalWorkouts.getValue() == 0 && !workouts.isEmpty()) {
                    totalWorkouts.postValue(workouts.size());
                }
                
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки последних тренировок", e);
                errorMessage.postValue("Не удалось загрузить последние тренировки: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    

    

    
    public void loadWorkoutPlans() {
        String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
        if (userId == null) return;
        
        executor.execute(() -> {
            try {
                List<WorkoutPlan> plans = workoutRepository.getWorkoutPlansByDateRange(userId, startDate, endDate);
                
                
                plansByDate.clear();
                for (WorkoutPlan plan : plans) {
                    long normalizedDate = normalizeDate(plan.getPlannedDate());
                    plansByDate.put(normalizedDate, plan);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки планов тренировок", e);
                
            }
        });
    }
    

    
    
    public void loadAllMonthWorkoutsFromRoom() {
        String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
        if (userId == null) return;
        
        isLoading.postValue(true);
        
        executor.execute(() -> {
            try {
                
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_MONTH, 1); 
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long monthStartDate = calendar.getTimeInMillis();
                
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.MILLISECOND, -1); 
                long monthEndDate = calendar.getTimeInMillis();
                
                
                AppDatabase db = AppDatabase.getInstance(getApplication().getApplicationContext());
                WorkoutDao dao = db.workoutDao();
                
                
                List<UserWorkoutEntity> localWorkouts = dao.getAllWorkoutsByTimeRange(userId, monthStartDate, monthEndDate);
                
                if (localWorkouts != null && !localWorkouts.isEmpty()) {
                    List<UserWorkout> workouts = new ArrayList<>();
                    
                    
                    for (UserWorkoutEntity entity : localWorkouts) {
                        UserWorkout workout = entity.toModel(Collections.emptyList());
                        workouts.add(workout);
                    }
                    
                    
                    for (UserWorkout workout : workouts) {
                        try {
                            List<WorkoutExerciseEntity> exerciseEntities = dao.getExercisesForWorkout(workout.getId());
                            List<WorkoutExercise> exercises = new ArrayList<>();
                            
                            for (WorkoutExerciseEntity exerciseEntity : exerciseEntities) {
                                
                                com.martist.vitamove.db.entity.ExerciseEntity exerciseEntityObj = 
                                    db.exerciseDao().getExerciseById(exerciseEntity.getBaseExerciseId());
                                
                                if (exerciseEntityObj != null) {
                                    
                                    Exercise baseExercise = convertEntityToExercise(exerciseEntityObj);
                                    
                                    
                                    List<ExerciseSetEntity> setEntities = dao.getSetsForExercise(exerciseEntity.getId());
                                    List<ExerciseSet> sets = new ArrayList<>();
                                    
                                    for (ExerciseSetEntity setEntity : setEntities) {
                                        sets.add(setEntity.toModel());
                                    }
                                    
                                    
                                    WorkoutExercise exercise = exerciseEntity.toModel(baseExercise, sets);
                                    exercises.add(exercise);
                                }
                            }
                            
                            
                            workout.setExercises(exercises);
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при загрузке упражнений для тренировки " + workout.getId(), e);
                        }
                    }
                    
                    
                    
                    
                    workoutsByDate.clear();
                    for (UserWorkout workout : workouts) {
                        long normalizedDate = normalizeDate(workout.getStartTime());
                        workoutsByDate.put(normalizedDate, workout);
                    }
                    
                    
                    
                    
                    
                    
                    allWorkouts.postValue(workouts);
                    
                    
                    totalWorkouts.postValue(workouts.size());
                    
                } else {
                    
                    
                    workoutsByDate.clear();
                    
                    allWorkouts.postValue(new ArrayList<>());
                }
                
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке тренировок из Room", e);
                errorMessage.postValue("Не удалось загрузить историю тренировок: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    
    public void initializeLiveDataObservers() {
        try {
            
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.e(TAG, "initializeLiveDataObservers: вызван не из главного потока!");
                
                errorMessage.postValue("Ошибка инициализации наблюдателей LiveData: неправильный поток");
                isLoading.postValue(false);
                return;
            }
            
            String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
            if (userId == null) {
                Log.e(TAG, "LiveData: Пользователь не авторизован");
                errorMessage.postValue("Пользователь не авторизован");
                isLoading.postValue(false);
                return;
            }
            
            isLoading.setValue(true);
            
            
            Calendar calendar = Calendar.getInstance();
            
            
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long monthStartDate = calendar.getTimeInMillis();
            
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.MILLISECOND, -1); 
            long monthEndDate = calendar.getTimeInMillis();
            
            
            AppDatabase db = AppDatabase.getInstance(getApplication().getApplicationContext());
            WorkoutDao dao = db.workoutDao();
            
            
            androidx.lifecycle.LiveData<List<UserWorkoutEntity>> workoutsLiveData = 
                dao.getAllWorkoutsByTimeRangeLiveData(userId, monthStartDate, monthEndDate);
            
            
            workoutsLiveData.observeForever(workoutEntities -> {
                if (workoutEntities == null || workoutEntities.isEmpty()) {
                    
                    workoutsByDate.clear();
                    allWorkouts.postValue(new ArrayList<>());
                    isLoading.postValue(false);
                    return;
                }
                
                
                
                
                executor.execute(() -> {
                    try {
                        List<UserWorkout> workouts = new ArrayList<>();
                        Map<Long, UserWorkout> tempWorkoutsByDate = new HashMap<>();
                        
                        for (UserWorkoutEntity entity : workoutEntities) {
                            
                            UserWorkout workout = entity.toModel(Collections.emptyList());
                            
                            
                            List<WorkoutExerciseEntity> exerciseEntities = dao.getExercisesForWorkout(workout.getId());
                            
                            if (exerciseEntities != null && !exerciseEntities.isEmpty()) {
                                List<WorkoutExercise> exercises = new ArrayList<>();
                                
                                for (WorkoutExerciseEntity exerciseEntity : exerciseEntities) {
                                    
                                    com.martist.vitamove.db.entity.ExerciseEntity exerciseEntityObj = 
                                        db.exerciseDao().getExerciseById(exerciseEntity.getBaseExerciseId());
                                    
                                    if (exerciseEntityObj != null) {
                                        
                                        Exercise baseExercise = convertEntityToExercise(exerciseEntityObj);
                                        
                                        
                                        List<ExerciseSetEntity> setEntities = dao.getSetsForExercise(exerciseEntity.getId());
                                        List<ExerciseSet> sets = new ArrayList<>();
                                        
                                        for (ExerciseSetEntity setEntity : setEntities) {
                                            sets.add(setEntity.toModel());
                                        }
                                        
                                        
                                        WorkoutExercise exercise = exerciseEntity.toModel(baseExercise, sets);
                                        exercises.add(exercise);
                                    }
                                }
                                
                                
                                workout.setExercises(exercises);
                            }
                            
                            workouts.add(workout);
                            tempWorkoutsByDate.put(normalizeDate(workout.getStartTime()), workout);
                        }
                        
                        
                        workoutsByDate.clear();
                        workoutsByDate.putAll(tempWorkoutsByDate);
                        allWorkouts.postValue(workouts);
                        totalWorkouts.postValue(workouts.size());
                        isLoading.postValue(false);
                        
                        
                    } catch (Exception e) {
                        Log.e(TAG, "LiveData: Ошибка при обработке данных наблюдателя", e);
                        errorMessage.postValue("Ошибка при обработке данных: " + e.getMessage());
                        isLoading.postValue(false);
                    }
                });
            });
            
            
        } catch (Exception e) {
            Log.e(TAG, "LiveData: Ошибка при инициализации наблюдателя", e);
            isLoading.postValue(false);
        }
    }
    
    
    public void refreshWorkoutHistory() {
        String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
        if (userId == null) return;
        
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                
                loadAllMonthWorkoutsFromRoom();
                
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    initializeLiveDataObservers();
                });
                
                
                loadWorkoutPlans();
                
                isLoading.postValue(false);
                
                
            } catch (Exception e) {
                errorMessage.postValue("Ошибка обновления: " + e.getMessage());
                isLoading.postValue(false);
                Log.e(TAG, "Ошибка при обновлении данных о тренировках: " + e.getMessage(), e);
            }
        });
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
            .build();
    }
    
    
    @Deprecated
    public void loadNextPage() {
        
        
    }
    
    
    private String getCacheKey(int page) {
        return KEY_PREFIX_CACHE + page;
    }
    
    
    private long normalizeDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    


    @Override
    protected void onCleared() {
        super.onCleared();
        
        saveCacheData();
    }

    
    public static void clearCache(Application application, String userId) {
        if (userId != null && !userId.isEmpty()) {
            try {
                String cacheFileName = PREF_NAME + "_" + userId;
                SharedPreferences prefs = application.getSharedPreferences(cacheFileName, Application.MODE_PRIVATE);
                prefs.edit().clear().apply();
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при очистке кэша истории: " + e.getMessage());
            }
        }
    }
} 