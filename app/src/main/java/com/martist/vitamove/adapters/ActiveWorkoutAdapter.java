package com.martist.vitamove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.viewmodels.ExerciseViewModel;
import com.martist.vitamove.workout.data.models.ExerciseSet;
import com.martist.vitamove.workout.data.models.WorkoutExercise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActiveWorkoutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EXERCISE = 0;
    private static final int TYPE_ADD_BUTTON = 1;
    private ExerciseViewModel viewModel;
    
    private List<WorkoutExercise> exercises;
    private final OnExerciseClickListener listener;

    public interface OnExerciseClickListener {
        void onExerciseClick(WorkoutExercise exercise, int position);

        void onDeleteExercise(WorkoutExercise exercise, int position);
        void onExerciseOrderChanged(List<WorkoutExercise> exercises);
        void onAddExerciseClick();
    }

    public ActiveWorkoutAdapter(List<WorkoutExercise> exercises, OnExerciseClickListener listener) {
        this.exercises = new ArrayList<>(exercises);
        this.listener = listener;

    }

    @Override
    public int getItemViewType(int position) {
        return position == exercises.size() ? TYPE_ADD_BUTTON : TYPE_EXERCISE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_ADD_BUTTON) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_exercise, parent, false);
            return new AddExerciseViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_active_exercise, parent, false);
            return new ExerciseViewHolder(view);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_EXERCISE) {
            WorkoutExercise exercise = exercises.get(position);
            ((ExerciseViewHolder) holder).bind(exercise, position);

        } else {
            
        }
    }

    @Override
    public int getItemCount() {
        
        return exercises.size() + 1;
    }

    public void updateExercises(List<WorkoutExercise> newExercises) {


        this.exercises = new ArrayList<>(newExercises);

        
        updateOrderNumbers();

        notifyDataSetChanged();
    }




    public boolean moveExercise(int fromPosition, int toPosition) {
        
        if (fromPosition == exercises.size() || toPosition == exercises.size()) {
            return false;
        }

        if (fromPosition < 0 || fromPosition >= exercises.size() ||
            toPosition < 0 || toPosition >= exercises.size()) {
            return false;
        }

        Collections.swap(exercises, fromPosition, toPosition);

        updateOrderNumbers();

        notifyItemMoved(fromPosition, toPosition);

        if (listener != null) {
            listener.onExerciseOrderChanged(exercises);
        }

        return true;
    }

    private void updateOrderNumbers() {
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).setOrderNumber(i);
        }
    }



    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView nameText;
        private final TextView muscleGroupsText;
        private final ImageButton deleteButton;
        private final TextView setsText;
        private final TextView repsText;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            nameText = itemView.findViewById(R.id.exercise_name);
            muscleGroupsText = itemView.findViewById(R.id.exercise_muscle_groups);
            deleteButton = itemView.findViewById(R.id.delete_button);
            setsText = itemView.findViewById(R.id.exercise_sets);
            repsText = itemView.findViewById(R.id.exercise_reps);

            
            cardView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExerciseClick(exercises.get(position), position);
                }
            });

            
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteExercise(exercises.get(position), position);
                }
            });
        }

        void bind(WorkoutExercise exercise, int position) {
            nameText.setText(exercise.getExercise().getName());

            List<String> muscleGroupRussianNames = exercise.getExercise().getMuscleGroupRussianNames();
            String muscleGroups = String.join(", ", muscleGroupRussianNames);
            muscleGroupsText.setText(muscleGroups);


            List<ExerciseSet> sets = exercise.getSetsCompleted();
            int totalSets = sets.size();



            for (int i = 0; i < sets.size(); i++) {
                ExerciseSet set = sets.get(i);


            }

            
            boolean isCardioExercise = exercise.getExercise().isCardioExercise();
            boolean isStaticExercise = exercise.getExercise().isStaticExercise();

            
            boolean isWarmupStretching = exercise.getExercise().getExerciseType() != null &&
                (exercise.getExercise().getExerciseType().equalsIgnoreCase("разминка") ||
                 exercise.getExercise().getExerciseType().equalsIgnoreCase("warm-up") ||
                 exercise.getExercise().getExerciseType().equalsIgnoreCase("растяжка") ||
                 exercise.getExercise().getExerciseType().equalsIgnoreCase("stretching"));



            
            ImageView repsIcon = itemView.findViewById(R.id.exercise_reps_icon);
            
            ImageView setsIcon = itemView.findViewById(R.id.exercise_sets_icon);
            
            TextView separator = itemView.findViewById(R.id.stats_separator);

            
            if (isWarmupStretching) {
                
                boolean isCompleted = exercise.isRated();

                

                
                if (!isCompleted && sets != null && !sets.isEmpty()) {
                    for (ExerciseSet set : sets) {
                        if (set.isCompleted()) {
                            isCompleted = true;
                            break;
                        }
                    }
                }

                
                setsIcon.setVisibility(View.GONE);
                setsText.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);

                
                repsIcon.setVisibility(View.VISIBLE);
                repsIcon.setImageResource(isCompleted ?
                   R.drawable.ic_check : R.drawable.ic_time); 
                repsText.setVisibility(View.VISIBLE);
                repsText.setText(isCompleted ? "Выполнено" : "Не выполнено");


            }
            
            else if (isCardioExercise || isStaticExercise) {
                
                int totalSeconds = 0;

                
                for (ExerciseSet set : sets) {
                    if (set.getDurationSeconds() != null) {
                        totalSeconds += set.getDurationSeconds();
                    }
                }

                
                int totalMinutes = totalSeconds / 60;

                
                setsIcon.setVisibility(View.GONE);
                setsText.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);

                
                repsIcon.setVisibility(View.VISIBLE);
                repsIcon.setImageResource(R.drawable.ic_timer); 
                repsText.setVisibility(View.VISIBLE);
                repsText.setText(totalMinutes > 0 ? totalMinutes + " минут" : totalSeconds > 0 ? totalSeconds + " секунд" : "0 минут");


            } else {
                
                int completedSets = exercise.getCompletedSetsCount();


                int targetSets = exercise.getExercise().getDefaultSets();

                
                setsText.setVisibility(View.VISIBLE);

                
                if (repsIcon != null) {
                    repsIcon.setImageResource(R.drawable.ic_fitness);
                }

                
                if (setsIcon != null) {
                    setsIcon.setVisibility(View.VISIBLE);
                }

                
                if (separator != null) {
                    separator.setVisibility(View.VISIBLE);
                }

                if (totalSets > 0) {
                    setsText.setText(completedSets + "/" + totalSets + " подходов");

                } else if (targetSets > 0) {
                    setsText.setText("0/" + targetSets + " подходов");

                } else {
                    setsText.setText("0 подходов");

                }

                String defaultReps = exercise.getExercise().getDefaultReps();

                if (totalSets > 0 && !sets.isEmpty()) {
                    ExerciseSet firstSet = sets.get(0);
                    Integer reps = firstSet.getTargetReps() != null
                        ? firstSet.getTargetReps()
                        : firstSet.getReps();

                    if (reps != null) {
                        repsText.setText(reps + " повторений");

                    } else if (firstSet.getDurationSeconds() != null) {
                        int seconds = firstSet.getDurationSeconds();
                        repsText.setText(seconds + " секунд");

                    } else if (defaultReps != null && !defaultReps.isEmpty()) {
                        repsText.setText(defaultReps + " повторений");

                    } else {
                        repsText.setText("-- повторений");

                    }
                } else if (defaultReps != null && !defaultReps.isEmpty()) {
                    repsText.setText(defaultReps + " повторений");

                } else {
                    repsText.setText("-- повторений");

                }
            }
        }
    }
    
    class AddExerciseViewHolder extends RecyclerView.ViewHolder {
        AddExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddExerciseClick();
                }
            });
        }
    }
} 