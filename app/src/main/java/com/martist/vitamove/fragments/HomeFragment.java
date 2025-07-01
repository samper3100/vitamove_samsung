package com.martist.vitamove.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.martist.vitamove.R;
import com.martist.vitamove.activities.NutritionAnalyticsActivity;
import com.martist.vitamove.activities.WeightHistoryActivity;
import com.martist.vitamove.fragments.workout.AnalyticsFragment;
import com.martist.vitamove.managers.CaloriesManager;
import com.martist.vitamove.managers.DashboardManager;
import com.martist.vitamove.managers.WaterHistoryManager;
import com.martist.vitamove.models.DashboardData;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.viewmodels.UserWeightViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class HomeFragment extends Fragment {
    

    private TextView greetingText;
    private TextView dateText;
    

    private TextView stepsCount;
    private TextView stepsGoal;
    private ProgressBar stepsCircularProgress;
    private LineChart stepsChart;
    

    private TextView caloriesRemaining;
    private TextView caloriesFood;
    private TextView caloriesExercise;
    private ProgressBar caloriesCircularProgress;
    private TextView caloriesProgressText;
    private CardView potreb_calories;
    private CardView burned_calories;
    

    private TextView waterAmount;
    private TextView waterProgressText;
    private ProgressBar waterCircularProgress;
    private View btnAddWater200;
    private View btnAddWater500;
    

    private TextView initialWeightCardView;
    private TextView currentWeightCardView;
    private TextView targetWeightCardView;
    private CardView weightCard;
    private UserWeightViewModel weightViewModel;
    

    private DashboardManager dashboardManager;
    private DashboardData dashboardData;
    private CaloriesManager caloriesManager;
    private WaterHistoryManager waterHistoryManager;
    

    private Handler updateHandler;
    private static final int UPDATE_INTERVAL_MS = 10000;
    private boolean isUpdateActive = false;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUpdateActive && dashboardManager != null) {

                dashboardManager.syncStepsData();
                

                dashboardData = dashboardManager.getDashboardData();
                

                if (isAdded() && !isDetached() && getActivity() != null) {
                    stepsCount.setText(String.format(Locale.getDefault(), "%,d", dashboardData.getStepsToday()));
                    stepsCircularProgress.setProgress(dashboardData.getStepsProgress());
                    

                }
                

                updateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        }
    };


    private final BroadcastReceiver dashboardUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            

            SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
            int targetCalories = prefs.getInt("target_calories", 0);
            if (targetCalories > 0 && caloriesManager != null) {
                caloriesManager.setTargetCalories(targetCalories);

            }
            

            initializeCorrectCaloriesGoal();
            

            dashboardData = dashboardManager.getDashboardData();
            

            int burnedCalories = caloriesManager.getTotalBurnedCalories();
            int consumedCalories = caloriesManager.getConsumedCalories();
            

            dashboardData.setCaloriesBurned(burnedCalories);
            dashboardData.setCaloriesConsumed(consumedCalories);
            

            updateCaloriesUI(burnedCalories, consumedCalories);
            

            setupStepsCard();
            setupWaterCard();
            setupWeightCard();
            


        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
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
        

        dashboardManager = DashboardManager.getInstance(requireContext());
        caloriesManager = CaloriesManager.getInstance(requireContext());
        waterHistoryManager = WaterHistoryManager.getInstance(requireContext());
        

        updateHandler = new Handler(Looper.getMainLooper());
        

        weightViewModel = new ViewModelProvider(this).get(UserWeightViewModel.class);
        

        initializeCorrectCaloriesGoal();
        

        dashboardData = dashboardManager.getDashboardData();
        

        initViews(view);
        

        updateGreetingAndDate();
        setupStepsCard();
        setupCaloriesCard();
        setupWaterCard();
        setupWeightCard();
        

        setupButtonListeners();
        

        observeCaloriesChanges();
    }
    

    private void initViews(View view) {

        greetingText = view.findViewById(R.id.dashboard_greeting);
        dateText = view.findViewById(R.id.dashboard_date);
        

        stepsCount = view.findViewById(R.id.steps_count);
        stepsGoal = view.findViewById(R.id.steps_goal);
        stepsCircularProgress = view.findViewById(R.id.steps_circular_progress);
        stepsChart = view.findViewById(R.id.steps_chart);
        

        caloriesRemaining = view.findViewById(R.id.calories_remaining);
        caloriesFood = view.findViewById(R.id.calories_food);
        caloriesExercise = view.findViewById(R.id.calories_exercise);
        caloriesCircularProgress = view.findViewById(R.id.calories_circular_progress);
        caloriesProgressText = view.findViewById(R.id.calories_progress_text);
        

        waterAmount = view.findViewById(R.id.water_amount);
        waterProgressText = view.findViewById(R.id.water_progress_text);
        waterCircularProgress = view.findViewById(R.id.water_circular_progress);
        btnAddWater200 = view.findViewById(R.id.btn_add_water_200);
        btnAddWater500 = view.findViewById(R.id.btn_add_water_500);
        

        initialWeightCardView = view.findViewById(R.id.initial_weight_card);
        currentWeightCardView = view.findViewById(R.id.current_weight_card);
        targetWeightCardView = view.findViewById(R.id.target_weight_card);
        weightCard = view.findViewById(R.id.card_weight);
        

        potreb_calories = view.findViewById(R.id.home_calories_potreb);
        burned_calories = view.findViewById(R.id.home_calories_burned);
        

        setupButtonListeners();
    }
    

    private void updateGreetingAndDate() {

        SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String userName = prefs.getString("name", "Сергей");
        

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (timeOfDay < 12) {
            greeting = "Доброе утро";
        } else if (timeOfDay < 18) {
            greeting = "Добрый день";
        } else {
            greeting = "Добрый вечер";
        }
        
        greetingText.setText(greeting + ", " + userName + "!");
        

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM", new Locale("ru"));
        String currentDate = "Сегодня, " + dateFormat.format(new Date());
        dateText.setText(currentDate);
    }
    

    private void setupStepsCard() {

        stepsCount.setText(String.format(Locale.getDefault(), "%,d", dashboardData.getStepsToday()));
        stepsGoal.setText(String.format(Locale.getDefault(), "из %,d", dashboardData.getStepsGoal()));
        

        stepsCircularProgress.setProgress(dashboardData.getStepsProgress());
        

        setupStepsChart();
    }
    

    private void setupCaloriesCard() {

        if (caloriesManager == null) {
            Log.e("HomeFragment", "setupCaloriesCard: caloriesManager не инициализирован");
            return;
        }
        

        if (dashboardData == null) {
            Log.e("HomeFragment", "setupCaloriesCard: dashboardData не инициализирован");
            

            if (dashboardManager != null) {
                dashboardData = dashboardManager.getDashboardData();
            }
            

            if (dashboardData == null) {
                return;
            }
        }
        

        int caloriesBurned = caloriesManager.getTotalBurnedCalories();
        int caloriesConsumed = caloriesManager.getConsumedCalories();
        

        dashboardData.setCaloriesBurned(caloriesBurned);
        dashboardData.setCaloriesConsumed(caloriesConsumed);
        

        updateCaloriesUI(caloriesBurned, caloriesConsumed);
    }
    

    private void observeCaloriesChanges() {

        caloriesManager.getBurnedCaloriesLiveData().observe(getViewLifecycleOwner(), burnedCalories -> {

            updateCaloriesUI(burnedCalories, dashboardData.getCaloriesConsumed());

        });
        

        caloriesManager.getConsumedCaloriesLiveData().observe(getViewLifecycleOwner(), consumedCalories -> {

            dashboardData.setCaloriesConsumed(consumedCalories);
            

            updateCaloriesUI(dashboardData.getCaloriesBurned(), consumedCalories);

        });
    }
    

    private void updateCaloriesUI(int burnedCalories, int consumedCalories) {

        if (caloriesRemaining == null || caloriesFood == null || 
            caloriesExercise == null || caloriesCircularProgress == null) {
            Log.e("HomeFragment", "updateCaloriesUI: UI компоненты не инициализированы");
            return;
        }
        

        if (dashboardData == null) {
            Log.e("HomeFragment", "updateCaloriesUI: dashboardData не инициализирован");
            return;
        }
        

        int caloriesGoalValue = dashboardData.getCaloriesGoal();
        

        if (caloriesGoalValue <= 0) {

            initializeCorrectCaloriesGoal();

            caloriesGoalValue = dashboardData.getCaloriesGoal();
            

            if (caloriesGoalValue <= 0) {
                caloriesGoalValue = 2000;

            }
        }
        

        int totalAvailableCalories = caloriesGoalValue + burnedCalories;
        int remainingCalories = totalAvailableCalories - consumedCalories;
        

        if (remainingCalories < 0) {
            remainingCalories = 0;
        }
        

        caloriesRemaining.setText(String.format(Locale.getDefault(), "%,d", remainingCalories));
        caloriesFood.setText(String.format(Locale.getDefault(), "%,d", consumedCalories));
        caloriesExercise.setText(String.format(Locale.getDefault(), "%,d", burnedCalories));
        

        int consumedPercent = totalAvailableCalories > 0 ? 
            (int) (((float) consumedCalories / totalAvailableCalories) * 100) : 0;
        consumedPercent = Math.min(consumedPercent, 100);
        

              
        caloriesCircularProgress.setProgress(consumedPercent);
    }
    

    private void setupStepsChart() {

        List<Integer> weeklySteps = dashboardData.getWeeklySteps();
        
        ArrayList<Entry> entries = new ArrayList<>();
        

        for (int i = 0; i < weeklySteps.size(); i++) {
            entries.add(new Entry(i, weeklySteps.get(i)));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Шаги за неделю");
        dataSet.setColor(Color.WHITE);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setCircleHoleColor(Color.parseColor("#FF6A00"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        

        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.WHITE);
        dataSet.setFillAlpha(50);
        
        LineData lineData = new LineData(dataSet);
        
        stepsChart.setData(lineData);
        stepsChart.getDescription().setEnabled(false);
        

        stepsChart.getAxisLeft().setDrawGridLines(false);
        stepsChart.getAxisRight().setDrawGridLines(false);
        stepsChart.getXAxis().setDrawGridLines(false);
        

        YAxis leftAxis = stepsChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setTextSize(8f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawAxisLine(false);
        
        stepsChart.getAxisRight().setEnabled(false);
        

        XAxis xAxis = stepsChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(8f);
        xAxis.setDrawAxisLine(false);
        

        final String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        

        Legend legend = stepsChart.getLegend();
        legend.setEnabled(false);
        

        stepsChart.setTouchEnabled(true);
        stepsChart.setDragEnabled(true);
        stepsChart.setScaleEnabled(false);
        stepsChart.setPinchZoom(false);
        stepsChart.setHighlightPerTapEnabled(false);
        stepsChart.invalidate();
    }
    

    private void setupButtonListeners() {
        

        potreb_calories.setOnClickListener(v->startActivity(new Intent(getContext(), NutritionAnalyticsActivity.class)));
        burned_calories.setOnClickListener(v->getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,new AnalyticsFragment())
                .addToBackStack(null)
                .commit()

        );
        

        float[] waterPortions = loadWaterPortions();
        btnAddWater200.setOnClickListener(v -> addWater(waterPortions[0], "Быстрое добавление"));
        btnAddWater500.setOnClickListener(v -> addWater(waterPortions[1], "Быстрое добавление"));
        

        updateWaterButtonsText(waterPortions);
        

        View waterCard = getView().findViewById(R.id.card_water);
        waterCard.setOnClickListener(v -> {

            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new WaterBalanceFragment())
                .addToBackStack(null)
                .commit();
        });
        

        View stepsCard = getView().findViewById(R.id.card_steps);
        stepsCard.setOnClickListener(v -> {

            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new StepsStatsFragment())
                .addToBackStack(null)
                .commit();
            

        });
        

        weightCard.setOnClickListener(v -> {

            Intent intent = new Intent(getActivity(), WeightHistoryActivity.class);
            startActivity(intent);

        });
    }
    

    private float[] loadWaterPortions() {
        SharedPreferences prefs = requireContext().getSharedPreferences("water_portions_prefs", Context.MODE_PRIVATE);
        float portion1 = prefs.getFloat("portion_1", 0.2f);
        float portion2 = prefs.getFloat("portion_2", 0.5f);
        return new float[]{portion1, portion2};
    }
    

    private void updateWaterButtonsText(float[] portions) {

        View view200 = btnAddWater200.findViewById(android.R.id.text1);
        if (view200 == null) {

            ViewGroup container = (ViewGroup) ((ViewGroup) btnAddWater200).getChildAt(0);
            if (container != null && container instanceof LinearLayout) {
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child instanceof TextView) {
                        view200 = child;
                        break;
                    }
                }
            }
        }
        
        View view500 = btnAddWater500.findViewById(android.R.id.text1);
        if (view500 == null) {

            ViewGroup container = (ViewGroup) ((ViewGroup) btnAddWater500).getChildAt(0);
            if (container != null && container instanceof LinearLayout) {
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child instanceof TextView) {
                        view500 = child;
                        break;
                    }
                }
            }
        }
        

        if (view200 instanceof TextView) {
            ((TextView) view200).setText(String.format(Locale.getDefault(), "%.0f мл", portions[0] * 1000));
        }
        
        if (view500 instanceof TextView) {
            ((TextView) view500).setText(String.format(Locale.getDefault(), "%.0f мл", portions[1] * 1000));
        }
    }
    

    private void setupWaterCard() {

        float waterConsumed = waterHistoryManager.getTotalWaterConsumption();
        

        dashboardData.setWaterConsumed(waterConsumed);
        
        float waterGoal = dashboardData.getWaterGoal();
        

        waterAmount.setText(String.format(Locale.getDefault(), "%.1f / %.1f л", waterConsumed, waterGoal));
        

        int waterPercent = (int) ((waterConsumed / waterGoal) * 100);
        

        waterProgressText.setText(waterPercent + "%");
        

        waterCircularProgress.setProgress(Math.min(waterPercent, 100));

    }
    

    private void addWater(float amount, String description) {

        dashboardManager.addWaterConsumption(amount);
        

        waterHistoryManager.addWaterRecord(amount, description);
        

        dashboardData = dashboardManager.getDashboardData();
        

        setupWaterCard();
        

        String message = String.format(Locale.getDefault(), "Добавлено %.0f мл воды", amount * 1000);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    

    private void setupWeightCard() {

        SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        final float targetWeightPref = prefs.getFloat("target_weight", 0f);
        final float initialWeightPref = prefs.getFloat("initial_weight", 0f);
        final float currentWeightPref = prefs.getFloat("current_weight", 0f);
        

        final float initialWeight;
        if (initialWeightPref <= 0) {
            initialWeight = currentWeightPref;

            if (initialWeight > 0) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("initial_weight", initialWeight);
                editor.apply();
            }
        } else {
            initialWeight = initialWeightPref;
        }
        
        final float targetWeight = targetWeightPref;
        

        if (initialWeight > 0) {
            initialWeightCardView.setText(String.format(Locale.getDefault(), "%.1f кг", initialWeight));
        } else {
            initialWeightCardView.setText("–");
        }
        
        if (targetWeight > 0) {
            targetWeightCardView.setText(String.format(Locale.getDefault(), "%.1f кг", targetWeight));
        } else {
            targetWeightCardView.setText("–");
        }


        if (currentWeightPref > 0) {
            currentWeightCardView.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeightPref));

        } else {
            currentWeightCardView.setText("–");
        }


        weightViewModel.getLatestWeightRecord().observe(getViewLifecycleOwner(), weightRecord -> {
            if (weightRecord != null) {
                float weightFromDb = weightRecord.getWeight();
                

                if (currentWeightPref > 0 && Math.abs(currentWeightPref - weightFromDb) > 5) {



                    

                    currentWeightCardView.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeightPref));
                } else {

                    currentWeightCardView.setText(String.format(Locale.getDefault(), "%.1f кг", weightFromDb));
                    

                    if (Math.abs(currentWeightPref - weightFromDb) > 0.1) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putFloat("current_weight", weightFromDb);
                        editor.apply();

                    }
                }
                


            } else {

                if (currentWeightPref > 0) {
                    currentWeightCardView.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeightPref));
                } else {
                    currentWeightCardView.setText("–");
                }
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        

        IntentFilter filter = new IntentFilter("com.martist.vitamove.UPDATE_DASHBOARD");
        requireContext().registerReceiver(dashboardUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        

        if (dashboardManager != null) {

            dashboardManager.checkAndResetDailyDataIfNeeded();
            waterHistoryManager.checkAndResetDailyDataIfNeeded();
            

            dashboardManager.syncStepsData();
            

            dashboardData = dashboardManager.getDashboardData();
            

            dashboardData.setWaterConsumed(waterHistoryManager.getTotalWaterConsumption());
            

            float[] waterPortions = loadWaterPortions();
            updateWaterButtonsText(waterPortions);
            

            btnAddWater200.setOnClickListener(v -> addWater(waterPortions[0], "Быстрое добавление"));
            btnAddWater500.setOnClickListener(v -> addWater(waterPortions[1], "Быстрое добавление"));
            

            setupStepsCard();
            setupCaloriesCard();
            setupWaterCard();
            setupWeightCard();
            

            startPeriodicUpdates();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        

        try {
            requireContext().unregisterReceiver(dashboardUpdateReceiver);
        } catch (IllegalArgumentException e) {

            Log.e("HomeFragment", "Ошибка при отмене регистрации приемника: " + e.getMessage());
        }
        

        stopPeriodicUpdates();
    }
    

    private void startPeriodicUpdates() {
        if (!isUpdateActive) {
            isUpdateActive = true;
            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);

        }
    }
    

    private void stopPeriodicUpdates() {
        isUpdateActive = false;
        updateHandler.removeCallbacks(updateRunnable);

    }


    public void updateDashboardData() {
        if (dashboardManager != null) {

            dashboardManager.syncStepsData();
            

            dashboardData = dashboardManager.getDashboardData();
            

            updateGreetingAndDate();
            setupStepsCard();
            setupCaloriesCard();
            setupWaterCard();
            setupWeightCard();
            

        }
    }


    private void initializeCorrectCaloriesGoal() {
        try {

            SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
            

            int targetCalories = prefs.getInt("target_calories", 0);
            


            if (targetCalories <= 0 || targetCalories == 3178) {

                

                String name = prefs.getString("name", "Пользователь");
                int age = prefs.getInt("age", 30);
                String gender = prefs.getString("gender", "Мужчина");
                float currentWeight = prefs.getFloat("current_weight", 70);
                float targetWeight = prefs.getFloat("target_weight", 70);
                float height = prefs.getFloat("height", 175);
                float bodyFat = prefs.getFloat("body_fat", 20);
                float waist = prefs.getFloat("waist", 80);
                

                UserProfile userProfile = new UserProfile(name, age, gender, currentWeight, 
                                                         targetWeight, height, bodyFat, waist);
                userProfile.updateTargetCalories();
                

                targetCalories = userProfile.getTargetCalories();
                

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("target_calories", targetCalories);
                editor.apply();
                

                dashboardManager.updateCaloriesGoalFromProfile();
                

                caloriesManager.setTargetCalories(targetCalories);
                

            } else {

            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Ошибка при инициализации целевых калорий", e);
        }
    }
}
