package com.martist.vitamove.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.martist.vitamove.R;
import com.martist.vitamove.create_program.CreateProgramWeekActivity;
import com.martist.vitamove.fragments.AssistantFragment;
import com.martist.vitamove.fragments.CaloriesFragment;
import com.martist.vitamove.fragments.HomeFragment;
import com.martist.vitamove.fragments.ProfileFragment;
import com.martist.vitamove.fragments.WorkoutFragment;
import com.martist.vitamove.managers.DashboardManager;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.managers.StepCounterManager;
import com.martist.vitamove.utils.BMICalculator;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.viewmodels.ExerciseListViewModel;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private ImageView btnAssistant, btnWorkouts, btnHome, btnNutrition, btnProfile;
    private Fragment currentFragment;

    private static final String TAG = "MainActivity";
    private static final int ACTIVITY_RECOGNITION_PERMISSION_CODE = 100;

    private ExerciseListViewModel exerciseListViewModel;
    private StepCounterManager stepCounterManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        
        exerciseListViewModel = new ViewModelProvider(this).get(ExerciseListViewModel.class);

        
        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
        boolean isLogged = prefs.getBoolean("isLogged", false);
        boolean exercisesCached = prefs.getBoolean("exercises_cached", false);



        if (isFirstRun || !isLogged) {

            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkActivityRecognitionPermission();
        } else {
            
            initializeStepCounter();
        }

        
        if (!exercisesCached) {

            exerciseListViewModel.cacheExercisesAfterLogin().observe(this, isComplete -> {
                if (isComplete) {

                    
                    prefs.edit().putBoolean("exercises_cached", true).apply();
                }
            });
        } else {
            

            exerciseListViewModel.preloadCacheWithPriority();
        }

        
        syncFitnessGoals();

        
        FoodManager foodManager = FoodManager.getInstance(this);
        foodManager.refreshNutrientNorms();

        
        initNavigationButtons();

        
        String openTab = getIntent().getStringExtra("open_tab");
        boolean navigateToPrograms = getIntent().getBooleanExtra(CreateProgramWeekActivity.EXTRA_NAVIGATE_TO_PROGRAMS, false);

        if (navigateToPrograms) {
            
            loadFragment(new WorkoutFragment());
            btnWorkouts.setSelected(true);
            
            prefs.edit().putInt("workout_tab_index", 2).apply();
        } else if (openTab != null && openTab.equals("workout")) {
            
            loadFragment(new WorkoutFragment());
            btnWorkouts.setSelected(true);

            
            int workoutTabIndex = getIntent().getIntExtra("workout_tab_index", -1);
            String activeWorkoutId = getIntent().getStringExtra("active_workout_id"); 
            if (workoutTabIndex != -1) {
                
                SharedPreferences.Editor editor = prefs.edit().putInt("workout_tab_index", workoutTabIndex);
                if (activeWorkoutId != null) {
                    
                    editor.putString("active_workout_id_for_fragment", activeWorkoutId);
                }
                editor.apply();
            }
        } else if (savedInstanceState == null) {
            
            loadFragment(new HomeFragment());
            btnHome.setSelected(true);
        }
    }

    
    private void checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        ACTIVITY_RECOGNITION_PERMISSION_CODE);
            } else {
                
                initializeStepCounter();
            }
        } else {
            
            initializeStepCounter();
        }
    }

    private void initializeStepCounter() {
        
        stepCounterManager = StepCounterManager.getInstance(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                initializeStepCounter();
            } else {
                
                Toast.makeText(this, "Для отслеживания шагов необходимо разрешение на отслеживание активности",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initNavigationButtons() {

        View navAssistant = findViewById(R.id.nav_assistant);
        View navWorkouts = findViewById(R.id.nav_workouts);
        View navHome = findViewById(R.id.nav_home);
        View navNutrition = findViewById(R.id.nav_nutrition);
        View navProfile = findViewById(R.id.nav_profile);

        
        btnAssistant = findViewById(R.id.btn_assistant);
        btnWorkouts = findViewById(R.id.btn_workouts);
        btnHome = findViewById(R.id.btn_home);
        btnNutrition = findViewById(R.id.btn_nutrition);
        btnProfile = findViewById(R.id.btn_profile);

        navAssistant.setOnClickListener(this);
        navWorkouts.setOnClickListener(this);
        navHome.setOnClickListener(this);
        navNutrition.setOnClickListener(this);
        navProfile.setOnClickListener(this);
    }

    
    @Override
    public void onClick(View view) {
        
        resetButtonSelection();

        int id = view.getId();
        if (id == R.id.nav_assistant) {
            loadFragment(new AssistantFragment());
            btnAssistant.setSelected(true);
        } else if (id == R.id.nav_workouts) {
            loadFragment(new WorkoutFragment());
            btnWorkouts.setSelected(true);
        } else if (id == R.id.nav_home) {
            loadFragment(new HomeFragment());
            btnHome.setSelected(true);
        } else if (id == R.id.nav_nutrition) {
            loadFragment(new CaloriesFragment());
            btnNutrition.setSelected(true);
        } else if (id == R.id.nav_profile) {
            loadFragment(new ProfileFragment());
            btnProfile.setSelected(true);
        }
    }

    
    private void resetButtonSelection() {
        btnAssistant.setSelected(false);
        btnWorkouts.setSelected(false);
        btnHome.setSelected(false);
        btnNutrition.setSelected(false);
        btnProfile.setSelected(false);
    }

    
    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            currentFragment = fragment;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    
    private void syncFitnessGoals() {
        
        SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String userDataGoal = userDataPrefs.getString("fitness_goal", "");

        
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String appPrefsGoal = appPrefs.getString("fitness_goal", "weight_loss");

        
        if (!userDataGoal.isEmpty() && !userDataGoal.equals(appPrefsGoal)) {
            
            appPrefs.edit().putString("fitness_goal", userDataGoal).apply();

        }
        
        else if (userDataGoal.isEmpty() && !appPrefsGoal.isEmpty()) {
            userDataPrefs.edit().putString("fitness_goal", appPrefsGoal).apply();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        
        checkAndSyncUserData();

        
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        
        DashboardManager dashboardManager = DashboardManager.getInstance(this);
        dashboardManager.stopTracking();
    }

    
    private void syncUserDataWithSupabase() {


        
        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId == null) {

            return;
        }

        
        String name = prefs.getString("user_name", "");
        int age = prefs.getInt("user_age", 0);
        String gender = prefs.getString("user_gender", "");
        String fitnessGoal = prefs.getString("user_fitness_goal", "weight_loss");
        float height = prefs.getFloat("user_height", 0);
        float currentWeight = prefs.getFloat("user_current_weight", 0);
        float targetWeight = prefs.getFloat("user_target_weight", 0);
        String fitnessLevel = prefs.getString("user_fitness_level", "beginner");
        boolean isMetric = prefs.getBoolean("use_metric", true);

        
        if (name.isEmpty() || age == 0 || gender.isEmpty() || height == 0 || currentWeight == 0 || targetWeight == 0) {

            return;
        }

        
        String accessToken = prefs.getString("accessToken", null);
        String refreshToken = prefs.getString("refreshToken", null);

        if (accessToken == null || refreshToken == null) {
            Log.e(TAG, "Отсутствуют токены авторизации, синхронизация невозможна");
            return;
        }

        
        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                "qjopbdiafgbbstkwmhpt",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFqb3BiZGlhZmdiYnN0a3dtaHB0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTU1MDg4ODAsImV4cCI6MjAzMTA4NDg4MH0.F0XS4F4k31O7ciI43vYjzJFyK5wHHvlU0Jl2AFYZF4A"
        );

        
        supabaseClient.setUserToken(accessToken);
        supabaseClient.setRefreshToken(refreshToken);



        
        new Thread(() -> {
            try {
                boolean success = supabaseClient.updateUserProfile(
                        userId,
                        name,
                        age,
                        gender,
                        fitnessGoal,
                        height,
                        currentWeight,
                        targetWeight,
                        fitnessLevel,
                        isMetric
                );

                if (success) {


                    
                    SharedPreferences.Editor syncEditor = prefs.edit();
                    syncEditor.putBoolean("user_data_synced", true);
                    syncEditor.apply();

                    
                    SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
                    SharedPreferences.Editor userDataEditor = userDataPrefs.edit();

                    userDataEditor.putString("name", name);
                    userDataEditor.putInt("age", age);
                    userDataEditor.putString("gender", gender);
                    userDataEditor.putString("fitness_goal", fitnessGoal);
                    userDataEditor.putFloat("height", height);
                    userDataEditor.putFloat("current_weight", currentWeight);
                    userDataEditor.putFloat("target_weight", targetWeight);
                    userDataEditor.putString("fitness_level", fitnessLevel);
                    userDataEditor.putFloat("bmi", BMICalculator.calculateBMI(currentWeight, height));
                    userDataEditor.putBoolean("is_metric", isMetric);

                    userDataEditor.apply();


                } else {
                    Log.e(TAG, "Не удалось синхронизировать данные пользователя с Supabase");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при синхронизации данных пользователя: " + e.getMessage(), e);
            }
        }).start();
    }

    
    private void checkAndSyncUserData() {
        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        boolean dataSynced = prefs.getBoolean("user_data_synced", false);

        if (!dataSynced) {
            syncUserDataWithSupabase();
            
            prefs.edit().putBoolean("user_data_synced", true).apply();
        }
    }

    
    private void updateUI() {
        
        
    }

    
    public void updateNavigationHeader() {
        
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);

        String name = prefs.getString("name", "Пользователь");
        int calories = prefs.getInt("target_calories", 0);
        float targetWater = prefs.getFloat("target_water", 0);


        
        DashboardManager dashboardManager = DashboardManager.getInstance(this);
        dashboardManager.updateWaterGoalFromProfile();
        dashboardManager.updateCaloriesGoalFromProfile();

        
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            
            ((HomeFragment) currentFragment).updateDashboardData();

        }

        
        
        
        updateUI();
    }
} 