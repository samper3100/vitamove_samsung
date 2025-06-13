package com.martist.vitamove.workout.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.activities.BaseActivity;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.adapters.ExerciseAdapter;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.util.List;

public class ExerciseListActivity extends BaseActivity {
    private static final String TAG = "ExerciseListActivity";
    private RecyclerView exerciseList;
    private ExerciseAdapter adapter;
    private WorkoutRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_list);

        
        repository = new SupabaseWorkoutRepository(SupabaseClient.getInstance(
            Constants.SUPABASE_CLIENT_ID,
            Constants.SUPABASE_CLIENT_SECRET
        ));

        
        exerciseList = findViewById(R.id.exercise_list);
        exerciseList.setLayoutManager(new LinearLayoutManager(this));
        
        
        adapter = new ExerciseAdapter(this, new ExerciseAdapter.OnExerciseClickListener() {
            @Override
            public void onExerciseClick(Exercise exercise) {
                
                Intent intent = new Intent(ExerciseListActivity.this, ExerciseDetailsActivity.class);
                intent.putExtra(ExerciseDetailsActivity.EXTRA_EXERCISE_ID, exercise.getId());
                startActivity(intent);
            }
            
            @Override
            public void onAddExerciseClick(Exercise exercise) {
                
                Toast.makeText(ExerciseListActivity.this, "Добавлено упражнение: " + exercise.getName(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        exerciseList.setAdapter(adapter);

        
        loadExercises();
    }

    private void loadExercises() {
        new Thread(() -> {
            try {
                List<Exercise> exercises = repository.getAllExercises();
                runOnUiThread(() -> {
                    adapter.updateExercises(exercises);
                    
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading exercises: " + e.getMessage());
                runOnUiThread(() -> 
                    Toast.makeText(this, "Ошибка загрузки упражнений", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
} 