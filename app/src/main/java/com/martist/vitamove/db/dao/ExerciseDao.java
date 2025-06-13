package com.martist.vitamove.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.martist.vitamove.db.entity.ExerciseEntity;

import java.util.List;

@Dao
public interface ExerciseDao {
    
    @Query("SELECT * FROM exercises")
    List<ExerciseEntity> getAllExercises();
    
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    ExerciseEntity getExerciseById(String exerciseId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExercise(ExerciseEntity exercise);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExercises(List<ExerciseEntity> exercises);
    
    @Query("DELETE FROM exercises")
    void deleteAllExercises();
    
    @Transaction
    default void updateExercises(List<ExerciseEntity> exercises) {
        deleteAllExercises();
        insertExercises(exercises);
    }
    
    @Query("SELECT COUNT(*) FROM exercises")
    int getExerciseCount();
} 