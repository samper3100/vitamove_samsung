package com.martist.vitamove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.models.Food;

import java.util.List;

public class FoodSelectionAdapter extends RecyclerView.Adapter<FoodSelectionAdapter.ViewHolder> {
    private final List<Food> foods;
    private final OnFoodClickListener listener;

    public interface OnFoodClickListener {
        void onFoodClick(Food food);
    }

    public FoodSelectionAdapter(List<Food> foods, OnFoodClickListener listener) {
        this.foods = foods;
        this.listener = listener;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Food food = foods.get(position);
        holder.bind(food, listener);
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView detailsView;

        ViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.food_name);
            detailsView = itemView.findViewById(R.id.food_details);
        }

        void bind(final Food food, final OnFoodClickListener listener) {
            nameView.setText(food.getName());
            String details = String.format("%.0f ккал | Б: %.1f | Ж: %.1f | У: %.1f",
                food.getCalories(),
                food.getProteins(),
                food.getFats(),
                food.getCarbs());
            detailsView.setText(details);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFoodClick(food);
                }
            });
        }
    }
} 