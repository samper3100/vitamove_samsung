package com.martist.vitamove.workout.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.activities.BaseActivity;
import com.martist.vitamove.events.WorkoutStartedEvent;
import com.martist.vitamove.viewmodels.ExerciseViewModel;
import com.martist.vitamove.workout.adapters.CardioSetAdapter;
import com.martist.vitamove.workout.adapters.ExerciseSetAdapter;
import com.martist.vitamove.workout.adapters.RepsOnlySetAdapter;
import com.martist.vitamove.workout.data.managers.ExerciseManager;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.WorkoutExercise;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.utils.WorkoutSettingsManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExerciseSettingsActivity extends BaseActivity {
    private static final String TAG = "ExerciseSettingsActivity";
    private static final int DEFAULT_REPS = 12;
    private static final String PREFS_NAME = "ExerciseSettings";
    private static final String KEY_SETS_DATA = "sets_data_";
    private static final String KEY_IS_RESTING = "is_resting_";
    private static final String KEY_REST_TIME = "rest_time_";
    private static final String KEY_IS_SET_ACTIVE = "is_set_active_";
    private static final String KEY_ACTIVE_SET_START_TIME = "active_set_start_time_";
    private static final String KEY_ACTIVE_SET_DURATION = "active_set_duration_";

    private TextView exerciseNameText;
    private TextView restTimerText;
    private TextView exerciseDescriptionText;
    private RecyclerView setsList;
    private MaterialButton completeSetButton;
    private View restTimerContainer;
    private View exerciseDescriptionContainer;
    private MaterialButton skipRestButton;
    private ProgressBar progressBar;
    private ExerciseSetAdapter adapter;
    private CardioSetAdapter cardioAdapter;
    private RepsOnlySetAdapter repsOnlyAdapter;
    private boolean isCardioExercise = false;
    private boolean isWarmupStretching = false;

    private View cardioSummaryView;
    private TextView cardioTotalMinutesText;

    private Exercise exercise;
    private WorkoutExercise workoutExercise;
    private String workoutId;
    private CountDownTimer restTimer;
    private boolean isResting = false;
    private SupabaseWorkoutRepository workoutRepository;
    

    private final boolean isDescriptionExpanded = false;
    

    private View muscleGroupsContainer;
    private com.google.android.material.chip.ChipGroup muscleGroupsChipGroup;
    

    private ExerciseViewModel viewModel;


    private ExerciseManager exerciseManager;
    private Executor executor;
    private Handler mainHandler;

    private GestureDetectorCompat gestureDetector;
    private ConstraintLayout rootLayout;


    private TextView activeSetTimerText;
    private View activeSetTimerContainer;
    private MaterialButton startSetButton;
    private CountDownTimer activeSetTimer;
    private boolean isSetActive = false;
    private long activeSetStartTime = 0;
    private long activeSetDuration = 0;
    private ExerciseSet activeSet;
    private TextView warmupStretchingMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_settings);

        



        if (getIntent() != null) {
            exercise = getIntent().getParcelableExtra("exercise");
            workoutExercise = getIntent().getParcelableExtra("workout_exercise");
            workoutId = getIntent().getStringExtra("workout_id");

            
            if (exercise != null) {
                
                

                isCardioExercise = exercise.isCardioExercise() || exercise.isStaticExercise();
                

                isWarmupStretching = exercise.getExerciseType() != null &&
                    (exercise.getExerciseType().equalsIgnoreCase("разминка") ||
                     exercise.getExerciseType().equalsIgnoreCase("warm-up") ||
                     exercise.getExerciseType().equalsIgnoreCase("растяжка") ||
                     exercise.getExerciseType().equalsIgnoreCase("stretching"));
                
                
            }
            if (workoutExercise != null) {
                
            }

            if (exercise == null || workoutExercise == null || workoutId == null) {
                Log.e(TAG, "onCreate: exercise, workoutExercise или workoutId равны null");
                Toast.makeText(this, "Ошибка: данные упражнения не найдены", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            
        } else {
            Log.e(TAG, "onCreate: intent is null");
            finish();
            return;
        }

        try {

            exerciseManager = new ExerciseManager(this);
            executor = Executors.newSingleThreadExecutor();
            mainHandler = new Handler(Looper.getMainLooper());

            setupToolbar();
            initViews();
            

            removeAllCardioCirclesFromLayout();
            

            setupGestureDetector();
            

            if (progressBar == null) {
                Log.e(TAG, "onCreate: progressBar все еще null после initViews(), пытаемся инициализировать повторно");
                progressBar = findViewById(R.id.progress_bar);
                
                if (progressBar == null) {
                    Log.e(TAG, "onCreate: не удалось инициализировать progressBar после повторной попытки");
                }
            }
            
            setupViewModel();
            setupRecyclerView();
            setupClickListeners();
            updateUI();


            if (exercise != null && exercise.getExerciseType() != null && 
                (exercise.getExerciseType().equals("восстановительное") || 
                 exercise.getExerciseType().equals("реабилитационное") || 
                 exercise.getExerciseType().equals("функциональное") ||
                 exercise.getExerciseType().equals("с собственным весом"))) {
                
                List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
                if (sets == null || sets.isEmpty()) {
                    
                    

                    

                    new Handler().postDelayed(() -> {
                        updateUI();
                        if (repsOnlyAdapter != null) {
                            List<ExerciseSet> updatedSets = viewModel.getExerciseSets().getValue();
                            if (updatedSets != null) {
                                repsOnlyAdapter.updateSets(updatedSets);
                                
                            }
                        }
                    }, 500);
                }
            }


            if (exercise != null && exercise.getId() != null) {
                loadFullExerciseDetails(exercise.getId());
            } else {
                Log.e(TAG, "onCreate: Не удалось получить ID упражнения для загрузки деталей");
                Toast.makeText(this, "Ошибка: ID упражнения не найден", Toast.LENGTH_SHORT).show();
                finish();
            }
            

            if (isWarmupStretching) {
                
                

                if (workoutExercise != null && workoutExercise.getSetsCompleted() != null && !workoutExercise.getSetsCompleted().isEmpty()) {
                    for (ExerciseSet set : workoutExercise.getSetsCompleted()) {
                        if (set.isCompleted()) {
                            
                            break;
                        }
                    }
                }
                

                new Handler().postDelayed(() -> updateUI(), 100);
            }

            
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ошибка при инициализации: " + e.getMessage(), e);
            Toast.makeText(this, "Произошла ошибка при загрузке упражнения", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });
        

        if (getWindow() != null) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.background_color));
            

            int nightModeFlags = getResources().getConfiguration().uiMode & 
                                 android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            
            View decorView = getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {

                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {

                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            
            decorView.setSystemUiVisibility(flags);
        }
        

        ImageView infoButton = findViewById(R.id.info_button);
        infoButton.setOnClickListener(v -> {

            Intent intent = ExerciseDetailsActivity.newIntent(this, exercise.getId());

            intent.putExtra("hide_add_button", true);
            startActivity(intent);

            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });


        ImageView rutubeButton = findViewById(R.id.rutube_button);
        rutubeButton.setOnClickListener(v -> openRutubeSearch());
    }

    private void initViews() {
        

        exerciseNameText = findViewById(R.id.exercise_name);

        setsList = findViewById(R.id.sets_recycler);
        restTimerText = findViewById(R.id.rest_timer_text);
        completeSetButton = findViewById(R.id.complete_set_button);
        restTimerContainer = findViewById(R.id.rest_timer_container);
        skipRestButton = findViewById(R.id.skip_rest_button);
        progressBar = findViewById(R.id.progress_bar);
        exerciseDescriptionText = findViewById(R.id.exercise_description);
        exerciseDescriptionContainer = findViewById(R.id.exercise_description_container);
        

        warmupStretchingMessageText = findViewById(R.id.warmup_stretching_message);
        

        View cardioInfoCard = findViewById(R.id.cardio_info_card);
        if (cardioInfoCard != null) {

            cardioInfoCard.setVisibility(isCardioExercise ? View.VISIBLE : View.GONE);
        }
        



        

        activeSetTimerText = findViewById(R.id.active_set_timer_text);
        activeSetTimerContainer = findViewById(R.id.active_set_timer_container);
        startSetButton = findViewById(R.id.start_set_button);
        

        muscleGroupsContainer = findViewById(R.id.muscle_groups_container);
        muscleGroupsChipGroup = findViewById(R.id.muscle_groups_chip_group);
        
        if (muscleGroupsContainer == null) {
            Log.e(TAG, "initViews: muscleGroupsContainer is null");
        }
        
        if (muscleGroupsChipGroup == null) {
            Log.e(TAG, "initViews: muscleGroupsChipGroup is null");
        }
        

        View exerciseDescriptionHeader = findViewById(R.id.exercise_description_header);
        ImageView toggleDescriptionButton = findViewById(R.id.toggle_description_button);
        

        exerciseDescriptionHeader.setOnClickListener(v -> {
            Intent intent = ExerciseDetailsActivity.newIntent(this, exercise.getId());

            intent.putExtra("hide_add_button", true);
            startActivity(intent);

            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        if (exerciseNameText == null) Log.e(TAG, "exerciseNameText is null");
        if (setsList == null) Log.e(TAG, "setsList is null");
        if (restTimerText == null) Log.e(TAG, "restTimerText is null");
        if (completeSetButton == null) Log.e(TAG, "completeSetButton is null");
        if (restTimerContainer == null) Log.e(TAG, "restTimerContainer is null");
        if (skipRestButton == null) Log.e(TAG, "skipRestButton is null");
        if (progressBar == null) Log.e(TAG, "progressBar is null");
        if (exerciseDescriptionText == null) Log.e(TAG, "exerciseDescriptionText is null");
        if (exerciseDescriptionContainer == null) Log.e(TAG, "exerciseDescriptionContainer is null");
        if (warmupStretchingMessageText == null) Log.e(TAG, "warmupStretchingMessageText is null");


        rootLayout = findViewById(R.id.root_layout);
        if (rootLayout == null) {
            Log.e(TAG, "initViews: rootLayout is null");
        }
    }
    
    private void setupViewModel() {
        try {
            viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);
            

            viewModel.initialize(exercise, workoutExercise, workoutId);
            

            viewModel.getExerciseSets().observe(this, sets -> {
                if (isCardioExercise) {
                    if (cardioAdapter != null) {


                        if (sets != null && !sets.isEmpty()) {

                            List<ExerciseSet> uncompletedSets = new ArrayList<>();
                            for (ExerciseSet set : sets) {
                                if (!set.isCompleted()) {
                                    uncompletedSets.add(set);
                                }
                            }
                            

                            if (uncompletedSets.size() > 1) {
                                
                                

                                List<ExerciseSet> updatedSets = new ArrayList<>();
                                boolean foundFirstUncompleted = false;
                                List<String> idsToDelete = new ArrayList<>();
                                

                                for (ExerciseSet set : sets) {
                                    if (set.isCompleted()) {
                                        updatedSets.add(set);
                                    } else if (!foundFirstUncompleted) {

                                        updatedSets.add(set);
                                        foundFirstUncompleted = true;
                                    } else {

                                        if (set.getId() != null) {
                                            idsToDelete.add(set.getId());
                                        }
                                    }
                                }
                                

                                for (String id : idsToDelete) {
                                    viewModel.deleteSet(id);
                                }
                                

                                cardioAdapter.updateSets(updatedSets);
                                

                                updateCardioTotalTime();
                                return;
                            }
                        }
                        

                        cardioAdapter.updateSets(sets);
                        updateCardioTotalTime();
                    } else {
                        Log.e(TAG, "setupViewModel: cardioAdapter is null when updating sets");
                    }
                } else {
                    if (adapter != null) {
                        adapter.updateSets(sets);
                    } else {
                        Log.e(TAG, "setupViewModel: adapter is null when updating sets");
                    }
                }
                

                WorkoutExercise currentExercise = viewModel.getWorkoutExercise().getValue();
                if (currentExercise != null) {
                    workoutExercise = currentExercise;
                }
                

                updateUI(); 
            });
            

            viewModel.getIsLoading().observe(this, isLoading -> {
                if (progressBar != null) {
                    progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                } else {
                    Log.e(TAG, "progressBar is null when setting visibility");
                }
            });
            

            viewModel.getErrorMessage().observe(this, error -> {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
            });
            

            viewModel.getIsResting().observe(this, resting -> {
                if (this.isResting != resting) {
                    this.isResting = resting;
                    updateRestTimerUI();
                    

                    if (completeSetButton != null) {
                        completeSetButton.setEnabled(!resting);
                    } else {
                        Log.e(TAG, "completeSetButton is null when updating rest state");
                    }
                    

                    updateUI();
                }
            });
            

            viewModel.getRestTimeRemaining().observe(this, timeRemaining -> {
                if (timeRemaining > 0) {
                    updateRestTimer(timeRemaining);
                }
            });
            
            
        } catch (Exception e) {
            Log.e(TAG, "setupViewModel: ошибка при инициализации ViewModel: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка инициализации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        setsList.setLayoutManager(new LinearLayoutManager(this));

        
        

        if (isWarmupStretching) {
            setsList.setVisibility(View.GONE);
            
            return;
        }


        String exerciseType = exercise != null ? exercise.getExerciseType() : null;

        if (isCardioExercise) {

            
            

            addCardioSummaryView();
            
            cardioAdapter = new CardioSetAdapter();

            cardioAdapter.setOnSetClickListener((set, position, isCompleted) -> {
                
                set.setCompleted(isCompleted);
                viewModel.updateSet(set);
            });

            cardioAdapter.setOnDeleteClickListener((set, position) -> {
                
                deleteSet(set, position);
            });
            

            cardioAdapter.setOnDataChangeListener((set, position) -> {

                if (set != null && set.getId() != null && !set.getId().isEmpty()) {

                    viewModel.updateSet(set);

                    updateCardioTotalTime();
                } else {
                    

                }
            });

            setsList.setAdapter(cardioAdapter);
            

            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
            if (sets == null || sets.isEmpty()) {
                

                ExerciseSet newSet = new ExerciseSet(null, null, 0, false);
                newSet.setDurationSeconds(0);
                addNewSet(newSet);
            } else if (sets.size() > 1) {

                
                

                List<ExerciseSet> uncompletedSets = new ArrayList<>();
                for (ExerciseSet set : sets) {
                    if (!set.isCompleted()) {
                        uncompletedSets.add(set);
                    }
                }
                
                if (uncompletedSets.size() > 1) {
                    
                    

                    List<ExerciseSet> updatedSets = new ArrayList<>();
                    boolean foundFirstUncompleted = false;
                    

                    for (ExerciseSet set : sets) {
                        if (set.isCompleted()) {
                            updatedSets.add(set);
                        } else if (!foundFirstUncompleted) {

                            updatedSets.add(set);
                            foundFirstUncompleted = true;
                        } else {

                            if (set.getId() != null) {
                                viewModel.deleteSet(set.getId());
                            }
                        }
                    }
                    

                    viewModel.setSets(updatedSets);
                }
            }
            

            updateCardioTotalTime();
        } else if (exerciseType != null && 
                (exerciseType.equals("восстановительное") || 
                 exerciseType.equals("реабилитационное") || 
                 exerciseType.equals("функциональное") ||
                 exerciseType.equals("с собственным весом"))) {

            
            
            repsOnlyAdapter = new RepsOnlySetAdapter();

            repsOnlyAdapter.setOnSetClickListener((set, position, isCompleted) -> {
                
                set.setCompleted(isCompleted);
                viewModel.updateSet(set);
            });

            repsOnlyAdapter.setOnDeleteClickListener((set, position) -> {
                
                deleteSet(set, position);
            });
            

            repsOnlyAdapter.setOnDataChangeListener((set, position) -> {
                if (set != null && set.getId() != null && !set.getId().isEmpty()) {

                    viewModel.updateSet(set);
                } else {
                    
                }
            });
            
            setsList.setAdapter(repsOnlyAdapter);
            


            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
            if (sets == null || sets.isEmpty()) {

                
            } else {
                
            }
        } else {

            adapter = new ExerciseSetAdapter();

            adapter.setOnSetClickListener((set, position, isCompleted) -> {

                
                set.setCompleted(isCompleted);
                viewModel.updateSet(set);
            });

            adapter.setOnDeleteClickListener((set, position) -> {
                
                deleteSet(set, position);
            });
            

            adapter.setOnDataChangeListener((set, position) -> {

                if (set != null && set.getId() != null && !set.getId().isEmpty()) {

                    viewModel.updateSet(set);
                } else {
                    

                }
            });
            
            setsList.setAdapter(adapter);
        }
    }
    

    private void addCardioSummaryView() {

        if (cardioSummaryView != null) {

            removeCardioSummaryView();
        }
        

        ViewGroup cardioSummaryContainer = findViewById(R.id.cardio_summary_container);
        if (cardioSummaryContainer == null) {
            Log.e(TAG, "addCardioSummaryView: контейнер cardio_summary_container не найден");
            return;
        }
        

        cardioSummaryContainer.setVisibility(View.VISIBLE);
        

        cardioSummaryView = getLayoutInflater().inflate(R.layout.cardio_summary_view, cardioSummaryContainer, false);
        

        cardioTotalMinutesText = cardioSummaryView.findViewById(R.id.cardio_total_minutes);
        

        cardioSummaryContainer.removeAllViews();
        

        cardioSummaryContainer.addView(cardioSummaryView);
        
        

        updateCardioTotalTime();
    }
    

    private void removeCardioSummaryView() {

        if (cardioSummaryView != null) {
            ViewGroup parent = (ViewGroup) cardioSummaryView.getParent();
            if (parent != null) {
                parent.removeView(cardioSummaryView);
                
            }
            cardioSummaryView = null;
            cardioTotalMinutesText = null;
        }
        

        ViewGroup cardioSummaryContainer = findViewById(R.id.cardio_summary_container);
        if (cardioSummaryContainer != null) {
            cardioSummaryContainer.setVisibility(View.GONE);
        }
    }
    

    private void updateCardioTotalTime() {
        if (cardioTotalMinutesText == null || !isCardioExercise) {
            return;
        }
        
        int totalSeconds = 0;
        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
        
        if (sets != null && !sets.isEmpty()) {
            for (ExerciseSet set : sets) {
                if (set.getDurationSeconds() != null) {

                    totalSeconds += set.getDurationSeconds();
                }
            }
        }
        

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        

        cardioTotalMinutesText.setText(String.valueOf(minutes));
        

        TextView cardioSecondsLabel = null;
        if (cardioSummaryView != null) {
            cardioSecondsLabel = cardioSummaryView.findViewById(R.id.cardio_seconds_value);
            
            if (cardioSecondsLabel != null) {

                cardioSecondsLabel.setText(String.format(Locale.getDefault(), "%02d", seconds));
            }
        }
        
        
    }


    private void addNewSet(ExerciseSet set) {
        
        

        boolean isStaticExercise = exercise != null && exercise.getExerciseType() != null && 
            (exercise.getExerciseType().equalsIgnoreCase("Статическое") || 
             exercise.getExerciseType().equalsIgnoreCase("Static") ||
             exercise.getExerciseType().toLowerCase().contains("статич") ||
             exercise.getExerciseType().toLowerCase().contains("static") ||
             exercise.getExerciseType().toLowerCase().contains("удержание") ||
             exercise.getExerciseType().toLowerCase().contains("планка"));
        
        if (exercise != null && exercise.getId() != null) {

            List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
            
            

            if (isCardioExercise) {
                if (currentSets != null && !currentSets.isEmpty()) {

                    boolean hasUncompletedSets = false;
                    for (ExerciseSet existingSet : currentSets) {
                        if (!existingSet.isCompleted()) {
                            hasUncompletedSets = true;
                            break;
                        }
                    }
                    

                    if (hasUncompletedSets) {
                        
                        Toast.makeText(this, "Завершите текущий подход перед добавлением нового", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                

                set.setDurationSeconds(0);
                set.setReps(null);
                set.setWeight(null);
                
                

                int nextSetNumber = (currentSets != null && !currentSets.isEmpty()) ? 
                    currentSets.size() + 1 : 1;
                set.setSetNumber(nextSetNumber);
                

                viewModel.addNewSet(set);
                

                if (cardioAdapter != null && currentSets != null) {
                    List<ExerciseSet> updatedSets = new ArrayList<>(currentSets);
                    updatedSets.add(set);
                    cardioAdapter.updateSets(updatedSets);
                }
                
                
                

                updateCardioTotalTime();
                


                new Handler().postDelayed(() -> {
                    
                    updateUI();
                    

                    if (startSetButton != null) {
                        startSetButton.setVisibility(View.VISIBLE);

                        if (isStaticExercise) {
                            startSetButton.setText("НАЧАТЬ УДЕРЖАНИЕ");
                        } else {
                            startSetButton.setText("НАЧАТЬ КАРДИО");
                        }
                        startSetButton.invalidate();
                    }
                }, 300);
            } else {

                
                

                boolean hasUncompletedSets = false;
                if (currentSets != null && !currentSets.isEmpty()) {
                    for (ExerciseSet existingSet : currentSets) {
                        if (!existingSet.isCompleted()) {
                            hasUncompletedSets = true;
                            
                            break;
                        }
                    }
                    
                    if (hasUncompletedSets) {
                        
                        Toast.makeText(this, "Завершите текущий подход перед добавлением нового", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                

                int nextSetNumber = (currentSets != null && !currentSets.isEmpty()) ? 
                    currentSets.size() + 1 : 1;
                set.setSetNumber(nextSetNumber);
                

                
                viewModel.addNewSet(set);
                

                new Handler().postDelayed(() -> {
                    
                    updateUI();
                    

                    if (startSetButton != null) {
                        startSetButton.setVisibility(View.VISIBLE);
                        startSetButton.setText("НАЧАТЬ ПОДХОД");
                        startSetButton.invalidate();
                    }
                }, 300);
            }
        } else {
            Log.e(TAG, "addNewSet: exercise или exercise.getId() равен null");
            Toast.makeText(this, "Ошибка: не удалось добавить подход, информация об упражнении отсутствует", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSet(ExerciseSet set, int position) {

        if (set == null || set.getId() == null) {
            Log.e(TAG, "deleteSet: set или set.getId() равны null, удаление невозможно");
            Toast.makeText(this, "Ошибка: не удалось удалить подход (ID не найден)", Toast.LENGTH_SHORT).show();
            return;
        }
        

        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        if (currentSets != null && currentSets.size() <= 1) {
            
            Toast.makeText(this, "Нельзя удалить последний подход", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {

            String setIdToDelete = set.getId();
            

            
            viewModel.deleteSet(setIdToDelete);
            

            if (set.equals(activeSet)) {
                stopActiveSetTimer();
                activeSet = null;
                activeSetTimer = null;
            }
            

            if (isCardioExercise && cardioAdapter != null) {

                List<ExerciseSet> updatedSets = viewModel.getExerciseSets().getValue();
                if (updatedSets != null) {
                    cardioAdapter.updateSets(new ArrayList<>(updatedSets));
                    updateCardioTotalTime();
                }
            } else if (repsOnlyAdapter != null) {
                List<ExerciseSet> updatedSets = viewModel.getExerciseSets().getValue();
                if (updatedSets != null) {
                    repsOnlyAdapter.updateSets(new ArrayList<>(updatedSets));
                }
            } else if (adapter != null) {
                List<ExerciseSet> updatedSets = viewModel.getExerciseSets().getValue();
                if (updatedSets != null) {
                    adapter.updateSets(new ArrayList<>(updatedSets));
                }
            }
            

            updateUI();
            

            Toast.makeText(this, "Подход удален", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "deleteSet: Ошибка при удалении подхода: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка при удалении подхода: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        


        startSetButton.setOnClickListener(v -> startActiveSet());
        

        completeSetButton.setOnClickListener(v -> {
            if (isWarmupStretching) {

                
                finishExercise();
            } else {

                completeActiveSet();
            }
        });
        skipRestButton.setOnClickListener(v -> skipRest());
    }


    private void startActiveSet() {
        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        boolean allCompleted = false;
        if (isCardioExercise && currentSets != null && !currentSets.isEmpty()) {
            allCompleted = true;
            for (ExerciseSet set : currentSets) {
                if (!set.isCompleted()) {
                    allCompleted = false;
                    break;
                }
            }
        }

        ExerciseSet setToActivate = null;


        if (isCardioExercise && allCompleted) {
            

            ExerciseSet newSet = new ExerciseSet(null, null, 0, false);
            newSet.setDurationSeconds(0);
            newSet.setWorkoutExerciseId(workoutExercise.getId());

            int nextSetNumber = (currentSets != null && !currentSets.isEmpty()) ? currentSets.size() + 1 : 1;
            newSet.setSetNumber(nextSetNumber);


            viewModel.addNewSet(newSet);
            


            setToActivate = newSet; 
            

        } else {

            if (currentSets != null) {
                for (ExerciseSet set : currentSets) {
                    if (!set.isCompleted()) {
                        setToActivate = set;
                        
                        break;
                    }
                }
            }
        }


        if (setToActivate == null) {
             Log.e(TAG, "startActiveSet: Не удалось определить подход для активации.");
             Toast.makeText(this, "Не удалось начать подход.", Toast.LENGTH_SHORT).show();
             return;
        }


        activeSet = setToActivate;
        isSetActive = true;


        boolean isStaticExercise = exercise != null && exercise.getExerciseType() != null && 
            (exercise.getExerciseType().equalsIgnoreCase("Статическое") || 
             exercise.getExerciseType().equalsIgnoreCase("Static") ||
             exercise.getExerciseType().toLowerCase().contains("статич") ||
             exercise.getExerciseType().toLowerCase().contains("static") ||
             exercise.getExerciseType().toLowerCase().contains("удержание") ||
             exercise.getExerciseType().toLowerCase().contains("планка"));
        









        

        if (isCardioExercise) {

            int initialSeconds = 0;
            if (setToActivate.getDurationSeconds() != null) {
                initialSeconds = setToActivate.getDurationSeconds();
                
            }
            

            activeSetStartTime = System.currentTimeMillis() - (initialSeconds * 1000L);
            

            startSetButton.setVisibility(View.GONE);
            

            activeSetTimerContainer.setVisibility(View.GONE);
            completeSetButton.setVisibility(View.VISIBLE);
            

            if (cardioSummaryView != null) {
                cardioSummaryView.setVisibility(View.VISIBLE);
                updateCardioTotalTime(); 
            }
            
            if (isStaticExercise) {
                
            } else {
                
            }
        } else {

            activeSetStartTime = System.currentTimeMillis();
            startSetButton.setVisibility(View.GONE);
            activeSetTimerContainer.setVisibility(View.VISIBLE);
            completeSetButton.setVisibility(View.VISIBLE);
        }
        

        startActiveSetTimer();
    }
    

    private void startActiveSetTimer() {

        if (activeSetTimer != null) {
            activeSetTimer.cancel();
        }
        

        activeSetTimer = new CountDownTimer(3600000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedTime = System.currentTimeMillis() - activeSetStartTime;
                

                if (isCardioExercise) {
                    updateCardioActiveTimer(elapsedTime);
                } else {
                    updateActiveSetTimer(elapsedTime);
                }
            }
            
            @Override
            public void onFinish() {

            }
        }.start();
    }
    

    private void updateCardioActiveTimer(long elapsedTimeMillis) {

        int totalSeconds = (int)(elapsedTimeMillis / 1000);
        

        if (activeSet != null) {

            Integer previousValue = activeSet.getDurationSeconds();
            

            activeSet.setDurationSeconds(totalSeconds);
            

            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            

            updateCardioTotalTime();
            

            if (activeSetTimerText != null && activeSetTimerText.getVisibility() == View.VISIBLE) {
                String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                activeSetTimerText.setText(timeFormatted);
            }
            
            
        }
    }
    

    private void updateActiveSetTimer(long elapsedTimeMillis) {

        activeSetDuration = elapsedTimeMillis;
        

        if (activeSet != null && !activeSet.isCompleted()) {
            int seconds = (int) (elapsedTimeMillis / 1000);
            activeSet.setDurationSeconds(seconds);
            viewModel.updateSet(activeSet);
        }
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis) - 
                TimeUnit.MINUTES.toSeconds(minutes);
        

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        activeSetTimerText.setText(timeFormatted);
    }
    

    private void completeActiveSet() {
        if (activeSet == null) {

            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
            if (sets != null) {
                for (ExerciseSet set : sets) {
                    if (!set.isCompleted()) {
                        activeSet = set;
                        break;
                    }
                }
            } else {
                Log.e(TAG, "completeActiveSet: sets is null");
                return;
            }
            

            if (activeSet == null) {
                Log.e(TAG, "completeActiveSet: activeSet is null");
                Toast.makeText(this, "Ошибка: не найден активный подход", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        

        if (activeSetTimer != null) {
            activeSetTimer.cancel();
            activeSetTimer = null;
        }
        

        long elapsedTimeMillis = System.currentTimeMillis() - activeSetStartTime;
        

        if (isCardioExercise) {

            int totalSeconds = (int)(elapsedTimeMillis / 1000);
            

            activeSet.setDurationSeconds(totalSeconds);
            activeSet.setCompleted(true);
            

            viewModel.updateSet(activeSet);
            

            updateCardioTotalTime();
            
            
            

            resetActiveSet();
            

            boolean isStaticExercise = exercise != null && exercise.isStaticExercise();
            

            if (isStaticExercise) {
                Toast.makeText(this, "Удержание завершено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Подход кардио завершен", Toast.LENGTH_SHORT).show();
            }
        } else {

            activeSetDuration = elapsedTimeMillis;
            
            completeSet();
        }
        

        if (isCardioExercise) {

            new Handler().postDelayed(() -> {
                updateUI();
                

                List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
                boolean hasUncompletedSets = false;
                boolean allCompleted = true;
                
                if (sets != null && !sets.isEmpty()) {
                    for (ExerciseSet set : sets) {
                        if (!set.isCompleted()) {
                            hasUncompletedSets = true;
                            allCompleted = false;
                            break;
                        }
                    }
                    

                    if (allCompleted) {
                        

                        new Handler().postDelayed(() -> {
                            finishExercise();
                        }, 1000);
                        return;
                    }
                }
                

                if (hasUncompletedSets && startSetButton != null) {
                    startSetButton.setVisibility(View.VISIBLE);
                    startSetButton.setText("НАЧАТЬ КАРДИО");
                }
            }, 300);
        }
    }

    private void completeSet() {
        try {

            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
            
            if (sets == null || sets.isEmpty()) {
                
                return;
            }
                
            int position = -1;
            for (int i = 0; i < sets.size(); i++) {
                if (!sets.get(i).isCompleted()) {
                    position = i;
                    break;
                }
            }

            if (position != -1) {

                ExerciseSet originalSet = sets.get(position);
                

                if (originalSet.getId() == null || originalSet.getId().isEmpty()) {
                    Log.e(TAG, "completeSet: Невозможно завершить подход - ID не существует. Позиция: " + position);
                    Toast.makeText(this, "Ошибка: ID подхода не найден", Toast.LENGTH_SHORT).show();
                    return;
                }
                

                Integer duration = originalSet.getDurationSeconds();
                if (activeSetDuration > 0) {
                    duration = (int)(activeSetDuration / 1000);
                    
                } else if (duration == null || duration <= 0) {


                    int reps = originalSet.getReps() != null ? originalSet.getReps() : 12;
                    duration = reps * 3;
                    
                } else {
                    
                }
                

                if (isCardioExercise) {
                    if (duration == null || duration <= 0) {


                        if (originalSet.getDurationSeconds() != null && originalSet.getDurationSeconds() > 0) {
                            duration = originalSet.getDurationSeconds();
                            
                        } else {

                            int totalMinutes = 0;
                            try {
                                if (cardioTotalMinutesText != null) {
                                    totalMinutes = Integer.parseInt(cardioTotalMinutesText.getText().toString());
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "completeSet: ошибка парсинга времени кардио: " + e.getMessage());
                            }
                            
                            TextView cardioSecondsLabel = null;
                            int totalSeconds = 0;
                            if (cardioSummaryView != null) {
                                cardioSecondsLabel = cardioSummaryView.findViewById(R.id.cardio_seconds_value);
                                if (cardioSecondsLabel != null) {
                                    try {
                                        totalSeconds = Integer.parseInt(cardioSecondsLabel.getText().toString());
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "completeSet: ошибка парсинга секунд кардио: " + e.getMessage());
                                    }
                                }
                            }
                            
                            duration = totalMinutes * 60 + totalSeconds;
                            
                        }
                    }
                }
                
                ExerciseSet updatedSet = new ExerciseSet(
                    originalSet.getId(),
                    originalSet.getWeight(),
                    originalSet.getReps(), 
                    true,
                    originalSet.getWorkoutExerciseId(),
                    originalSet.getSetNumber()
                );
                

                updatedSet.setDurationSeconds(duration);
                
                
                

                if (updatedSet != null && updatedSet.getId() != null && !updatedSet.getId().isEmpty()) {

                    boolean isFirstCompletedSet = true;
                    for (ExerciseSet set : sets) {
                        if (set.isCompleted() && !set.getId().equals(updatedSet.getId())) {
                            isFirstCompletedSet = false;
                            break;
                        }
                    }
                    

                    if (isFirstCompletedSet) {
                        checkAndStartWorkoutIfNeeded();
                    }
                    

                    viewModel.updateSet(updatedSet);
                    

                    if (isCardioExercise) {
                        updateCardioTotalTime();
                    }
                } else {
                    Log.e(TAG, "completeSet: Невозможно обновить подход - null или отсутствует ID. Позиция: " + position);
                    Toast.makeText(this, "Ошибка: не удалось обновить данные подхода", Toast.LENGTH_SHORT).show();
                    return;
                }
                

                resetActiveSet();
                

                boolean hasMoreUncompleted = false;
                for (int i = position + 1; i < sets.size(); i++) {
                    if (!sets.get(i).isCompleted()) {
                        hasMoreUncompleted = true;
                        break;
                    }
                }
                

                if (isCardioExercise) {

                    boolean wasRatedBefore = viewModel.isExerciseRated(workoutExercise.getId());
                    
                    if (!wasRatedBefore) {
                        
                        

                        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
                        if (currentSets != null && !currentSets.isEmpty()) {
                            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));
                            

                            for (int i = 0; i < currentSets.size(); i++) {
                                ExerciseSet s = currentSets.get(i);
                                
                            }
                        }
                        
                        finishExercise();
                    } else {
                        
                        

                        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
                        if (currentSets != null && !currentSets.isEmpty()) {
                            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));
                            

                            for (int i = 0; i < currentSets.size(); i++) {
                                ExerciseSet s = currentSets.get(i);
                                
                            }
                        }
                        
                        Toast.makeText(this, "Кардио упражнение выполнено!", Toast.LENGTH_SHORT).show();
                        
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("completed_exercise", workoutExercise);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                } else {

                    if (hasMoreUncompleted) {

                        boolean restTimerEnabled = WorkoutSettingsManager.getInstance(this).isRestTimerEnabled();
                        
                        if (restTimerEnabled) {
                            startRestTimer();
                        } else {
                            

                            isResting = false;
                            updateRestTimerUI();
                            updateUI();
                        }
                    } else {

                        boolean wasRatedBefore = viewModel.isExerciseRated(workoutExercise.getId());
                        
                        if (!wasRatedBefore) {

                            
                            finishExercise();
                        } else {

                            
                            

                            List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
                            if (currentSets != null && !currentSets.isEmpty()) {

                                
                                workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));
                                

                                for (int i = 0; i < currentSets.size(); i++) {
                                    ExerciseSet set = currentSets.get(i);
                                    
                                }
                            }
                            
                            Toast.makeText(this, "Все подходы выполнены!", Toast.LENGTH_SHORT).show();
                            
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("completed_exercise", workoutExercise);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    }
                }
            } else {
                
            }
        } catch (Exception e) {
            Log.e(TAG, "completeSet: ошибка при завершении подхода: " + e.getMessage(), e);
            Toast.makeText(this, "Произошла ошибка при завершении подхода", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRestTimer() {
        try {

            if (restTimer != null) {
                restTimer.cancel();
                restTimer = null;
            }
            

            int restTimerSeconds = WorkoutSettingsManager.getInstance(this).getRestTimerSeconds();
            long restTimeMillis = restTimerSeconds * 1000L;
            

            viewModel.startRest(restTimeMillis);
            
            
        } catch (Exception e) {
            Log.e(TAG, "startRestTimer: ошибка при запуске таймера: " + e.getMessage(), e);

            isResting = false;
            updateRestTimerUI();
            updateUI();
        }
    }


    private void updateRestTimer(long millisUntilFinished) {
        try {
            if (restTimerText == null) {
                Log.e(TAG, "updateRestTimer: restTimerText is null");
                return;
            }
        

            int minutes = (int) (millisUntilFinished / 1000) / 60;
            int seconds = (int) (millisUntilFinished / 1000) % 60;
            

            String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            restTimerText.setText(timeFormatted);
            
            
        } catch (Exception e) {
            Log.e(TAG, "updateRestTimer: ошибка при обновлении времени: " + e.getMessage(), e);
        }
    }

    private void skipRest() {
        try {
            if (restTimer != null) {
                restTimer.cancel();
                restTimer = null;
            }
            

            viewModel.endRest();
            

            if (isResting) {
                isResting = false;
                updateRestTimerUI();
                updateUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "skipRest: ошибка при завершении отдыха: " + e.getMessage(), e);
            isResting = false;
            updateRestTimerUI();
            updateUI();
        }
    }


    private void updateRestTimerUI() {
        try {
            if (restTimerContainer == null || skipRestButton == null) {
                Log.e(TAG, "updateRestTimerUI: restTimerContainer или skipRestButton равны null");
                return;
            }
            

            restTimerContainer.setVisibility(isResting ? View.VISIBLE : View.GONE);
            skipRestButton.setVisibility(isResting ? View.VISIBLE : View.GONE);
            

            if (startSetButton != null) {
                startSetButton.setVisibility(isResting ? View.GONE : View.VISIBLE);
            }
            

            if (activeSetTimerContainer != null) {
                activeSetTimerContainer.setVisibility(isResting ? View.GONE : 
                    (isSetActive ? View.VISIBLE : View.GONE));
            }
            

            if (completeSetButton != null) {
                completeSetButton.setVisibility(isResting ? View.GONE : 
                    (isSetActive ? View.VISIBLE : View.GONE));
            }
            
            
        } catch (Exception e) {
            Log.e(TAG, "updateRestTimerUI: ошибка при обновлении UI таймера: " + e.getMessage(), e);
        }
    }

    private void updateUI() {
        if (exercise == null) return;
        


        exerciseNameText.setText(exercise.getName());
        

        setupMuscleGroupChips();
        

        String instructions = exercise.getInstructions();
        boolean hasInstructions = instructions != null && !instructions.isEmpty();
        if (hasInstructions) {
            exerciseDescriptionText.setText(instructions);
            exerciseDescriptionContainer.setVisibility(View.VISIBLE);

            exerciseDescriptionText.setVisibility(isDescriptionExpanded ? View.VISIBLE : View.GONE);
            findViewById(R.id.exercise_description_header).setClickable(true);
            findViewById(R.id.toggle_description_button).setVisibility(View.VISIBLE);
        } else {

            exerciseDescriptionContainer.setVisibility(View.GONE);
            findViewById(R.id.exercise_description_header).setClickable(false);
            findViewById(R.id.toggle_description_button).setVisibility(View.GONE);
        }



        if (isWarmupStretching) {
            
            setsList.setVisibility(View.GONE);
            if (warmupStretchingMessageText != null) {
                warmupStretchingMessageText.setVisibility(View.VISIBLE);
                warmupStretchingMessageText.setText("Сделайте столько, сколько считаете нужным");
            } else {
                Log.e(TAG, "updateUI: warmupStretchingMessageText is null!");
            }
            
            if (cardioSummaryView != null) {
                cardioSummaryView.setVisibility(View.GONE);
            }
            

            restTimerContainer.setVisibility(View.GONE);
            activeSetTimerContainer.setVisibility(View.GONE);
            startSetButton.setVisibility(View.GONE);
            

            boolean isExerciseCompleted = false;
            

            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();

            if (sets != null && !sets.isEmpty()) {
                for (ExerciseSet set : sets) {
                    if (set.isCompleted()) {
                        isExerciseCompleted = true;
                        break;
                    }
                }
            }
            

            if (workoutExercise != null && workoutExercise.isRated()) {
                isExerciseCompleted = true;
            }
            
            if (isExerciseCompleted) {

                completeSetButton.setText("ВЫПОЛНЕНО");
                completeSetButton.setClickable(false);
                completeSetButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_500)));
                completeSetButton.setIconResource(R.drawable.ic_check);
                completeSetButton.setIconTint(ColorStateList.valueOf(Color.WHITE));
                completeSetButton.setIconGravity(MaterialButton.ICON_GRAVITY_START);
                completeSetButton.setIconPadding(16);
                
            } else {

                completeSetButton.setText("ЗАВЕРШИТЬ УПРАЖНЕНИЕ");
                completeSetButton.setClickable(true);
                completeSetButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange_500)));
                completeSetButton.setIcon(null);
                
            }
            
            completeSetButton.setVisibility(View.VISIBLE);

            
            return;
        }
        

        if (warmupStretchingMessageText != null) {
            warmupStretchingMessageText.setVisibility(View.GONE);
        }



        setsList.setVisibility(View.VISIBLE);
        

        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
        if (sets != null) {
            
            if (isCardioExercise && cardioAdapter != null) {
                cardioAdapter.updateSets(sets);
                updateCardioTotalTime();
            } else if (repsOnlyAdapter != null) {
                repsOnlyAdapter.updateSets(sets);
            } else if (adapter != null) {
                adapter.updateSets(sets);
            }
        }
        

        if (isResting) {
            restTimerContainer.setVisibility(View.VISIBLE);
            completeSetButton.setVisibility(View.GONE);
            startSetButton.setVisibility(View.GONE);
            activeSetTimerContainer.setVisibility(View.GONE);
        } else if (isSetActive) {
            restTimerContainer.setVisibility(View.GONE);
            completeSetButton.setVisibility(View.VISIBLE);
            startSetButton.setVisibility(View.GONE);
            activeSetTimerContainer.setVisibility(View.VISIBLE);
        } else {

            restTimerContainer.setVisibility(View.GONE);
            completeSetButton.setVisibility(View.GONE);
            activeSetTimerContainer.setVisibility(View.GONE);
            

            boolean allSetsCompleted = true;
            if (sets != null && !sets.isEmpty()) {
                for (ExerciseSet set : sets) {
                    if (!set.isCompleted()) {
                        allSetsCompleted = false;
                        break;
                    }
                }
            }



            if (!isCardioExercise && allSetsCompleted) {
                startSetButton.setVisibility(View.GONE);
                
            } else {
                startSetButton.setVisibility(View.VISIBLE);
                
                

                if (isCardioExercise) {
                    boolean isStaticExercise = exercise.getExerciseType() != null && 
                        (exercise.getExerciseType().equalsIgnoreCase("Статическое") || 
                         exercise.getExerciseType().equalsIgnoreCase("Static") ||
                         exercise.getExerciseType().toLowerCase().contains("статич") ||
                         exercise.getExerciseType().toLowerCase().contains("static") ||
                         exercise.getExerciseType().toLowerCase().contains("удержание") ||
                         exercise.getExerciseType().toLowerCase().contains("планка"));
                    
                    if (isStaticExercise) {
                        startSetButton.setText("НАЧАТЬ УДЕРЖАНИЕ");
                    } else {
                        startSetButton.setText("НАЧАТЬ КАРДИО");
                    }
                } else {
                    startSetButton.setText("НАЧАТЬ ПОДХОД");
                }
            }
        }
    }

    private void finishExercise() {

        if (isWarmupStretching) {
            
            

            List<ExerciseSet> existingSets = viewModel.getExerciseSets().getValue();
            boolean hasCompletedSets = false;
            
            if (existingSets != null && !existingSets.isEmpty()) {

                for (ExerciseSet set : existingSets) {
                    if (set.isCompleted()) {
                        hasCompletedSets = true;
                        break;
                    }
                }
            }
            

            if (!hasCompletedSets) {
                ExerciseSet dummySet = new ExerciseSet();
                dummySet.setWorkoutExerciseId(workoutExercise.getId());
                dummySet.setCompleted(true);
                dummySet.setReps(1);
                dummySet.setSetNumber(1);
                

                viewModel.addNewSet(dummySet);
                

                if (workoutExercise.getSetsCompleted() == null) {
                    workoutExercise.setSetsCompleted(new ArrayList<>());
                }
                workoutExercise.getSetsCompleted().add(dummySet);
                
                
            } else {
                
            }
            

            workoutExercise.setRated(true);
            workoutExercise.setCompleted(true);
            

            completeSetButton.setClickable(false);
            completeSetButton.setText("ВЫПОЛНЕНО");
            completeSetButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_500)));
            

            completeSetButton.setIconResource(R.drawable.ic_check);
            completeSetButton.setIconTint(ColorStateList.valueOf(Color.WHITE));
            completeSetButton.setIconGravity(MaterialButton.ICON_GRAVITY_START);
            completeSetButton.setIconPadding(16);
            

            Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            pulse.setDuration(500);
            completeSetButton.startAnimation(pulse);
            

            new Handler().postDelayed(() -> {

                
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("completed_exercise", workoutExercise);
                setResult(RESULT_OK, resultIntent);
                finish();
            }, 1000);
            
            return;
        }

        

        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        if (currentSets != null && !currentSets.isEmpty()) {

            for (ExerciseSet set : currentSets) {
                if (!set.isCompleted()) {
                    

                    set.setCompleted(true);
                    

                    if (isCardioExercise && (set.getDurationSeconds() == null || set.getDurationSeconds() <= 0)) {

                        int totalMinutes = 0;
                        int totalSeconds = 0;
                        try {
                            if (cardioTotalMinutesText != null) {
                                totalMinutes = Integer.parseInt(cardioTotalMinutesText.getText().toString());
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "finishExercise: ошибка парсинга времени кардио: " + e.getMessage());
                        }
                        
                        TextView cardioSecondsLabel = null;
                        if (cardioSummaryView != null) {
                            cardioSecondsLabel = cardioSummaryView.findViewById(R.id.cardio_seconds_value);
                            if (cardioSecondsLabel != null) {
                                try {
                                    totalSeconds = Integer.parseInt(cardioSecondsLabel.getText().toString());
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "finishExercise: ошибка парсинга секунд кардио: " + e.getMessage());
                                }
                            }
                        }
                        
                        int duration = totalMinutes * 60 + totalSeconds;
                        if (duration > 0) {
                            set.setDurationSeconds(duration);
                            
                        } else {

                            set.setDurationSeconds(60);
                            
                        }
                    }
                    

                    viewModel.updateSet(set);
                }
            }
            

            currentSets = viewModel.getExerciseSets().getValue();
            

            
            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));
            

            for (int i = 0; i < currentSets.size(); i++) {
                ExerciseSet set = currentSets.get(i);
                
            }
        }
        

        boolean enableAutoNextExercise = WorkoutSettingsManager.getInstance(this).isAutoNextExerciseEnabled();
        
        







        

        Intent resultIntent = new Intent();
        

        
        if (workoutExercise.getSetsCompleted() != null) {
            for (int i = 0; i < workoutExercise.getSetsCompleted().size(); i++) {
                ExerciseSet set = workoutExercise.getSetsCompleted().get(i);
                
            }
        }
                
        resultIntent.putExtra("completed_exercise", workoutExercise);
        

        if (enableAutoNextExercise) {

            resultIntent.putExtra("auto_next_exercise", true);
            
        }
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {

        if (isWarmupStretching) {
            boolean hasCompletedSet = false;
            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
            
            if (sets != null && !sets.isEmpty()) {
                for (ExerciseSet set : sets) {
                    if (set.isCompleted()) {
                        hasCompletedSet = true;
                        break;
                    }
                }
            }
            

            if (hasCompletedSet) {
                
                workoutExercise.setRated(true);
                workoutExercise.setCompleted(true);
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("completed_exercise", workoutExercise);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }

        }
        

        if (isCardioExercise) {
            boolean autoCompleted = autoCompleteCardioSets();
            if (autoCompleted) {
                

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Log.e(TAG, "onBackPressed: ошибка задержки: " + e.getMessage());
                }
            }
        }
        

        boolean hasChanges = workoutExercise.getSetsCompleted().stream()
            .anyMatch(ExerciseSet::isCompleted);
        

        boolean allCompleted = workoutExercise.getSetsCompleted().stream()
            .allMatch(ExerciseSet::isCompleted) && !workoutExercise.getSetsCompleted().isEmpty();
        

        boolean wasRatedBefore = viewModel.isExerciseRated(workoutExercise.getId());
        

        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        if (currentSets != null && !currentSets.isEmpty()) {

            
            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));
            

            for (int i = 0; i < currentSets.size(); i++) {
                ExerciseSet set = currentSets.get(i);
                
            }
        }
        
        if (hasChanges) {







                if (wasRatedBefore) {
                    
                } else {
                    
                }
                
                Intent resultIntent = new Intent();
                

                
                if (workoutExercise.getSetsCompleted() != null) {
                    for (int i = 0; i < workoutExercise.getSetsCompleted().size(); i++) {
                        ExerciseSet set = workoutExercise.getSetsCompleted().get(i);
                        
                    }
                }
                
                resultIntent.putExtra("completed_exercise", workoutExercise);
                setResult(RESULT_OK, resultIntent);
                finish();

        } else {

            
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        

        if (activeSetTimer != null) {
            activeSetTimer.cancel();
        }
        

        if (restTimer != null) {
            restTimer.cancel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        


        if (adapter != null) {
            adapter.savePendingChangesIfAny();
            
        }

        if (cardioAdapter != null) {



        }
        if (repsOnlyAdapter != null) {


        }


        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            

            if (restTimer != null) {
                restTimer.cancel();
                restTimer = null;
            }
            
            if (activeSetTimer != null) {
                activeSetTimer.cancel();
                activeSetTimer = null;
            }
            

            editor.putBoolean(KEY_IS_SET_ACTIVE + workoutExercise.getId(), isSetActive);
            

            if (isSetActive) {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - activeSetStartTime;
                editor.putLong(KEY_ACTIVE_SET_DURATION + workoutExercise.getId(), elapsedTime);
                editor.putLong(KEY_ACTIVE_SET_START_TIME + workoutExercise.getId(), activeSetStartTime);
                
            }
            

            boolean isRestingNow = viewModel.getIsResting().getValue() != null && viewModel.getIsResting().getValue();
            editor.putBoolean(KEY_IS_RESTING + workoutExercise.getId(), isRestingNow);
            

            if (isRestingNow) {
                Long remainingTime = viewModel.getRestTimeRemaining().getValue();
                if (remainingTime != null && remainingTime > 0) {
                    editor.putLong(KEY_REST_TIME + workoutExercise.getId(), remainingTime);
                    
                }
            }
            
            editor.apply();
            
        } catch (Exception e) {
            Log.e(TAG, "onPause: ошибка при сохранении состояния: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        try {

            if (isWarmupStretching) {
                
                

                boolean wasCompleted = workoutExercise.isCompleted() || workoutExercise.isRated();
                List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
                
                if (sets != null && !sets.isEmpty()) {
                    for (ExerciseSet set : sets) {
                        if (set.isCompleted()) {
                            wasCompleted = true;
                            break;
                        }
                    }
                }
                

                if (wasCompleted) {
                    
                    

                    workoutExercise.setCompleted(true);
                    workoutExercise.setRated(true);
                    

                    new Handler().postDelayed(() -> {
                        updateUI();

                    }, 200);
                }
                

                updateUI();
                return;
            }
            

            updateUI();
            

            if (isCardioExercise) {
                
                

                List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
                boolean hasUncompletedSets = false;
                if (sets != null && !sets.isEmpty()) {
                    for (ExerciseSet set : sets) {
                        if (!set.isCompleted()) {
                            hasUncompletedSets = true;
                            break;
                        }
                    }
                }
                

                if (hasUncompletedSets && !isSetActive && !isResting) {
                    
                    

                    new Handler().postDelayed(() -> {
                        if (startSetButton != null) {
                            startSetButton.setVisibility(View.VISIBLE);

                            boolean isStaticExercise = exercise != null && exercise.getExerciseType() != null && 
                                (exercise.getExerciseType().equalsIgnoreCase("Статическое") || 
                                 exercise.getExerciseType().equalsIgnoreCase("Static") ||
                                 exercise.getExerciseType().toLowerCase().contains("статич") ||
                                 exercise.getExerciseType().toLowerCase().contains("static") ||
                                 exercise.getExerciseType().toLowerCase().contains("удержание") ||
                                 exercise.getExerciseType().toLowerCase().contains("планка"));
                            
                            if (isStaticExercise) {
                                startSetButton.setText("НАЧАТЬ УДЕРЖАНИЕ");
                                
                            } else {
                                startSetButton.setText("НАЧАТЬ КАРДИО");
                                
                            }
                            startSetButton.invalidate();
                        }
                    }, 300);
                }
                

                List<ExerciseSet> currentSets = workoutExercise.getSetsCompleted();
                
                if (currentSets != null && !currentSets.isEmpty()) {
                    
                    

                    List<ExerciseSet> viewModelSets = viewModel.getExerciseSets().getValue();
                    

                    boolean needsUpdate = viewModelSets == null || viewModelSets.isEmpty();
                    
                    if (!needsUpdate && viewModelSets.size() > 0 && currentSets.size() > 0) {

                        Integer vmDuration = viewModelSets.get(0).getDurationSeconds();
                        Integer savedDuration = currentSets.get(0).getDurationSeconds();
                        
                        if ((vmDuration == null && savedDuration != null) || 
                            (vmDuration != null && savedDuration == null) ||
                            (vmDuration != null && savedDuration != null && !vmDuration.equals(savedDuration))) {
                            needsUpdate = true;
                            
                        }
                    }
                    
                    if (needsUpdate) {

                        
                        viewModel.setSets(new ArrayList<>(currentSets));
                        

                        if (cardioAdapter != null) {
                            cardioAdapter.updateSets(currentSets);
                        }
                        

                        updateCardioTotalTime();
                    }
                } else {
                    
                }
            }
            

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            

            boolean wasResting = prefs.getBoolean(KEY_IS_RESTING + workoutExercise.getId(), false);
            if (wasResting) {
                long remainingTime = prefs.getLong(KEY_REST_TIME + workoutExercise.getId(), 0);
                

                if (remainingTime > 1000) {

                    viewModel.startRest(remainingTime);
                    
                } else {

                    viewModel.endRest();
                    
                }
            } else {

                boolean wasSetActive = prefs.getBoolean(KEY_IS_SET_ACTIVE + workoutExercise.getId(), false);
                
                if (wasSetActive) {

                    long savedDuration = prefs.getLong(KEY_ACTIVE_SET_DURATION + workoutExercise.getId(), 0);
                    long savedStartTime = prefs.getLong(KEY_ACTIVE_SET_START_TIME + workoutExercise.getId(), 0);
                    

                    if (savedDuration > 0) {
                        isSetActive = true;
                        activeSetStartTime = System.currentTimeMillis() - savedDuration;
                        

                        updateActiveSetTimer(savedDuration);
                        startActiveSetTimer();
                        
                        
                    }
                }
            }
            

            updateUI();
            
            
        } catch (Exception e) {
            Log.e(TAG, "onResume: ошибка при восстановлении состояния: " + e.getMessage(), e);

            resetState();
        }
    }
    

    private void resetState() {
        try {
            isResting = false;
            isSetActive = false;
            
            if (restTimer != null) {
                restTimer.cancel();
                restTimer = null;
            }
            
            if (activeSetTimer != null) {
                activeSetTimer.cancel();
                activeSetTimer = null;
            }
            

            updateRestTimerUI();
            updateUI();
            
            
        } catch (Exception e) {
            Log.e(TAG, "resetState: ошибка при сбросе состояния: " + e.getMessage(), e);
        }
    }


    private void setupMuscleGroupChips() {
        if (exercise == null) {
            Log.e(TAG, "setupMuscleGroupChips: exercise is null");
            return;
        }


        com.google.android.material.chip.ChipGroup chipGroup = muscleGroupsChipGroup;
        if (chipGroup == null) {
            chipGroup = findViewById(R.id.muscle_groups_chip_group);

            muscleGroupsChipGroup = chipGroup;
        }
        
        if (chipGroup == null) {
            Log.e(TAG, "setupMuscleGroupChips: chip group is null");
            return;
        }


        List<String> muscleGroupRussianNames = exercise.getMuscleGroupRussianNames();
        if (muscleGroupRussianNames == null || muscleGroupRussianNames.isEmpty()) {
            
            chipGroup.removeAllViews();
            

            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText("Группы мышц не указаны");
            chip.setTextColor(getResources().getColor(R.color.gray_500));
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeWidth(1);
            chip.setChipStrokeColorResource(R.color.gray_500);
            chipGroup.addView(chip);
            return;
        }

        
        

        chipGroup.removeAllViews();


        for (String muscleGroupRussianName : muscleGroupRussianNames) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(muscleGroupRussianName);
            

            chip.setTextColor(getResources().getColor(R.color.orange_500));
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeWidth(1);
            chip.setChipStrokeColorResource(R.color.orange_500);
            chip.setTextSize(14);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipMinHeight(36);
            

            chipGroup.addView(chip);
        }
        

        chipGroup.requestLayout();
    }


    private int getMuscleGroupColor(String muscleGroup) {
        switch (muscleGroup) {
            case "chest":
                return R.color.goal_strength;
            case "back":
                return R.color.goal_endurance;
            case "shoulders":
                return R.color.goal_flexibility;
            case "biceps":
                return R.color.goal_muscle;
            case "triceps":
                return R.color.orange_500;
            case "legs":
                return R.color.goal_weight_loss;
            case "abs":
                return R.color.teal_700;
            default:
                return R.color.gray_500;
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        gestureDetector.onTouchEvent(event);
        

        return super.dispatchTouchEvent(event);
    }
    

    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener());
        

        if (rootLayout != null) {
            rootLayout.setOnTouchListener((v, event) -> {

                gestureDetector.onTouchEvent(event);
                return false;
            });
        } else {
            Log.e(TAG, "setupGestureDetector: rootLayout is null");
        }
        

        if (setsList != null) {
            setsList.setOnTouchListener((v, event) -> {

                boolean handled = gestureDetector.onTouchEvent(event);

                return handled;
            });
        }
    }
    

    private void openExerciseDetails() {

        if (exercise != null) {

            Intent intent = ExerciseDetailsActivity.newIntent(this, exercise.getId());

            intent.putExtra("hide_add_button", true);
            

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            Log.e(TAG, "openExerciseDetails: exercise is null");
        }
    }
    

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                

                if (Math.abs(diffX) > Math.abs(diffY)) {

                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD && diffX < 0) {

                        openExerciseDetails();
                        return true;
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG, "onFling: Error processing gesture: " + exception.getMessage());
            }
            return false;
        }
    }


    private void loadFullExerciseDetails(String exerciseId) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, "loadFullExerciseDetails: progressBar is null перед показом");
        }

        executor.execute(() -> {
            try {
                

                Exercise fullExercise = exerciseManager.getExerciseById(exerciseId);
                
                mainHandler.post(() -> {
                    if (fullExercise != null) {
                        

                        this.exercise = fullExercise;

                        updateUI();
                    } else {
                        Log.e(TAG, "loadFullExerciseDetails: Упражнение с ID " + exerciseId + " не найдено ExerciseManager-ом");
                        Toast.makeText(this, R.string.error_exercise_not_found, Toast.LENGTH_SHORT).show();


                    }

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        Log.e(TAG, "loadFullExerciseDetails: progressBar is null перед скрытием");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "loadFullExerciseDetails: Ошибка при загрузке упражнения ID " + exerciseId + ": " + e.getMessage(), e);
                mainHandler.post(() -> {
                    Toast.makeText(this, getString(R.string.error_loading_exercise) + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }


                });
            }
        });
    }


    private void openRutubeSearch() {
        if (exercise == null || exercise.getName() == null || exercise.getName().isEmpty()) {
            Log.e(TAG, "openRutubeSearch: Название упражнения не найдено");
            Toast.makeText(this, "Не удалось получить название упражнения для поиска", Toast.LENGTH_SHORT).show();
            return;
        }

        String exerciseName = exercise.getName();

        String searchQuery = "техника выполнения упражнения " + exerciseName;
        String query = Uri.encode(searchQuery); 
        String rutubeAppPackage = "ru.rutube.app"; 
        String rutubeSearchUrl = "https://rutube.ru/search/?query=" + query;

        try {

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("rutube://search?query=" + query));
            intent.setPackage(rutubeAppPackage);
            startActivity(intent);
            
        } catch (ActivityNotFoundException e) {
            

            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(rutubeSearchUrl));

                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(browserIntent);
            } catch (ActivityNotFoundException browserException) {
                Log.e(TAG, "openRutubeSearch: Не найден браузер для открытия URL");
                Toast.makeText(this, "Не найден браузер для открытия ссылки", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void resetActiveSet() {
        isSetActive = false;
        activeSetStartTime = 0;
        activeSetDuration = 0;
        activeSet = null;
        
        if (activeSetTimer != null) {
            activeSetTimer.cancel();
            activeSetTimer = null;
        }
        

        startSetButton.setVisibility(View.VISIBLE);
        activeSetTimerContainer.setVisibility(View.GONE);
        completeSetButton.setVisibility(View.GONE);
        

        activeSetTimerText.setText("00:00");
        

        updateUI();
    }


    private void checkAndStartWorkoutIfNeeded() {
        try {

            SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
            boolean isWorkoutActive = prefs.getBoolean("is_workout_active", false);
            
            if (!isWorkoutActive) {
                
                

                long workoutStartTime = System.currentTimeMillis();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_workout_active", true);
                editor.putLong("workout_start_time", workoutStartTime);
                editor.apply();
                

                Toast.makeText(this, "Тренировка автоматически началась!", Toast.LENGTH_SHORT).show();
                

                EventBus.getDefault().post(new WorkoutStartedEvent(workoutStartTime));
            }
        } catch (Exception e) {
            Log.e(TAG, "checkAndStartWorkoutIfNeeded: Ошибка при проверке/запуске тренировки: " + e.getMessage(), e);
        }
    }


    public void deleteSet(ExerciseSet set) {

        deleteSet(set, -1);
    }


    private void stopActiveSetTimer() {
        if (activeSetTimer != null) {
            activeSetTimer.cancel();
            activeSetTimer = null;
        }
        

        isSetActive = false;
        

        if (activeSetTimerContainer != null) {
            activeSetTimerContainer.setVisibility(View.GONE);
        }
        
        if (completeSetButton != null) {
            completeSetButton.setVisibility(View.GONE);
        }
        

        if (startSetButton != null) {
            startSetButton.setVisibility(View.VISIBLE);
        }
    }


    private void removeAllCardioCirclesFromLayout() {

        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView instanceof ViewGroup) {

            findAndRemoveCircleViews((ViewGroup) rootView);
            
        }
    }
    

    private void findAndRemoveCircleViews(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            

            if (child instanceof ViewGroup) {
                findAndRemoveCircleViews((ViewGroup) child);
            }
        }
    }


    private boolean autoCompleteCardioSets() {
        if (!isCardioExercise) {
            return false;
        }
        
        boolean wasAutoCompleted = false;
        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
        
        if (sets != null && !sets.isEmpty()) {
            for (ExerciseSet set : sets) {

                if (!set.isCompleted() && set.getDurationSeconds() != null && set.getDurationSeconds() > 0) {

                    
                    

                    set.setCompleted(true);
                    

                    viewModel.updateSet(set);
                    wasAutoCompleted = true;
                }
            }
        }
        
        return wasAutoCompleted;
    }


}