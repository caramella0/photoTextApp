package com.phototext.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.phototext.R;
import java.io.File;
import java.util.ArrayList;

public class AudioLibraryActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<String> audioFiles;
    private ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_library);

        listView = findViewById(R.id.audioListView);
        audioFiles = new ArrayList<>();

        loadAudioFiles();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, audioFiles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> playAudio(audioFiles.get(position)));
    }

    private void loadAudioFiles() {
        File audioDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioEstratti");
        if (audioDir.exists()) {
            File[] files = audioDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    audioFiles.add(file.getAbsolutePath());
                }
            }
        }
    }

    private void playAudio(String filePath) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Riproduzione: " + filePath, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Errore nella riproduzione", Toast.LENGTH_SHORT).show();
        }
    }
}
