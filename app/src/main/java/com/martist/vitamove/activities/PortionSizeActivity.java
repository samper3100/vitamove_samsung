package com.martist.vitamove.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.martist.vitamove.R;
import com.martist.vitamove.events.FoodAddedEvent;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.Food;
import com.martist.vitamove.models.Meal;
import com.martist.vitamove.repositories.SupabaseBarcodeRepository;
import com.martist.vitamove.repositories.SupabaseFoodRepository;
import com.martist.vitamove.utils.Constants;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PortionSizeActivity extends BaseActivity {
    private static final String TAG = "PortionSizeActivity";
    private FoodManager foodManager;
    private Food selectedFood;
    private String mealType;
    private String barcode;
    private EditText portionSizeInput;
    private TextView caloriesText;
    
    
    private TextView foodCategoryText;
    private TextView proteinsValue;
    private TextView fatsValue;
    private TextView carbsValue;
    
    
    private TextView usefulnessIndexValue;
    private ProgressBar usefulnessIndexProgress;
    private TextView usefulnessIndexDescription;
    
    
    private TextView fiberValue;
    private TextView sugarValue;
    private TextView cholesterolValue;
    private TextView saturatedFatsValue;
    private TextView transFatsValue;
    private TextView fiberPercent;
    private TextView sugarPercent;
    private TextView cholesterolPercent;
    private TextView saturatedFatsPercent;
    private TextView transFatsPercent;
    
    
    private TextView calciumValue;
    private TextView ironValue;
    private TextView magnesiumValue;
    private TextView phosphorusValue;
    private TextView potassiumValue;
    private TextView sodiumValue;
    private TextView zincValue;
    
    private ProgressBar calciumProgress;
    private ProgressBar ironProgress;
    private ProgressBar magnesiumProgress;
    private ProgressBar phosphorusProgress;
    private ProgressBar potassiumProgress;
    private ProgressBar sodiumProgress;
    private ProgressBar zincProgress;
    
    
    private TextView vitaminAValue;
    private TextView vitaminB1Value;
    private TextView vitaminB2Value;
    private TextView vitaminB3Value;
    private TextView vitaminB5Value;
    private TextView vitaminB6Value;
    private TextView vitaminB9Value;
    private TextView vitaminB12Value;
    private TextView vitaminCValue;
    private TextView vitaminDValue;
    private TextView vitaminEValue;
    private TextView vitaminKValue;
    
    
    private static final float DAILY_FIBER = 25.0f; 
    private static final float DAILY_SUGAR = 50.0f; 
    private static final float DAILY_CHOLESTEROL = 300.0f; 
    private static final float DAILY_SATURATED_FATS = 20.0f; 
    private static final float DAILY_TRANS_FATS = 2.0f; 
    
    private static final float DAILY_CALCIUM = 1000.0f; 
    private static final float DAILY_IRON = 18.0f; 
    private static final float DAILY_MAGNESIUM = 400.0f; 
    private static final float DAILY_PHOSPHORUS = 700.0f; 
    private static final float DAILY_POTASSIUM = 4700.0f; 
    private static final float DAILY_SODIUM = 2300.0f; 
    private static final float DAILY_ZINC = 11.0f; 
    
    private static final float DAILY_VITAMIN_A = 900.0f; 
    private static final float DAILY_VITAMIN_B1 = 1.2f; 
    private static final float DAILY_VITAMIN_B2 = 1.3f; 
    private static final float DAILY_VITAMIN_B3 = 16.0f; 
    private static final float DAILY_VITAMIN_B5 = 5.0f; 
    private static final float DAILY_VITAMIN_B6 = 1.7f; 
    private static final float DAILY_VITAMIN_B9 = 400.0f; 
    private static final float DAILY_VITAMIN_B12 = 2.4f; 
    private static final float DAILY_VITAMIN_C = 90.0f; 
    private static final float DAILY_VITAMIN_D = 15.0f; 
    private static final float DAILY_VITAMIN_E = 15.0f; 
    private static final float DAILY_VITAMIN_K = 120.0f; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portion_size);
        
        
        try {
            Food.ensureClassLoaded();
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке класса Food: " + e.getMessage());
        }
        
        foodManager = FoodManager.getInstance(this);
        
        
        
        selectedFood = getIntent().getParcelableExtra(Constants.EXTRA_FOOD);
        mealType = getIntent().getStringExtra(Constants.EXTRA_MEAL_TYPE);
        barcode = getIntent().getStringExtra(Constants.EXTRA_BARCODE);
        
        
        int portionSize = getIntent().getIntExtra(Constants.EXTRA_PORTION_SIZE, 100);
        
        
        String selectedDateStr = getIntent().getStringExtra(Constants.EXTRA_SELECTED_DATE);
        if (selectedDateStr != null && !selectedDateStr.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date selectedDate = dateFormat.parse(selectedDateStr);
                if (selectedDate != null) {
                    
                    foodManager.setSelectedDateForView(selectedDate);
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при парсинге даты: " + e.getMessage());
            }
        }
        
        
        
        
        
        
        
        
        if (selectedFood != null) {
            
        }
        
        if (mealType == null) {
            Log.e(TAG, "mealType is null! Intent extras: " + getIntent().getExtras());
        }
        
        if (selectedFood == null) {
            Log.e(TAG, "selectedFood is null! Intent extras: " + getIntent().getExtras());
        }
        
        initViews();
        removeAllNutrientRows(); 
        setupClickListeners();
        setupTextChangedListener();

        
        portionSizeInput.setText(String.valueOf(portionSize));

        
        setupFoodInfo();
    }
    
    private void initViews() {
        
        portionSizeInput = findViewById(R.id.custom_portion_input);
        caloriesText = findViewById(R.id.calories_text);
        foodCategoryText = findViewById(R.id.food_category_text);
        
        
        proteinsValue = findViewById(R.id.proteins_value);
        fatsValue = findViewById(R.id.fats_value);
        carbsValue = findViewById(R.id.carbs_value);
        
        
        usefulnessIndexValue = findViewById(R.id.usefulness_index_value);
        usefulnessIndexProgress = findViewById(R.id.usefulness_index_progress);
        usefulnessIndexDescription = findViewById(R.id.usefulness_index_description);
        
        
        fiberValue = findViewById(R.id.fiber_value);
        sugarValue = findViewById(R.id.sugar_value);
        cholesterolValue = findViewById(R.id.cholesterol_value);
        saturatedFatsValue = findViewById(R.id.saturated_fats_value);
        transFatsValue = findViewById(R.id.trans_fats_value);
        
        fiberPercent = findViewById(R.id.fiber_percent);
        sugarPercent = findViewById(R.id.sugar_percent);
        cholesterolPercent = findViewById(R.id.cholesterol_percent);
        saturatedFatsPercent = findViewById(R.id.saturated_fats_percent);
        transFatsPercent = findViewById(R.id.trans_fats_percent);
        
        
        calciumValue = findViewById(R.id.calcium_value);
        ironValue = findViewById(R.id.iron_value);
        magnesiumValue = findViewById(R.id.magnesium_value);
        phosphorusValue = findViewById(R.id.phosphorus_value);
        potassiumValue = findViewById(R.id.potassium_value);
        sodiumValue = findViewById(R.id.sodium_value);
        zincValue = findViewById(R.id.zinc_value);
        
        calciumProgress = findViewById(R.id.calcium_progress);
        ironProgress = findViewById(R.id.iron_progress);
        magnesiumProgress = findViewById(R.id.magnesium_progress);
        phosphorusProgress = findViewById(R.id.phosphorus_progress);
        potassiumProgress = findViewById(R.id.potassium_progress);
        sodiumProgress = findViewById(R.id.sodium_progress);
        zincProgress = findViewById(R.id.zinc_progress);
        
        
        vitaminAValue = findViewById(R.id.vitamin_a_value);
        vitaminB1Value = findViewById(R.id.vitamin_b1_value);
        vitaminB2Value = findViewById(R.id.vitamin_b2_value);
        vitaminB3Value = findViewById(R.id.vitamin_b3_value);
        vitaminB5Value = findViewById(R.id.vitamin_b5_value);
        vitaminB6Value = findViewById(R.id.vitamin_b6_value);
        vitaminB9Value = findViewById(R.id.vitamin_b9_value);
        vitaminB12Value = findViewById(R.id.vitamin_b12_value);
        vitaminCValue = findViewById(R.id.vitamin_c_value);
        vitaminDValue = findViewById(R.id.vitamin_d_value);
        vitaminEValue = findViewById(R.id.vitamin_e_value);
        vitaminKValue = findViewById(R.id.vitamin_k_value);
    }

    private void setupClickListeners() {
        
        findViewById(R.id.add_button).setOnClickListener(v -> {
            String portionSizeStr = portionSizeInput.getText().toString();
            if (!portionSizeStr.isEmpty()) {
                int portionSize = Integer.parseInt(portionSizeStr);
                if (portionSize > 0) {
                    if (mealType == null) {
                        Toast.makeText(this, "Не указан прием пищи", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    
                    
                    
                    
                    Meal currentMeal = foodManager.getMeal(mealType);
                    
                    
                    if (currentMeal == null) {
                        
                        foodManager.addFoodToMeal(mealType, selectedFood, portionSize);
                        
                        
                        
                        EventBus.getDefault().post(new FoodAddedEvent(selectedFood, portionSize, mealType));
                    } 
                    else {
                        
                        
                        Meal newMeal = new Meal(currentMeal.getTitle(), currentMeal.getIconResId());
                        
                        
                        boolean foundExisting = false;
                        
                        
                        
                        
                        
                        int existingFoodIndex = -1;
                        for (int i = 0; i < currentMeal.getFoods().size(); i++) {
                            Meal.FoodPortion currentPortion = currentMeal.getFoods().get(i);
                            Food currentFood = currentPortion.getFood();
                            
                            
                            
                            
                            
                            
                            boolean sameId = currentFood.getId() == selectedFood.getId();
                            boolean sameName = currentFood.getName().equals(selectedFood.getName());
                            
                            
                            
                            
                            
                            
                            if (sameId && sameName) {
                                foundExisting = true;
                                existingFoodIndex = i;
                                
                                break;
                            }
                        }
                        
                        
                        for (int i = 0; i < currentMeal.getFoods().size(); i++) {
                            Meal.FoodPortion currentPortion = currentMeal.getFoods().get(i);
                            Food currentFood = currentPortion.getFood();
                            
                            if (i == existingFoodIndex) {
                                
                                newMeal.addFood(selectedFood, portionSize);
                                
                            } else {
                                
                                newMeal.addFood(currentFood, currentPortion.getPortionSize());
                                
                            }
                        }
                        
                        
                        if (!foundExisting) {
                            newMeal.addFood(selectedFood, portionSize);
                            
                        }
                        
                        
                        foodManager.updateMeal(mealType, newMeal);
                        
                        
                        if (foundExisting) {
                            Toast.makeText(this, "Размер порции обновлен", Toast.LENGTH_SHORT).show();
                            
                        } else {
                            
                            EventBus.getDefault().post(new FoodAddedEvent(selectedFood, portionSize, mealType));
                            Toast.makeText(this, "Продукт добавлен", Toast.LENGTH_SHORT).show();
                            
                        }
                    }
                    
                    
                    if (barcode != null && !barcode.isEmpty() && selectedFood != null) {
                        
                        new Thread(() -> {
                            
                            
                            try {
                                SupabaseBarcodeRepository barcodeRepository = foodManager.getBarcodeRepository();
                                SupabaseFoodRepository foodRepository = foodManager.getFoodRepository();
                                
                                if (barcodeRepository == null || foodRepository == null) {
                                    Log.e(TAG, "barcodeRepository или foodRepository равен null, не удалось сохранить штрихкод");
                                    return;
                                }
                                
                                
                                Food existingFoodByBarcode = barcodeRepository.findFoodByBarcode(barcode);
                                if (existingFoodByBarcode != null) {
                                    
                                    
                                    return;
                                }
                                
                                
                                String uuid = selectedFood.getIdUUID();
                                
                                
                                if (uuid == null || uuid.isEmpty()) {
                                    
                                    
                                    
                                    Food existingFood = foodRepository.getFoodByName(selectedFood.getName());
                                    
                                    if (existingFood != null) {
                                        
                                        uuid = existingFood.getIdUUID();
                                        
                                    } else {
                                        
                                        uuid = foodRepository.addFood(selectedFood);
                                        
                                        if (uuid == null) {
                                            Log.e(TAG, "Не удалось сохранить продукт в базу данных");
                                            return;
                                        }
                                        
                                        
                                    }
                                } else {
                                    
                                }
                                
                                
                                
                                if (uuid == null || uuid.isEmpty()) {
                                    Log.e(TAG, "Не удалось получить UUID продукта для сохранения штрихкода");
                                    return;
                                }
                                
                                
                                Food foodWithUUID = new Food.Builder()
                                    .id(selectedFood.getId())
                                    .idUUID(uuid)
                                    .name(selectedFood.getName())
                                    .category(selectedFood.getCategory())
                                    .subcategory(selectedFood.getSubcategory())
                                    .calories(selectedFood.getCalories())
                                    .proteins(selectedFood.getProteins())
                                    .fats(selectedFood.getFats())
                                    .carbs(selectedFood.getCarbs())
                                    .build();
                                
                                
                                boolean saved = barcodeRepository.addBarcode(barcode, foodWithUUID);
                                
                                if (saved) {
                                    
                                } else {
                                    Log.e(TAG, "Не удалось сохранить штрихкод в базе данных");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка при сохранении штрихкода: " + e.getMessage(), e);
                            }
                        }).start();
                    } else {
                        
                    }
                    
                    finish();
                } else {
                    Toast.makeText(this, "Размер порции должен быть больше 0", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Введите размер порции", Toast.LENGTH_SHORT).show();
            }
        });

        
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void setupTextChangedListener() {
        
        portionSizeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateNutrients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFoodInfo() {
        if (selectedFood != null) {
            TextView foodNameView = findViewById(R.id.food_name_text);
            foodNameView.setText(selectedFood.getName());
            
            
            foodCategoryText.setText(selectedFood.getCategory() + " / " + selectedFood.getSubcategory());
            
            
            updateUsefulnessIndex(selectedFood.getUsefulnessIndex());
            
            
            boolean isFromGigaChat = selectedFood.getId() == 0 && 
                                    (Float.isNaN(selectedFood.getVitaminA()) &&
                                     Float.isNaN(selectedFood.getCalcium()) &&
                                     Float.isNaN(selectedFood.getFiber()));
            
            
            boolean isAlreadyInMeal = false;
            if (mealType != null) {
                Meal currentMeal = foodManager.getMeal(mealType);
                if (currentMeal != null) {
                    
                    for (Meal.FoodPortion portion : currentMeal.getFoods()) {
                        Food mealFood = portion.getFood();
                        
                        if (mealFood.getName().equals(selectedFood.getName())) {
                            isAlreadyInMeal = true;
                            break;
                        }
                    }
                }
            }
            
            
            ImageButton editCategoryButton = findViewById(R.id.edit_category_button);
            editCategoryButton.setVisibility((isFromGigaChat && !isAlreadyInMeal) ? View.VISIBLE : View.GONE);
            
            
            editCategoryButton.setOnClickListener(v -> showCategoryPickerDialog());
        }
    }
    
    
    private void updateUsefulnessIndex(int usefulnessIndex) {
        usefulnessIndexValue.setText(String.valueOf(usefulnessIndex));
        usefulnessIndexProgress.setProgress(usefulnessIndex);
        
        
        if (usefulnessIndex >= 8) {
            
            usefulnessIndexProgress.setProgressDrawable(getResources().getDrawable(R.drawable.usefulness_progress_high));
            usefulnessIndexValue.setTextColor(getResources().getColor(R.color.green_500));
        } else if (usefulnessIndex >= 4) {
            
            usefulnessIndexProgress.setProgressDrawable(getResources().getDrawable(R.drawable.usefulness_progress_medium));
            usefulnessIndexValue.setTextColor(getResources().getColor(R.color.yellow_500));
        } else {
            
            usefulnessIndexProgress.setProgressDrawable(getResources().getDrawable(R.drawable.usefulness_progress_low));
            usefulnessIndexValue.setTextColor(getResources().getColor(R.color.red_500));
        }
        
        
        String description;
        if (usefulnessIndex >= 8) {
            description = "Очень полезный продукт, богатый питательными веществами";
        } else if (usefulnessIndex >= 6) {
            description = "Хороший выбор, содержит много полезных элементов";
        } else if (usefulnessIndex >= 4) {
            description = "Продукт средней пищевой ценности";
        } else if (usefulnessIndex >= 2) {
            description = "Умеренно полезный продукт, используйте в ограниченном количестве";
        } else {
            description = "Продукт с низкой пищевой ценностью, рекомендуется ограничить употребление";
        }
        
        usefulnessIndexDescription.setText(description);
    }

    private void updateNutrients(String portionStr) {
        try {
            float portion = Float.parseFloat(portionStr);
            double multiplier = portion / 100.0;

            
            if (selectedFood.getCalories() >= 0) {
                double calories = selectedFood.getCalories() * multiplier;
                caloriesText.setText(String.format("%d ккал", Math.round(calories)));
            } else {
                caloriesText.setText("нет данных");
            }

            
            updateMacronutrient(proteinsValue, selectedFood.getProteins(), multiplier);
            updateMacronutrient(fatsValue, selectedFood.getFats(), multiplier);
            updateMacronutrient(carbsValue, selectedFood.getCarbs(), multiplier);
            
            
            updateAdditionalNutrients(multiplier);
            
            
            updateMinerals(multiplier);
            
            
            updateVitamins(multiplier);

        } catch (NumberFormatException e) {
            resetValues();
        }
    }
    
    
    private void updateMacronutrient(TextView textView, float value, double multiplier) {
        if (!Float.isNaN(value) && value >= 0) {
            textView.setText(String.format("%.1f г", value * multiplier));
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setText("нет данных");
        }
    }
    
    private void updateAdditionalNutrients(double multiplier) {
        
        View additionalNutrientsContainer = findViewById(R.id.additional_nutrients_container);
        
        
        updateNutrientWithPercent(
            fiberValue, fiberPercent, 
            selectedFood.getFiber(), multiplier, 
            DAILY_FIBER, "г");
            
        updateNutrientWithPercent(
            sugarValue, sugarPercent, 
            selectedFood.getSugar(), multiplier, 
            DAILY_SUGAR, "г");
            
        updateNutrientWithPercent(
            cholesterolValue, cholesterolPercent, 
            selectedFood.getCholesterol(), multiplier, 
            DAILY_CHOLESTEROL, "мг");
            
        updateNutrientWithPercent(
            saturatedFatsValue, saturatedFatsPercent, 
            selectedFood.getSaturatedFats(), multiplier, 
            DAILY_SATURATED_FATS, "г");
            
        updateNutrientWithPercent(
            transFatsValue, transFatsPercent, 
            selectedFood.getTransFats(), multiplier, 
            DAILY_TRANS_FATS, "г");
            
        
        boolean hasVisibleElements = 
            (fiberValue.getParent() != null && ((View)fiberValue.getParent()).getVisibility() == View.VISIBLE) ||
            (sugarValue.getParent() != null && ((View)sugarValue.getParent()).getVisibility() == View.VISIBLE) ||
            (cholesterolValue.getParent() != null && ((View)cholesterolValue.getParent()).getVisibility() == View.VISIBLE) ||
            (saturatedFatsValue.getParent() != null && ((View)saturatedFatsValue.getParent()).getVisibility() == View.VISIBLE) ||
            (transFatsValue.getParent() != null && ((View)transFatsValue.getParent()).getVisibility() == View.VISIBLE);
            
        
        additionalNutrientsContainer.setVisibility(hasVisibleElements ? View.VISIBLE : View.GONE);
    }
    
    private void updateMinerals(double multiplier) {
        
        View mineralsCardView = findViewById(R.id.minerals_container);
        
        
        LinearLayout mineralsContent = findViewById(R.id.minerals_content);
        
        
        updateMineralWithProgress(
            calciumValue, calciumProgress,
            selectedFood.getCalcium(), multiplier,
            DAILY_CALCIUM);
            
        updateMineralWithProgress(
            ironValue, ironProgress,
            selectedFood.getIron(), multiplier,
            DAILY_IRON);
            
        updateMineralWithProgress(
            magnesiumValue, magnesiumProgress,
            selectedFood.getMagnesium(), multiplier,
            DAILY_MAGNESIUM);
            
        updateMineralWithProgress(
            phosphorusValue, phosphorusProgress,
            selectedFood.getPhosphorus(), multiplier,
            DAILY_PHOSPHORUS);
            
        updateMineralWithProgress(
            potassiumValue, potassiumProgress,
            selectedFood.getPotassium(), multiplier,
            DAILY_POTASSIUM);
            
        updateMineralWithProgress(
            sodiumValue, sodiumProgress,
            selectedFood.getSodium(), multiplier,
            DAILY_SODIUM);
            
        updateMineralWithProgress(
            zincValue, zincProgress,
            selectedFood.getZinc(), multiplier,
            DAILY_ZINC);
            
        
        boolean hasVisibleElements = false;
        if (mineralsContent != null) {
            for (int i = 0; i < mineralsContent.getChildCount(); i++) {
                View child = mineralsContent.getChildAt(i);
                if (child != null && child.getVisibility() == View.VISIBLE) {
                    hasVisibleElements = true;
                    break;
                }
            }
        }
            
        
        if (mineralsCardView != null) {
            mineralsCardView.setVisibility(hasVisibleElements ? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateVitamins(double multiplier) {
        
        View vitaminsContainer = findViewById(R.id.vitamins_container);
        
        
        updateVitaminValue(vitaminAValue, selectedFood.getVitaminA(), multiplier, DAILY_VITAMIN_A, "мкг");
        updateVitaminValue(vitaminB1Value, selectedFood.getVitaminB1(), multiplier, DAILY_VITAMIN_B1, "мг");
        updateVitaminValue(vitaminB2Value, selectedFood.getVitaminB2(), multiplier, DAILY_VITAMIN_B2, "мг");
        updateVitaminValue(vitaminB3Value, selectedFood.getVitaminB3(), multiplier, DAILY_VITAMIN_B3, "мг");
        updateVitaminValue(vitaminB5Value, selectedFood.getVitaminB5(), multiplier, DAILY_VITAMIN_B5, "мг");
        updateVitaminValue(vitaminB6Value, selectedFood.getVitaminB6(), multiplier, DAILY_VITAMIN_B6, "мг");
        updateVitaminValue(vitaminB9Value, selectedFood.getVitaminB9(), multiplier, DAILY_VITAMIN_B9, "мкг");
        updateVitaminValue(vitaminB12Value, selectedFood.getVitaminB12(), multiplier, DAILY_VITAMIN_B12, "мкг");
        updateVitaminValue(vitaminCValue, selectedFood.getVitaminC(), multiplier, DAILY_VITAMIN_C, "мг");
        updateVitaminValue(vitaminDValue, selectedFood.getVitaminD(), multiplier, DAILY_VITAMIN_D, "мкг");
        updateVitaminValue(vitaminEValue, selectedFood.getVitaminE(), multiplier, DAILY_VITAMIN_E, "мг");
        updateVitaminValue(vitaminKValue, selectedFood.getVitaminK(), multiplier, DAILY_VITAMIN_K, "мкг");
        
        
        boolean hasVisibleElements = 
            (vitaminAValue.getParent() != null && ((View)vitaminAValue.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminB1Value.getParent() != null && ((View)vitaminB1Value.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminB2Value.getParent() != null && ((View)vitaminB2Value.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminB3Value.getParent() != null && ((View)vitaminB3Value.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminB5Value.getParent() != null && ((View)vitaminB5Value.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminB6Value.getParent() != null && ((View)vitaminB6Value.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminB9Value.getParent() != null && ((View)vitaminB9Value.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminB12Value.getParent() != null && ((View)vitaminB12Value.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminCValue.getParent() != null && ((View)vitaminCValue.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminDValue.getParent() != null && ((View)vitaminDValue.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminEValue.getParent() != null && ((View)vitaminEValue.getParent()).getVisibility() == View.VISIBLE) ||
            (vitaminKValue.getParent() != null && ((View)vitaminKValue.getParent()).getVisibility() == View.VISIBLE);
            
        
        vitaminsContainer.setVisibility(hasVisibleElements ? View.VISIBLE : View.GONE);
    }
    
    
    private void removeAllNutrientRows() {
        
        
        
        
        setRowVisibility((TableRow)findViewById(R.id.fiber_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.sugar_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.cholesterol_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.saturated_fats_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.trans_fats_row), View.GONE);
        
        
        setRowVisibility((TableRow)findViewById(R.id.vitamin_a_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_b1_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_b2_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_b3_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_b5_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_b6_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_b9_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_b12_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_c_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_d_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_e_row), View.GONE);
        setRowVisibility((TableRow)findViewById(R.id.vitamin_k_row), View.GONE);
        
        
        LinearLayout mineralsContent = findViewById(R.id.minerals_content);
        if (mineralsContent != null) {
            for (int i = 0; i < mineralsContent.getChildCount(); i++) {
                View child = mineralsContent.getChildAt(i);
                if (child != null) {
                    child.setVisibility(View.GONE);
                }
            }
        }
    }
    
    
    private void setRowVisibility(TableRow row, int visibility) {
        if (row != null) {
            row.setVisibility(visibility);
        }
    }

    
    private void updateNutrientWithPercent(TextView valueView, TextView percentView, 
                                         float value, double multiplier, 
                                         float dailyNorm, String unit) {
        TableRow parentRow = (TableRow) valueView.getParent();
        
        if (!Float.isNaN(value) && value > 0) {
            float calculatedValue = value * (float)multiplier;
            valueView.setText(String.format("%.1f %s", calculatedValue, unit));
            
            int percentValue = Math.round(calculatedValue / dailyNorm * 100);
            percentView.setText(String.format("%d%%", percentValue));
            
            
            if (parentRow != null) {
                parentRow.setVisibility(View.VISIBLE);
            }
        } else {
            
            if (parentRow != null) {
                parentRow.setVisibility(View.GONE);
            }
        }
    }
    
    
    private void updateMineralWithProgress(TextView valueView, ProgressBar progressBar,
                                         float value, double multiplier,
                                         float dailyNorm) {
        LinearLayout parentRow = (LinearLayout) valueView.getParent().getParent();
        
        if (!Float.isNaN(value) && value > 0) {
            float calculatedValue = value * (float)multiplier;
            int percent = Math.min(100, Math.round(calculatedValue / dailyNorm * 100));
            
            valueView.setText(String.format("%.1f мг (%.0f%%)", calculatedValue, calculatedValue / dailyNorm * 100));
            progressBar.setProgress(percent);
            
            
            valueView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            if (parentRow != null) {
                parentRow.setVisibility(View.VISIBLE);
            }
        } else {
            
            if (parentRow != null) {
                parentRow.setVisibility(View.GONE);
            }
        }
    }
    
    
    private void updateVitaminValue(TextView valueView, float value, double multiplier, float dailyNorm, String unit) {
        TableRow parentRow = (TableRow) valueView.getParent();
        
        if (!Float.isNaN(value) && value > 0) {
            float calculatedValue = value * (float)multiplier;
            valueView.setText(String.format("%.1f %s (%.0f%%)", 
                calculatedValue, unit, calculatedValue / dailyNorm * 100));
            
            
            if (parentRow != null) {
                parentRow.setVisibility(View.VISIBLE);
            }
        } else {
            
            if (parentRow != null) {
                parentRow.setVisibility(View.GONE);
            }
        }
    }
    
    private void resetValues() {
        
        caloriesText.setText("нет данных");
        
        
        proteinsValue.setText("нет данных");
        fatsValue.setText("нет данных");
        carbsValue.setText("нет данных");
        
        
        findViewById(R.id.additional_nutrients_container).setVisibility(View.GONE);
        findViewById(R.id.minerals_container).setVisibility(View.GONE);
        findViewById(R.id.vitamins_container).setVisibility(View.GONE);
    }
    
    
    private Meal createMealCopy(Meal originalMeal) {
        Meal copy = new Meal(originalMeal.getTitle(), originalMeal.getIconResId());
        
        for (Meal.FoodPortion portion : originalMeal.getFoods()) {
            copy.addFood(portion.getFood(), portion.getPortionSize());
        }
        
        return copy;
    }
    
    
    private static class FoodPortion extends Meal.FoodPortion {
        public FoodPortion(Food food, int portionSize) {
            super(food, portionSize);
        }
    }

    
    private void showCategoryPickerDialog() {
        
        SupabaseFoodRepository foodRepository = foodManager.getFoodRepository();
        
        
        if (foodRepository == null) {
            Toast.makeText(this, "Не удалось получить доступ к базе данных", Toast.LENGTH_SHORT).show();
            return;
        }
        
        
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Загрузка категорий...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        
        new Thread(() -> {
            try {
                
                List<String> categoryList = foodRepository.getAllUniqueCategories();
                
                
                if (selectedFood.getCategory() != null && !selectedFood.getCategory().isEmpty()) {
                    if (!categoryList.contains(selectedFood.getCategory())) {
                        categoryList.add(selectedFood.getCategory());
                    }
                }
                
                
                if (!categoryList.contains("Другое")) {
                    categoryList.add("Другое");
                }
                
                
                Collections.sort(categoryList);
                
                
                final String[] categories = categoryList.toArray(new String[0]);
                
                
                final Map<String, String[]> subcategories = new HashMap<>();
                
                
                for (String category : categories) {
                    List<String> subcategoryList = foodRepository.getUniqueSubcategoriesForCategory(category);
                    
                    
                    if (selectedFood.getCategory() != null && 
                        selectedFood.getCategory().equals(category) && 
                        selectedFood.getSubcategory() != null && 
                        !selectedFood.getSubcategory().isEmpty()) {
                        if (!subcategoryList.contains(selectedFood.getSubcategory())) {
                            subcategoryList.add(selectedFood.getSubcategory());
                        }
                    }
                    
                    
                    if (!subcategoryList.contains("Другое")) {
                        subcategoryList.add("Другое");
                    }
                    
                    
                    Collections.sort(subcategoryList);
                    
                    
                    subcategories.put(category, subcategoryList.toArray(new String[0]));
                }
                
                
                runOnUiThread(() -> {
                    
                    progressDialog.dismiss();
                    
                    
                    String currentCategory = selectedFood.getCategory();
                    String currentSubcategory = selectedFood.getSubcategory();
                    
                    
                    int currentCategoryIndex = 0;
                    for (int i = 0; i < categories.length; i++) {
                        if (categories[i].equalsIgnoreCase(currentCategory)) {
                            currentCategoryIndex = i;
                            break;
                        }
                    }
                    
                    
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_picker, null);
                    
                    
                    NumberPicker categoryPicker = dialogView.findViewById(R.id.category_picker);
                    NumberPicker subcategoryPicker = dialogView.findViewById(R.id.subcategory_picker);
                    
                    
                    categoryPicker.setMinValue(0);
                    categoryPicker.setMaxValue(categories.length - 1);
                    categoryPicker.setDisplayedValues(categories);
                    categoryPicker.setValue(currentCategoryIndex);
                    
                    
                    Runnable updateSubcategories = () -> {
                        String selectedCategory = categories[categoryPicker.getValue()];
                        String[] subcat = subcategories.get(selectedCategory);
                        
                        
                        subcategoryPicker.setDisplayedValues(null);
                        
                        
                        subcategoryPicker.setMinValue(0);
                        subcategoryPicker.setMaxValue(subcat.length - 1);
                        
                        
                        subcategoryPicker.setDisplayedValues(subcat);
                        
                        
                        if (selectedCategory.equalsIgnoreCase(currentCategory)) {
                            String[] currentSubcategories = subcategories.get(selectedCategory);
                            int index = 0;
                            for (int i = 0; i < currentSubcategories.length; i++) {
                                if (currentSubcategories[i].equalsIgnoreCase(currentSubcategory)) {
                                    index = i;
                                    break;
                                }
                            }
                            subcategoryPicker.setValue(index);
                        } else {
                            subcategoryPicker.setValue(0);
                        }
                    };
                    
                    
                    updateSubcategories.run();
                    
                    
                    categoryPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSubcategories.run());
                    
                    
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Выберите категорию и подкатегорию")
                           .setView(dialogView)
                           .setPositiveButton("Сохранить", (dialog, which) -> {
                               
                               String selectedCategory = categories[categoryPicker.getValue()];
                               String[] subcat = subcategories.get(selectedCategory);
                               String selectedSubcategory = subcat[subcategoryPicker.getValue()];
                               
                               
                               selectedFood = new Food.Builder()
                                   .id(selectedFood.getId())
                                   .idUUID(selectedFood.getIdUUID())
                                   .name(selectedFood.getName())
                                   .category(selectedCategory)
                                   .subcategory(selectedSubcategory)
                                   .calories(selectedFood.getCalories())
                                   .proteins(selectedFood.getProteins())
                                   .fats(selectedFood.getFats())
                                   .carbs(selectedFood.getCarbs())
                                   .fiber(selectedFood.getFiber())
                                   .sugar(selectedFood.getSugar())
                                   .saturatedFats(selectedFood.getSaturatedFats())
                                   .transFats(selectedFood.getTransFats())
                                   .cholesterol(selectedFood.getCholesterol())
                                   .sodium(selectedFood.getSodium())
                                   .calcium(selectedFood.getCalcium())
                                   .iron(selectedFood.getIron())
                                   .magnesium(selectedFood.getMagnesium())
                                   .phosphorus(selectedFood.getPhosphorus())
                                   .potassium(selectedFood.getPotassium())
                                   .zinc(selectedFood.getZinc())
                                   .vitaminA(selectedFood.getVitaminA())
                                   .vitaminB1(selectedFood.getVitaminB1())
                                   .vitaminB2(selectedFood.getVitaminB2())
                                   .vitaminB3(selectedFood.getVitaminB3())
                                   .vitaminB5(selectedFood.getVitaminB5())
                                   .vitaminB6(selectedFood.getVitaminB6())
                                   .vitaminB9(selectedFood.getVitaminB9())
                                   .vitaminB12(selectedFood.getVitaminB12())
                                   .vitaminC(selectedFood.getVitaminC())
                                   .vitaminD(selectedFood.getVitaminD())
                                   .vitaminE(selectedFood.getVitaminE())
                                   .vitaminK(selectedFood.getVitaminK())
                                   .popularity(selectedFood.getPopularity())
                                   .usefulness_index(selectedFood.getUsefulnessIndex())
                                   .build();
                               
                               
                               foodCategoryText.setText(selectedCategory + " / " + selectedSubcategory);
                               Toast.makeText(this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                           })
                           .setNegativeButton("Отмена", null);
                    
                    builder.create().show();
                });
                
            } catch (Exception e) {
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка загрузки категорий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Ошибка при загрузке категорий: " + e.getMessage(), e);
                    
                    
                    showStaticCategoryPickerDialog();
                });
            }
        }).start();
    }
    
    
    private void showStaticCategoryPickerDialog() {
        
        String[] categories = {
            "Молочные продукты", "Мясо", "Рыба и морепродукты", "Яйца", "Крупы и злаки",
            "Фрукты", "Ягоды", "Овощи", "Бобовые", "Орехи и семена",
            "Хлеб и выпечка", "Сладости", "Напитки", "Соусы и приправы", "Другое"
        };
        
        
        java.util.Map<String, String[]> subcategories = new java.util.HashMap<>();
        subcategories.put("Молочные продукты", new String[]{"Молоко", "Творог", "Сыр", "Йогурт", "Сметана", "Кефир", "Другое"});
        subcategories.put("Мясо", new String[]{"Говядина", "Свинина", "Курица", "Индейка", "Утка", "Кролик", "Баранина", "Другое"});
        subcategories.put("Рыба и морепродукты", new String[]{"Морская рыба", "Речная рыба", "Креветки", "Кальмары", "Мидии", "Крабы", "Осьминоги", "Другое"});
        subcategories.put("Яйца", new String[]{"Куриные", "Перепелиные", "Другое"});
        subcategories.put("Крупы и злаки", new String[]{"Рис", "Гречка", "Овсянка", "Пшеница", "Ячмень", "Кукуруза", "Киноа", "Другое"});
        subcategories.put("Фрукты", new String[]{"Яблоки", "Груши", "Бананы", "Апельсины", "Мандарины", "Лимоны", "Другое"});
        subcategories.put("Ягоды", new String[]{"Клубника", "Малина", "Черника", "Голубика", "Ежевика", "Вишня", "Другое"});
        subcategories.put("Овощи", new String[]{"Картофель", "Морковь", "Капуста", "Лук", "Чеснок", "Огурцы", "Помидоры", "Перец", "Другое"});
        subcategories.put("Бобовые", new String[]{"Горох", "Фасоль", "Чечевица", "Нут", "Соя", "Другое"});
        subcategories.put("Орехи и семена", new String[]{"Грецкие орехи", "Миндаль", "Фундук", "Кешью", "Семена льна", "Семена чиа", "Другое"});
        subcategories.put("Хлеб и выпечка", new String[]{"Хлеб белый", "Хлеб ржаной", "Булочки", "Пирожки", "Другое"});
        subcategories.put("Сладости", new String[]{"Шоколад", "Конфеты", "Печенье", "Торты", "Мороженое", "Другое"});
        subcategories.put("Напитки", new String[]{"Вода", "Чай", "Кофе", "Соки", "Газированные напитки", "Алкоголь", "Другое"});
        subcategories.put("Соусы и приправы", new String[]{"Майонез", "Кетчуп", "Горчица", "Соевый соус", "Соль", "Перец", "Другое"});
        subcategories.put("Другое", new String[]{"Другое"});
        
        
        String currentCategory = selectedFood.getCategory();
        String currentSubcategory = selectedFood.getSubcategory();
        
        
        int currentCategoryIndex = 0;
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(currentCategory)) {
                currentCategoryIndex = i;
                break;
            }
        }
        
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_picker, null);
        
        
        NumberPicker categoryPicker = dialogView.findViewById(R.id.category_picker);
        NumberPicker subcategoryPicker = dialogView.findViewById(R.id.subcategory_picker);
        
        
        categoryPicker.setMinValue(0);
        categoryPicker.setMaxValue(categories.length - 1);
        categoryPicker.setDisplayedValues(categories);
        categoryPicker.setValue(currentCategoryIndex);
        
        
        Runnable updateSubcategories = () -> {
            String selectedCategory = categories[categoryPicker.getValue()];
            String[] subcat = subcategories.get(selectedCategory);
            
            
            subcategoryPicker.setDisplayedValues(null);
            
            
            subcategoryPicker.setMinValue(0);
            subcategoryPicker.setMaxValue(subcat.length - 1);
            
            
            subcategoryPicker.setDisplayedValues(subcat);
            
            
            if (selectedCategory.equalsIgnoreCase(currentCategory)) {
                String[] currentSubcategories = subcategories.get(selectedCategory);
                int index = 0;
                for (int i = 0; i < currentSubcategories.length; i++) {
                    if (currentSubcategories[i].equalsIgnoreCase(currentSubcategory)) {
                        index = i;
                        break;
                    }
                }
                subcategoryPicker.setValue(index);
            } else {
                subcategoryPicker.setValue(0);
            }
        };
        
        
        updateSubcategories.run();
        
        
        categoryPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSubcategories.run());
        
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите категорию и подкатегорию")
               .setView(dialogView)
               .setPositiveButton("Сохранить", (dialog, which) -> {
                   
                   String selectedCategory = categories[categoryPicker.getValue()];
                   String[] subcat = subcategories.get(selectedCategory);
                   String selectedSubcategory = subcat[subcategoryPicker.getValue()];
                   
                   
                   selectedFood = new Food.Builder()
                       .id(selectedFood.getId())
                       .idUUID(selectedFood.getIdUUID())
                       .name(selectedFood.getName())
                       .category(selectedCategory)
                       .subcategory(selectedSubcategory)
                       .calories(selectedFood.getCalories())
                       .proteins(selectedFood.getProteins())
                       .fats(selectedFood.getFats())
                       .carbs(selectedFood.getCarbs())
                       .fiber(selectedFood.getFiber())
                       .sugar(selectedFood.getSugar())
                       .saturatedFats(selectedFood.getSaturatedFats())
                       .transFats(selectedFood.getTransFats())
                       .cholesterol(selectedFood.getCholesterol())
                       .sodium(selectedFood.getSodium())
                       .calcium(selectedFood.getCalcium())
                       .iron(selectedFood.getIron())
                       .magnesium(selectedFood.getMagnesium())
                       .phosphorus(selectedFood.getPhosphorus())
                       .potassium(selectedFood.getPotassium())
                       .zinc(selectedFood.getZinc())
                       .vitaminA(selectedFood.getVitaminA())
                       .vitaminB1(selectedFood.getVitaminB1())
                       .vitaminB2(selectedFood.getVitaminB2())
                       .vitaminB3(selectedFood.getVitaminB3())
                       .vitaminB5(selectedFood.getVitaminB5())
                       .vitaminB6(selectedFood.getVitaminB6())
                       .vitaminB9(selectedFood.getVitaminB9())
                       .vitaminB12(selectedFood.getVitaminB12())
                       .vitaminC(selectedFood.getVitaminC())
                       .vitaminD(selectedFood.getVitaminD())
                       .vitaminE(selectedFood.getVitaminE())
                       .vitaminK(selectedFood.getVitaminK())
                       .popularity(selectedFood.getPopularity())
                       .usefulness_index(selectedFood.getUsefulnessIndex())
                       .build();
                   
                   
                   foodCategoryText.setText(selectedCategory + " / " + selectedSubcategory);
                   Toast.makeText(this, "Категория обновлена", Toast.LENGTH_SHORT).show();
               })
               .setNegativeButton("Отмена", null);
        
        builder.create().show();
    }
} 