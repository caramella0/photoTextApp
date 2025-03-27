package com.phototext.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.phototext.R;
import com.phototext.utils.AudioFileManager;

import java.io.File;
import java.util.List;

public class AudioLibraryActivity extends AppCompatActivity {
    private ListView audioListView;
    private AudioFileManager audioFileManager;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_library);

        audioListView = findViewById(R.id.audioListView);
        audioFileManager = new AudioFileManager(this);

        // Carica e mostra la lista dei file audio
        loadAudioFiles();
    }

    private void loadAudioFiles() {
        List<File> audioFiles = audioFileManager.getAudioFiles();
        if (audioFiles.isEmpty()) {
            Toast.makeText(this, "Nessuna traccia audio trovata", Toast.LENGTH_SHORT).show();
        } else {
            AudioListAdapter adapter = new AudioListAdapter(this, audioFiles);
            audioListView.setAdapter(adapter);
        }
    }

    /** Avvia la riproduzione di un file audio */
    public void playAudio(File audioFile) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(audioFile));
        mediaPlayer.start();
        Toast.makeText(this, "Riproduzione: " + audioFile.getName(), Toast.LENGTH_SHORT).show();
    }

    /** Elimina un file audio */
    public void deleteAudio(File audioFile) {
        if (audioFileManager.deleteAudioFile(audioFile)) {
            Toast.makeText(this, "Traccia eliminata", Toast.LENGTH_SHORT).show();
            loadAudioFiles();
        } else {
            Toast.makeText(this, "Errore nell'eliminazione", Toast.LENGTH_SHORT).show();
        }
    }

    /** Condivide un file audio */
    public void shareAudio(File audioFile) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        Uri fileUri = Uri.fromFile(audioFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        startActivity(Intent.createChooser(shareIntent, "Condividi audio"));
    }
}
