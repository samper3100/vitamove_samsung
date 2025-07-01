package com.martist.vitamove.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.viewmodels.HistoryViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialButton googleLoginButton;
    private TextView registerLink;
    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        setContentView(R.layout.activity_login);


        supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );

        initializeViews();
        setupClickListeners();
        
        
        clearPreviousUserCache();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        
        
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            
        }
    }
    
    private void clearPreviousUserCache() {
        try {
            
            SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
            String prevUserId = prefs.getString("userId", null);
            
            if (prevUserId != null && !prevUserId.isEmpty()) {

                
                
                HistoryViewModel.clearCache(getApplication(), prevUserId);
                
                
                com.martist.vitamove.managers.CaloriesManager.resetInstance();
                com.martist.vitamove.managers.FoodManager.resetInstance();
                com.martist.vitamove.managers.DashboardManager.resetInstance();
                

                
                
                
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке кэша: " + e.getMessage());
        }
    }

    private void initializeViews() {

        emailInput = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);
    }

    private void setupClickListeners() {

        loginButton.setOnClickListener(v -> attemptLogin());



        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SurveyActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        
        if (email.isEmpty()) {
            showError("Пожалуйста, введите email");
            return;
        }

        if (password.isEmpty()) {
            showError("Пожалуйста, введите пароль");
            return;
        }

        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Пожалуйста, введите корректный email адрес");
            return;
        }

        
        loginButton.setEnabled(false);
        
        
        new Thread(() -> {
            try {
                
                try {
                    com.martist.vitamove.managers.FoodManager.resetInstance();
                    com.martist.vitamove.managers.CaloriesManager.resetInstance();
                    com.martist.vitamove.managers.DashboardManager.resetInstance();

                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при сбросе менеджеров", e);
                }
                
                
                String responseJson = supabaseClient.signIn(email, password);
                JSONObject jsonResponse = new JSONObject(responseJson);
                
                
                String accessToken = jsonResponse.getString("access_token");
                String refreshToken = jsonResponse.optString("refresh_token", "");
                
                
                String userId = null;
                try {
                    String[] jwtParts = accessToken.split("\\.");
                    if (jwtParts.length > 1) {
                        String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                        JSONObject jwtJson = new JSONObject(payload);
                        userId = jwtJson.getString("sub");

                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка получения ID пользователя из токена", e);
                }
                
                final String finalUserId = userId;
                
                runOnUiThread(() -> {
                    try {
                        
                        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLogged", true)
                                .putBoolean("isFirstRun", false)
                                .putString("accessToken", accessToken)
                                .putString("refreshToken", refreshToken)
                                .putString("userId", finalUserId)
                                .putString("userEmail", email)
                                .apply();

                        
                        loadUserProfileFromSupabase(finalUserId);

                        Toast.makeText(this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show();
                        
                        
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка сохранения данных: " + e.getMessage());
                        showError("Ошибка сохранения данных: " + e.getMessage());
                        loginButton.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Login error: " + e.getMessage());
                String errorMessage = e.getMessage();
                
                
                String userFriendlyMessage;
                if (errorMessage.contains("Invalid login credentials")) {
                    userFriendlyMessage = "Неверный email или пароль. Пожалуйста, проверьте данные и попробуйте снова.";
                } else if (errorMessage.contains("Email not confirmed")) {
                    userFriendlyMessage = "Email не подтвержден. Проверьте вашу почту и подтвердите регистрацию.";
                } else if (errorMessage.contains("User not found")) {
                    userFriendlyMessage = "Пользователь с таким email не найден. Возможно, вы еще не зарегистрированы.";
                } else if (errorMessage.contains("not activated")) {
                    userFriendlyMessage = "Ваша учетная запись не активирована. Пожалуйста, свяжитесь с поддержкой.";
                } else {
                    userFriendlyMessage = "Ошибка входа: " + errorMessage;
                }
                
                final String finalMessage = userFriendlyMessage;
                runOnUiThread(() -> {
                    showError(finalMessage);
                    loginButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    
    private void loadUserProfileFromSupabase(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "loadUserProfileFromSupabase: userId пустой или null");
            return;
        }
        
        
        new Thread(() -> {
            try {

                
                
                JSONArray result = supabaseClient.from("users")
                        .select("*")
                        .eq("id", userId)
                        .executeAndGetArray();
                
                if (result.length() > 0) {
                    
                    JSONObject userProfile = result.getJSONObject(0);
                    
                    
                    SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
                    SharedPreferences.Editor editor = userDataPrefs.edit();
                    
                    
                    editor.putString("name", userProfile.optString("name", ""));
                    editor.putInt("age", userProfile.optInt("age", 30));
                    editor.putString("gender", userProfile.optString("gender", ""));
                    editor.putString("fitness_goal", userProfile.optString("fitness_goal", ""));
                    
                    
                    editor.putFloat("height", (float) userProfile.optDouble("height", 0));
                    editor.putFloat("current_weight", (float) userProfile.optDouble("current_weight", 0));
                    editor.putFloat("target_weight", (float) userProfile.optDouble("target_weight", 0));
                    editor.putFloat("body_fat", (float) userProfile.optDouble("body_fat", 0));
                    editor.putFloat("waist", (float) userProfile.optDouble("waist", 0));
                    
                    
                    editor.putFloat("bmi", (float) userProfile.optDouble("bmi", 0));
                    
                    
                    editor.putString("fitness_level", userProfile.optString("fitness_level", ""));
                    editor.putBoolean("is_metric", userProfile.optBoolean("is_metric", true));
                    
                    
                    editor.putInt("target_calories", userProfile.optInt("target_calories", 0));
                    
                    
                    editor.putFloat("target_water", (float) userProfile.optDouble("target_water", 0));
                    
                    
                    editor.putBoolean("is_synchronized", true);
                    
                    editor.apply();
                    
                    
                    try {
                        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor appEditor = appPrefs.edit();
                        
                        
                        appEditor.putString("fitness_goal", userProfile.optString("fitness_goal", "weight_loss"));
                        appEditor.putString("user_fitness_level", userProfile.optString("fitness_level", "intermediate"));
                        appEditor.apply();
                        

                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при синхронизации настроек между SharedPreferences: " + e.getMessage(), e);
                    }
                    


                    
                    
                    runOnUiThread(() -> {
                        com.martist.vitamove.managers.FoodManager.getInstance(LoginActivity.this).refreshNutrientNorms();
                    });

                    
                    try {
                        com.martist.vitamove.workout.data.repository.WorkoutRepository workoutRepository = 
                            ((VitaMoveApplication) getApplication()).getWorkoutRepository();
                        
                        if (workoutRepository instanceof com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository) {

                            
                            com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository supabaseRepo = 
                                (com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository) workoutRepository;

                            supabaseRepo.syncUserWorkouts(
                                    userId,
                                    () -> Log.d(TAG, "Синхронизация тренировок успешно завершена"),
                                    new com.martist.vitamove.utils.Callback<Exception>() {
                                        @Override
                                        public void call(Exception error) {
                                            Log.e(TAG, "Ошибка при синхронизации тренировок: " + error.getMessage(), error);
                                        }
                                    }
                            );
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при синхронизации тренировок: " + e.getMessage(), e);
                    }

                } else {

                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке профиля: " + e.getMessage(), e);
            }
        }).start();
    }
}