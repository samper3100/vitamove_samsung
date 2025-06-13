package com.martist.vitamove.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.models.DashboardData;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.repositories.StepHistoryRepository;
import com.martist.vitamove.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class DashboardManager {
    private static final String TAG = "DashboardManager";
    
    private static DashboardManager instance;
    private Context context;
    private SharedPreferences sharedPreferences;
    private UserRepository userRepository;
    private StepCounterManager stepCounterManager;
    private StepHistoryRepository stepHistoryRepository;
    private WaterHistoryManager waterHistoryManager;
    

    private static final String PREF_FILE = "dashboard_prefs";
    private static final String KEY_STEPS_GOAL = "steps_goal";
    private static final String KEY_WATER_GOAL = "water_goal";
    private static final String KEY_WEIGHT = "current_weight";
    private static final String KEY_PROTEIN_GOAL = "protein_goal";
    private static final String KEY_FATS_GOAL = "fats_goal";
    private static final String KEY_CARBS_GOAL = "carbs_goal";
    private static final String KEY_WATER_CONSUMED = "water_consumed";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    


    private int stepsToday = 0;
    private int stepsGoal = 10000;
    private float stepsProgress = 0.0f;
    private int caloriesGoal = 3178;
    private int caloriesConsumed = 0;
    private int caloriesBurned = 527;
    private float waterConsumed = 0.0f;
    private float waterGoal = 2.5f;
    private float currentWeight = 78.5f;
    private float weightChange = -0.5f;
    

    private int proteinConsumed = 0;
    private int fatsConsumed = 0;
    private int carbsConsumed = 0;
    private int proteinGoal = 120;
    private int fatsGoal = 70;
    private int carbsGoal = 250;
    

    public static synchronized DashboardManager getInstance(Context context) {
        if (instance == null) {
            instance = new DashboardManager(context.getApplicationContext());
        }
        return instance;
    }
    

    public static synchronized void resetInstance() {
        if (instance != null) {
            
            instance = null;
        }
    }
    

    private DashboardManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        this.userRepository = new UserRepository(context);
        this.stepHistoryRepository = StepHistoryRepository.getInstance(context);
        this.waterHistoryManager = WaterHistoryManager.getInstance(context);
        

        stepHistoryRepository.deleteOldHistory();
        

        this.stepCounterManager = StepCounterManager.getInstance(context);
        boolean sensorAvailable = this.stepCounterManager.startTracking();
        if (!sensorAvailable) {
            
        }
        

        stepsGoal = sharedPreferences.getInt(KEY_STEPS_GOAL, 10000);
        waterGoal = sharedPreferences.getFloat(KEY_WATER_GOAL, 2.5f);
        currentWeight = sharedPreferences.getFloat(KEY_WEIGHT, 78.5f);
        

        waterConsumed = waterHistoryManager.getTotalWaterConsumption();
        

        proteinGoal = sharedPreferences.getInt(KEY_PROTEIN_GOAL, 120);
        fatsGoal = sharedPreferences.getInt(KEY_FATS_GOAL, 70);
        carbsGoal = sharedPreferences.getInt(KEY_CARBS_GOAL, 250);
        

        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        caloriesGoal = userPrefs.getInt("target_calories", 3178);
        

        updateWaterGoalFromProfile();
        

        checkAndResetDailyDataIfNeeded();
        

        syncStepsData();
        


    }
    

    public DashboardData getDashboardData() {


        

        syncStepsData();
        

        waterConsumed = waterHistoryManager.getTotalWaterConsumption();
        
        DashboardData data = new DashboardData();
        

        data.setStepsToday(stepsToday);
        data.setStepsGoal(stepsGoal);
        data.setStepsProgress(stepsProgress);
        data.setStepsHistory(generateStepsHistory());
        

        data.setCaloriesGoal(caloriesGoal);
        data.setCaloriesConsumed(caloriesConsumed);
        data.setCaloriesBurned(caloriesBurned);
        

        data.setWaterConsumed(waterConsumed);
        data.setWaterGoal(waterGoal);
        

        data.setCurrentWeight(currentWeight);
        data.setWeightChange(weightChange);
        data.setWeightHistory(generateWeightHistory());
        

        data.setProteinConsumed(proteinConsumed);
        data.setFatsConsumed(fatsConsumed);
        data.setCarbsConsumed(carbsConsumed);
        data.setProteinGoal(proteinGoal);
        data.setFatsGoal(fatsGoal);
        data.setCarbsGoal(carbsGoal);
        
        return data;
    }
    

    public void addWaterConsumption(float amount) {

        waterConsumed = waterConsumed + amount;
        

        sharedPreferences.edit().putFloat(KEY_WATER_CONSUMED, waterConsumed).apply();
        

    }



    private List<Integer> generateStepsHistory() {

        List<Integer> weeklySteps = stepHistoryRepository.getStepsForLastWeek();
        

        if (weeklySteps.isEmpty()) {
            
            return new ArrayList<>(Arrays.asList(9500, 9200, 10800, 11500, 6800, 8400, stepsToday));
        }
        
        return weeklySteps;
    }
    

    private List<Float> generateWeightHistory() {
        return new ArrayList<>(Arrays.asList(80.2f, 79.8f, 79.5f, 79.1f, 78.7f, currentWeight));
    }
    

    public void syncStepsData() {

        if (stepCounterManager != null) {

            stepsToday = stepCounterManager.getStepsToday();
            

            stepsProgress = (float) stepsToday / stepsGoal;
            

            stepHistoryRepository.saveStepsForToday(stepsToday);
            
            
        } else {
            
        }
    }
    

    public void syncStepsForStatistics() {

        syncStepsData();
        

        
    }
    

    

    public void resetDailyData() {
        stepsToday = 0;
        waterConsumed = 0.0f;
        sharedPreferences.edit().putFloat(KEY_WATER_CONSUMED, waterConsumed).apply();
        caloriesConsumed = 0;
        proteinConsumed = 0;
        fatsConsumed = 0;
        carbsConsumed = 0;
        

        waterHistoryManager.resetDailyData();
    }
    

    public void stopTracking() {
        if (stepCounterManager != null) {
            stepCounterManager.stopTracking();
        }
    }
    

    public void updateWaterGoalFromProfile() {
        UserProfile profile = userRepository.getCurrentUserProfile();
        if (profile != null) {
            float oldWaterGoal = waterGoal;
            waterGoal = profile.getTargetWater();
            

            sharedPreferences.edit().putFloat(KEY_WATER_GOAL, waterGoal).apply();
            

            
        }
    }
    

    public void updateCaloriesGoalFromProfile() {
        UserProfile profile = userRepository.getCurrentUserProfile();
        if (profile != null) {
            int oldCaloriesGoal = caloriesGoal;
            caloriesGoal = profile.getTargetCalories();
            

            sharedPreferences.edit().putInt("target_calories", caloriesGoal).apply();
            

            CaloriesManager caloriesManager = CaloriesManager.getInstance(context);
            caloriesManager.setTargetCalories(caloriesGoal);
            

            
        }
    }
    

    public void checkAndResetDailyDataIfNeeded() {
        try {

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long todayInMillis = today.getTimeInMillis();
            

            long lastResetDate = sharedPreferences.getLong(KEY_LAST_RESET_DATE, 0);
            

            if (lastResetDate < todayInMillis) {

                resetDailyData();
                

                sharedPreferences.edit().putLong(KEY_LAST_RESET_DATE, todayInMillis).apply();
                
                
            } else {
                
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке даты для сброса данных: " + e.getMessage(), e);
        }
    }
} 