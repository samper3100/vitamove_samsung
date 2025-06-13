package com.martist.vitamove.repositories;

import android.content.Context;
import android.util.Log;

import com.martist.vitamove.db.AppDatabase;
import com.martist.vitamove.db.dao.StepHistoryDao;
import com.martist.vitamove.db.entity.StepHistoryEntity;
import com.martist.vitamove.managers.StepCounterManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class StepHistoryRepository {
    private static final String TAG = "StepHistoryRepository";
    
    private final StepHistoryDao stepHistoryDao;
    private final ExecutorService executor;
    private static StepHistoryRepository instance;
    

    public static synchronized StepHistoryRepository getInstance(Context context) {
        if (instance == null) {
            instance = new StepHistoryRepository(context);
        }
        return instance;
    }
    

    private StepHistoryRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        stepHistoryDao = db.stepHistoryDao();
        executor = Executors.newSingleThreadExecutor();
    }
    

    public void saveStepsForToday(int stepCount) {
        String today = formatDate(new Date());
        executor.execute(() -> {
            try {
                StepHistoryEntity entity = stepHistoryDao.getStepHistoryForDate(today);
                if (entity == null) {

                    entity = new StepHistoryEntity(today, stepCount);
                    stepHistoryDao.insert(entity);
                    
                } else {

                    entity.setStepCount(stepCount);
                    stepHistoryDao.update(entity);
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при сохранении истории шагов: " + e.getMessage(), e);
            }
        });
    }
    

    public List<Integer> getStepsForLastWeek() {

        List<Integer> weeklySteps = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0));

        try {

            Calendar startOfWeek = Calendar.getInstance();
            int dayOfWeek = startOfWeek.get(Calendar.DAY_OF_WEEK);
            int daysToSubtract = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
            startOfWeek.add(Calendar.DATE, -daysToSubtract);
            startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
            startOfWeek.set(Calendar.MINUTE, 0);
            startOfWeek.set(Calendar.SECOND, 0);
            startOfWeek.set(Calendar.MILLISECOND, 0);


            Calendar endOfWeek = (Calendar) startOfWeek.clone();
            endOfWeek.add(Calendar.DATE, 6);
            endOfWeek.set(Calendar.HOUR_OF_DAY, 23);
            endOfWeek.set(Calendar.MINUTE, 59);
            endOfWeek.set(Calendar.SECOND, 59);

            String startDate = formatDate(startOfWeek.getTime());
            String endDate = formatDate(endOfWeek.getTime());

            


            Future<List<StepHistoryEntity>> future = executor.submit(() ->
                    stepHistoryDao.getHistoryBetweenDates(startDate, endDate));
            List<StepHistoryEntity> historyEntities = future.get();


            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (StepHistoryEntity entity : historyEntities) {
                Calendar entityDate = Calendar.getInstance();
                entityDate.setTime(sdf.parse(entity.getDate()));

                int entityDayOfWeek = entityDate.get(Calendar.DAY_OF_WEEK);
                int mondayBasedIndex = (entityDayOfWeek == Calendar.SUNDAY) ? 6 : entityDayOfWeek - 2;

                if (mondayBasedIndex >= 0 && mondayBasedIndex < 7) {
                    weeklySteps.set(mondayBasedIndex, entity.getStepCount());
                }
            }


            StepCounterManager stepCounterManager = StepCounterManager.getInstance(AppDatabase.getContext());
            int currentSteps = stepCounterManager.getStepsToday();

            Calendar today = Calendar.getInstance();
            int todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK);
            int todayIndex = (todayDayOfWeek == Calendar.SUNDAY) ? 6 : todayDayOfWeek - 2;

            if (todayIndex >= 0 && todayIndex < 7 && weeklySteps.get(todayIndex) < currentSteps) {
                weeklySteps.set(todayIndex, currentSteps);
                
            }

            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении истории шагов за неделю: " + e.getMessage(), e);
        }

        return weeklySteps;
    }


    public List<Integer> getStepsForCurrentMonth() {
        try {

            Calendar calendar = Calendar.getInstance();
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
            

            List<Integer> monthlySteps = new ArrayList<>();
            for (int i = 0; i < daysInMonth; i++) {
                monthlySteps.add(0);
            }
            

            Calendar startOfMonth = Calendar.getInstance();
            startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
            startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
            startOfMonth.set(Calendar.MINUTE, 0);
            startOfMonth.set(Calendar.SECOND, 0);
            startOfMonth.set(Calendar.MILLISECOND, 0);
            

            Calendar endOfMonth = Calendar.getInstance();
            endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
            endOfMonth.set(Calendar.MINUTE, 59);
            endOfMonth.set(Calendar.SECOND, 59);
            endOfMonth.set(Calendar.MILLISECOND, 999);
            
            String startDate = formatDate(startOfMonth.getTime());
            String endDate = formatDate(endOfMonth.getTime());
            
            
            

            Future<List<StepHistoryEntity>> future = executor.submit(() -> 
                stepHistoryDao.getHistoryBetweenDates(startDate, endDate));
            List<StepHistoryEntity> historyEntities = future.get();
            

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (StepHistoryEntity entity : historyEntities) {

                Calendar entityDate = Calendar.getInstance();
                entityDate.setTime(sdf.parse(entity.getDate()));
                

                int dayOfMonth = entityDate.get(Calendar.DAY_OF_MONTH);
                

                monthlySteps.set(dayOfMonth - 1, entity.getStepCount());
                
                
            }
            

            if (currentDay > 0 && currentDay <= daysInMonth) {

                int stepsToday = monthlySteps.get(currentDay - 1);
                if (stepsToday == 0) {

                    StepCounterManager stepCounterManager = StepCounterManager.getInstance(AppDatabase.getContext());
                    int currentSteps = stepCounterManager.getStepsToday();
                    

                    monthlySteps.set(currentDay - 1, currentSteps);
                    
                }
            }
            
            
            return monthlySteps;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении истории шагов за месяц: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    

    public void deleteOldHistory() {
        executor.execute(() -> {
            try {

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -30);
                String thirtyDaysAgo = formatDate(calendar.getTime());
                

                stepHistoryDao.deleteHistoryOlderThan(thirtyDaysAgo);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при удалении старой истории шагов: " + e.getMessage(), e);
            }
        });
    }
    

    

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }
}