package com.martist.vitamove.events;


public class AddExerciseEvent {
    public final String exerciseId;

    public AddExerciseEvent(String exerciseId) {
        this.exerciseId = exerciseId;
    }
}