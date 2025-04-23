package com.phototext.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioFileManager {
    private static final String TAG = "AudioFileManager";
    private final Context context;
    private static final String AUDIO_DIR = "PhotoText_Audios";

    public AudioFileManager(Context context) {
        this.context = context;
    }

    public List<File> getAudioFiles() {
        List<File> audioFiles = new ArrayList<>();
        try {
            File audioDir = new File(context.getExternalFilesDir(null), AUDIO_DIR);
            Log.d(TAG, "Looking for audio files in: " + audioDir.getAbsolutePath());

            if (audioDir.exists() && audioDir.isDirectory()) {
                File[] files = audioDir.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".wav") ||
                                name.toLowerCase().endsWith(".mp3") ||
                                name.toLowerCase().endsWith(".ogg"));

                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            audioFiles.add(file);
                        }
                    }
                }
            }
            Log.d(TAG, "Found " + audioFiles.size() + " audio files");
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio files", e);
        }
        return audioFiles;
    }

    public boolean deleteAudioFile(File audioFile) {
        try {
            if (audioFile.exists()) {
                boolean deleted = audioFile.delete();
                Log.d(TAG, "File " + audioFile.getName() + " deletion " + (deleted ? "successful" : "failed"));
                return deleted;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting audio file", e);
            return false;
        }
    }
}