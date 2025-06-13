package com.martist.vitamove.workout.data.models;


public enum ProgramType {
    
    OFFICIAL,
    
    
    USER_CREATED,
    
    
    SYSTEM,
    
    
    OTHER;
    
    
    public String getDisplayName() {
        switch (this) {
            case OFFICIAL:
                return "Официальная";
            case USER_CREATED:
                return "Пользовательская";
            case SYSTEM:
                return "Системная";
            case OTHER:
                return "Другая";
            default:
                return "Неизвестная";
        }
    }
} 