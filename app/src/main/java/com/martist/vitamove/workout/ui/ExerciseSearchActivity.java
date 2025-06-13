package com.martist.vitamove.workout.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.activities.BaseActivity;
import com.martist.vitamove.viewmodels.ExerciseListViewModel;
import com.martist.vitamove.workout.adapters.ExerciseAdapter;
import com.martist.vitamove.workout.data.models.Exercise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExerciseSearchActivity extends BaseActivity {
    private static final String TAG = "ExerciseSearchActivity";
    private SearchView searchView;
    private RecyclerView exerciseList;
    private ExerciseAdapter adapter;
    private ExerciseListViewModel viewModel;
    private ProgressBar progressBar;
    private List<Exercise> allExercises = new ArrayList<>();
    private static final int REQUEST_CODE_EXERCISE_DETAILS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_search);
        

        viewModel = new ViewModelProvider(this).get(ExerciseListViewModel.class);

        setupToolbar();
        setupViews();
        setupObservers();
        

        viewModel.loadExercises();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Поиск упражнений");
    }

    private void setupViews() {
        searchView = findViewById(R.id.search_view);
        exerciseList = findViewById(R.id.exercise_list);
        progressBar = findViewById(R.id.progressBar);
        

        exerciseList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExerciseAdapter(this, new ExerciseAdapter.OnExerciseClickListener() {
            @Override
            public void onExerciseClick(Exercise exercise) {

                Intent intent = new Intent(ExerciseSearchActivity.this, ExerciseDetailsActivity.class);
                intent.putExtra(ExerciseDetailsActivity.EXTRA_EXERCISE_ID, exercise.getId());
                startActivityForResult(intent, REQUEST_CODE_EXERCISE_DETAILS);
            }
            
            @Override
            public void onAddExerciseClick(Exercise exercise) {

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_exercise", exercise);
                resultIntent.putExtra("action", "add");
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        exerciseList.setAdapter(adapter);


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
    
    private void setupObservers() {

        viewModel.getExercises().observe(this, exercises -> {
            allExercises = exercises;
            adapter.updateExercises(exercises);
            
        });
        

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLocalSearch(String query) {
        if (query == null || query.isEmpty()) {

            adapter.updateExercises(allExercises);
            return;
        }
        

        String queryLowerCase = query.toLowerCase();
        String[] queryWords = queryLowerCase.split("\\s+");
        

        List<Exercise> filteredExercises = allExercises.stream()
            .filter(exercise -> {

                for (String word : queryWords) {

                    if (!containsWordOrPrefix(exercise, word)) {
                        return false;
                    }
                }

                return true;
            })
            .collect(Collectors.toList());
        

        sortExercisesByRelevance(filteredExercises, queryLowerCase, queryWords);
        
        adapter.updateExercises(filteredExercises);
        
    }
    

    private boolean containsWordOrPrefix(Exercise exercise, String word) {

        if (exercise.getName() != null) {
            String nameLower = exercise.getName().toLowerCase();

            if (containsWordOrPrefix(nameLower, word)) {
                return true;
            }
        }
        

        if (exercise.getDescription() != null) {
            String descLower = exercise.getDescription().toLowerCase();
            if (containsWordOrPrefix(descLower, word)) {
                return true;
            }
        }
        

        if (exercise.getMuscleGroupRussianNames() != null) {
            for (String muscle : exercise.getMuscleGroupRussianNames()) {
                String muscleLower = muscle.toLowerCase();
                if (containsWordOrPrefix(muscleLower, word)) {
                    return true;
                }
            }
        }
        

        if (exercise.getSecondaryMuscles() != null) {
            for (String muscle : exercise.getSecondaryMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (containsWordOrPrefix(muscleLower, word)) {
                    return true;
                }
            }
        }
        

        if (exercise.getStabilizerMuscles() != null) {
            for (String muscle : exercise.getStabilizerMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (containsWordOrPrefix(muscleLower, word)) {
                    return true;
                }
            }
        }
        

        if (exercise.getInstructions() != null) {
            String instructionsLower = exercise.getInstructions().toLowerCase();
            if (containsWordOrPrefix(instructionsLower, word)) {
                return true;
            }
        }
        
        return false;
    }
    

    private boolean containsWordOrPrefix(String text, String wordOrPrefix) {

        final int MIN_PREFIX_LENGTH = 3;
        

        if (text.contains(wordOrPrefix)) {
            return true;
        }
        

        if (wordOrPrefix.length() < MIN_PREFIX_LENGTH) {
            return false;
        }
        

        String[] words = text.split("\\s+");
        
        for (String w : words) {

            if (w.startsWith(wordOrPrefix)) {
                return true;
            }
            

            if (wordOrPrefix.length() >= MIN_PREFIX_LENGTH && w.length() > wordOrPrefix.length()) {

                String wPrefix = w.substring(0, Math.min(w.length(), wordOrPrefix.length() + 3));
                

                int distance = calculateLevenshteinDistance(wPrefix, wordOrPrefix);
                

                int maxAllowedDistance = Math.max(1, wordOrPrefix.length() / 3);
                
                if (distance <= maxAllowedDistance) {
                    
                    return true;
                }
            }
            

            if (wordOrPrefix.length() >= MIN_PREFIX_LENGTH && w.length() >= MIN_PREFIX_LENGTH) {


                if (w.length() <= wordOrPrefix.length() * 1.5) {
                    int distance = calculateLevenshteinDistance(w, wordOrPrefix);

                    int maxAllowedDistance = Math.max(1, Math.min(wordOrPrefix.length() / 3, 3));
                    
                    if (distance <= maxAllowedDistance) {
                        
                        return true;
                    }
                }

                else {
                    String wStart = w.substring(0, Math.min(w.length(), wordOrPrefix.length() + 2));
                    int distance = calculateLevenshteinDistance(wStart, wordOrPrefix);
                    int maxAllowedDistance = Math.max(1, Math.min(wordOrPrefix.length() / 3, 2));
                    
                    if (distance <= maxAllowedDistance) {
                        
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    

    private int calculateLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        

        int[][] dp = new int[len1 + 1][len2 + 1];
        

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {

                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                

                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        

        return dp[len1][len2];
    }
    

    private void sortExercisesByRelevance(List<Exercise> exercises, String query, String[] queryWords) {
        Collections.sort(exercises, (a, b) -> {
            int scoreA = calculateRelevanceScore(a, query, queryWords);
            int scoreB = calculateRelevanceScore(b, query, queryWords);

            return Integer.compare(scoreB, scoreA);
        });
    }
    

    private int calculateRelevanceScore(Exercise exercise, String query, String[] queryWords) {
        int score = 0;
        

        if (exercise.getName() != null) {
            String name = exercise.getName().toLowerCase();
            

            if (name.equals(query)) {
                score += 100;
            }

            else if (name.startsWith(query)) {
                score += 50;
            }

            else if (name.contains(query)) {
                score += 30;
            }
            

            for (String word : queryWords) {
                if (name.contains(word)) {
                    score += 10;
                }
            }
            


            String[] nameWords = name.split("\\s+");
            int excessWords = Math.max(0, nameWords.length - queryWords.length);
            score -= excessWords * 3;
        }
        

        if (exercise.getDescription() != null) {
            String desc = exercise.getDescription().toLowerCase();
            if (desc.contains(query)) {
                score += 15;
            }
            for (String word : queryWords) {
                if (desc.contains(word)) {
                    score += 5;
                }
            }
        }
        


        boolean queryHasNegativeIncline = false;
        for (String word : queryWords) {
            if (word.contains("отрицат") || word.contains("наклон")) {
                queryHasNegativeIncline = true;
                break;
            }
        }
        
        if (!queryHasNegativeIncline) {

            if (exercise.getName() != null) {
                String name = exercise.getName().toLowerCase();
                if (name.contains("отрицательн") && name.contains("наклон")) {
                    score -= 40;
                }
            }
        }
        

        if (exercise.getMuscleGroupRussianNames() != null) {
            for (String muscle : exercise.getMuscleGroupRussianNames()) {
                String muscleLower = muscle.toLowerCase();
                if (muscleLower.equals(query)) {
                    score += 40;
                } else if (muscleLower.contains(query)) {
                    score += 20;
                }
                for (String word : queryWords) {
                    if (muscleLower.contains(word)) {
                        score += 8;
                    }
                }
            }
        }
        

        if (exercise.getSecondaryMuscles() != null) {
            for (String muscle : exercise.getSecondaryMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (muscleLower.contains(query)) {
                    score += 10;
                }
                for (String word : queryWords) {
                    if (muscleLower.contains(word)) {
                        score += 3;
                    }
                }
            }
        }
        

        if (exercise.getStabilizerMuscles() != null) {
            for (String muscle : exercise.getStabilizerMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (muscleLower.contains(query)) {
                    score += 5;
                }
                for (String word : queryWords) {
                    if (muscleLower.contains(word)) {
                        score += 2;
                    }
                }
            }
        }
        

        if (exercise.getInstructions() != null) {
            String instructions = exercise.getInstructions().toLowerCase();
            if (instructions.contains(query)) {
                score += 5;
            }
            for (String word : queryWords) {
                if (instructions.contains(word)) {
                    score += 1;
                }
            }
        }
        


        boolean queryHasHorizontal = false;
        for (String word : queryWords) {
            if (word.contains("горизонт")) {
                queryHasHorizontal = true;
                break;
            }
        }
        
        if (queryHasHorizontal && exercise.getName() != null) {
            String name = exercise.getName().toLowerCase();
            if (name.contains("горизонт")) {
                score += 35;
            } else if (name.contains("наклон")) {
                score -= 30;
            }
        }
        
        return score;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_EXERCISE_DETAILS && resultCode == RESULT_OK && data != null) {

            if (data.getBooleanExtra("exercise_added", false)) {
                String exerciseId = data.getStringExtra("exercise_id");
                
                

                Intent resultIntent = new Intent();
                resultIntent.putExtra("exercise_added_via_details", true);
                resultIntent.putExtra("exercise_id", exerciseId);
                setResult(RESULT_OK, resultIntent);
                

                finish();
            }
        }
    }
} 