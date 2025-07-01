package com.martist.vitamove.ui.program.constructor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.activities.BaseActivity;
import com.martist.vitamove.adapters.MultiSelectExerciseAdapter;
import com.martist.vitamove.viewmodels.ExerciseListViewModel;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.ui.ExerciseDetailsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DayExerciseSelectionActivity extends BaseActivity {

    public static final String TAG = "DayExerciseSelection";
    public static final String EXTRA_WEEK_NUMBER = "com.martist.vitamove.EXTRA_WEEK_NUMBER";
    public static final String EXTRA_DAY_NUMBER = "com.martist.vitamove.EXTRA_DAY_NUMBER";

    public static final String EXTRA_DAY_TITLE = "com.martist.vitamove.EXTRA_DAY_TITLE";

    public static final String EXTRA_SELECTED_EXERCISES = "com.martist.vitamove.EXTRA_SELECTED_EXERCISES";

    public static final String EXTRA_IS_REPLACEMENT_MODE = "com.martist.vitamove.EXTRA_IS_REPLACEMENT_MODE";

    public static final String EXTRA_PREVIOUS_SELECTION = "com.martist.vitamove.EXTRA_PREVIOUS_SELECTION";

    private RecyclerView recyclerViewExercises;
    private MultiSelectExerciseAdapter adapter;
    private SearchView searchViewExercises;
    private MaterialButton buttonConfirmSelection;
    private MaterialToolbar toolbar;
    private ExerciseListViewModel exerciseListViewModel;

    private int weekNumber;
    private int dayNumber;
    private String dayTitle;
    private boolean isSingleSelectionMode = false;

    private ActivityResultLauncher<Intent> exerciseDetailsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_exercise_selection);


        weekNumber = getIntent().getIntExtra(EXTRA_WEEK_NUMBER, -1);
        dayNumber = getIntent().getIntExtra(EXTRA_DAY_NUMBER, -1);

        dayTitle = getIntent().getStringExtra(EXTRA_DAY_TITLE);
        if (dayTitle == null || dayTitle.isEmpty()) {
            dayTitle = "День " + dayNumber;
        }

        isSingleSelectionMode = getIntent().getBooleanExtra(EXTRA_IS_REPLACEMENT_MODE, false);

        if (weekNumber == -1 || dayNumber == -1) {
            Toast.makeText(this, "Ошибка: Неверные данные дня", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        registerExerciseDetailsLauncher();


        exerciseListViewModel = new ViewModelProvider(this).get(ExerciseListViewModel.class);


        toolbar = findViewById(R.id.toolbar);

        if (isSingleSelectionMode) {
             toolbar.setTitle("Выберите замену (" + dayTitle + ")");
        } else {
             toolbar.setTitle("Выберите упражнения (" + dayTitle + ")");
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());


        searchViewExercises = findViewById(R.id.searchViewExercises);
        recyclerViewExercises = findViewById(R.id.recyclerViewExercises);
        buttonConfirmSelection = findViewById(R.id.buttonConfirmSelection);


        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(this));
        

        adapter = new MultiSelectExerciseAdapter(this, isSingleSelectionMode, exercise -> {
            openExerciseDetails(exercise);
        });
        recyclerViewExercises.setAdapter(adapter);


        observeViewModel();


        ArrayList<String> previouslySelectedIds = getIntent().getStringArrayListExtra(EXTRA_PREVIOUS_SELECTION);
        if (previouslySelectedIds != null && !previouslySelectedIds.isEmpty()) {

            adapter.setPreviouslySelectedIds(previouslySelectedIds);
        }


        exerciseListViewModel.loadExercises();


        setupSearch();



        if (isSingleSelectionMode) {
            buttonConfirmSelection.setText("Заменить");
        }
        buttonConfirmSelection.setOnClickListener(v -> confirmSelection());
    }


    private void openExerciseDetails(Exercise exercise) {
        if (exercise == null || exercise.getId() == null) {
            Toast.makeText(this, "Ошибка: нет данных об упражнении", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, ExerciseDetailsActivity.class);
        intent.putExtra(ExerciseDetailsActivity.EXTRA_EXERCISE_ID, exercise.getId());

        intent.putExtra("show_add_to_program_button", true);
        intent.putExtra(EXTRA_WEEK_NUMBER, weekNumber);
        intent.putExtra(EXTRA_DAY_NUMBER, dayNumber);
        intent.putExtra(EXTRA_DAY_TITLE, dayTitle);
        intent.putExtra(EXTRA_IS_REPLACEMENT_MODE, isSingleSelectionMode);
        

        exerciseDetailsLauncher.launch(intent);
    }


    private void registerExerciseDetailsLauncher() {
        exerciseDetailsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                    Intent data = result.getData();
                    String exerciseId = data.getStringExtra("selected_exercise_id");
                    boolean isReplacement = data.getBooleanExtra(EXTRA_IS_REPLACEMENT_MODE, false);
                    
                    if (exerciseId != null && !exerciseId.isEmpty()) {

                        ArrayList<String> selectedIds = new ArrayList<>();
                        selectedIds.add(exerciseId);
                        
                        Intent resultIntent = new Intent();
                        resultIntent.putStringArrayListExtra(EXTRA_SELECTED_EXERCISES, selectedIds);
                        resultIntent.putExtra(EXTRA_WEEK_NUMBER, weekNumber);
                        resultIntent.putExtra(EXTRA_DAY_NUMBER, dayNumber);
                        resultIntent.putExtra(EXTRA_IS_REPLACEMENT_MODE, isReplacement);
                        setResult(Activity.RESULT_OK, resultIntent);
                        

                        
                        finish();
                    }
                }
            }
        );
    }

    private void observeViewModel() {

        exerciseListViewModel.getExercises().observe(this, exercises -> {



            performLocalSearch(searchViewExercises.getQuery().toString());
        });


        exerciseListViewModel.getIsLoading().observe(this, isLoading -> {



        });


        exerciseListViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e("DayExerciseSelection", "Ошибка загрузки: " + error);
                Toast.makeText(this, "Ошибка: " + error, Toast.LENGTH_LONG).show();

            }
        });
    }

    private void setupSearch() {
        searchViewExercises.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                performLocalSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                performLocalSearch(newText);
                return true;
            }
        });
    }


    private void performLocalSearch(String query) {

        List<Exercise> fullExerciseList = exerciseListViewModel.getExercises().getValue();
        if (fullExerciseList == null) {
            fullExerciseList = new ArrayList<>();
        }

        List<Exercise> filteredExercises;
        if (query == null || query.trim().isEmpty()) {

            filteredExercises = new ArrayList<>(fullExerciseList);

        } else {

            String lowerCaseQuery = query.toLowerCase().trim();

            filteredExercises = fullExerciseList.stream()
                .filter(exercise -> {
                    boolean nameMatch = exercise.getName() != null && exercise.getName().toLowerCase().contains(lowerCaseQuery);
                    boolean muscleGroupMatch = false;
                    List<String> muscleGroups = exercise.getMuscleGroupRussianNames();
                    if (muscleGroups != null) {
                        muscleGroupMatch = muscleGroups.stream()
                            .anyMatch(muscle -> muscle != null && muscle.toLowerCase().contains(lowerCaseQuery));
                    }

                    return nameMatch || muscleGroupMatch;
                })
                .collect(Collectors.toList());

        }


        adapter.submitList(filteredExercises);
    }


    private void confirmSelection() {

        List<String> selectedExerciseIds = adapter.getSelectedExerciseIds();

        if (selectedExerciseIds.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы одно упражнение", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();

        resultIntent.putStringArrayListExtra(EXTRA_SELECTED_EXERCISES, new ArrayList<>(selectedExerciseIds));
        resultIntent.putExtra(EXTRA_WEEK_NUMBER, weekNumber);
        resultIntent.putExtra(EXTRA_DAY_NUMBER, dayNumber);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 