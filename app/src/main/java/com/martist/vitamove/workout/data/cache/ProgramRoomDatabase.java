package com.martist.vitamove.workout.data.cache;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.martist.vitamove.workout.data.dao.ProgramDao;
import com.martist.vitamove.workout.data.dao.ProgramDayDao;
import com.martist.vitamove.workout.data.dao.ProgramExerciseDao;
import com.martist.vitamove.workout.data.models.room.ProgramEntity;
import com.martist.vitamove.workout.data.models.room.ProgramDayEntity;
import com.martist.vitamove.workout.data.models.room.ProgramExerciseEntity;
import com.martist.vitamove.workout.data.models.cache.WorkoutPlanEntity;


@Database(
    entities = {
        ProgramEntity.class,
        ProgramDayEntity.class, 
        ProgramExerciseEntity.class,
        WorkoutPlanEntity.class
    }, 
    version = 2,
    exportSchema = false
)
public abstract class ProgramRoomDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "program_database";
    private static volatile ProgramRoomDatabase INSTANCE;
    

    public abstract ProgramDao programDao();
    public abstract ProgramDayDao programDayDao();
    public abstract ProgramExerciseDao programExerciseDao();
    public abstract WorkoutPlanDao workoutPlanDao();
    

    public static ProgramRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ProgramRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ProgramRoomDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
} 