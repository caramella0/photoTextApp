package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TextToSpeechManager implements TTSProvider {
    private static final String TAG = "TextToSpeechManager";
    private final Context context;
    private TTSProvider currentProvider;
    private final SharedPreferences preferences;
    private TTSListener listener;

    public TextToSpeechManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE);
        initTTSProvider();
    }

    // Aggiungi il metodo initTTSProvider
    private void initTTSProvider() {
        try {
            currentProvider = new GoogleTTSManager(context);
            Log.d(TAG, "Using Google TTS provider");

            // CARICA LE IMPOSTAZIONI SUBITO DOPO L'INIZIALIZZAZIONE
            loadSettings();

        } catch (Exception e) {
            Log.w(TAG, "Google TTS not available, falling back to offline");
            currentProvider = new OfflineTTSManager(context);
            loadSettings();
        }

        currentProvider.setTtsListener(new TTSListener() {
            @Override public void onStart() { if (listener != null) listener.onStart(); }
            @Override public void onDone() { if (listener != null) listener.onDone(); }
            @Override public void onError(String error) { if (listener != null) listener.onError(error); }
        });
    }



    // Aggiungi il metodo saveAudioToFile
    public void saveAudioToFile(String text, String filename) {
        if (currentProvider instanceof GoogleTTSManager) {
            ((GoogleTTSManager) currentProvider).saveAudioToFile(text, filename);
        } else {
            Log.w(TAG, "Audio saving not supported in offline mode");
            if (listener != null) {
                listener.onError("Salvataggio audio non supportato in modalità offline");
            }
        }
    }
    // Aggiungi il metodo saveSettings
    @Override
    public void saveSettings(float pitch, float speed, String gender) {
        Log.d(TAG, "Salvataggio impostazioni - Pitch: " + pitch +
                ", Speed: " + speed + ", Gender: " + gender);

        preferences.edit()
                .putFloat("pitch", pitch)
                .putFloat("speed", speed)
                .putString("voiceGender", gender)
                .apply();

        currentProvider.saveSettings(pitch, speed, gender);
    }

    // Aggiungi il metodo loadSettings
    public void loadSettings() {
        float pitch = preferences.getFloat("pitch", 1.0f);
        float speed = preferences.getFloat("speed", 1.0f);
        String gender = preferences.getString("voiceGender", "female");
        currentProvider.saveSettings(pitch, speed, gender);
    }

    @Override
    public void speak(String text) {
        currentProvider.speak(text);
    }

    @Override
    public void pause() {
        currentProvider.pause();
    }

    @Override
    public void resume() {
        currentProvider.resume();
    }

    @Override
    public void stop() {
        currentProvider.stop();
    }

    @Override
    public void shutdown() {
        currentProvider.shutdown();
    }
    @Override
    public void setLanguage(String languageCode) {
        currentProvider.setLanguage(languageCode);
    }

    @Override
    public boolean isSpeaking() {
        return currentProvider.isSpeaking();
    }

    @Override
    public void setTtsListener(TTSListener listener) {
        this.listener = listener;
    }

}