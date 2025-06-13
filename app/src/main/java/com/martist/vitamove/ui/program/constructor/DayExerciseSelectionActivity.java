package com.martist.vitamove.ui.program.constructor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DayExerciseSelectionActivity extends BaseActivity {

    public static final String EXTRA_WEEK_NUMBER = "com.martist.vitamove.EXTRA_WEEK_NUMBER";
    public static final String EXTRA_DAY_NUMBER = "com.martist.vitamove.EXTRA_DAY_NUMBER";

    public static final String EXTRA_SELECTED_EXERCISES = "com.martist.vitamove.EXTRA_SELECTED_EXERCISES";

    public static final String EXTRA_IS_REPLACEMENT_MODE = "com.martist.vitamove.EXTRA_IS_REPLACEMENT_MODE";

    private RecyclerView recyclerViewExercises;
    private MultiSelectExerciseAdapter adapter;
    private SearchView searchViewExercises;
    private MaterialButton buttonConfirmSelection;
    private MaterialToolbar toolbar;
    private ExerciseListViewModel exerciseListViewModel;

    private int weekNumber;
    private int dayNumber;
    private boolean isSingleSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_exercise_selection);


        weekNumber = getIntent().getIntExtra(EXTRA_WEEK_NUMBER, -1);
        dayNumber = getIntent().getIntExtra(EXTRA_DAY_NUMBER, -1);

        isSingleSelectionMode = getIntent().getBooleanExtra(EXTRA_IS_REPLACEMENT_MODE, false);

        if (weekNumber == -1 || dayNumber == -1) {
            Toast.makeText(this, "Ошибка: Неверные данные дня", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        exerciseListViewModel = new ViewModelProvider(this).get(ExerciseListViewModel.class);


        toolbar = findViewById(R.id.toolbar);

        if (isSingleSelectionMode) {
             toolbar.setTitle("Выберите замену (День " + dayNumber + ")");
        } else {
             toolbar.setTitle("Выберите упражнения (День " + dayNumber + ")");
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

        adapter = new MultiSelectExerciseAdapter(this, isSingleSelectionMode);
        recyclerViewExercises.setAdapter(adapter);


        observeViewModel();


        exerciseListViewModel.loadExercises();


        setupSearch();



        if (isSingleSelectionMode) {
            buttonConfirmSelection.setText("Заменить");
        }
        buttonConfirmSelection.setOnClickListener(v -> confirmSelection());
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

    private static final String TAG = "DayExerciseSelection";


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