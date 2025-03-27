package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import java.io.File;
import java.util.Locale;
import java.util.Set;

public class TextToSpeechManager {
    private TextToSpeech textToSpeech;
    private final Context context;
    private static final String TAG = "TextToSpeechManager";

    private float pitch = 1.0f;
    private float speed = 1.0f;
    private String voiceGender = "male";

    public TextToSpeechManager(Context context) {
        this.context = context;
        loadSettings(); // Carica le impostazioni salvate
        initTextToSpeech();
    }

    /** Inizializza il TextToSpeech */
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
                applySettings(); // Applica le impostazioni salvate
            } else {
                Log.e(TAG, "Errore nell'inizializzazione di TextToSpeech.");
            }
        });
    }

    /** Carica le impostazioni salvate */
    private void loadSettings() {
        SharedPreferences preferences = context.getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE);
        pitch = preferences.getFloat("pitch", 1.0f);
        speed = preferences.getFloat("speed", 1.0f);
        voiceGender = preferences.getString("voiceGender", "male");

        Log.d("TextToSpeechManager", "il genere selezionato è:" + voiceGender);
    }

    /** Salva le impostazioni e le applica immediatamente */
    public void saveSettings(float newPitch, float newSpeed, String newGender) {
        this.pitch = newPitch;
        this.speed = newSpeed;
        this.voiceGender = newGender;

        SharedPreferences preferences = context.getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("pitch", newPitch);
        editor.putFloat("speed", newSpeed);
        editor.putString("voiceGender", newGender);
        editor.apply();

        applySettings(); // Applica subito le nuove impostazioni
    }

    /** Applica le impostazioni attuali */
    public void applySettings() {
        SharedPreferences preferences = context.getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE);
        float newPitch = preferences.getFloat("pitch", 1.0f);
        float newSpeed = preferences.getFloat("speed", 1.0f);
        String newVoiceGender = preferences.getString("voiceGender", "male");

        if (textToSpeech != null) {
            textToSpeech.setPitch(newPitch);
            textToSpeech.setSpeechRate(newSpeed);

            Voice selectedVoice = getVoiceByGender(newVoiceGender);
            if (selectedVoice != null && !selectedVoice.equals(textToSpeech.getVoice())) {
                textToSpeech.setVoice(selectedVoice);
            }
        }
    }


    /** Ottiene una voce maschile o femminile tra quelle disponibili */
    private Voice getVoiceByGender(String gender) {
        if (textToSpeech == null) return null;

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
        if (textToSpeech == null) return null;

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

    /** Salva il file audio */
    public void saveAudioFile(String text) {
        if (text == null || text.trim().isEmpty()) return;

        // Creiamo la cartella per salvare gli audio
        File audioDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioEstratti");
        if (!audioDir.exists()) audioDir.mkdirs();

        // Nome predefinito per il file audio
        String fileName = "Testo audio estratto_" + System.currentTimeMillis() + ".mp3";
        File audioFile = new File(audioDir, fileName);

        // Salviamo l'audio generato
        textToSpeech.synthesizeToFile(text, null, audioFile, "TTS_AUDIO");

        Log.d("TTS", "File audio salvato: " + audioFile.getAbsolutePath());
    }

    /** Riproduce il testo */
    public void speak(String text) {
        if (text == null || text.trim().isEmpty() || textToSpeech == null) return;
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
    }

    /** Ferma la lettura */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    /** Simula la pausa fermando la riproduzione */
    public void pause() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    /** Rilascia le risorse di TextToSpeech */
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
