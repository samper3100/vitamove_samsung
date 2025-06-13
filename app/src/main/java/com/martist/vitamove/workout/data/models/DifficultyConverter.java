package com.martist.vitamove.workout.data.models;

import android.util.Log;

import com.martist.vitamove.utils.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DifficultyConverter {
    private static final String TAG = "DifficultyConverter";
    private static DifficultyConverter instance;
    private static final Map<String, String> difficultyMap = new HashMap<>();
    

    static {
        difficultyMap.put("легкое", "beginner");
        difficultyMap.put("среднее", "intermediate");
        difficultyMap.put("сложное", "advanced");
        difficultyMap.put("очень сложное", "expert");
    }
    

    public static synchronized DifficultyConverter getInstance() {
        if (instance == null) {
            instance = new DifficultyConverter();
        }
        return instance;
    }

    

    public String getRussianNameByName(String name) {
        for (Map.Entry<String, String> entry : difficultyMap.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }
        return "легкое";
    }
    

    public List<String> getAllDifficulties(SupabaseClient supabaseClient) {
        List<String> difficulties = new ArrayList<>();
        try {

            JSONArray result = supabaseClient.rpc("get_distinct_difficulties")
                    .executeAndGetArray();
            
            for (int i = 0; i < result.length(); i++) {
                String difficulty = result.getJSONObject(i).getString("difficulty");
                difficulties.add(difficulty);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при получении уровней сложности: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Непредвиденная ошибка: " + e.getMessage());
        }
        return difficulties;
    }


} 