package com.martist.vitamove.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Meal implements Parcelable {
    private final String title;
    private final int iconResId;
    private final List<FoodPortion> foods;
    private float calories;
    private float proteins;
    private float fats;
    private float carbs;
    private static final String TAG = "Meal";

    public Meal(String title, int iconResId) {
        this.title = title;
        this.iconResId = iconResId;
        this.foods = new ArrayList<>();
        this.calories = 0;
    }

    public String getTitle() { return title; }
    public String getName() { return title; }
    public int getIconResId() { return iconResId; }
    public List<FoodPortion> getFoods() { return foods; }

    
    public Food getFood(int index) {
        if (foods != null && index >= 0 && index < foods.size()) {
            return foods.get(index).getFood();
        }
        return null;
    }

    public float getCalories() {
        float totalCalories = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getPortionSize() / 100f;
                totalCalories += food.getCalories() * portionMultiplier;
            }
        }
        return totalCalories;
    }

    public float getProteins() {
        float totalProteins = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getPortionSize() / 100f;
                totalProteins += food.getProteins() * portionMultiplier;
            }
        }
        return totalProteins;
    }

    public float getFats() {
        float totalFats = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getPortionSize() / 100f;
                totalFats += food.getFats() * portionMultiplier;
            }
        }
        return totalFats;
    }

    public float getCarbs() {
        float totalCarbs = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getPortionSize() / 100f;
                totalCarbs += food.getCarbs() * portionMultiplier;
            }
        }
        return totalCarbs;
    }

    public float getTotalProteins() {
        float total = 0;
        for (FoodPortion portion : foods) {
            Food food = portion.getFood();
            float multiplier = portion.getPortionSize() / 100f;
            total += food.getProteins() * multiplier;
        }
        return total;
    }

    public float getTotalFats() {
        float total = 0;
        for (FoodPortion portion : foods) {
            Food food = portion.getFood();
            float multiplier = portion.getPortionSize() / 100f;
            total += food.getFats() * multiplier;
        }
        return total;
    }

    public float getTotalCarbs() {
        float total = 0;
        for (FoodPortion portion : foods) {
            Food food = portion.getFood();
            float multiplier = portion.getPortionSize() / 100f;
            total += food.getCarbs() * multiplier;
        }
        return total;
    }

    public void addFood(Food food, int portionSize) {
        foods.add(new FoodPortion(food, portionSize));
        updateNutrients();
    }

    public void addFood(SelectedFood selectedFood) {
        
        Food food = selectedFood.getFood();
        int portionSize = (int) selectedFood.getAmount();
        addFood(food, portionSize);
    }

    private void updateNutrients() {
        calories = 0;
        proteins = 0;
        fats = 0;
        carbs = 0;

        for (FoodPortion food : foods) {
            calories += food.getFood().getCalories() * food.getPortionSize();
            proteins += food.getFood().getProteins() * food.getPortionSize();
            fats += food.getFood().getFats() * food.getPortionSize();
            carbs += food.getFood().getCarbs() * food.getPortionSize();
        }
    }

    
    public boolean removeFood(int position) {

        if (foods != null && position >= 0 && position < foods.size()) {
            foods.remove(position);
            updateNutrients();

            return true;
        }
        Log.e(TAG, "Ошибка при удалении продукта: некорректная позиция " + position);
        return false;
    }

    
    public boolean updateFoodPortion(String foodId, int newPortionSize) {

        if (foods != null) {
            for (int i = 0; i < foods.size(); i++) {
                FoodPortion portion = foods.get(i);
                if (portion.getFood().getId() == Long.parseLong(foodId)) {
                    
                    FoodPortion newPortion = new FoodPortion(portion.getFood(), newPortionSize);
                    
                    foods.set(i, newPortion);
                    
                    updateNutrients();

                    return true;
                }
            }
        }

        return false;
    }
    
    
    public boolean addOrUpdateFood(Food food, int portionSize) {
        if (food == null) {
            Log.e(TAG, "Невозможно добавить null продукт");
            return false;
        }
        
        
        if (foods != null) {
            for (int i = 0; i < foods.size(); i++) {
                FoodPortion portion = foods.get(i);
                if (portion.getFood().getId() == food.getId()) {
                    
                    FoodPortion newPortion = new FoodPortion(food, portionSize);
                    foods.set(i, newPortion);
                    updateNutrients();

                    return true;
                }
            }
        }
        
        
        foods.add(new FoodPortion(food, portionSize));
        updateNutrients();

        return false;
    }

    
    protected Meal(Parcel in) {
        title = in.readString();
        iconResId = in.readInt();
        foods = new ArrayList<>();
        in.readTypedList(foods, FoodPortion.CREATOR);
        calories = in.readFloat();
        proteins = in.readFloat();
        fats = in.readFloat();
        carbs = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeInt(iconResId);
        dest.writeTypedList(foods);
        dest.writeFloat(calories);
        dest.writeFloat(proteins);
        dest.writeFloat(fats);
        dest.writeFloat(carbs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Meal> CREATOR = new Creator<Meal>() {
        @Override
        public Meal createFromParcel(Parcel in) {
            return new Meal(in);
        }

        @Override
        public Meal[] newArray(int size) {
            return new Meal[size];
        }
    };

    public static class FoodPortion implements Parcelable {
        private final Food food;
        private final int portionSize;

        public FoodPortion(Food food, int portionSize) {
            this.food = food;
            this.portionSize = portionSize;
        }

        public Food getFood() {
            return food;
        }

        public int getPortionSize() {
            return portionSize;
        }

        
        protected FoodPortion(Parcel in) {
            food = in.readParcelable(Food.class.getClassLoader());
            portionSize = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(food, flags);
            dest.writeInt(portionSize);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<FoodPortion> CREATOR = new Creator<FoodPortion>() {
            @Override
            public FoodPortion createFromParcel(Parcel in) {
                return new FoodPortion(in);
            }

            @Override
            public FoodPortion[] newArray(int size) {
                return new FoodPortion[size];
            }
        };
    }

    
    private float calculateTotalNutrient(Function<Food, Float> nutrientGetter) {
        return foods.stream()
            .map(portion -> {
                Food food = portion.getFood();
                float multiplier = portion.getPortionSize() / 100f;
                return nutrientGetter.apply(food) * multiplier;
            })
            .reduce(0f, Float::sum);
    }

    
    public void updateAllNutrients() {
        updateNutrients();
    }
} 