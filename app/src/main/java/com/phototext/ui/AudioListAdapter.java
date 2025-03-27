package com.phototext.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.phototext.R;

import java.io.File;
import java.util.List;

public class AudioListAdapter extends BaseAdapter {
    private final AudioLibraryActivity activity;
    private final List<File> audioFiles;

    public AudioListAdapter(AudioLibraryActivity activity, List<File> audioFiles) {
        this.activity = activity;
        this.audioFiles = audioFiles;
    }

    @Override
    public int getCount() {
        return audioFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return audioFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.item_audio_file, parent, false);
        }

        File audioFile = audioFiles.get(position);
        TextView txtAudioName = convertView.findViewById(R.id.txtAudioName);
        ImageButton btnPlay = convertView.findViewById(R.id.btnPlay);
        ImageButton btnShare = convertView.findViewById(R.id.btnShare);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDelete);

        txtAudioName.setText(audioFile.getName());

        btnPlay.setOnClickListener(v -> activity.playAudio(audioFile));
        btnShare.setOnClickListener(v -> activity.shareAudio(audioFile));
        btnDelete.setOnClickListener(v -> activity.deleteAudio(audioFile));

        return convertView;
    }
}
