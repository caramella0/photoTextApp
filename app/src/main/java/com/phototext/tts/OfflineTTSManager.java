package com.phototext.tts;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflineTTSManager implements TTSProvider {
    private final Context context;
    private final ExecutorService executor;
    private final Handler handler;
    private MediaPlayer mediaPlayer;
    private TTSListener listener;
    private float pitch = 1.0f;
    private float speed = 1.0f;

    public OfflineTTSManager(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void speak(String text) {
        executor.execute(() -> {
            try {
                File audioFile = generateAudio(text);
                handler.post(() -> playAudio(audioFile));
            } catch (Exception e) {
                Log.e("OfflineTTS", "Error generating speech", e);
                if (listener != null) listener.onError("Audio generation failed");
            }
        });
    }

    @Override
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void shutdown() {
        stop();
        executor.shutdown();
    }

    @Override
    public void saveSettings(float pitch, float speed, String gender) {
        this.pitch = pitch;
        this.speed = speed;
        // Gender setting not used in offline mode
    }

    @Override
    public void setLanguage(String languageCode) {
        // Limited language support in offline mode
    }

    @Override
    public boolean isSpeaking() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void setTtsListener(TTSListener listener) {
        this.listener = listener;
    }

    private File generateAudio(String text) throws Exception {
        // Implementazione semplificata - in produzione usare un motore TTS offline
        File outputFile = new File(context.getCacheDir(), "tts_temp.wav");
        try (InputStream is = context.getAssets().open("default_audio.wav");
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return outputFile;
    }

    private void playAudio(File audioFile) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            if (listener != null) listener.onStart();

            mediaPlayer.setOnCompletionListener(mp -> {
                if (listener != null) listener.onDone();
                releaseMediaPlayer();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                if (listener != null) listener.onError("Playback error");
                releaseMediaPlayer();
                return true;
            });
        } catch (Exception e) {
            Log.e("OfflineTTS", "Playback error", e);
            if (listener != null) listener.onError("Playback failed");
            releaseMediaPlayer();
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}