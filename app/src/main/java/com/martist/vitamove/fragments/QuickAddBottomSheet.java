package com.martist.vitamove.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.Food;
import com.martist.vitamove.models.Meal;
import com.martist.vitamove.models.SelectedFood;
import com.martist.vitamove.utils.Constants;

import java.util.Calendar;


public class QuickAddBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "QuickAddBottomSheet";
    
    private RadioGroup mealTypeGroup;
    private RadioButton breakfastRadio;
    private RadioButton lunchRadio;
    private RadioButton dinnerRadio;
    private RadioButton snackRadio;
    private TextInputEditText nameInput;
    private TextInputEditText caloriesInput;
    private TextInputEditText proteinsInput;
    private TextInputEditText fatsInput;
    private TextInputEditText carbsInput;
    private MaterialButton addButton;
    
    private FoodManager foodManager;
    

    public interface OnFoodQuickAddedListener {
        void onFoodQuickAdded(String mealType);
    }
    
    private OnFoodQuickAddedListener listener;
    

    public void setListener(OnFoodQuickAddedListener listener) {
        this.listener = listener;
    }
    
    public static QuickAddBottomSheet newInstance() {
        return new QuickAddBottomSheet();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        foodManager = FoodManager.getInstance(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_quick_add, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        

        mealTypeGroup = view.findViewById(R.id.meal_type_group);
        breakfastRadio = view.findViewById(R.id.radio_breakfast);
        lunchRadio = view.findViewById(R.id.radio_lunch);
        dinnerRadio = view.findViewById(R.id.radio_dinner);
        snackRadio = view.findViewById(R.id.radio_snack);
        nameInput = view.findViewById(R.id.name_input);
        caloriesInput = view.findViewById(R.id.calories_input);
        proteinsInput = view.findViewById(R.id.proteins_input);
        fatsInput = view.findViewById(R.id.fats_input);
        carbsInput = view.findViewById(R.id.carbs_input);
        addButton = view.findViewById(R.id.add_button);
        

        selectDefaultMealByTime();
        

        addButton.setOnClickListener(v -> addQuickNutrients());
    }
    

    private void selectDefaultMealByTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        

        if (hour >= 4 && hour < 11) {

            breakfastRadio.setChecked(true);
        } else if (hour >= 11 && hour < 16) {

            lunchRadio.setChecked(true);
        } else if (hour >= 16 && hour < 21) {

            dinnerRadio.setChecked(true);
        } else {

            snackRadio.setChecked(true);
        }
    }
    

    private void addQuickNutrients() {
        try {

            String nameStr = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String caloriesStr = caloriesInput.getText() != null ? caloriesInput.getText().toString() : "0";
            String proteinsStr = proteinsInput.getText() != null ? proteinsInput.getText().toString() : "0";
            String fatsStr = fatsInput.getText() != null ? fatsInput.getText().toString() : "0";
            String carbsStr = carbsInput.getText() != null ? carbsInput.getText().toString() : "0";
            

            double calories = caloriesStr.isEmpty() ? 0 : Double.parseDouble(caloriesStr);
            double proteins = proteinsStr.isEmpty() ? 0 : Double.parseDouble(proteinsStr);
            double fats = fatsStr.isEmpty() ? 0 : Double.parseDouble(fatsStr);
            double carbs = carbsStr.isEmpty() ? 0 : Double.parseDouble(carbsStr);
            

            if (calories <= 0) {
                Toast.makeText(requireContext(), "Укажите количество калорий", Toast.LENGTH_SHORT).show();
                return;
            }
            

            String mealType;
            int selectedId = mealTypeGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.radio_breakfast) {
                mealType = Constants.MEAL_TYPE_BREAKFAST;
            } else if (selectedId == R.id.radio_lunch) {
                mealType = Constants.MEAL_TYPE_LUNCH;
            } else if (selectedId == R.id.radio_dinner) {
                mealType = Constants.MEAL_TYPE_DINNER;
            } else if (selectedId == R.id.radio_snack) {
                mealType = Constants.MEAL_TYPE_SNACK;
            } else {
                Toast.makeText(requireContext(), "Выберите прием пищи", Toast.LENGTH_SHORT).show();
                return;
            }
            

            String foodName = nameStr.isEmpty() ? "Быстрое добавление" : nameStr;
            

            Food quickAddFood = new Food.Builder()
                .id(-1)
                .name(foodName)
                .calories((int) calories)
                .proteins((float) proteins)
                .fats((float) fats)
                .carbs((float) carbs)
                .build();
            

            SelectedFood selectedFood = new SelectedFood(quickAddFood, 100);
            

            Meal meal = foodManager.getMeal(mealType);
            if (meal == null) {
                meal = new Meal(getMealTitle(mealType), getMealIcon(mealType));
            }
            

            meal.addFood(selectedFood);
            

            foodManager.updateMeal(mealType, meal);
            

            if (listener != null) {
                listener.onFoodQuickAdded(mealType);
            }
            

            Toast.makeText(requireContext(), "Добавлено в " + getMealTitle(mealType).toLowerCase(), Toast.LENGTH_SHORT).show();
            dismiss();
            
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Некорректное значение", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка при добавлении нутриентов: " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Произошла ошибка", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Неизвестная ошибка: " + e.getMessage());
        }
    }
    

    private String getMealTitle(String mealType) {
        switch (mealType) {
            case Constants.MEAL_TYPE_BREAKFAST: return "Завтрак";
            case Constants.MEAL_TYPE_LUNCH: return "Обед";
            case Constants.MEAL_TYPE_DINNER: return "Ужин";
            case Constants.MEAL_TYPE_SNACK: return "Перекус";
            default: return "Прием пищи";
        }
    }
    

    private int getMealIcon(String mealType) {
        switch (mealType) {
            case Constants.MEAL_TYPE_BREAKFAST: return R.drawable.ic_breakfast;
            case Constants.MEAL_TYPE_LUNCH: return R.drawable.ic_lunch;
            case Constants.MEAL_TYPE_DINNER: return R.drawable.ic_dinner;
            case Constants.MEAL_TYPE_SNACK: return R.drawable.ic_snack;
            default: return R.drawable.ic_food;
        }
    }
} 