package com.phototext.tts;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import java.io.File;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class GoogleTTSManager implements BaseTTSManager {
    private TextToSpeech tts;
    private final Context context;
    private float pitch = 1.0f;
    private float speed = 1.0f;
    private String voiceGender = "male";
    private boolean isInitialized = false;

    public GoogleTTSManager(Context context) {
        this.context = context;
        loadSettings();

        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true;
                configureTTS();
                loadSettings();
                Log.d("GoogleTTS", "TTS inizializzato con successo");
            } else {
                Log.e("GoogleTTS", "Inizializzazione TTS fallita");
            }
        });
    }

    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true;
                configureTTS();
                loadSettings();
                applyVoiceSettings(); // Applica solo dopo l'inizializzazione
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
        if (isInitialized) {
            applyVoiceSettings();
        }
    }

    private void applyVoiceSettings() {
        if (tts == null || !isInitialized) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Set<Voice> voices = tts.getVoices();
                if (voices == null || voices.isEmpty()) {
                    Log.w("GoogleTTS", "Nessuna voce disponibile");
                    return;
                }

                for (Voice voice : voices) {
                    if (voice.getLocale().equals(Locale.ITALIAN)) {
                        boolean isFemale = voice.getName().toLowerCase().contains("female") ||
                                voice.getName().toLowerCase().contains("femmina");

                        if (("female".equals(voiceGender) && isFemale) ||
                                ("male".equals(voiceGender) && !isFemale)) {
                            tts.setVoice(voice);
                            Log.d("GoogleTTS", "Voce selezionata: " + voice.getName());
                            return;
                        }
                    }
                }
                Log.w("GoogleTTS", "Nessuna voce trovata per il genere: " + voiceGender);
            } catch (Exception e) {
                Log.e("GoogleTTS", "Errore nell'impostazione della voce", e);
            }
        }
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) {
            Log.w("GoogleTTS", "Testo vuoto per la sintesi");
            return;
        }

        if (!isInitialized) {
            Log.e("GoogleTTS", "Motore TTS non inizializzato");
            return;
        }

        tts.setPitch(pitch);
        tts.setSpeechRate(speed);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance");
        Log.d("GoogleTTS", "Avviata sintesi vocale");
    }

    @Override
    public void pause() {
        Log.d(TAG, "Pausing TTS");
        stop();
    }

    public void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        pitch = prefs.getFloat("pitch", 1.0f);
        speed = prefs.getFloat("speed", 1.0f);
        voiceGender = prefs.getString("voiceGender", "male");

        if (isInitialized) {
            applyVoiceSettings();
        }
    }

    public void saveAudioToFile(String text, String filePath) {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized for saving");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Usa la directory interna dell'app invece di un percorso arbitrario
            File outputDir = context.getExternalFilesDir("tts_audio");
            if (outputDir == null) {
                outputDir = context.getFilesDir(); // Fallback alla directory interna
            }

            // Crea il nome file univoco
            String fileName = "audio_" + System.currentTimeMillis() + ".wav";
            File outputFile = new File(outputDir, fileName);

            // Crea le directory se non esistono
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    Log.e(TAG, "Failed to create output directory");
                    return;
                }
            }

            int result = tts.synthesizeToFile(text, null, outputFile, "tts_file");
            if (result == TextToSpeech.SUCCESS) {
                Log.d(TAG, "Successfully started audio file synthesis to: " + outputFile.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to start audio file synthesis");
            }
        } else {
            Log.e(TAG, "Audio file saving not supported on this Android version");
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
    public boolean isInitialized() {
        return isInitialized;
    }
}
