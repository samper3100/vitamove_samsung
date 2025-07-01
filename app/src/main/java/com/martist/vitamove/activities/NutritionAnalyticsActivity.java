package com.martist.vitamove.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.martist.vitamove.R;
import com.martist.vitamove.events.MealsLoadedEvent;
import com.martist.vitamove.events.TrackedNutrientsChangedEvent;
import com.martist.vitamove.fragments.NutrientSelectionBottomSheet;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.Meal;
import com.martist.vitamove.views.CustomCalendarDialog;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NutritionAnalyticsActivity extends BaseActivity {
    
    private static final String TAG = "NutritionAnalytics";
    

    private FoodManager foodManager;
    

    private TextView dateText;
    private TextView caloriesText;
    private TextView caloriesPercentText;
    private CircularProgressIndicator circularCaloriesProgress;
    

    private TextView proteinsValueText;
    private TextView fatsValueText;
    private TextView carbsValueText;
    private LinearProgressIndicator proteinsProgressBar;
    private LinearProgressIndicator fatsProgressBar;
    private LinearProgressIndicator carbsProgressBar;
    

    private LinearLayout vitaminsContainer;
    private LinearLayout mineralsContainer;
    private LinearLayout additionalNutrientsContainer;
    

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    

    private static final Map<String, Float> DAILY_NORMS = new HashMap<>();
    

    private ProgressBar loadingIndicator;
    

    private final Calendar calendar = Calendar.getInstance();
    
    static {

        

        DAILY_NORMS.put("calories", 2500f);
        DAILY_NORMS.put("proteins", 75f);
        DAILY_NORMS.put("fats", 83f);
        DAILY_NORMS.put("carbs", 365f);
        

        DAILY_NORMS.put("vitamin_a", 900f);
        DAILY_NORMS.put("vitamin_b1", 1.2f);
        DAILY_NORMS.put("vitamin_b2", 1.3f);
        DAILY_NORMS.put("vitamin_b3", 16f);
        DAILY_NORMS.put("vitamin_b5", 5f);
        DAILY_NORMS.put("vitamin_b6", 1.7f);
        DAILY_NORMS.put("vitamin_b9", 400f);
        DAILY_NORMS.put("vitamin_b12", 2.4f);
        DAILY_NORMS.put("vitamin_c", 90f);
        DAILY_NORMS.put("vitamin_d", 15f);
        DAILY_NORMS.put("vitamin_e", 15f);
        DAILY_NORMS.put("vitamin_k", 120f);
        

        DAILY_NORMS.put("calcium", 1000f);
        DAILY_NORMS.put("iron", 8f);
        DAILY_NORMS.put("magnesium", 400f);
        DAILY_NORMS.put("phosphorus", 700f);
        DAILY_NORMS.put("potassium", 3500f);
        DAILY_NORMS.put("sodium", 2300f);
        DAILY_NORMS.put("zinc", 11f);
        

        DAILY_NORMS.put("fiber", 25f);
        DAILY_NORMS.put("sugar", 50f);
        DAILY_NORMS.put("cholesterol", 300f);
        DAILY_NORMS.put("saturated_fats", 20f);
        DAILY_NORMS.put("trans_fats", 2f);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition_analytics);
        

        foodManager = FoodManager.getInstance(this);
        

        initViews();
        

        setupToolbar();
        

        updateNutritionData();
    }
    
    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }
    
    @Override
    protected void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }
    
    private void setupToolbar() {

        CardView toolbar = findViewById(R.id.toolbar);
        

        findViewById(R.id.back_button).setOnClickListener(v -> onBackPressed());
        

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> showNutrientSelectionBottomSheet());
        

        findViewById(R.id.date_prev_button).setOnClickListener(v -> changeDate(-1));
        findViewById(R.id.date_next_button).setOnClickListener(v -> changeDate(1));
        

        TextView dateTextView = findViewById(R.id.date_text);
        dateTextView.setClickable(true);
        dateTextView.setFocusable(true);
        dateTextView.setBackground(getResources().getDrawable(R.drawable.date_text_background));
        dateTextView.setCompoundDrawablesWithIntrinsicBounds(
                getResources().getDrawable(R.drawable.ic_calendar), null, null, null);
        dateTextView.setCompoundDrawablePadding(8);
        dateTextView.setOnClickListener(v -> showDatePickerDialog());
    }
    
    private void initViews() {

        dateText = findViewById(R.id.date_text);
        

        loadingIndicator = findViewById(R.id.loading_indicator);
        

        caloriesText = findViewById(R.id.calories_text);
        caloriesPercentText = findViewById(R.id.calories_percent);
        circularCaloriesProgress = findViewById(R.id.circular_calories_progress);
        

        proteinsValueText = findViewById(R.id.proteins_value);
        fatsValueText = findViewById(R.id.fats_value);
        carbsValueText = findViewById(R.id.carbs_value);
        proteinsProgressBar = findViewById(R.id.proteins_progress);
        fatsProgressBar = findViewById(R.id.fats_progress);
        carbsProgressBar = findViewById(R.id.carbs_progress);
        

        vitaminsContainer = findViewById(R.id.vitamins_container);
        mineralsContainer = findViewById(R.id.minerals_container);
        additionalNutrientsContainer = findViewById(R.id.additional_nutrients_container);
    }
    
    private void updateNutritionData() {
        showLoadingState(true);
        

        Date selectedDate = foodManager.getSelectedDateForView();
        dateText.setText(dateFormat.format(selectedDate));
        

        updateMacronutrients();
        

        updateVitamins();
        

        updateMinerals();
        

        updateAdditionalNutrients();
        
        showLoadingState(false);
    }
    
    private void updateMacronutrients() {

        float totalCalories = (float) foodManager.getTotalCaloriesForSelectedDate();
        float targetCalories = foodManager.getDailyNorm("calories");
        float totalProteins = 0;
        float totalFats = 0;
        float totalCarbs = 0;
        

        Meal breakfast = foodManager.getMeal("breakfast");
        Meal lunch = foodManager.getMeal("lunch");
        Meal dinner = foodManager.getMeal("dinner");
        Meal snack = foodManager.getMeal("snack");
        

        if (breakfast != null) {
            totalProteins += breakfast.getTotalProteins();
            totalFats += breakfast.getTotalFats();
            totalCarbs += breakfast.getTotalCarbs();
        }
        if (lunch != null) {
            totalProteins += lunch.getTotalProteins();
            totalFats += lunch.getTotalFats();
            totalCarbs += lunch.getTotalCarbs();
        }
        if (dinner != null) {
            totalProteins += dinner.getTotalProteins();
            totalFats += dinner.getTotalFats();
            totalCarbs += dinner.getTotalCarbs();
        }
        if (snack != null) {
            totalProteins += snack.getTotalProteins();
            totalFats += snack.getTotalFats();
            totalCarbs += snack.getTotalCarbs();
        }
        

        float targetProteins = foodManager.getDailyNorm("proteins");
        float targetFats = foodManager.getDailyNorm("fats");
        float targetCarbs = foodManager.getDailyNorm("carbs");
        

        caloriesText.setText(String.format(Locale.getDefault(), "%.0f/%.0f", totalCalories, targetCalories));
        

        int caloriesProgress = Math.min((int)((totalCalories / targetCalories) * 100), 150);
        circularCaloriesProgress.setProgress(caloriesProgress);
        

        int caloriesPercent = (int)((totalCalories / targetCalories) * 100);
        caloriesPercentText.setText(String.format(Locale.getDefault(), "%d%%", caloriesPercent));
        

        int textColor;
        if (caloriesPercent < 30) {
            textColor = ContextCompat.getColor(this, R.color.colorDanger);
        } else if (caloriesPercent < 70) {
            textColor = ContextCompat.getColor(this, R.color.colorWarning);
        } else {
            textColor = ContextCompat.getColor(this, R.color.colorSuccess);
        }
        caloriesPercentText.setTextColor(textColor);
        

        proteinsValueText.setText(String.format(Locale.getDefault(), "%.1f/%.0fг", totalProteins, targetProteins));
        proteinsProgressBar.setProgress(Math.min((int)((totalProteins / targetProteins) * 100), 150));
        

        fatsValueText.setText(String.format(Locale.getDefault(), "%.1f/%.0fг", totalFats, targetFats));
        fatsProgressBar.setProgress(Math.min((int)((totalFats / targetFats) * 100), 150));
        

        carbsValueText.setText(String.format(Locale.getDefault(), "%.1f/%.0fг", totalCarbs, targetCarbs));
        carbsProgressBar.setProgress(Math.min((int)((totalCarbs / targetCarbs) * 100), 150));
    }
    
    private void updateVitamins() {

        vitaminsContainer.removeAllViews();
        

        Date selectedDate = foodManager.getSelectedDateForView();
        String selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate);
        Map<String, Float> consumedNutrients = foodManager.getConsumedNutrients(selectedDateStr);
        

        if (consumedNutrients.getOrDefault("vitamin_a", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин A", consumedNutrients.getOrDefault("vitamin_a", 0f), DAILY_NORMS.get("vitamin_a"), "мкг");
        if (consumedNutrients.getOrDefault("vitamin_b1", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин B1", consumedNutrients.getOrDefault("vitamin_b1", 0f), DAILY_NORMS.get("vitamin_b1"), "мг");
        if (consumedNutrients.getOrDefault("vitamin_b2", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин B2", consumedNutrients.getOrDefault("vitamin_b2", 0f), DAILY_NORMS.get("vitamin_b2"), "мг");
        if (consumedNutrients.getOrDefault("vitamin_b3", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин B3", consumedNutrients.getOrDefault("vitamin_b3", 0f), DAILY_NORMS.get("vitamin_b3"), "мг");
        if (consumedNutrients.getOrDefault("vitamin_b5", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин B5", consumedNutrients.getOrDefault("vitamin_b5", 0f), DAILY_NORMS.get("vitamin_b5"), "мг");
        if (consumedNutrients.getOrDefault("vitamin_b6", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин B6", consumedNutrients.getOrDefault("vitamin_b6", 0f), DAILY_NORMS.get("vitamin_b6"), "мг");
        if (consumedNutrients.getOrDefault("vitamin_b9", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин B9", consumedNutrients.getOrDefault("vitamin_b9", 0f), DAILY_NORMS.get("vitamin_b9"), "мкг");
        if (consumedNutrients.getOrDefault("vitamin_b12", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин B12", consumedNutrients.getOrDefault("vitamin_b12", 0f), DAILY_NORMS.get("vitamin_b12"), "мкг");
        if (consumedNutrients.getOrDefault("vitamin_c", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин C", consumedNutrients.getOrDefault("vitamin_c", 0f), DAILY_NORMS.get("vitamin_c"), "мг");
        if (consumedNutrients.getOrDefault("vitamin_d", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин D", consumedNutrients.getOrDefault("vitamin_d", 0f), DAILY_NORMS.get("vitamin_d"), "мкг");
        if (consumedNutrients.getOrDefault("vitamin_e", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин E", consumedNutrients.getOrDefault("vitamin_e", 0f), DAILY_NORMS.get("vitamin_e"), "мг");
        if (consumedNutrients.getOrDefault("vitamin_k", 0f) > 0)
            addVitaminItem(vitaminsContainer, "Витамин K", consumedNutrients.getOrDefault("vitamin_k", 0f), DAILY_NORMS.get("vitamin_k"), "мкг");
    }
    
    private void updateMinerals() {

        mineralsContainer.removeAllViews();
        

        Date selectedDate = foodManager.getSelectedDateForView();
        String selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate);
        Map<String, Float> consumedNutrients = foodManager.getConsumedNutrients(selectedDateStr);
        

        if (consumedNutrients.getOrDefault("calcium", 0f) > 0)
            addMineralItem(mineralsContainer, "Кальций", consumedNutrients.getOrDefault("calcium", 0f), DAILY_NORMS.get("calcium"), "мг");
        if (consumedNutrients.getOrDefault("iron", 0f) > 0)
            addMineralItem(mineralsContainer, "Железо", consumedNutrients.getOrDefault("iron", 0f), DAILY_NORMS.get("iron"), "мг");
        if (consumedNutrients.getOrDefault("magnesium", 0f) > 0)
            addMineralItem(mineralsContainer, "Магний", consumedNutrients.getOrDefault("magnesium", 0f), DAILY_NORMS.get("magnesium"), "мг");
        if (consumedNutrients.getOrDefault("phosphorus", 0f) > 0)
            addMineralItem(mineralsContainer, "Фосфор", consumedNutrients.getOrDefault("phosphorus", 0f), DAILY_NORMS.get("phosphorus"), "мг");
        if (consumedNutrients.getOrDefault("potassium", 0f) > 0)
            addMineralItem(mineralsContainer, "Калий", consumedNutrients.getOrDefault("potassium", 0f), DAILY_NORMS.get("potassium"), "мг");
        if (consumedNutrients.getOrDefault("sodium", 0f) > 0)
            addMineralItem(mineralsContainer, "Натрий", consumedNutrients.getOrDefault("sodium", 0f), DAILY_NORMS.get("sodium"), "мг");
        if (consumedNutrients.getOrDefault("zinc", 0f) > 0)
            addMineralItem(mineralsContainer, "Цинк", consumedNutrients.getOrDefault("zinc", 0f), DAILY_NORMS.get("zinc"), "мг");
    }
    
    private void updateAdditionalNutrients() {

        additionalNutrientsContainer.removeAllViews();
        

        Date selectedDate = foodManager.getSelectedDateForView();
        String selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate);
        Map<String, Float> consumedNutrients = foodManager.getConsumedNutrients(selectedDateStr);
        

        if (consumedNutrients.getOrDefault("fiber", 0f) > 0)
            addNutrientItem(additionalNutrientsContainer, "Клетчатка", consumedNutrients.getOrDefault("fiber", 0f), DAILY_NORMS.get("fiber"), "г");
        if (consumedNutrients.getOrDefault("sugar", 0f) > 0)
            addNutrientItem(additionalNutrientsContainer, "Сахар", consumedNutrients.getOrDefault("sugar", 0f), DAILY_NORMS.get("sugar"), "г");
        if (consumedNutrients.getOrDefault("cholesterol", 0f) > 0)
            addNutrientItem(additionalNutrientsContainer, "Холестерин", consumedNutrients.getOrDefault("cholesterol", 0f), DAILY_NORMS.get("cholesterol"), "мг");
        if (consumedNutrients.getOrDefault("saturated_fats", 0f) > 0)
            addNutrientItem(additionalNutrientsContainer, "Насыщенные жиры", consumedNutrients.getOrDefault("saturated_fats", 0f), DAILY_NORMS.get("saturated_fats"), "г");
        if (consumedNutrients.getOrDefault("trans_fats", 0f) > 0)
            addNutrientItem(additionalNutrientsContainer, "Трансжиры", consumedNutrients.getOrDefault("trans_fats", 0f), DAILY_NORMS.get("trans_fats"), "г");
    }
    
    private void addVitaminItem(LinearLayout container, String name, float value, float norm, String unit) {
        if (value <= 0) return;
        
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_nutrient, container, false);
        
        TextView nameText = itemView.findViewById(R.id.nutrient_name);
        TextView valueText = itemView.findViewById(R.id.nutrient_value);
        LinearProgressIndicator progressBar = itemView.findViewById(R.id.nutrient_progress);
        TextView percentText = itemView.findViewById(R.id.nutrient_percent);
        
        nameText.setText(name);
        valueText.setText(String.format(Locale.getDefault(), "%.1f %s", value, unit));
        
        int percentOfNorm = (int) ((value / norm) * 100);
        
        progressBar.setMax(100);
        progressBar.setProgress(Math.min(percentOfNorm, 100));
        
        percentText.setText(String.format(Locale.getDefault(), "%d%%", percentOfNorm));
        

        int colorId;
        if (percentOfNorm < 30) {
            colorId = R.color.colorDanger;
        } else if (percentOfNorm < 70) {
            colorId = R.color.colorWarning;
        } else {
            colorId = R.color.colorSuccess;
        }
        
        int color = ContextCompat.getColor(this, colorId);
        percentText.setTextColor(color);
        progressBar.setIndicatorColor(color);
        
        container.addView(itemView);
    }
    
    private void addMineralItem(LinearLayout container, String name, float value, float norm, String unit) {

        addVitaminItem(container, name, value, norm, unit);
    }
    
    private void addNutrientItem(LinearLayout container, String name, float value, float norm, String unit) {

        addVitaminItem(container, name, value, norm, unit);
    }
    

    private void showNutrientSelectionBottomSheet() {
        NutrientSelectionBottomSheet bottomSheet = new NutrientSelectionBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), "NutrientSelectionBottomSheet");
    }
    

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTrackedNutrientsChanged(TrackedNutrientsChangedEvent event) {

        updateNutritionData();
    }
    

    private void showLoadingState(boolean isLoading) {
        if (isLoading) {
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            loadingIndicator.setVisibility(View.GONE);
        }
    }
    

    private void changeDate(int daysDelta) {

        showLoadingState(true);
        
        Date currentDate = foodManager.getSelectedDateForView();
        

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(currentDate);
        

        calendar.add(java.util.Calendar.DAY_OF_MONTH, daysDelta);
        

        Date newDate = calendar.getTime();
        foodManager.setSelectedDateForView(newDate);
        

        updateNutritionData();
    }
    

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMealsLoaded(MealsLoadedEvent event) {

        updateNutritionData();
    }
    

    private void showDatePickerDialog() {
        Date currentSelectedDate = foodManager.getSelectedDateForView();
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentSelectedDate);
        CalendarDay initialDay = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        CustomCalendarDialog dialog = new CustomCalendarDialog(initialDay, date -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(date.getYear(), date.getMonth() - 1, date.getDay());
            Date selectedDate = selectedCal.getTime();
            foodManager.setSelectedDateForView(selectedDate);
            updateNutritionData();
        });
        dialog.show(getSupportFragmentManager(), "CustomCalendarDialog");
    }
} 