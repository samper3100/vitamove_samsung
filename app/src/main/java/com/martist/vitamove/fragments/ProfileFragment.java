package com.martist.vitamove.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.martist.vitamove.R;
import com.martist.vitamove.activities.EditProfileActivity;
import com.martist.vitamove.activities.MainActivity;
import com.martist.vitamove.activities.SettingsActivity;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.utils.BMICalculator;
import com.martist.vitamove.utils.DateUtils;
import com.martist.vitamove.utils.ImageUtils;
import com.martist.vitamove.views.DevelopmentOverlay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Date;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_EDIT_PROFILE = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    private static final int REQUEST_IMAGE_CAPTURE = 1003;
    private static final String PREFS_USER_DATA = "user_data";
    private static final String AVATAR_FILE_NAME = "profile_avatar.jpg";
    private static final String TEMP_PHOTO_FILE_NAME = "temp_profile_photo.jpg";

    private TextView userName;
    private TextView userInfo;
    private TextView currentWeight;
    private TextView targetWeight;
    private TextView heightValue;
    private TextView bodyFatValue;
    private TextView waistValue;
    private TextView bmiValue;
    private TextView bmiDescription;
    private ImageView bmiMarker;
    private TextView weightToLose;
    private LinearProgressIndicator weightProgress;
    private TextView progressPercent;
    private TextView weightLossRate;
    private TextView estimatedCompletionDate;

    private TextView dailyCaloriesValue;
    private TextView waterIntakeValue;

    private UserProfile userProfile;

    private de.hdodenhof.circleimageview.CircleImageView profileImage;

    private Uri cameraPhotoUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        loadProfileData(view);
        
        
        applyDevelopmentOverlaysToAchievements(view);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusbar_color));
            
            
            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; 
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        
        
        if (getView() != null) {
            loadProfileData(getView());
        }
        
        
        
    }

    private void initViews(View view) {
        
        userName = view.findViewById(R.id.userName);
        userInfo = view.findViewById(R.id.userInfo);
        currentWeight = view.findViewById(R.id.currentWeight);
        targetWeight = view.findViewById(R.id.targetWeight);
        
        
        heightValue = view.findViewById(R.id.heightValue);
        bodyFatValue = view.findViewById(R.id.bodyFatValue);
        waistValue = view.findViewById(R.id.waistValue);
        
        
        bmiValue = view.findViewById(R.id.bmiValue);
        bmiDescription = view.findViewById(R.id.bmiDescription);
        bmiMarker = view.findViewById(R.id.bmiMarker);
        
        
        weightToLose = view.findViewById(R.id.weightToLose);
        weightProgress = view.findViewById(R.id.weightProgress);
        progressPercent = view.findViewById(R.id.progressPercent);
        weightLossRate = view.findViewById(R.id.weightLossRate);
        estimatedCompletionDate = view.findViewById(R.id.estimatedCompletionDate);
        
        

        dailyCaloriesValue = view.findViewById(R.id.dailyCaloriesValue);
        waterIntakeValue = view.findViewById(R.id.waterIntakeValue);

        
        profileImage = view.findViewById(R.id.profileImage);
        profileImage.setOnClickListener(v -> openGallery());

        
        MaterialButton settingsButton = view.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> openSettings());

        MaterialButton viewAllAchievementsButton = view.findViewById(R.id.viewAllAchievementsButton);
        viewAllAchievementsButton.setOnClickListener(v -> openAchievements());

        MaterialButton editProfileFab = view.findViewById(R.id.editProfileFab);
        editProfileFab.setOnClickListener(v -> openEditProfile());
    }

    public void loadProfileData(View view) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
        
        
        String name = prefs.getString("name", "Пользователь");
        int age = prefs.getInt("age", 30);
        String gender = prefs.getString("gender", "Мужчина");
        float currentWeightValue = prefs.getFloat("current_weight", 0);
        float targetWeightValue = prefs.getFloat("target_weight", 0);
        float height = prefs.getFloat("height", 0);
        float bodyFat = prefs.getFloat("body_fat", 0);
        float waist = prefs.getFloat("waist", 0);
        
        
        userProfile = new UserProfile(name, age, gender, currentWeightValue, targetWeightValue, height, bodyFat, waist);
        
        
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");
        String currentGoal = prefs.getString("fitness_goal", "weight_loss");
        
        
        if (!fitnessGoal.equals(currentGoal)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("fitness_goal", fitnessGoal);
            editor.apply();
            
        }
        
        
        boolean dataFromSupabase = prefs.getBoolean("is_synchronized", false);
        int targetCalories = prefs.getInt("target_calories", 0);
        float targetWater = prefs.getFloat("target_water", 0);
        
        
        
        
        if (dataFromSupabase && targetCalories > 0) {
            
            userProfile.setTargetCalories(targetCalories);
            userProfile.setTargetWater(targetWater > 0 ? targetWater : userProfile.calculateTargetWater());
            
            
        } else {
            
            userProfile.updateTargetCalories();
            userProfile.updateTargetWater();
            
            
            int updatedTargetCalories = userProfile.getTargetCalories();
            float updatedTargetWater = userProfile.getTargetWater();
            
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("target_calories", updatedTargetCalories);
            editor.putFloat("target_water", updatedTargetWater);
            editor.apply();
            
            
        }
        
        
        com.martist.vitamove.managers.DashboardManager dashboardManager = 
            com.martist.vitamove.managers.DashboardManager.getInstance(requireContext());
        dashboardManager.updateWaterGoalFromProfile();
        dashboardManager.updateCaloriesGoalFromProfile();
        
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNavigationHeader();
        }
        
        
        userName.setText(userProfile.getName());
        userInfo.setText(String.format("%s, %d %s", userProfile.getGender(), userProfile.getAge(), getAgeString(userProfile.getAge())));
        currentWeight.setText(String.format("%.1f кг", userProfile.getCurrentWeight()));
        targetWeight.setText(String.format("%.1f кг", userProfile.getTargetWeight()));
        
        updateBMIData(view);
        updateWeightProgress();
        updateEnergyData();
        updateWaterData();
        
        heightValue.setText(String.format("%.0f см", userProfile.getHeight()));
        bodyFatValue.setText(String.format("%.1f%%", userProfile.getBodyFat()));
        waistValue.setText(String.format("%.0f см", userProfile.getWaist()));
        
        
        loadProfileAvatar();
    }
    
    private void updateBMIData(View view) {
        
        float bmi = BMICalculator.calculateBMI(userProfile.getCurrentWeight(), userProfile.getHeight());
        
        
        DecimalFormat df = new DecimalFormat("#.#");
        bmiValue.setText(df.format(bmi));
        
        
        View bmiMarkerCard = view.findViewById(R.id.bmiMarkerCard);
        
        
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bmiMarkerCard.getLayoutParams();
        
        
        float markerPosition;
        if (bmi < 18.5f) {
            
            markerPosition = (bmi / 18.5f) * 20.0f;
            bmiValue.setTextColor(getResources().getColor(R.color.green_500));
            bmiDescription.setText("У вас недостаточный вес. Рекомендуется набрать вес для улучшения здоровья.");
        } else if (bmi < 25.0f) {
            
            markerPosition = 20.0f + ((bmi - 18.5f) / (25.0f - 18.5f)) * 20.0f;
            bmiValue.setTextColor(getResources().getColor(R.color.light_green_500));
            bmiDescription.setText("У вас нормальный вес. Продолжайте поддерживать здоровый образ жизни.");
        } else if (bmi < 30.0f) {
            
            markerPosition = 40.0f + ((bmi - 25.0f) / (30.0f - 25.0f)) * 20.0f;
            bmiValue.setTextColor(getResources().getColor(R.color.yellow_500));
            bmiDescription.setText("У вас есть избыточный вес. Работайте над снижением веса для улучшения здоровья.");
        } else if (bmi < 35.0f) {
            
            markerPosition = 60.0f + ((bmi - 30.0f) / (35.0f - 30.0f)) * 20.0f;
            bmiValue.setTextColor(getResources().getColor(R.color.orange_500));
            bmiDescription.setText("У вас ожирение I степени. Рекомендуется обратиться к специалисту для снижения веса.");
        } else {
            
            markerPosition = 80.0f + Math.min(((bmi - 35.0f) / 15.0f) * 20.0f, 20.0f);
            bmiValue.setTextColor(getResources().getColor(R.color.red_500));
            bmiDescription.setText("У вас выраженное ожирение. Необходима консультация врача и план по снижению веса.");
        }
        
        
        final View container = (View) bmiMarkerCard.getParent();
        
        
        if (container.getWidth() <= 0) {
            
            container.post(() -> updateBMIMarkerPosition(bmiMarkerCard, container, markerPosition));
        } else {
            
            updateBMIMarkerPosition(bmiMarkerCard, container, markerPosition);
        }
    }
    
    
    private void updateBMIMarkerPosition(View markerCard, View container, float markerPosition) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) markerCard.getLayoutParams();
        
        float containerWidth = container.getWidth();
        float markerWidth = markerCard.getWidth();
        
        
        float absolutePosition = (containerWidth - markerWidth) * (markerPosition / 100f);
        
        
        params.setMarginStart((int)absolutePosition);
        markerCard.setLayoutParams(params);
        
        
        
    }
    
    private void updateWeightProgress() {
        float weightDifference = userProfile.getCurrentWeight() - userProfile.getTargetWeight();
        weightToLose.setText(String.format("%.1f кг", weightDifference));
        
        
        float initialDifference = weightDifference * 1.2f;
        float weightLost = initialDifference - weightDifference;
        int progressPercentValue = (int)((weightLost / initialDifference) * 100);
        
        
        weightProgress.setProgress(progressPercentValue);
        progressPercent.setText(String.format("%d%% к цели", progressPercentValue));
        
        
        float weeksToGoal = weightDifference / 0.5f;
        Date estimatedDate = DateUtils.getDateAfterWeeks(weeksToGoal);
        estimatedCompletionDate.setText(String.format("Расчетная дата достижения цели: %s", 
                DateUtils.formatDate(estimatedDate)));
                
        
        weightLossRate.setText(String.format("Средняя скорость: %.1f кг в неделю", 0.5f));
    }
    
    private void updateEnergyData() {
        

        
        userProfile.updateTargetCalories();
        int targetCalories = userProfile.getTargetCalories();
        
        
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");
        
        
        if (dailyCaloriesValue != null) {
            dailyCaloriesValue.setText(String.format("%d ккал", targetCalories));
            
            
            
        } else {
            Log.e("ProfileFragment", "Не удалось обновить данные калорий: компонент равен null");
        }
        
        
        try {
            FoodManager foodManager = FoodManager.getInstance(requireContext());
            foodManager.updateTargetCalories(targetCalories);
        } catch (Exception e) {
            Log.e("ProfileFragment", "Ошибка при обновлении целевых калорий в FoodManager", e);
        }
    }
    
    private float calculateBMR() {
        
        boolean isMale = userProfile.getGender().equalsIgnoreCase("Мужчина");
        float bmr;
        
        if (isMale) {
            bmr = 10 * userProfile.getCurrentWeight() + 6.25f * userProfile.getHeight() - 5 * userProfile.getAge() + 5;
        } else {
            bmr = 10 * userProfile.getCurrentWeight() + 6.25f * userProfile.getHeight() - 5 * userProfile.getAge() - 161;
        }
        
        return bmr;
    }
    
    private String getAgeString(int age) {
        int lastDigit = age % 10;
        int lastTwoDigits = age % 100;
        
        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
            return "лет";
        }
        
        if (lastDigit == 1) {
            return "год";
        }
        
        if (lastDigit >= 2 && lastDigit <= 4) {
            return "года";
        }
        
        return "лет";
    }
    
    private void openEditProfile() {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivityForResult(intent, REQUEST_EDIT_PROFILE);
    }
    
    private void openSettings() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }
    
    private void openAchievements() {
        
    }
    
    
    private void openGallery() {
        
        String[] options = {"Камера", "Галерея"};
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите источник")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    
                    takePictureFromCamera();
                } else {
                    
                    pickImageFromGallery();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    
    private void takePictureFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            
            File photoFile = null;
            try {
                photoFile = new File(requireContext().getExternalCacheDir(), TEMP_PHOTO_FILE_NAME);
            } catch (Exception e) {
                Log.e("ProfileFragment", "Ошибка при создании файла для камеры: " + e.getMessage(), e);
            }
            
            
            if (photoFile != null) {
                
                cameraPhotoUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", photoFile);
                
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(requireContext(), "Не удалось создать файл для фото", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "На устройстве нет приложения камеры", Toast.LENGTH_SHORT).show();
        }
    }
    
    
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, 
                             MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        
        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == Activity.RESULT_OK) {
            
            if (getView() != null) {
                loadProfileData(getView());
            }
            
            
            Toast.makeText(requireContext(), "Нормы калорий и потребления воды пересчитаны с учетом новых данных", Toast.LENGTH_SHORT).show();
            
            
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNavigationHeader();
            }
        } 
        
        else if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                saveAndDisplayAvatar(selectedImageUri);
            }
        } 
        
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            
            if (cameraPhotoUri != null) {
                saveAndDisplayAvatar(cameraPhotoUri);
            }
        }
    }
    
    
    private void saveAndDisplayAvatar(Uri imageUri) {
        try {
            
            boolean saveSuccess = ImageUtils.saveImageToInternalStorage(requireContext(), imageUri, AVATAR_FILE_NAME);
            
            if (saveSuccess) {
                
                Bitmap avatarBitmap = ImageUtils.loadImageFromInternalStorage(requireContext(), AVATAR_FILE_NAME);
                if (avatarBitmap != null) {
                    
                    profileImage.setImageBitmap(avatarBitmap);
                    
                    
                    SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("profile_image", imageUri.toString());
                    editor.apply();
                    
                    Toast.makeText(requireContext(), "Аватар успешно обновлен", Toast.LENGTH_SHORT).show();
                    
                }
            } else {
                Toast.makeText(requireContext(), "Не удалось сохранить аватар", Toast.LENGTH_SHORT).show();
                Log.e("ProfileFragment", "Не удалось сохранить аватар во внутреннее хранилище");
            }
        } catch (Exception e) {
            Log.e("ProfileFragment", "Ошибка при обработке аватара: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Ошибка при обработке фото", Toast.LENGTH_SHORT).show();
        }
    }

    
    private void applyDevelopmentOverlaysToAchievements(View view) {
        
        View achievementsCard = view.findViewById(R.id.achievements_card);
        if (achievementsCard != null) {
            DevelopmentOverlay.applyToView(achievementsCard);
        }
        
        
        View progressGoalCard = view.findViewById(R.id.progress_goal_card);
        if (progressGoalCard != null) {
            DevelopmentOverlay.applyToView(progressGoalCard);
        }
        
        
    }

    
    private void updateWaterData() {
        if (waterIntakeValue != null && userProfile != null) {
            float targetWater = userProfile.getTargetWater();
            waterIntakeValue.setText(String.format("%.1f л", targetWater));
            
            
            SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");
            
            
            
        } else {
            Log.e("ProfileFragment", "Не удалось обновить данные о потреблении воды: компонент или профиль равны null");
        }
    }

    
    private void loadProfileAvatar() {
        try {
            
            if (ImageUtils.imageExists(requireContext(), AVATAR_FILE_NAME)) {
                
                Bitmap avatarBitmap = ImageUtils.loadImageFromInternalStorage(requireContext(), AVATAR_FILE_NAME);
                if (avatarBitmap != null) {
                    profileImage.setImageBitmap(avatarBitmap);
                    
                    return;
                }
            }
            
            
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
            String avatarUriString = prefs.getString("profile_image", null);
            
            if (avatarUriString != null) {
                try {
                    Uri avatarUri = Uri.parse(avatarUriString);
                    
                    try (InputStream inputStream = requireContext().getContentResolver().openInputStream(avatarUri)) {
                        if (inputStream != null) {
                            
                            if (ImageUtils.saveImageToInternalStorage(requireContext(), avatarUri, AVATAR_FILE_NAME)) {
                                
                                Bitmap avatarBitmap = ImageUtils.loadImageFromInternalStorage(requireContext(), AVATAR_FILE_NAME);
                                if (avatarBitmap != null) {
                                    profileImage.setImageBitmap(avatarBitmap);
                                    
                                }
                            }
                        } else {
                            
                            
                            prefs.edit().remove("profile_image").apply();
                        }
                    } catch (IOException e) {
                        Log.e("ProfileFragment", "URI аватара недействителен: " + e.getMessage());
                        
                        prefs.edit().remove("profile_image").apply();
                    }
                } catch (Exception e) {
                    Log.e("ProfileFragment", "Ошибка при загрузке аватара из URI: " + e.getMessage(), e);
                    
                    prefs.edit().remove("profile_image").apply();
                }
            } else {
                
            }
        } catch (Exception e) {
            Log.e("ProfileFragment", "Ошибка при загрузке аватара: " + e.getMessage(), e);
        }
    }
} 