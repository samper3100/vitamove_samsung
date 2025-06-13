package com.martist.vitamove.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.R;
import com.martist.vitamove.adapters.ChatAdapter;
import com.martist.vitamove.api.GigaChatService;
import com.martist.vitamove.models.ChatMessage;
import com.martist.vitamove.services.SupabaseService;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseClient;

import java.lang.reflect.Type;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;

public class AssistantFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private AppCompatImageButton sendButton;
    private ChatAdapter chatAdapter;
    private GigaChatService gigaChatService;
    private SupabaseService supabaseService;
    private boolean isProcessingMessage = false;
    private static final String TAG = "AssistantFragment";
    
    
    private static final String PREFS_NAME = "AssistantFragmentPrefs";
    private static final String KEY_CHAT_HISTORY = "chat_history";
    private Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        setupToolbar(view);
        initGigaChatService();
        
        SupabaseClient supabaseClient = SupabaseClient.getInstance(Constants.SUPABASE_CLIENT_ID, Constants.SUPABASE_CLIENT_SECRET);
        supabaseService = new SupabaseService(supabaseClient);
        
        
        restoreChatHistory();
        
        
        if (chatAdapter.getItemCount() == 0) {
            addAssistantMessage("Здравствуйте! Я ваш фитнес-ассистент в VitaMove. Готов помочь вам с вопросами о тренировках, питании и здоровом образе жизни. Что вас интересует?");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (gigaChatService != null) {
            gigaChatService.updateUserContext();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        saveChatHistory();
    }

    private void initViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
    }

    private void initGigaChatService() {
        gigaChatService = GigaChatService.getInstance(
            Constants.GIGACHAT_CLIENT_ID,
            Constants.GIGACHAT_CLIENT_SECRET
        );
        
        
        gigaChatService.initializeUserContext();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        chatRecyclerView.setLayoutManager(layoutManager);
        
        
        Markwon markwon = Markwon.builder(requireContext())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(requireContext()))
            .build();
            
        chatAdapter = new ChatAdapter(markwon);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    
    
    private void saveChatHistory() {
        if (getContext() == null) return;
        
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        
        List<ChatMessage> messages = chatAdapter.getMessages();
        
        
        String json = gson.toJson(messages);
        
        
        editor.putString(KEY_CHAT_HISTORY, json);
        editor.apply();
        
        
    }
    
    
    private void restoreChatHistory() {
        if (getContext() == null) return;
        
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CHAT_HISTORY, null);
        
        if (json != null) {
            
            Type type = new TypeToken<List<ChatMessage>>(){}.getType();
            List<ChatMessage> messages = gson.fromJson(json, type);
            
            
            if (messages != null && !messages.isEmpty()) {
                for (ChatMessage message : messages) {
                    chatAdapter.addMessage(message);
                }
                
                
                chatRecyclerView.post(() -> chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1));
                
                
                
                
                restoreChatHistoryToGigaChat(messages);
            }
        }
    }
    
    
    private void restoreChatHistoryToGigaChat(List<ChatMessage> messages) {
        
        gigaChatService.resetConversation();
        
        
        for (ChatMessage message : messages) {
            if (message.isFromUser()) {
                gigaChatService.addMessageToHistory("user", message.getText());
            } else {
                gigaChatService.addMessageToHistory("assistant", message.getText());
            }
        }
        
        
    }
    
    
    private void resetConversation() {
        if (gigaChatService != null) {
            gigaChatService.resetConversation();
            chatAdapter.clearMessages();
            
            addAssistantMessage("Разговор сброшен. Чем я могу помочь вам сегодня?");
            Toast.makeText(getContext(), "Разговор сброшен", Toast.LENGTH_SHORT).show();
            
            
            saveChatHistory();
        }
    }

    private String preprocessMessage(String message) {
        
        message = message.replaceAll("\\s+", " ").trim();
        
        
        message = message.replaceAll("\\?+", "?")
                        .replaceAll("!+", "!")
                        .replaceAll("\\.+", ".");
        
        
        String[] greetings = {"привет", "здравствуйте", "добрый день", "доброе утро", "добрый вечер"};
        for (String greeting : greetings) {
            message = message.toLowerCase().replaceAll("^" + greeting + "[,\\s]*", "");
        }
        
        
        message = message.toLowerCase()
                 .replace("я хотел бы узнать", "")
                 .replace("подскажите пожалуйста", "")
                 .replace("не могли бы вы", "")
                 .replace("скажите", "")
                 .replace("расскажите", "");
        
        return message.trim();
    }

    private void sendMessage() {
        if (isProcessingMessage) {
            Toast.makeText(getContext(), "Пожалуйста, подождите ответа...", Toast.LENGTH_SHORT).show();
            return;
        }

        final String originalMessage = messageInput.getText().toString().trim();
        if (originalMessage.isEmpty()) {
            return;
        }
        
        
        setUIState(false);

        
        
        final Handler timeoutHandler = new Handler(Looper.getMainLooper());
        final Runnable timeoutRunnable = () -> {
            if (isProcessingMessage && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Не удалось получить ответ. Пожалуйста, попробуйте еще раз.", Toast.LENGTH_LONG).show();
                    setUIState(true);
                });
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000); 

        supabaseService.canSendMessage(canSend -> {
            if (getActivity() == null) {
                
                timeoutHandler.removeCallbacks(timeoutRunnable);
                setUIState(true);
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (canSend) {
                    
                    String processedMessage = preprocessMessage(originalMessage);
                    
                    if (processedMessage.isEmpty()) {
                        processedMessage = originalMessage;
                    }
        
                    addUserMessage(originalMessage);
                    messageInput.setText("");

                    gigaChatService.sendMessage(processedMessage, new GigaChatService.ChatCallback() {
                        @Override
                        public void onResponse(final String response) {
                            
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    addAssistantMessage(response);
                                    saveChatHistory(); 
                                    setUIState(true);
                                });
                            } else {
                                
                                isProcessingMessage = false;
                            }
                        }

                        @Override
                        public void onError(final String error) {
                            
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_LONG).show();
                                    
                                    messageInput.setText(originalMessage);
                                    
                                    chatAdapter.removeLastMessage();
                                    setUIState(true);
                                });
                            } else {
                                
                                isProcessingMessage = false;
                            }
                        }
                    });

                } else {
                    
                    Toast.makeText(getContext(), "Вы достигли лимита отправки сообщений на сегодня. Попробуйте завтра.", Toast.LENGTH_LONG).show();
                    
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    setUIState(true);
                }
            });
        });
    }

    private void setUIState(boolean isEnabled) {
        isProcessingMessage = !isEnabled;
        if (sendButton != null) {
            sendButton.setEnabled(isEnabled);
            sendButton.setAlpha(isEnabled ? 1.0f : 0.5f);
        }
        if (messageInput != null) {
            messageInput.setEnabled(isEnabled);
            messageInput.setAlpha(isEnabled ? 1.0f : 0.7f);
        }
    }

    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true);
        chatAdapter.addMessage(chatMessage);
        chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void addAssistantMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false);
        chatAdapter.addMessage(chatMessage);
        chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    
    private void setupToolbar(View view) {
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        
        
        toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        
        
        ImageButton resetButton = view.findViewById(R.id.resetButton);
        if (resetButton != null) {
            resetButton.setOnClickListener(v -> resetConversation());
            resetButton.setVisibility(View.VISIBLE);
        }
        

    }
} 