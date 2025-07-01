package com.martist.vitamove.workout.data.cache;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.utils.AsyncCallback;
import com.martist.vitamove.workout.data.models.Program;
import com.martist.vitamove.workout.data.models.ProgramDay;
import com.martist.vitamove.workout.data.models.ProgramExercise;
import com.martist.vitamove.workout.data.models.WorkoutPlan;
import com.martist.vitamove.workout.data.models.cache.WorkoutPlanEntity;
import com.martist.vitamove.workout.data.models.room.ProgramDayEntity;
import com.martist.vitamove.workout.data.models.room.ProgramEntity;
import com.martist.vitamove.workout.data.models.room.ProgramExerciseEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class ProgramRoomCache {
    
    private static final String TAG = "ProgramRoomCache";
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private static final Gson gson = new Gson();
    
    
    public static void saveProgram(String programId, JSONObject programJson) {
        try {
            Context context = VitaMoveApplication.getContext();
            if (context == null) {
                Log.e(TAG, "saveProgram: Контекст приложения недоступен");
                return;
            }
            
            
            inspectProgramJson(programJson);
            
            executor.execute(() -> {
                try {
                    ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                    
                    
                    ProgramEntity programEntity = jsonToProgramEntity(programJson);
                    if (programEntity != null) {
                        db.programDao().insert(programEntity);

                    }
                    
                    
                    if (programJson.has("days")) {
                        JSONArray daysArray = programJson.getJSONArray("days");

                        List<ProgramDayEntity> dayEntities = jsonToDayEntities(daysArray);
                        
                        if (!dayEntities.isEmpty()) {
                            
                            db.programDayDao().replaceDaysForProgram(programId, dayEntities);

                            
                            
                            int totalExercisesSaved = 0;
                            for (int i = 0; i < daysArray.length(); i++) {
                                JSONObject dayJson = daysArray.getJSONObject(i);
                                String currentDayId = dayJson.getString("id");
                                
                                if (dayJson.has("exercises")) {
                                    JSONArray exercisesArray = dayJson.getJSONArray("exercises");

                                    
                                    List<ProgramExerciseEntity> exerciseEntities = jsonToExerciseEntities(exercisesArray);

                                    
                                    if (!exerciseEntities.isEmpty()) {
                                        
                                        boolean allMatch = true;
                                        for(ProgramExerciseEntity ex : exerciseEntities) {
                                            if (!ex.getDayId().equals(currentDayId)) {

                                                allMatch = false;
                                                
                                                
                                            }
                                        }
                                        
                                        if (allMatch) {
                                            try {
                                                db.programExerciseDao().replaceExercisesForDay(currentDayId, exerciseEntities);

                                                totalExercisesSaved += exerciseEntities.size();
                                            } catch (Exception e) {
                                                Log.e(TAG, "saveProgram: Ошибка при сохранении упражнений для дня " + currentDayId, e);
                                            }
                                        } else {
                                            Log.e(TAG, "saveProgram: Пропуск сохранения упражнений для дня " + currentDayId + " из-за несоответствия day_id.");
                                        }
                                    } else {
                                        

                                        
                                        db.programExerciseDao().deleteAllByDayId(currentDayId);
                                    }
                                } else {

                                    db.programExerciseDao().deleteAllByDayId(currentDayId);
                                }
                            }

                        } else {

                        }
                    } else {

                    }
                    
                    
                    
                    

                } catch (Exception e) {
                    Log.e(TAG, "saveProgram: Ошибка сохранения программы в Room", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "saveProgram: Ошибка инициации сохранения программы", e);
        }
    }
    
    
    private static void inspectProgramJson(JSONObject json) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Структура JSON программы:\n");
            sb.append("ID: ").append(json.optString("id", "не указан")).append("\n");
            sb.append("Имя: ").append(json.optString("name", "не указано")).append("\n");
            
            
            String[] keyFields = {"id", "name", "description", "days", "exercises"};
            for (String field : keyFields) {
                sb.append(field).append(": ").append(json.has(field) ? "присутствует" : "отсутствует").append("\n");
            }
            
            
            if (json.has("periodization_type")) {
                try {
                    Object typeValue = json.opt("periodization_type");
                    sb.append("periodization_type: присутствует, тип: ")
                      .append(typeValue != null ? typeValue.getClass().getName() : "null")
                      .append(", значение: ").append(typeValue).append("\n");
                    
                    
                    if (typeValue != null && !(typeValue instanceof String)) {
                        sb.append("⚠️ periodization_type не является строкой, будет преобразовано в: ")
                          .append(typeValue).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("⚠️ Ошибка при анализе periodization_type: ").append(e.getMessage()).append("\n");
                }
            } else {
                sb.append("periodization_type: отсутствует\n");
            }
            
            
            if (json.has("current_phase")) {
                try {
                    Object value = json.opt("current_phase");
                    sb.append("current_phase: присутствует, тип: ")
                      .append(value != null ? value.getClass().getName() : "null")
                      .append(", значение: ").append(value).append("\n");
                    
                    
                    if (value != null && !(value instanceof String)) {
                        sb.append("⚠️ current_phase не является строкой, будет преобразовано в: ")
                          .append(value).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("⚠️ Ошибка при анализе current_phase: ").append(e.getMessage()).append("\n");
                }
            } else {
                sb.append("current_phase: отсутствует\n");
            }
            
            
            if (json.has("current_phase_start_date")) {
                try {
                    Object value = json.opt("current_phase_start_date");
                    sb.append("current_phase_start_date: присутствует, тип: ")
                      .append(value != null ? value.getClass().getName() : "null")
                      .append(", значение: ").append(value).append("\n");
                    
                    if (value != null && !(value instanceof String)) {
                        sb.append("⚠️ current_phase_start_date не является строкой, будет преобразовано в: ")
                          .append(value).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("⚠️ Ошибка при анализе current_phase_start_date: ").append(e.getMessage()).append("\n");
                }
            } else {
                sb.append("current_phase_start_date: отсутствует\n");
            }
            
            
            if (json.has("progression_type")) {
                try {
                    Object value = json.opt("progression_type");
                    sb.append("progression_type: присутствует, тип: ")
                      .append(value != null ? value.getClass().getName() : "null")
                      .append(", значение: ").append(value).append("\n");
                    
                    if (value != null && !(value instanceof String)) {
                        sb.append("⚠️ progression_type не является строкой, будет преобразовано в: ")
                          .append(value).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("⚠️ Ошибка при анализе progression_type: ").append(e.getMessage()).append("\n");
                }
            } else {
                sb.append("progression_type: отсутствует\n");
            }
            
            
            if (json.has("days")) {
                JSONArray days = json.getJSONArray("days");
                sb.append("Количество дней: ").append(days.length()).append("\n");
                if (days.length() > 0) {
                    sb.append("Пример дня: ").append(days.getJSONObject(0).toString()).append("\n");
                }
            }
            
            
            if (json.has("exercises")) {
                JSONArray exercises = json.getJSONArray("exercises");
                sb.append("Количество упражнений: ").append(exercises.length()).append("\n");
                
                
                if (exercises.length() > 0) {
                    JSONObject firstExercise = exercises.getJSONObject(0);
                    sb.append("Пример упражнения: ").append(firstExercise.toString()).append("\n");
                    
                    
                    String[] exerciseFields = {"id", "day_id", "exercise_id", "order_number", "target_sets", "target_reps", "target_weight"};
                    for (String field : exerciseFields) {
                        boolean hasField = firstExercise.has(field);
                        sb.append("- Поле ").append(field).append(": ")
                          .append(hasField ? "присутствует" : "отсутствует");
                        
                        if (hasField) {
                            sb.append(" (").append(firstExercise.opt(field)).append(")");
                        }
                        sb.append("\n");
                    }
                    
                    
                    if (firstExercise.has("day_id") && json.has("days")) {
                        String dayId = firstExercise.getString("day_id");
                        boolean dayFound = false;
                        JSONArray days = json.getJSONArray("days");
                        for (int i = 0; i < days.length(); i++) {
                            if (days.getJSONObject(i).getString("id").equals(dayId)) {
                                dayFound = true;
                                break;
                            }
                        }
                        sb.append("- day_id соответствует существующим дням: ").append(dayFound ? "да" : "нет").append("\n");
                    }
                }
            }
            

        } catch (Exception e) {
            Log.e(TAG, "inspectProgramJson: Ошибка при анализе JSON программы", e);
        }
    }
    
    
    public static void getProgramAsync(String programId, AsyncCallback<Program> callback) {
        Context context = VitaMoveApplication.getContext();
        if (context == null) {
            callback.onFailure(new IllegalStateException("Контекст приложения недоступен"));
            return;
        }
        
        executor.execute(() -> {
            try {
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                
                
                ProgramEntity programEntity = db.programDao().getById(programId);
                if (programEntity == null) {
                    
                    
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onFailure(new Exception("Программа ID: " + programId + " не найдена в кэше Room")));
                    return;
                }
                
                
                Program program = entityToProgram(programEntity);
                
                
                List<ProgramDayEntity> dayEntities = db.programDayDao().getAllByProgramId(programId);
                List<ProgramDay> days = new ArrayList<>();
                for (ProgramDayEntity dayEntity : dayEntities) {
                    ProgramDay day = entityToProgramDay(dayEntity);
                    
                    
                    List<ProgramExerciseEntity> exerciseEntities = db.programExerciseDao().getAllByDayId(dayEntity.getId());
                    List<ProgramExercise> exercises = new ArrayList<>();
                    for (ProgramExerciseEntity exerciseEntity : exerciseEntities) {
                        exercises.add(entityToProgramExercise(exerciseEntity));
                    }
                    day.setExercises(exercises);
                    days.add(day);
                }
                program.setDays(days);
                
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(program));
            } catch (Exception e) {
                Log.e(TAG, "getProgramAsync: Ошибка получения программы из Room", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        });
    }
    
    
    private static ProgramEntity jsonToProgramEntity(JSONObject json) {
        try {
            ProgramEntity entity = new ProgramEntity();
            entity.setId(json.getString("id"));
            
            if (json.has("user_id")) {
                entity.setUserId(json.getString("user_id"));
            }
            
            entity.setName(json.getString("name"));
            entity.setDescription(json.getString("description"));
            
            if (json.has("type")) {
                entity.setType(json.getString("type"));
            }
            
            entity.setDurationWeeks(json.getInt("duration_weeks"));
            entity.setDaysPerWeek(json.getInt("days_per_week"));
            
            if (json.has("is_active")) {
                entity.setActive(json.getBoolean("is_active"));
            } else {
                entity.setActive(true);
            }
            
            if (json.has("start_date")) {
                entity.setStartDate(json.getString("start_date"));
            }
            
            if (json.has("created_at")) {
                entity.setCreatedAt(json.getString("created_at"));
            } else {
                entity.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));
            }
            
            if (json.has("updated_at")) {
                entity.setUpdatedAt(json.getString("updated_at"));
            } else {
                entity.setUpdatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));
            }
            
            if (json.has("template_id")) {
                entity.setTemplateId(json.getString("template_id"));
            }
            
            
            if (json.has("periodization_type")) {
                try {
                    
                    entity.setPeriodizationType(json.getString("periodization_type"));
                } catch (Exception e) {
                    
                    Object value = json.opt("periodization_type");
                    if (value == null) {
                        entity.setPeriodizationType(null);
                    } else {
                        
                        entity.setPeriodizationType(String.valueOf(value));


                    }
                }
            }
            
            
            if (json.has("current_phase")) {
                try {
                    
                    entity.setCurrentPhase(json.getString("current_phase"));
                } catch (Exception e) {
                    
                    Object value = json.opt("current_phase");
                    if (value == null) {
                        entity.setCurrentPhase(null);
                    } else {
                        
                        entity.setCurrentPhase(String.valueOf(value));


                    }
                }
            }
            
            
            if (json.has("current_phase_start_date")) {
                try {
                    
                    entity.setCurrentPhaseStartDate(json.getString("current_phase_start_date"));
                } catch (Exception e) {
                    
                    Object value = json.opt("current_phase_start_date");
                    if (value == null) {
                        entity.setCurrentPhaseStartDate(null);
                    } else {
                        
                        entity.setCurrentPhaseStartDate(String.valueOf(value));


                    }
                }
            }
            
            
            if (json.has("progression_type")) {
                try {
                    
                    entity.setProgressionType(json.getString("progression_type"));
                } catch (Exception e) {
                    
                    Object value = json.opt("progression_type");
                    if (value == null) {
                        entity.setProgressionType(null);
                    } else {
                        
                        entity.setProgressionType(String.valueOf(value));


                    }
                }
            }
            
            return entity;
        } catch (Exception e) {
            Log.e(TAG, "jsonToProgramEntity: Ошибка преобразования JSON в сущность", e);
            return null;
        }
    }
    
    
    private static List<ProgramDayEntity> jsonToDayEntities(JSONArray daysArray) {
        List<ProgramDayEntity> entities = new ArrayList<>();
        try {
            for (int i = 0; i < daysArray.length(); i++) {
                JSONObject dayJson = daysArray.getJSONObject(i);
                ProgramDayEntity entity = new ProgramDayEntity();
                
                entity.setId(dayJson.getString("id"));
                entity.setProgramId(dayJson.getString("program_id"));
                entity.setDayNumber(dayJson.getInt("day_number"));
                entity.setName(dayJson.getString("name"));
                entity.setDescription(dayJson.getString("description"));
                
                if (dayJson.has("template_day_id")) {
                    entity.setTemplateDayId(dayJson.getString("template_day_id"));
                }
                
                if (dayJson.has("created_at")) {
                    entity.setCreatedAt(dayJson.getString("created_at"));
                } else {
                    entity.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));
                }
                
                if (dayJson.has("updated_at")) {
                    entity.setUpdatedAt(dayJson.getString("updated_at"));
                } else {
                    entity.setUpdatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));
                }
                
                entities.add(entity);
            }
        } catch (Exception e) {
            Log.e(TAG, "jsonToDayEntities: Ошибка преобразования JSON в сущности дней", e);
        }
        
        return entities;
    }
    
    
    private static List<ProgramExerciseEntity> jsonToExerciseEntities(JSONArray exercisesArray) {
        List<ProgramExerciseEntity> entities = new ArrayList<>();
        try {

            
            if (exercisesArray.length() > 0) {
                JSONObject firstExercise = exercisesArray.getJSONObject(0);

            }
            
            for (int i = 0; i < exercisesArray.length(); i++) {
                JSONObject exerciseJson = exercisesArray.getJSONObject(i);
                ProgramExerciseEntity entity = new ProgramExerciseEntity();
                
                try {
                    
                    try {
                        entity.setId(exerciseJson.getString("id"));
                    } catch (Exception e) {
                        Object value = exerciseJson.opt("id");
                        if (value == null) {
                            Log.e(TAG, "jsonToExerciseEntities: Обязательное поле id отсутствует или null!");
                            continue; 
                        } else {
                            entity.setId(String.valueOf(value));


                        }
                    }
                    

                    
                    
                    try {
                        entity.setDayId(exerciseJson.getString("day_id"));
                    } catch (Exception e) {
                        Object value = exerciseJson.opt("day_id");
                        if (value == null) {
                            Log.e(TAG, "jsonToExerciseEntities: Обязательное поле day_id отсутствует или null для упражнения ID: " + entity.getId());
                            continue; 
                        } else {
                            entity.setDayId(String.valueOf(value));


                        }
                    }
                    
                    
                    try {
                        entity.setExerciseId(exerciseJson.getString("exercise_id"));
                    } catch (Exception e) {
                        Object value = exerciseJson.opt("exercise_id");
                        if (value == null) {
                            Log.e(TAG, "jsonToExerciseEntities: Обязательное поле exercise_id отсутствует или null для упражнения ID: " + entity.getId());
                            continue; 
                        } else {
                            entity.setExerciseId(String.valueOf(value));


                        }
                    }
                    
                    entity.setOrderNumber(exerciseJson.getInt("order_number"));
                    entity.setTargetSets(exerciseJson.getInt("target_sets"));
                    
                    
                    try {
                        entity.setTargetReps(exerciseJson.getString("target_reps"));
                    } catch (Exception e) {
                        
                        Object value = exerciseJson.opt("target_reps");
                        if (value == null) {
                            entity.setTargetReps("10"); 

                        } else {
                            
                            entity.setTargetReps(String.valueOf(value));

                        }
                    }
                    
                    
                    try {
                        entity.setTargetWeight(exerciseJson.getString("target_weight"));
                    } catch (Exception e) {
                        
                        Object value = exerciseJson.opt("target_weight");
                        if (value == null) {
                            entity.setTargetWeight("0"); 

                        } else {
                            
                            entity.setTargetWeight(String.valueOf(value));


                        }
                    }
                    
                    if (exerciseJson.has("rest_seconds")) {
                        entity.setRestSeconds(exerciseJson.getInt("rest_seconds"));
                    } else {
                        entity.setRestSeconds(60); 
                    }
                    
                    
                    if (exerciseJson.has("template_exercise_id")) {
                        try {
                            entity.setTemplateExerciseId(exerciseJson.getString("template_exercise_id"));
                        } catch (Exception e) {
                            
                            Object value = exerciseJson.opt("template_exercise_id");
                            if (value == null) {
                                entity.setTemplateExerciseId(null);
                            } else {
                                
                                entity.setTemplateExerciseId(String.valueOf(value));


                            }
                        }
                    }
                    
                    
                    if (exerciseJson.has("notes")) {
                        try {
                            entity.setNotes(exerciseJson.getString("notes"));
                        } catch (Exception e) {
                            
                            Object value = exerciseJson.opt("notes");
                            if (value == null) {
                                entity.setNotes(null);
                            } else {
                                
                                entity.setNotes(String.valueOf(value));


                            }
                        }
                    }
                    
                    
                    if (exerciseJson.has("created_at")) {
                        try {
                            entity.setCreatedAt(exerciseJson.getString("created_at"));
                        } catch (Exception e) {
                            
                            Object value = exerciseJson.opt("created_at");
                            if (value == null) {
                                entity.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));

                            } else {
                                
                                entity.setCreatedAt(String.valueOf(value));


                            }
                        }
                    } else {
                        entity.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));
                    }
                    
                    
                    if (exerciseJson.has("updated_at")) {
                        try {
                            entity.setUpdatedAt(exerciseJson.getString("updated_at"));
                        } catch (Exception e) {
                            
                            Object value = exerciseJson.opt("updated_at");
                            if (value == null) {
                                entity.setUpdatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));

                            } else {
                                
                                entity.setUpdatedAt(String.valueOf(value));


                            }
                        }
                    } else {
                        entity.setUpdatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new java.util.Date()));
                    }
                    
                    entities.add(entity);
                } catch (Exception e) {
                    Log.e(TAG, "jsonToExerciseEntities: Ошибка при обработке упражнения #" + i, e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "jsonToExerciseEntities: Ошибка преобразования JSON в сущности упражнений", e);
        }
        
        return entities;
    }
    
    
    private static Program entityToProgram(ProgramEntity entity) {
        Program program = new Program();
        program.setId(entity.getId());
        program.setName(entity.getName());
        program.setDescription(entity.getDescription());
        program.setDurationWeeks(entity.getDurationWeeks());
        program.setWorkoutsPerWeek(entity.getDaysPerWeek());
        
        if (entity.getType() != null) {
            program.addGoal(entity.getType());
        }
        
        return program;
    }
    
    
    public static ProgramDay entityToProgramDay(ProgramDayEntity entity) {
        if (entity == null) {
            return null;
        }
        ProgramDay day = new ProgramDay();
        day.setId(entity.getId());
        day.setProgramId(entity.getProgramId());
        day.setDayNumber(entity.getDayNumber());
        day.setName(entity.getName());
        day.setDescription(entity.getDescription());
        return day;
    }
    
    
    public static ProgramExercise entityToProgramExercise(ProgramExerciseEntity entity) {
        if (entity == null) {
            return null;
        }
        ProgramExercise exercise = new ProgramExercise();
        exercise.setId(entity.getId());
        exercise.setDayId(entity.getDayId());
        exercise.setExerciseId(entity.getExerciseId());
        exercise.setOrderNumber(entity.getOrderNumber());
        exercise.setTargetSets(entity.getTargetSets());
        
        
        try {
            String repsStr = entity.getTargetReps();
            if (repsStr != null) {
                
                if (repsStr.contains("-")) {
                    
                    String firstNumber = repsStr.split("-")[0].trim();
                    exercise.setTargetReps(Integer.parseInt(firstNumber));
                } else if (repsStr.contains("+")) {
                    
                    String number = repsStr.replace("+", "").trim();
                    exercise.setTargetReps(Integer.parseInt(number));
                } else {
                    
                    exercise.setTargetReps(Integer.parseInt(repsStr.trim()));
                }
            } else {

                exercise.setTargetReps(10); 
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "entityToProgramExercise: Ошибка парсинга target_reps для ID: " + entity.getId() + ", значение: " + entity.getTargetReps(), e);
            exercise.setTargetReps(10); 
        }
        
        
        try {
            String weightStr = entity.getTargetWeight();
            if (weightStr != null) {
                
                weightStr = weightStr.replace(',', '.').replaceAll("[^0-9.]", "");
                 if (!weightStr.isEmpty() && !weightStr.equals(".")) { 
                    exercise.setTargetWeight(Float.parseFloat(weightStr));
                 } else {

                     exercise.setTargetWeight(0f); 
                 }
            } else {

                exercise.setTargetWeight(0f); 
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "entityToProgramExercise: Ошибка парсинга target_weight для ID: " + entity.getId() + ", значение: " + entity.getTargetWeight(), e);
            exercise.setTargetWeight(0f); 
        }
        
        exercise.setNotes(entity.getNotes());
        
        
        
        
        return exercise;
    }
    
    
    public static void clearCache() {
        Context context = VitaMoveApplication.getContext();
        if (context == null) {
            Log.e(TAG, "clearCache: Контекст приложения недоступен");
            return;
        }
        
        executor.execute(() -> {
            try {
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                db.programExerciseDao().deleteAll();
                db.programDayDao().deleteAll();
                db.programDao().deleteAll();
                db.workoutPlanDao().deleteAll();
                
                
                
                

            } catch (Exception e) {
                Log.e(TAG, "clearCache: Ошибка очистки кэша", e);
            }
        });
    }
    
    
    public static void debugExerciseSaving(String programId) {
        Context context = VitaMoveApplication.getContext();
        if (context == null) {
            Log.e(TAG, "debugExerciseSaving: Контекст приложения недоступен");
            return;
        }
        
        executor.execute(() -> {
            try {
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                
                
                List<ProgramDayEntity> days = db.programDayDao().getAllByProgramId(programId);

                
                
                for (ProgramDayEntity day : days) {
                    List<ProgramExerciseEntity> exercises = db.programExerciseDao().getAllByDayId(day.getId());

                    
                    
                    for (int i = 0; i < exercises.size(); i++) {
                        ProgramExerciseEntity exercise = exercises.get(i);


                    }
                }
                
                
                List<ProgramExerciseEntity> allExercises = db.programExerciseDao().getAll();

                
            } catch (Exception e) {
                Log.e(TAG, "debugExerciseSaving: Ошибка при отладке сохранения упражнений", e);
            }
        });
    }

    

    
    public static void saveWorkoutPlans(String programId, List<WorkoutPlan> plans) {
        if (plans == null || programId == null || programId.isEmpty()) {

            return;
        }
        Context context = VitaMoveApplication.getContext();
        if (context == null) {
            Log.e(TAG, "saveWorkoutPlans: Контекст приложения недоступен");
            return;
        }

        executor.execute(() -> {
            try {
                
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                WorkoutPlanDao dao = db.workoutPlanDao();

                List<WorkoutPlanEntity> entities = plans.stream()
                        .map(ProgramRoomCache::mapWorkoutPlanToEntity) 
                        .collect(Collectors.toList());

                
                if (!entities.isEmpty()) {
                    List<String> entityIds = entities.stream().map(e -> e.id).collect(Collectors.toList());

                } else {

                }

                
                db.runInTransaction(() -> {
                    
                    dao.deletePlansByProgramId(programId);
                    
                    dao.insertAll(entities);
                });


            } catch (Exception e) {
                Log.e(TAG, "saveWorkoutPlans: Ошибка сохранения планов в Room", e);
            }
        });
    }

    
    public static void getWorkoutPlansByProgramId(String programId, AsyncCallback<List<WorkoutPlan>> callback) {
        Context context = VitaMoveApplication.getContext();
        if (context == null) {
            Log.e(TAG, "getWorkoutPlansByProgramId: Контекст приложения недоступен");
            callback.onFailure(new IllegalStateException("Context is null"));
            return;
        }
        executor.execute(() -> {
            try {
                
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                WorkoutPlanDao dao = db.workoutPlanDao();
                List<WorkoutPlanEntity> entities = dao.getPlansByProgramId(programId);
                List<WorkoutPlan> plans = entities.stream()
                        .map(ProgramRoomCache::mapEntityToWorkoutPlan) 
                        .collect(Collectors.toList());



                
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(plans);
                });
            } catch (Exception e) {
                Log.e(TAG, "getWorkoutPlansByProgramId: Ошибка получения планов из Room", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onFailure(e);
                });
            }
        });
    }

    

    private static WorkoutPlanEntity mapWorkoutPlanToEntity(WorkoutPlan plan) {
        WorkoutPlanEntity entity = new WorkoutPlanEntity();
        entity.id = plan.getId();
        entity.userId = plan.getUserId();
        entity.name = plan.getName();
        
        entity.plannedDate = com.martist.vitamove.utils.DateUtils.formatDateToIso(plan.getPlannedDate());
        entity.programId = plan.getProgramId();
        entity.programDayId = plan.getProgramDayId();
        entity.status = plan.getStatus();
        entity.notes = plan.getNotes();
        entity.createdAt = com.martist.vitamove.utils.DateUtils.formatDateToIso(plan.getCreatedAt());
        entity.updatedAt = com.martist.vitamove.utils.DateUtils.formatDateToIso(plan.getUpdatedAt());
        entity.cachedAt = System.currentTimeMillis();
        return entity;
    }

    
    public static WorkoutPlan mapEntityToWorkoutPlan(WorkoutPlanEntity entity) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setId(entity.id);
        plan.setUserId(entity.userId);
        plan.setName(entity.name);
        
        try {
            plan.setPlannedDate(com.martist.vitamove.utils.DateUtils.parseIsoDate(entity.plannedDate));
            plan.setCreatedAt(com.martist.vitamove.utils.DateUtils.parseIsoDate(entity.createdAt));
            plan.setUpdatedAt(com.martist.vitamove.utils.DateUtils.parseIsoDate(entity.updatedAt));
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка парсинга даты при маппинге из WorkoutPlanEntity: " + e.getMessage());
            
            plan.setPlannedDate(0);
            plan.setCreatedAt(0);
            plan.setUpdatedAt(0);
        }
        plan.setProgramId(entity.programId);
        plan.setProgramDayId(entity.programDayId);
        plan.setStatus(entity.status);
        plan.setNotes(entity.notes);
        
        plan.setCompleted(entity.status != null && entity.status.equalsIgnoreCase("completed"));
        plan.setMissed(entity.status != null && entity.status.equalsIgnoreCase("skipped")); 
        
        
        return plan;
    }

    
    public static void updateWorkoutPlanStatus(String planId, String newStatus) {
        if (planId == null || planId.isEmpty()) {
            Log.e(TAG, "updateWorkoutPlanStatus: planId не может быть пустым");
            return;
        }
        
        Context context = VitaMoveApplication.getContext();
        if (context == null) {
            Log.e(TAG, "updateWorkoutPlanStatus: Контекст приложения недоступен");
            return;
        }
        

        
        executor.execute(() -> {
            try {
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                WorkoutPlanDao dao = db.workoutPlanDao();
                
                
                List<WorkoutPlanEntity> entities = dao.getPlanById(planId);
                if (entities.isEmpty()) {

                    return;
                }
                
                
                WorkoutPlanEntity entity = entities.get(0);
                entity.status = newStatus;
                entity.updatedAt = com.martist.vitamove.utils.DateUtils.formatDateToIso(System.currentTimeMillis());
                
                
                dao.update(entity);
                

            } catch (Exception e) {
                Log.e(TAG, "updateWorkoutPlanStatus: Ошибка обновления статуса плана в кэше", e);
            }
        });
    }

    
    public static void saveProgramAsync(JSONObject programJson, AsyncCallback<Boolean> callback) {
        try {
            if (programJson == null) {
                if (callback != null) {
                    callback.onFailure(new IllegalArgumentException("programJson не может быть null"));
                }
                return;
            }
            
            
            String programId;
            try {
                programId = programJson.getString("id");
            } catch (Exception e) {
                Log.e(TAG, "saveProgramAsync: ID программы отсутствует в JSON", e);
                if (callback != null) {
                    callback.onFailure(new IllegalArgumentException("ID программы отсутствует в JSON"));
                }
                return;
            }
            

            
            
            inspectProgramJson(programJson);
            
            executor.execute(() -> {
                try {
                    Context context = VitaMoveApplication.getContext();
                    if (context == null) {
                        Log.e(TAG, "saveProgramAsync: Контекст приложения недоступен");
                        if (callback != null) {
                            callback.onFailure(new IllegalStateException("Контекст приложения недоступен"));
                        }
                        return;
                    }
                    
                    ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                    
                    
                    ProgramEntity programEntity = jsonToProgramEntity(programJson);
                    if (programEntity != null) {
                        db.programDao().insert(programEntity);

                    } else {
                        String errorMsg = "Не удалось создать ProgramEntity из JSON";
                        Log.e(TAG, "saveProgramAsync: " + errorMsg);
                        
                        try {
                            if (programJson.has("periodization_type")) {
                                Object periodzationType = programJson.opt("periodization_type");
                                Log.e(TAG, "saveProgramAsync: Проблемное поле periodization_type тип: " + 
                                      (periodzationType != null ? periodzationType.getClass().getName() : "null") + 
                                      ", значение: " + periodzationType);
                            } else {
                                Log.e(TAG, "saveProgramAsync: Поле periodization_type отсутствует в JSON");
                            }
                            
                            
                            if (programJson.has("current_phase")) {
                                Object currentPhase = programJson.opt("current_phase");
                                Log.e(TAG, "saveProgramAsync: Проблемное поле current_phase тип: " + 
                                      (currentPhase != null ? currentPhase.getClass().getName() : "null") + 
                                      ", значение: " + currentPhase);
                            } else {
                                Log.e(TAG, "saveProgramAsync: Поле current_phase отсутствует в JSON");
                            }
                            
                            
                            if (programJson.has("current_phase_start_date")) {
                                Object currentPhaseStartDate = programJson.opt("current_phase_start_date");
                                Log.e(TAG, "saveProgramAsync: Поле current_phase_start_date тип: " + 
                                      (currentPhaseStartDate != null ? currentPhaseStartDate.getClass().getName() : "null") + 
                                      ", значение: " + currentPhaseStartDate);
                            } 
                            
                            
                            if (programJson.has("progression_type")) {
                                Object progressionType = programJson.opt("progression_type");
                                Log.e(TAG, "saveProgramAsync: Поле progression_type тип: " + 
                                      (progressionType != null ? progressionType.getClass().getName() : "null") + 
                                      ", значение: " + progressionType);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "saveProgramAsync: Ошибка при диагностике полей JSON", e);
                        }
                        
                        if (callback != null) {
                            callback.onFailure(new IllegalStateException(errorMsg));
                        }
                        return;
                    }
                    
                    
                    if (programJson.has("days")) {
                        JSONArray daysArray = programJson.getJSONArray("days");

                        List<ProgramDayEntity> dayEntities = jsonToDayEntities(daysArray);
                        
                        if (!dayEntities.isEmpty()) {
                            
                            db.programDayDao().replaceDaysForProgram(programId, dayEntities);

                            
                            
                            int totalExercisesSaved = 0;
                            for (int i = 0; i < daysArray.length(); i++) {
                                JSONObject dayJson = daysArray.getJSONObject(i);
                                String currentDayId = dayJson.getString("id");
                                
                                if (dayJson.has("exercises")) {
                                    JSONArray exercisesArray = dayJson.getJSONArray("exercises");

                                    
                                    List<ProgramExerciseEntity> exerciseEntities = jsonToExerciseEntities(exercisesArray);
                                    
                                    if (!exerciseEntities.isEmpty()) {
                                        db.programExerciseDao().replaceExercisesForDay(currentDayId, exerciseEntities);

                                        totalExercisesSaved += exerciseEntities.size();
                                    }
                                } else {

                                    
                                    db.programExerciseDao().deleteAllByDayId(currentDayId);
                                }
                            }
                            

                            if (callback != null) {
                                callback.onSuccess(true);
                            }
                        } else {

                            if (callback != null) {
                                callback.onSuccess(false); 
                            }
                        }
                    } else {

                        if (callback != null) {
                            callback.onSuccess(false); 
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "saveProgramAsync: Ошибка сохранения программы", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "saveProgramAsync: Ошибка при начале асинхронного сохранения", e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }
} 