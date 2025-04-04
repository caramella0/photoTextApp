package com.phototext.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioFileManager {
    private static final String[] SUPPORTED_EXTENSIONS = {".mp3", ".wav", ".ogg", ".m4a", ".aac"};
    private final File audioDirectory;

    public AudioFileManager(Context context) {
        this.audioDirectory = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioEstratti");
        createDirectoryIfNotExists();
    }

    private void createDirectoryIfNotExists() {
        if (!audioDirectory.exists() && !audioDirectory.mkdirs()) {
            Log.e("AudioFileManager", "Directory creation failed: " + audioDirectory.getAbsolutePath());
        }
    }

    public List<File> getAudioFiles() {
        List<File> audioFiles = new ArrayList<>();
        File[] files = audioDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (isAudioFile(file)) {
                    audioFiles.add(file);
                }
            }
        }
        return audioFiles;
    }

    public boolean deleteAudioFile(File audioFile) {
        return audioFile != null && audioFile.exists() && audioFile.delete();
    }

    public File createNewAudioFile(String baseName) throws IOException {
        String fileName = addExtensionIfMissing(baseName);
        File newFile = new File(audioDirectory, generateUniqueFilename(fileName));

        if (!newFile.createNewFile()) {
            throw new IOException("File creation failed: " + newFile.getAbsolutePath());
        }
        return newFile;
    }

    private String generateUniqueFilename(String fileName) {
        File tempFile = new File(audioDirectory, fileName);
        if (!tempFile.exists()) return fileName;

        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        return nameWithoutExt + "_" + System.currentTimeMillis() + extension;
    }

    private String addExtensionIfMissing(String fileName) {
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (fileName.toLowerCase().endsWith(ext)) {
                return fileName;
            }
        }
        return fileName + ".mp3";
    }

    private boolean isAudioFile(File file) {
        if (file == null || !file.isFile()) return false;

        String name = file.getName().toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }
}