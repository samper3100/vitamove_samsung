package com.martist.vitamove.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.martist.vitamove.db.entity.StepHistoryEntity;

import java.util.List;


@Dao
public interface StepHistoryDao {

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StepHistoryEntity stepHistoryEntity);

    
    @Update
    void update(StepHistoryEntity stepHistoryEntity);

    
    @Query("SELECT * FROM step_history ORDER BY date DESC")
    List<StepHistoryEntity> getAllHistory();

    
    @Query("SELECT * FROM step_history ORDER BY date DESC")
    LiveData<List<StepHistoryEntity>> getAllHistoryLiveData();

    
    @Query("SELECT * FROM step_history WHERE date >= date('now', '-' || :days || ' days') ORDER BY date DESC")
    List<StepHistoryEntity> getLastNDaysHistory(int days);

    
    @Query("DELETE FROM step_history WHERE date < :date")
    int deleteHistoryOlderThan(String date);

    
    @Query("SELECT * FROM step_history WHERE date = :date LIMIT 1")
    StepHistoryEntity getStepHistoryForDate(String date);

    
    @Query("SELECT * FROM step_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    List<StepHistoryEntity> getHistoryBetweenDates(String startDate, String endDate);
} 