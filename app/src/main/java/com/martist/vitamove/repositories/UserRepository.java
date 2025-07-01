package com.martist.vitamove.repositories;

import android.content.Context;

import com.martist.vitamove.models.UserProfile;


public class UserRepository {
    private static final String PREFS_NAME = "VitaMoveUserPrefs";
    private final Context context;
    private static final String TAG = "UserRepository";
    

    public UserRepository(Context context) {
        this.context = context;
    }
    

    public UserProfile getCurrentUserProfile() {

        android.content.SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        
        String name = prefs.getString("name", "Пользователь");
        int age = prefs.getInt("age", 30);
        String gender = prefs.getString("gender", "Мужчина");
        float currentWeight = prefs.getFloat("current_weight", 75.0f);
        float targetWeight = prefs.getFloat("target_weight", 70.0f);
        float height = prefs.getFloat("height", 170.0f);
        float bodyFat = prefs.getFloat("body_fat", 20.0f);
        float waist = prefs.getFloat("waist", 80.0f);
        String fitnessGoal = prefs.getString("fitness_goal", "weight_loss");
        String fitnessLevel = prefs.getString("fitness_level", "intermediate");
        int targetCalories = prefs.getInt("target_calories", 0);
        float targetWater = prefs.getFloat("target_water", 0);
        boolean isFromSupabase = prefs.getBoolean("is_synchronized", false);
        
        UserProfile userProfile = new UserProfile(name, age, gender, currentWeight, targetWeight, height, bodyFat, waist);
        

        if (isFromSupabase && targetCalories > 0) {
            userProfile.setTargetCalories(targetCalories);
        }
        
        if (isFromSupabase && targetWater > 0) {
            userProfile.setTargetWater(targetWater);
        }
        

        
        
        return userProfile;
    }
    

    

} 