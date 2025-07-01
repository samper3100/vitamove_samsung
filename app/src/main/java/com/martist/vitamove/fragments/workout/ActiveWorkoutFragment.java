package com.martist.vitamove.fragments.workout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.activities.LoginActivity;
import com.martist.vitamove.adapters.ActiveWorkoutAdapter;
import com.martist.vitamove.events.AddExerciseEvent;
import com.martist.vitamove.managers.CaloriesManager;
import com.martist.vitamove.viewmodels.ActiveWorkoutViewModel;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.UserWorkout;
import com.martist.vitamove.workout.data.models.WorkoutExercise;
import com.martist.vitamove.workout.ui.ExerciseSearchActivity;
import com.martist.vitamove.workout.ui.ExerciseSettingsActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ActiveWorkoutFragment extends Fragment 
    implements ActiveWorkoutAdapter.OnExerciseClickListener {
    
    private static final String TAG = "ActiveWorkoutFragment";
    private static final int REQUEST_CODE_SELECT_EXERCISE = 1;
    private static final int REQUEST_CODE_CONFIGURE_EXERCISE = 2;

    private RecyclerView exerciseList;
    private TextView totalSetsText;
    private TextView totalExercisesText;
    private ActiveWorkoutAdapter adapter;
    private ActiveWorkoutViewModel workoutViewModel;
    private String userId;
    private TextView workoutTimerText;
    private TextView workoutCaloriesText;
    private MaterialButton startWorkoutButton;
    private MaterialButton finishWorkoutButton;
    private CountDownTimer workoutTimer;
    private long workoutStartTime;
    private boolean isWorkoutActive = false;
    private static final String KEY_WORKOUT_START_TIME = "workout_start_time";
    private static final String KEY_IS_WORKOUT_ACTIVE = "is_workout_active";
    private ItemTouchHelper itemTouchHelper;
    private CaloriesManager caloriesManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        String accessToken = prefs.getString("accessToken", null);
        String refreshToken = prefs.getString("refreshToken", null);

        if (userId == null || accessToken == null || refreshToken == null) {
            Log.e(TAG, "Отсутствуют необходимые данные авторизации");
            Toast.makeText(requireContext(), "Ошибка авторизации. Пожалуйста, войдите снова", Toast.LENGTH_LONG).show();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }
        
        
        caloriesManager = CaloriesManager.getInstance(requireContext());


        
        
        workoutViewModel = new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())
                .create(ActiveWorkoutViewModel.class);

        
        
        if (userId != null) {

            workoutViewModel.loadOrCreateActiveWorkout(userId);
        } else {
            Log.e(TAG, "onCreate: userId is null, не могу загрузить тренировку");
            
        }
        
        
        
        
        
        
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_workout, container, false);
        
        initializeViews(view);
        setupClickListeners();
        setupObservers();
        setupItemTouchHelper();
        restoreWorkoutState();
        
        return view;
    }

    private void initializeViews(View view) {
        exerciseList = view.findViewById(R.id.exercise_list);
        totalSetsText = view.findViewById(R.id.total_sets);
        totalExercisesText = view.findViewById(R.id.total_exercises);
        startWorkoutButton = view.findViewById(R.id.start_workout_button);
        finishWorkoutButton = view.findViewById(R.id.finish_workout_button);
        workoutTimerText = view.findViewById(R.id.workout_timer);
        workoutCaloriesText = view.findViewById(R.id.workout_calories);
        
        
        exerciseList.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new ActiveWorkoutAdapter(new ArrayList<>(), this);
        exerciseList.setAdapter(adapter);
        
        
        restoreWorkoutState();
    }

    private void setupClickListeners() {
        startWorkoutButton.setOnClickListener(v -> startWorkout());
        finishWorkoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Завершить тренировку?")
                .setMessage("Вы уверены, что хотите завершить тренировку?")
                .setPositiveButton("Да", (dialog, which) -> finishWorkout())
                .setNegativeButton("Нет", null)
                .show();
        });
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 
                0) { 
                
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                  @NonNull RecyclerView.ViewHolder viewHolder, 
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                
                return adapter.moveExercise(fromPosition, toPosition);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                
            }
            
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, 
                                 @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                
                
            }
            
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    
                    if (viewHolder != null) {
                        viewHolder.itemView.setAlpha(0.9f);
                    }
                }
            }
        };
        
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(exerciseList);
    }

    private void startWorkout() {
        
        UserWorkout workout = workoutViewModel.getActiveWorkout().getValue();
        if (workout == null || workout.getEndTime() != null) {
            
            if (userId != null) {
                
                workoutViewModel.loadOrCreateActiveWorkout(userId);
            } else {
                Log.e(TAG, "startWorkout: userId is null, не могу создать новую тренировку");
                Toast.makeText(requireContext(), "Ошибка создания тренировки", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        isWorkoutActive = true;
        workoutStartTime = System.currentTimeMillis();
        
        
        if (workout != null && workout.getId() != null) {
            try {
                
                new Thread(() -> {
                    try {
                        workoutViewModel.updateWorkoutStartTime(workout.getId(), workoutStartTime);

                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обновлении времени начала тренировки: " + e.getMessage(), e);
                        
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(requireContext(), "Не удалось обновить время начала тренировки", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при запуске потока обновления времени: " + e.getMessage(), e);
            }
        }
        
        
        startWorkoutButton.setVisibility(View.GONE);
        finishWorkoutButton.setVisibility(View.VISIBLE);
        
        
        startWorkoutTimer();
        
        
        saveWorkoutState();
    }

    private void finishWorkout() {
        UserWorkout workout = workoutViewModel.getActiveWorkout().getValue();
        if (workout == null || workout.getId().startsWith("temp-")) {
            Toast.makeText(requireContext(), "Тренировка еще не сохранена", Toast.LENGTH_SHORT).show();
            return;
        }
        
        
        Integer currentCalories = workoutViewModel.getRealTimeCalories().getValue();
        if (currentCalories != null && currentCalories > 0) {
            
            caloriesManager.addCompletedWorkoutCalories(currentCalories);
            
            caloriesManager.resetActiveWorkoutCalories();
            
        }
        
        long endTime = System.currentTimeMillis();
        workoutViewModel.completeWorkout(endTime);
        
        
    }

    private void startWorkoutTimer() {
        
        workoutTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                
                long elapsedTime = System.currentTimeMillis() - workoutStartTime;
                
                String timeText = formatElapsedTime(elapsedTime);
                
                workoutTimerText.setText(timeText);
                
                finishWorkoutButton.setIcon(null); 
                finishWorkoutButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                finishWorkoutButton.setIconPadding(8);
                finishWorkoutButton.setText(timeText + " | Завершить");
                
                
                if (workoutViewModel != null) {
                    
                    workoutViewModel.calculateRealTimeCalories(userId);
                }
            }

            @Override
            public void onFinish() {
                
                start();
            }
        };
        workoutTimer.start();
    }

    private void saveWorkoutState() {
        SharedPreferences.Editor editor = requireActivity()
            .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE)
            .edit();
        editor.putLong(KEY_WORKOUT_START_TIME, workoutStartTime);
        editor.putBoolean(KEY_IS_WORKOUT_ACTIVE, isWorkoutActive);
        editor.apply();
    }

    private void restoreWorkoutState() {
        SharedPreferences prefs = requireActivity()
            .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        isWorkoutActive = prefs.getBoolean(KEY_IS_WORKOUT_ACTIVE, false);
        workoutStartTime = prefs.getLong(KEY_WORKOUT_START_TIME, 0);

        if (isWorkoutActive) {
            startWorkoutButton.setVisibility(View.GONE);
            finishWorkoutButton.setVisibility(View.VISIBLE);
            startWorkoutTimer();
        }
    }

    private void setupObservers() {
        if (workoutViewModel != null) {
            
            workoutViewModel.getActiveWorkout().observe(getViewLifecycleOwner(), workout -> {
                if (workout != null) {

                    adapter.updateExercises(workout.getExercises());
                    
                    updateWorkoutStats(workout);
                } else {

                    adapter.updateExercises(new ArrayList<>());
                }
            });
            
            
            workoutViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
                
            });
            
            
            workoutViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
                if (error != null && !error.isEmpty()) {
                    Log.e(TAG,error);
                }
            });
            
            
            workoutViewModel.getIsWorkoutCompleted().observe(getViewLifecycleOwner(), isCompleted -> {
                if (isCompleted) {
                    handleWorkoutCompletionUI();

                }
            });
            
            
            workoutViewModel.getRealTimeCalories().observe(getViewLifecycleOwner(), calories -> {
                
                if (workoutCaloriesText != null) {
                    workoutCaloriesText.setText(String.format(Locale.getDefault(), "%d кал", calories));
                }
                
                
                if (isWorkoutActive) {
                    caloriesManager.updateActiveWorkoutCalories(calories);
                }
            });
        }
    }

    private void handleWorkoutCompletionUI() {
        
        if (workoutTimer != null) {
            workoutTimer.cancel();
            workoutTimer = null;
        }

        isWorkoutActive = false;
        saveWorkoutState();
        
        
        startWorkoutButton.setVisibility(View.VISIBLE);
        finishWorkoutButton.setVisibility(View.GONE);
        
        
        totalExercisesText.setText("0");
        totalSetsText.setText("0");
        workoutCaloriesText.setText("0");
        workoutTimerText.setText("00:00:00");
        
        
        if (adapter != null) {
            adapter.updateExercises(new ArrayList<>());
        }
        
        
        if (caloriesManager != null) {
            caloriesManager.updateActiveWorkoutCalories(0);
        }
        
        
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (data == null) {

            return;
        }
        
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_EXERCISE) { 
                if (data.hasExtra("selected_exercise")) { 
                    Exercise selectedExercise = data.getParcelableExtra("selected_exercise");
                    if (selectedExercise != null && userId != null) {

                        
                        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
                        if (currentWorkout == null || currentWorkout.getEndTime() != null) {

                            workoutViewModel.loadOrCreateActiveWorkout(userId);
                            
                        }
                        workoutViewModel.addExercise(selectedExercise.getId(), userId);
                    } else {

                    }
                } else if (data.getBooleanExtra("exercise_added_via_details", false)) { 
                    String exerciseId = data.getStringExtra("exercise_id");
                    if (exerciseId != null && userId != null) {

                        
                        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
                        if (currentWorkout == null || currentWorkout.getEndTime() != null) {

                            workoutViewModel.loadOrCreateActiveWorkout(userId);
                            
                        }
                        workoutViewModel.addExercise(exerciseId, userId);
                    } else {

                    }
                } else {

                }
            } else if (requestCode == REQUEST_CODE_CONFIGURE_EXERCISE) { 

                
                WorkoutExercise completedExercise = data.getParcelableExtra("completed_exercise");
                if (completedExercise != null) {
                    String exerciseName = completedExercise.getExercise() != null ? 
                        completedExercise.getExercise().getName() : "Неизвестное упражнение";
                    

                    
                    if (completedExercise.getSetsCompleted() == null || completedExercise.getSetsCompleted().isEmpty()) {

                    } else {
                        for (ExerciseSet set : completedExercise.getSetsCompleted()) {
                            String durationInfo = "";
                            if (set.getDurationSeconds() != null && set.getDurationSeconds() > 0) {
                                int minutes = set.getDurationSeconds() / 60;
                                int seconds = set.getDurationSeconds() % 60;
                                durationInfo = ", длительность: " + minutes + " мин. " + seconds + " сек.";
                            }


                        }
                    }
                          
                    workoutViewModel.updateWorkoutExerciseSets(
                        completedExercise.getId(), 
                        completedExercise.getSetsCompleted()
                    );


                    if (userId != null) {

                        workoutViewModel.calculateRealTimeCalories(userId);
                    }
                    
                    workoutViewModel.saveCurrentWorkoutStateToDb();
                    
                    UserWorkout currentWorkoutState = workoutViewModel.getActiveWorkout().getValue();
                    if (currentWorkoutState != null && currentWorkoutState.getExercises() != null) {

                        adapter.updateExercises(currentWorkoutState.getExercises());
                        
                        
                        boolean autoNextExercise = data.getBooleanExtra("auto_next_exercise", false);
                        if (autoNextExercise) {

                            
                            
                            int currentIndex;
                            List<WorkoutExercise> exercises = currentWorkoutState.getExercises();

                            currentIndex = IntStream.range(0, exercises.size()).filter(i -> exercises.get(i).getId().equals(completedExercise.getId())).findFirst().orElse(-1);
                            
                            
                            if (currentIndex >= 0 && currentIndex < exercises.size() - 1) {
                                
                                WorkoutExercise nextExercise = exercises.get(currentIndex + 1);


                                
                                new Handler().postDelayed(() -> {
                                    onExerciseClick(nextExercise, currentIndex + 1);
                                }, 300);
                            } else {

                                Toast.makeText(requireContext(), "Вы завершили все упражнения!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    
                    Boolean workoutOverallCompleted = workoutViewModel.getIsWorkoutCompleted().getValue();
                    if (workoutOverallCompleted != null && workoutOverallCompleted) {

                    } else {

                    }

                } else {

                }
            }
        } else if (resultCode != Activity.RESULT_CANCELED) { 

        }
    }

    @Override
    public void onExerciseClick(WorkoutExercise exercise, int position) {

        
        
        Intent intent = new Intent(getActivity(), ExerciseSettingsActivity.class);
        intent.putExtra("exercise", exercise.getExercise());
        intent.putExtra("workout_exercise", exercise);
        intent.putExtra("workout_id", workoutViewModel.getActiveWorkoutId().getValue());
        startActivityForResult(intent, REQUEST_CODE_CONFIGURE_EXERCISE);
    }

    @Override
    public void onDeleteExercise(WorkoutExercise exercise, int position) {

        
        new AlertDialog.Builder(requireContext())
            .setTitle("Удалить упражнение?")
            .setMessage("Вы уверены, что хотите удалить " + exercise.getExercise().getName() + "?")
            .setPositiveButton("Да", (dialog, which) -> {
                workoutViewModel.removeExercise(exercise.getId());
            })
            .setNegativeButton("Нет", null)
            .show();
    }

    @Override
    public void onExerciseOrderChanged(List<WorkoutExercise> exercises) {
        if (workoutViewModel != null) {
            workoutViewModel.updateExerciseOrder(exercises);
        }
    }

    @Override
    public void onAddExerciseClick() {
        Intent intent = new Intent(getActivity(), ExerciseSearchActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_EXERCISE);
    }

    private void updateWorkoutStats(UserWorkout workout) {
        if (workout != null && workout.getExercises() != null) {
            int totalExercises = workout.getExercises().size();
            int totalSets = 0;
            
            
            for (WorkoutExercise workoutExercise : workout.getExercises()) {
                Exercise exercise = workoutExercise.getExercise();
                if (exercise == null) continue;

                String exerciseType = exercise.getExerciseType() != null ? exercise.getExerciseType().toLowerCase() : "";
                boolean isCardio = exerciseType.contains("кардио") || exerciseType.contains("cardio") ||
                                   exerciseType.contains("статич") || exerciseType.contains("static") ||
                                   exerciseType.contains("удержание") || exerciseType.contains("планка");

                if (isCardio) {
                    totalSets += 1; 
                } else {
                    int targetSets = exercise.getDefaultSets();
                    if (workoutExercise.getSetsCompleted() != null && !workoutExercise.getSetsCompleted().isEmpty()) {
                        totalSets += Math.max(targetSets, workoutExercise.getSetsCompleted().size());
                    } else {
                        totalSets += (targetSets > 0 ? targetSets : 3); 
                    }
                }
            }
            
            
            totalExercisesText.setText(String.valueOf(totalExercises));
            totalSetsText.setText(String.valueOf(totalSets));
            
            
            if (isWorkoutActive && workout.getTotalCalories() > 0) {
                workoutCaloriesText.setText(String.valueOf(workout.getTotalCalories()));
            }
        } else {
            
            totalExercisesText.setText("0");
            totalSetsText.setText("0");
            workoutCaloriesText.setText("0");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        saveWorkoutState();
        
        
        if (isWorkoutActive && userId != null) {
            workoutViewModel.calculateRealTimeCalories(userId);
            
            
            Integer currentCalories = workoutViewModel.getRealTimeCalories().getValue();
            if (currentCalories != null && currentCalories > 0) {
                caloriesManager.updateActiveWorkoutCalories(currentCalories);

            }
        }
        

    }

    @Override
    public void onStart() {
        super.onStart();
        
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        
        
        if (isWorkoutActive && userId != null) {
            workoutViewModel.calculateRealTimeCalories(userId);
            
            
            Integer currentCalories = workoutViewModel.getRealTimeCalories().getValue();
            if (currentCalories != null && currentCalories > 0) {
                caloriesManager.updateActiveWorkoutCalories(currentCalories);

            }
        }
        
        
        if (workoutViewModel != null) {
            workoutViewModel.saveCurrentWorkoutStateToDb();
        }
        
        
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);

        }
        

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (workoutTimer != null) {
            workoutTimer.cancel();
            workoutTimer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        
        
        
        
        
    }

    @Override
    public void onResume() {
        super.onResume();

        
        
        if (isWorkoutActive && userId != null) {
            workoutViewModel.calculateRealTimeCalories(userId);
        }
    }

    
    private String formatElapsedTime(long elapsedTime) {
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddExerciseEvent(AddExerciseEvent event) {
        if (event == null || event.exerciseId == null) {
            Log.e(TAG, "Получено некорректное AddExerciseEvent или отсутствует exerciseId");
            return;
        }



        if (userId == null) {
            Log.e(TAG, "userId is null, не могу добавить упражнение. Пользователь может быть не авторизован.");
            Toast.makeText(getContext(), "Ошибка: пользователь не авторизован.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (workoutViewModel == null) {
            Log.e(TAG, "workoutViewModel is null, не могу добавить упражнение.");
            
            return;
        }

        
        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
        if (currentWorkout == null || currentWorkout.getEndTime() != null) {
            

            workoutViewModel.loadOrCreateActiveWorkout(userId);

        }


        workoutViewModel.addExercise(event.exerciseId, userId);
    }
} 