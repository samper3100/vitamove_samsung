package com.martist.vitamove.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.activities.FoodSelectionActivity;
import com.martist.vitamove.activities.NutritionAnalyticsActivity;
import com.martist.vitamove.activities.PortionSizeActivity;
import com.martist.vitamove.adapters.MealFoodsAdapter;
import com.martist.vitamove.managers.CaloriesManager;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.Food;
import com.martist.vitamove.models.Meal;
import com.martist.vitamove.models.SelectedFood;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.views.CustomCalendarDialog;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.greenrobot.eventbus.EventBus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CaloriesFragment extends Fragment {
    private static final String TAG = "CaloriesFragment";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());



    private final Map<String, Meal> mealMap = new HashMap<>();
    private ActivityResultLauncher<Intent> foodSelectionLauncher;
    private FoodManager foodManager;

    private final Map<String, MealCard> mealCards = new HashMap<>();

    View view;
    private final View[] dayViews = new View[7];
    private String selectedDate;
    private Calendar selectedWeekStart;


    private float targetCalories = 2000f;
    private float targetProteins = 90f;
    private float targetFats = 70f;
    private float targetCarbs = 250f;

    private CaloriesManager caloriesManager;
    private List<String> trackedNutrients;


    private final BroadcastReceiver caloriesUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            

            refreshNutrientsNorms();
            

            updateCaloriesCard();
            

        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        foodManager = new FoodManager(requireContext());
        caloriesManager = CaloriesManager.getInstance(requireContext());
        foodSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String mealType = data.getStringExtra("MEAL_TYPE");
                        SelectedFood selectedFood = data.getParcelableExtra("SELECTED_FOOD");



                        if (selectedFood != null && mealType != null) {
                            Meal meal = mealMap.get(mealType);
                            if (meal != null) {
                                meal.addFood(selectedFood);
                                updateMealCard(mealType);
                                updateCaloriesCard();
                            }
                        }
                    }
                });

        selectedDate = dateFormat.format(new Date());
        

        trackedNutrients = NutrientSelectionBottomSheet.getTrackedNutrients(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_calories, container, false);
        

        foodManager = FoodManager.getInstance(requireContext());
        caloriesManager = CaloriesManager.getInstance(requireContext());
        

        targetCalories = foodManager.getDailyNorm("calories");
        targetProteins = foodManager.getDailyNorm("proteins");
        targetFats = foodManager.getDailyNorm("fats");
        targetCarbs = foodManager.getDailyNorm("carbs");
        

        dayViews[0] = view.findViewById(R.id.day_1);
        dayViews[1] = view.findViewById(R.id.day_2);
        dayViews[2] = view.findViewById(R.id.day_3);
        dayViews[3] = view.findViewById(R.id.day_4);
        dayViews[4] = view.findViewById(R.id.day_5);
        dayViews[5] = view.findViewById(R.id.day_6);
        dayViews[6] = view.findViewById(R.id.day_7);

        MaterialButton calendarButton = view.findViewById(R.id.calendar_button);
        calendarButton.setOnClickListener(v -> showDatePicker());
        

        MaterialButton quickAddButton = view.findViewById(R.id.quick_add_button);
        quickAddButton.setOnClickListener(v -> showQuickAddDialog());
        

        initializeCalendar();
        

        initializeMealCards(view);
        




        
        return view;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        

        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow(). setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusbar_color));
            

            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
        

        updateCaloriesCard();
        

        caloriesManager.getConsumedCaloriesLiveData().observe(getViewLifecycleOwner(), calories -> {

            updateCaloriesCard();
        });
        
        caloriesManager.getBurnedCaloriesLiveData().observe(getViewLifecycleOwner(), calories -> {

            updateCaloriesCard();
        });


        foodManager.getCaloriesNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) {
                this.targetCalories = norm;
                updateCaloriesCard();

            }
        });
        foodManager.getProteinsNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) this.targetProteins = norm;
        });
        foodManager.getFatsNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) this.targetFats = norm;
        });
        foodManager.getCarbsNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) this.targetCarbs = norm;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        

        updateDayIndicators();
        


        foodManager.refreshNutrientNorms();
        
        updateAllMealCards();
        syncCaloriesWithManager();
        

        IntentFilter filter = new IntentFilter("com.martist.vitamove.UPDATE_DASHBOARD");
        requireContext().registerReceiver(caloriesUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroyView() {

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
        

        try {
            requireContext().unregisterReceiver(caloriesUpdateReceiver);
        } catch (IllegalArgumentException e) {

            Log.e(TAG, "Ошибка при отмене регистрации приемника: " + e.getMessage());
        }
    }

    private void updateCaloriesCard() {
        View caloriesCard = view.findViewById(R.id.calories_potreb);
        TextView totalCalories = caloriesCard.findViewById(R.id.total_calories);
        TextView proteinsValue = caloriesCard.findViewById(R.id.proteins_value);
        TextView fatsValue = caloriesCard.findViewById(R.id.fats_value);
        TextView carbsValue = caloriesCard.findViewById(R.id.carbs_value);
        ProgressBar progressBar = caloriesCard.findViewById(R.id.calories_progress);


        caloriesCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NutritionAnalyticsActivity.class);
            startActivity(intent);
        });


        Meal breakfast = foodManager.getMealForDate("breakfast", selectedDate);
        Meal lunch = foodManager.getMealForDate("lunch", selectedDate);
        Meal dinner = foodManager.getMealForDate("dinner", selectedDate);
        Meal snack = foodManager.getMealForDate("snack", selectedDate);


        double totalProt = 0;
        double totalFat = 0;
        double totalCarb = 0;
        double consumedCalories = 0;


        if (breakfast != null) {
            totalProt += breakfast.getTotalProteins();
            totalFat += breakfast.getTotalFats();
            totalCarb += breakfast.getTotalCarbs();
            consumedCalories += breakfast.getCalories();
        }
        if (lunch != null) {
            totalProt += lunch.getTotalProteins();
            totalFat += lunch.getTotalFats();
            totalCarb += lunch.getTotalCarbs();
            consumedCalories += lunch.getCalories();
        }
        if (dinner != null) {
            totalProt += dinner.getTotalProteins();
            totalFat += dinner.getTotalFats();
            totalCarb += dinner.getTotalCarbs();
            consumedCalories += dinner.getCalories();
        }
        if (snack != null) {
            totalProt += snack.getTotalProteins();
            totalFat += snack.getTotalFats();
            totalCarb += snack.getTotalCarbs();
            consumedCalories += snack.getCalories();
        }


        int burnedCalories = 0;
        

        String currentDateStr = dateFormat.format(new Date());
        boolean isCurrentDay = selectedDate.equals(currentDateStr);
        

        if (isCurrentDay) {
            burnedCalories = caloriesManager.getTotalBurnedCalories();
        } else {

            burnedCalories = 0; 
        }
        

        int totalAvailableCalories = (int)(targetCalories + burnedCalories);


        totalCalories.setText(String.format("%d/%d ккал", (int)consumedCalories, totalAvailableCalories));
        proteinsValue.setText(String.format("%.1f/%.0fг", totalProt, targetProteins));
        fatsValue.setText(String.format("%.1f/%.0fг", totalFat, targetFats));
        carbsValue.setText(String.format("%.1f/%.0fг", totalCarb, targetCarbs));


        int progress = totalAvailableCalories > 0 ? (int)((consumedCalories / (float)totalAvailableCalories) * 100) : 0;
        progress = Math.min(progress, 100);
        progressBar.setProgress(progress);




        

        updateTrackedNutrients();
    }

    private void updateMealCard(String mealType) {

        MealCard card = mealCards.get(mealType);
        if (card != null) {

            Meal meal = foodManager.getMealForDate(mealType, selectedDate);
            card.update(meal);
        }
    }

    private void initializeMealCards(View view) {
        View breakfastCardView = view.findViewById(R.id.breakfast_card);
        breakfastCardView.setTag(Constants.MEAL_TYPE_BREAKFAST);
        
        View lunchCardView = view.findViewById(R.id.lunch_card);
        lunchCardView.setTag(Constants.MEAL_TYPE_LUNCH);
        
        View dinnerCardView = view.findViewById(R.id.dinner_card);
        dinnerCardView.setTag(Constants.MEAL_TYPE_DINNER);
        
        View snackCardView = view.findViewById(R.id.snack_card);
        snackCardView.setTag(Constants.MEAL_TYPE_SNACK);
        
        mealCards.put(Constants.MEAL_TYPE_BREAKFAST,
                new MealCard(breakfastCardView, "Завтрак", R.drawable.ic_breakfast, Constants.MEAL_TYPE_BREAKFAST));
        mealCards.put(Constants.MEAL_TYPE_LUNCH,
                new MealCard(lunchCardView, "Обед", R.drawable.ic_lunch, Constants.MEAL_TYPE_LUNCH));
        mealCards.put(Constants.MEAL_TYPE_DINNER,
                new MealCard(dinnerCardView, "Ужин", R.drawable.ic_dinner, Constants.MEAL_TYPE_DINNER));
        mealCards.put(Constants.MEAL_TYPE_SNACK,
                new MealCard(snackCardView, "Перекус", R.drawable.ic_snack, Constants.MEAL_TYPE_SNACK));


    }

    private void openFoodSelection(String mealType) {
        Intent intent = new Intent(requireContext(), FoodSelectionActivity.class);
        intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
        foodSelectionLauncher.launch(intent);
    }




    private class MealCard {
        private final View cardView;
        private final RecyclerView foodList;
        private final TextView totalCalories;
        private final TextView proteinsValue;
        private final TextView fatsValue;
        private final TextView carbsValue;
        private final View divider;
        private final ImageView expandIcon;
        private final MealFoodsAdapter adapter;
        private boolean isExpanded = false;
        private final String mealType;

        MealCard(View cardView, String title, int iconResId, String mealType) {
            this.cardView = cardView;
            this.mealType = mealType;
            

            TextView titleView = cardView.findViewById(R.id.meal_title);
            titleView.setText(title);
            

            ImageView iconView = cardView.findViewById(R.id.meal_icon);
            iconView.setImageResource(iconResId);
            

            expandIcon = cardView.findViewById(R.id.expand_icon);
            

            divider = cardView.findViewById(R.id.divider);
            

            foodList = cardView.findViewById(R.id.food_list);
            foodList.setLayoutManager(new LinearLayoutManager(requireContext()));
            

            totalCalories = cardView.findViewById(R.id.total_calories);
            proteinsValue = cardView.findViewById(R.id.proteins_value);
            fatsValue = cardView.findViewById(R.id.fats_value);
            carbsValue = cardView.findViewById(R.id.carbs_value);
            

            adapter = new MealFoodsAdapter(new ArrayList<>(), mealType);
            foodList.setAdapter(adapter);
            

            adapter.setOnFoodClickListener((food, portionSize, mealTypeInner) -> {
                openPortionSizeActivity(food, mealTypeInner);
            });
            

            setupSwipeToDelete();
            

            Button addFoodButton = cardView.findViewById(R.id.add_button);
            if (addFoodButton != null) {
                addFoodButton.setOnClickListener(v -> openFoodSelection(mealType));
            }
            

            cardView.setOnClickListener(v -> toggleExpand());
        }

        private void toggleExpand() {

            Meal meal = foodManager.getMealForDate(mealType, selectedDate);
            

            if (meal != null && !meal.getFoods().isEmpty()) {
                isExpanded = !isExpanded;
                foodList.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                if (divider != null) {
                    divider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                }
                if (expandIcon != null) {
                    expandIcon.setRotation(isExpanded ? 180 : 0);
                }
            }
        }

        private void setupSwipeToDelete() {

            adapter.setupSwipeToDelete(foodList);
            
            adapter.setOnFoodRemovedListener(position -> {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    foodList.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                }


                Meal updatedMeal = foodManager.getMeal(mealType);
                if (updatedMeal == null) {
                    return;
                }
                

                if (position >= 0 && position < updatedMeal.getFoods().size()) {

                    String foodName = updatedMeal.getFood(position).getName();
                    

                    if (updatedMeal.removeFood(position)) {

                        

                        foodManager.updateMeal(mealType, updatedMeal);
                        

                        adapter.notifyItemRemoved(position);
                        if (position < updatedMeal.getFoods().size()) {
                            adapter.notifyItemRangeChanged(position, updatedMeal.getFoods().size() - position);
                        }
                        

                        totalCalories.setText(String.format("%.0f ккал", updatedMeal.getCalories()));
                        

                        View expandIconContainer = cardView.findViewById(R.id.expand_icon_container);
                        
                        if (!updatedMeal.getFoods().isEmpty()) {

                            proteinsValue.setText(String.format("%.1f г", updatedMeal.getTotalProteins()));
                            fatsValue.setText(String.format("%.1f г", updatedMeal.getTotalFats()));
                            carbsValue.setText(String.format("%.1f г", updatedMeal.getTotalCarbs()));
                            proteinsValue.setVisibility(View.VISIBLE);
                            fatsValue.setVisibility(View.VISIBLE);
                            carbsValue.setVisibility(View.VISIBLE);
                            

                            if (expandIconContainer != null) {
                                expandIconContainer.setVisibility(View.VISIBLE);
                            }
                        } else {

                            proteinsValue.setText("0 г");
                            fatsValue.setText("0 г");
                            carbsValue.setText("0 г");
                            proteinsValue.setVisibility(View.VISIBLE);
                            fatsValue.setVisibility(View.VISIBLE);
                            carbsValue.setVisibility(View.VISIBLE);
                            

                            if (expandIconContainer != null) {
                                expandIconContainer.setVisibility(View.GONE);
                            }
                            
                            divider.setVisibility(View.GONE);
                            foodList.setVisibility(View.GONE);
                            isExpanded = false;
                            

                            if (expandIcon != null) {
                                expandIcon.setRotation(0);
                            }
                        }
                        

                        calculateAndDisplayTotalNutrients();
                    } else {
                        Log.e(TAG, "Ошибка при удалении продукта");
                    }
                }
            });
        }
        

        private void calculateAndDisplayTotalNutrients() {

            updateCaloriesCard();
        }

        void update(Meal meal) {

            View expandIconContainer = cardView.findViewById(R.id.expand_icon_container);
            
            if (meal == null || meal.getFoods().isEmpty()) {
                totalCalories.setText("0 ккал");
                if (proteinsValue != null) proteinsValue.setText("0 г");
                if (fatsValue != null) fatsValue.setText("0 г");
                if (carbsValue != null) carbsValue.setText("0 г");
                

                if (expandIconContainer != null) {
                    expandIconContainer.setVisibility(View.GONE);
                }
                

                foodList.setVisibility(View.GONE);
                if (divider != null) {
                    divider.setVisibility(View.GONE);
                }
                

                isExpanded = false;
                if (expandIcon != null) {
                    expandIcon.setRotation(0);
                }
                
                return;
            }


            if (expandIconContainer != null) {
                expandIconContainer.setVisibility(View.VISIBLE);
            }


            float calories = meal.getCalories();
            float percentage = (targetCalories > 0) ? (calories / targetCalories) * 100 : 0;
            totalCalories.setText(String.format("%.0f ккал (%.0f%%)", calories, percentage));
            

            if (proteinsValue != null) {
                proteinsValue.setText(String.format("%.1f г", meal.getTotalProteins()));
            }
            
            if (fatsValue != null) {
                fatsValue.setText(String.format("%.1f г", meal.getTotalFats()));
            }
            
            if (carbsValue != null) {
                carbsValue.setText(String.format("%.1f г", meal.getTotalCarbs()));
            }


            adapter.updateFoods(meal.getFoods());
        }
    }

    private void initializeCalendar() {

        for (int i = 1; i <= 7; i++) {
            int viewId = getResources().getIdentifier("day_" + i, "id", requireContext().getPackageName());
            dayViews[i-1] = view.findViewById(viewId);
            
            final int dayIndex = i;
            dayViews[i-1].setOnClickListener(v -> selectDay(dayIndex));
        }


        Calendar now = Calendar.getInstance();
        now.setFirstDayOfWeek(Calendar.MONDAY);
        

        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        currentDayOfWeek = currentDayOfWeek == 1 ? 7 : currentDayOfWeek - 1;


        selectedWeekStart = (Calendar) now.clone();
        selectedWeekStart.add(Calendar.DAY_OF_MONTH, -(currentDayOfWeek - 1));


        selectDay(currentDayOfWeek);
    }

    private void selectDay(int dayIndex) {

        Calendar selectedDay = (Calendar) selectedWeekStart.clone();
        selectedDay.add(Calendar.DAY_OF_MONTH, dayIndex - 1);
        

        selectedDate = dateFormat.format(selectedDay.getTime());
        

        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {

                foodManager.setSelectedDateForView(date);
                updateDayIndicators();

                updateCaloriesCard();
                updateAllMealCards();
            }
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка при установке даты: " + e.getMessage());
            Toast.makeText(requireContext(), "Ошибка при выборе даты", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDayIndicators() {
        if (selectedWeekStart == null) return;
        
        Calendar calendar = (Calendar) selectedWeekStart.clone();
        

        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(calendar.getTime());
            

            if (date.equals(selectedDate)) {
                dayViews[i].setBackgroundResource(R.drawable.selected_day_background);
            }

            else if (foodManager.hasFoodForDate(date)) {
                dayViews[i].setBackgroundResource(R.drawable.day_circle_with_check);

            }

            else {
                dayViews[i].setBackgroundResource(R.drawable.day_circle_background);
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        try {
            Date currentDate = dateFormat.parse(selectedDate);
            if (currentDate != null) {
                cal.setTime(currentDate);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка парсинга даты: " + e.getMessage());
        }
        CalendarDay initialDay = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        CustomCalendarDialog dialog = new CustomCalendarDialog(initialDay, date -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(date.getYear(), date.getMonth() - 1, date.getDay());
            Date selectedDateObj = selectedCal.getTime();
            foodManager.setSelectedDateForView(selectedDateObj);

            int dayOfWeek = selectedCal.get(Calendar.DAY_OF_WEEK);
            dayOfWeek = dayOfWeek == 1 ? 7 : dayOfWeek - 1;
            selectedWeekStart = (Calendar) selectedCal.clone();
            selectedWeekStart.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1));
            selectDay(dayOfWeek);
            String formattedDate = new java.text.SimpleDateFormat("d MMMM yyyy", new java.util.Locale("ru"))
                .format(selectedCal.getTime());
            Toast.makeText(requireContext(), formattedDate, Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "CustomCalendarDialog");
    }

    private void updateAllMealCards() {
        updateMealCard("breakfast");
        updateMealCard("lunch");
        updateMealCard("dinner");
        updateMealCard("snack");
        syncCaloriesWithManager();
    }


    private void refreshNutrientsNorms() {
        if (foodManager != null) {
            targetCalories = foodManager.getDailyNorm("calories");
            targetProteins = foodManager.getDailyNorm("proteins");
            targetFats = foodManager.getDailyNorm("fats");
            targetCarbs = foodManager.getDailyNorm("carbs");
            




            updateCaloriesCard();
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        

        new Handler().postDelayed(this::refreshNutrientsNorms, 300);
    }


    private void openPortionSizeActivity(Food food, String mealType) {

        int portionSize = 100;
        Meal meal = foodManager.getMeal(mealType);
        
        if (meal != null) {

            for (Meal.FoodPortion foodPortion : meal.getFoods()) {
                if (foodPortion.getFood().getId() == food.getId()) {

                    portionSize = foodPortion.getPortionSize();
                    break;
                }
            }
        }
        
        Intent intent = new Intent(requireContext(), PortionSizeActivity.class);
        intent.putExtra(Constants.EXTRA_FOOD, food);
        intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
        intent.putExtra(Constants.EXTRA_PORTION_SIZE, portionSize);
        intent.putExtra(Constants.EXTRA_SELECTED_DATE, selectedDate);
        startActivity(intent);
    }


    private void syncCaloriesWithManager() {

        String currentDateStr = dateFormat.format(new Date());
        String selectedDateStr = foodManager.getSelectedDateFormatted();
        

        int totalConsumedCalories = (int)foodManager.getTotalCaloriesForSelectedDate();
        

        if (currentDateStr.equals(selectedDateStr)) {
            caloriesManager.setConsumedCalories(totalConsumedCalories);

        } else {

        }
    }


    private void showQuickAddDialog() {
        QuickAddBottomSheet bottomSheet = QuickAddBottomSheet.newInstance();

        bottomSheet.setListener(mealType -> {

            updateMealCard(mealType);

            updateCaloriesCard();

            syncCaloriesWithManager();
        });
        bottomSheet.show(getChildFragmentManager(), "QuickAddBottomSheet");
    }


    private void updateTrackedNutrients() {

        View view = getView();
        if (view == null) {

            return;
        }
        

        View nutrientsContainer = view.findViewById(R.id.tracked_nutrients_container);
        View micronutrientsCard = view.findViewById(R.id.micronutrients_card);
        if (nutrientsContainer != null ) {

            trackedNutrients = NutrientSelectionBottomSheet.getTrackedNutrients(requireContext());
            

            if (trackedNutrients.isEmpty()) {
                nutrientsContainer.setVisibility(View.GONE);
                micronutrientsCard.setVisibility(View.GONE);
                return;
            }
            

            nutrientsContainer.setVisibility(View.VISIBLE);
            micronutrientsCard.setVisibility(View.VISIBLE);


            ((ViewGroup) nutrientsContainer).removeAllViews();
            

            Map<String, Float> consumedNutrients = foodManager.getConsumedNutrients(selectedDate);
            Map<String, Float> normNutrients = foodManager.getNutrientNorms();
            

            Map<String, String> nutrientNames = getNutrientNames();
            

            for (String nutrientId : trackedNutrients) {
                String name = nutrientNames.getOrDefault(nutrientId, nutrientId);
                float consumed = consumedNutrients.getOrDefault(nutrientId, 0f);
                float norm = normNutrients.getOrDefault(nutrientId, 100f);
                

                View nutrientView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_tracked_nutrient, (ViewGroup) nutrientsContainer, false);
                

                TextView nameView = nutrientView.findViewById(R.id.nutrient_name);
                TextView valueView = nutrientView.findViewById(R.id.nutrient_value);
                TextView percentView = nutrientView.findViewById(R.id.nutrient_percent);
                ProgressBar progressBar = nutrientView.findViewById(R.id.nutrient_progress);
                
                nameView.setText(name);
                

                String unit = getNutrientUnit(nutrientId);
                valueView.setText(String.format(Locale.getDefault(), "%.1f %s", consumed, unit));
                

                int percent = (int) (consumed * 100 / norm);
                percentView.setText(String.format(Locale.getDefault(), "%d%%", percent));
                

                if (percent < 30) {
                    percentView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorDanger));
                } else if (percent < 70) {
                    percentView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorWarning));
                } else {
                    percentView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSuccess));
                }
                

                progressBar.setProgress(Math.min(percent, 100));
                

                ((ViewGroup) nutrientsContainer).addView(nutrientView);
            }
        }
    }
    

    private Map<String, String> getNutrientNames() {
        Map<String, String> names = new HashMap<>();
        

        names.put("vitamin_a", "Витамин A");
        names.put("vitamin_b1", "Витамин B1");
        names.put("vitamin_b2", "Витамин B2");
        names.put("vitamin_b3", "Витамин B3");
        names.put("vitamin_b5", "Витамин B5");
        names.put("vitamin_b6", "Витамин B6");
        names.put("vitamin_b9", "Витамин B9");
        names.put("vitamin_b12", "Витамин B12");
        names.put("vitamin_c", "Витамин C");
        names.put("vitamin_d", "Витамин D");
        names.put("vitamin_e", "Витамин E");
        names.put("vitamin_k", "Витамин K");
        

        names.put("calcium", "Кальций");
        names.put("iron", "Железо");
        names.put("magnesium", "Магний");
        names.put("phosphorus", "Фосфор");
        names.put("potassium", "Калий");
        names.put("sodium", "Натрий");
        names.put("zinc", "Цинк");
        

        names.put("fiber", "Клетчатка");
        names.put("sugar", "Сахар");
        names.put("cholesterol", "Холестерин");
        names.put("saturated_fats", "Насыщ. жиры");
        names.put("trans_fats", "Трансжиры");
        
        return names;
    }
    

    private String getNutrientUnit(String nutrientId) {
        if (nutrientId.startsWith("vitamin_") || 
            nutrientId.equals("iron") || 
            nutrientId.equals("zinc")) {
            return "мг";
        } else if (nutrientId.equals("vitamin_a") || 
                   nutrientId.equals("vitamin_d") || 
                   nutrientId.equals("vitamin_e")) {
            return "мкг";
        } else if (nutrientId.equals("cholesterol")) {
            return "мг";
        } else if (nutrientId.equals("sodium") || 
                   nutrientId.equals("potassium") || 
                   nutrientId.equals("calcium") ||
                   nutrientId.equals("magnesium") ||
                   nutrientId.equals("phosphorus")) {
            return "мг";
        } else {
            return "г";
        }
    }
} 