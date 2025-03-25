package com.phototext.tts;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoquiTTSManager {
    private final Context context;
    private MediaPlayer mediaPlayer;
    private final ExecutorService executorService;

    public CoquiTTSManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void speak(String text) {
        executorService.execute(() -> {
            File outputWav = new File(context.getCacheDir(), "speech.wav");

            try {
                // Apriamo il modello vocale e scriviamo il file
                InputStream is = context.getAssets().open("models/it_IT-paola.onnx");
                FileOutputStream fos = new FileOutputStream(outputWav);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                is.close();

                // Verifica se il file è valido prima di riprodurlo
                if (!outputWav.exists() || outputWav.length() == 0) {
                    Log.e("CoquiTTS", "Errore: il file audio non è stato generato correttamente.");
                    return;
                }

                playAudio(outputWav);

            } catch (Exception e) {
                Log.e("CoquiTTS", "Errore nella generazione audio", e);
            }
        });
    }


    private void playAudio(File file) {
        if (!file.exists() || file.length() == 0) {
            Log.e("CoquiTTS", "Errore: il file audio non esiste o è vuoto.");
            return;
        }

        // Convertire il file in un formato compatibile
        File convertedFile = new File(context.getCacheDir(), "speech_converted.wav");
        try {
            Process process = new ProcessBuilder()
                    .command("ffmpeg", "-i", file.getAbsolutePath(), "-ar", "16000", "-ac", "1", convertedFile.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
        } catch (Exception e) {
            Log.e("CoquiTTS", "Errore nella conversione del file audio", e);
            return;
        }

        // Verifica se la conversione è andata a buon fine
        if (!convertedFile.exists() || convertedFile.length() == 0) {
            Log.e("CoquiTTS", "Errore: la conversione del file audio non è riuscita.");
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(convertedFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("CoquiTTS", "Errore nella riproduzione audio", e);
        }
    }

    public void shutdown() {
        executorService.shutdown();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


}
