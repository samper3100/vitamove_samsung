package com.martist.vitamove.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.fragments.SettingsFragment;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.io.File;
import java.lang.reflect.Field;


public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Настройки");
        
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
        
        
        MaterialButton logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logout();
                }
            });
        }
    }
    
    
    private void logout() {
        
        
        
        androidx.appcompat.app.AlertDialog loadingDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(R.layout.dialog_logout)
            .setCancelable(false)
            .create();
        loadingDialog.show();
        
        
        TextView messageTextView = null;
        try {
            messageTextView = loadingDialog.findViewById(R.id.logout_message);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Не удалось найти TextView в диалоге: " + e.getMessage(), e);
        }
        
        
        final TextView finalMessageTextView = messageTextView;
        
        
        
        SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings_container);
        if (fragment != null) {
            fragment.unregisterListener();
            
        }

        
        SharedPreferences appPrefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        String userId = appPrefs.getString("userId", null);
        
        
        if (userId != null) {
            updateLoadingMessage(finalMessageTextView, "Удаление незавершенных тренировок...");
            new Thread(() -> {
                try {
                    WorkoutRepository workoutRepository = new SupabaseWorkoutRepository(SupabaseClient.getInstance(SupabaseClient.SUPABASE_URL, Constants.SUPABASE_CLIENT_SECRET));
                    workoutRepository.deleteUnfinishedWorkouts(userId);
                    
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при удалении незавершенных тренировок: " + e.getMessage(), e);
                    
                }
            }).start();
        }
        
        
        updateLoadingMessage(finalMessageTextView, "Закрытие активных соединений с базами данных...");
        
        

        
        
        updateLoadingMessage(finalMessageTextView, "Удаление служебных файлов...");
        removeMealsDatabaseSaveTags();
        
        
        updateLoadingMessage(finalMessageTextView, "Очистка настроек приложения...");
        clearSharedPreferences();
        
        
        updateLoadingMessage(finalMessageTextView, "Сброс менеджеров данных...");
        resetManagers();
        
        
        updateLoadingMessage(finalMessageTextView, "Очистка баз данных...");
        clearRoomDatabases(userId);
        
        
        updateLoadingMessage(finalMessageTextView, "Сброс авторизационных токенов...");
        clearSupabaseTokens();
        
        
        updateLoadingMessage(finalMessageTextView, "Очистка кэшей приложения...");
        try {
            
            clearApplicationData();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке кэша приложения: " + e.getMessage(), e);
        }
        
        
        updateLoadingMessage(finalMessageTextView, "Финальная очистка данных...");
        try {
            clearApplicationDataViaContentProvider();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке через ContentProvider: " + e.getMessage(), e);
        }
        
        
        updateLoadingMessage(finalMessageTextView, "Выход завершен. Возврат на экран входа...");
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        

        
        try {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при закрытии диалога: " + e.getMessage(), e);
        }

        
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); 
    }
    

    
    private void clearApplicationDataViaContentProvider() {
        try {
            
            getContentResolver().call(
                android.net.Uri.parse("content://" + getPackageName() + ".provider"), 
                "clearAllTables", 
                null, 
                null
            );
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке через ContentProvider: " + e.getMessage(), e);
        }
    }
    
    
    private void clearSharedPreferences() {
        try {
            
            String[] prefsFiles = {
                "user_data",                 
                "VitaMovePrefs",             
                "workout_history_cache",     
                "calories_data",             
                "water_history_prefs",       
                "dashboard_prefs",           
                "FoodManagerPrefs",          
                "food_meal_history"          
            };
            
            
            
            
            for (String prefName : prefsFiles) {
                SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                try {
                    editor.commit(); 
                    
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при очистке настроек " + prefName + ": " + e.getMessage(), e);
                    
                    editor = prefs.edit();
                    editor.clear();
                    editor.apply();
                }
            }
            
            
            safelyClearDefaultPreferences();
            
            
            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            if (prefsDir.exists() && prefsDir.isDirectory()) {
                File[] preferenceFiles = prefsDir.listFiles();
                if (preferenceFiles != null) {
                    for (File file : preferenceFiles) {
                        try {
                            String filename = file.getName();
                            if (filename.endsWith(".xml")) {
                                
                                if (file.delete()) {
                                    
                                }
                            }
                        } catch (Exception e) {
                            Log.e("SettingsActivity", "Ошибка при удалении файла настроек: " + e.getMessage(), e);
                        }
                    }
                }
            }
            
            
            try {
                
                SharedPreferences vmPrefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                SharedPreferences.Editor vmEditor = vmPrefs.edit();
                vmEditor.clear();
                vmEditor.putBoolean("isLogged", false);
                vmEditor.apply(); 
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при сбросе VitaMovePrefs: " + e.getMessage(), e);
            }
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке SharedPreferences: " + e.getMessage(), e);
        }
    }
    
    
    private void safelyClearDefaultPreferences() {
        try {
            SharedPreferences defaultPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = defaultPrefs.edit();
            editor.clear();
            editor.commit(); 
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при безопасной очистке стандартных настроек: " + e.getMessage(), e);
        }
    }
    
    
    private void resetManagers() {
        

        
        try {
            com.martist.vitamove.db.AppDatabase.resetInstance();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе AppDatabase: " + e.getMessage(), e);
        }

        
        try {
            com.martist.vitamove.managers.FoodManager.resetInstance();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе FoodManager: " + e.getMessage(), e);
        }
        
        
        try {
            com.martist.vitamove.managers.CaloriesManager.resetInstance();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе CaloriesManager: " + e.getMessage(), e);
        }
        
        
        try {
            com.martist.vitamove.managers.FoodManager.resetInstance();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при повторном сбросе FoodManager: " + e.getMessage(), e);
        }
        
        
        try {
            com.martist.vitamove.managers.DashboardManager.resetInstance();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе DashboardManager: " + e.getMessage(), e);
        }
        
        
        try {
            com.martist.vitamove.managers.WaterHistoryManager.resetInstance();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе WaterHistoryManager: " + e.getMessage(), e);
        }
        
        
        try {
            resetMealsDatabase();
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе MealsDatabase: " + e.getMessage(), e);
        }
        
        
        try {
            
            Runtime.getRuntime().gc();
            
            
            
            System.runFinalization();
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке кэшей: " + e.getMessage(), e);
        }
        
        
    }
    
    
    private void resetMealsDatabase() {
        try {
            
            Class<?> mealsDatabaseClass = Class.forName("com.martist.vitamove.database.MealsDatabase");
            
            
            Field instanceField = mealsDatabaseClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            
            
            instanceField.set(null, null);
            
            
            
            try {
                java.lang.reflect.Method resetMethod = mealsDatabaseClass.getDeclaredMethod("resetInstance");
                resetMethod.setAccessible(true);
                resetMethod.invoke(null);
                
            } catch (NoSuchMethodException e) {
                
                
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе MealsDatabase: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    
    private void clearRoomDatabases(String userId) {
        if (userId == null || userId.isEmpty()) {
            
            return;
        }
        
        try {
            
            com.martist.vitamove.viewmodels.HistoryViewModel.clearCache(getApplication(), userId);
            
            
            
            try {
                
                
                
                try {
                    com.martist.vitamove.database.MealsDatabase mealsDatabase = 
                        com.martist.vitamove.database.MealsDatabase.getInstance(getApplicationContext());
                    mealsDatabase.mealDao().deleteAllMealsForUser(userId);
                    
                    
                    com.martist.vitamove.database.MealsDatabase.resetInstance();
                    
                    
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при очистке meals через DAO: " + e.getMessage(), e);
                }
                
                
                android.database.sqlite.SQLiteDatabase mealsDb = getApplication().openOrCreateDatabase("meals_database", MODE_PRIVATE, null);
                try {
                    
                    mealsDb.execSQL("DELETE FROM meals");
                    
                    
                    mealsDb.execSQL("DELETE FROM meals WHERE user_id=?", new String[]{userId});
                    
                    
                    mealsDb.execSQL("VACUUM");
                    
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при очистке таблицы meals: " + e.getMessage(), e);
                } finally {
                    mealsDb.close();
                }
                
                

                
                
                File mealsDbFile = getDatabasePath("meals_database");
                if (mealsDbFile.exists()) {
                    boolean deleted = mealsDbFile.delete();
                    if (deleted) {
                        
                    } else {
                        Log.e("SettingsActivity", "Не удалось удалить файл meals_database");
                        
                        
                        boolean deletedAlt = getApplicationContext().deleteDatabase("meals_database");
                        
                    }
                }
                
                
                File mealsDbJournal = new File(mealsDbFile.getPath() + "-journal");
                if (mealsDbJournal.exists()) {
                    mealsDbJournal.delete();
                }
                
                File mealsDbShm = new File(mealsDbFile.getPath() + "-shm");
                if (mealsDbShm.exists()) {
                    mealsDbShm.delete();
                }
                
                File mealsDbWal = new File(mealsDbFile.getPath() + "-wal");
                if (mealsDbWal.exists()) {
                    mealsDbWal.delete();
                }
                
                
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при принудительной очистке meals_database: " + e.getMessage(), e);
            }
            
            
            String[] databases = {
                "meals.db", "food_cache.db", "exercise_cache.db", 
                "workout_database.db", "user_cache.db", "program_cache.db", 
                "workout_cache.db", "calories.db", "vitamove.db", 
                "WorkName", "WorkProgress", "WorkSpec", "WorkTag",
                "WorkManager", "room_master_table", "program_database.db",
                "meals_database.db"
            };
            
            for (String dbName : databases) {
                try {
                    
                    android.database.sqlite.SQLiteDatabase db = getApplication().openOrCreateDatabase(dbName, MODE_PRIVATE, null);
                    
                    
                    android.database.Cursor cursor = db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata' AND name!='sqlite_sequence'", 
                        null
                    );
                    
                    while (cursor.moveToNext()) {
                        String tableName = cursor.getString(0);
                        try {
                            db.execSQL("DELETE FROM " + tableName);
                            
                        } catch (Exception e) {
                            Log.e("SettingsActivity", "Ошибка при очистке таблицы " + tableName + ": " + e.getMessage());
                        }
                    }
                    
                    cursor.close();
                    
                    
                    try {
                        db.execSQL("VACUUM");
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "Ошибка при сжатии базы данных " + dbName + ": " + e.getMessage());
                    }
                    
                    db.close();
                    
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при работе с базой данных " + dbName + ": " + e.getMessage(), e);
                }
            }
            
            
            try {
                clearSpecificSharedPreferences("calories_data");
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при очистке SharedPreferences calories_data: " + e.getMessage(), e);
            }
            
            
            for (String dbName : databases) {
                try {
                    
                    boolean deleted = getApplication().getDatabasePath(dbName).delete();
                    if (deleted) {
                        
                    } else {
                        
                        
                        
                        boolean deletedAlt = getApplicationContext().deleteDatabase(dbName);
                        if (deletedAlt) {
                            
                        }
                    }
                    
                    
                    getApplication().getDatabasePath(dbName + "-journal").delete();
                    getApplication().getDatabasePath(dbName + "-shm").delete();
                    getApplication().getDatabasePath(dbName + "-wal").delete();
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при удалении базы данных " + dbName + ": " + e.getMessage(), e);
                }
            }
            
            
            try {
                
                File databaseDir = getApplication().getDatabasePath("dummy").getParentFile();
                if (databaseDir != null && databaseDir.exists() && databaseDir.isDirectory()) {
                    
                    File[] databaseFiles = databaseDir.listFiles();
                    if (databaseFiles != null) {
                        for (File file : databaseFiles) {
                            String fileName = file.getName();
                            
                            if (file.isFile() && 
                                (fileName.endsWith(".db") || 
                                 fileName.contains("-journal") ||
                                 fileName.contains("-shm") ||
                                 fileName.contains("-wal") ||
                                 fileName.contains(".sqlite"))) {
                                try {
                                    boolean success = file.delete();
                                    
                                } catch (Exception e) {
                                    Log.e("SettingsActivity", "Ошибка при удалении файла " + fileName + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при очистке директории баз данных: " + e.getMessage());
            }
            
            
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке баз данных Room: " + e.getMessage(), e);
        }
    }
    
    
    private void clearSpecificSharedPreferences(String prefsName) {
        try {
            SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.commit(); 
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке SharedPreferences " + prefsName + ": " + e.getMessage(), e);
        }
    }
    
    
    private void clearSupabaseTokens() {
        try {
            
            SupabaseClient supabaseClient = 
                SupabaseClient.getInstance(
                    "qjopbdiafgbbstkwmhpt", 
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFqb3BiZGlhZmdiYnN0a3dtaHB0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTU1MDg4ODAsImV4cCI6MjAzMTA4NDg4MH0.F0XS4F4k31O7ciI43vYjzJFyK5wHHvlU0Jl2AFYZF4A"
                );
                
            
            supabaseClient.setUserToken(null);
            supabaseClient.setRefreshToken(null);
            
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе токенов SupabaseClient: " + e.getMessage(), e);
        }
    }
    
    
    private void clearApplicationData() {
        try {
            
            File cache = getCacheDir();
            deleteDir(cache);
            
            
            if (getExternalCacheDir() != null) {
                deleteDir(getExternalCacheDir());
            }
            
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке кэша: " + e.getMessage(), e);
        }
    }
    
    
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
    
    
    private void removeMealsDatabaseSaveTags() {
        try {
            
            
            
            File roomSharedPrefs = new File(getApplicationInfo().dataDir + "/shared_prefs/androidx.room.RoomSharedPreferences.xml");
            if (roomSharedPrefs.exists() && roomSharedPrefs.delete()) {
                
            }
            
            
            File cacheDir = getCacheDir();
            File[] cacheFiles = cacheDir.listFiles((dir, name) -> 
                name.contains("meals") || name.contains("room") || name.contains("food"));
                
            if (cacheFiles != null) {
                for (File file : cacheFiles) {
                    if (file.delete()) {
                        
                    }
                }
            }
            
        
            
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при удалении тегов сохранения: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    private void updateLoadingMessage(final TextView messageTextView, final String message) {
        if (messageTextView == null) {
            
            return;
        }
        
        try {
            
            runOnUiThread(() -> {
                try {
                    messageTextView.setText(message);
                    
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при обновлении сообщения в диалоге: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при вызове runOnUiThread: " + e.getMessage(), e);
        }
    }

} 