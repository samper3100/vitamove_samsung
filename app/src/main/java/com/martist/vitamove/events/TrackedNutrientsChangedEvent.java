package com.martist.vitamove.events;

import java.util.List;


public class TrackedNutrientsChangedEvent {
    private final List<String> trackedNutrients;
    
    public TrackedNutrientsChangedEvent(List<String> trackedNutrients) {
        this.trackedNutrients = trackedNutrients;
    }
    
    public List<String> getTrackedNutrients() {
        return trackedNutrients;
    }
} 