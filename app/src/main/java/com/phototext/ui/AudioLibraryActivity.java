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
        Log.d(TAG, "Activity created");

        audioFileManager = new AudioFileManager(this);
        adapter = new AudioListAdapter(this::onAudioItemAction);

        ListView listView = findViewById(R.id.audioListView);
        listView.setAdapter(adapter);

        loadAudioFiles();
    }

    private void onAudioItemAction(AudioAction action, File audioFile) {
        Log.d(TAG, "Action: " + action + " on file: " + audioFile.getName());
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
        Log.d(TAG, "Loading audio files...");
        executor.execute(() -> {
            try {
                List<File> files = audioFileManager.getAudioFiles();
                Log.d(TAG, "Found " + files.size() + " audio files");
                mainHandler.post(() -> {
                    adapter.updateList(files);
                    if (files.isEmpty()) {
                        showToast("No audio files found");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading audio files", e);
                mainHandler.post(() -> showToast("Error loading audio files"));
            }
        });
    }

    private void playAudio(File audioFile) {
        Log.d(TAG, "Attempting to play: " + audioFile.getAbsolutePath());
        stopAudio();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Playback completed");
                stopAudio();
            });
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting playback");
                mediaPlayer.start();
                showToast("Playing: " + audioFile.getName());
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error - what: " + what + ", extra: " + extra);
                stopAudio();
                showToast("Playback failed");
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Playback error", e);
            showToast("Playback failed");
            stopAudio();
        }
    }

    private void shareAudio(File audioFile) {
        Log.d(TAG, "Sharing audio file: " + audioFile.getName());
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(this, getPackageName() + ".provider", audioFile));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share audio"));
            Log.d(TAG, "Share intent started");
        } catch (Exception e) {
            Log.e(TAG, "Sharing error", e);
            showToast("Sharing failed");
        }
    }

    private void deleteAudio(File audioFile) {
        Log.d(TAG, "Deleting audio file: " + audioFile.getName());
        executor.execute(() -> {
            boolean success = audioFileManager.deleteAudioFile(audioFile);
            mainHandler.post(() -> {
                if (success) {
                    Log.d(TAG, "File deleted successfully");
                    showToast("Deleted successfully");
                    loadAudioFiles();
                } else {
                    Log.w(TAG, "Deletion failed");
                    showToast("Deletion failed");
                }
            });
        });
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                Log.d(TAG, "MediaPlayer released");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping MediaPlayer", e);
            } finally {
                mediaPlayer = null;
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
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