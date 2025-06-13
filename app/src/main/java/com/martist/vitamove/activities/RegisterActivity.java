package com.martist.vitamove.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;

import org.json.JSONObject;

public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton registerButton;
    private MaterialButton googleRegisterButton;
    private TextView loginLink;
    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        setContentView(R.layout.activity_register);

        
        supabaseClient = SupabaseClient.getInstance(
            Constants.SUPABASE_CLIENT_ID,
            Constants.SUPABASE_CLIENT_SECRET
        );

        initializeViews();
        setupClickListeners();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        
        
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            
        }
    }
    
    private void initializeViews() {
        emailInput = findViewById(R.id.register_email_text);
        passwordInput = findViewById(R.id.register_password_text);
        confirmPasswordInput = findViewById(R.id.register_confirm_password_input);
        registerButton = findViewById(R.id.register_button);
        googleRegisterButton = findViewById(R.id.register_google_button);
        loginLink = findViewById(R.id.login_link);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegistration());
        
        googleRegisterButton.setOnClickListener(v -> {
            Toast.makeText(this, "Регистрация через Google пока недоступна", Toast.LENGTH_SHORT).show();
        });
        
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    private void attemptRegistration() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        
        if (email.isEmpty()) {
            showError("Пожалуйста, введите email");
            return;
        }

        if (password.isEmpty()) {
            showError("Пожалуйста, введите пароль");
            return;
        }

        if (confirmPassword.isEmpty()) {
            showError("Пожалуйста, подтвердите пароль");
            return;
        }

        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Пожалуйста, введите корректный email адрес");
            return;
        }

        
        if (password.length() < 6) {
            showError("Пароль должен содержать не менее 6 символов");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Пароли не совпадают");
            return;
        }

        
        registerButton.setEnabled(false);
        
        
        new Thread(() -> {
            try {
                
                String responseJson = supabaseClient.signUp(email, password);
                JSONObject jsonResponse = new JSONObject(responseJson);
                
                
                String accessToken = jsonResponse.getString("access_token");
                String refreshToken = jsonResponse.optString("refresh_token", "");
                
                
                String userId = null;
                try {
                    String[] jwtParts = accessToken.split("\\.");
                    if (jwtParts.length > 1) {
                        String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                        JSONObject jwtJson = new JSONObject(payload);
                        userId = jwtJson.getString("sub");
                        
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка получения ID пользователя из токена", e);
                }
                
                final String finalUserId = userId;
                
                runOnUiThread(() -> {
                    try {
                        
                        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLogged", true)
                                .putBoolean("isFirstRun", false)
                                .putString("accessToken", accessToken)
                                .putString("refreshToken", refreshToken)
                                .putString("userId", finalUserId)
                                .putString("userEmail", email)
                                .apply();

                        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                        
                        
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка сохранения данных: " + e.getMessage());
                        showError("Ошибка сохранения данных: " + e.getMessage());
                        registerButton.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Registration error: " + e.getMessage());
                String errorMessage = e.getMessage();
                
                
                String userFriendlyMessage;
                if (errorMessage.contains("уже существует")) {
                    userFriendlyMessage = "Этот email уже зарегистрирован. Попробуйте войти или используйте другой email.";
                } else if (errorMessage.contains("Неверный формат")) {
                    userFriendlyMessage = "Неверный формат email адреса. Пожалуйста, проверьте ввод.";
                } else if (errorMessage.contains("не менее 6 символов")) {
                    userFriendlyMessage = "Пароль должен содержать не менее 6 символов.";
                } else if (errorMessage.contains("422")) {
                    userFriendlyMessage = "Неверный формат данных. Проверьте правильность email и пароля.";
                } else {
                    userFriendlyMessage = "Ошибка регистрации: " + errorMessage;
                }
                
                final String finalMessage = userFriendlyMessage;
                runOnUiThread(() -> {
                    showError(finalMessage);
                    registerButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
