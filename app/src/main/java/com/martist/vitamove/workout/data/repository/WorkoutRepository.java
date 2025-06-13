package com.martist.vitamove.workout.data.repository;

import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.UserWorkout;
import com.martist.vitamove.workout.data.models.WorkoutPlan;

import java.util.List;


public interface WorkoutRepository {

    String createWorkout(String userId) throws Exception;

    void deleteWorkout(String workoutId) throws Exception;

    void deleteUnfinishedWorkouts(String userId) throws Exception;
    

    void updateWorkoutStartTime(String workoutId, long startTime) throws Exception;
    

    String addExerciseToWorkout(String workoutId, String exerciseId, int orderNumber) throws Exception;


    void updateExerciseOrderNumber(String exerciseId, int orderNumber) throws Exception;
    

    String addSet(String workoutExerciseId, ExerciseSet set) throws Exception;
    void updateSet(String setId, ExerciseSet set) throws Exception;
    void deleteSet(String setId) throws Exception;
    List<ExerciseSet> getExerciseSets(String workoutExerciseId) throws Exception;
    

    Float getLastWeightForExercise(String exerciseId);
    

    Integer getLastRepsForExercise(String exerciseId);
    

    List<Exercise> getAllExercises() throws Exception;


    Exercise getExerciseById(String id) throws Exception;
    

    List<UserWorkout> getWorkoutHistory(String userId, long startDate, long endDate) throws Exception;
    

    List<UserWorkout> getWorkoutHistory(String userId, long startDate, long endDate, int page, int pageSize) throws Exception;
    

    

    void updateWorkoutPlan(WorkoutPlan plan) throws Exception;
    List<WorkoutPlan> getWorkoutPlansByDateRange(String userId, long startDate, long endDate) throws Exception;

    WorkoutPlan getTodayWorkoutPlan(String userId) throws Exception;

    List<UserWorkout> getRecentWorkouts(String userId, int limit) throws Exception;


    UserWorkout getWorkoutById(String workoutId) throws Exception;


    String createWorkoutFromPlan(WorkoutPlan plan) throws Exception;
    

    void loadWorkoutExercises(UserWorkout workout) throws Exception;




    WorkoutPlan getWorkoutPlanById(String id) throws Exception;


    void saveCompletedWorkout(UserWorkout workout) throws Exception;


    void updateWorkoutPlanStatus(String planId, String newStatus) throws Exception;


}