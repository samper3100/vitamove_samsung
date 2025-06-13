package com.martist.vitamove.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import com.martist.vitamove.workout.data.models.WorkoutExercise;
import com.martist.vitamove.workout.data.models.Exercise;
import com.martist.vitamove.workout.data.models.ExerciseSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Entity(tableName = "workout_exercises",
        foreignKeys = @ForeignKey(entity = UserWorkoutEntity.class,
                                  parentColumns = "id",
                                  childColumns = "workout_id",
                                  onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "workout_id")})
public class WorkoutExerciseEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "order_number")
    private int orderNumber;

    private String notes;

    @ColumnInfo(name = "start_time")
    private Date startTime;

    @ColumnInfo(name = "end_time")
    private Date endTime;

    @ColumnInfo(name = "is_rated")
    private boolean isRated;

    @NonNull
    @ColumnInfo(name = "workout_id")
    private String workoutId;

    @NonNull
    @ColumnInfo(name = "base_exercise_id")
    private String baseExerciseId;







    public WorkoutExerciseEntity() {}


    @Ignore
    public WorkoutExerciseEntity(@NonNull String id, int orderNumber, String notes, Date startTime, Date endTime,
                                 boolean isRated, @NonNull String workoutId, @NonNull String baseExerciseId) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.notes = notes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isRated = isRated;
        this.workoutId = workoutId;
        this.baseExerciseId = baseExerciseId;
    }
    

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public boolean isRated() {
        return isRated;
    }

    public void setRated(boolean rated) {
        isRated = rated;
    }

    @NonNull
    public String getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(@NonNull String workoutId) {
        this.workoutId = workoutId;
    }

    @NonNull
    public String getBaseExerciseId() {
        return baseExerciseId;
    }

    public void setBaseExerciseId(@NonNull String baseExerciseId) {
        this.baseExerciseId = baseExerciseId;
    }
    

    public WorkoutExercise toModel(Exercise baseExerciseDetails, List<ExerciseSet> sets) {
        WorkoutExercise model = new WorkoutExercise();
        model.setId(this.id);
        model.setExercise(baseExerciseDetails);
        model.setOrderNumber(this.orderNumber);
        model.setSetsCompleted(sets != null ? sets : new ArrayList<>());
        model.setNotes(this.notes);
        model.setStartTime(this.startTime);
        model.setEndTime(this.endTime);
        model.setRated(this.isRated);

        return model;
    }


    public static WorkoutExerciseEntity fromModel(WorkoutExercise model, @NonNull String workoutId) {
        if (model.getId() == null) {
            model.setId(java.util.UUID.randomUUID().toString());
        }
        if (model.getExercise() == null || model.getExercise().getId() == null) {
            throw new IllegalArgumentException("Base Exercise ID cannot be null in WorkoutExercise model");
        }
        return new WorkoutExerciseEntity(
                model.getId(),
                model.getOrderNumber(),
                model.getNotes(),
                model.getStartTime(),
                model.getEndTime(),
                model.isRated(),
                workoutId,
                model.getExercise().getId()
        );
    }
} 