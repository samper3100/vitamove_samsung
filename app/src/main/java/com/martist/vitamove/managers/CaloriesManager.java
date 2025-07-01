package com.martist.vitamove.managers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


public class CaloriesManager {
    private static final String TAG = "CaloriesManager";
    private static final String PREFS_NAME = "calories_data";
    private static final String KEY_COMPLETED_CALORIES = "completed_workout_calories";
    private static final String KEY_ACTIVE_WORKOUT_CALORIES = "active_workout_calories";
    private static final String KEY_CONSUMED_CALORIES = "consumed_calories";
    private static final String KEY_DATE = "calories_date";

    private static CaloriesManager instance;
    private final SharedPreferences prefs;
    private final Context context;
    private final MutableLiveData<Integer> burnedCaloriesLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> consumedCaloriesLiveData = new MutableLiveData<>(0);

    
    public static synchronized CaloriesManager getInstance(Context context) {
        if (instance == null) {
            instance = new CaloriesManager(context.getApplicationContext());
        }
        return instance;
    }
    
    
    public static synchronized void resetInstance() {
        if (instance != null) {

            instance = null;
        }
    }

    private CaloriesManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        
        resetActiveWorkoutCalories();
        
        
        checkDateAndResetIfNeeded();
        
        
        int completedCalories = getCompletedWorkoutCalories();
        int activeWorkoutCalories = getActiveWorkoutCalories();
        int totalBurnedCalories = completedCalories + activeWorkoutCalories;
        int consumedCalories = getConsumedCalories();
        
        burnedCaloriesLiveData.setValue(totalBurnedCalories);
        consumedCaloriesLiveData.setValue(consumedCalories);
        


    }

    
    public LiveData<Integer> getBurnedCaloriesLiveData() {
        return burnedCaloriesLiveData;
    }
    
    
    public LiveData<Integer> getConsumedCaloriesLiveData() {
        return consumedCaloriesLiveData;
    }

    
    public void addCompletedWorkoutCalories(int calories) {
        if (calories <= 0) {

            return;
        }
        
        int currentCalories = getCompletedWorkoutCalories();
        int newTotal = currentCalories + calories;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COMPLETED_CALORIES, newTotal);
        editor.apply();
        

        
        
        updateBurnedCaloriesLiveData();
    }

    
    
    public void setConsumedCalories(int calories) {
        if (calories < 0) {

            return;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CONSUMED_CALORIES, calories);
        editor.apply();
        

        
        
        updateConsumedCaloriesLiveData();
    }

    
    public void updateActiveWorkoutCalories(int calories) {
        if (calories < 0) {

            return;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_ACTIVE_WORKOUT_CALORIES, calories);
        editor.apply();
        

        
        
        updateBurnedCaloriesLiveData();
    }

    
    public void resetActiveWorkoutCalories() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_ACTIVE_WORKOUT_CALORIES, 0);
        editor.apply();
        

        
        
        updateBurnedCaloriesLiveData();
    }

    
    public int getCompletedWorkoutCalories() {
        return prefs.getInt(KEY_COMPLETED_CALORIES, 0);
    }

    
    public int getActiveWorkoutCalories() {
        return prefs.getInt(KEY_ACTIVE_WORKOUT_CALORIES, 0);
    }
    
    
    public int getConsumedCalories() {
        return prefs.getInt(KEY_CONSUMED_CALORIES, 0);
    }

    
    public int getTotalBurnedCalories() {
        return getCompletedWorkoutCalories() + getActiveWorkoutCalories();
    }

    
    private void updateBurnedCaloriesLiveData() {
        int totalCalories = getTotalBurnedCalories();
        burnedCaloriesLiveData.postValue(totalCalories);

    }
    
    
    private void updateConsumedCaloriesLiveData() {
        int calories = getConsumedCalories();
        consumedCaloriesLiveData.postValue(calories);

    }

    
    private void checkDateAndResetIfNeeded() {
        String savedDateStr = prefs.getString(KEY_DATE, "");
        String currentDateStr = java.time.LocalDate.now().toString();
        
        if (!currentDateStr.equals(savedDateStr)) {
            

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_COMPLETED_CALORIES, 0);
            editor.putInt(KEY_ACTIVE_WORKOUT_CALORIES, 0);
            editor.putInt(KEY_CONSUMED_CALORIES, 0);
            editor.putString(KEY_DATE, currentDateStr);
            editor.apply();
        }
    }

    
    public void setTargetCalories(int targetCalories) {
        if (targetCalories <= 0) {

            return;
        }
        
        
        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        userPrefs.edit().putInt("target_calories", targetCalories).apply();
        

        
        
    }
} 