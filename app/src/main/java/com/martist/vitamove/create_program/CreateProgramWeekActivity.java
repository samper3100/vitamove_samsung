package com.martist.vitamove.create_program;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.activities.BaseActivity;
import com.martist.vitamove.create_program.adapter.CreateWeekDayAdapter;
import com.martist.vitamove.create_program.model.CreateProgramDay;
import com.martist.vitamove.ui.program.constructor.DayExerciseSelectionActivity;
import com.martist.vitamove.viewmodels.ExerciseListViewModel;
import com.martist.vitamove.workout.data.managers.ProgramTemplateManager;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ProgramTemplate;
import com.martist.vitamove.workout.data.models.ProgramTemplateDay;
import com.martist.vitamove.workout.data.models.ProgramTemplateExercise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateProgramWeekActivity extends BaseActivity implements
        CreateWeekDayAdapter.OnDayClickListener,
        CreateWeekDayAdapter.ExerciseActionCallback {


    public static final String EXTRA_TOTAL_WEEKS = "com.vitamove.app.EXTRA_TOTAL_WEEKS";
    public static final String EXTRA_NAVIGATE_TO_PROGRAMS = "com.vitamove.app.NAVIGATE_TO_PROGRAMS";

    private TextView tvWeekTitle;
    private RecyclerView rvDays;
    private Button btnCopyWeek;
    private Button btnNextWeek;
    private Button btnCreateProgram;
    private CreateWeekDayAdapter dayAdapter;
    private ExerciseListViewModel exerciseListViewModel;
    private ProgressBar progressBarSaving;
    private View dimOverlay;
    private ProgramTemplateManager templateManager;

    private int currentWeek = 1;
    private int totalWeeks = 1;
    private int numberOfDaysPerWeek = 0;


    private Map<Integer, List<String>> selectedExerciseIdsPerDay = new HashMap<>();

    private List<CreateProgramDay> currentWeekDays = new ArrayList<>();

    private ActivityResultLauncher<Intent> exerciseSelectionLauncher;

    private int replacingDayNumber = -1;
    private int replacingExercisePosition = -1;


    private String programName;
    private String programLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_program_week);

        exerciseListViewModel = new ViewModelProvider(this).get(ExerciseListViewModel.class);
        templateManager = new ProgramTemplateManager(this);


        numberOfDaysPerWeek = getIntent().getIntExtra("NUMBER_OF_DAYS", 0);
        totalWeeks = getIntent().getIntExtra(EXTRA_TOTAL_WEEKS, 1);
        programName = getIntent().getStringExtra("PROGRAM_NAME");
        programLevel = getIntent().getStringExtra("PROGRAM_LEVEL");


        if (programName == null || programName.isEmpty()) {
            programName = getString(R.string.default_program_name);
        }
        if (programLevel == null || programLevel.isEmpty()) {
            programLevel = "MEDIUM";
        }

        

        findViews();
        setupRecyclerView();
        updateWeekTitle();
        setupButtonClickListeners();
        registerActivityLauncher();
        loadWeekData(currentWeek);
        updateButtonStates();
    }

    private void findViews() {
        tvWeekTitle = findViewById(R.id.tv_week_title);
        rvDays = findViewById(R.id.rv_days);
        btnCopyWeek = findViewById(R.id.btn_copy_week);
        btnNextWeek = findViewById(R.id.btn_next_week);
        btnCreateProgram = findViewById(R.id.btn_create_program);
        progressBarSaving = findViewById(R.id.progress_bar_saving);
        dimOverlay = findViewById(R.id.dim_overlay);
    }

    private void setupRecyclerView() {
        dayAdapter = new CreateWeekDayAdapter(this, this);
        rvDays.setLayoutManager(new LinearLayoutManager(this));
        rvDays.setAdapter(dayAdapter);
    }

    private void updateWeekTitle() {
        tvWeekTitle.setText(String.format("Неделя %d", currentWeek));
    }

    private void setupButtonClickListeners() {
        btnNextWeek.setOnClickListener(v -> {
            if (!validateCurrentWeekDaysFilled()) { return; }
            
            currentWeek++;
            updateWeekTitle();
            loadWeekData(currentWeek);
            updateButtonStates();
        });

        btnCopyWeek.setOnClickListener(v -> {
            if (!validateCurrentWeekDaysFilled()) { return; }
            

            for (int dayNum = 1; dayNum <= numberOfDaysPerWeek; dayNum++) {
                int currentDayKey = generateDayKey(currentWeek, dayNum);
                int nextWeekDayKey = generateDayKey(currentWeek + 1, dayNum);

                List<String> idsToCopy = selectedExerciseIdsPerDay.get(currentDayKey);

                if (idsToCopy != null) {

                    selectedExerciseIdsPerDay.put(nextWeekDayKey, new ArrayList<>(idsToCopy));
                    
                } else {


                    selectedExerciseIdsPerDay.remove(nextWeekDayKey);
                     
                }
            }

            currentWeek++;
            updateWeekTitle();
            loadWeekData(currentWeek);
            updateButtonStates();
            Toast.makeText(this, "Неделя скопирована", Toast.LENGTH_SHORT).show();
        });


        btnCreateProgram.setOnClickListener(v -> {

            if (!validateCurrentWeekDaysFilled()) {
                Toast.makeText(this, "Заполните упражнения для всех дней последней недели", Toast.LENGTH_LONG).show();
                return;
            }


            showLoading(true);


            String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
            if (userId == null || userId.isEmpty()) {
                showLoading(false);
                Toast.makeText(this, "Не удалось определить ID пользователя", Toast.LENGTH_LONG).show();
                return;
            }


            

            templateManager.createTemplateAsync(
                userId,
                "",
                programName,
                getString(R.string.program_type_user_created_description),
                "CUSTOM",
                totalWeeks,
                numberOfDaysPerWeek,
                programLevel,
                false,
                new ProgramTemplateManager.AsyncCallback<ProgramTemplate>() {
                    @Override
                    public void onSuccess(ProgramTemplate template) {

                        saveAllWeeksAndExercises(template.getId(), new ProgramTemplateManager.AsyncCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(CreateProgramWeekActivity.this, 
                                            "Программа успешно сохранена!", Toast.LENGTH_SHORT).show();
                                    

                                    Intent intent = new Intent(CreateProgramWeekActivity.this, com.martist.vitamove.activities.MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra(EXTRA_NAVIGATE_TO_PROGRAMS, true);
                                    startActivity(intent);
                                    finish();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(CreateProgramWeekActivity.this, 
                                            "Ошибка при сохранении программы: " + e.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                    Log.e("CreateProgramWeek", "Ошибка сохранения программы", e);
                                });
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(CreateProgramWeekActivity.this, 
                                    "Ошибка при создании программы: " + e.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                            Log.e("CreateProgramWeek", "Ошибка создания программы", e);
                        });
                    }
                }
            );
        });
    }


    private void updateButtonStates() {
        if (currentWeek >= totalWeeks) {

            btnNextWeek.setVisibility(View.GONE);
            btnCopyWeek.setVisibility(View.GONE);
            btnCreateProgram.setVisibility(View.VISIBLE);
            
        } else {

            btnNextWeek.setVisibility(View.VISIBLE);
            btnCopyWeek.setVisibility(View.VISIBLE);
            btnCreateProgram.setVisibility(View.GONE);
            
        }
    }


    private boolean validateCurrentWeekDaysFilled() {
        boolean allDaysConfigured = true;
        if (currentWeekDays == null || currentWeekDays.isEmpty()) {
             Toast.makeText(this, "Нет дней для проверки.", Toast.LENGTH_SHORT).show();
             return false;
        }
        for (CreateProgramDay day : currentWeekDays) {
            if (day.getSelectedExercises() == null || day.getSelectedExercises().isEmpty()) {
                allDaysConfigured = false;
                String message = String.format("Пожалуйста, добавьте упражнения для Дня %d", day.getDayNumber());
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                break;
            }
        }
        return allDaysConfigured;
    }

    private void loadWeekData(int weekNumber) {
        currentWeekDays.clear();
        if (numberOfDaysPerWeek > 0) {
            for (int i = 1; i <= numberOfDaysPerWeek; i++) {
                CreateProgramDay day = new CreateProgramDay(i);
                int dayKey = generateDayKey(weekNumber, i);


                List<String> savedExerciseIds = selectedExerciseIdsPerDay.get(dayKey);
                if (savedExerciseIds != null && !savedExerciseIds.isEmpty()) {
                    List<Exercise> exercises = getExercisesFromCache(savedExerciseIds);
                    day.setSelectedExercises(exercises);
                }
                currentWeekDays.add(day);
            }
        }
        dayAdapter.setDays(currentWeekDays);
    }


    private List<Exercise> getExercisesFromCache(List<String> exerciseIds) {
        List<Exercise> exercises = new ArrayList<>();
        if (exerciseIds == null || exerciseListViewModel == null) {
            return exercises;
        }
        for (String id : exerciseIds) {
            Exercise exercise = exerciseListViewModel.getExerciseById(id);
            if (exercise != null) {
                exercises.add(exercise);
            } else {

                

            }
        }
        return exercises;
    }

    @Override
    public void onDayClick(int dayNumber) {

        replacingDayNumber = -1;
        replacingExercisePosition = -1;
        
        launchExerciseSelection(dayNumber);
    }

    @Override
    public void onReplaceExerciseRequest(int dayNumber, int exercisePosition, String exerciseId) {
        this.replacingDayNumber = dayNumber;
        this.replacingExercisePosition = exercisePosition;
        

        launchExerciseSelection(dayNumber, true);
    }

    private void launchExerciseSelection(int dayNumber, boolean isReplacement) {
        if (dayNumber == -1 && replacingDayNumber == -1) {
            Log.e("CreateProgramWeek", "Попытка запустить выбор упражнений без указания дня!");
            return;
        }

        Intent intent = new Intent(this, DayExerciseSelectionActivity.class);
        int targetDay = (replacingDayNumber != -1) ? replacingDayNumber : dayNumber;

        intent.putExtra(DayExerciseSelectionActivity.EXTRA_WEEK_NUMBER, currentWeek);
        intent.putExtra(DayExerciseSelectionActivity.EXTRA_DAY_NUMBER, targetDay);

        intent.putExtra(DayExerciseSelectionActivity.EXTRA_IS_REPLACEMENT_MODE, isReplacement);


        int dayKey = generateDayKey(currentWeek, targetDay);
        ArrayList<String> currentSelectionIds = (ArrayList<String>) selectedExerciseIdsPerDay.get(dayKey);
        if (currentSelectionIds != null && !currentSelectionIds.isEmpty()) {

            

        }

        exerciseSelectionLauncher.launch(intent);
    }


    private void launchExerciseSelection(int dayNumber) {
        launchExerciseSelection(dayNumber, false);
    }

    private void registerActivityLauncher() {
        exerciseSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int week = data.getIntExtra(DayExerciseSelectionActivity.EXTRA_WEEK_NUMBER, -1);
                        int day = data.getIntExtra(DayExerciseSelectionActivity.EXTRA_DAY_NUMBER, -1);
                        ArrayList<String> selectedIds = data.getStringArrayListExtra(DayExerciseSelectionActivity.EXTRA_SELECTED_EXERCISES);


                        if (week == currentWeek && day != -1) {
                            int dayPosition = findDayPosition(day);
                            if (dayPosition == -1) {
                                Log.e("CreateProgramWeek", "Не удалось найти позицию для дня " + day);

                                replacingDayNumber = -1;
                                replacingExercisePosition = -1;
                                return;
                            }


                            if (replacingExercisePosition != -1 && replacingDayNumber == day) {
                                
                                if (selectedIds != null && !selectedIds.isEmpty()) {
                                    String newExerciseId = selectedIds.get(0);
                                    
                                    Exercise newExercise = exerciseListViewModel.getExerciseById(newExerciseId);

                                    if (newExercise != null) {
                                        try {

                                            List<Exercise> existingExercises = currentWeekDays.get(dayPosition).getSelectedExercises();

                                            List<String> existingIds = selectedExerciseIdsPerDay.getOrDefault(generateDayKey(week, day), new ArrayList<>());


                                            if (replacingExercisePosition >= 0 && replacingExercisePosition < existingExercises.size()) {

                                                existingExercises.set(replacingExercisePosition, newExercise);

                                                 if (replacingExercisePosition < existingIds.size()) {
                                                      existingIds.set(replacingExercisePosition, newExerciseId);
                                                 } else {

                                                      
                                                      existingIds.add(newExerciseId);
                                                 }
                                                selectedExerciseIdsPerDay.put(generateDayKey(week, day), existingIds);


                                                dayAdapter.notifyItemChanged(dayPosition);
                                                Toast.makeText(this, "Упражнение заменено", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Log.e("CreateProgramWeek", "Ошибка: Неверная позиция для замены (" + replacingExercisePosition + ")");
                                                Toast.makeText(this, "Ошибка индекса при замене", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception e) {
                                            Log.e("CreateProgramWeek", "Ошибка при замене упражнения", e);
                                            Toast.makeText(this, "Ошибка замены упражнения", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                         Log.e("CreateProgramWeek", "Не удалось найти детали для нового упражнения с ID: " + newExerciseId);
                                         Toast.makeText(this, "Ошибка: Упражнение для замены не найдено", Toast.LENGTH_SHORT).show();
                                    }
                                } else {

                                    Toast.makeText(this, "Замена отменена: упражнение не выбрано", Toast.LENGTH_SHORT).show();
                                }

                                replacingDayNumber = -1;
                                replacingExercisePosition = -1;
                            }

                            else if (replacingExercisePosition == -1) {
                                
                                if (selectedIds != null) {
                                     int dayKey = generateDayKey(week, day);
                                     selectedExerciseIdsPerDay.put(dayKey, selectedIds);
                                     List<Exercise> selectedExercises = getExercisesFromCache(selectedIds);
                                     currentWeekDays.get(dayPosition).setSelectedExercises(selectedExercises);
                                     dayAdapter.notifyItemChanged(dayPosition);
                                     String message = String.format("День %d обновлен: %d упр.", day, selectedIds.size());
                                     Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                } else {
                                     
                                     int dayKey = generateDayKey(week, day);
                                     selectedExerciseIdsPerDay.put(dayKey, new ArrayList<>());
                                     currentWeekDays.get(dayPosition).setSelectedExercises(new ArrayList<>());
                                     dayAdapter.notifyItemChanged(dayPosition);
                                     Toast.makeText(this, "Упражнения для дня " + day + " очищены", Toast.LENGTH_SHORT).show();
                                }
                            } else {

                                 
                                 replacingDayNumber = -1;
                                 replacingExercisePosition = -1;
                            }
                        } else {
                             
                             replacingDayNumber = -1;
                             replacingExercisePosition = -1;
                             Toast.makeText(this, "Ошибка получения данных или неверная неделя", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                         
                         replacingDayNumber = -1;
                         replacingExercisePosition = -1;
                         Toast.makeText(this, "Выбор упражнений отменен", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private int findDayPosition(int dayNumber) {
        for (int i = 0; i < currentWeekDays.size(); i++) {
            if (currentWeekDays.get(i).getDayNumber() == dayNumber) {
                return i;
            }
        }
        return -1;
    }

    private int generateDayKey(int week, int day) {
        return week * 100 + day;
    }


    private void saveAllWeeksAndExercises(String templateId, ProgramTemplateManager.AsyncCallback<Boolean> callback) {

        int totalDays = totalWeeks * numberOfDaysPerWeek;
        final AtomicInteger savedDays = new AtomicInteger(0);
        final AtomicInteger failedDays = new AtomicInteger(0);
        

        for (int week = 1; week <= totalWeeks; week++) {

            for (int day = 1; day <= numberOfDaysPerWeek; day++) {
                final int weekNum = week;
                final int dayNum = day;
                

                int dayKey = generateDayKey(weekNum, dayNum);
                List<String> exerciseIds = selectedExerciseIdsPerDay.get(dayKey);
                

                if (exerciseIds == null || exerciseIds.isEmpty()) {
                    savedDays.incrementAndGet();
                    checkCompletion(savedDays, failedDays, totalDays, callback);
                    continue;
                }
                

                templateManager.addTemplateDayAsync(
                    templateId,
                    "День " + dayNum,
                    "",
                    dayNum,
                    weekNum,
                    "",
                    "",
                    60,
                    new ProgramTemplateManager.AsyncCallback<ProgramTemplateDay>() {
                        @Override
                        public void onSuccess(ProgramTemplateDay day) {

                            List<Exercise> exercises = getExercisesFromCache(exerciseIds);

                            if (exercises.isEmpty()) {

                                savedDays.incrementAndGet();
                                checkCompletion(savedDays, failedDays, totalDays, callback);
                                return;
                            }


                            final AtomicInteger savedExercises = new AtomicInteger(0);
                            final int totalExercises = exercises.size();


                            for (int i = 0; i < exercises.size(); i++) {
                                Exercise exercise = exercises.get(i);
                                final int orderIndex = i + 1;
                                

                                templateManager.addTemplateExerciseAsync(
                                    day.getId(),
                                    exercise.getId(),
                                    exercise.getName(),
                                    orderIndex,
                                    4,
                                    "8-12",
                                    "",
                                    "90",
                                    "",
                                    new ProgramTemplateManager.AsyncCallback<ProgramTemplateExercise>() {
                                        @Override
                                        public void onSuccess(ProgramTemplateExercise templateExercise) {

                                            if (savedExercises.incrementAndGet() == totalExercises) {

                                                savedDays.incrementAndGet();
                                                checkCompletion(savedDays, failedDays, totalDays, callback);
                                            }
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e("CreateProgramWeek", "Ошибка сохранения упражнения: " + e.getMessage(), e);

                                            if (savedExercises.incrementAndGet() == totalExercises) {
                                                savedDays.incrementAndGet();
                                                checkCompletion(savedDays, failedDays, totalDays, callback);
                                            }
                                        }
                                    }
                                );
                            }
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            Log.e("CreateProgramWeek", "Ошибка сохранения дня: " + e.getMessage(), e);
                            failedDays.incrementAndGet();
                            checkCompletion(savedDays, failedDays, totalDays, callback);
                        }
                    }
                );
            }
        }
    }


    private void checkCompletion(AtomicInteger savedDays, AtomicInteger failedDays, 
                                int totalDays, ProgramTemplateManager.AsyncCallback<Boolean> callback) {
        if (savedDays.get() + failedDays.get() >= totalDays) {
            if (failedDays.get() == 0) {
                callback.onSuccess(true);
            } else {
                callback.onFailure(new Exception("Не удалось сохранить " + failedDays.get() + " дней программы"));
            }
        }
    }


    private void showLoading(boolean show) {
        if (progressBarSaving != null) {
            progressBarSaving.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        

        if (dimOverlay != null) {
            dimOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        

        if (btnCreateProgram != null) {
            btnCreateProgram.setEnabled(!show);
        }
        if (btnNextWeek != null) {
            btnNextWeek.setEnabled(!show);
        }
        if (btnCopyWeek != null) {
            btnCopyWeek.setEnabled(!show);
        }
    }



} 