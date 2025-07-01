package com.martist.vitamove.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.adapters.WaterHistoryAdapter;
import com.martist.vitamove.managers.DashboardManager;
import com.martist.vitamove.managers.WaterHistoryManager;
import com.martist.vitamove.models.DashboardData;
import com.martist.vitamove.models.WaterConsumptionRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class WaterBalanceFragment extends Fragment {


    private static final String PREF_NAME = "water_portions_prefs";
    private static final String KEY_PORTION_1 = "portion_1";
    private static final String KEY_PORTION_2 = "portion_2";
    private static final float DEFAULT_PORTION_1 = 0.2f;
    private static final float DEFAULT_PORTION_2 = 0.5f;


    private TextView waterAmount;
    private TextView waterGoalText;
    private TextView waterPercentText;
    private BarChart waterHistoryChart;
    private TabLayout chartPeriodToggle;
    private RecyclerView waterHistoryRecyclerView;
    private WaterHistoryAdapter waterHistoryAdapter;
    private MaterialButton addCustomWaterButton;
    private CircularProgressIndicator waterProgressIndicator;
    

    private View btnAddWater200;
    private View btnAddWater500;
    

    private MaterialButton btnEditPortions;
    

    private DashboardManager dashboardManager;
    private DashboardData dashboardData;
    private WaterHistoryManager waterHistoryManager;
    

    private final float[] waterPortions = new float[2];
    

    private int currentChartPeriod = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_water_balance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        

        setStatusBarColor();
        

        loadWaterPortions();
        

        dashboardManager = DashboardManager.getInstance(requireContext());
        waterHistoryManager = WaterHistoryManager.getInstance(requireContext());
        

        dashboardData = dashboardManager.getDashboardData();
        

        initViews(view);
        

        setupEventListeners();
        

        updateUI();
        

        updateWaterHistoryChart();
        

        loadWaterHistory();
    }
    

    private void loadWaterPortions() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        waterPortions[0] = prefs.getFloat(KEY_PORTION_1, DEFAULT_PORTION_1);
        waterPortions[1] = prefs.getFloat(KEY_PORTION_2, DEFAULT_PORTION_2);
    }
    

    private void saveWaterPortions() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_PORTION_1, waterPortions[0]);
        editor.putFloat(KEY_PORTION_2, waterPortions[1]);
        editor.apply();
    }
    

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActivity() != null) {
            Window window = getActivity().getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.water_blue_dark, null));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        

        setStatusBarColor();
        

        if (dashboardManager != null) {

            dashboardData = dashboardManager.getDashboardData();
            

            updateUI();
            

            updateWaterHistoryChart();
            

            loadWaterHistory();
        }
    }
    

    private void initViews(View view) {
        waterAmount = view.findViewById(R.id.water_amount);
        waterGoalText = view.findViewById(R.id.water_goal);
        waterPercentText = view.findViewById(R.id.water_percent);
        waterHistoryChart = view.findViewById(R.id.water_history_chart);
        chartPeriodToggle = view.findViewById(R.id.chart_period_toggle);
        waterHistoryRecyclerView = view.findViewById(R.id.water_history_recycler);
        addCustomWaterButton = view.findViewById(R.id.btn_add_custom_water);
        waterProgressIndicator = view.findViewById(R.id.water_progress_indicator);
        

        btnAddWater200 = view.findViewById(R.id.btn_add_water_200);
        btnAddWater500 = view.findViewById(R.id.btn_add_water_500);
        btnEditPortions = view.findViewById(R.id.btn_edit_portions);
        

        waterHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        waterHistoryAdapter = new WaterHistoryAdapter();
        waterHistoryRecyclerView.setAdapter(waterHistoryAdapter);
    }
    

    private void setupEventListeners() {

        btnAddWater200.setOnClickListener(v -> addWater(waterPortions[0], ""));
        btnAddWater500.setOnClickListener(v -> addWater(waterPortions[1], ""));
        

        btnEditPortions.setOnClickListener(v -> showEditPortionsDialog());
        

        chartPeriodToggle.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentChartPeriod = tab.getPosition();
                updateWaterHistoryChart();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        

        addCustomWaterButton.setOnClickListener(v -> showAddCustomWaterDialog());
    }
    

    private void updateUI() {
        float waterConsumed = dashboardData.getWaterConsumed();
        float waterGoal = dashboardData.getWaterGoal();
        

        waterAmount.setText(String.format(Locale.getDefault(), "%.1f л", waterConsumed));
        

        waterGoalText.setText(String.format(Locale.getDefault(), "из %.1f л", waterGoal));
        

        int waterPercent = (int) (dashboardData.getWaterProgress() * 100);
        waterPercentText.setText(String.format(Locale.getDefault(), "%d%%", waterPercent));
        

        waterProgressIndicator.setProgress(waterPercent);
        

        updatePortionButtonsText();
    }
    

    private void updatePortionButtonsText() {
        ((TextView)btnAddWater200.findViewById(R.id.portion_text)).setText(
                String.format(Locale.getDefault(), "%.0f мл", waterPortions[0] * 1000));
        
        ((TextView)btnAddWater500.findViewById(R.id.portion_text)).setText(
                String.format(Locale.getDefault(), "%.0f мл", waterPortions[1] * 1000));
    }
    

    private void addWater(float amount, String description) {

        dashboardManager.addWaterConsumption(amount);
        

        waterHistoryManager.addWaterRecord(amount, description);
        

        dashboardData = dashboardManager.getDashboardData();
        

        updateUI();
        

        updateWaterHistoryChart();
        

        loadWaterHistory();
        

        String message = String.format(Locale.getDefault(), "Добавлено %.0f мл воды", amount * 1000);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    

    private void showAddCustomWaterDialog() {

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Введите количество в мл");
        

        final EditText description = new EditText(requireContext());
        description.setInputType(InputType.TYPE_CLASS_TEXT);
        description.setHint("Добавьте примечание (необязательно)");
        

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);
        layout.addView(description);
        layout.setPadding(50, 20, 50, 20);
        

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Добавить воду");
        builder.setView(layout);
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            try {
                int mlValue = Integer.parseInt(input.getText().toString());
                if (mlValue > 0) {

                    float literValue = mlValue / 1000f;
                    addWater(literValue, description.getText().toString());
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Введите корректное значение", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    

    private void showEditPortionsDialog() {

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_water_portions_new, null);
        

        EditText portion1 = dialogView.findViewById(R.id.edit_portion_1);
        EditText portion2 = dialogView.findViewById(R.id.edit_portion_2);
        

        portion1.setText(String.format(Locale.getDefault(), "%.0f", waterPortions[0] * 1000));
        portion2.setText(String.format(Locale.getDefault(), "%.0f", waterPortions[1] * 1000));
        

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Изменить порции воды");
        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            try {

                waterPortions[0] = Float.parseFloat(portion1.getText().toString()) / 1000f;
                waterPortions[1] = Float.parseFloat(portion2.getText().toString()) / 1000f;
                

                saveWaterPortions();
                

                updatePortionButtonsText();
                
                Toast.makeText(requireContext(), "Порции воды обновлены", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Введите корректные значения", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    

    private void updateWaterHistoryChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        

        if (currentChartPeriod == 0) {

            List<Float> weeklyData = waterHistoryManager.getWeeklyWaterData();
            for (int i = 0; i < weeklyData.size(); i++) {
                entries.add(new BarEntry(i, weeklyData.get(i)));
            }
            

            labels.add("Пн");
            labels.add("Вт");
            labels.add("Ср");
            labels.add("Чт");
            labels.add("Пт");
            labels.add("Сб");
            labels.add("Вс");
        } else {

            List<Float> dailyData = waterHistoryManager.getDailyWaterDataForMonth();
            

            List<Float> filteredData = new ArrayList<>();
            List<String> filteredLabels = new ArrayList<>();
            
            for (int i = 0; i < dailyData.size(); i++) {
                float value = dailyData.get(i);
                if (value > 0) {

                    filteredData.add(value);

                    filteredLabels.add(String.valueOf(i + 1));
                }
            }
            

            if (filteredData.isEmpty()) {

                setupEmptyChart();
                return;
            }
            

            for (int i = 0; i < filteredData.size(); i++) {
                entries.add(new BarEntry(i, filteredData.get(i)));
            }
            

            labels = filteredLabels;
        }
        

        BarDataSet dataSet = new BarDataSet(entries, "Потребление воды");
        dataSet.setColor(getResources().getColor(R.color.water_blue, null));
        dataSet.setValueTextColor(getResources().getColor(R.color.white, null));
        dataSet.setValueTextSize(10f);
        

        dataSet.setDrawValues(false);
        

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        

        waterHistoryChart.setData(barData);
        waterHistoryChart.getDescription().setEnabled(false);
        waterHistoryChart.getLegend().setEnabled(false);
        

        XAxis xAxis = waterHistoryChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(getResources().getColor(R.color.text_secondary, null));
        xAxis.setTextSize(10f);
        

        if (currentChartPeriod == 1 && labels.size() > 7) {

            xAxis.setGranularity(1f);
            

            if (labels.size() > 15) {
                int labelCount = Math.min(10, labels.size() / 5 + 1);
                xAxis.setLabelCount(labelCount, true);
            }
            

            waterHistoryChart.setDragEnabled(true);
            

            int visibleCount = Math.min(7, labels.size());
            waterHistoryChart.setVisibleXRangeMaximum(visibleCount);
            

            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(labels.size() - 0.5f);
            

            waterHistoryChart.moveViewToX(0);
        } else {

            xAxis.setLabelCount(labels.size());
            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(labels.size() - 0.5f);
            waterHistoryChart.setDragEnabled(false);
        }
        
        YAxis leftAxis = waterHistoryChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_secondary, null));
        leftAxis.setTextSize(10f);
        
        waterHistoryChart.getAxisRight().setEnabled(false);
        

        WaterMarkerView mv = new WaterMarkerView(getContext(), R.layout.custom_marker_view, labels);
        mv.setChartView(waterHistoryChart);
        waterHistoryChart.setMarker(mv);
        

        waterHistoryChart.setScaleEnabled(false);
        

        waterHistoryChart.setHighlightPerTapEnabled(true);
        

        waterHistoryChart.setDoubleTapToZoomEnabled(false);
        

        waterHistoryChart.invalidate();
    }
    

    private void setupEmptyChart() {

        waterHistoryChart.clear();
        waterHistoryChart.setData(null);
        

        waterHistoryChart.setNoDataText("Нет данных о потреблении воды");
        waterHistoryChart.setNoDataTextColor(getResources().getColor(R.color.text_secondary, null));
        

        waterHistoryChart.invalidate();
    }
    

    private void loadWaterHistory() {

        List<WaterConsumptionRecord> historyRecords = waterHistoryManager.getWaterHistory();
        

        if (historyRecords.isEmpty()) {
            waterHistoryRecyclerView.setVisibility(View.GONE);
            return;
        }
        

        waterHistoryRecyclerView.setVisibility(View.VISIBLE);
        

        waterHistoryAdapter.setWaterRecords(historyRecords);
        

        waterHistoryAdapter.setOnDeleteClickListener(this::deleteWaterRecord);
    }


    private void deleteWaterRecord(int position) {

        boolean success = waterHistoryManager.deleteWaterRecord(position);
        
        if (success) {

            dashboardData = dashboardManager.getDashboardData();
            

            updateUI();
            loadWaterHistory();
            updateWaterHistoryChart();
            

            Toast.makeText(requireContext(), "Запись удалена", Toast.LENGTH_SHORT).show();
        } else {

            Toast.makeText(requireContext(), "Не удалось удалить запись", Toast.LENGTH_SHORT).show();
        }
    }


    private class WaterMarkerView extends MarkerView {
        private final TextView tvContent;
        private final List<String> labels;


        public WaterMarkerView(Context context, int layoutResource, List<String> labels) {
            super(context, layoutResource);
            this.labels = labels;
            tvContent = findViewById(R.id.tvContent);
            

            setBackgroundResource(R.drawable.water_tooltip_background);
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            int index = (int) e.getX();
            float water = e.getY();
            
            String label = "";
            if (labels != null && index >= 0 && index < labels.size()) {
                label = labels.get(index);
            }
            
            String format = currentChartPeriod == 0 ? "%s: %.2f л" : "День %s: %.2f л";
            

            tvContent.setText(String.format(Locale.getDefault(), format, label, water));
            
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {

            return new MPPointF(-(getWidth() / 2f), -getHeight() - 10);
        }
    }
} 