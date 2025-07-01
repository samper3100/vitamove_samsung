package com.martist.vitamove.events;


public class WorkoutStartedEvent {
    private final long startTime;

    public WorkoutStartedEvent(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }
} 