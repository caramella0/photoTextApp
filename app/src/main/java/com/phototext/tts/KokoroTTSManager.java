package com.phototext.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class KokoroTTSManager {
    private final Context context;
    private MediaPlayer mediaPlayer;
    private static final String SERVER_URL = "http://172.24.24.58:5000";
    private String serverUrl;

    public KokoroTTSManager(Context context) {
        this.context = context;
        loadSettings();
    }
    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String serverIp = prefs.getString("server_ip", "10.0.2.2");
        serverUrl = "http://" + serverIp + ":5000/synthesize";
    }
    public void speak(String text) {
        new SynthesisTask().execute(text);
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    private class SynthesisTask extends AsyncTask<String, Void, File> {
        @Override
        protected File doInBackground(String... texts) {
            try {
                String inputText = texts[0];
                URL url = new URL(SERVER_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "text=" + inputText;
                connection.getOutputStream().write(postData.getBytes());

                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = connection.getInputStream();
                    File tempFile = File.createTempFile("tts_kokoro", ".wav", context.getCacheDir());
                    FileOutputStream outputStream = new FileOutputStream(tempFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();
                    return tempFile;
                }
            } catch (Exception e) {
                Log.e("KokoroTTS", "Errore nella sintesi", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(File audioFile) {
            if (audioFile != null) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (Exception e) {
                    Log.e("KokoroTTS", "Errore nella riproduzione audio", e);
                }
            }
        }
    }
}
