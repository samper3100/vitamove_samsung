package com.martist.vitamove.workout.data.repositories;

import android.content.Context;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.WorkoutTemplate;
import io.reactivex.rxjava3.core.Completable;
import org.json.JSONArray;
import org.json.JSONObject;

public class WorkoutTemplateRepository {
    private static final String TAG = "WorkoutTemplateRepo";
    private final SupabaseClient supabaseClient;

    public WorkoutTemplateRepository(Context context) {
        this.supabaseClient = SupabaseClient.getInstance(
            Constants.SUPABASE_CLIENT_ID,
            Constants.SUPABASE_CLIENT_SECRET
        );
    }

    public Completable saveTemplate(WorkoutTemplate template) {
        return Completable.create(emitter -> {
            try {
                JSONObject templateJson = new JSONObject();
                templateJson.put("name", template.getName());
                templateJson.put("user_id", template.getUserId());
                

                JSONArray exercisesArray = new JSONArray();
                for (Exercise exercise : template.getExercises()) {
                    JSONObject exerciseJson = new JSONObject();
                    exerciseJson.put("id", exercise.getId());
                    exerciseJson.put("name", exercise.getName());
                    exerciseJson.put("description", exercise.getDescription());
                    exerciseJson.put("default_sets", exercise.getDefaultSets());
                    exerciseJson.put("default_reps", exercise.getDefaultReps());
                    exerciseJson.put("default_rest_seconds", exercise.getDefaultRestSeconds());
                    exerciseJson.put("met", exercise.getMet());
                    exercisesArray.put(exerciseJson);
                }
                templateJson.put("exercises", exercisesArray);
                
                templateJson.put("notes", template.getNotes());

                supabaseClient.from("workout_templates")
                    .insert(templateJson)
                    .execute();

                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
} 