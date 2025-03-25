package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import java.util.Locale;
import java.util.Set;

public class TextToSpeechManager {
    private TextToSpeech textToSpeech;
    private final Context context;
    private boolean isPaused = false;
    private static final String TAG = "TextToSpeechManager";

    public TextToSpeechManager(Context context) {
        this.context = context;
        initTextToSpeech();
    }

    /** Inizializza il TextToSpeech */
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
                updateVoiceSettings();
            } else {
                Log.e(TAG, "Errore nell'inizializzazione di TextToSpeech.");
            }
        });
    }

    /** Aggiorna le impostazioni della sintesi vocale */
    private void updateVoiceSettings() {
        SharedPreferences preferences = context.getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE);
        float pitch = preferences.getFloat("pitch", 1.0f);
        float speed = preferences.getFloat("speed", 1.0f);
        String voiceGender = preferences.getString("voiceGender", "male");

        Voice selectedVoice = getVoiceByGender(voiceGender);
        if (selectedVoice != null) {
            textToSpeech.setVoice(selectedVoice);
        } else {
            Log.e(TAG, "Voce non trovata per il genere: " + voiceGender);
        }

        textToSpeech.setPitch(pitch);
        textToSpeech.setSpeechRate(speed);
    }

    /** Salva la scelta della voce e aggiorna immediatamente le impostazioni */
    public void saveVoiceChoice(String gender) {
        SharedPreferences preferences = context.getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE);
        preferences.edit().putString("voiceGender", gender).apply();
        updateVoiceSettings();
    }

    /** Ottiene una voce maschile o femminile tra quelle disponibili */
    private Voice getVoiceByGender(String gender) {
        Set<Voice> voices = textToSpeech.getVoices();
        if (voices == null) return null;

        for (Voice voice : voices) {
            if (voice.getLocale().equals(Locale.getDefault())) {
                boolean isFemale = voice.getName().toLowerCase().contains("female");
                boolean isMale = voice.getName().toLowerCase().contains("male");

                if ("female".equals(gender) && isFemale) return voice;
                if ("male".equals(gender) && isMale) return voice;
            }
        }

        return getDefaultVoice();
    }

    /** Restituisce una voce predefinita nel caso in cui non trovi quella richiesta */
    private Voice getDefaultVoice() {
        Set<Voice> voices = textToSpeech.getVoices();
        if (voices != null) {
            for (Voice voice : voices) {
                if (voice.getLocale().equals(Locale.getDefault())) {
                    return voice;
                }
            }
        }
        return null;
    }

    /** Riproduce il testo */
    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        updateVoiceSettings();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
    }

    /** Ferma la lettura */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            isPaused = false;
        }
    }

    /** Simula la pausa fermando la riproduzione */
    public void pause() {
        if (textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            isPaused = true;
        }
    }

    /** Rilascia le risorse di TextToSpeech */
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public boolean isPaused() {
        return isPaused;
    }
}
