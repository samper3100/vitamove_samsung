package com.martist.vitamove.db.dao;

import android.util.Log;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.martist.vitamove.db.entity.ExerciseSetEntity;
import com.martist.vitamove.db.entity.UserWorkoutEntity;
import com.martist.vitamove.db.entity.WorkoutExerciseEntity;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.UserWorkout;
import com.martist.vitamove.workout.data.models.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Dao
public abstract class WorkoutDao {

    private static final String TAG = "WorkoutDao"; 

    

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertWorkout(UserWorkoutEntity workout);

    @Update
    public abstract void updateWorkout(UserWorkoutEntity workout);

    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    public abstract UserWorkoutEntity getActiveWorkoutEntity(String userId);
    
    @Query("DELETE FROM user_workouts WHERE id = :workoutId")
    public abstract void deleteWorkoutById(String workoutId);
    
    @Query("DELETE FROM user_workouts")
    public abstract void deleteAllWorkouts(); 
    
    
    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    public abstract List<UserWorkoutEntity> getWorkoutsByTimeRange(String userId, long startTime, long endTime, int offset, int limit);
    
    
    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    public abstract List<UserWorkoutEntity> getAllWorkoutsByTimeRange(String userId, long startTime, long endTime);

    
    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    public abstract androidx.lifecycle.LiveData<List<UserWorkoutEntity>> getAllWorkoutsByTimeRangeLiveData(String userId, long startTime, long endTime);

    
    @Query("SELECT * FROM user_workouts WHERE id = :workoutId LIMIT 1")
    public abstract UserWorkoutEntity getWorkoutEntityById(String workoutId);

    

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertWorkoutExercises(List<WorkoutExerciseEntity> exercises);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertWorkoutExercise(WorkoutExerciseEntity exercise);

    @Update
    public abstract void updateWorkoutExercise(WorkoutExerciseEntity exercise);

    @Query("SELECT * FROM workout_exercises WHERE workout_id = :workoutId ORDER BY order_number ASC")
    public abstract List<WorkoutExerciseEntity> getExercisesForWorkout(String workoutId);
    
    
    @Query("SELECT * FROM workout_exercises WHERE workout_id = :workoutId ORDER BY order_number ASC")
    public abstract androidx.lifecycle.LiveData<List<WorkoutExerciseEntity>> getExercisesForWorkoutLiveData(String workoutId);
    
    @Query("DELETE FROM workout_exercises WHERE id = :exerciseId")
    public abstract void deleteWorkoutExerciseById(String exerciseId);

    

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertExerciseSets(List<ExerciseSetEntity> sets);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertExerciseSet(ExerciseSetEntity set);

    @Update
    public abstract void updateExerciseSet(ExerciseSetEntity set);

    @Query("SELECT * FROM exercise_sets WHERE workout_exercise_id = :exerciseId ORDER BY set_number ASC")
    public abstract List<ExerciseSetEntity> getSetsForExercise(String exerciseId);

    
    @Query("SELECT * FROM exercise_sets WHERE workout_exercise_id = :exerciseId ORDER BY set_number ASC")
    public abstract androidx.lifecycle.LiveData<List<ExerciseSetEntity>> getSetsForExerciseLiveData(String exerciseId);

    @Query("DELETE FROM exercise_sets WHERE workout_exercise_id = :exerciseId")
    public abstract void deleteSetsForExercise(String exerciseId);

    

    
    @Transaction
    public UserWorkout getFullActiveWorkout(String userId, WorkoutRepositoryHelper repoHelper) {
        UserWorkoutEntity workoutEntity = getActiveWorkoutEntity(userId);
        if (workoutEntity == null) {
            return null;
        }
        
        List<WorkoutExercise> workoutExercises = new ArrayList<>();
        List<WorkoutExerciseEntity> exerciseEntities = getExercisesForWorkout(workoutEntity.getId());
        
         
        
        for (WorkoutExerciseEntity exerciseEntity : exerciseEntities) {
            
            com.martist.vitamove.workout.data.models.Exercise baseExercise = repoHelper.getExerciseDetailsSync(exerciseEntity.getBaseExerciseId());
            if (baseExercise == null) {
                
                baseExercise = new com.martist.vitamove.workout.data.models.Exercise.Builder()
                        .id(exerciseEntity.getBaseExerciseId())
                        .name("Упражнение не найдено")
                        .build(); 
            }
            
            List<ExerciseSetEntity> setEntities = getSetsForExercise(exerciseEntity.getId());
            List<ExerciseSet> sets = new ArrayList<>();
            for (ExerciseSetEntity setEntity : setEntities) {
                sets.add(setEntity.toModel());
            }
            
            workoutExercises.add(exerciseEntity.toModel(baseExercise, sets));
        }
        
        
        
        return workoutEntity.toModel(workoutExercises);
    }

    
    @Transaction
    public void saveFullWorkout(UserWorkout workout) {
        if (workout == null) return;

        UserWorkoutEntity workoutEntity = UserWorkoutEntity.fromModel(workout);
        insertWorkout(workoutEntity);
        
        if (workout.getExercises() != null) {
            List<WorkoutExerciseEntity> exerciseEntities = new ArrayList<>();
            List<ExerciseSetEntity> allSetEntities = new ArrayList<>();
            
            for (WorkoutExercise exerciseModel : workout.getExercises()) {
                WorkoutExerciseEntity exerciseEntity = WorkoutExerciseEntity.fromModel(exerciseModel, workout.getId());
                exerciseEntities.add(exerciseEntity);
                
                if (exerciseModel.getSetsCompleted() != null) {
                    for (ExerciseSet setModel : exerciseModel.getSetsCompleted()) {
                        
                        if (setModel.getExerciseId() == null && exerciseModel.getExercise() != null) {
                            setModel.setExerciseId(exerciseModel.getExercise().getId());
                            
                        }
                        
                        ExerciseSetEntity setEntity = ExerciseSetEntity.fromModel(setModel, exerciseEntity.getId());
                        allSetEntities.add(setEntity);
                        
                        
                    }
                }
            }
            insertWorkoutExercises(exerciseEntities);
            
            
            for (int i = 0; i < allSetEntities.size(); i++) {
                ExerciseSetEntity entity = allSetEntities.get(i);
                
            }
            
            insertExerciseSets(allSetEntities);
        }
    }
    
    
    @Transaction
    public void updateSingleSet(ExerciseSet set) {
         if (set == null || set.getWorkoutExerciseId() == null) return;
         
         
         if (set.getExerciseId() == null) {
             try {
                 WorkoutExerciseEntity exercise = getWorkoutExerciseById(set.getWorkoutExerciseId());
                 if (exercise != null) {
                     set.setExerciseId(exercise.getBaseExerciseId());
                 }
             } catch (Exception e) {
                 Log.e(TAG, "updateSingleSet: Ошибка при получении exercise_id", e);
             }
         }
         
         ExerciseSetEntity setEntity = ExerciseSetEntity.fromModel(set, set.getWorkoutExerciseId());
         updateExerciseSet(setEntity);
    }
    
    
    @Query("SELECT * FROM workout_exercises WHERE id = :exerciseId LIMIT 1")
    public abstract WorkoutExerciseEntity getWorkoutExerciseById(String exerciseId);
    
