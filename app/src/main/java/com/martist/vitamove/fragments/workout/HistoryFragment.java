package com.martist.vitamove.fragments.workout;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.adapters.WorkoutDetailsAdapter;
import com.martist.vitamove.adapters.WorkoutHistoryAdapter;
import com.martist.vitamove.utils.OtherMonthDecorator;
import com.martist.vitamove.utils.WorkoutDayDecorator;
import com.martist.vitamove.viewmodels.HistoryViewModel;
import com.martist.vitamove.workout.data.models.UserWorkout;
import com.martist.vitamove.workout.data.models.WorkoutPlan;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;

import org.threeten.bp.DayOfWeek;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class HistoryFragment extends Fragment implements WorkoutHistoryAdapter.OnWorkoutClickListener {
    private static final String TAG = "HistoryFragment";
    private MaterialCalendarView calendarView;
    private TextView totalWorkoutsText;
    private TextView totalExercisesText;
    private TextView avgDurationText;
    private TextView favoriteExerciseText;
    private RecyclerView recentWorkoutsList;
    private View contentScroll;
    private View progressBar;
    private WorkoutHistoryAdapter adapter;
    private WorkoutRepository repository;
    private Map<Long, UserWorkout> workoutsByDate;
    private Map<Long, WorkoutPlan> plansByDate;
    private WorkoutDayDecorator workoutDecorator;
    private final Set<CalendarDay> workoutDays = new HashSet<>();
    private HistoryViewModel viewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    

    private final Executor executor = Executors.newCachedThreadPool();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        

        repository = ((VitaMoveApplication) requireActivity().getApplication()).getWorkoutRepository();
        

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        

        workoutsByDate = new HashMap<>();
        plansByDate = new HashMap<>();


        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        showLoading(true);


        view.post(() -> {
            executor.execute(() -> {
                setupViewModel();

                if (isAdded()) {
                    try {
                        requireActivity().runOnUiThread(this::setupCalendar);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Ошибка при вызове setupCalendar: " + e.getMessage());
                    }
                }
                updateDataFromViewModel();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        viewModel.initializeLiveDataObservers();
                        SharedPreferences prefs = requireActivity().getSharedPreferences(
                            "history_fragment_state", Context.MODE_PRIVATE
                        );
                        prefs.edit().putLong("last_refresh_time", System.currentTimeMillis()).apply();
                    });
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        

        updateDataFromViewModel();
        

        executor.execute(() -> {
            SharedPreferences prefs = requireActivity().getSharedPreferences(
                "history_fragment_state", 
                Context.MODE_PRIVATE
            );
            
            long lastRefreshTime = prefs.getLong("last_refresh_time", 0);
            long currentTime = System.currentTimeMillis();
            

            if (lastRefreshTime == 0 || currentTime - lastRefreshTime > 30 * 60 * 1000) {
                
                

                if (isAdded()) {

                    requireActivity().runOnUiThread(() -> {
                        viewModel.initializeLiveDataObservers();
                        

                        prefs.edit().putLong("last_refresh_time", currentTime).apply();
                    });
                }
            }
        });
    }

    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        totalWorkoutsText = view.findViewById(R.id.total_workouts);
        totalExercisesText = view.findViewById(R.id.total_exercises);
        avgDurationText = view.findViewById(R.id.avg_duration);
        favoriteExerciseText = view.findViewById(R.id.favorite_exercise);
        recentWorkoutsList = view.findViewById(R.id.recent_workouts_list);
        contentScroll = view.findViewById(R.id.content_scroll);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        

        recentWorkoutsList.setHasFixedSize(true);
    }
    
    private void setupSwipeRefresh() {

        swipeRefreshLayout.setColorSchemeResources(
            R.color.orange_500, 
            R.color.orange_700, 
            R.color.orange_500
        );
        

        swipeRefreshLayout.setOnRefreshListener(() -> {
            
            

            SharedPreferences prefs = requireActivity().getSharedPreferences(
                "history_fragment_state", 
                Context.MODE_PRIVATE
            );
            prefs.edit().putLong("last_refresh_time", System.currentTimeMillis()).apply();
            

            if (isAdded()) {

                requireActivity().runOnUiThread(() -> {
                    viewModel.initializeLiveDataObservers();
                    

                    viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
                        if (!isLoading) {
                            
                            swipeRefreshLayout.setRefreshing(false);
                            

                            updateDataFromViewModel();
                        }
                    });
                });
            }
        });
    }
    

    private void updateDataFromViewModel() {
        Map<Long, UserWorkout> viewModelWorkoutsByDate = viewModel.getWorkoutsByDate();
        if (viewModelWorkoutsByDate != null) {
            workoutsByDate.clear();
            workoutsByDate.putAll(viewModelWorkoutsByDate);
            
        }
        
        Map<Long, WorkoutPlan> viewModelPlansByDate = viewModel.getPlansByDate();
        if (viewModelPlansByDate != null) {
            plansByDate.clear();
            plansByDate.putAll(viewModelPlansByDate);
            
        }
    }
    
    private void setupViewModel() {

        Runnable setupObservers = () -> {
            if (isAdded()) {

                viewModel.getAllWorkouts().observe(getViewLifecycleOwner(), this::updateUI);
                

                viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::showLoading);
                

                viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                    if (!message.isEmpty()) {
                        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
                

                viewModel.getAllWorkouts().observe(getViewLifecycleOwner(), workouts -> {
                    if (workouts != null) {
                        
                        updateDataFromViewModel();
                    }
                });
            }
        };
        

        if (isAdded()) {
            requireActivity().runOnUiThread(setupObservers);
        }
    }

    private void setupRecyclerView() {
        adapter = new WorkoutHistoryAdapter(this);
        

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recentWorkoutsList.setLayoutManager(layoutManager);
        recentWorkoutsList.setItemAnimator(null);
        

        recentWorkoutsList.setAdapter(adapter);
    }

    private void setupCalendar() {

        if (!isAdded()) {

            Log.e(TAG, "setupCalendar: Fragment is not attached to a context");
            return;
        }


        calendarView.setTitleAnimationOrientation(MaterialCalendarView.VERTICAL);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        

        calendarView.setHeaderTextAppearance(R.style.CalendarHeader);
        calendarView.setDateTextAppearance(R.style.CalendarDateText);
        calendarView.setWeekDayTextAppearance(R.style.CalendarWeekText);
        

        String[] monthsArray = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", 
                               "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(monthsArray));
        

        String[] weekDaysArray = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(weekDaysArray));
        

        calendarView.state().edit()
            .setFirstDayOfWeek(DayOfWeek.of(1))
            .commit();
        

        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_OUT_OF_RANGE);
        

        calendarView.setDynamicHeightEnabled(true);
        

        calendarView.addDecorator(new OtherMonthDecorator());
        

        try {
            if (isAdded()) {
                workoutDecorator = new WorkoutDayDecorator(requireContext(), workoutDays);
                calendarView.addDecorator(workoutDecorator);
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error creating workout decorator: " + e.getMessage());
        }
        

        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return calendarView.getSelectedDate() != null && 
                       calendarView.getSelectedDate().equals(day);
            }

            @Override
            public void decorate(DayViewFacade view) {
                if (!isAdded()) {
                    return;
                }
                try {
                    Drawable background = ContextCompat.getDrawable(requireContext(), R.drawable.calendar_day_selected);
                    view.setBackgroundDrawable(background);
                    view.addSpan(new ForegroundColorSpan(Color.WHITE));
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error in selected day decorator: " + e.getMessage());
                }
            }
        });
        

        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.equals(CalendarDay.today());
            }

            @Override
            public void decorate(DayViewFacade view) {
                if (!isAdded()) {
                    return;
                }
                try {

                    Drawable background = ContextCompat.getDrawable(requireContext(), R.drawable.calendar_day_today);
                    view.setBackgroundDrawable(background);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error in today decorator: " + e.getMessage());
                }
            }
        });
        

        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.MONTH, -6);
        
        Calendar maxDate = Calendar.getInstance();
        
        calendarView.state().edit()
            .setMinimumDate(CalendarDay.from(
                minDate.get(Calendar.YEAR),
                minDate.get(Calendar.MONTH) + 1,
                minDate.get(Calendar.DAY_OF_MONTH)))
            .setMaximumDate(CalendarDay.from(
                maxDate.get(Calendar.YEAR),
                maxDate.get(Calendar.MONTH) + 1,
                maxDate.get(Calendar.DAY_OF_MONTH)))
            .commit();
            

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {

                executor.execute(() -> loadWorkoutsForDate(date));
            }
        });
    }

    private void updateUI(List<UserWorkout> workouts) {

        executor.execute(() -> {

            final List<UserWorkout> filteredWorkouts = workouts.stream()
                .filter(workout -> workout.getExercises() != null && !workout.getExercises().isEmpty())
                .collect(Collectors.toList());
            

            final int totalWorkoutsCount = filteredWorkouts.size();
            final int totalExercisesCount = filteredWorkouts.stream()
                .mapToInt(w -> w.getExercises().size())
                .sum();
            final double avgDurationMinutesValue = filteredWorkouts.stream()
                .mapToInt(UserWorkout::getDurationMinutes)
                .average()
                .orElse(0.0);
            

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {

                    if (filteredWorkouts.isEmpty()) {
                        recentWorkoutsList.setVisibility(View.GONE);
                        return;
                    }
                    
                    recentWorkoutsList.setVisibility(View.VISIBLE);
                    

                    adapter.updateWorkouts(filteredWorkouts);
                    

                    updateStats(totalWorkoutsCount, totalExercisesCount, avgDurationMinutesValue);
                    updateCalendarMarkers(filteredWorkouts);
                });
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        

        if (isLoading && adapter.getItemCount() == 0) {
            contentScroll.setVisibility(View.GONE);
        } else {
            contentScroll.setVisibility(View.VISIBLE);
        }
    }

    private void updateStats(int totalWorkouts, int totalExercises, double avgDurationMinutes) {

        executor.execute(() -> {

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    totalWorkoutsText.setText(String.format(Locale.getDefault(), "%d", totalWorkouts));
                    totalExercisesText.setText(String.format(Locale.getDefault(), "%d", totalExercises));
                    avgDurationText.setText(String.format(Locale.getDefault(), "%.0f мин", avgDurationMinutes));
                });
            }
            


            List<UserWorkout> currentWorkouts = adapter.getWorkouts();
            
            if (currentWorkouts != null && !currentWorkouts.isEmpty()) {


                String favoriteExercise = currentWorkouts.stream()
                    .flatMap(w -> w.getExercises().stream())
                    .filter(e -> e.getExercise() != null)
                    .collect(Collectors.groupingBy(
                        e -> e.getExercise().getName(),
                        Collectors.counting()
                    ))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
    
                
                

                final String finalFavoriteExercise = favoriteExercise;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (finalFavoriteExercise != null) {
                            favoriteExerciseText.setText(finalFavoriteExercise);
                        } else {
                            favoriteExerciseText.setText("Нет данных");
                        }
                    });
                }
            } else {

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> 
                        favoriteExerciseText.setText("Нет данных")
                    );
                }
            }
        });
    }

    private void updateCalendarMarkers(List<UserWorkout> workouts) {

        executor.execute(() -> {

            Set<CalendarDay> tempWorkoutDays = new HashSet<>();
            
            for (UserWorkout workout : workouts) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(workout.getStartTime());
                tempWorkoutDays.add(CalendarDay.from(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                ));
            }
            

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {

                    workoutDays.clear();
                    workoutDays.addAll(tempWorkoutDays);
                    
                    if (workoutDecorator != null) {
                        workoutDecorator.setDates(workoutDays);
                        calendarView.invalidateDecorators();
                    }
                });
            }
        });
    }

    private void showWorkoutDetails(UserWorkout workout) {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_workout_details, null);
        

        executor.execute(() -> {

            final String workoutName = workout.getName();
            final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, EEEE", new Locale("ru"));
            final String formattedDate = dateFormat.format(workout.getStartTime());
            

            String formattedDuration;
            if (workout.getEndTime() != null) {
                long duration = workout.getEndTime() - workout.getStartTime();
                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                
                if (hours > 0) {
                    formattedDuration = String.format(Locale.getDefault(), "%d ч %d мин", hours, minutes);
                } else {
                    formattedDuration = String.format(Locale.getDefault(), "%d минут", minutes);
                }
            } else {
                formattedDuration = "-";
            }
            

            final int exerciseCount = workout.getExercises().size();
            

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    try {

                        TextView workoutNameText = dialogView.findViewById(R.id.workout_name);
                        if (workoutName != null && !workoutName.isEmpty()) {
                            workoutNameText.setText(workoutName);
                            workoutNameText.setVisibility(View.VISIBLE);
                        } else {
                            workoutNameText.setVisibility(View.GONE);
                        }
                        

                        TextView dateText = dialogView.findViewById(R.id.workout_date);
                        dateText.setText(formattedDate);
                        

                        TextView durationText = dialogView.findViewById(R.id.workout_duration);
                        durationText.setText(formattedDuration);
                        

                        TextView exerciseCountText = dialogView.findViewById(R.id.exercise_count);
                        String exercisesText = getResources().getQuantityString(
                                R.plurals.exercise_count, exerciseCount, exerciseCount);
                        exerciseCountText.setText(exercisesText);
                        

                        RecyclerView exercisesList = dialogView.findViewById(R.id.exercises_list);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
                        exercisesList.setLayoutManager(layoutManager);
                        

                        exercisesList.setHasFixedSize(false);
                        exercisesList.setNestedScrollingEnabled(true);
                        

                        WorkoutDetailsAdapter detailsAdapter = new WorkoutDetailsAdapter();
                        exercisesList.setAdapter(detailsAdapter);
                        detailsAdapter.updateExercises(workout.getExercises());
                        

                        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
                        dialog.setContentView(dialogView);
                        

                        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                        if (bottomSheet != null) {
                            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                        }
                        
                        dialog.show();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при отображении деталей тренировки", e);
                    }
                });
            }
        });
    }

    private void showPlannedWorkout(WorkoutPlan plan) {

    }

    private void loadWorkoutsForDate(CalendarDay date) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.getYear(), date.getMonth() - 1, date.getDay());
        long normalizedDate = normalizeDate(calendar.getTimeInMillis());

        UserWorkout workout = workoutsByDate.get(normalizedDate);
        if (workout != null && workout.getExercises() != null && !workout.getExercises().isEmpty()) {
            showWorkoutDetails(workout);
        } else {
            WorkoutPlan plan = plansByDate.get(normalizedDate);
            if (plan != null) {
                showPlannedWorkout(plan);
            }
        }
    }

    @Override
    public void onWorkoutClick(UserWorkout workout) {
        if (workout != null && workout.getExercises() != null && !workout.getExercises().isEmpty()) {
            showWorkoutDetails(workout);
        }
    }


    private long normalizeDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
