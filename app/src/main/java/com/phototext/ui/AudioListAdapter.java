package com.phototext.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import com.phototext.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioListAdapter extends BaseAdapter {
    private final AudioLibraryActivity.AudioActionListener actionListener;
    private List<File> audioFiles = new ArrayList<>();

    public AudioListAdapter(AudioLibraryActivity.AudioActionListener listener) {
        this.actionListener = listener;
    }

    public void updateList(List<File> newFiles) {
        this.audioFiles = new ArrayList<>(newFiles);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return audioFiles.size(); }
    @Override public File getItem(int position) { return audioFiles.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio_file, parent, false);
            holder = new ViewHolder(convertView, actionListener);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.bind(getItem(position));
        return convertView;
    }

    private static class ViewHolder {
        private final TextView txtAudioName;
        private final ImageButton btnPlay, btnShare, btnDelete;
        private final AudioLibraryActivity.AudioActionListener listener;

        ViewHolder(View view, AudioLibraryActivity.AudioActionListener listener) {
            this.listener = listener;
            txtAudioName = view.findViewById(R.id.txtAudioName);
            btnPlay = view.findViewById(R.id.btnPlay);
            btnShare = view.findViewById(R.id.btnShare);
            btnDelete = view.findViewById(R.id.btnDelete);
        }

        void bind(File audioFile) {
            txtAudioName.setText(audioFile.getName());
            btnPlay.setOnClickListener(v -> listener.onAudioAction(
                    AudioLibraryActivity.AudioAction.PLAY, audioFile));
            btnShare.setOnClickListener(v -> listener.onAudioAction(
                    AudioLibraryActivity.AudioAction.SHARE, audioFile));
            btnDelete.setOnClickListener(v -> listener.onAudioAction(
                    AudioLibraryActivity.AudioAction.DELETE, audioFile));
        }
    }
}