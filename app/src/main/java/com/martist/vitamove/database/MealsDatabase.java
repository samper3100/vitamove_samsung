package com.martist.vitamove.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {DayMeal.class}, version = 3)
@TypeConverters({DateConverter.class})
public abstract class MealsDatabase extends RoomDatabase {
    private static MealsDatabase instance;

    public abstract MealDao mealDao();

    public static synchronized MealsDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MealsDatabase.class,
                            "meals_database"
                    )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }
} 