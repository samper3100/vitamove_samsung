package com.martist.vitamove.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Calendar;


public class StepCounterManager implements SensorEventListener {
    private static final String TAG = "StepCounterManager";
    private static StepCounterManager instance;
    
    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor stepSensor;
    private final SharedPreferences sharedPreferences;
    
    private static final String PREFS_NAME = "step_counter_prefs";
    private static final String KEY_INITIAL_STEPS = "initial_steps";
    private static final String KEY_PREVIOUS_STEPS = "previous_steps";
    private static final String KEY_CURRENT_STEPS = "current_steps";
    private static final String KEY_LAST_SAVE_DATE = "last_save_date";
    private static final String KEY_STEPS_BEFORE_REBOOT = "steps_before_reboot";
    
    private int initialSteps = -1;
    private int currentSteps = 0;
    private int stepsToday = 0;
    private int stepsBeforeReboot = 0; 
    private boolean isListening = false;
    
    private StepCounterManager(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        
        loadSavedData();
    }
    
    public static synchronized StepCounterManager getInstance(Context context) {
        if (instance == null) {
            instance = new StepCounterManager(context);
        }
        return instance;
    }
    
    
    public boolean startTracking() {
        if (stepSensor == null) {
            Log.e(TAG, "Сенсор шагомера не доступен на этом устройстве");
            return false;
        }
        
        if (!isListening) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isListening = true;
            
        }
        
        return true;
    }
    
    
    public void stopTracking() {
        if (isListening) {
            sensorManager.unregisterListener(this);
            isListening = false;
            
            
            saveCurrentSteps();
            
            
        }
    }
    
    
    public int getStepsToday() {
        
        return Math.max(0, stepsToday);
    }
    
    
    private void loadSavedData() {
        initialSteps = sharedPreferences.getInt(KEY_INITIAL_STEPS, -1);
        currentSteps = sharedPreferences.getInt(KEY_CURRENT_STEPS, 0);
        stepsToday = sharedPreferences.getInt(KEY_PREVIOUS_STEPS, 0);
        stepsBeforeReboot = sharedPreferences.getInt(KEY_STEPS_BEFORE_REBOOT, 0);
        
        
        long lastSaveDate = sharedPreferences.getLong(KEY_LAST_SAVE_DATE, 0);
        if (!isSameDay(lastSaveDate, System.currentTimeMillis())) {
            
            initialSteps = currentSteps; 
            stepsToday = 0;
            stepsBeforeReboot = 0; 
            saveData();
        }
    }
    
    
    private void saveCurrentSteps() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_PREVIOUS_STEPS, stepsToday);
        editor.putInt(KEY_CURRENT_STEPS, currentSteps);
        editor.putLong(KEY_LAST_SAVE_DATE, System.currentTimeMillis());
        editor.apply();
        
        
    }
    
    
    private void saveData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_INITIAL_STEPS, initialSteps);
        editor.putInt(KEY_PREVIOUS_STEPS, stepsToday);
        editor.putInt(KEY_CURRENT_STEPS, currentSteps);
        editor.putInt(KEY_STEPS_BEFORE_REBOOT, stepsBeforeReboot);
        editor.putLong(KEY_LAST_SAVE_DATE, System.currentTimeMillis());
        editor.apply();
        
        
    }
    
    
    private boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            
            int sensorSteps = (int) event.values[0];
            
            if (initialSteps == -1) {
                
                initialSteps = sensorSteps;
                stepsBeforeReboot = 0; 
                saveData();
                
            }

            
            if (sensorSteps < initialSteps) {
                
                
                stepsBeforeReboot = stepsToday;
                
                initialSteps = sensorSteps;
                
                saveData();
            }
            
            
            currentSteps = sensorSteps;
            stepsToday = (currentSteps - initialSteps) + stepsBeforeReboot;
            
            
            if (stepsToday % 10 == 0) { 
                
            }
            
            
            if (stepsToday % 100 == 0) {
                saveCurrentSteps();
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    }
} 