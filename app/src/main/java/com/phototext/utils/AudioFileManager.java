package com.phototext.utils;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioFileManager {
    private File audioDirectory;

    public AudioFileManager(Context context) {
        audioDirectory = new File(context.getFilesDir(), "audio");
        if (!audioDirectory.exists()) {
            audioDirectory.mkdirs();
        }
    }

    /** Restituisce la lista dei file audio salvati */
    public List<File> getAudioFiles() {
        File[] files = audioDirectory.listFiles();
        List<File> audioFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".mp3") || file.getName().endsWith(".wav")) {
                    audioFiles.add(file);
                }
            }
        }
        return audioFiles;
    }

    /** Elimina un file audio */
    public boolean deleteAudioFile(File audioFile) {
        return audioFile.exists() && audioFile.delete();
    }
}
