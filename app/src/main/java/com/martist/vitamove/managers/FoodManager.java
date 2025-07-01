package com.martist.vitamove.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.R;
import com.martist.vitamove.api.GigaChatProductService;
import com.martist.vitamove.database.DayMeal;
import com.martist.vitamove.database.MealsDatabase;
import com.martist.vitamove.events.MealUpdatedEvent;
import com.martist.vitamove.events.MealsLoadedEvent;
import com.martist.vitamove.events.NutrientsNormsUpdatedEvent;
import com.martist.vitamove.models.Food;
import com.martist.vitamove.models.Meal;
import com.martist.vitamove.models.SelectedFood;
import com.martist.vitamove.repositories.SupabaseBarcodeRepository;
import com.martist.vitamove.repositories.SupabaseFoodRepository;
import com.martist.vitamove.utils.SupabaseClient;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class FoodManager {
    private static final String PREFS_NAME = "FoodManagerPrefs";
    private static final String SELECTED_FOODS_KEY = "selectedFoods";
    private static FoodManager instance;
    private final Map<String, List<SelectedFood>> selectedFoods = new HashMap<>();
    

    private final Map<String, Float> dailyNorms = new HashMap<>();
    private String currentFitnessGoal = "weight_loss";
    private int targetCalories = 2000;
    
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context context;
    private final Map<String, Meal> meals;
    private final SupabaseFoodRepository foodRepository;
    private final SupabaseBarcodeRepository barcodeRepository;
    private final GigaChatProductService gigaChatProductService;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private List<Food> foods;
    private final Date currentDate;
    private Date selectedDateForView;
    private final MealsDatabase database;
    private static final String TAG = "FoodManager";
    private final Map<String, Boolean> foodExistsCache = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler();
    private final String userId;


    private final MutableLiveData<Float> caloriesNormLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> proteinsNormLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> fatsNormLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> carbsNormLiveData = new MutableLiveData<>();

    public FoodManager(Context context) {
        this.context = context;
        this.database = MealsDatabase.getInstance(context);
        

        this.gson = new com.google.gson.GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .create();
            
        this.currentDate = new Date();
        this.meals = new HashMap<>();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        

        SharedPreferences userPrefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        this.userId = userPrefs.getString("userId", "default_user");
        
        

        SupabaseClient supabaseClient = SupabaseClient.getInstance(
            "qjopbdiafgbbstkwmhpt", 
            context.getString(R.string.supabase_key)
        );
        

        this.foodRepository = new SupabaseFoodRepository(supabaseClient, context);
        this.barcodeRepository = new SupabaseBarcodeRepository(supabaseClient, context);
        

        this.gigaChatProductService = new GigaChatProductService();
        

        initDefaultNorms();
        

        loadUserSettings();
        

        loadSelectedFoods();
        loadMealsForCurrentDate();
    }

    public static FoodManager getInstance(Context context) {
        if (instance == null) {
            instance = new FoodManager(context);
        }
        return instance;
    }


    public LiveData<Float> getCaloriesNormLiveData() {
        return caloriesNormLiveData;
    }

    public LiveData<Float> getProteinsNormLiveData() {
        return proteinsNormLiveData;
    }

    public LiveData<Float> getFatsNormLiveData() {
        return fatsNormLiveData;
    }

    public LiveData<Float> getCarbsNormLiveData() {
        return carbsNormLiveData;
    }

    public float getDailyNorm(String nutrient) {
        return dailyNorms.getOrDefault(nutrient, 0f);
    }

    private void loadSelectedFoods() {
        try {

            String json = prefs.getString(SELECTED_FOODS_KEY, "{}");
            Type mapType = new TypeToken<HashMap<String, List<SelectedFood>>>(){}.getType();
            Map<String, List<SelectedFood>> loaded = gson.fromJson(json, mapType);
            if (loaded != null) {
                selectedFoods.clear();
                selectedFoods.putAll(loaded);
            }
        } catch (JsonSyntaxException e) {
            try {

                String json = prefs.getString(SELECTED_FOODS_KEY, "[]");
                Type listType = new TypeToken<ArrayList<SelectedFood>>(){}.getType();
                List<SelectedFood> oldData = gson.fromJson(json, listType);


                if (oldData != null && !oldData.isEmpty()) {
                    selectedFoods.clear();

                    selectedFoods.put("breakfast", new ArrayList<>(oldData));

                    saveSelectedFoods();

                    prefs.edit().putString(SELECTED_FOODS_KEY, "{}").apply();
                }
            } catch (Exception ignored) {

                selectedFoods.clear();
                saveSelectedFoods();
            }
        }
    }

    private void saveSelectedFoods() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SELECTED_FOODS_KEY, gson.toJson(selectedFoods));
        editor.apply();
    }


    public double getTotalCalories() {
        return getTotalCaloriesForSelectedDate();
    }


    public double getTotalCaloriesForCurrentDate() {
        double total = 0;
        

        Map<String, Meal> savedMeals = new HashMap<>(meals);
        

        loadMealsForDate(currentDate);
        

        Meal breakfast = getMeal("breakfast");
        Meal lunch = getMeal("lunch");
        Meal dinner = getMeal("dinner");
        Meal snack = getMeal("snack");
        

        if (breakfast != null) {
            total += breakfast.getCalories();
        }
        if (lunch != null) {
            total += lunch.getCalories();
        }
        if (dinner != null) {
            total += dinner.getCalories();
        }
        if (snack != null) {
            total += snack.getCalories();
        }
        

        meals.clear();
        meals.putAll(savedMeals);
        
        
        return total;
    }


    public double getTotalCaloriesForSelectedDate() {
        double total = 0;
        

        Meal breakfast = getMeal("breakfast");
        Meal lunch = getMeal("lunch");
        Meal dinner = getMeal("dinner");
        Meal snack = getMeal("snack");
        

        if (breakfast != null) {
            total += breakfast.getCalories();
        }
        if (lunch != null) {
            total += lunch.getCalories();
        }
        if (dinner != null) {
            total += dinner.getCalories();
        }
        if (snack != null) {
            total += snack.getCalories();
        }
        
        
        return total;
    }

    public double getTotalProteins() {
        double total = 0;
        for (List<SelectedFood> mealFoods : selectedFoods.values()) {
            for (SelectedFood selectedFood : mealFoods) {
                double amount = selectedFood.getAmount() / 100.0;
                total += selectedFood.getFood().getProteins() * amount;
            }
        }
        return total;
    }


    public double getTotalProtein() {
        return getTotalProteins();
    }

    public double getTotalFat() {
        double total = 0;
        for (List<SelectedFood> mealFoods : selectedFoods.values()) {
            for (SelectedFood selectedFood : mealFoods) {
                double amount = selectedFood.getAmount() / 100.0;
                total += selectedFood.getFood().getFats() * amount;
            }
        }
        return total;
    }



    public double getTotalCarbs() {
        double total = 0;
        for (List<SelectedFood> mealFoods : selectedFoods.values()) {
            for (SelectedFood selectedFood : mealFoods) {
                double amount = selectedFood.getAmount() / 100.0;
                total += selectedFood.getFood().getCarbs() * amount;
            }
        }
        return total;
    }

    public void addFoodToMeal(String mealType, Food food, float amount) {
        
                  
        if (mealType == null) {
            Log.e(TAG, "Ошибка: mealType равен null");
            return;
        }
        
        if (food == null) {
            Log.e(TAG, "Ошибка: food равен null");
            return;
        }
        

        Meal meal = meals.get(mealType);
        if (meal == null) {
            
            meal = new Meal(getMealTitle(mealType), getMealIcon(mealType));
        }
        

        meal.addFood(food, (int)amount);
        

        meals.put(mealType, meal);
        
        


        final String dateStr = dateFormat.format(getSelectedDateForView());
        final Meal finalMeal = meal;

        executor.execute(() -> {
            try {

                DayMeal existingMeal = database.mealDao().getMealByDateAndType(dateStr, mealType, userId);
                

                DayMeal dayMeal = new DayMeal();
                dayMeal.date = dateStr;
                dayMeal.mealType = mealType;
                dayMeal.mealData = gson.toJson(finalMeal);
                dayMeal.createdAt = new Date();
                dayMeal.userId = userId;
                
                if (existingMeal != null) {

                    existingMeal.mealData = dayMeal.mealData;
                    existingMeal.updatedAt = new Date();
                    database.mealDao().update(existingMeal);
                    
                } else {

                    database.mealDao().insert(dayMeal);
                    
                }
                

                int totalCalories = (int) getTotalCalories();
                

                CaloriesManager caloriesManager = CaloriesManager.getInstance(context);
                caloriesManager.setConsumedCalories(totalCalories);
                
                
                

                mainHandler.post(() -> {
                    
                    EventBus.getDefault().post(new MealUpdatedEvent(mealType));
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при сохранении приема пищи: " + e.getMessage(), e);
            }
        });
    }


    private int getMealIcon(String mealType) {
        switch (mealType) {
            case "breakfast":
                return R.drawable.ic_breakfast;
            case "lunch":
                return R.drawable.ic_lunch;
            case "dinner":
                return R.drawable.ic_dinner;
            case "snack":
                return R.drawable.ic_snack;
            default:
                return R.drawable.ic_food;
        }
    }

    private String getMealTitle(String mealType) {
        switch (mealType.toLowerCase()) {
            case "breakfast": return "Завтрак";
            case "lunch": return "Обед";
            case "dinner": return "Ужин";
            case "snack": return "Перекус";
            default: return "Прием пищи";
        }
    }











    public void setSelectedDateForView(Date date) {
        this.selectedDateForView = date;
        loadMealsForSelectedDate();
    }


    public Date getSelectedDateForView() {
        return selectedDateForView != null ? selectedDateForView : currentDate;
    }



    public String getSelectedDateFormatted() {
        return dateFormat.format(getSelectedDateForView());
    }

    public Meal getMeal(String mealType) {
        return meals.get(mealType);
    }


    private void loadMealsForSelectedDate() {
        loadMealsForDate(selectedDateForView);
    }


    private void loadMealsForCurrentDate() {
        loadMealsForDate(currentDate);
    }


    private void loadMealsForDate(Date date) {
        if (date == null) {
            Log.e(TAG, "loadMealsForDate: date is null");
            return;
        }
        
        new Thread(() -> {
            String dateStr = dateFormat.format(date);
            
            
            List<DayMeal> dayMeals = database.mealDao().getMealsForDate(dateStr, userId);
            meals.clear();
            
            for (DayMeal dayMeal : dayMeals) {
                try {
                    Meal meal = gson.fromJson(dayMeal.mealData, Meal.class);
                    meals.put(dayMeal.mealType, meal);
                    
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "Error parsing meal data: " + e.getMessage());
                }
            }
            

            foodExistsCache.clear();
            
            EventBus.getDefault().post(new MealsLoadedEvent(meals));
        }).start();
    }

    public List<Food> searchFoods(String query) {
        if (query == null || query.trim().isEmpty()) {
            return foodRepository.getPopularFoods();
        }


        return foodRepository.searchFoodsByQuery(query);
    }

    public List<Food> getAllFoods() {
        return foodRepository.getAllFoods();
    }

    public boolean hasFoodForDate(String date) {

        if (foodExistsCache.containsKey(date)) {
            return foodExistsCache.get(date);
        }

        try {

            List<DayMeal> dayMeals = database.mealDao().getMealsForDate(date, userId);
            
            for (DayMeal dayMeal : dayMeals) {
                try {
                    Meal meal = gson.fromJson(dayMeal.mealData, Meal.class);
                    if (meal != null && !meal.getFoods().isEmpty()) {
                        foodExistsCache.put(date, true);
                        return true;
                    }
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "Error parsing meal data: " + e.getMessage());
                }
            }

            foodExistsCache.put(date, false);
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking food for date: " + e.getMessage());
            return false;
        }
    }




    private void initDefaultNorms() {
        dailyNorms.put("calories", 2000f);
        dailyNorms.put("proteins", 90f);
        dailyNorms.put("fats", 70f);
        dailyNorms.put("carbs", 250f);
        dailyNorms.put("vitamin_a", 900f);
        dailyNorms.put("vitamin_c", 90f);
        dailyNorms.put("vitamin_d", 15f);
        dailyNorms.put("calcium", 1000f);
        dailyNorms.put("iron", 18f);
    }
    

    private void loadUserSettings() {

        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");
        

        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String userDataGoal = userPrefs.getString("fitness_goal", "");
        


        if (!userDataGoal.isEmpty() && !userDataGoal.equals(fitnessGoal)) {
            fitnessGoal = userDataGoal;

            SharedPreferences.Editor editor = appPrefs.edit();
            editor.putString("fitness_goal", fitnessGoal);
            editor.apply();
            
        }
        
        this.currentFitnessGoal = fitnessGoal;
        

        this.targetCalories = userPrefs.getInt("target_calories", 2000);
        

        
        

        updateNutrientsNorms();
    }
    

    private void updateNutrientsNorms() {

        dailyNorms.put("calories", (float) targetCalories);
        

        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        float currentWeight = userPrefs.getFloat("current_weight", 70f);
        

        float proteinPerKg = 0f;
        float fatPercentage = 0f;
        float carbPercentage = 0f;
        

        float maxProteinGrams = currentWeight * 2.2f;
        
        switch (currentFitnessGoal) {
            case "muscle_gain":




                proteinPerKg = 1.8f;
                fatPercentage = 0.25f;
                

                float proteinGrams = currentWeight * proteinPerKg;

                float proteinCalories = proteinGrams * 4f;
                float proteinPercentage = proteinCalories / targetCalories;
                

                carbPercentage = 1f - proteinPercentage - fatPercentage;
                

                if (carbPercentage < 0.4f) {
                    fatPercentage = 0.22f;
                    carbPercentage = 1f - proteinPercentage - fatPercentage;
                }
                

                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;
                
            case "weight_loss":



                proteinPerKg = 1.8f;
                fatPercentage = 0.25f;
                

                proteinGrams = currentWeight * proteinPerKg;

                proteinCalories = proteinGrams * 4f;
                proteinPercentage = proteinCalories / targetCalories;
                

                carbPercentage = 1f - proteinPercentage - fatPercentage;
                

                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;
                
            case "endurance":




                proteinPerKg = 1.2f;
                fatPercentage = 0.22f;
                

                proteinGrams = currentWeight * proteinPerKg;

                proteinCalories = proteinGrams * 4f;
                proteinPercentage = proteinCalories / targetCalories;
                

                carbPercentage = 1f - proteinPercentage - fatPercentage;
                if (carbPercentage < 0.55f) {
                    fatPercentage = 1f - proteinPercentage - 0.55f;
                    carbPercentage = 0.55f;
                }
                

                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;
                
            case "general_fitness":
            default:




                proteinPerKg = 1.2f;
                fatPercentage = 0.28f;
                

                proteinGrams = currentWeight * proteinPerKg;

                proteinCalories = proteinGrams * 4f;
                proteinPercentage = proteinCalories / targetCalories;
                

                carbPercentage = 1f - proteinPercentage - fatPercentage;
                

                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;
        }
        

        

        float currentProteinGrams = dailyNorms.get("proteins");
        if (currentProteinGrams > maxProteinGrams) {

            
            dailyNorms.put("proteins", maxProteinGrams);
            

            float proteinCalories = maxProteinGrams * 4f;
            float proteinPercentage = proteinCalories / targetCalories;
            

            if (currentFitnessGoal.equals("endurance")) {

                carbPercentage = 0.6f;
                fatPercentage = 1f - proteinPercentage - carbPercentage;
            } else {

                fatPercentage = 0.25f;
                carbPercentage = 1f - proteinPercentage - fatPercentage;
            }
            

            dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
            dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
        }
        

        float currentFatGrams = dailyNorms.get("fats");
        float minFatGrams = 0.5f * currentWeight;
        
        if (currentFatGrams < minFatGrams) {
            
            dailyNorms.put("fats", minFatGrams);
            

            float proteinCalories = dailyNorms.get("proteins") * 4f;
            float fatCalories = minFatGrams * 9f;
            float carbCalories = targetCalories - proteinCalories - fatCalories;
            

            if (carbCalories > 0) {
                dailyNorms.put("carbs", carbCalories / 4f);
            }
        }
        

        
        

        caloriesNormLiveData.postValue(dailyNorms.get("calories"));
        proteinsNormLiveData.postValue(dailyNorms.get("proteins"));
        fatsNormLiveData.postValue(dailyNorms.get("fats"));
        carbsNormLiveData.postValue(dailyNorms.get("carbs"));


        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("calories_norm", dailyNorms.getOrDefault("calories", 2000f));
        editor.putFloat("proteins_norm", dailyNorms.getOrDefault("proteins", 0f));
        editor.putFloat("fats_norm", dailyNorms.getOrDefault("fats", 0f));
        editor.putFloat("carbs_norm", dailyNorms.getOrDefault("carbs", 0f));
        editor.apply();


        EventBus.getDefault().post(new NutrientsNormsUpdatedEvent(dailyNorms));
    }
    

    public void updateTargetCalories(int newTargetCalories) {
        this.targetCalories = newTargetCalories;
        updateNutrientsNorms();
    }
    

    public void updateFitnessGoal(String newFitnessGoal) {
        this.currentFitnessGoal = newFitnessGoal;
        updateNutrientsNorms();
    }


    public void refreshNutrientNorms() {
        loadUserSettings();
        updateNutrientsNorms();
    }


    public void updateMeal(String mealType, Meal meal) {
        
        

        meals.put(mealType, meal);
        

        executor.execute(() -> {
            try {

                String dateStr = dateFormat.format(getSelectedDateForView());
                

                DayMeal dayMeal = new DayMeal();
                dayMeal.date = dateStr;
                dayMeal.mealType = mealType;
                dayMeal.mealData = gson.toJson(meal);
                dayMeal.createdAt = new Date();
                dayMeal.userId = userId;
                

                DayMeal existingMeal = database.mealDao().getMealByDateAndType(dateStr, mealType, userId);
                
                if (existingMeal != null) {

                    existingMeal.mealData = dayMeal.mealData;
                    existingMeal.updatedAt = new Date();
                    database.mealDao().update(existingMeal);
                    
                } else {

                    database.mealDao().insert(dayMeal);
                    
                }
                

                int totalCalories = (int) getTotalCalories();
                

                CaloriesManager caloriesManager = CaloriesManager.getInstance(context);
                caloriesManager.setConsumedCalories(totalCalories);
                
                
                


                mainHandler.post(() -> EventBus.getDefault().post(new MealUpdatedEvent(mealType)));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении приема пищи: " + e.getMessage(), e);
            }
        });


        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        defaultPrefs.edit().putString("fitness_goal", currentFitnessGoal).apply();


        caloriesNormLiveData.postValue(dailyNorms.get("calories"));
        proteinsNormLiveData.postValue(dailyNorms.get("proteins"));
        fatsNormLiveData.postValue(dailyNorms.get("fats"));
        carbsNormLiveData.postValue(dailyNorms.get("carbs"));


        EventBus.getDefault().post(new NutrientsNormsUpdatedEvent(dailyNorms));
    }

    public void searchProductWithGigaChat(String productName, GigaChatProductService.ProductListener listener) {
        
        gigaChatProductService.searchProduct(productName, new GigaChatProductService.ProductListener() {
            @Override
            public void onProductFound(Food food) {
                
                listener.onProductFound(food);
            }

            @Override
            public void onProductNotFound() {
                
                listener.onProductNotFound();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Ошибка при поиске продукта через GigaChat: " + message);
                listener.onError(message);
            }
        });
    }

    public List<Food> getPopularFoods() {
        if (foods == null) {
            loadFoods();
        }
        

        return foods.stream()
            .sorted((food1, food2) -> {

                Integer p1 = food1.getPopularity();
                Integer p2 = food2.getPopularity();
                

                if (p1 == null && p2 == null) {
                    return 0;
                }
                

                if (p1 == null) {
                    return 1;
                }
                

                if (p2 == null) {
                    return -1;
                }
                

                return Integer.compare(p2, p1);
            })
            .limit(10)
            .collect(Collectors.toList());
    }


    public Food findFoodByExactName(String name) {


        if (foods == null || foods.isEmpty()) {
            
            return null;
        }
        
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        String normalizedName = name.trim().toLowerCase();
        
        
        for (Food food : foods) {
            if (food.getName() != null && food.getName().trim().toLowerCase().equals(normalizedName)) {
                
                return food;
            }
        }
        
        
        return null;
    }


    public void findFoodByExactNameAsync(String name, FoodSearchCallback callback) {
        if (name == null || name.isEmpty()) {
            callback.onFoodSearchResult(null);
            return;
        }
        
        executor.execute(() -> {

            if (foods == null || foods.isEmpty()) {
                loadFoods();
            }
            
            if (foods == null || foods.isEmpty()) {

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFoodSearchResult(null));
                }
                return;
            }
            
            String normalizedName = name.trim().toLowerCase();
            
            
            Food foundFood = null;
            for (Food food : foods) {
                if (food.getName() != null && food.getName().trim().toLowerCase().equals(normalizedName)) {
                    
                    foundFood = food;
                    break;
                }
            }
            
            if (foundFood == null) {
                
            }
            

            final Food result = foundFood;
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFoodSearchResult(result));
            }
        });
    }
    

    public interface FoodSearchCallback {
        void onFoodSearchResult(Food food);
    }


    public void loadFoodsAsync(Runnable onComplete) {
        executor.execute(() -> {
            loadFoods();
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    private void loadFoods() {
        foods = foodRepository.getAllFoods();
    }


    public static void resetInstance() {
        if (instance != null) {
            instance.executor.shutdown();
            instance = null;

            MealsDatabase.resetInstance();
            
        }
    }


    public SupabaseBarcodeRepository getBarcodeRepository() {
        return barcodeRepository;
    }
    

    public SupabaseFoodRepository getFoodRepository() {
        return foodRepository;
    }


    public Map<String, Float> getConsumedNutrients(String date) {
        Map<String, Float> nutrients = new HashMap<>();
        

        initializeNutrientsMap(nutrients);
        

        Meal breakfast = getMealForDate("breakfast", date);
        Meal lunch = getMealForDate("lunch", date);
        Meal dinner = getMealForDate("dinner", date);
        Meal snack = getMealForDate("snack", date);
        

        addNutrientsFromMeal(nutrients, breakfast);
        addNutrientsFromMeal(nutrients, lunch);
        addNutrientsFromMeal(nutrients, dinner);
        addNutrientsFromMeal(nutrients, snack);
        
        return nutrients;
    }
    

    public Map<String, Float> getNutrientNorms() {
        Map<String, Float> norms = new HashMap<>();
        

        norms.put("vitamin_a", 900f);
        norms.put("vitamin_b1", 1.2f);
        norms.put("vitamin_b2", 1.3f);
        norms.put("vitamin_b3", 16f);
        norms.put("vitamin_b5", 5f);
        norms.put("vitamin_b6", 1.7f);
        norms.put("vitamin_b9", 400f);
        norms.put("vitamin_b12", 2.4f);
        norms.put("vitamin_c", 90f);
        norms.put("vitamin_d", 15f);
        norms.put("vitamin_e", 15f);
        norms.put("vitamin_k", 120f);
        

        norms.put("calcium", 1000f);
        norms.put("iron", 18f);
        norms.put("magnesium", 400f);
        norms.put("phosphorus", 700f);
        norms.put("potassium", 4700f);
        norms.put("sodium", 2300f);
        norms.put("zinc", 11f);
        

        norms.put("fiber", 25f);
        norms.put("sugar", 50f);
        norms.put("cholesterol", 300f);
        norms.put("saturated_fats", 20f);
        norms.put("trans_fats", 2f);
        
        return norms;
    }
    

    private void initializeNutrientsMap(Map<String, Float> nutrients) {

        nutrients.put("vitamin_a", 0f);
        nutrients.put("vitamin_b1", 0f);
        nutrients.put("vitamin_b2", 0f);
        nutrients.put("vitamin_b3", 0f);
        nutrients.put("vitamin_b5", 0f);
        nutrients.put("vitamin_b6", 0f);
        nutrients.put("vitamin_b9", 0f);
        nutrients.put("vitamin_b12", 0f);
        nutrients.put("vitamin_c", 0f);
        nutrients.put("vitamin_d", 0f);
        nutrients.put("vitamin_e", 0f);
        nutrients.put("vitamin_k", 0f);
        

        nutrients.put("calcium", 0f);
        nutrients.put("iron", 0f);
        nutrients.put("magnesium", 0f);
        nutrients.put("phosphorus", 0f);
        nutrients.put("potassium", 0f);
        nutrients.put("sodium", 0f);
        nutrients.put("zinc", 0f);
        

        nutrients.put("fiber", 0f);
        nutrients.put("sugar", 0f);
        nutrients.put("cholesterol", 0f);
        nutrients.put("saturated_fats", 0f);
        nutrients.put("trans_fats", 0f);
    }
    

    private void addNutrientsFromMeal(Map<String, Float> nutrients, Meal meal) {
        if (meal == null || meal.getFoods().isEmpty()) return;
        
        for (Meal.FoodPortion portion : meal.getFoods()) {
            float multiplier = portion.getPortionSize() / 100f;
            

            nutrients.put("vitamin_a", nutrients.get("vitamin_a") + portion.getFood().getVitaminA() * multiplier);
            nutrients.put("vitamin_b1", nutrients.get("vitamin_b1") + portion.getFood().getVitaminB1() * multiplier);
            nutrients.put("vitamin_b2", nutrients.get("vitamin_b2") + portion.getFood().getVitaminB2() * multiplier);
            nutrients.put("vitamin_b3", nutrients.get("vitamin_b3") + portion.getFood().getVitaminB3() * multiplier);
            nutrients.put("vitamin_b5", nutrients.get("vitamin_b5") + portion.getFood().getVitaminB5() * multiplier);
            nutrients.put("vitamin_b6", nutrients.get("vitamin_b6") + portion.getFood().getVitaminB6() * multiplier);
            nutrients.put("vitamin_b9", nutrients.get("vitamin_b9") + portion.getFood().getVitaminB9() * multiplier);
            nutrients.put("vitamin_b12", nutrients.get("vitamin_b12") + portion.getFood().getVitaminB12() * multiplier);
            nutrients.put("vitamin_c", nutrients.get("vitamin_c") + portion.getFood().getVitaminC() * multiplier);
            nutrients.put("vitamin_d", nutrients.get("vitamin_d") + portion.getFood().getVitaminD() * multiplier);
            nutrients.put("vitamin_e", nutrients.get("vitamin_e") + portion.getFood().getVitaminE() * multiplier);
            nutrients.put("vitamin_k", nutrients.get("vitamin_k") + portion.getFood().getVitaminK() * multiplier);
            

            nutrients.put("calcium", nutrients.get("calcium") + portion.getFood().getCalcium() * multiplier);
            nutrients.put("iron", nutrients.get("iron") + portion.getFood().getIron() * multiplier);
            nutrients.put("magnesium", nutrients.get("magnesium") + portion.getFood().getMagnesium() * multiplier);
            nutrients.put("phosphorus", nutrients.get("phosphorus") + portion.getFood().getPhosphorus() * multiplier);
            nutrients.put("potassium", nutrients.get("potassium") + portion.getFood().getPotassium() * multiplier);
            nutrients.put("sodium", nutrients.get("sodium") + portion.getFood().getSodium() * multiplier);
            nutrients.put("zinc", nutrients.get("zinc") + portion.getFood().getZinc() * multiplier);
            

            nutrients.put("fiber", nutrients.get("fiber") + portion.getFood().getFiber() * multiplier);
            nutrients.put("sugar", nutrients.get("sugar") + portion.getFood().getSugar() * multiplier);
            nutrients.put("cholesterol", nutrients.get("cholesterol") + portion.getFood().getCholesterol() * multiplier);
            nutrients.put("saturated_fats", nutrients.get("saturated_fats") + portion.getFood().getSaturatedFats() * multiplier);
            nutrients.put("trans_fats", nutrients.get("trans_fats") + portion.getFood().getTransFats() * multiplier);
        }
    }
    

    public Meal getMealForDate(String mealType, String date) {
        try {

            Meal currentMeal = getMeal(mealType);
            

            if (date.equals(getSelectedDateFormatted())) {
                return currentMeal;
            }
            

            DayMeal dayMeal = database.mealDao().getMealForDateAndType(date, mealType, userId);
            

            if (dayMeal != null && dayMeal.mealData != null && !dayMeal.mealData.isEmpty()) {
                try {
                    Meal meal = gson.fromJson(dayMeal.mealData, Meal.class);
                    if (meal != null) {
                        
                        return meal;
                    }
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "Ошибка при десериализации приема пищи: " + e.getMessage());
                }
            }
            

            return new Meal(getMealTitle(mealType), getMealIcon(mealType));
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении приема пищи для даты: " + e.getMessage());
        }
        

        return new Meal(getMealTitle(mealType), getMealIcon(mealType));
    }
    

} 