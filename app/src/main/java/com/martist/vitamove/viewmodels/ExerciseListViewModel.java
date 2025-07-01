package com.martist.vitamove.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.martist.vitamove.db.AppDatabase;
import com.martist.vitamove.db.dao.ExerciseDao;
import com.martist.vitamove.db.entity.ExerciseEntity;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ExerciseListViewModel extends AndroidViewModel {
    private static final String TAG = "ExerciseListViewModel";
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private static final long CACHE_EXPIRATION_TIME = 24 * 60 * 60 * 1000; 
    
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final SupabaseWorkoutRepository workoutRepository;
    private final ExerciseDao exerciseDao;
    
    
    private final MutableLiveData<List<Exercise>> exercises = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final Map<String, Exercise> exerciseCache = new HashMap<>();
    
    private final MutableLiveData<Map<String, Exercise>> cachedExercisesMap = new MutableLiveData<>(new HashMap<>());
    
    
    private final SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ExerciseListCache";
    
    
    private final MutableLiveData<Boolean> initialCachingComplete = new MutableLiveData<>(false);

    public ExerciseListViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        
        exerciseDao = AppDatabase.getInstance(application).exerciseDao();
        
        
        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );
        workoutRepository = new SupabaseWorkoutRepository(supabaseClient);
        
        
        loadExercisesFromCache();
    }

    
    public void loadExercises() {
        isLoading.postValue(true);
        
        
        
        if (!exerciseCache.isEmpty()) {
            List<Exercise> cachedList = new ArrayList<>(exerciseCache.values());
            exercises.postValue(cachedList);
            isLoading.postValue(false);
            
            
            
            long lastUpdateTime = sharedPreferences.getLong(KEY_LAST_UPDATE, 0);
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastUpdateTime > CACHE_EXPIRATION_TIME) {
                
                
                refreshExercisesFromServer();
            }
        } else {
            
            executor.execute(() -> {
                try {
                    
                    List<ExerciseEntity> exerciseEntities = exerciseDao.getAllExercises();
                    
                    if (exerciseEntities != null && !exerciseEntities.isEmpty()) {
                        
                        List<Exercise> cachedExercises = new ArrayList<>();
                        exerciseCache.clear();
                        
                        for (ExerciseEntity entity : exerciseEntities) {
                            Exercise exercise = entity.toExercise();
                            exerciseCache.put(exercise.getId(), exercise);
                            cachedExercises.add(exercise);
                        }
                        
                        
                        exercises.postValue(cachedExercises);
                        
                        cachedExercisesMap.postValue(new HashMap<>(exerciseCache));
                        
                        
                        
                        
                        long lastUpdateTime = sharedPreferences.getLong(KEY_LAST_UPDATE, 0);
                        long currentTime = System.currentTimeMillis();
                        
                        if (currentTime - lastUpdateTime > CACHE_EXPIRATION_TIME) {
                            
                            
                            refreshExercisesFromServer();
                        }
                    } else {
                        
                        
                        refreshExercisesFromServer();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при чтении из ROOM DATABASE: " + e.getMessage(), e);
                    refreshExercisesFromServer();
                } finally {
                    isLoading.postValue(false);
                }
            });
        }
    }
    
    
    public void refreshExercisesFromServer() {
        isLoading.postValue(true);
        
        executor.execute(() -> {
            try {
                
                long startTime = System.currentTimeMillis();
                List<Exercise> loadedExercises = workoutRepository.getAllExercises();
                long endTime = System.currentTimeMillis();
                
                if (loadedExercises != null && !loadedExercises.isEmpty()) {
                    
                    exerciseCache.clear();
                    for (Exercise exercise : loadedExercises) {
                        exerciseCache.put(exercise.getId(), exercise);
                    }
                    
                    
                    exercises.postValue(loadedExercises);
                    
                    cachedExercisesMap.postValue(new HashMap<>(exerciseCache));
                    
                    
                    saveExercisesToCache(loadedExercises);
                    
                    
                } else {
                    Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Упражнения не получены с СЕРВЕРА или список пуст");
                    errorMessage.postValue("Не удалось загрузить упражнения с сервера");
                }
                
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке с СЕРВЕРА: " + e.getMessage(), e);
                errorMessage.postValue("Ошибка при загрузке упражнений: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    
    private void loadExercisesFromCache() {
        executor.execute(() -> {
            try {
                
                List<ExerciseEntity> entities = exerciseDao.getAllExercises();
                
                if (entities != null && !entities.isEmpty()) {
                    
                    List<Exercise> cachedExercises = new ArrayList<>();
                    exerciseCache.clear();
                    
                    for (ExerciseEntity entity : entities) {
                        Exercise exercise = entity.toExercise();
                        exerciseCache.put(exercise.getId(), exercise);
                        cachedExercises.add(exercise);
                    }
                    
                    
                    exercises.postValue(cachedExercises);
                    
                    cachedExercisesMap.postValue(new HashMap<>(exerciseCache));
                    
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке упражнений из ROOM DATABASE: " + e.getMessage(), e);
            }
        });
    }
    
    
    private void saveExercisesToCache(List<Exercise> exerciseList) {
        executor.execute(() -> {
            try {
                
                long startTime = System.currentTimeMillis();
                
                
                List<ExerciseEntity> entities = new ArrayList<>();
                for (Exercise exercise : exerciseList) {
                    entities.add(new ExerciseEntity(exercise));
                }
                
                
                exerciseDao.updateExercises(entities);
                
                
                sharedPreferences.edit()
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply();
                
                long endTime = System.currentTimeMillis();
                
            } catch (Exception e) {
                Log.e(TAG, "КЭШИРОВАНИЕ: Ошибка при сохранении упражнений в ROOM DATABASE: " + e.getMessage(), e);
            }
        });
    }
    
    
    public Exercise getExerciseById(String exerciseId) {
        if (exerciseId == null) return null;
        
        Exercise cachedExercise = exerciseCache.get(exerciseId);
        if (cachedExercise != null) {
             
             return cachedExercise;
        }

        
        
        
        
        return null;
    }

    
    
    public void preloadCacheWithPriority() {
        
        
        
        if (!exerciseCache.isEmpty()) {
            
            return;
        }
        
        
        Thread cacheThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
            
            try {
                long startTime = System.currentTimeMillis();
                
                
                List<ExerciseEntity> entities = exerciseDao.getAllExercises();
                
                if (entities != null && !entities.isEmpty()) {
                    
                    List<Exercise> cachedExercises = new ArrayList<>();
                    
                    synchronized (exerciseCache) {
                        exerciseCache.clear();
                        for (ExerciseEntity entity : entities) {
                            Exercise exercise = entity.toExercise();
                            exerciseCache.put(exercise.getId(), exercise);
                            cachedExercises.add(exercise);
                        }
                    }
                    
                    
                    exercises.postValue(cachedExercises);
                    
                    cachedExercisesMap.postValue(new HashMap<>(exerciseCache));
                    
                    long endTime = System.currentTimeMillis();
                    
                    
                    
                    checkCacheInBackground();
                } else {
                    
                    refreshExercisesFromServer();
                }
            } catch (Exception e) {
                Log.e(TAG, "КЭШИРОВАНИЕ: Ошибка при быстром прогреве кэша: " + e.getMessage(), e);
                refreshExercisesFromServer();
            }
        });
        
        
        cacheThread.setPriority(Thread.MAX_PRIORITY);
        cacheThread.start();
    }
    
    
    private void checkCacheInBackground() {
        executor.execute(() -> {
            
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            
            try {
                
                long lastUpdateTime = sharedPreferences.getLong(KEY_LAST_UPDATE, 0);
                long currentTime = System.currentTimeMillis();
                
                
                if (currentTime - lastUpdateTime > CACHE_EXPIRATION_TIME) {
                    
                    refreshExercisesFromServer();
                } else {
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "КЭШИРОВАНИЕ: Ошибка при проверке актуальности кэша: " + e.getMessage(), e);
            }
        });
    }
    
    
    public LiveData<Boolean> cacheExercisesAfterLogin() {
        
        initialCachingComplete.postValue(false);
        
        executor.execute(() -> {
            try {
                
                int exerciseCount = exerciseDao.getExerciseCount();
                
                if (exerciseCount > 0) {
                    
                    
                    
                    long lastUpdateTime = sharedPreferences.getLong(KEY_LAST_UPDATE, 0);
                    long currentTime = System.currentTimeMillis();
                    
                    if (currentTime - lastUpdateTime > CACHE_EXPIRATION_TIME) {
                        
                        loadExercisesFromServerAndCache();
                    } else {
                        
                        loadExercisesFromCache();
                        initialCachingComplete.postValue(true);
                    }
                } else {
                    
                    loadExercisesFromServerAndCache();
                }
            } catch (Exception e) {
                Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при кэшировании после входа: " + e.getMessage(), e);
                initialCachingComplete.postValue(true); 
            }
        });
        
        return initialCachingComplete;
    }
    
    
    private void loadExercisesFromServerAndCache() {
        try {
            
            List<Exercise> loadedExercises = workoutRepository.getAllExercises();
            
            if (loadedExercises != null && !loadedExercises.isEmpty()) {
                
                
                
                exerciseCache.clear();
                for (Exercise exercise : loadedExercises) {
                    exerciseCache.put(exercise.getId(), exercise);
                }
                
                
                exercises.postValue(loadedExercises);
                cachedExercisesMap.postValue(new HashMap<>(exerciseCache));
                
                
                List<ExerciseEntity> entities = new ArrayList<>();
                for (Exercise exercise : loadedExercises) {
                    entities.add(new ExerciseEntity(exercise));
                }
                
                exerciseDao.updateExercises(entities);
                sharedPreferences.edit()
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply();
                
                
            } else {
                Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Не удалось загрузить упражнения с сервера (пустой список)");
            }
        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке упражнений с сервера: " + e.getMessage(), e);
        } finally {
            initialCachingComplete.postValue(true);
        }
    }
    

    
    
    public LiveData<List<Exercise>> getExercises() {
        return exercises;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    

} 