    @Update
    public abstract void updateExerciseSetList(List<ExerciseSetEntity> setEntities);
    
    
    @Transaction
    public void addSetToExercise(ExerciseSet set) {
         if (set == null || set.getWorkoutExerciseId() == null) return;
         
         
         if (set.getId() == null) {
             set.setId(UUID.randomUUID().toString());
         }
         
         
         if (set.getExerciseId() == null) {
             try {
                 WorkoutExerciseEntity exercise = getWorkoutExerciseById(set.getWorkoutExerciseId());
                 if (exercise != null) {
                     set.setExerciseId(exercise.getBaseExerciseId());
                     
                 } else {
                     Log.e(TAG, "addSetToExercise: Не удалось получить workout_exercise с id = " + set.getWorkoutExerciseId());
                 }
             } catch (Exception e) {
                 Log.e(TAG, "addSetToExercise: Ошибка при получении exercise_id", e);
             }
         } else {
             
         }
         
         ExerciseSetEntity setEntity = ExerciseSetEntity.fromModel(set, set.getWorkoutExerciseId());
         
         insertExerciseSet(setEntity);
    }
    
    
    @Transaction
    public void addExerciseToWorkout(WorkoutExercise exercise, String workoutId) {
        if (exercise == null || exercise.getExercise() == null) return;
        WorkoutExerciseEntity exerciseEntity = WorkoutExerciseEntity.fromModel(exercise, workoutId);
        insertWorkoutExercise(exerciseEntity);
        
        String exerciseId = exercise.getExercise().getId();
        
        
        if (exercise.getSetsCompleted() != null && !exercise.getSetsCompleted().isEmpty()) {
             List<ExerciseSetEntity> setEntities = new ArrayList<>();
             for (ExerciseSet setModel : exercise.getSetsCompleted()) {
                
                setModel.setExerciseId(exerciseId);
                
                
                if (setModel.getCreatedAt() == null) {
                    setModel.setCreatedAt(System.currentTimeMillis());
                }
                
                ExerciseSetEntity entity = ExerciseSetEntity.fromModel(setModel, exerciseEntity.getId());
                setEntities.add(entity);
                
                
            }
            
            
            
            
            insertExerciseSets(setEntities);
        }
    }
    
    
    @Transaction
    public void deleteFullWorkoutExercise(String exerciseId) {
        deleteSetsForExercise(exerciseId); 
        deleteWorkoutExerciseById(exerciseId); 
    }

    
    @Transaction
    public void deleteFullWorkout(String workoutId) {
        List<WorkoutExerciseEntity> exercises = getExercisesForWorkout(workoutId);
        for (WorkoutExerciseEntity exercise : exercises) {
            deleteSetsForExercise(exercise.getId());
        }
        
        deleteWorkoutById(workoutId);
    }
    
    
    public interface WorkoutRepositoryHelper {
       Exercise getExerciseDetailsSync(String exerciseId);
    }

    
    @Query("SELECT weight FROM exercise_sets WHERE exercise_id = :exerciseId AND weight IS NOT NULL ORDER BY created_at DESC LIMIT 1")
    public abstract Float getLastWeightForExercise(String exerciseId);
    
    
    @Query("SELECT reps FROM exercise_sets WHERE exercise_id = :exerciseId AND reps IS NOT NULL ORDER BY created_at DESC LIMIT 1")
    public abstract Integer getLastRepsForExercise(String exerciseId);
} 