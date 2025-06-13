package com.martist.vitamove.workout.data.models;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import com.martist.vitamove.utils.SupabaseClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MuscleGroupConverter {
    private static final String TAG = "MuscleGroupConverter";
    private static MuscleGroupConverter instance;
    private static final Map<String, String> muscleGroupMap = new HashMap<>();
    


    static {

        muscleGroupMap.put("грудь", "chest");
        muscleGroupMap.put("спина", "back");
        muscleGroupMap.put("плечи", "shoulders");
        muscleGroupMap.put("бицепс", "biceps");
        muscleGroupMap.put("трицепс", "triceps");
        muscleGroupMap.put("ноги", "legs");
        muscleGroupMap.put("икры", "calves");
        muscleGroupMap.put("пресс", "abs");
        muscleGroupMap.put("предплечья", "forearms");
        muscleGroupMap.put("широчайшие мышцы", "lats");
        muscleGroupMap.put("трапеции", "traps");
        muscleGroupMap.put("ягодицы", "glutes");

    }
    

    public static synchronized MuscleGroupConverter getInstance() {
        if (instance == null) {
            instance = new MuscleGroupConverter();
        }
        return instance;
    }
    

    public String getNameByRussianName(String russianName) {
        return muscleGroupMap.getOrDefault(russianName.toLowerCase(), russianName.toLowerCase().replace(' ', '_'));
    }
    

    public String getRussianNameByName(String name) {
        for (Map.Entry<String, String> entry : muscleGroupMap.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }

        return name.replace('_', ' ');
    }
    

    public List<String> getAllMuscleGroups(SupabaseClient supabaseClient) {
        List<String> muscleGroups = new ArrayList<>();
        try {

            JSONArray result = supabaseClient.rpc("get_distinct_muscle_groups")
                .executeAndGetArray();
            
            for (int i = 0; i < result.length(); i++) {
                String muscleName = result.getJSONObject(i).getString("muscle_name");
                muscleGroups.add(muscleName);
                

                if (!muscleGroupMap.containsKey(muscleName.toLowerCase())) {
                    String englishName = muscleName.toLowerCase().replace(' ', '_');
                    muscleGroupMap.put(muscleName.toLowerCase(), englishName);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при получении групп мышц: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Непредвиденная ошибка: " + e.getMessage());
        }
        return muscleGroups;
    }
    

    public List<String> convertMuscleGroupNamesToEngish(List<String> muscleGroupsFromDb) {
        List<String> muscleGroupNames = new ArrayList<>();
        for (String muscleGroup : muscleGroupsFromDb) {
            muscleGroupNames.add(getNameByRussianName(muscleGroup));
        }
        return muscleGroupNames;
    }
    

    public boolean isMuscleGroupValid(String muscleGroupName) {
        return muscleGroupMap.containsValue(muscleGroupName);
    }
} 