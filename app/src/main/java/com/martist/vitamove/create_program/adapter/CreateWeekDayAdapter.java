package com.martist.vitamove.create_program.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.create_program.model.CreateProgramDay;
import com.martist.vitamove.workout.data.models.Exercise;

import java.util.ArrayList;
import java.util.List;


public class CreateWeekDayAdapter extends RecyclerView.Adapter<CreateWeekDayAdapter.DayViewHolder> {

    
    private List<CreateProgramDay> daysData = new ArrayList<>();
    private final OnDayClickListener dayClickListener;
    private final ExerciseActionCallback exerciseActionCallback;

    
    public interface OnDayClickListener {
        void onDayClick(int dayNumber);
    }

    
    public interface ExerciseActionCallback {
        void onReplaceExerciseRequest(int dayNumber, int exercisePosition, String exerciseId);
    }

    
    public CreateWeekDayAdapter(OnDayClickListener dayClickListener, ExerciseActionCallback exerciseActionCallback) {
        this.dayClickListener = dayClickListener;
        this.exerciseActionCallback = exerciseActionCallback;
    }

    
    public void setDays(List<CreateProgramDay> newDays) {
        
        this.daysData = newDays != null ? new ArrayList<>(newDays) : new ArrayList<>();
        notifyDataSetChanged();
    }

    
    public List<CreateProgramDay> getDays() {
        return daysData;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_create_program_day, parent, false);
        return new DayViewHolder(view, dayClickListener, exerciseActionCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CreateProgramDay day = daysData.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return daysData.size();
    }

    
    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayTitle;
        TextView dayDescription;
        RecyclerView rvDayExercises; 
        DayExerciseListAdapter exerciseAdapter; 
        private final OnDayClickListener dayClickListener;
        private final ExerciseActionCallback exerciseActionCallback;

        public DayViewHolder(@NonNull View itemView, OnDayClickListener dayClickListener, ExerciseActionCallback exerciseActionCallback) {
            super(itemView);
            this.dayClickListener = dayClickListener;
            this.exerciseActionCallback = exerciseActionCallback;
            dayTitle = itemView.findViewById(R.id.tv_day_title);
            dayDescription = itemView.findViewById(R.id.tv_day_description);
            rvDayExercises = itemView.findViewById(R.id.rv_day_exercises); 
        }

        public void bind(CreateProgramDay day) {
            dayTitle.setText(day.getTitle());

            if (rvDayExercises != null) {
                
                
                DayExerciseListAdapter.OnExerciseActionListener innerListener = 
                    (position, exerciseId) -> { 
                        if (exerciseActionCallback != null) {
                            
                            exerciseActionCallback.onReplaceExerciseRequest(day.getDayNumber(), position, exerciseId);
                        }
                    };

                
                if (rvDayExercises.getLayoutManager() == null) {
                    rvDayExercises.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                    rvDayExercises.setNestedScrollingEnabled(false);
                }
                exerciseAdapter = new DayExerciseListAdapter(innerListener); 
                rvDayExercises.setAdapter(exerciseAdapter);

                
                List<Exercise> exercises = day.getSelectedExercises();
                if (exercises != null && !exercises.isEmpty()) {
                    dayDescription.setText(String.format("Упражнений: %d", exercises.size()));
                    rvDayExercises.setVisibility(View.VISIBLE);
                    exerciseAdapter.setExercises(exercises);
                } else {
                    dayDescription.setText("Нажмите, чтобы настроить день");
                    rvDayExercises.setVisibility(View.GONE);
                    exerciseAdapter.setExercises(new ArrayList<>());
                }
            }

            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (dayClickListener != null && position != RecyclerView.NO_POSITION) {
                    dayClickListener.onDayClick(day.getDayNumber());
                }
            });
        }
    }
} 