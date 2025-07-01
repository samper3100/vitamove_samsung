package com.martist.vitamove.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.martist.vitamove.db.converters.ListConverter;
import com.martist.vitamove.workout.data.models.Exercise;

import java.util.List;

@Entity(tableName = "exercises")
public class ExerciseEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String description;
    private String difficulty;
    private String exerciseType;
    private float met;
    
    @TypeConverters(ListConverter.class)
    private List<String> categories;
    
    @TypeConverters(ListConverter.class)
    private List<String> muscleGroups;
    
    @TypeConverters(ListConverter.class)
    private List<String> muscleGroupRussianNames;
    
    @TypeConverters(ListConverter.class)
    private List<String> equipmentRequired;
    

    private String instructions;


    public ExerciseEntity() {
    }


    public ExerciseEntity(Exercise exercise) {
        this.id = exercise.getId();
        this.name = exercise.getName();
        this.description = exercise.getDescription();
        this.difficulty = exercise.getDifficulty();
        this.exerciseType = exercise.getExerciseType();
        this.muscleGroups = exercise.getMuscleGroups();
        this.muscleGroupRussianNames = exercise.getMuscleGroupRussianNames();
        this.equipmentRequired = exercise.getEquipmentRequired();
        this.met = exercise.getMet();
        this.categories = exercise.getCategories();
        this.instructions = exercise.getInstructionsText();
    }


    public Exercise toExercise() {
        Exercise.Builder builder = new Exercise.Builder();
        builder.id(id)
               .name(name)
               .description(description)
               .difficulty(difficulty)
               .exerciseType(exerciseType)
               .muscleGroups(muscleGroups)
               .equipmentRequired(equipmentRequired)
               .met(this.met)
               .categories(this.categories)
               .instructions(this.instructions);
               
        return builder.build();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public List<String> getMuscleGroups() {
        return muscleGroups;
    }

    public void setMuscleGroups(List<String> muscleGroups) {
        this.muscleGroups = muscleGroups;
    }

    public List<String> getMuscleGroupRussianNames() {
        return muscleGroupRussianNames;
    }

    public void setMuscleGroupRussianNames(List<String> muscleGroupRussianNames) {
        this.muscleGroupRussianNames = muscleGroupRussianNames;
    }
    
    public List<String> getEquipmentRequired() {
        return equipmentRequired;
    }

    public void setEquipmentRequired(List<String> equipmentRequired) {
        this.equipmentRequired = equipmentRequired;
    }

    public float getMet() {
        return met;
    }

    public void setMet(float met) {
        this.met = met;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
    

    public String getCategory() {
        if (categories != null && !categories.isEmpty()) {
            return categories.get(0);
        }
        return "";
    }


    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
} 