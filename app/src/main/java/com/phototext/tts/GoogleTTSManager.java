package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class GoogleTTSManager {
    private TextToSpeech tts;
    private final Context context;
    private float pitch = 1.0f;
    private float speed = 1.0f;
    private String voiceGender = "male"; // Valori: "male" o "female"

    public GoogleTTSManager(Context context) {
        this.context = context;
        initializeTTS();
    }

    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                configureTTS();
                loadSettings();
                setVoiceGender(voiceGender); // Applica il genere vocale
            } else {
                Log.e("GoogleTTS", "Errore inizializzazione TTS");
            }
        });
    }

    private void configureTTS() {
        int result = tts.setLanguage(Locale.ITALIAN);
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("GoogleTTS", "Lingua non supportata");
        }
    }

    public void setVoiceGender(String gender) {
        this.voiceGender = gender;
        applyVoiceSettings();
    }

    private void applyVoiceSettings() {
        if (tts == null) return;

        Set<String> features = new HashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Cerca una voce che corrisponda al genere selezionato
            for (Voice voice : tts.getVoices()) {
                if (voice.getLocale().equals(Locale.ITALIAN)) {
                    boolean isFemale = voice.getName().toLowerCase().contains("female") ||
                            voice.getName().toLowerCase().contains("femmina");

                    if (("female".equals(voiceGender) && isFemale) ||
                            ("male".equals(voiceGender) && !isFemale)) {
                        tts.setVoice(voice);
                        Log.d("GoogleTTS", "Voce selezionata: " + voice.getName());
                        break;
                    }
                }
            }
        }
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;

        tts.setPitch(pitch);
        tts.setSpeechRate(speed);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void stop() {
        tts.stop();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        pitch = prefs.getFloat("pitch", 1.0f);
        speed = prefs.getFloat("speed", 1.0f);
        voiceGender = prefs.getString("voiceGender", "male");
        applyVoiceSettings(); // Applica le impostazioni caricate
    }

    public void saveAudioToFile(String text, String filename) {
        // Può essere aggiunto usando setAudioParams + synthesizeToFile()
        // Funzione opzionale per salvataggio offline
    }

    public void logAvailableVoices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Voice voice : tts.getVoices()) {
                Log.d("GoogleTTS", "Voice: " + voice.getName() +
                        ", Locale: " + voice.getLocale() +
                        ", Gender: " + (voice.getName().toLowerCase().contains("female") ? "female" : "male"));
            }
        }
    }
}
