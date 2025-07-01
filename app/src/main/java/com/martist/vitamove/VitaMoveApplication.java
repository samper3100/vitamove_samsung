package com.martist.vitamove;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.martist.vitamove.db.AppDatabase;
import com.martist.vitamove.utils.AuthManager;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;


public class VitaMoveApplication extends Application {
    private static final String TAG = "VitaMoveApplication";
    public static Context context;
    private WorkoutRepository workoutRepository;
    public static final String PREFS_NAME = "VitaMovePrefs";
    private static AuthManager authManager;
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        context = getApplicationContext();




        preloadParcelableClasses();
        

        database = AppDatabase.getInstance(this);
        

        SupabaseClient supabaseClient = SupabaseClient.getInstance(
            Constants.SUPABASE_CLIENT_ID,
            Constants.SUPABASE_CLIENT_SECRET
        );
        

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String accessToken = prefs.getString("accessToken", null);
        String refreshToken = prefs.getString("refreshToken", null);
        

        authManager = AuthManager.getInstance(supabaseClient);
        
        if (accessToken != null && refreshToken != null) {

            supabaseClient.setUserToken(accessToken);
            supabaseClient.setRefreshToken(refreshToken);
        }
        

        supabaseClient.initializeWithInterceptor(authManager);
        

        if (accessToken != null && refreshToken != null) {
            new Thread(() -> {
                try {
                    boolean isValid = authManager.ensureValidToken();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при проверке токенов: " + e.getMessage(), e);
                }
            }).start();
        }
        

        workoutRepository = new SupabaseWorkoutRepository(supabaseClient);
        

        initializeAppSettings();
        

        if (getCurrentUserId() != null) {
            initializeWorkoutCleanup();
        }
    }

    public static Context getContext() {
        return context;
    }
    

    public static Context getAppContext() {
        return context;
    }
    

    public static AppDatabase getDatabase() {
        return database;
    }

    public WorkoutRepository getWorkoutRepository() {
        return workoutRepository;
    }

    public String getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("userId", null);
    }
    

    public static AuthManager getAuthManager() {
        return authManager;
    }


    private void initializeAppSettings() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        

        String darkModeValue = sharedPreferences.getString("dark_mode", "system");
        applyDarkMode(darkModeValue);
    }
    

    private void applyDarkMode(String darkModeValue) {
        int nightMode;
        switch (darkModeValue) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "system":
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }


    private void preloadParcelableClasses() {
        Thread preloadThread = new Thread(() -> {
            try {

                
                

                try {
                    Class<?> foodClass = Class.forName("com.martist.vitamove.models.Food");
                    
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "ClassNotFoundException при загрузке Food (способ 1): " + e.getMessage(), e);
                }
                

                try {
                    com.martist.vitamove.models.Food.ensureClassLoaded();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при загрузке Food (способ 2): " + e.getMessage(), e);
                }
                

                try {

                    com.martist.vitamove.models.Food.Builder builder = new com.martist.vitamove.models.Food.Builder();
                    com.martist.vitamove.models.Food testFood = builder
                            .id(1)
                            .name("TestFood")
                            .calories(100)
                            .build();
                    

                    Bundle bundle = new Bundle();
                    bundle.putParcelable("testFood", testFood);
                    

                    com.martist.vitamove.models.Food retrievedFood = bundle.getParcelable("testFood");
                    
                    if (retrievedFood != null) {
                        
                    } else {
                        Log.e(TAG, "Ошибка при тестовой десериализации Food: объект null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при тестовой сериализации/десериализации Food: " + e.getMessage(), e);
                }
                

                if (com.martist.vitamove.models.Food.isClassLoaded()) {
                    
                } else {
                    
                }
                
                
            } catch (Exception e) {
                Log.e(TAG, "Общая ошибка при загрузке классов Parcelable: " + e.getMessage(), e);
            }
        });
        

        preloadThread.setPriority(Thread.MAX_PRIORITY);
        preloadThread.start();
        

        try {
            preloadThread.join(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Прерывание при ожидании загрузки классов: " + e.getMessage());
        }
    }


    private void initializeWorkoutCleanup() {
        new Thread(() -> {
            try {
                String userId = getCurrentUserId();
                if (userId != null && !userId.isEmpty()) {
                    

                    workoutRepository.getTodayWorkoutPlan(userId);
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при проверке невыполненных тренировок: " + e.getMessage(), e);
            }
        }).start();
    }
} 