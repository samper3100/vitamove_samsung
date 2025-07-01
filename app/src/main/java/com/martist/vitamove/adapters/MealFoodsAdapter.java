package com.martist.vitamove.adapters;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.models.Food;
import com.martist.vitamove.models.Meal;

import java.util.ArrayList;
import java.util.List;

public class MealFoodsAdapter extends RecyclerView.Adapter<MealFoodsAdapter.ViewHolder> {
    private static final String TAG = "MealFoodsAdapter";
    private List<Meal.FoodPortion> foods;
    private final String mealType;
    private ItemTouchHelper itemTouchHelper;
    private OnFoodRemovedListener onFoodRemovedListener;
    private OnFoodClickListener onFoodClickListener;
    private final Paint textPaint;
    private final ColorDrawable background;
    private Drawable deleteIcon;

    
    public interface OnFoodRemovedListener {
        void onFoodRemoved(int position);
    }
    
    
    public interface OnFoodClickListener {
        void onFoodClick(Food food, int portionSize, String mealType);
    }

    public MealFoodsAdapter(List<Meal.FoodPortion> foods, String mealType) {
        this.foods = foods != null ? foods : new ArrayList<>();
        this.mealType = mealType;
        this.textPaint = new Paint();
        this.background = new ColorDrawable(Color.RED);
    }

    public void setOnFoodRemovedListener(OnFoodRemovedListener listener) {
        this.onFoodRemovedListener = listener;
    }
    
    public void setOnFoodClickListener(OnFoodClickListener listener) {
        this.onFoodClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_meal_food, parent, false);
            
        
        if (deleteIcon == null) {
            deleteIcon = ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_delete);
        }
        
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Meal.FoodPortion foodPortion = foods.get(position);
            Food food = foodPortion.getFood();
            int portion = foodPortion.getPortionSize();
            
            
            holder.foodName.setText(food.getName());
            
            
            String unit = food.isLiquid() ? "мл" : "г";
            holder.portionSize.setText(portion + unit);
            
            
            String nutrients = String.format("Б %.1f • Ж %.1f • У %.1f",
                food.getProteins() * portion / 100f,
                food.getFats() * portion / 100f,
                food.getCarbs() * portion / 100f
            );
            holder.foodNutrients.setText(nutrients);
            
            
            holder.foodCalories.setText(String.format("%d ккал", 
                Math.round(food.getCalories() * portion / 100f)));
                
            holder.itemView.setVisibility(View.VISIBLE);
            
            
            holder.itemView.setOnClickListener(v -> {
                if (onFoodClickListener != null) {
                    onFoodClickListener.onFoodClick(food, portion, mealType);
                }
            });
            

        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return foods != null ? foods.size() : 0;
    }

    public void updateFoods(List<Meal.FoodPortion> newFoods) {

        this.foods = newFoods != null ? newFoods : new ArrayList<>();
        notifyDataSetChanged();
    }

    
    public void setupSwipeToDelete(RecyclerView recyclerView) {
        
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, 
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; 
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (onFoodRemovedListener != null) {
                        onFoodRemovedListener.onFoodRemoved(position);
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, 
                                  @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, 
                                  int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    
                    
                    float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth();
                    itemView.setAlpha(alpha);
                    
                    
                    if (dX < 0) {
                        
                        background.setBounds(
                            itemView.getRight() + (int)dX, 
                            itemView.getTop(), 
                            itemView.getRight(), 
                            itemView.getBottom()
                        );
                        background.draw(c);
                        
                        
                        if (deleteIcon != null) {
                            int itemHeight = itemView.getBottom() - itemView.getTop();
                            
                            int iconSize = (int)(itemHeight * 0.6f);
                            
                            
                            int iconMargin = (itemHeight - iconSize) / 2;
                            
                            
                            int iconLeft = itemView.getRight() - iconMargin - iconSize;
                            int iconRight = itemView.getRight() - iconMargin;
                            int iconTop = itemView.getTop() + (itemHeight - iconSize) / 2;
                            int iconBottom = iconTop + iconSize;
                            
                            
                            float scaleFactor = Math.min(1.0f, Math.abs(dX) / (itemView.getWidth() / 3f));
                            float iconScale = 0.9f + 0.3f * scaleFactor; 
                            
                            int iconCenterX = (iconLeft + iconRight) / 2;
                            int iconCenterY = (iconTop + iconBottom) / 2;
                            
                            int scaledIconSize = (int)(iconSize * iconScale);
                            iconLeft = iconCenterX - scaledIconSize / 2;
                            iconRight = iconCenterX + scaledIconSize / 2;
                            iconTop = iconCenterY - scaledIconSize / 2;
                            iconBottom = iconCenterY + scaledIconSize / 2;
                            
                            
                            if (Math.abs(dX) < itemView.getWidth() / 3) {
                                iconLeft = Math.max(itemView.getRight() - iconMargin - scaledIconSize, 
                                                   itemView.getRight() + (int)dX + iconMargin);
                                iconRight = Math.max(itemView.getRight() - iconMargin, 
                                                    itemView.getRight() + (int)dX + iconMargin + scaledIconSize);
                            }
                            
                            
                            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            deleteIcon.setAlpha(255);  
                            deleteIcon.draw(c);
                        }
                    }
                    
                    
                    float maxSwipe = itemView.getWidth() / 2.5f; 
                    if (Math.abs(dX) > maxSwipe) {
                        dX = -maxSwipe;
                    }
                    
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
            
            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.4f; 
            }
            
            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 5; 
            }
            
            @Override
            public float getSwipeVelocityThreshold(float defaultValue) {
                return defaultValue * 0.5f; 
            }
            
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setTranslationX(0);
            }
        };
        
        
        itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView foodName;
        final TextView portionSize;
        final TextView foodNutrients;
        final TextView foodCalories;

        ViewHolder(View view) {
            super(view);
            foodName = view.findViewById(R.id.food_name);
            portionSize = view.findViewById(R.id.portion_size);
            foodNutrients = view.findViewById(R.id.food_nutrients);
            foodCalories = view.findViewById(R.id.food_calories);
        }
    }
} 