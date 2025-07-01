package com.martist.vitamove.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.utils.BMICalculator;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.viewmodels.UserWeightViewModel;

import java.util.Date;


public class EditProfileActivity extends BaseActivity {

    private static final String PREFS_USER_DATA = "user_data";
    private static final String TAG = "EditProfileActivity";
    
    private TextInputEditText nameInput;
    private TextInputEditText ageInput;
    private AutoCompleteTextView genderInput;
    private TextInputEditText currentWeightInput;
    private TextInputEditText targetWeightInput;
    private TextInputEditText heightInput;
    private TextInputEditText bodyFatInput;
    private TextInputEditText waistInput;
    
    private TextInputLayout currentWeightLayout;
    private TextInputLayout targetWeightLayout;
    private TextInputLayout heightLayout;
    private TextInputLayout bodyFatLayout;
    private TextInputLayout waistLayout;
    
    private UserProfile userProfile;
    private SupabaseClient supabaseClient;
    private UserWeightViewModel userWeightViewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Редактирование профиля");
        
        
        initViews();
        
        
        loadProfileData();
        
        
        supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );
        
        
        userWeightViewModel = new ViewModelProvider(this).get(UserWeightViewModel.class);
        
        
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    saveProfileData();
                }
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        getMenuInflater().inflate(R.menu.edit_profile_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_save) {
            
            if (validateInputs()) {
                saveProfileData();
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    
    private void initViews() {
        nameInput = findViewById(R.id.nameInput);
        ageInput = findViewById(R.id.ageInput);
        genderInput = findViewById(R.id.genderDropdown);
        currentWeightInput = findViewById(R.id.weightInput);
        targetWeightInput = findViewById(R.id.targetWeightInput);
        heightInput = findViewById(R.id.heightInput);
        bodyFatInput = findViewById(R.id.bodyFatInput);
        waistInput = findViewById(R.id.waistInput);
        

        currentWeightLayout = (TextInputLayout) currentWeightInput.getParent().getParent();
        targetWeightLayout = (TextInputLayout) targetWeightInput.getParent().getParent();
        heightLayout = (TextInputLayout) heightInput.getParent().getParent();
        bodyFatLayout = (TextInputLayout) bodyFatInput.getParent().getParent();
        waistLayout = (TextInputLayout) waistInput.getParent().getParent();
        
        
        String[] genders = new String[]{"Мужчина", "Женщина"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, genders);
        genderInput.setAdapter(adapter);
    }
    
    
    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
        
        String name = prefs.getString("name", "");
        int age = prefs.getInt("age", 30);
        String gender = prefs.getString("gender", "Женщина");
        float currentWeight = prefs.getFloat("current_weight", 70.0f);
        float targetWeight = prefs.getFloat("target_weight", 60.0f);
        float height = prefs.getFloat("height", 170.0f);
        float bodyFat = prefs.getFloat("body_fat", 25.0f);
        float waist = prefs.getFloat("waist", 75.0f);
        
        userProfile = new UserProfile(name, age, gender, currentWeight, targetWeight, height, bodyFat, waist);
        
        
        nameInput.setText(userProfile.getName());
        ageInput.setText(String.valueOf(userProfile.getAge()));
        genderInput.setText(userProfile.getGender(), false);
        currentWeightInput.setText(String.format("%.1f", userProfile.getCurrentWeight()));
        targetWeightInput.setText(String.format("%.1f", userProfile.getTargetWeight()));
        heightInput.setText(String.format("%.0f", userProfile.getHeight()));
        bodyFatInput.setText(String.format("%.1f", userProfile.getBodyFat()));
        waistInput.setText(String.format("%.0f", userProfile.getWaist()));
    }
    
    
    private void saveProfileData() {
        String name = nameInput.getText().toString();
        int age = Integer.parseInt(ageInput.getText().toString());
        String gender = genderInput.getText().toString();
        float currentWeight = Float.parseFloat(currentWeightInput.getText().toString());
        float targetWeight = Float.parseFloat(targetWeightInput.getText().toString());
        float height = Float.parseFloat(heightInput.getText().toString());
        float bodyFat = Float.parseFloat(bodyFatInput.getText().toString());
        float waist = Float.parseFloat(waistInput.getText().toString());
        
        
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
        float oldWeight = prefs.getFloat("current_weight", 0f);
        
        
        userProfile = new UserProfile(name, age, gender, currentWeight, targetWeight, height, bodyFat, waist);
        
        
        userProfile.updateTargetCalories();
        userProfile.updateTargetWater();
        
        
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putString("name", userProfile.getName());
        editor.putInt("age", userProfile.getAge());
        editor.putString("gender", userProfile.getGender());
        editor.putFloat("current_weight", userProfile.getCurrentWeight());
        editor.putFloat("target_weight", userProfile.getTargetWeight());
        editor.putFloat("height", userProfile.getHeight());
        editor.putFloat("body_fat", userProfile.getBodyFat());
        editor.putFloat("waist", userProfile.getWaist());
        editor.putInt("target_calories", userProfile.getTargetCalories());
        editor.putFloat("target_water", userProfile.getTargetWater());
        
        
        float bmi = BMICalculator.calculateBMI(currentWeight, height);
        editor.putFloat("bmi", bmi);
        
        
        if (currentWeight != oldWeight && currentWeight > 0) {
            
            userWeightViewModel.addWeightRecord(currentWeight, new Date(), null);
            
        }
        
        
        editor.apply();
        
        
        SharedPreferences appPrefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        boolean isMetric = appPrefs.getBoolean("use_metric", true);
        String userId = appPrefs.getString("userId", null);
        
        
        
        SharedPreferences userDataPrefs = getSharedPreferences(PREFS_USER_DATA, MODE_PRIVATE);
        String fitnessGoal = userDataPrefs.getString("fitness_goal", 
                                PreferenceManager.getDefaultSharedPreferences(this)
                                .getString("fitness_goal", "weight_loss"));
                                
        String fitnessLevel = userDataPrefs.getString("fitness_level", 
                                PreferenceManager.getDefaultSharedPreferences(this)
                                .getString("user_fitness_level", "intermediate"));
        
        
        
        
        
        if (userId != null) {
            
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Обновление профиля...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            
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
                    
                    
                    runOnUiThread(() -> {
                        
                        progressDialog.dismiss();
                        
                        if (success) {
                            Toast.makeText(this, "Профиль успешно обновлен", Toast.LENGTH_SHORT).show();
                            
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, "Профиль сохранен локально", Toast.LENGTH_SHORT).show();
                            
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при обновлении профиля пользователя: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Профиль сохранен локально", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
            }).start();
        } else {
            
            Toast.makeText(this, "Профиль обновлен", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }
    
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        
        if (nameInput.getText().toString().trim().isEmpty()) {
            nameInput.setError("Введите имя");
            isValid = false;
        }
        
        
        try {
            int age = Integer.parseInt(ageInput.getText().toString());
            if (age <= 0 || age > 120) {
                ageInput.setError("Введите корректный возраст");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            ageInput.setError("Введите числовое значение");
            isValid = false;
        }
        
        
        if (genderInput.getText().toString().trim().isEmpty()) {
            genderInput.setError("Выберите пол");
            isValid = false;
        }
        
        
        try {
            float weight = Float.parseFloat(currentWeightInput.getText().toString());
            if (weight <= 0 || weight > 300) {
                currentWeightLayout.setError("Введите корректный вес");
                isValid = false;
            } else {
                currentWeightLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            currentWeightLayout.setError("Введите числовое значение");
            isValid = false;
        }
        
        
        try {
            float weight = Float.parseFloat(targetWeightInput.getText().toString());
            if (weight <= 0 || weight > 300) {
                targetWeightLayout.setError("Введите корректный вес");
                isValid = false;
            } else {
                targetWeightLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            targetWeightLayout.setError("Введите числовое значение");
            isValid = false;
        }
        
        
        try {
            float height = Float.parseFloat(heightInput.getText().toString());
            if (height <= 0 || height > 250) {
                heightLayout.setError("Введите корректный рост");
                isValid = false;
            } else {
                heightLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            heightLayout.setError("Введите числовое значение");
            isValid = false;
        }
        
        
        try {
            float bodyFat = Float.parseFloat(bodyFatInput.getText().toString());
            if (bodyFat < 0 || bodyFat > 70) {
                bodyFatLayout.setError("Введите корректный % жира (0-70%)");
                isValid = false;
            } else {
                bodyFatLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            bodyFatLayout.setError("Введите числовое значение");
            isValid = false;
        }
        
        
        try {
            float waist = Float.parseFloat(waistInput.getText().toString());
            if (waist < 0 || waist > 200) {
                waistLayout.setError("Введите корректный обхват талии");
                isValid = false;
            } else {
                waistLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            waistLayout.setError("Введите числовое значение");
            isValid = false;
        }
        
        return isValid;
    }
} 