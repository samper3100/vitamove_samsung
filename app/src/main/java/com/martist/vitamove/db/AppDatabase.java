package com.martist.vitamove.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.martist.vitamove.db.converters.DateConverter;
import com.martist.vitamove.db.converters.ListConverter;
import com.martist.vitamove.db.dao.ExerciseDao;
import com.martist.vitamove.db.dao.StepHistoryDao;
import com.martist.vitamove.db.dao.WorkoutDao;
import com.martist.vitamove.db.entity.ExerciseEntity;
import com.martist.vitamove.db.entity.ExerciseSetEntity;
import com.martist.vitamove.db.entity.StepHistoryEntity;
import com.martist.vitamove.db.entity.UserWorkoutEntity;
import com.martist.vitamove.db.entity.WorkoutExerciseEntity;

@Database(entities = {
        UserWorkoutEntity.class, 
        WorkoutExerciseEntity.class, 
        ExerciseSetEntity.class,
        ExerciseEntity.class,
        StepHistoryEntity.class},
        version = 7,
        exportSchema = false)
@TypeConverters({DateConverter.class, ListConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "vitamove.db";
    private static AppDatabase instance;
    private static Context applicationContext;
    

    public abstract WorkoutDao workoutDao();
    public abstract ExerciseDao exerciseDao();
    public abstract StepHistoryDao stepHistoryDao();
    

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            applicationContext = context.getApplicationContext();
            instance = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase.class,
                    DATABASE_NAME)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
    

    public static synchronized void resetInstance() {
        if (instance != null) {
            if (instance.isOpen()) {
                instance.close();
            }
            instance = null;
        }
    }
    

    public static Context getContext() {
        return applicationContext;
    }
    

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `step_history` " +
                "(`date` TEXT NOT NULL, " +
                "`step_count` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`date`))");
        }
    };
    

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                "ALTER TABLE exercise_sets ADD COLUMN exercise_id TEXT");
            

            database.execSQL(
                "UPDATE exercise_sets SET exercise_id = (" +
                "SELECT base_exercise_id FROM workout_exercises " +
                "WHERE workout_exercises.id = exercise_sets.workout_exercise_id) " +
                "WHERE exercise_id IS NULL");
        }
    };
    

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                "ALTER TABLE exercise_sets ADD COLUMN created_at INTEGER");
            

            database.execSQL(
                "UPDATE exercise_sets SET created_at = " + System.currentTimeMillis() + 
                " WHERE created_at IS NULL");
        }
    };
} 