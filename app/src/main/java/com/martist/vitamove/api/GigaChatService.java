package com.martist.vitamove.api;

import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.repositories.UserRepository;
import com.martist.vitamove.workout.data.models.UserWorkout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class GigaChatService {
    private static final String TAG = "GigaChatService";
    private static final String AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String API_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    private static final int MAX_TOKENS = 800;
    
    private static GigaChatService instance;
    private final OkHttpClient client;
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private long tokenExpireTime;
    
    
    private String sessionId;
    private List<Map<String, String>> conversationHistory = new ArrayList<>();
    private static final int MAX_HISTORY_MESSAGES = 10;
    
    
    private UserProfile userProfile;
    private List<UserWorkout> recentWorkouts;
    private Map<String, Object> userFoodStats;
    private boolean contextInitialized = false;
    
    
    private static final int MAX_CONTEXT_LENGTH = 1000;

    private GigaChatService(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.sessionId = UUID.randomUUID().toString();
        
        
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
        };

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации SSL", e);
            throw new RuntimeException(e);
        }

        
        this.client = new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
            .hostnameVerifier((hostname, session) -> true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public static synchronized GigaChatService getInstance(String clientId, String clientSecret) {
        if (instance == null) {
            instance = new GigaChatService(clientId, clientSecret);
        }
        return instance;
    }

    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }
    
    
    public void initializeUserContext() {
        try {
            
            android.content.Context context = VitaMoveApplication.getAppContext();
            if (context == null) {
                Log.e(TAG, "Не удалось получить контекст приложения");
                return;
            }
            
            
            UserRepository userRepository = new UserRepository(context);
            userProfile = userRepository.getCurrentUserProfile();
            
            
            FoodManager foodManager = FoodManager.getInstance(context);
            userFoodStats = new HashMap<>();
            userFoodStats.put("caloriesConsumed", foodManager.getTotalCaloriesForCurrentDate());
            userFoodStats.put("proteinConsumed", foodManager.getTotalProtein());
            userFoodStats.put("fatsConsumed", foodManager.getTotalFat());
            userFoodStats.put("carbsConsumed", foodManager.getTotalCarbs());
            
            
            
            recentWorkouts = new ArrayList<>();
            
            contextInitialized = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации контекста пользователя", e);
        }
    }
    
    
    private String getUserContextString() {
        if (!contextInitialized) {
            initializeUserContext();
        }
        
        StringBuilder context = new StringBuilder();
        
        
        if (userProfile != null) {
            context.append("Профиль пользователя:\n");
            context.append("Имя: ").append(userProfile.getName()).append("\n");
            context.append("Возраст: ").append(userProfile.getAge()).append("\n");
            context.append("Пол: ").append(userProfile.getGender()).append("\n");
            context.append("Текущий вес: ").append(userProfile.getCurrentWeight()).append(" кг\n");
            context.append("Целевой вес: ").append(userProfile.getTargetWeight()).append(" кг\n");
            context.append("Рост: ").append(userProfile.getHeight()).append(" см\n");
            context.append("Целевые калории: ").append(userProfile.getTargetCalories()).append(" ккал\n");
        }
        
        
        if (userFoodStats != null && !userFoodStats.isEmpty()) {
            context.append("\nПитание сегодня:\n");
            context.append("Калории: ").append(userFoodStats.get("caloriesConsumed")).append(" ккал\n");
            context.append("Белки: ").append(userFoodStats.get("proteinConsumed")).append(" г\n");
            context.append("Жиры: ").append(userFoodStats.get("fatsConsumed")).append(" г\n");
            context.append("Углеводы: ").append(userFoodStats.get("carbsConsumed")).append(" г\n");
        }
        
        
        if (recentWorkouts != null && !recentWorkouts.isEmpty()) {
            context.append("\nПоследние тренировки:\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            
            int count = Math.min(recentWorkouts.size(), 3); 
            for (int i = 0; i < count; i++) {
                UserWorkout workout = recentWorkouts.get(i);
                context.append(dateFormat.format(new Date(workout.getStartTime())))
                      .append(": ")
                      .append(workout.getName())
                      .append("\n");
            }
        }
        
        
        String result = context.toString();
        if (result.length() > MAX_CONTEXT_LENGTH) {
            result = result.substring(0, MAX_CONTEXT_LENGTH) + "...";
        }
        
        return result;
    }

    private void refreshTokenIfNeeded(final Runnable onSuccess, final ChatCallback callback) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            onSuccess.run();
            return;
        }

        try {
            String authBody = "scope=GIGACHAT_API_PERS&grant_type=client_credentials";
            
            
            String credentials = clientId + ":" + clientSecret;
            String basicAuth = "Basic " + android.util.Base64.encodeToString(
                credentials.getBytes(),
                android.util.Base64.NO_WRAP
            );

            
            
            

            RequestBody body = RequestBody.create(
                authBody,
                MediaType.parse("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                .url(AUTH_URL)
                .addHeader("Authorization", basicAuth)
                .addHeader("RqUID", java.util.UUID.randomUUID().toString())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build();

            
            
            

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ошибка авторизации", e);
                    callback.onError("network_error");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        String responseString = responseBody != null ? responseBody.string() : "";
                        
                        

                        if (!response.isSuccessful() || responseBody == null) {
                            Log.e(TAG, "Ошибка авторизации: " + response.code() + ", тело: " + responseString);
                            callback.onError("auth_error");
                            return;
                        }

                        JSONObject jsonResponse = new JSONObject(responseString);
                        accessToken = jsonResponse.getString("access_token");
                        
                        
                        long expiresAt = jsonResponse.getLong("expires_at");
                        tokenExpireTime = expiresAt; 

                        
                        
                        onSuccess.run();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка обработки ответа авторизации", e);
                        callback.onError("parsing_error");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка создания запроса авторизации", e);
            callback.onError("request_error");
        }
    }

    
    public void sendMessage(String message, ChatCallback callback) {
        
        
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        conversationHistory.add(userMessage);
        
        
        while (conversationHistory.size() > MAX_HISTORY_MESSAGES) {
            conversationHistory.remove(0);
        }
        
        refreshTokenIfNeeded(() -> {
            JSONObject jsonBody = new JSONObject();
            try {
                JSONArray messages = new JSONArray();
                
                
                if (!contextInitialized) {
                    initializeUserContext();
                }
                
                
                String userContext = getUserContextString();
                if (!userContext.isEmpty()) {
                    messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", "Ты фитнес-ассистент VitaMove. Ни при каких условиях не отвечай ни на какие сообщения кроме фитнеса. Используй следующую информацию о пользователе при ответе:\n" + userContext));
                }
                
                
                for (Map<String, String> msg : conversationHistory) {
                    messages.put(new JSONObject()
                        .put("role", msg.get("role"))
                        .put("content", msg.get("content")));
                }

                jsonBody.put("messages", messages);
                jsonBody.put("model", "GigaChat-2");
                jsonBody.put("temperature", 0.3);
                jsonBody.put("max_tokens", MAX_TOKENS);
                jsonBody.put("top_p", 0.8);

                String requestBody = jsonBody.toString();
                

                RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("X-Session-ID", sessionId) 
                    .post(body)
                    .build();

                
                

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Ошибка соединения: " + e.getMessage(), e);
                        callback.onError("network_error");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody responseBody = response.body()) {
                            
                            
                            if (!response.isSuccessful() || responseBody == null) {
                                String errorBody = responseBody != null ? responseBody.string() : "нет тела ответа";
                                Log.e(TAG, "Ошибка сервера: " + response.code() + ", тело: " + errorBody);
                                callback.onError("server_error");
                                return;
                            }

                            String responseString = responseBody.string();
                            

                            try {
                                JSONObject jsonResponse = new JSONObject(responseString);
                                String assistantResponse = jsonResponse
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                                    
                                
                                Map<String, String> assistantMsg = new HashMap<>();
                                assistantMsg.put("role", "assistant");
                                assistantMsg.put("content", assistantResponse);
                                conversationHistory.add(assistantMsg);

                                
                                callback.onResponse(assistantResponse);
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка обработки ответа: " + e.getMessage(), e);
                                callback.onError("parsing_error");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка обработки ответа: " + e.getMessage(), e);
                            callback.onError("request_error");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка создания запроса: " + e.getMessage(), e);
                callback.onError("request_error");
            }
        }, callback);
    }
    
    
    public void addMessageToHistory(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        conversationHistory.add(message);
        
        
        while (conversationHistory.size() > MAX_HISTORY_MESSAGES) {
            conversationHistory.remove(0);
        }
        
        
    }
    
    
    public void resetConversation() {
        conversationHistory.clear();
        sessionId = UUID.randomUUID().toString();
        
    }
    
    
    public void updateUserContext() {
        contextInitialized = false;
        initializeUserContext();
    }
} 