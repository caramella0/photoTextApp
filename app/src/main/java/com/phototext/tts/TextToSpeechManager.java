package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TextToSpeechManager implements BaseTTSManager {
    private static final String TAG = "TextToSpeechManager";
    private final Context context;
    private GoogleTTSManager googleTTS;
    private KokoroTTSManager kokoroTTS;
    private String selectedTTS;
    private String voiceGender;
    private boolean isGoogleTTSReady = false;

    public TextToSpeechManager(Context context) {
        this.context = context;
        Log.d(TAG, "Initializing TextToSpeechManager");
        loadSettings();

        if ("kokoro".equals(selectedTTS)) {
            Log.d(TAG, "Using Kokoro TTS engine");
            kokoroTTS = new KokoroTTSManager(context);
        } else {
            Log.d(TAG, "Using Google TTS engine");
            googleTTS = new GoogleTTSManager(context);
        }
    }

    public void speak(String text) {
        Log.d(TAG, "Request to speak text: " + (text != null ? text.substring(0, Math.min(text.length(), 50)) + "..." : "null"));
        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Empty or null text provided");
            return;
        }

        if ("kokoro".equals(selectedTTS)) {
            if (kokoroTTS != null) {
                Log.d(TAG, "Forwarding to KokoroTTS");
                kokoroTTS.speak(text);
            } else {
                Log.e(TAG, "KokoroTTS instance is null");
            }
        } else {
            if (googleTTS != null && googleTTS.isInitialized()) {
                Log.d(TAG, "Forwarding to GoogleTTS");
                googleTTS.speak(text);
            } else {
                Log.e(TAG, "GoogleTTS not ready - initialized: " + (googleTTS != null && googleTTS.isInitialized()));
            }
        }
    }

    public void stop() {
        Log.d(TAG, "Stopping TTS");
        if (googleTTS != null) googleTTS.stop();
        if (kokoroTTS != null) kokoroTTS.stop();
    }

    public void pause() {
        Log.d(TAG, "Pausing TTS");
        stop();
    }

    public void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        selectedTTS = prefs.getString("ttsEngine", "google");
        voiceGender = prefs.getString("voiceGender", "male");
        Log.d(TAG, "Loaded settings - Engine: " + selectedTTS + ", Voice: " + voiceGender);

        if (googleTTS != null && "google".equals(selectedTTS)) {
            googleTTS.setVoiceGender(voiceGender);
        }
    }

    public void saveAudioToFile(String text, String filename) {
        Log.d(TAG, "Request to save audio to file: " + filename);
        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Empty or null text provided for saving");
            return;
        }

        if ("google".equals(selectedTTS) && googleTTS != null) {
            Log.d(TAG, "Saving with GoogleTTS");
            googleTTS.saveAudioToFile(text, filename);
        } else if ("kokoro".equals(selectedTTS) && kokoroTTS != null) {
            Log.w(TAG, "Audio saving not supported for Kokoro");
        } else {
            Log.e(TAG, "No valid TTS engine available for saving");
        }
    }

    public boolean isReady() {
        boolean ready = ("google".equals(selectedTTS) && googleTTS != null && googleTTS.isInitialized()) ||
                ("kokoro".equals(selectedTTS) && kokoroTTS != null);
        Log.d(TAG, "isReady: " + ready);
        return ready;
    }

    public void shutdown() {
        Log.d(TAG, "Shutting down TTS");
        if (googleTTS != null) googleTTS.shutdown();
        if (kokoroTTS != null) kokoroTTS.stop();
    }
}