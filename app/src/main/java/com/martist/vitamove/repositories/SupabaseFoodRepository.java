package com.martist.vitamove.repositories;

import android.content.Context;
import android.util.Log;

import com.martist.vitamove.models.Food;
import com.martist.vitamove.utils.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class SupabaseFoodRepository {
    private static final String TAG = "SupabaseFoodRepo";
    private final SupabaseClient supabaseClient;
    private final Context context;
    
    
    private List<Food> allFoodsCache;
    private long allFoodsCacheTime;
    
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30);

    public SupabaseFoodRepository(SupabaseClient supabaseClient, Context context) {
        this.supabaseClient = supabaseClient;
        this.context = context;
    }

    
    public List<Food> getPopularFoods() {
        try {
            JSONArray foodsArray = null;
            int maxRetries = 3;
            int currentRetry = 0;
            
            
            while (currentRetry < maxRetries && foodsArray == null) {
                try {
                    foodsArray = supabaseClient.from("foods")
                        .select("*")
                        .order("popularity", false) 
                        .limit(20)
                        .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    
                    
                    currentRetry++;
                    
                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос популярных продуктов после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }
            
            if (foodsArray == null) {
                Log.e(TAG, "Не удалось получить популярные продукты после обновления токена");
                return new ArrayList<>();
            }
            
            List<Food> popularFoods = new ArrayList<>();
            
            for (int i = 0; i < foodsArray.length(); i++) {
                JSONObject foodJson = foodsArray.getJSONObject(i);
                Food food = parseFoodFromJson(foodJson);
                
                if (food != null) {
                    popularFoods.add(food);
                }
            }
            
            return popularFoods;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении популярных продуктов: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    
    public List<Food> searchFoodsByQuery(String query) {
        
        if (query == null || query.trim().isEmpty()) {
            return getPopularFoods();
        }
        
        String normalizedQuery = query.trim().toLowerCase();
        
        
        try {
            
            List<Food> allFoods = getAllFoods();
            
            
            List<Food> exactMatches = new ArrayList<>();
            List<Food> partialMatches = new ArrayList<>();
            
            
            String[] queryWords = normalizedQuery.split("\\s+|-");
            
            
            for (Food food : allFoods) {
                String name = food.getName() != null ? food.getName().toLowerCase() : "";
                String category = food.getCategory() != null ? food.getCategory().toLowerCase() : "";
                String subcategory = food.getSubcategory() != null ? food.getSubcategory().toLowerCase() : "";
                
                boolean hasExactMatch = false;
                boolean hasPartialMatch = false;
                
                String matchReason = "";
                
                
                String[] nameWords = name.split("\\s+|-");
                String[] categoryWords = category.split("\\s+|-");
                String[] subcategoryWords = subcategory.split("\\s+|-");
                
                
                if (name.equals(normalizedQuery) || 
                    category.equals(normalizedQuery) || 
                    subcategory.equals(normalizedQuery)) {
                    hasExactMatch = true;
                    matchReason = "абсолютно точное совпадение";
                }
                
                
                if (!hasExactMatch) {
                    for (String qWord : queryWords) {
                        
                        for (String word : nameWords) {
                            if (word.equals(qWord)) {
                                hasExactMatch = true;
                                matchReason = "точное совпадение слова '" + word + "' в названии";
                                break;
                            }
                        }
                        
                        if (hasExactMatch) break;
                        
                        for (String word : categoryWords) {
                            if (word.equals(qWord)) {
                                hasExactMatch = true;
                                matchReason = "точное совпадение слова '" + word + "' в категории";
                                break;
                            }
                        }
                        
                        if (hasExactMatch) break;
                        
                        for (String word : subcategoryWords) {
                            if (word.equals(qWord)) {
                                hasExactMatch = true;
                                matchReason = "точное совпадение слова '" + word + "' в подкатегории";
                                break;
                            }
                        }
                        
                        if (hasExactMatch) break;
                    }
                }
                
                
                if (!hasExactMatch) {
                    for (String qWord : queryWords) {
                        
                        for (String word : nameWords) {
                            if (word.startsWith(qWord) && !word.equals(qWord)) {
                                hasPartialMatch = true;
                                matchReason = "слово '" + word + "' в названии начинается с части запроса '" + qWord + "'";
                                break;
                            }
                        }
                        
                        if (hasPartialMatch) break;
                        
                        for (String word : categoryWords) {
                            if (word.startsWith(qWord) && !word.equals(qWord)) {
                                hasPartialMatch = true;
                                matchReason = "слово '" + word + "' в категории начинается с части запроса '" + qWord + "'";
                                break;
                            }
                        }
                        
                        if (hasPartialMatch) break;
                        
                        for (String word : subcategoryWords) {
                            if (word.startsWith(qWord) && !word.equals(qWord)) {
                                hasPartialMatch = true;
                                matchReason = "слово '" + word + "' в подкатегории начинается с части запроса '" + qWord + "'";
                                break;
                            }
                        }
                        
                        if (hasPartialMatch) break;
                    }
                }
                
                
                if (!hasExactMatch && !hasPartialMatch && queryWords.length > 1) {
                    if (name.contains(normalizedQuery)) {
                        hasPartialMatch = true;
                        matchReason = "составной запрос содержится в названии";
                    }
                }
                
                
                if (hasExactMatch) {
                    exactMatches.add(food);
                    
                } else if (hasPartialMatch) {
                    partialMatches.add(food);
                    
                }
            }
            
            
            List<Food> matchingFoods = new ArrayList<>(exactMatches);
            matchingFoods.addAll(partialMatches);
            
            
            
            
            return matchingFoods;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при поиске продуктов: " + e.getMessage(), e);
            
            return new ArrayList<>();
        }
    }

    
    public List<Food> getAllFoods() {
        
        if (allFoodsCache != null && !isCacheExpired()) {
            
            return allFoodsCache;
        }
        
        try {
            
            
            List<Food> foods = new ArrayList<>();
            int pageSize = 1000; 
            int offset = 0;
            boolean hasMoreData = true;
            
            
            while (hasMoreData) {
                
                
                JSONArray foodsArray = null;
                int maxRetries = 3;
                int currentRetry = 0;
                
                
                while (currentRetry < maxRetries && foodsArray == null) {
                    try {
                        foodsArray = supabaseClient.from("foods")
                            .select("*")
                            .limit(pageSize)
                            .offset(offset)
                            .executeAndGetArray();
                    } catch (SupabaseClient.TokenRefreshedException e) {
                        
                        
                        currentRetry++;
                        
                        if (currentRetry >= maxRetries) {
                            Log.e(TAG, "Не удалось выполнить запрос после " + maxRetries + " попыток");
                            throw e;
                        }
                    }
                }
                
                if (foodsArray == null) {
                    Log.e(TAG, "Не удалось получить данные после обновления токена");
                    break;
                }
                
                
                
                
                for (int i = 0; i < foodsArray.length(); i++) {
                    JSONObject foodJson = foodsArray.getJSONObject(i);
                    Food food = parseFoodFromJson(foodJson);
                    
                    if (food != null) {
                        foods.add(food);
                    }
                }
                
                
                if (foodsArray.length() < pageSize) {
                    hasMoreData = false; 
                } else {
                    
                    offset += pageSize;
                }
            }
            
            
            
            
            allFoodsCache = foods;
            allFoodsCacheTime = System.currentTimeMillis();
            
            return foods;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении всех продуктов: " + e.getMessage(), e);
            
            
            if (allFoodsCache != null) {
                
                return allFoodsCache;
            }
            
            return new ArrayList<>();
        }
    }

    
    private boolean isCacheExpired() {
        return System.currentTimeMillis() - allFoodsCacheTime > CACHE_TTL_MS;
    }

    
    private Food parseFoodFromJson(JSONObject json) {
        try {
            String name = json.optString("name", "Неизвестный продукт");
            
            
            
            float proteins = (float) json.optDouble("proteins", 0);
            float calcium = (float) json.optDouble("calcium", 0);
            float iron = (float) json.optDouble("iron", 0);
            float magnesium = (float) json.optDouble("magnesium", 0);
            float phosphorus = (float) json.optDouble("phosphorus", 0);
            float potassium = (float) json.optDouble("potassium", 0);
            float sodium = (float) json.optDouble("sodium", 0);
            float vitaminA = (float) json.optDouble("vitamin_a", 0);
            float vitaminC = (float) json.optDouble("vitamin_c", 0);
            float saturatedFats = (float) json.optDouble("saturated_fats", 0);
            float transFats = (float) json.optDouble("trans_fats", 0);
            float fiber = (float) json.optDouble("fiber", 0);
            float sugar = (float) json.optDouble("sugar", 0);
            int usefulnessIndex = json.optInt("usefulness_index", 5);
            boolean isLiquid = json.optBoolean("is_liquid", false);
            
            
            if (json.isNull("popularity")) {
                
            }
            
            
            String idStr = null;
            long numericId = 0;
            
            if (!json.isNull("id")) {
                try {
                    idStr = json.getString("id");
                    
                    numericId = Long.parseLong(idStr);
                } catch (NumberFormatException e) {
                    
                    
                    numericId = Math.abs((long)idStr.hashCode());
                    
                }
            }
            
            Food food = new Food.Builder()
                .id(numericId)
                .idUUID(idStr)  
                .name(json.optString("name", "Неизвестный продукт"))
                .category(json.optString("category", "Другое"))
                .subcategory(json.optString("subcategory", ""))
                .calories(json.optInt("calories", 0))
                .proteins(proteins)
                .fats((float) json.optDouble("fats", 0))
                .carbs((float) json.optDouble("carbs", 0))
                .popularity(json.optInt("popularity", 0))
                .calcium(calcium)
                .iron(iron)
                .magnesium(magnesium)
                .phosphorus(phosphorus)
                .potassium(potassium)
                .sodium(sodium)
                .zinc((float) json.optDouble("zinc", 0))
                .vitaminA(vitaminA)
                .vitaminB1((float) json.optDouble("vitamin_b1", 0))
                .vitaminB2((float) json.optDouble("vitamin_b2", 0))
                .vitaminB3((float) json.optDouble("vitamin_b3", 0))
                .vitaminB5((float) json.optDouble("vitamin_b5", 0))
                .vitaminB6((float) json.optDouble("vitamin_b6", 0))
                .vitaminB9((float) json.optDouble("vitamin_b9", 0))
                .vitaminB12((float) json.optDouble("vitamin_b12", 0))
                .vitaminC(vitaminC)
                .vitaminD((float) json.optDouble("vitamin_d", 0))
                .vitaminE((float) json.optDouble("vitamin_e", 0))
                .vitaminK((float) json.optDouble("vitamin_k", 0))
                .cholesterol((float) json.optDouble("cholesterol", 0))
                .saturatedFats(saturatedFats)
                .transFats(transFats)
                .fiber(fiber)
                .sugar(sugar)
                .usefulness_index(usefulnessIndex)
                .is_liquid(isLiquid)
                .build();
                
            
                      
            return food;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании Food из JSON: " + e.getMessage(), e);
            return null;
        }
    }

    
    public String addFood(Food food) {
        try {
            
            
            
            String foodName = food.getName();
            if (foodName == null || foodName.trim().isEmpty()) {
                Log.e(TAG, "Имя продукта пустое или null");
                return null;
            }
            
            
            Food existingFood = getFoodByName(foodName);
            if (existingFood != null) {
                
                String existingId = String.valueOf(existingFood.getId());
                
                return existingId;
            }
            
            
            
            
            String newUUID = java.util.UUID.randomUUID().toString();
            
            
            
            JSONObject foodData = new JSONObject();
            
            
            
            
            foodData.put("id", newUUID);
            foodData.put("name", foodName);
            
            
            if (food.getCategory() != null && !food.getCategory().isEmpty()) {
                foodData.put("category", food.getCategory());
            } else {
                foodData.put("category", "Другое"); 
            }
            
            if (food.getSubcategory() != null && !food.getSubcategory().isEmpty()) {
                foodData.put("subcategory", food.getSubcategory());
            } else {
                foodData.put("subcategory", "Другое"); 
            }
            
            
            if (food.getCalories() > 0) {
                foodData.put("calories", food.getCalories());
            } else {
                foodData.put("calories", 0); 
            }
            
            
            addFloatIfValid(foodData, "proteins", food.getProteins());
            addFloatIfValid(foodData, "fats", food.getFats());
            addFloatIfValid(foodData, "carbs", food.getCarbs());
            
            if (food.getPopularity() > 0) {
                foodData.put("popularity", food.getPopularity());
            }
            
            addFloatIfValid(foodData, "calcium", food.getCalcium());
            addFloatIfValid(foodData, "iron", food.getIron());
            addFloatIfValid(foodData, "magnesium", food.getMagnesium());
            addFloatIfValid(foodData, "phosphorus", food.getPhosphorus());
            addFloatIfValid(foodData, "potassium", food.getPotassium());
            addFloatIfValid(foodData, "sodium", food.getSodium());
            addFloatIfValid(foodData, "zinc", food.getZinc());
            addFloatIfValid(foodData, "vitamin_a", food.getVitaminA());
            addFloatIfValid(foodData, "vitamin_b1", food.getVitaminB1());
            addFloatIfValid(foodData, "vitamin_b2", food.getVitaminB2());
            addFloatIfValid(foodData, "vitamin_b3", food.getVitaminB3());
            addFloatIfValid(foodData, "vitamin_b5", food.getVitaminB5());
            addFloatIfValid(foodData, "vitamin_b6", food.getVitaminB6());
            addFloatIfValid(foodData, "vitamin_b9", food.getVitaminB9());
            addFloatIfValid(foodData, "vitamin_b12", food.getVitaminB12());
            addFloatIfValid(foodData, "vitamin_c", food.getVitaminC());
            addFloatIfValid(foodData, "vitamin_d", food.getVitaminD());
            addFloatIfValid(foodData, "vitamin_e", food.getVitaminE());
            addFloatIfValid(foodData, "vitamin_k", food.getVitaminK());
            addFloatIfValid(foodData, "cholesterol", food.getCholesterol());
            addFloatIfValid(foodData, "saturated_fats", food.getSaturatedFats());
            addFloatIfValid(foodData, "trans_fats", food.getTransFats());
            addFloatIfValid(foodData, "fiber", food.getFiber());
            addFloatIfValid(foodData, "sugar", food.getSugar());
            
            if (food.getUsefulnessIndex() > 0) {
                foodData.put("usefulness_index", food.getUsefulnessIndex());
            }
            
            
            foodData.put("is_liquid", food.isLiquid());
            
            
            foodData.put("is_moderated", false); 
            
            
            
            

            
            try {
                JSONArray result = supabaseClient.from("foods")
                        .insert(foodData)
                        .executeAndGetArray();
                
                if (result != null && result.length() > 0) {
                    
                    
                    
                    allFoodsCache = null;
                } else {
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении продукта в Supabase: " + e.getMessage(), e);
                
            }

            return newUUID;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при добавлении продукта: " + e.getMessage(), e);
            return null;
        }
    }
    
    
    private void addFloatIfValid(JSONObject json, String key, float value) {
        try {
            if (!Float.isNaN(value) && value > 0) {
                json.put(key, value);
            } else if (Float.isNaN(value)) {
                
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при добавлении значения для ключа '" + key + "': " + e.getMessage(), e);
        }
    }

    
    public Food getFoodByName(String foodName) {
        try {
            
            
            if (foodName == null || foodName.trim().isEmpty()) {
                Log.e(TAG, "Пустое имя продукта в запросе");
                return null;
            }
            
            
            String normalizedFoodName = foodName.trim();
            
            JSONArray foodsArray = null;
            int maxRetries = 3;
            int currentRetry = 0;
            
            
            while (currentRetry < maxRetries && foodsArray == null) {
                try {
                    foodsArray = supabaseClient.from("foods")
                        .select("*")
                        .eq("name", normalizedFoodName)  
                        .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    
                    currentRetry++;
                    
                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос поиска по имени после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }
            
            if (foodsArray == null) {
                Log.e(TAG, "Не удалось получить результаты поиска по имени после обновления токена");
                return null;
            }
            
            
            if (foodsArray.length() > 0) {
                JSONObject foodJson = foodsArray.getJSONObject(0);
                Food food = parseFoodFromJson(foodJson);
                
                return food;
            } else {
                
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при поиске продукта по имени: " + e.getMessage(), e);
            return null;
        }
    }

    
    public List<String> getAllUniqueCategories() {
        try {
            
            
            
            List<Food> foods = getAllFoods();
            
            
            Map<String, String> uniqueCategories = new HashMap<>();
            
            for (Food food : foods) {
                String category = food.getCategory();
                if (category != null && !category.isEmpty()) {
                    
                    String lowerCaseKey = category.toLowerCase();
                    
                    uniqueCategories.put(lowerCaseKey, category);
                }
            }
            
            
            List<String> categories = new ArrayList<>(uniqueCategories.values());
            
            
            return categories;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении уникальных категорий: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    
    public List<String> getUniqueSubcategoriesForCategory(String category) {
        try {
            
            
            
            List<Food> foods = getAllFoods();
            
            
            Map<String, String> uniqueSubcategories = new HashMap<>();
            
            for (Food food : foods) {
                String foodCategory = food.getCategory();
                String subcategory = food.getSubcategory();
                
                
                if (foodCategory != null && subcategory != null && !subcategory.isEmpty() && 
                    foodCategory.equalsIgnoreCase(category)) {
                    
                    
                    String lowerCaseKey = subcategory.toLowerCase();
                    
                    uniqueSubcategories.put(lowerCaseKey, subcategory);
                }
            }
            
            
            List<String> subcategories = new ArrayList<>(uniqueSubcategories.values());
            
            
            return subcategories;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении уникальных подкатегорий: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
} 