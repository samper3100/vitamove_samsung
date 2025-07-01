package com.martist.vitamove.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.adapters.ExerciseAdapter;
import com.martist.vitamove.workout.data.models.Exercise;

import java.util.ArrayList;
import java.util.List;

public class ExerciseSelectionActivity extends BaseActivity {

    private ExerciseAdapter adapter;
    private EditText searchView;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_selection);

        setupViews();
        setupRecyclerView();
        loadExercises();
    }

    private void setupViews() {
        searchView = findViewById(R.id.searchExercise);
        recyclerView = findViewById(R.id.exerciseList);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            List<Exercise> selectedExercises = adapter.getSelectedExercises();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_exercises", new ArrayList<>(selectedExercises));
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setupRecyclerView() {
        adapter = new ExerciseAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadExercises() {



















        List<Exercise> exercises = new ArrayList<>();





        adapter.submitList(exercises);
    }
} 