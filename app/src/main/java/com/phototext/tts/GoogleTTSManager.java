package com.phototext.tts;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.io.File;
import java.util.Locale;

public class GoogleTTSManager implements TTSProvider {
    private TextToSpeech tts;
    private float pitch = 1.0f;
    private float speed = 1.0f;
    private String gender = "female";
    private TTSListener listener;
    private boolean isReady = false;
    private final Context context;  // Aggiungi questo campo

    public GoogleTTSManager(Context context) {

        this.context = context;  // Inizializza il context

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isReady = true;
                tts.setLanguage(Locale.getDefault());
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {
                        if (listener != null) listener.onStart();
                    }
                    @Override public void onDone(String utteranceId) {
                        if (listener != null) listener.onDone();
                    }
                    @Override public void onError(String utteranceId) {
                        if (listener != null) listener.onError("TTS error");
                    }
                });
            } else {
                if (listener != null) listener.onError("TTS initialization failed");
            }
        }, "com.google.android.tts");
    }

    @Override
    public void speak(String text) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance");
        }
    }

    @Override
    public void saveSettings(float pitch, float speed, String gender) {
        this.pitch = pitch;
        this.speed = speed;
        this.gender = gender;
        applySettings();
    }

    @Override
    public void setLanguage(String languageCode) {
        if (isReady) {
            Locale locale = new Locale(languageCode);
            tts.setLanguage(locale);
        }
    }

    @Override
    public boolean isSpeaking() {
        return isReady && tts.isSpeaking();
    }

    @Override
    public void setTtsListener(TTSListener listener) {
        this.listener = listener;
    }

    public void saveAudioToFile(String text, String filename) {
        if (isReady && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Crea la directory se non esiste
                File audioDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioEstratti");
                if (!audioDir.exists() && !audioDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory");
                    if (listener != null) listener.onError("Errore creazione cartella");
                    return;
                }

                File file = new File(audioDir, filename);
                int result = tts.synthesizeToFile(text, null, file, "tts_file");

                if (result == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "Audio saved to: " + file.getAbsolutePath());
                    if (listener != null) listener.onDone();
                } else {
                    Log.e(TAG, "Failed to save audio file");
                    if (listener != null) listener.onError("Errore nel salvataggio");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving audio", e);
                if (listener != null) listener.onError("Errore: " + e.getMessage());
            }
        } else {
            String errorMsg = "Funzionalità non disponibile";
            if (!isReady) errorMsg += " (TTS non pronto)";
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                errorMsg += " (Richiede Android Lollipop o superiore)";
            }
            Log.w(TAG, errorMsg);
            if (listener != null) listener.onError(errorMsg);
        }
    }
    private void applySettings() {
        if (isReady) {
            tts.setPitch(pitch);
            tts.setSpeechRate(speed);

            // Implementazione migliore per la selezione della voce
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (android.speech.tts.Voice voice : tts.getVoices()) {
                    if (gender.equals("female") && voice.getName().toLowerCase().contains("female")) {
                        tts.setVoice(voice);
                        break;
                    } else if (gender.equals("male") && voice.getName().toLowerCase().contains("male")) {
                        tts.setVoice(voice);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void pause() {
        if (isReady) tts.stop();
    }

    @Override
    public void resume() {
        // Implementazione specifica se necessario
    }

    @Override
    public void stop() {
        if (isReady) tts.stop();
    }

    @Override
    public void shutdown() {
        if (tts != null) tts.shutdown();
    }

}