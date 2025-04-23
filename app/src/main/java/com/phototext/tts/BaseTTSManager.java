package com.phototext.tts;

public interface BaseTTSManager {
    void speak(String text);
    void pause();
    void stop();
    void shutdown();
    void saveAudioToFile(String text, String filename);
}
