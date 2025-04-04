package com.phototext.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.phototext.R;
import com.phototext.utils.AudioFileManager;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioLibraryActivity extends AppCompatActivity {
    private static final String TAG = "AudioLibrary";
    private AudioListAdapter adapter;
    private MediaPlayer mediaPlayer;
    private AudioFileManager audioFileManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_library);

        audioFileManager = new AudioFileManager(this);
        adapter = new AudioListAdapter(this::onAudioItemAction);

        ListView listView = findViewById(R.id.audioListView);
        listView.setAdapter(adapter);

        loadAudioFiles();
    }

    private void onAudioItemAction(AudioAction action, File audioFile) {
        switch (action) {
            case PLAY:
                playAudio(audioFile);
                break;
            case SHARE:
                shareAudio(audioFile);
                break;
            case DELETE:
                deleteAudio(audioFile);
                break;
        }
    }

    private void loadAudioFiles() {
        executor.execute(() -> {
            List<File> files = audioFileManager.getAudioFiles();
            mainHandler.post(() -> adapter.updateList(files));
        });
    }

    private void playAudio(File audioFile) {
        stopAudio();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.setOnCompletionListener(mp -> stopAudio());
            mediaPlayer.prepare();
            mediaPlayer.start();
            showToast("Playing: " + audioFile.getName());
        } catch (Exception e) {
            Log.e(TAG, "Playback error", e);
            showToast("Playback failed");
            stopAudio();
        }
    }

    private void shareAudio(File audioFile) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(this, getPackageName() + ".provider", audioFile));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share audio"));
        } catch (Exception e) {
            Log.e(TAG, "Sharing error", e);
            showToast("Sharing failed");
        }
    }

    private void deleteAudio(File audioFile) {
        executor.execute(() -> {
            boolean success = audioFileManager.deleteAudioFile(audioFile);
            mainHandler.post(() -> {
                if (success) {
                    showToast("Deleted successfully");
                    loadAudioFiles();
                } else {
                    showToast("Deletion failed");
                }
            });
        });
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();
        executor.shutdown();
    }

    public enum AudioAction {
        PLAY, SHARE, DELETE
    }

    public interface AudioActionListener {
        void onAudioAction(AudioAction action, File audioFile);
    }
}