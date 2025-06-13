package com.martist.vitamove.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.workout.data.models.Exercise;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

public class ExerciseAdapter extends ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder> {

    private final Set<Exercise> selectedItems = new HashSet<>();
    private List<Exercise> originalList = new ArrayList<>();

    public ExerciseAdapter() {
        super(new ExerciseDiffCallback());
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = getItem(position);
        holder.bind(exercise, selectedItems.contains(exercise));
        holder.itemView.setOnClickListener(v -> {
            if (selectedItems.contains(exercise)) {
                selectedItems.remove(exercise);
            } else {
                selectedItems.add(exercise);
            }
            notifyItemChanged(position);
        });
    }

    public void filter(String query) {
        List<Exercise> filteredList;
        if (query.isEmpty()) {
            filteredList = new ArrayList<>(originalList);
        } else {
            filteredList = originalList.stream()
                .filter(exercise -> exercise.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        }
        submitList(filteredList);
    }

    @Override
    public void submitList(List<Exercise> list) {
        originalList = new ArrayList<>(list);
        super.submitList(list);
    }

    public List<Exercise> getSelectedExercises() {
        return new ArrayList<>(selectedItems);
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final MaterialCardView cardView;

        ExerciseViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.exercise_name);
            cardView = (MaterialCardView) view;
        }

        void bind(Exercise exercise, boolean isSelected) {
            nameTextView.setText(exercise.getName());
            cardView.setCardBackgroundColor(isSelected ? 
                itemView.getContext().getColor(R.color.colorSecondary) : 
                Color.WHITE);
        }
    }

    private static class ExerciseDiffCallback extends DiffUtil.ItemCallback<Exercise> {
        @Override
        public boolean areItemsTheSame(@NonNull Exercise oldItem, @NonNull Exercise newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Exercise oldItem, @NonNull Exercise newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId()) &&
                   Objects.equals(oldItem.getName(), newItem.getName());
        }
    }
} 