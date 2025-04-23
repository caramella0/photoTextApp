package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;

public class TextToSpeechManager {
    private final Context context;
    private GoogleTTSManager googleTTS;
    private KokoroTTSManager kokoroTTS;
    private String selectedTTS;
    private String voiceGender; // Aggiunta la variabile per il genere vocale

    public TextToSpeechManager(Context context) {
        this.context = context;
        loadSettings();

        if ("kokoro".equals(selectedTTS)) {
            kokoroTTS = new KokoroTTSManager(context);
        } else {
            googleTTS = new GoogleTTSManager(context);
            googleTTS.setVoiceGender(voiceGender); // Imposta il genere vocale iniziale
        }
    }

    /** Avvia la riproduzione del testo */
    public void speak(String text) {
        if ("kokoro".equals(selectedTTS)) {
            if (kokoroTTS != null) kokoroTTS.speak(text);
        } else {
            if (googleTTS != null) googleTTS.speak(text);
        }
    }

    /** Ferma la riproduzione del TTS */
    public void stop() {
        if (googleTTS != null) googleTTS.stop();
        if (kokoroTTS != null) kokoroTTS.stop();
    }

    /** Pausa non supportata per Kokoro, solo per Google */
    public void pause() {
        // Il TTS Android non supporta la pausa nativa, puoi simulare con stop()
        stop();
    }

    /** Aggiorna le impostazioni e ricarica il motore TTS */
    public void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        selectedTTS = prefs.getString("ttsEngine", "google");
        voiceGender = prefs.getString("voiceGender", "male"); // Carica il genere dalle preferenze

        // Aggiorna il genere vocale se stiamo usando Google TTS
        if (googleTTS != null && "google".equals(selectedTTS)) {
            googleTTS.setVoiceGender(voiceGender);
        }
    }

    /** Salva un file audio solo se il TTS è Google (offline) */
    public void saveAudioToFile(String text, String filename) {
        if ("google".equals(selectedTTS) && googleTTS != null) {
            googleTTS.saveAudioToFile(text, filename);
        }
        // Per Kokoro: implementeremo successivamente la funzione download su richiesta
    }

    /** Spegne il TTS quando l'app viene chiusa */
    public void shutdown() {
        if (googleTTS != null) googleTTS.shutdown();
        if (kokoroTTS != null) kokoroTTS.stop();  // Pulisce il MediaPlayer
    }
}
