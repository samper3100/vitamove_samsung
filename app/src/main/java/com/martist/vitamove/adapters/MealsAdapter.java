package com.martist.vitamove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.models.Meal;

import java.util.List;
import java.util.Locale;

public class MealsAdapter extends RecyclerView.Adapter<MealsAdapter.ViewHolder> {
    private List<Meal> meals;

    public MealsAdapter(List<Meal> meals) {
        this.meals = meals;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_meal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meal meal = meals.get(position);
        
        holder.mealName.setText(meal.getTitle());
        holder.mealIcon.setImageResource(meal.getIconResId());
        

        float calories = meal.getCalories();
        holder.mealCalories.setText(String.format(Locale.getDefault(), "%.0f ккал", calories));
        

        holder.foodList.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.foodList.setAdapter(new MealFoodsAdapter(meal.getFoods(), meal.getTitle().toLowerCase()));
        

        ImageView expandIcon = holder.itemView.findViewById(R.id.expand_icon);
        

        holder.itemView.setOnClickListener(v -> {
            boolean expanded = holder.foodList.getVisibility() == View.VISIBLE;
            holder.foodList.setVisibility(expanded ? View.GONE : View.VISIBLE);
            

            if (expandIcon != null) {
                expandIcon.setRotation(expanded ? 0 : 180);
            }
        });
        

        TextView totalProteins = holder.itemView.findViewById(R.id.total_proteins);
        TextView totalFats = holder.itemView.findViewById(R.id.total_fats);
        TextView totalCarbs = holder.itemView.findViewById(R.id.total_carbs);
        
        if (totalProteins != null) {
            totalProteins.setText(String.format(Locale.getDefault(), "Б: %.1f г", meal.getTotalProteins()));
        }
        
        if (totalFats != null) {
            totalFats.setText(String.format(Locale.getDefault(), "Ж: %.1f г", meal.getTotalFats()));
        }
        
        if (totalCarbs != null) {
            totalCarbs.setText(String.format(Locale.getDefault(), "У: %.1f г", meal.getTotalCarbs()));
        }
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    public void updateMeals(List<Meal> newMeals) {
        this.meals = newMeals;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mealIcon;
        TextView mealName;
        RecyclerView foodList;
        TextView mealCalories;

        ViewHolder(View view) {
            super(view);
            mealIcon = view.findViewById(R.id.meal_icon);
            mealName = view.findViewById(R.id.meal_title);
            foodList = view.findViewById(R.id.food_list);
            mealCalories = view.findViewById(R.id.total_calories);
        }
    }
} 