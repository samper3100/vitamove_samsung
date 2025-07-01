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
import com.martist.vitamove.db.dao.UserWeightDao;
import com.martist.vitamove.db.dao.WorkoutDao;
import com.martist.vitamove.db.entity.ExerciseEntity;
import com.martist.vitamove.db.entity.ExerciseSetEntity;
import com.martist.vitamove.db.entity.StepHistoryEntity;
import com.martist.vitamove.db.entity.UserWeightEntity;
import com.martist.vitamove.db.entity.UserWorkoutEntity;
import com.martist.vitamove.db.entity.WorkoutExerciseEntity;

@Database(entities = {
        UserWorkoutEntity.class, 
        WorkoutExerciseEntity.class, 
        ExerciseSetEntity.class,
        ExerciseEntity.class,
        StepHistoryEntity.class,
        UserWeightEntity.class},
        version = 12,
        exportSchema = false)
@TypeConverters({DateConverter.class, ListConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "vitamove.db";
    private static AppDatabase instance;
    private static Context applicationContext;
    

    public abstract WorkoutDao workoutDao();
    public abstract ExerciseDao exerciseDao();
    public abstract StepHistoryDao stepHistoryDao();
    public abstract UserWeightDao userWeightDao();
    

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            applicationContext = context.getApplicationContext();
            instance = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase.class,
                    DATABASE_NAME)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
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
    

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `user_weight_history` (" +
                "`id` TEXT NOT NULL, " +
                "`user_id` TEXT, " +
                "`weight` REAL NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`notes` TEXT, " +
                "`created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "`is_synced` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`))");
                

            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_user_weight_history_user_id` " +
                "ON `user_weight_history` (`user_id`)");
                

            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_user_weight_history_date` " +
                "ON `user_weight_history` (`date`)");
        }
    };
    

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

        }
    };
    

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE exercises ADD COLUMN categories TEXT");
            

            database.execSQL(
                "UPDATE exercises SET categories = '[\"' || category || '\"]' " +
                "WHERE category IS NOT NULL AND category != ''");
            

            database.execSQL(
                "UPDATE exercises SET categories = '[]' " +
                "WHERE (category IS NULL OR category = '') AND categories IS NULL");
        }
    };
    

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            

            database.execSQL(
                "CREATE TABLE `exercises_new` (" +
                "`id` TEXT NOT NULL PRIMARY KEY, " +
                "`name` TEXT, " +
                "`description` TEXT, " +
                "`difficulty` TEXT, " +
                "`exerciseType` TEXT, " +
                "`met` REAL NOT NULL, " +
                "`muscleGroups` TEXT, " +
                "`muscleGroupRussianNames` TEXT, " +
                "`equipmentRequired` TEXT, " +
                "`categories` TEXT)");
            

            database.execSQL(
                "INSERT INTO `exercises_new` " +
                "(`id`, `name`, `description`, `difficulty`, `exerciseType`, " +
                "`met`, `muscleGroups`, `muscleGroupRussianNames`, " +
                "`equipmentRequired`, `categories`) " +
                "SELECT `id`, `name`, `description`, `difficulty`, `exerciseType`, " +
                "`met`, `muscleGroups`, `muscleGroupRussianNames`, " +
                "`equipmentRequired`, `categories` " +
                "FROM `exercises`");
            

            database.execSQL("DROP TABLE `exercises`");
            

            database.execSQL("ALTER TABLE `exercises_new` RENAME TO `exercises`");
        }
    };
    

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE exercises ADD COLUMN instructions TEXT");
        }
    };
} 