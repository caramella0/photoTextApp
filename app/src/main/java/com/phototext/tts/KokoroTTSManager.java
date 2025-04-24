package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KokoroTTSManager implements BaseTTSManager {

    private static final String TAG = "KokoroTTS";
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MediaPlayer mediaPlayer;
    private String serverUrl;

    public KokoroTTSManager(Context context) {
        this.context = context;
        loadSettings();
    }

    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String serverIp = prefs.getString("server_ip", "10.0.2.2");
        serverUrl = "http://" + serverIp + ":5000/synthesize";
        Log.d(TAG, "Server URL impostato a: " + serverUrl);
    }

    @Override
    public void speak(String text) {
        executor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "text=" + text;
                connection.getOutputStream().write(postData.getBytes());

                if (connection.getResponseCode() == 200) {
                    File tempFile = File.createTempFile("tts_kokoro_", ".wav", context.getCacheDir());
                    try (InputStream in = connection.getInputStream();
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    playAudio(tempFile);
                } else {
                    Log.e(TAG, "Errore HTTP: " + connection.getResponseCode());
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Errore durante la richiesta al server TTS", e);
            }
        });
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
        } catch (Exception e) {
            Log.e(TAG, "Errore nella riproduzione audio", e);
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        executor.shutdown();
    }

    @Override
    public void saveAudioToFile(String text, String filename) {
        executor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "text=" + text;
                connection.getOutputStream().write(postData.getBytes());

                if (connection.getResponseCode() == 200) {
                    File outputFile = new File(context.getExternalFilesDir(null), filename);
                    try (InputStream in = connection.getInputStream();
                         FileOutputStream out = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    Log.d(TAG, "Audio salvato: " + outputFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "Errore HTTP nel salvataggio: " + connection.getResponseCode());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Errore durante il salvataggio audio", e);
            }
        });
    }

}
