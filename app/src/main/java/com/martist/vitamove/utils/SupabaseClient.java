package com.martist.vitamove.utils;

import static com.martist.vitamove.VitaMoveApplication.context;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.models.UserProfile;
import com.martist.vitamove.repositories.UserRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {
    public static final String SUPABASE_URL = "https://qjopbdiafgbbstkwmhpt.supabase.co";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static SupabaseClient instance;
    private OkHttpClient client;
    private final String apiKey;
    private String userToken;
    private String refreshToken;

    public SupabaseClient(String clientId, String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    
    public void initializeWithInterceptor(AuthManager authManager) {
        if (authManager == null) {
            Log.e("SupabaseClient", "AuthManager не может быть null");
            return;
        }
        
        
        this.client = this.client.newBuilder()
                .addInterceptor(new TokenRefreshInterceptor(this, authManager))
                .build();
        
        
    }

    public void setUserToken(String token) {
        this.userToken = token;
        
        saveTokensToPrefs();
    }

    public String getUserToken() {
        return userToken;
    }

    public String getApiKey() {
        return apiKey;
    }

    public static synchronized SupabaseClient getInstance(String clientId, String apiKey) {
        if (instance == null) {
            instance = new SupabaseClient(clientId, apiKey);
        }
        return instance;
    }

    
    public String signIn(String email, String password) throws IOException, JSONException {
        

        JSONObject signInData = new JSONObject();
        signInData.put("email", email);
        signInData.put("password", password);

        RequestBody body = RequestBody.create(signInData.toString(), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                
                if (responseBody.contains("Invalid login credentials")) {
                    throw new IOException("Неверный email или пароль");
                } else if (responseBody.contains("Email not confirmed")) {
                    throw new IOException("Email не подтвержден");
                }
                throw new IOException("Ошибка входа: " + response.code());
            }

            JSONObject jsonResponse = new JSONObject(responseBody);
            if (!jsonResponse.has("access_token")) {
                throw new IOException("Неверный ответ сервера: отсутствует access_token");
            }

            String accessToken = jsonResponse.getString("access_token");
            String refreshToken = jsonResponse.optString("refresh_token", "");
            setUserToken(accessToken);
            setRefreshToken(refreshToken);

            
            String userId = null;
            try {
                String[] jwtParts = accessToken.split("\\.");
                if (jwtParts.length > 1) {
                    String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                    JSONObject jwtJson = new JSONObject(payload);
                    userId = jwtJson.getString("sub");
                    

                    
                    try {
                        JSONArray result = this.from("users")
                                .eq("id", userId)
                                .executeAndGetArray();

                        if (result.length() == 0) {

                            
                        } else {
                            
                        }
                    } catch (Exception e) {
                        Log.e("SupabaseClient", "Ошибка при проверке/создании записи пользователя: " + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка извлечения ID пользователя из токена: " + e.getMessage(), e);
            }

            
            return responseBody;
        }
    }

    public String signUp(String email, String password) throws IOException, JSONException {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 6 символов");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Некорректный формат email");
        }

        

        JSONObject signUpData = new JSONObject();
        signUpData.put("email", email);
        signUpData.put("password", password);

        
        JSONObject metadata = new JSONObject();
        metadata.put("app_version", "1.0");
        signUpData.put("data", metadata);

        RequestBody body = RequestBody.create(signUpData.toString(), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/signup")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            
            if (!response.isSuccessful()) {
                
                if (responseBody.contains("User already registered")) {
                    throw new IOException("Пользователь с таким email уже зарегистрирован");
                } else if (responseBody.contains("invalid_input")) {
                    throw new IOException("Некорректные данные для регистрации");
                }
                throw new IOException("Ошибка регистрации: " + response.code());
            }

            
            JSONObject jsonResponse = new JSONObject(responseBody);
            if (!jsonResponse.has("access_token")) {
                throw new IOException("Неверный ответ сервера: отсутствует access_token");
            }

            
            String accessToken = jsonResponse.getString("access_token");
            String refreshToken = jsonResponse.optString("refresh_token", "");
            setUserToken(accessToken);
            setRefreshToken(refreshToken);

            
            String userId = null;


            try {
                String[] jwtParts = accessToken.split("\\.");
                if (jwtParts.length > 1) {
                    String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                    JSONObject jwtJson = new JSONObject(payload);
                    userId = jwtJson.getString("sub");
                    

                    
                    try {
                        JSONObject userData = new JSONObject();
                        SharedPreferences prefs = null;

                        try {
                            
                            Context context = VitaMoveApplication.getAppContext();
                            if (context != null) {
                                prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            }
                        } catch (Exception e) {
                            
                            Log.e("UserProfile", "Не удалось получить контекст для доступа к настройкам", e);
                        }
                        String fitnessGoal = prefs.getString("fitness_goal", "weight_loss");
                        String level = prefs.getString("user_fitness_level", "intermediate");

                        UserRepository profile_rep = new UserRepository(context);
                        UserProfile profile = profile_rep.getCurrentUserProfile();
                        float bmi = BMICalculator.calculateBMI(profile.getCurrentWeight(), profile.getHeight());
                        userData.put("id", userId);
                        userData.put("name", profile.getName());
                        userData.put("age", profile.getAge());
                        userData.put("target_calories", profile.getTargetCalories()); 
                        userData.put("gender", profile.getGender()); 
                        userData.put("fitness_goal", profile.getWaist()); 
                        userData.put("height", profile.getName()); 
                        userData.put("fitness_goal", fitnessGoal); 
                        userData.put("height", profile.getHeight()); 
                        userData.put("current_weight", profile.getCurrentWeight()); 
                        userData.put("target_weight", profile.getTargetWeight()); 
                        userData.put("fitness_level", level); 
                        userData.put("bmi", bmi); 
                        userData.put("target_water", profile.getTargetWater()); 

                        this.from("users")
                                .insert(userData)
                                .executeInsert();

                        
                    } catch (Exception e) {
                        
                        Log.e("SupabaseClient", "Не удалось создать запись в таблице users: " + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка извлечения ID пользователя из токена: " + e.getMessage(), e);
            }

            
            return responseBody;
        }
    }

    public QueryBuilder from(String table) {
        return new QueryBuilder(table);
    }



    public class QueryBuilder {
        private final String table;
        private final StringBuilder query;
        private final List<String> conditions;
        private String selectClause = "*";
        private String orderClause = null;
        private boolean orderAscending = true;
        private String jsonData;
        private String method;
        private final JSONObject headers = new JSONObject();

        public QueryBuilder(String table) {
            this.table = table;
            this.query = new StringBuilder();
            this.conditions = new ArrayList<>();
        }

        private Request.Builder getRequestBuilder() {
            StringBuilder urlBuilder = new StringBuilder(SUPABASE_URL)
                    .append("/rest/v1/")
                    .append(table);

            if (!conditions.isEmpty() || selectClause != null || orderClause != null) {
                urlBuilder.append("?");
                if (selectClause != null) {
                    urlBuilder.append("select=").append(selectClause);
                }
                for (String condition : conditions) {
                    urlBuilder.append("&").append(condition);
                }
                if (orderClause != null) {
                    urlBuilder.append("&order=").append(orderClause)
                            .append(".").append(orderAscending ? "asc" : "desc");
                }
            }

            Request.Builder builder = new Request.Builder()
                    .url(urlBuilder.toString())
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json");

            if (jsonData != null) {
                builder.addHeader("Prefer", "return=representation");
            }

            return builder;
        }

        public QueryBuilder select(String fields) {
            this.selectClause = fields;
            return this;
        }

        public QueryBuilder eq(String field, Object value) {
            conditions.add(field + "=eq." + value);
            return this;
        }

        public QueryBuilder gt(String field, Object value) {
            conditions.add(field + "=gt." + value);
            return this;
        }

        public QueryBuilder gte(String field, Object value) {
            conditions.add(field + "=gte." + value);
            return this;
        }

        public QueryBuilder lt(String field, Object value) {
            conditions.add(field + "=lt." + value);
            return this;
        }

        public QueryBuilder lte(String field, Object value) {
            conditions.add(field + "=lte." + value);
            return this;
        }

        public QueryBuilder ilike(String field, String value) {
            
            conditions.add(field + "=ilike." + "*" + value + "*");
            return this;
        }

        public QueryBuilder is(String field, String value) {
            conditions.add(field + "=is." + value);
            return this;
        }

        
        public QueryBuilder limit(int count) {
            query.append("&limit=").append(count);
            return this;
        }

        
        public QueryBuilder offset(int count) {
            conditions.add("offset=" + count);
            return this;
        }

        public QueryBuilder order(String field, boolean ascending) {
            this.orderClause = field;
            this.orderAscending = ascending;
            return this;
        }

        public void execute() throws Exception {
            Request.Builder requestBuilder = getRequestBuilder();

            if (jsonData != null) {
                RequestBody body = RequestBody.create(jsonData, JSON);
                requestBuilder.post(body);
            } else {
                requestBuilder.get();
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }

        
        public QueryBuilder insert(JSONObject jsonData) {
            try {
                this.jsonData = jsonData.toString();
                this.method = "POST";
                this.headers.put("Prefer", "return=representation");
                
                return this;
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при создании JSON для вставки", e);
                throw new RuntimeException("Ошибка при создании JSON для вставки", e);
            }
        }

        
        public JSONObject executeAndGetSingle() throws Exception {
            int maxRetries = 3;
            int currentTry = 0;
            Exception lastException = null;

            
            String requestUrl = getRequestBuilder().build().url().toString();
            

            while (currentTry < maxRetries) {
                try {
                    Request.Builder requestBuilder = getRequestBuilder();

                    if (jsonData != null) {
                        RequestBody body = RequestBody.create(jsonData, JSON);
                        requestBuilder.post(body);
                        
                    } else {
                        requestBuilder.get();
                        
                    }

                    
                    try (Response response = client.newCall(requestBuilder.build()).execute()) {
                        int responseCode = response.code();
                        

                        if (!response.isSuccessful()) {
                            String responseBody = response.body() != null ? response.body().string() : "null";
                            Log.e("SupabaseClient", "Неудачный запрос, код: " + responseCode + ", тело: " + responseBody);

                            if (responseCode == 401) {
                                throw new IOException("Ошибка авторизации. Будет выполнена автоматическая повторная попытка.");
                            } else if (responseCode >= 500) {
                                
                                
                                Thread.sleep(1000L * (currentTry + 1));
                                currentTry++;
                                continue;
                            } else {
                                throw new IOException("Запрос не удался: " + response.code() + ", тело: " + responseBody);
                            }
                        }

                        String responseBody = response.body().string();
                        

                        if (responseBody == null || responseBody.isEmpty()) {
                            Log.e("SupabaseClient", "Ответ пуст");
                            throw new IOException("Пустой ответ");
                        }

                        
                        if (responseBody.trim().startsWith("[")) {
                            
                            JSONArray array = new JSONArray(responseBody);
                            if (array.length() > 0) {
                                return array.getJSONObject(0);
                            } else {
                                
                                
                                return null;
                            }
                        } else {
                            
                            return new JSONObject(responseBody);
                        }
                    }
                } catch (Exception e) {
                    lastException = e;
                    Log.e("SupabaseClient", "Ошибка запроса: " + e.getMessage(), e);
                    if (currentTry < maxRetries - 1) {
                        
                        Thread.sleep(1000L * (currentTry + 1));
                        currentTry++;
                    } else {
                        break;
                    }
                }
            }

            Log.e("SupabaseClient", "Все попытки запроса не удались", lastException);
            throw new IOException("Запрос не удался после " + maxRetries + " попыток: " +
                    (lastException != null ? lastException.getMessage() : "неизвестная ошибка"));
        }

        
        public JSONArray executeAndGetArray() throws Exception {
            int maxRetries = 3;
            int currentTry = 0;
            Exception lastException = null;

            
            String requestUrl = getRequestBuilder().build().url().toString();
            

            while (currentTry < maxRetries) {
                try {
                    Request.Builder requestBuilder = getRequestBuilder();

                    if (jsonData != null) {
                        RequestBody body = RequestBody.create(jsonData, JSON);

                        
                        if ("POST".equals(method)) {
                            requestBuilder.post(body);
                            
                        } else if ("PATCH".equals(method)) {
                            requestBuilder.patch(body);
                            
                        } else if ("PUT".equals(method)) {
                            requestBuilder.put(body);
                            
                        } else {
                            requestBuilder.post(body);
                            
                        }
                    } else {
                        requestBuilder.get();
                        
                    }

                    
                    try (Response response = client.newCall(requestBuilder.build()).execute()) {
                        int responseCode = response.code();
                        

                        if (!response.isSuccessful()) {
                            String responseBody = response.body() != null ? response.body().string() : "null";
                            Log.e("SupabaseClient", "Неудачный запрос, код: " + responseCode + ", тело: " + responseBody);

                            if (responseCode == 401) {
                                throw new IOException("Ошибка авторизации. Будет выполнена автоматическая повторная попытка.");
                            } else if (responseCode >= 500) {
                                
                                
                                Thread.sleep(1000L * (currentTry + 1));
                                currentTry++;
                                continue;
                            } else {
                                throw new IOException("Запрос не удался: " + response.code() + ", тело: " + responseBody);
                            }
                        }

                        String responseBody = response.body().string();
                        

                        if (responseBody == null || responseBody.isEmpty()) {
                            Log.e("SupabaseClient", "Ответ пуст");
                            throw new IOException("Пустой ответ");
                        }

                        
                        if (responseBody.equals("[]")) {
                            
                            return new JSONArray();
                        }

                        return new JSONArray(responseBody);
                    }
                } catch (Exception e) {
                    lastException = e;
                    Log.e("SupabaseClient", "Ошибка запроса: " + e.getMessage(), e);
                    if (currentTry < maxRetries - 1) {
                        
                        Thread.sleep(1000L * (currentTry + 1));
                        currentTry++;
                    } else {
                        break;
                    }
                }
            }

            Log.e("SupabaseClient", "Все попытки запроса не удались", lastException);
            throw new IOException("Запрос не удался после " + maxRetries + " попыток: " +
                    (lastException != null ? lastException.getMessage() : "неизвестная ошибка"));
        }

        
        public QueryBuilder update(JSONObject jsonData) {
            try {
                this.jsonData = jsonData.toString();
                this.method = "PATCH";
                this.headers.put("Prefer", "return=representation");
                return this;
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при создании JSON для обновления", e);
                throw new RuntimeException("Ошибка при создании JSON для обновления", e);
            }
        }

        public QueryBuilder delete() {
            return this;
        }

        public void executeDelete() throws Exception {
            Request.Builder requestBuilder = getRequestBuilder()
                    .addHeader("Prefer", "return=minimal")
                    .delete();

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }

        public void executeUpdate() throws Exception {
            if (jsonData == null) {
                throw new Exception("No data to update");
            }

            RequestBody body = RequestBody.create(jsonData, JSON);
            Request.Builder requestBuilder = getRequestBuilder()
                    .addHeader("Prefer", "return=representation")
                    .patch(body);

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }

        public void executeInsert() throws Exception {
            if (jsonData == null) {
                throw new Exception("No data to insert");
            }

            RequestBody body = RequestBody.create(jsonData, JSON);
            Request.Builder requestBuilder = getRequestBuilder()
                    .addHeader("Prefer", "return=representation")
                    .post(body);

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }

        private void handleResponse(Response response) throws Exception {
            if (!response.isSuccessful()) {
                String responseBody = "";
                try {
                    responseBody = response.body().string();
                } catch (Exception e) {
                    
                }

                
                if (response.code() == 300) {
                    
                    throw new IOException("Проблема с перенаправлением запроса. Тело ответа: " + responseBody);
                } else if (response.code() >= 500) {
                    Log.e("SupabaseClient", "Серверная ошибка " + response.code() + ": " + responseBody);
                    throw new IOException("Серверная ошибка: " + response.code());
                } else {
                    Log.e("SupabaseClient", "Запрос не удался с кодом " + response.code() + ": " + responseBody);
                    throw new IOException("Запрос не удался: " + response.code() + ". Тело ответа: " + responseBody);
                }
            }
        }

        

    }

    public void setRefreshToken(String token) {
        this.refreshToken = token;
        
        saveTokensToPrefs();
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String refreshAccessToken() throws IOException, JSONException, AuthException {
        if (refreshToken == null) {
            throw new IOException("Refresh token is not available");
        }

        JSONObject refreshData = new JSONObject();
        refreshData.put("refresh_token", refreshToken);
        refreshData.put("grant_type", "refresh_token");

        RequestBody body = RequestBody.create(
                refreshData.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        
        

        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            try {
                responseBody = response.body().string();
                
            } catch (Exception e) {
                Log.e("SupabaseClient", "Не удалось прочитать тело ответа", e);
            }

            if (!response.isSuccessful()) {
                Log.e("SupabaseClient", "Ошибка обновления токена: " + response.code() + " - " + responseBody);

                if (response.code() == 400) {
                    
                    if (responseBody.contains("refresh_token_already_used") ||
                            responseBody.contains("Already Used") ||
                            responseBody.contains("Invalid Refresh Token")) {

                        
                        Log.e("SupabaseClient", "Refresh token был использован ранее, требуется повторная авторизация");

                        
                        this.userToken = null;
                        this.refreshToken = null;

                        
                        try {
                            Context context = VitaMoveApplication.getContext();
                            if (context != null) {
                                SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
                                prefs.edit()
                                        .remove("accessToken")
                                        .remove("refreshToken")
                                        .apply();
                                
                            }
                        } catch (Exception e) {
                            Log.e("SupabaseClient", "Ошибка при удалении токенов из SharedPreferences", e);
                        }

                        throw new TokenInvalidatedException("Refresh token недействителен. Необходима повторная авторизация.");
                    }

                    
                    throw new AuthException("Токен обновления недействителен или истек. Необходима повторная авторизация.");
                }

                throw new IOException("Failed to refresh token: " + response.code() + " - " + responseBody);
            }

            JSONObject jsonResponse = new JSONObject(responseBody);

            String newAccessToken = jsonResponse.getString("access_token");
            String newRefreshToken = jsonResponse.getString("refresh_token");

            

            setUserToken(newAccessToken);
            setRefreshToken(newRefreshToken);

            return newAccessToken;
        } catch (JSONException e) {
            Log.e("SupabaseClient", "Ошибка парсинга JSON ответа", e);
            throw new IOException("Ошибка при обработке ответа сервера: " + e.getMessage(), e);
        }
    }

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }


    }

    public static class TokenRefreshedException extends AuthException {
        public TokenRefreshedException() {
            super("Token refreshed, please retry the request");
        }
    }



    public static class TokenInvalidatedException extends AuthException {
        public TokenInvalidatedException(String message) {
            super(message);
        }
    }

    public RpcBuilder rpc(String functionName) {
        return new RpcBuilder(functionName);
    }

    public class RpcBuilder {
        private final String functionName;
        private final JSONObject params;

        public RpcBuilder(String functionName) {
            this.functionName = functionName;
            this.params = new JSONObject();
        }

        public RpcBuilder param(String name, Object value) {
            try {
                params.put(name, value);
            } catch (JSONException e) {
                Log.e("SupabaseClient", "Ошибка при добавлении параметра: " + e.getMessage());
            }
            return this;
        }

        public JSONArray executeAndGetArray() throws Exception {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/rpc/" + functionName)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            RequestBody body = RequestBody.create(params.toString(), JSON);
            Request request = requestBuilder.post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e("SupabaseClient", "Ошибка RPC: " + errorBody);
                    throw new IOException("Ошибка при вызове RPC функции: " + response.code() + " " + errorBody);
                }

                String responseBody = response.body().string();
                return new JSONArray(responseBody);
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при выполнении RPC: " + e.getMessage());
                throw e;
            }
        }

        public JSONObject executeAndGetSingle() throws Exception {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/rpc/" + functionName)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            RequestBody body = RequestBody.create(params.toString(), JSON);
            Request request = requestBuilder.post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e("SupabaseClient", "Ошибка RPC: " + errorBody);
                    throw new IOException("Ошибка при вызове RPC функции: " + response.code() + " " + errorBody);
                }

                String responseBody = response.body().string();
                return new JSONObject(responseBody);
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при выполнении RPC: " + e.getMessage());
                throw e;
            }
        }

        
        public <T> void executeAsync(SupabaseCallback<T> callback) {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/rpc/" + functionName)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            RequestBody body = RequestBody.create(params.toString(), JSON);
            Request request = requestBuilder.post(body).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("SupabaseClient", "Ошибка выполнения RPC запроса: " + e.getMessage(), e);

                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onFailure(e);
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    try (Response responseClone = response) {
                        int responseCode = responseClone.code();
                        

                        if (!responseClone.isSuccessful()) {
                            String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";
                            Log.e("SupabaseClient", "Неудачный RPC запрос, код: " + responseCode + ", тело: " + responseBody);

                            
                            Exception error = new IOException("Ошибка сервера: " + responseCode + " - " + responseBody);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                callback.onFailure(error);
                            });
                            return;
                        }

                        try {
                            String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";

                            
                            if (responseBody.startsWith("{")) {
                                
                                JSONObject jsonResult = new JSONObject(responseBody);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    try {
                                        callback.onSuccess((T) jsonResult);
                                    } catch (ClassCastException e) {
                                        callback.onFailure(new Exception("Неправильный тип данных в ответе", e));
                                    }
                                });
                            } else if (responseBody.startsWith("[")) {
                                
                                JSONArray jsonArray = new JSONArray(responseBody);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    try {
                                        callback.onSuccess((T) jsonArray);
                                    } catch (ClassCastException e) {
                                        callback.onFailure(new Exception("Неправильный тип данных в ответе", e));
                                    }
                                });
                            } else {
                                
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    try {
                                        callback.onSuccess((T) responseBody);
                                    } catch (ClassCastException e) {
                                        callback.onFailure(new Exception("Неправильный тип данных в ответе", e));
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e("SupabaseClient", "Ошибка обработки ответа RPC: " + e.getMessage(), e);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                callback.onFailure(e);
                            });
                        }
                    }
                }
            });
        }
    }

    
    private void executeAsync(Request request, AsyncCallback callback) {
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("SupabaseClient", "Ошибка выполнения запроса: " + e.getMessage(), e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (Response responseClone = response) {
                    int responseCode = responseClone.code();
                    

                    if (!responseClone.isSuccessful()) {
                        String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";
                        Log.e("SupabaseClient", "Неудачный запрос, код: " + responseCode + ", тело: " + responseBody);

                        
                        callback.onFailure(new IOException("Ошибка сервера: " + responseCode + " - " + responseBody));
                        return;
                    }

                    String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";
                    callback.onSuccess(responseBody);
                } catch (Exception e) {
                    Log.e("SupabaseClient", "Ошибка обработки ответа: " + e.getMessage(), e);
                    callback.onFailure(e);
                }
            }
        });
    }

    
    public interface AsyncCallback {
        void onSuccess(String responseBody);

        void onFailure(Exception e);
    }





    
    public JSONObject insertRecord(String tableName, JSONObject data) throws IOException, JSONException {
        

        
        String url = SUPABASE_URL + "/rest/v1/" + tableName;

        
        RequestBody body = RequestBody.create(
                data.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка запроса: " + response.code());
            }

            String responseBody = response.body().string();
            JSONArray jsonArray = new JSONArray(responseBody);
            JSONObject jsonResponse = new JSONObject();

            if (jsonArray.length() > 0) {
                jsonResponse.put("data", jsonArray.getJSONObject(0));
            } else {
                jsonResponse.put("data", new JSONObject());
            }

            return jsonResponse;
        }
    }



    
    private void saveTokensToPrefs() {
        try {
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
                if (userToken != null) {
                    prefs.edit()
                            .putString("accessToken", userToken)
                            .apply();
                    
                }
                if (refreshToken != null) {
                    prefs.edit()
                            .putString("refreshToken", refreshToken)
                            .putLong("tokenUpdateTime", System.currentTimeMillis())
                            .apply();
                    
                }
            }
        } catch (Exception e) {
            Log.e("SupabaseClient", "Ошибка при сохранении токенов в SharedPreferences", e);
        }
    }


    
    public boolean updateUserProfile(String userId, String name, int age, String gender,
                                     String fitnessGoal, float height, float currentWeight,
                                     float targetWeight, String fitnessLevel, boolean isMetric) {
        

        try {
            
            float bmi = 0;
            if (height > 0 && currentWeight > 0) {
                if (isMetric) {
                    
                    bmi = currentWeight / ((height / 100) * (height / 100));
                } else {
                    
                    
                    bmi = currentWeight / ((height / 100) * (height / 100));
                }
            }

            
            UserProfile tempProfile = new UserProfile(name, age, gender, currentWeight, targetWeight, height, 0, 0);
            int targetCalories = tempProfile.calculateTargetCalories();
            float targetWater = tempProfile.calculateTargetWater();

            
            JSONObject data = new JSONObject();
            data.put("id", userId);
            data.put("name", name);
            data.put("age", age);
            data.put("gender", gender);
            data.put("fitness_goal", fitnessGoal);
            data.put("height", height);
            data.put("current_weight", currentWeight);
            data.put("target_weight", targetWeight);
            data.put("bmi", bmi);
            data.put("fitness_level", fitnessLevel);
            data.put("is_metric", isMetric);
            data.put("target_calories", targetCalories); 
            data.put("target_water", targetWater); 

            
            try {
                from("users")
                        .eq("id", userId)
                        .update(data)
                        .executeUpdate();

                
                return true;
            } catch (Exception e) {
                
                try {
                    data.remove("id"); 

                    from("users")
                            .insert(data)
                            .executeInsert();

                    
                    return true;
                } catch (Exception insertError) {
                    Log.e("SupabaseClient", "Ошибка при создании профиля: " + insertError.getMessage(), insertError);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e("SupabaseClient", "Ошибка при обновлении профиля: " + e.getMessage(), e);
            return false;
        }
    }

    
    public void deleteUserAccount(String userId, AsyncCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("ID пользователя не может быть пустым"));
            }
            return;
        }
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("userId", userId);
            
            
            Request request = new Request.Builder()
                .url(SUPABASE_URL + "/functions/v1/delete-user")
                .post(RequestBody.create(requestBody.toString(), JSON))
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();
            
            
            executeAsync(request, callback);
            
        } catch (JSONException e) {
            Log.e("SupabaseClient", "Ошибка при создании запроса для удаления пользователя: " + e.getMessage(), e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }
}