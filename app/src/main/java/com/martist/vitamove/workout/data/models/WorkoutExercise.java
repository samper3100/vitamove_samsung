package com.martist.vitamove.workout.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkoutExercise implements Parcelable {
    private String id;
    private Exercise exercise;
    private int orderNumber;
    private List<ExerciseSet> setsCompleted;
    private String notes;
    private Date startTime;
    private Date endTime;
    private boolean isRated;
    private boolean isCompleted;

    public WorkoutExercise() {
        this.setsCompleted = new ArrayList<>();
        this.isRated = false;
        this.isCompleted = false;
    }

    protected WorkoutExercise(Parcel in) {
        id = in.readString();
        exercise = in.readParcelable(Exercise.class.getClassLoader());
        orderNumber = in.readInt();
        setsCompleted = new ArrayList<>();
        in.readList(setsCompleted, ExerciseSet.class.getClassLoader());
        notes = in.readString();
        long tmpStartTime = in.readLong();
        startTime = tmpStartTime != -1 ? new Date(tmpStartTime) : null;
        long tmpEndTime = in.readLong();
        endTime = tmpEndTime != -1 ? new Date(tmpEndTime) : null;
        isRated = in.readByte() != 0;
        isCompleted = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeParcelable(exercise, flags);
        dest.writeInt(orderNumber);
        dest.writeList(setsCompleted);
        dest.writeString(notes);
        dest.writeLong(startTime != null ? startTime.getTime() : -1);
        dest.writeLong(endTime != null ? endTime.getTime() : -1);
        dest.writeByte((byte) (isRated ? 1 : 0));
        dest.writeByte((byte) (isCompleted ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WorkoutExercise> CREATOR = new Creator<WorkoutExercise>() {
        @Override
        public WorkoutExercise createFromParcel(Parcel in) {
            return new WorkoutExercise(in);
        }

        @Override
        public WorkoutExercise[] newArray(int size) {
            return new WorkoutExercise[size];
        }
    };

    public WorkoutExercise(WorkoutExercise template) {
        this.exercise = template.getExercise();
        this.orderNumber = template.getOrderNumber();
        this.setsCompleted = new ArrayList<>(template.getSetsCompleted());
        this.notes = template.getNotes();
        this.isRated = template.isRated();
        this.isCompleted = template.isCompleted();
    }

    public WorkoutExercise(String id, Exercise exercise, int orderNumber, 
                          List<ExerciseSet> setsCompleted, String notes) {
        this.id = id;
        this.exercise = exercise;
        this.orderNumber = orderNumber;
        this.setsCompleted = setsCompleted != null ? setsCompleted : new ArrayList<>();
        this.notes = notes;
        this.isRated = false;
        this.isCompleted = false;
    }


    public String getId() { return id; }
    public Exercise getExercise() { return exercise; }
    public int getOrderNumber() { return orderNumber; }
    public List<ExerciseSet> getSetsCompleted() { return setsCompleted; }
    public String getNotes() { return notes; }
    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }
    public boolean isRated() { return isRated; }
    public boolean isCompleted() { return isCompleted; }
    public List<Exercise> getExercises() { 
        List<Exercise> exercises = new ArrayList<>();
        if (exercise != null) {
            exercises.add(exercise);
        }
        return exercises;
    }


    public void setId(String id) { this.id = id; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }
    public void setSetsCompleted(List<ExerciseSet> setsCompleted) {
        this.setsCompleted = setsCompleted != null ? setsCompleted : new ArrayList<>();
    }
    public void setNotes(String notes) { this.notes = notes; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public void setRated(boolean rated) { this.isRated = rated; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }


    public void addSet(ExerciseSet set) {
        if (set != null) {
            setsCompleted.add(set);
        }
    }

    public void removeSet(ExerciseSet set) {
        setsCompleted.remove(set);
    }

    public void clearSets() {
        setsCompleted.clear();
    }

    public int getCompletedSetsCount() {
        if (setsCompleted == null || setsCompleted.isEmpty()) {
            return 0;
        }
        

        int count = 0;
        for (ExerciseSet set : setsCompleted) {
            boolean isCompleted = set.isCompleted();
            if (isCompleted) {
                count++;
            }

            
        }
        

        
        
        return count;
    }


    public void updateCompletedSetsCount() {


    }
} 