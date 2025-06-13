package com.martist.vitamove.services;

import android.util.Log;

import com.martist.vitamove.utils.SupabaseCallback;
import com.martist.vitamove.utils.SupabaseClient;

public class SupabaseService {

    private static final String TAG = "SupabaseService";
    private final SupabaseClient client;


    public SupabaseService(SupabaseClient client) {
        this.client = client;
    }

    public interface CanSendMessageCallback {
        void onResult(boolean canSend);
    }

    public void canSendMessage(CanSendMessageCallback callback) {
        if (client == null) {
            Log.e(TAG, "SupabaseClient is not initialized.");
            callback.onResult(false);
            return;
        }


        client.rpc("can_send_assistant_message")
                .executeAsync(new SupabaseCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        

                        boolean canSend = Boolean.parseBoolean(result);
                        callback.onResult(canSend);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failure calling RPC", e);
                        callback.onResult(false);
                    }
                });
    }
} 