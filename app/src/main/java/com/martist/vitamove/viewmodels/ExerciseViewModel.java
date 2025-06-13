package com.martist.vitamove.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.WorkoutExercise;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ExerciseViewModel extends AndroidViewModel {
    private static final String TAG = "ExerciseViewModel";
    private static final String PREFS_NAME = "ExerciseCache";
    private static final String KEY_EXERCISE_SETS = "exercise_sets_";
    private static final String KEY_WORKOUT_EXERCISE = "workout_exercise_";
    
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new GsonBuilder().create();
    private SupabaseWorkoutRepository workoutRepository;
    
    
    private final MutableLiveData<List<ExerciseSet>> exerciseSets = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<WorkoutExercise> workoutExercise = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isResting = new MutableLiveData<>(false);
    private final MutableLiveData<Long> restTimeRemaining = new MutableLiveData<>(0L);
    
    
    private Exercise exercise;
    private String workoutId;
    private String exerciseId;
    private CountDownTimer restTimer;
    
    
    private SharedPreferences sharedPreferences;

    public ExerciseViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        
        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );
        workoutRepository = new SupabaseWorkoutRepository(supabaseClient);
    }

    
    public void initialize(Exercise exercise, WorkoutExercise workoutExercise, String workoutId) {
        this.exercise = exercise;
        this.workoutId = workoutId;
        this.exerciseId = workoutExercise.getId();
        this.workoutExercise.setValue(workoutExercise);
        
        
        loadExerciseSets();
    }

    
    private <T> void safeSetValue(MutableLiveData<T> liveData, T value) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
    
    
    private void loadExerciseSets() {
        safeSetValue(isLoading, true);
        
        
        
        List<ExerciseSet> cachedSets = loadSetsFromCache();
        
        if (cachedSets != null && !cachedSets.isEmpty()) {
            
            
            safeSetValue(exerciseSets, cachedSets);
            
            
            WorkoutExercise currentExercise = workoutExercise.getValue();
            if (currentExercise != null) {
                currentExercise.getSetsCompleted().clear();
                currentExercise.getSetsCompleted().addAll(cachedSets);
                safeSetValue(workoutExercise, currentExercise);
            }
            
            safeSetValue(isLoading, false);
        } else {
            
            
            executor.execute(() -> {
                try {
                    
                    long startTime = System.currentTimeMillis();
                    
                    
                    List<ExerciseSet> setsFromDb = workoutRepository.getExerciseSets(exerciseId);
                    
                    long endTime = System.currentTimeMillis();
                    
                    if (setsFromDb != null && !setsFromDb.isEmpty()) {
                        
                        
                        exerciseSets.postValue(setsFromDb);
                        
                        
                        WorkoutExercise currentExercise = workoutExercise.getValue();
                        if (currentExercise != null) {
                            currentExercise.getSetsCompleted().clear();
                            currentExercise.getSetsCompleted().addAll(setsFromDb);
                            workoutExercise.postValue(currentExercise);
                        }
                        
                        
                        saveToCache(setsFromDb);
                    } else {
                        
                        
                        List<ExerciseSet> defaultSets = createDefaultSets(workoutId, exercise.getId(), exercise.getExerciseType(), exercise.getDefaultSets());
                        
                        
                        safeSetValue(exerciseSets, defaultSets);
                        
                        
                        
                        WorkoutExercise currentExercise = workoutExercise.getValue();
                        if (currentExercise != null) {
                            currentExercise.getSetsCompleted().clear();
                            currentExercise.getSetsCompleted().addAll(defaultSets);
                            safeSetValue(workoutExercise, currentExercise);
                        }
                        
                        
                        saveToCache(defaultSets);
                        
                        WorkoutExercise currentWorkoutExercise = workoutExercise.getValue();
                        if (currentWorkoutExercise != null && currentWorkoutExercise.getId() != null) {
                            saveDefaultSetsToDB(defaultSets, currentWorkoutExercise.getId()); 
                        } else {
                             Log.e(TAG, "loadExerciseSets: Невозможно сохранить стандартные подходы, т.к. workoutExercise или его ID не найдены");
                             errorMessage.postValue("Ошибка: не удалось инициализировать упражнение.");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке подходов из БАЗЫ ДАННЫХ: " + e.getMessage(), e);
                    errorMessage.postValue("Ошибка загрузки подходов: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            });
        }
    }

    
    private List<ExerciseSet> createDefaultSets(String workoutExerciseId, String baseExerciseId, String type, int numSets) {
        List<ExerciseSet> newSets = new ArrayList<>();
        
        
        boolean isSpecialExerciseType = (type != null && 
                                     (type.equals("восстановительное") || 
                                      type.equals("реабилитационное") || 
                                      type.equals("функциональное")));
        
        
        int setsToCreate = isCardioExercise() ? 1 : numSets;
        
        
        for (int i = 0; i < setsToCreate; i++) {
            ExerciseSet set = new ExerciseSet();
            set.setWorkoutExerciseId(workoutExerciseId);
            set.setExerciseId(baseExerciseId); 
            set.setSetNumber(i + 1);
            
            
            if (exercise != null) {
                String defaultReps = exercise.getDefaultReps();
                
                if (isCardioExercise()) {
                    set.setReps(null); 
                } else {
                    try {
                        
                        Integer lastReps = getLastRepsForExercise(baseExerciseId);
                        if (lastReps != null && lastReps > 0) {
                            
                            set.setReps(lastReps);
                            
                        } else if (defaultReps != null && !defaultReps.isEmpty()) {
                            
                            try {
                                int reps = Integer.parseInt(defaultReps.split("-")[0].trim());
                                set.setReps(reps);
                                
                            } catch (NumberFormatException e) {
                                
                                set.setReps(12);
                                
                            }
                        } else {
                            
                            set.setReps(12);
                            
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "createDefaultSets: Ошибка при установке повторений: " + e.getMessage(), e);
                        set.setReps(12); 
                    }
                }
                
                
                if (isCardioExercise()) {
                    set.setWeight(null); 
                } else if (isSpecialExerciseType) {
                    set.setWeight(null);
                } else {
                    
                    Float lastWeight = getLastWeightForExercise(baseExerciseId);
                    if (lastWeight != null && lastWeight > 0) {
                        
                        set.setWeight(lastWeight);
                        
                    } else {
                        
                        set.setWeight(0.0f);
                    }
                }
            }
            
            set.setCompleted(false);
            newSets.add(set);
        }
        
        
        boolean allHaveExerciseId = true;
        for (int i = 0; i < newSets.size(); i++) {
            ExerciseSet set = newSets.get(i);
            if (set.getExerciseId() == null || set.getExerciseId().isEmpty()) {
                allHaveExerciseId = false;
                Log.e(TAG, "createDefaultSets: Подход #" + (i+1) + " НЕ имеет exercise_id!");
            } else {
                
            }
        }
        
        if (!allHaveExerciseId) {
            Log.e(TAG, "createDefaultSets: ВНИМАНИЕ! Не все подходы имеют exercise_id!");
        }
        
        return newSets;
    }

    
    private List<ExerciseSet> loadSetsFromCache() {
        try {
            
            String key = KEY_EXERCISE_SETS + exerciseId;
            String setsJson = sharedPreferences.getString(key, null);
            
            if (setsJson != null && !setsJson.isEmpty()) {
                
                Type listType = new TypeToken<List<ExerciseSet>>(){}.getType();
                List<ExerciseSet> cachedSets = gson.fromJson(setsJson, listType);
                
                if (cachedSets != null && !cachedSets.isEmpty()) {
                    
                    return cachedSets;
                } else {
                    
                }
            } else {
                
            }
        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке подходов из КЭША: " + e.getMessage(), e);
        }
        return null;
    }

    
    private void saveToCache(List<ExerciseSet> sets) {
        if (sets == null) return;
        
        try {
            
            long startTime = System.currentTimeMillis();
            
            String key = KEY_EXERCISE_SETS + exerciseId;
            String setsJson = gson.toJson(sets);
            
            sharedPreferences.edit()
                .putString(key, setsJson)
                .apply();
                
            long endTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при сохранении подходов в КЭШ: " + e.getMessage(), e);
        }
    }

    
    public void updateSet(ExerciseSet updatedSet) {
        if (updatedSet == null) {
            Log.e(TAG, "updateSet: updatedSet равен null");
            return;
        }
        
        
        String setId = updatedSet.getId();
        if (setId == null || setId.isEmpty()) {
            Log.e(TAG, "updateSet: ID подхода равен null или пустой");
            return;
        }
        
        
        boolean isTempId = setId.startsWith("temp_");
        if (isTempId) {
            
            
            
            List<ExerciseSet> currentSets = exerciseSets.getValue();
            if (currentSets != null) {
                List<ExerciseSet> updatedSets = new ArrayList<>(currentSets);
                for (int i = 0; i < updatedSets.size(); i++) {
                    if (setId.equals(updatedSets.get(i).getId())) {
                        updatedSets.set(i, updatedSet);
                        break;
                    }
                }
                
                
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    exerciseSets.setValue(updatedSets);
                } else {
                    exerciseSets.postValue(updatedSets);
                }
                
                
                updateWorkoutExercise(updatedSets);
                
                
                saveToCache(updatedSets);
            }
            
            return; 
        }
        
        
        executor.execute(() -> {
            try {
                workoutRepository.updateSet(setId, updatedSet);
                
                
                List<ExerciseSet> currentSets = exerciseSets.getValue();
                if (currentSets != null) {
                    List<ExerciseSet> updatedSets = new ArrayList<>(currentSets);
                    boolean found = false;
                    
                    for (int i = 0; i < updatedSets.size(); i++) {
                        if (setId.equals(updatedSets.get(i).getId())) {
                            updatedSets.set(i, updatedSet);
                            found = true;
                            break;
                        }
                    }
                    
                    
                    if (!found) {
                        updatedSets.add(updatedSet);
                    }
                    
                    
                    exerciseSets.postValue(updatedSets);
                    
                    
                    updateWorkoutExercise(updatedSets);
                    
                    
                    saveToCache(updatedSets);
                }
                
                
            } catch (Exception e) {
                Log.e(TAG, "updateSet: Ошибка при обновлении подхода: " + e.getMessage(), e);
                
                
                if (e.getMessage() != null && (
                    e.getMessage().contains("row-level security policy") || 
                    e.getMessage().contains("42501") || 
                    e.getMessage().contains("403"))) {
                    errorMessage.postValue("Ошибка доступа: нет прав на обновление подходов. Пожалуйста, войдите снова в аккаунт.");
                } else {
                    errorMessage.postValue("Ошибка при обновлении подхода: " + e.getMessage());
                }
            }
        });
    }
    
    
    private void updateWorkoutExercise(List<ExerciseSet> updatedSets) {
        WorkoutExercise currentExercise = workoutExercise.getValue();
        if (currentExercise != null) {
            try {
                
                WorkoutExercise updatedExercise = new WorkoutExercise(
                    currentExercise.getId(),
                    currentExercise.getExercise(),
                    currentExercise.getOrderNumber(),
                    new ArrayList<>(updatedSets),
                    currentExercise.getNotes()
                );
                
                
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    
                    workoutExercise.setValue(updatedExercise);
                } else {
                    
                    workoutExercise.postValue(updatedExercise);
                }
                
                
            } catch (Exception e) {
                Log.e(TAG, "updateWorkoutExercise: Ошибка при обновлении WorkoutExercise: " + e.getMessage(), e);
            }
        }
    }

    
    public void deleteSet(String setId) {
        List<ExerciseSet> currentSets = exerciseSets.getValue();
        if (currentSets == null) return;
        
        
        int position = -1;
        for (int i = 0; i < currentSets.size(); i++) {
            if (currentSets.get(i).getId() != null && currentSets.get(i).getId().equals(setId)) {
                position = i;
                break;
            }
        }
        
        if (position >= 0) {
            currentSets.remove(position);
            
            
            for (int i = position; i < currentSets.size(); i++) {
                currentSets.get(i).setSetNumber(i + 1);
            }
            
            
            safeSetValue(exerciseSets, currentSets);
            
            
            WorkoutExercise currentExercise = workoutExercise.getValue();
            if (currentExercise != null) {
                currentExercise.getSetsCompleted().clear();
                currentExercise.getSetsCompleted().addAll(currentSets);
                safeSetValue(workoutExercise, currentExercise);
            }
            
            
            saveToCache(currentSets);
            
            
            
            if (setId != null && setId.startsWith("temp_")) {
                
                return;
            }
            
            
            final int finalPosition = position; 
            executor.execute(() -> {
                try {
                    workoutRepository.deleteSet(setId);
                    
                    
                    
                    for (int i = finalPosition; i < currentSets.size(); i++) {
                        
                        String currentId = currentSets.get(i).getId();
                        if (currentId != null && !currentId.startsWith("temp_")) {
                            workoutRepository.updateSet(currentId, currentSets.get(i));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при удалении подхода из БД: " + e.getMessage(), e);
                    errorMessage.postValue("Ошибка при удалении подхода: " + e.getMessage());
                }
            });
        }
    }



    
    public void startRest(long restTime) {
        safeSetValue(isResting, true);
        safeSetValue(restTimeRemaining, restTime);
        
        if (restTimer != null) {
            restTimer.cancel();
        }
        
        restTimer = new CountDownTimer(restTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                restTimeRemaining.postValue(millisUntilFinished);
            }
            
            @Override
            public void onFinish() {
                endRest();
            }
        };
        
        restTimer.start();
    }

    
    public void endRest() {
        if (restTimer != null) {
            restTimer.cancel();
        }
        
        safeSetValue(isResting, false);
        safeSetValue(restTimeRemaining, 0L);
    }



    
    private void saveDefaultSetsToDB(List<ExerciseSet> defaultSets, String workoutExerciseId) {
        if (defaultSets == null || defaultSets.isEmpty() || workoutExerciseId == null || workoutExerciseId.isEmpty()) {
            Log.e(TAG, "saveDefaultSetsToDB: Некорректные аргументы (пустые сеты или workoutExerciseId)");
            return;
        } 
        
        
        
        
        for (int i = 0; i < defaultSets.size(); i++) {
            ExerciseSet set = defaultSets.get(i);
            
        }
        
        
        List<ExerciseSet> savedSetsWithId = new ArrayList<>();
        boolean allSavedSuccessfully = true;
        
        try {
            
            for (ExerciseSet set : defaultSets) {
                try {
                    
                    
                    
                    String setId = workoutRepository.addSet(workoutExerciseId, set);
                    if (setId != null && !setId.isEmpty()) {
                        
                        set.setId(setId);
                        savedSetsWithId.add(set); 
                        
                    } else {
                        
                        allSavedSuccessfully = false;
                        Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Не удалось сохранить подход в БАЗЕ ДАННЫХ (setNumber: " + set.getSetNumber() + "), получен null или пустой ID");
                        
                        
                        errorMessage.postValue("Ошибка доступа к базе данных при сохранении подхода. Проверьте права доступа.");
                    }
                } catch (Exception e) {
                    
                    allSavedSuccessfully = false;
                    Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при сохранении подхода (setNumber: " + set.getSetNumber() + "): " + e.getMessage(), e);
                    
                    
                    if (e.getMessage() != null && (
                        e.getMessage().contains("row-level security policy") || 
                        e.getMessage().contains("42501") || 
                        e.getMessage().contains("403"))) {
                        errorMessage.postValue("Ошибка доступа: нет прав на добавление подходов. Пожалуйста, войдите снова в аккаунт.");
                    } else {
                        errorMessage.postValue("Ошибка сохранения подхода: " + e.getMessage());
                    }
                }
            }
            
            if (allSavedSuccessfully) {
                 
            } else {
                 
                 
                 
                 if (savedSetsWithId.isEmpty()) {
                     for (int i = 0; i < defaultSets.size(); i++) {
                         ExerciseSet set = defaultSets.get(i);
                         
                         set.setId("temp_" + UUID.randomUUID().toString());
                         savedSetsWithId.add(set);
                         
                     }
                     
                 }
            }

            
            exerciseSets.postValue(savedSetsWithId);
            
            
            WorkoutExercise currentExercise = workoutExercise.getValue();
            if (currentExercise != null) {
                currentExercise.getSetsCompleted().clear();
                currentExercise.getSetsCompleted().addAll(savedSetsWithId);
                workoutExercise.postValue(currentExercise);
            }
            
            
            saveToCache(savedSetsWithId);
            
        } catch (Exception e) {
            
            
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Непредвиденная ошибка при обработке сохранения подходов: " + e.getMessage(), e);
            errorMessage.postValue("Ошибка сохранения подходов: " + e.getMessage());
        }
    }

    
    public void addNewSet(ExerciseSet newSet) {
        if (newSet == null) {
            Log.e(TAG, "addNewSet: newSet равен null");
            return;
        }
        
        
        List<ExerciseSet> currentSets = exerciseSets.getValue();
        List<ExerciseSet> updatedSetsList = (currentSets == null) ? new ArrayList<>() : new ArrayList<>(currentSets);
        
        
        boolean hasTemporarySet = false;
        for (ExerciseSet set : updatedSetsList) {
            if (set.getId() != null && set.getId().startsWith("temp_") && !set.isCompleted()) {
                hasTemporarySet = true;
                
                break;
            }
        }
        
        if (hasTemporarySet) {
            
            
            safeSetValue(exerciseSets, updatedSetsList);
            return;
        }
        
        
        String tempId = "temp_" + UUID.randomUUID().toString();
        newSet.setId(tempId);
        updatedSetsList.add(newSet);
        
        
        safeSetValue(exerciseSets, updatedSetsList);
        
        
        WorkoutExercise currentExercise = workoutExercise.getValue();
        if (currentExercise != null) {
            currentExercise.getSetsCompleted().clear();
            currentExercise.getSetsCompleted().addAll(updatedSetsList);
            safeSetValue(workoutExercise, currentExercise);
            
            
            saveToCache(updatedSetsList);
        }
        
        
        executor.execute(() -> {
            try {
                String setId = workoutRepository.addSet(exerciseId, newSet);
                
                
                if (setId != null && !setId.isEmpty()) {
                    
                    newSet.setId(setId);
                    
                    
                    List<ExerciseSet> currentSetsAfterResponse = exerciseSets.getValue();
                    if (currentSetsAfterResponse != null) {
                        
                        List<ExerciseSet> newUpdatedSetsList = new ArrayList<>();
                        boolean foundAndReplaced = false;
                        
                        
                        for (ExerciseSet set : currentSetsAfterResponse) {
                            if (tempId.equals(set.getId())) {
                                
                                
                                ExerciseSet updatedSet = new ExerciseSet(set);
                                updatedSet.setId(setId);
                                newUpdatedSetsList.add(updatedSet);
                                foundAndReplaced = true;
                                
                            } else {
                                newUpdatedSetsList.add(set);
                            }
                        }
                        
                        
                        if (!foundAndReplaced) {
                            
                            boolean alreadyHasSetWithSameNumber = false;
                            for (ExerciseSet existingSet : newUpdatedSetsList) {
                                if (existingSet.getSetNumber() == newSet.getSetNumber()) {
                                    alreadyHasSetWithSameNumber = true;
                                    
                                    break;
                                }
                            }
                            
                            
                            if (!alreadyHasSetWithSameNumber) {
                                newUpdatedSetsList.add(newSet);
                                
                            } else {
                                
                            }
                        }
                        
                        
                        newUpdatedSetsList.sort((a, b) -> Integer.compare(a.getSetNumber(), b.getSetNumber()));
                        
                        
                        exerciseSets.postValue(newUpdatedSetsList);
                        
                        
                        WorkoutExercise exercise = workoutExercise.getValue();
                        if (exercise != null) {
                            exercise.getSetsCompleted().clear();
                            exercise.getSetsCompleted().addAll(newUpdatedSetsList);
                            workoutExercise.postValue(exercise);
                        }
                        
                        
                        saveToCache(newUpdatedSetsList);
                    }
                    
                    
                } else {
                    Log.e(TAG, "addNewSet: Не удалось сохранить подход в БД, получен null или пустой ID");
                    errorMessage.postValue("Ошибка: не удалось сохранить новый подход");
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "addNewSet: Ошибка при добавлении подхода: " + e.getMessage(), e);
                
                
                if (e.getMessage() != null && (
                    e.getMessage().contains("row-level security policy") || 
                    e.getMessage().contains("42501") || 
                    e.getMessage().contains("403"))) {
                    errorMessage.postValue("Ошибка доступа: нет прав на добавление подходов. Пожалуйста, войдите снова в аккаунт.");
                } else {
                    errorMessage.postValue("Ошибка при добавлении подхода: " + e.getMessage());
                }
                
                
                
            }
        });
    }



    
    public boolean isExerciseRated(String workoutExerciseId) {
        if (workoutExerciseId == null || workoutExerciseId.isEmpty()) {
            return false;
        }
        
        String ratedExerciseKey = "rated_exercise_" + workoutExerciseId;
        boolean isRated = sharedPreferences.getBoolean(ratedExerciseKey, false);
        
        
              
        return isRated;
    }

    
    public LiveData<List<ExerciseSet>> getExerciseSets() {
        return exerciseSets;
    }
    
    public LiveData<WorkoutExercise> getWorkoutExercise() {
        return workoutExercise;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsResting() {
        return isResting;
    }
    
    public LiveData<Long> getRestTimeRemaining() {
        return restTimeRemaining;
    }
    
    
    public void setSets(List<ExerciseSet> sets) {
        if (sets == null) {
            Log.e(TAG, "setSets: передан null список подходов");
            return;
        }
        
        
        safeSetValue(exerciseSets, new ArrayList<>(sets));
        
        
        WorkoutExercise currentExercise = workoutExercise.getValue();
        if (currentExercise != null) {
            currentExercise.getSetsCompleted().clear();
            currentExercise.getSetsCompleted().addAll(sets);
            safeSetValue(workoutExercise, currentExercise);
            
            
            saveToCache(sets);
        } else {
            Log.e(TAG, "setSets: не удалось обновить workoutExercise, так как текущее значение null");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        
        if (restTimer != null) {
            restTimer.cancel();
        }
    }

    
    public Float getLastWeightForExercise(String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.e(TAG, "getLastWeightForExercise: exerciseId равен null или пуст");
            return null;
        }
        
        try {
            
            List<ExerciseSet> currentSets = exerciseSets.getValue();
            if (currentSets != null && !currentSets.isEmpty()) {
                
                List<ExerciseSet> sortedSets = new ArrayList<>(currentSets);
                sortedSets.sort((a, b) -> {
                    Long timeA = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
                    Long timeB = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
                    return timeB.compareTo(timeA); 
                });
                
                
                for (ExerciseSet set : sortedSets) {
                    if (exerciseId.equals(set.getExerciseId()) && set.getWeight() != null && set.getWeight() > 0) {
                        
                        return set.getWeight();
                    }
                }
            }
            
            
            Float lastWeight = workoutRepository.getLastWeightForExercise(exerciseId);
            if (lastWeight != null) {
                
                return lastWeight;
            } else {
                
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastWeightForExercise: Ошибка при получении последнего веса: " + e.getMessage(), e);
            return null;
        }
    }

    
    public Integer getLastRepsForExercise(String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.e(TAG, "getLastRepsForExercise: exerciseId равен null или пуст");
            return null;
        }
        
        try {
            
            List<ExerciseSet> currentSets = exerciseSets.getValue();
            if (currentSets != null && !currentSets.isEmpty()) {
                
                List<ExerciseSet> sortedSets = new ArrayList<>(currentSets);
                sortedSets.sort((a, b) -> {
                    Long timeA = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
                    Long timeB = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
                    return timeB.compareTo(timeA); 
                });
                
                
                for (ExerciseSet set : sortedSets) {
                    if (exerciseId.equals(set.getExerciseId()) && set.getReps() != null && set.getReps() > 0) {
                        
                        return set.getReps();
                    }
                }
            }
            
            
            Integer lastReps = workoutRepository.getLastRepsForExercise(exerciseId);
            if (lastReps != null) {
                
                return lastReps;
            } else {
                
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastRepsForExercise: Ошибка при получении последних повторений: " + e.getMessage(), e);
            return null;
        }
    }

    
    public boolean isCardioExercise() {
        if (exercise == null || exercise.getExerciseType() == null) {
            return false;
        }
        
        String exerciseType = exercise.getExerciseType().toLowerCase();
        return exerciseType.contains("кардио") || 
               exerciseType.contains("cardio") || 
               exerciseType.contains("статич") || 
               exerciseType.contains("static") || 
               exerciseType.contains("удержание") || 
               exerciseType.contains("планка");
    }
} 