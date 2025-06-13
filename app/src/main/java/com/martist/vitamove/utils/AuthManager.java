package com.martist.vitamove.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.activities.LoginActivity;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class AuthManager {
    private static final String TAG = "AuthManager";
    private static AuthManager instance;
    private final SupabaseClient supabaseClient;
    private final ReadWriteLock tokenLock = new ReentrantReadWriteLock();
    private AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private static final String PREFS_NAME = "VitaMovePrefs";

    private AuthManager(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
    }

    public static synchronized AuthManager getInstance(SupabaseClient supabaseClient) {
        if (instance == null) {
            instance = new AuthManager(supabaseClient);
        }
        return instance;
    }

    
    public boolean ensureValidToken() {
        try {
            if (isTokenExpired()) {
                return refreshToken();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке/обновлении токена: " + e.getMessage(), e);
            return false;
        }
    }

    
    public boolean refreshToken() {
        
        if (isRefreshing.getAndSet(true)) {
            
            
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            isRefreshing.set(false);
            return true;
        }
        
        
        tokenLock.writeLock().lock();
        try {
            
            supabaseClient.refreshAccessToken();
            
            return true;
        } catch (SupabaseClient.TokenInvalidatedException e) {
            
            Log.e(TAG, "Refresh token недействителен, требуется повторная авторизация", e);
            clearAuthData();
            redirectToLogin();
            return false;
        } catch (SupabaseClient.AuthException | IOException | JSONException e) {
            Log.e(TAG, "Ошибка при обновлении токена: " + e.getMessage(), e);
            
            
            if (e.getMessage() != null && 
                (e.getMessage().contains("token") || 
                 e.getMessage().contains("auth") ||
                 e.getMessage().contains("401"))) {
                clearAuthData();
                redirectToLogin();
            }
            
            return false;
        } finally {
            tokenLock.writeLock().unlock();
            isRefreshing.set(false);
        }
    }

    
    private boolean isTokenExpired() {
        try {
            tokenLock.readLock().lock();
            String token = supabaseClient.getUserToken();
            
            if (token == null || token.isEmpty()) {
                return true;
            }
            
            
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                long lastTokenUpdate = prefs.getLong("tokenUpdateTime", 0);
                
                if (lastTokenUpdate == 0) {
                    return true; 
                }
                
                long currentTime = System.currentTimeMillis();
                
                
                return (currentTime - lastTokenUpdate) > 50 * 60 * 1000;
            }
            
            return false;
        } finally {
            tokenLock.readLock().unlock();
        }
    }

    
    private void clearAuthData() {
        try {
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                    .remove("accessToken")
                    .remove("refreshToken")
                    .apply();
            }
            
            
            supabaseClient.setUserToken(null);
            supabaseClient.setRefreshToken(null);
            
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке данных аутентификации", e);
        }
    }

    
    private void redirectToLogin() {
        try {
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                Intent loginIntent = new Intent(context, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(loginIntent);
                
                
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при перенаправлении на экран входа", e);
        }
    }
    
    
    public boolean handleAuthException(Exception exception) {
        if (exception instanceof SupabaseClient.TokenRefreshedException) {
            
            
            return true;
        } else if (exception instanceof SupabaseClient.TokenInvalidatedException 
                || exception instanceof SupabaseClient.AuthException) {
            Log.e(TAG, "Критическая ошибка аутентификации: " + exception.getMessage());
            clearAuthData();
            redirectToLogin();
            return false;
        }
        return false;
    }
} 