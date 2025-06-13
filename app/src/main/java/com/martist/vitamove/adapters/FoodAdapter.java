package com.martist.vitamove.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.martist.vitamove.R;
import com.martist.vitamove.models.Food;
import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
    private List<Food> foods;
    private static OnFoodClickListener listener;
    private static final String TAG = "FoodAdapter";

    public interface OnFoodClickListener {
        void onFoodClick(Food food);
    }

    public FoodAdapter(List<Food> foods, OnFoodClickListener listener) {
        this.foods = foods;
        this.listener = listener;
    }

    public void setOnFoodClickListener(OnFoodClickListener listener) {
        this.listener = listener;
    }

    public void updateFoods(List<Food> newFoods) {
        
        if (newFoods != null) {
            for (Food food : newFoods) {
                
            }
        }
        this.foods = newFoods;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Food food = foods.get(position);
        holder.bind(food);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFoodClick(food);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foods != null ? foods.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView calories;
        private final View addButton;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.food_name);
            calories = view.findViewById(R.id.food_calories);
            addButton = view.findViewById(R.id.add_button);
        }

        void bind(Food food) {
            name.setText(food.getName());
            calories.setText(String.format("%d ккал, %dг", 
                Math.round(food.getCalories()), 
                100)); 

            addButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFoodClick(food);
                }
            });
        }
    }
} 