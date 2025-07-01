package com.martist.vitamove.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.db.AppDatabase;
import com.martist.vitamove.db.dao.WorkoutDao;
import com.martist.vitamove.db.entity.UserWorkoutEntity;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.repositories.UserRepository;
import com.martist.vitamove.workout.data.managers.WorkoutScheduleManager;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.UserWorkout;
import com.martist.vitamove.workout.data.models.WorkoutExercise;
import com.martist.vitamove.workout.data.models.WorkoutPlan;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ActiveWorkoutViewModel extends AndroidViewModel implements WorkoutDao.WorkoutRepositoryHelper {
    private static final String TAG = "ActiveWorkoutViewModel";
    private static final String WORKOUT_ID_TRACE_TAG = "WorkoutIdTrace";
    
    private final WorkoutDao workoutDao;
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<UserWorkout> activeWorkout;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> isWorkoutCompleted;
    private final MutableLiveData<Integer> realTimeCalories = new MutableLiveData<>(0);
    

    private final Executor executor;
    

    private final ExerciseListViewModel exerciseListViewModel;


    private final MutableLiveData<String> activeWorkoutId = new MutableLiveData<>();


    private final WorkoutScheduleManager scheduleManager;

    public ActiveWorkoutViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        workoutDao = database.workoutDao();
        workoutRepository = ((VitaMoveApplication) application).getWorkoutRepository();
        userRepository = new UserRepository(application);
        
        activeWorkout = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>("");
        isWorkoutCompleted = new MutableLiveData<>(false);
        

        scheduleManager = WorkoutScheduleManager.getInstance(application);
        

        exerciseListViewModel = new ViewModelProvider.AndroidViewModelFactory(application)
            .create(ExerciseListViewModel.class);
            

        executor = Executors.newSingleThreadExecutor();
        


    }
    

    

    public void loadOrCreateActiveWorkout(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID не может быть пустым для загрузки тренировки");
            errorMessage.postValue("Ошибка: Не удалось определить пользователя.");
            return;
        }
        

        UserWorkout existingWorkout = activeWorkout.getValue();
        if (existingWorkout != null && userId.equals(existingWorkout.getUserId())) {
             

             if (Boolean.TRUE.equals(isLoading.getValue())) {
                 isLoading.postValue(false);
             }
             return;
        }

        
        
        isLoading.setValue(true);
        
        executor.execute(() -> {
            UserWorkout finalWorkout = null;
            
            try {

                UserWorkout existingActiveWorkout = workoutDao.getFullActiveWorkout(userId, this);
                
                

                boolean existingWorkoutIsEmpty = existingActiveWorkout != null && 
                    (existingActiveWorkout.getExercises() == null || existingActiveWorkout.getExercises().isEmpty());
                

                WorkoutPlan todayPlan = null;
                try {
                    todayPlan = scheduleManager.getTodayWorkoutPlan(userId);
                    
                    


                    if (todayPlan != null && existingWorkoutIsEmpty) {
                        
                        try {

                            String emptyWorkoutId = existingActiveWorkout.getId();
                            

                            workoutDao.deleteFullWorkout(emptyWorkoutId);
                            
                            

                            try {
                                workoutRepository.deleteWorkout(emptyWorkoutId);
                                
                            } catch (Exception supabaseEx) {
                                Log.e(TAG, "Ошибка при удалении пустой тренировки из Supabase: " + supabaseEx.getMessage(), supabaseEx);

                            }
                            

                            existingActiveWorkout = null;
                        } catch (Exception deletionEx) {
                            Log.e(TAG, "Ошибка при удалении пустой тренировки: " + deletionEx.getMessage(), deletionEx);

                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при проверке плана на сегодня: " + e.getMessage(), e);
                    errorMessage.postValue("Ошибка при проверке плана на сегодня.");

                }


                if (todayPlan != null) {

                    if (existingActiveWorkout != null) {

                        

                        if (existingActiveWorkout.getExercises() == null || existingActiveWorkout.getExercises().isEmpty()) {


                            
                            try {

                                String emptyWorkoutId = existingActiveWorkout.getId();
                                

                                workoutDao.deleteFullWorkout(emptyWorkoutId);
                                
                                

                                try {
                                    workoutRepository.deleteWorkout(emptyWorkoutId);
                                    
                                } catch (Exception supabaseEx) {
                                    Log.e(TAG, "Ошибка при удалении пустой тренировки из Supabase: " + supabaseEx.getMessage(), supabaseEx);

                                }
                                

                                
                                String createdWorkoutId = workoutRepository.createWorkoutFromPlan(todayPlan);
                                if (createdWorkoutId != null) {
                                    finalWorkout = workoutDao.getFullActiveWorkout(userId, this);
                                    if (finalWorkout != null) {
                                        
                                        
                                    } else {
                                         Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Не удалось загрузить тренировку из Room после создания из плана!");
                                         errorMessage.postValue("Критическая ошибка БД после создания из плана.");
                                    }
                                } else {
                                     Log.e(TAG, "Ошибка при создании активной тренировки из плана (метод вернул null ID).");
                                     errorMessage.postValue("Не удалось создать тренировку по плану.");

                                     finalWorkout = createAndSaveNewEmptyWorkout(userId);
                                     if (finalWorkout != null) {
                                        
                                     }
                                }
                            } catch (Exception deletionOrCreationEx) {
                                Log.e(TAG, "Ошибка при удалении пустой или создании из плана: " + deletionOrCreationEx.getMessage(), deletionOrCreationEx);
                                errorMessage.postValue("Ошибка: " + deletionOrCreationEx.getMessage());

                                finalWorkout = existingActiveWorkout;
                                
                            }
                        } else {

                            
                            finalWorkout = existingActiveWorkout;
                            
                        }
                    } else {


                        
                         try {
                            String createdWorkoutId = workoutRepository.createWorkoutFromPlan(todayPlan);
                            if (createdWorkoutId != null) {
                                finalWorkout = workoutDao.getFullActiveWorkout(userId, this);
                                if (finalWorkout != null) {
                                    
                                    
                                } else {
                                     Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Не удалось загрузить тренировку из Room после создания из плана!");
                                     errorMessage.postValue("Критическая ошибка БД после создания из плана.");
                                }
                            } else {
                                 Log.e(TAG, "Ошибка при создании активной тренировки из плана (метод вернул null ID).");
                                 errorMessage.postValue("Не удалось создать тренировку по плану.");

                                 finalWorkout = createAndSaveNewEmptyWorkout(userId);
                                 if (finalWorkout != null) {
                                    
                                 }
                            }
                        } catch (Exception creationEx) {
                             Log.e(TAG, "Ошибка при создании активной тренировки из плана: " + creationEx.getMessage(), creationEx);
                             errorMessage.postValue("Ошибка при создании тренировки по плану: " + creationEx.getMessage());

                             finalWorkout = createAndSaveNewEmptyWorkout(userId);
                        }
                    }
                } else {

                    if (existingActiveWorkout != null) {


                        
                        finalWorkout = existingActiveWorkout;
                        
                    } else {


                        
                        finalWorkout = createAndSaveNewEmptyWorkout(userId);
                        if (finalWorkout != null) {
                            
                        }
                    }
                }


                if (finalWorkout != null) {
                    activeWorkout.postValue(finalWorkout);
                    activeWorkoutId.postValue(finalWorkout.getId());
                     
                } else {

                    activeWorkout.postValue(null);
                    activeWorkoutId.postValue(null);
                    
                }

            } catch (Exception e) {
                Log.e(TAG, "Непредвиденная ошибка при загрузке/создании активной тренировки: " + e.getMessage(), e);
                errorMessage.postValue("Критическая ошибка: " + e.getMessage());
                activeWorkout.postValue(null);
                activeWorkoutId.postValue(null);
                 
            } finally {
                isLoading.postValue(false);
            }
        });
    }



    private UserWorkout createAndSaveNewEmptyWorkout(String userId) {
        String supabaseWorkoutId = null;
        try {

            supabaseWorkoutId = workoutRepository.createWorkout(userId);
            if (supabaseWorkoutId == null) {
                throw new Exception("Supabase createWorkout вернул null ID");
            }
            


            UserWorkout newWorkout = createNewLocalWorkout(userId);
            newWorkout.setId(supabaseWorkoutId);
            workoutDao.insertWorkout(UserWorkoutEntity.fromModel(newWorkout));
            


            UserWorkout createdWorkout = workoutDao.getFullActiveWorkout(userId, this);
            if (createdWorkout != null) {
                 activeWorkout.postValue(createdWorkout);
                 
                 activeWorkoutId.postValue(createdWorkout.getId());
                 
                 return createdWorkout;
            } else {
                 Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Не удалось получить только что созданную пустую тренировку из Room!");
                 errorMessage.postValue("Критическая ошибка локальной базы данных при создании пустой тренировки.");
                 activeWorkout.postValue(null);
                 
                 activeWorkoutId.postValue(null);
                 return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА при создании пустой тренировки: " + e.getMessage(), e);
            errorMessage.postValue("Критическая ошибка: " + e.getMessage());
            activeWorkout.postValue(null);
            
            activeWorkoutId.postValue(null);
            return null;
        }
    }





    public void addExercise(String exerciseId, String userId) {
        
        isLoading.setValue(true);

        executor.execute(() -> {
            UserWorkout currentActiveWorkout = activeWorkout.getValue();

            if (currentActiveWorkout == null || currentActiveWorkout.getId() == null || currentActiveWorkout.getEndTime() != null) {
                
                

                currentActiveWorkout = createAndSaveNewEmptyWorkout(userId);
                if (currentActiveWorkout == null) {
                    Log.e(TAG, "addExercise: Не удалось создать новую тренировку");
                    errorMessage.postValue("Ошибка: Не удалось создать новую тренировку. Попробуйте перезайти на экран.");
                    isLoading.postValue(false);
                    return;
                }
                

                UserWorkout finalWorkout = currentActiveWorkout;
                new Handler(Looper.getMainLooper()).post(() -> {
                    activeWorkout.setValue(finalWorkout);
                    activeWorkoutId.setValue(finalWorkout.getId());
                });
            }
            
            String workoutIdToUse = currentActiveWorkout.getId();

            try {

                Exercise fullExercise = getExerciseDetailsSync(exerciseId);
                if (fullExercise == null) {
                    throw new Exception("Упражнение с ID " + exerciseId + " не найдено в кэше");
                }


                String supabaseWorkoutExerciseId = null;

                int orderNumber = currentActiveWorkout.getExercises() != null ? currentActiveWorkout.getExercises().size() : 0;
                

                try {
                    supabaseWorkoutExerciseId = workoutRepository.addExerciseToWorkout(
                        workoutIdToUse, 
                        exerciseId, 
                        orderNumber
                    );
                    
                } catch (Exception e) {
                     Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА при создании WorkoutExercise в Supabase: " + e.getMessage(), e);
                     errorMessage.postValue("Критическая ошибка: не удалось добавить упражнение на сервер.");
                     throw new Exception("Не удалось создать запись упражнения в Supabase", e);
                }
                

                WorkoutExercise newWorkoutExercise = new WorkoutExercise();
                newWorkoutExercise.setId(supabaseWorkoutExerciseId);
                newWorkoutExercise.setExercise(fullExercise);
                newWorkoutExercise.setOrderNumber(orderNumber); 


                Integer lastReps = workoutDao.getLastRepsForExercise(exerciseId);
                

                int defaultSetsCount = fullExercise.getDefaultSets();
                boolean isCardio = fullExercise.usesTimer();

                if (isCardio) {
                    defaultSetsCount = 1;
                } else if (defaultSetsCount <= 0) {
                    defaultSetsCount = 3;
                }
                
                List<ExerciseSet> sets = new ArrayList<>();
                for (int i = 1; i <= defaultSetsCount; i++) {
                    ExerciseSet set = new ExerciseSet();
                    set.setSetNumber(i);
                    set.setReps(lastReps != null && lastReps > 0 ? lastReps : 12);
                    set.setExerciseId(exerciseId);
                    set.setCompleted(false);
                    sets.add(set);
                }
                
                newWorkoutExercise.setSetsCompleted(sets);
                


                workoutDao.addExerciseToWorkout(newWorkoutExercise, workoutIdToUse);
                
                


                String finalUserId = userId;
                if (finalUserId == null || finalUserId.isEmpty()) {
                     

                     finalUserId = currentActiveWorkout.getUserId();
                }
                if (finalUserId != null && !finalUserId.isEmpty()){
                    UserWorkout finalUpdatedWorkout = workoutDao.getFullActiveWorkout(finalUserId, this);
                    
                    activeWorkout.postValue(finalUpdatedWorkout);
                } else {
                    Log.e(TAG, "addExercise: Не удалось перезагрузить тренировку - отсутствует User ID как в параметре, так и в LiveData");
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении упражнения: " + e.getMessage(), e);

                if (!e.getMessage().contains("Supabase")) { 
                    errorMessage.postValue("Ошибка добавления упражнения: " + e.getMessage());
                }
            } finally {
                isLoading.postValue(false);
            }
        });
    }




    public void removeExercise(String exerciseIdToRemove) {
        
        String currentWorkoutId = activeWorkoutId.getValue();
        if (currentWorkoutId == null || exerciseIdToRemove == null) {
            Log.e(TAG, "Невозможно удалить упражнение: ID тренировки или упражнения не найдены");
            return;
        }
        isLoading.setValue(true);

        executor.execute(() -> {
            try {

                workoutDao.deleteFullWorkoutExercise(exerciseIdToRemove);
                


                UserWorkout updatedWorkout = workoutDao.getFullActiveWorkout(activeWorkout.getValue().getUserId(), this);
                activeWorkout.postValue(updatedWorkout);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при удалении упражнения: " + e.getMessage(), e);
                errorMessage.postValue("Ошибка удаления упражнения: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }


    public void completeWorkout(long endTime) {
        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null || currentWorkout.getUserId() == null) {
            Log.e(TAG, "Невозможно завершить тренировку: нет активной тренировки или user ID");
            return;
        }
        String userId = currentWorkout.getUserId();
        String currentWorkoutId = currentWorkout.getId();
        

        final String planId = currentWorkout.getPlanId();

        if (currentWorkoutId == null) {
             Log.e(TAG, "Невозможно завершить тренировку: ID активной тренировки равен null");
             Log.e(WORKOUT_ID_TRACE_TAG, "completeWorkout - ERROR: currentWorkoutId is null!");
             return;
        }

        
        isLoading.setValue(true);

        executor.execute(() -> {
            UserWorkout completedWorkoutToSend = null;
            boolean planStatusUpdated = false;
            


            double totalWorkoutCalories = 0.0;
            Float userWeight = null; 
            
            try {

                UserProfile profile = userRepository.getCurrentUserProfile(); 
                if (profile != null) {


                    userWeight = profile.getCurrentWeight(); 
                    
                } else {
                     Log.e(TAG, "Не удалось получить профиль пользователя (" + userId + ") для расчета калорий.");

                }

            } catch (Exception weightEx) {
                 Log.e(TAG, "Ошибка при получении профиля/веса пользователя ("+ userId +") для расчета калорий: " + weightEx.getMessage(), weightEx);

            }

            if (userWeight != null && userWeight > 0 && currentWorkout.getExercises() != null) {
                
                for (WorkoutExercise workoutExercise : currentWorkout.getExercises()) {
                    Exercise baseExercise = workoutExercise.getExercise();
                    if (baseExercise == null || baseExercise.getMet() <= 0) { 
                         
                        continue;
                    }
                    double metValue = baseExercise.getMet();
                    long totalExerciseDurationSeconds = 0;

                    if (workoutExercise.getSetsCompleted() != null) {
                        for (ExerciseSet set : workoutExercise.getSetsCompleted()) {

                            if (set.isCompleted() && set.getDurationSeconds() != null && set.getDurationSeconds() > 0) { 
                                totalExerciseDurationSeconds += set.getDurationSeconds();
                            }
                        }
                    }

                    if (totalExerciseDurationSeconds > 0) {
                        double durationMinutes = totalExerciseDurationSeconds / 60.0;

                        double exerciseCalories = metValue * 3.5 * userWeight / 200.0 * durationMinutes;
                        totalWorkoutCalories += exerciseCalories;
                         
                    } else {
                         
                    }
                }
                 
            } else {
                 
            }

            int calculatedCalories = (int) Math.round(totalWorkoutCalories); 


            try {

                
                UserWorkoutEntity workoutEntity = workoutDao.getActiveWorkoutEntity(userId);
                if (workoutEntity != null) {
                    
                    workoutEntity.setEndTime(endTime);

                    workoutEntity.setTotalCalories(calculatedCalories); 
                    workoutDao.updateWorkout(workoutEntity);
                    


                    completedWorkoutToSend = new UserWorkout(
                        workoutEntity.getId(),
                        workoutEntity.getUserId(),
                        workoutEntity.getName(),
                        workoutEntity.getStartTime(),
                        workoutEntity.getEndTime(), 
                        workoutEntity.getTotalCalories(),
                        workoutEntity.getNotes(),
                        workoutEntity.getProgramId(),
                        workoutEntity.getProgramDayNumber(),
                        workoutEntity.getProgramDayId(),
                        workoutEntity.getPlanId(),
                        currentWorkout.getExercises() 
                    );
                    

                    
                    
                    

                    
                    workoutRepository.saveCompletedWorkout(completedWorkoutToSend);
                    


                    if (planId != null && !planId.isEmpty()) {
                        try {
                            
                            workoutRepository.updateWorkoutPlanStatus(planId, "completed");
                            planStatusUpdated = true;
                            
                        } catch (Exception planUpdateEx) {
                            Log.e(TAG, "Ошибка при обновлении статуса плана ID: " + planId, planUpdateEx);


                            errorMessage.postValue("Ошибка обновления статуса плана: " + planUpdateEx.getMessage()); 
                        }
                    } else {
                        
                    }



                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    final boolean finalPlanStatusUpdated = planStatusUpdated;
                    mainHandler.post(() -> {
                        isWorkoutCompleted.setValue(true);
                        activeWorkout.setValue(null);
                        
                        
                        activeWorkoutId.setValue(null);
                        isLoading.setValue(false);
                        
                    });
                    
                } else {
                     Log.e(TAG, "Не найдена активная тренировка в Room для завершения (User ID: " + userId + ")");
                     Log.e(WORKOUT_ID_TRACE_TAG, "completeWorkout - ERROR: workoutEntity is null for User ID: " + userId);
                     throw new Exception("Активная тренировка не найдена в локальной базе.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при завершении или синхронизации тренировки (предполагаемый ID: " + currentWorkoutId + ")", e);
                Log.e(WORKOUT_ID_TRACE_TAG, "completeWorkout - Exception during completion for Workout ID: " + currentWorkoutId, e);

                 Handler mainHandler = new Handler(Looper.getMainLooper());
                 mainHandler.post(() -> {
                     errorMessage.setValue("Ошибка завершения тренировки: " + e.getMessage());
                     isLoading.setValue(false);
                 });
            } 
        });
    }





    private UserWorkout createNewLocalWorkout(String userId) {
        UserWorkout newWorkout = new UserWorkout();

        newWorkout.setId(UUID.randomUUID().toString()); 
        newWorkout.setUserId(userId);
        newWorkout.setName("Новая тренировка");

        newWorkout.setExercises(new ArrayList<>());
        return newWorkout;
    }
    


    @Override
    public Exercise getExerciseDetailsSync(String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) {
            
            return null;
        }
        

        Exercise cachedExercise = exerciseListViewModel.getExerciseById(exerciseId);
        
        if (cachedExercise != null) {


            return cachedExercise;
        } else {

            
            try {

                Exercise fetchedExercise = workoutRepository.getExerciseById(exerciseId);
                if (fetchedExercise != null) {
                    


                    return fetchedExercise;
                } else {
                    Log.e(TAG, "getExerciseDetailsSync: Не удалось загрузить упражнение ID: " + exerciseId + " из репозитория (вернулся null).");
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "getExerciseDetailsSync: Ошибка при синхронной загрузке упражнения ID: " + exerciseId + " из репозитория", e);
                return null;
            }
        }
    }
    


    public LiveData<UserWorkout> getActiveWorkout() {
        return activeWorkout;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsWorkoutCompleted() {
        return isWorkoutCompleted;
    }
    public LiveData<String> getActiveWorkoutId() {
        return activeWorkoutId;
    }


    public LiveData<Integer> getRealTimeCalories() {
        return realTimeCalories;
    }


    public void calculateRealTimeCalories(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID не может быть пустым для расчета калорий");
            return;
        }

        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null || currentWorkout.getExercises() == null || currentWorkout.getExercises().isEmpty()) {
            
            realTimeCalories.postValue(0);
            return;
        }

        executor.execute(() -> {
            double totalWorkoutCalories = 0.0;
            Float userWeight = null;
            
            try {

                UserProfile profile = userRepository.getCurrentUserProfile();
                if (profile != null) {
                    userWeight = profile.getCurrentWeight();
                    
                } else {
                    Log.e(TAG, "Не удалось получить профиль пользователя для расчета калорий в реальном времени");
                }

            } catch (Exception weightEx) {
                Log.e(TAG, "Ошибка при получении профиля/веса пользователя: " + weightEx.getMessage(), weightEx);
            }

            if (userWeight != null && userWeight > 0) {
                for (WorkoutExercise workoutExercise : currentWorkout.getExercises()) {
                    Exercise baseExercise = workoutExercise.getExercise();
                    if (baseExercise == null || baseExercise.getMet() <= 0) {
                        
                        continue;
                    }
                    
                    double metValue = baseExercise.getMet();
                    long totalExerciseDurationSeconds = 0;

                    if (workoutExercise.getSetsCompleted() != null) {
                        for (ExerciseSet set : workoutExercise.getSetsCompleted()) {

                            if (set.isCompleted() && set.getDurationSeconds() != null && set.getDurationSeconds() > 0) {
                                totalExerciseDurationSeconds += set.getDurationSeconds();
                            }
                        }
                    }

                    if (totalExerciseDurationSeconds > 0) {
                        double durationMinutes = totalExerciseDurationSeconds / 60.0;

                        double exerciseCalories = metValue * 3.5 * userWeight / 200.0 * durationMinutes;
                        totalWorkoutCalories += exerciseCalories;
                        
                        
                    } else {
                        
                    }
                }
                


            } else {
                
            }
            

            int calculatedCalories = (int) Math.round(totalWorkoutCalories);
            
            realTimeCalories.postValue(calculatedCalories);
        });
    }
    



    public void saveCurrentWorkoutStateToDb() {
        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null) {
            
            return;
        }

        
        executor.execute(() -> {
            try {

                workoutDao.saveFullWorkout(currentWorkout);
                
            } catch (Exception e) {
                Log.e(TAG, "saveCurrentWorkoutStateToDb: Ошибка при сохранении текущей тренировки в Room: " + e.getMessage(), e);

            }
        });
    }


    public void updateExerciseOrder(List<WorkoutExercise> exercises) {
        if (exercises == null || exercises.isEmpty()) {
            Log.e(TAG, "updateExerciseOrder: Список упражнений пуст или null");
            return;
        }
        

        isLoading.setValue(true);
        

        String workoutId = activeWorkoutId.getValue();
        if (workoutId == null) {
            Log.e(TAG, "updateExerciseOrder: ID активной тренировки не найден");
            errorMessage.postValue("Не удалось обновить порядок упражнений: ID тренировки не найден");
            isLoading.setValue(false);
            return;
        }
        

        executor.execute(() -> {
            try {

                for (WorkoutExercise exercise : exercises) {

                    workoutRepository.updateExerciseOrderNumber(exercise.getId(), exercise.getOrderNumber());
                }
                

                UserWorkout currentWorkout = activeWorkout.getValue();
                if (currentWorkout != null) {

                    List<WorkoutExercise> sortedExercises = new ArrayList<>(exercises);
                    sortedExercises.sort((e1, e2) -> Integer.compare(e1.getOrderNumber(), e2.getOrderNumber()));
                    currentWorkout.setExercises(sortedExercises);
                    

                    
                    for (WorkoutExercise ex : sortedExercises) {
                        
                    }
                    

                    new Handler(Looper.getMainLooper()).post(() -> {
                        activeWorkout.setValue(currentWorkout);
                        isLoading.setValue(false);
                    });
                    


                    
                    saveCurrentWorkoutStateToDb();
                } else {
                    isLoading.postValue(false);
                }
                
                
            } catch (Exception e) {
                Log.e(TAG, "updateExerciseOrder: Ошибка при обновлении порядка упражнений", e);
                errorMessage.postValue("Ошибка при обновлении порядка упражнений: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }



    public void updateWorkoutExerciseSets(String workoutExerciseId, List<ExerciseSet> updatedSets) {
        
        if (workoutExerciseId == null || updatedSets == null) {
            Log.e(TAG, "updateWorkoutExerciseSets: Невозможно обновить - ID упражнения или список подходов null.");
            return;
        }

        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null || currentWorkout.getExercises() == null) {
            Log.e(TAG, "updateWorkoutExerciseSets: Невозможно обновить - активная тренировка или ее список упражнений null.");
            return;
        }

        boolean exerciseFound = false;
        for (WorkoutExercise exercise : currentWorkout.getExercises()) {
            if (workoutExerciseId.equals(exercise.getId())) {
                
                          

                exercise.setSetsCompleted(new ArrayList<>(updatedSets));
                exerciseFound = true;
                

                
                for(int i=0; i<updatedSets.size(); i++) {
                    ExerciseSet set = updatedSets.get(i);
                    
                }
                break;
            }
        }

        if (exerciseFound) {


            activeWorkout.postValue(currentWorkout); 
            
            

            executor.execute(() -> {
                try {
                    
                    UserWorkoutEntity entity = workoutDao.getActiveWorkoutEntity(currentWorkout.getUserId());
                    if (entity != null) {

                        for (ExerciseSet set : updatedSets) {
                            if (set.getId() != null && !set.getId().isEmpty() && !set.getId().startsWith("temp_")) {
                                workoutRepository.updateSet(set.getId(), set);
                                
                            }
                        }
                        

                        saveCurrentWorkoutStateToDb();
                        
                    }
                } catch (Exception e) {
                    Log.e(TAG, "updateWorkoutExerciseSets: Ошибка при сохранении в БД: " + e.getMessage(), e);
                }
            });
            

            if (currentWorkout.getUserId() != null) {
                
                calculateRealTimeCalories(currentWorkout.getUserId());
            }
        } else {
            
        }
    }



    public void updateWorkoutStartTime(String workoutId, long startTime) throws Exception {
        if (workoutId == null || workoutId.isEmpty()) {
            throw new IllegalArgumentException("ID тренировки не может быть пустым");
        }
        
        
        

        workoutRepository.updateWorkoutStartTime(workoutId, startTime);
        

        executor.execute(() -> {
            try {

                UserWorkoutEntity entity = workoutDao.getWorkoutEntityById(workoutId);
                if (entity != null) {

                    entity.setStartTime(startTime);

                    workoutDao.updateWorkout(entity);
                    
                    

                    UserWorkout currentWorkout = activeWorkout.getValue();
                    if (currentWorkout != null && workoutId.equals(currentWorkout.getId())) {
                        currentWorkout.setStartTime(startTime);
                        activeWorkout.postValue(currentWorkout);
                        
                    }
                } else {
                    Log.e(TAG, "Не найдена тренировка с ID " + workoutId + " в локальной БД");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении времени начала тренировки в локальной БД: " + e.getMessage(), e);
            }
        });
    }
} 