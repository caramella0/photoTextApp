package com.phototext.tts;

public interface TTSProvider {
    void speak(String text);
    void pause();
    void resume();
    void stop();
    void shutdown();
    void saveSettings(float pitch, float speed, String gender);
    void setLanguage(String languageCode);
    boolean isSpeaking();
    void setTtsListener(TTSListener listener);

    interface TTSListener {
        void onStart();
        void onDone();
        void onError(String error);
    }
}