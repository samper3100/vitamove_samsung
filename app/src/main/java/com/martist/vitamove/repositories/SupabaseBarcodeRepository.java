package com.martist.vitamove.repositories;

import android.content.Context;
import android.util.Log;

import com.martist.vitamove.models.Food;
import com.martist.vitamove.utils.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;


public class SupabaseBarcodeRepository {
    private static final String TAG = "SupabaseBarcodeRepo";
    private final SupabaseClient supabaseClient;
    private final Context context;

    public SupabaseBarcodeRepository(SupabaseClient supabaseClient, Context context) {
        this.supabaseClient = supabaseClient;
        this.context = context;
    }

    
    public Food findFoodByBarcode(String barcode) {
        try {
            
            
            
            JSONArray results = null;
            int maxRetries = 3;
            int currentRetry = 0;
            
            
            while (currentRetry < maxRetries && results == null) {
                try {
                    results = supabaseClient.rpc("get_food_by_barcode")
                            .param("barcode_param", barcode)
                            .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    
                    
                    currentRetry++;
                    
                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос по штрихкоду после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }
            
            if (results == null) {
                Log.e(TAG, "Не удалось получить результаты поиска по штрихкоду после обновления токена");
                return null;
            }
            
            
            if (results.length() > 0) {
                JSONObject foodJson = results.getJSONObject(0);
                Food food = parseFoodFromJson(foodJson);
                
                return food;
            } else {
                
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при поиске продукта по штрихкоду: " + e.getMessage(), e);
            return null;
        }
    }
    
    
    public boolean addBarcode(String barcode, Food food) {
        try {
            if (barcode == null || barcode.trim().isEmpty()) {
                Log.e(TAG, "Ошибка: пустой штрихкод");
                return false;
            }
            
            if (food == null) {
                Log.e(TAG, "Ошибка: продукт равен null");
                return false;
            }
            
            String foodId = food.getIdUUID();
            if (foodId == null || foodId.trim().isEmpty()) {
                Log.e(TAG, "Ошибка: ID продукта пустой или равен null. Название продукта: " + food.getName());
                return false;
            }
            
            
            
            
            if (existsBarcode(barcode)) {
                
                return false;
            }
            
            
            JSONObject data = new JSONObject();
            data.put("barcode", barcode);
            data.put("food_id", foodId);
            
            JSONArray result = supabaseClient.from("barcodes")
                .insert(data)
                .executeAndGetArray();
            
            boolean success = result != null && result.length() > 0;
            if (success) {
                
            } else {
                Log.e(TAG, "Не удалось добавить штрихкод: " + barcode);
            }
            
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при добавлении штрихкода: " + e.getMessage(), e);
            return false;
        }
    }
    
    
    private boolean isUUID(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    
    private String generateUuidFromFoodId(String foodId) {
        try {
            
            
            
            if (isUUID(foodId)) {
                
                return foodId;
            }
            
            
            try {
                long numericId = Long.parseLong(foodId);
                
                
                String seedBaseName = "vitamove.food.";
                String seedName = seedBaseName + numericId;
                
                
                UUID uuid = UUID.nameUUIDFromBytes(seedName.getBytes());
                String uuidString = uuid.toString();
                
                
                return uuidString;
                
            } catch (NumberFormatException e) {
                
                
                
                
                String seedName = "vitamove.food.string." + foodId;
                
                
                UUID uuid = UUID.nameUUIDFromBytes(seedName.getBytes());
                String uuidString = uuid.toString();
                
                
                return uuidString;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при генерации UUID из foodId: " + e.getMessage(), e);
            
            String randomUuid = UUID.randomUUID().toString();
            
            return randomUuid;
        }
    }
    
    
    private boolean checkFoodExists(String uuid) {
        try {
            
            
            JSONArray foodsArray = null;
            int maxRetries = 3;
            int currentRetry = 0;
            
            
            while (currentRetry < maxRetries && foodsArray == null) {
                try {
                    foodsArray = supabaseClient.from("foods")
                        .select("id")
                        .eq("id", uuid)
                        .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    
                    currentRetry++;
                    
                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос проверки продукта после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }
            
            if (foodsArray == null) {
                Log.e(TAG, "Не удалось получить результаты проверки продукта после обновления токена");
                return false;
            }
            
            boolean exists = foodsArray.length() > 0;
            
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования продукта: " + e.getMessage(), e);
            return false;
        }
    }
    
    
    private Food parseFoodFromJson(JSONObject json) {
        try {
            Food.Builder builder = new Food.Builder();
            
            if (json.has("id")) {
                String idStr = json.getString("id");
                try {
                    
                    
                    if (!isUUID(idStr)) {
                        builder.id(Long.parseLong(idStr));
                    } else {
                        
                        
                        builder.idUUID(idStr);
                        
                    }
                } catch (NumberFormatException e) {
                    
                    builder.idUUID(idStr);
                    
                }
            }
            
            if (json.has("name")) builder.name(json.getString("name"));
            if (json.has("category")) builder.category(json.getString("category"));
            if (json.has("subcategory")) builder.subcategory(json.getString("subcategory"));
            if (json.has("calories")) builder.calories(json.optInt("calories", 0));
            if (json.has("proteins")) builder.proteins((float) json.optDouble("proteins", 0));
            if (json.has("fats")) builder.fats((float) json.optDouble("fats", 0));
            if (json.has("carbs")) builder.carbs((float) json.optDouble("carbs", 0));
            if (json.has("fiber")) builder.fiber((float) json.optDouble("fiber", 0));
            if (json.has("sugar")) builder.sugar((float) json.optDouble("sugar", 0));
            if (json.has("saturated_fats")) builder.saturatedFats((float) json.optDouble("saturated_fats", 0));
            if (json.has("trans_fats")) builder.transFats((float) json.optDouble("trans_fats", 0));
            if (json.has("cholesterol")) builder.cholesterol((float) json.optDouble("cholesterol", 0));
            if (json.has("sodium")) builder.sodium((float) json.optDouble("sodium", 0));
            if (json.has("calcium")) builder.calcium((float) json.optDouble("calcium", 0));
            if (json.has("iron")) builder.iron((float) json.optDouble("iron", 0));
            if (json.has("magnesium")) builder.magnesium((float) json.optDouble("magnesium", 0));
            if (json.has("phosphorus")) builder.phosphorus((float) json.optDouble("phosphorus", 0));
            if (json.has("potassium")) builder.potassium((float) json.optDouble("potassium", 0));
            if (json.has("zinc")) builder.zinc((float) json.optDouble("zinc", 0));
            if (json.has("vitamin_a")) builder.vitaminA((float) json.optDouble("vitamin_a", 0));
            if (json.has("vitamin_c")) builder.vitaminC((float) json.optDouble("vitamin_c", 0));
            if (json.has("vitamin_d")) builder.vitaminD((float) json.optDouble("vitamin_d", 0));
            if (json.has("vitamin_e")) builder.vitaminE((float) json.optDouble("vitamin_e", 0));
            if (json.has("vitamin_k")) builder.vitaminK((float) json.optDouble("vitamin_k", 0));
            if (json.has("vitamin_b1")) builder.vitaminB1((float) json.optDouble("vitamin_b1", 0));
            if (json.has("vitamin_b2")) builder.vitaminB2((float) json.optDouble("vitamin_b2", 0));
            if (json.has("vitamin_b3")) builder.vitaminB3((float) json.optDouble("vitamin_b3", 0));
            if (json.has("vitamin_b5")) builder.vitaminB5((float) json.optDouble("vitamin_b5", 0));
            if (json.has("vitamin_b6")) builder.vitaminB6((float) json.optDouble("vitamin_b6", 0));
            if (json.has("vitamin_b9")) builder.vitaminB9((float) json.optDouble("vitamin_b9", 0));
            if (json.has("vitamin_b12")) builder.vitaminB12((float) json.optDouble("vitamin_b12", 0));
            if (json.has("popularity")) builder.popularity(json.optInt("popularity", 0));
            if (json.has("usefulness_index")) builder.usefulness_index(json.optInt("usefulness_index", 0));
            
            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при парсинге JSON в Food: " + e.getMessage(), e);
            return null;
        }
    }

    
    public boolean existsBarcode(String barcode) {
        try {
            
            
            if (barcode == null || barcode.trim().isEmpty()) {
                Log.e(TAG, "Штрихкод пустой");
                return false;
            }

            
            JSONArray barcodeArray = null;
            int maxRetries = 3;
            int currentRetry = 0;
            
            
            while (currentRetry < maxRetries && barcodeArray == null) {
                try {
                    barcodeArray = supabaseClient.from("barcodes")
                        .select("barcode")
                        .eq("barcode", barcode)
                        .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    
                    currentRetry++;
                    
                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос проверки штрихкода после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }
            
            if (barcodeArray == null) {
                Log.e(TAG, "Не удалось получить результаты проверки штрихкода после обновления токена");
                return false;
            }
            
            
            boolean exists = barcodeArray.length() > 0;
            
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования штрихкода: " + e.getMessage(), e);
            return false;
        }
    }
} 