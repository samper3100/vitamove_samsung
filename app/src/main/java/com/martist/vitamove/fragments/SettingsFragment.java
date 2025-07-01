package com.martist.vitamove.fragments;

import static android.content.Context.MODE_PRIVATE;
import static com.martist.vitamove.VitaMoveApplication.context;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.martist.vitamove.R;
import com.martist.vitamove.activities.OnboardingActivity;
import com.martist.vitamove.managers.DashboardManager;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.repositories.UserRepository;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;

public  class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        
        initPreferences();

        
        Preference deleteAccountPref = findPreference("delete_account");
        if (deleteAccountPref != null) {
            deleteAccountPref.setOnPreferenceClickListener(preference -> {
                showDeleteAccountDialog();
                return true;
            });
        }
    }

    
    private void initPreferences() {
        
        PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences, false);

        
        updatePreferenceSummaries();
    }

    
    private void updatePreferenceSummaries() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        
        ListPreference goalPref = findPreference("fitness_goal");
        if (goalPref != null) {
            String value = sharedPreferences.getString("fitness_goal", "weight_loss");
            switch (value) {
                case "weight_loss":
                    goalPref.setSummary("Снижение веса");
                    break;
                case "muscle_gain":
                    goalPref.setSummary("Набор мышечной массы");
                    break;
                case "endurance":
                    goalPref.setSummary("Улучшение выносливости");
                    break;
                default:
                    goalPref.setSummary("Общая физическая подготовка");
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    
    public void unregisterListener() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        
        updatePreferenceSummaries();

        
        if (key.equals("notifications_enabled")) {
            boolean notificationsEnabled = sharedPreferences.getBoolean(key, true);

            
            
            

            
            Toast.makeText(requireContext(),
                    notificationsEnabled ? "Уведомления включены" : "Уведомления отключены",
                    Toast.LENGTH_SHORT).show();
        }
        
        else if (key.equals("dark_mode")) {
            String darkModeValue = sharedPreferences.getString(key, "system");
            applyDarkMode(darkModeValue);

            Toast.makeText(requireContext(), "Тема изменена", Toast.LENGTH_SHORT).show();
        }
        
        else if (key.equals("fitness_goal")) {
            String fitnessGoal = sharedPreferences.getString(key, "weight_loss");

            
            FoodManager foodManager = FoodManager.getInstance(requireContext());
            foodManager.updateFitnessGoal(fitnessGoal);

            
            SharedPreferences userPrefs = requireContext().getSharedPreferences("user_data", MODE_PRIVATE);
            userPrefs.edit().putString("fitness_goal", fitnessGoal).apply();

            
            UserRepository userRepository =
                    new UserRepository(requireContext());
            UserProfile userProfile = userRepository.getCurrentUserProfile();

            if (userProfile != null) {
                
                userProfile.updateTargetCalories();
                userProfile.updateTargetWater();

                
                int updatedTargetCalories = userProfile.getTargetCalories();
                float updatedTargetWater = userProfile.getTargetWater();

                SharedPreferences.Editor editor = userPrefs.edit();
                editor.putInt("target_calories", updatedTargetCalories);
                editor.putFloat("target_water", updatedTargetWater);
                editor.apply();

                
                DashboardManager dashboardManager =
                        DashboardManager.getInstance(requireContext());
                dashboardManager.updateCaloriesGoalFromProfile();
                dashboardManager.updateWaterGoalFromProfile();

                

                
                if (getActivity() != null) {
                    
                    if (getActivity() instanceof com.martist.vitamove.activities.MainActivity) {
                        
                        ((com.martist.vitamove.activities.MainActivity) getActivity()).updateNavigationHeader();
                    }
                }
                
                String fitnessLevel = PreferenceManager.getDefaultSharedPreferences(context)
                                .getString("user_fitness_level", "intermediate");
                syncUpdatedDataWithSupabase(userProfile, fitnessGoal, fitnessLevel);
            }

            
            String goalMessage;
            switch (fitnessGoal) {
                case "weight_loss":
                    goalMessage = "Цель изменена: снижение веса. Обновлены нормы калорий и воды.";
                    break;
                case "muscle_gain":
                    goalMessage = "Цель изменена: набор мышечной массы. Обновлены нормы калорий и воды.";
                    break;
                case "endurance":
                    goalMessage = "Цель изменена: повышение выносливости. Обновлены нормы калорий и воды.";
                    break;
                default:
                    goalMessage = "Цель изменена: общий фитнес. Обновлены нормы калорий и воды.";
                    break;
            }
            Toast.makeText(requireContext(), goalMessage, Toast.LENGTH_LONG).show();
        }
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

    
    private void showDeleteAccountDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить свой аккаунт? Это действие нельзя отменить. Все ваши данные будут удалены.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteAccount();
                })
                .setNegativeButton("Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    
    private void deleteAccount() {
        Context context = requireContext();

        
        Toast.makeText(context, "Удаление аккаунта...", Toast.LENGTH_SHORT).show();

        try {
            
            SharedPreferences appPrefs = context.getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
            String userId = appPrefs.getString("userId", null);

            if (userId != null) {
                
                SupabaseClient supabaseClient =
                        SupabaseClient.getInstance(
                                "qjopbdiafgbbstkwmhpt",
                                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFqb3BiZGlhZmdiYnN0a3dtaHB0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTU1MDg4ODAsImV4cCI6MjAzMTA4NDg4MH0.F0XS4F4k31O7ciI43vYjzJFyK5wHHvlU0Jl2AFYZF4A"
                        );

                
                

                supabaseClient.deleteUserAccount(userId, new SupabaseClient.AsyncCallback() {
                    @Override
                    public void onSuccess(String responseBody) {
                        
                        

                        
                        clearLocalUserData(context);

                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(context, "Аккаунт успешно удален", Toast.LENGTH_LONG).show();

                                
                                Intent intent = new Intent(context, OnboardingActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);

                                
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        
                        Log.e("SettingsActivity", "Ошибка при удалении аккаунта: " + e.getMessage(), e);

                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(context, "Произошла ошибка при удалении аккаунта: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(context, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при удалении аккаунта: " + e.getMessage(), e);
            Toast.makeText(context, "Произошла ошибка при удалении аккаунта", Toast.LENGTH_SHORT).show();
        }
    }

    
    private void clearLocalUserData(Context context) {
        try {
            
            context.getSharedPreferences("user_data", MODE_PRIVATE).edit().clear().apply();
            context.getSharedPreferences("VitaMovePrefs", MODE_PRIVATE).edit().clear().apply();
            context.getSharedPreferences("workout_history_cache", MODE_PRIVATE).edit().clear().apply();

            
            com.martist.vitamove.managers.FoodManager.resetInstance();
            com.martist.vitamove.managers.CaloriesManager.resetInstance();
            com.martist.vitamove.managers.DashboardManager.resetInstance();

            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке локальных данных: " + e.getMessage(), e);
        }
    }

    
    private void syncUpdatedDataWithSupabase(UserProfile userProfile, String fitnessGoal, String fitnessLevel) {
        try {
            
            SharedPreferences appPrefs = requireContext().getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
            
            String userId = appPrefs.getString("userId", null);
            boolean isMetric = appPrefs.getBoolean("use_metric", true);
            
            if (userId == null || userId.isEmpty()) {
                
                return;
            }

            
            SupabaseClient supabaseClient = SupabaseClient.getInstance(
                    Constants.SUPABASE_CLIENT_ID,
                    Constants.SUPABASE_CLIENT_SECRET
            );
            
            
            String accessToken = appPrefs.getString("accessToken", null);
            String refreshToken = appPrefs.getString("refreshToken", null);
            
            if (accessToken == null || refreshToken == null) {
                
                return;
            }
            
            supabaseClient.setUserToken(accessToken);
            supabaseClient.setRefreshToken(refreshToken);
            
            
            new Thread(() -> {
                try {
                    
                    boolean success = supabaseClient.updateUserProfile(
                            userId,
                            userProfile.getName(),
                            userProfile.getAge(),
                            userProfile.getGender(),
                            fitnessGoal,
                            userProfile.getHeight(),
                            userProfile.getCurrentWeight(),
                            userProfile.getTargetWeight(),
                            fitnessLevel,
                            isMetric
                    );

                    if (success) {
                        
                    } else {
                        Log.e("SettingsFragment", "Не удалось синхронизировать данные с Supabase");
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Ошибка при синхронизации данных с Supabase: " + e.getMessage(), e);
                }
            }).start();
        } catch (Exception e) {
            Log.e("SettingsFragment", "Ошибка при подготовке синхронизации: " + e.getMessage(), e);
        }
    }
}