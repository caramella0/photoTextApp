package com.phototext.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.phototext.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.AudioViewHolder> {
    private static AudioLibraryActivity.AudioActionListener actionListener;
    private List<File> audioFiles;

    public AudioListAdapter(AudioLibraryActivity.AudioActionListener listener) {
        actionListener = listener;
        this.audioFiles = new ArrayList<>();
    }

    public void updateList(List<File> newFiles) {
        this.audioFiles = new ArrayList<>(newFiles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio_file, parent, false);
        return new AudioViewHolder(view, actionListener);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        holder.bind(audioFiles.get(position));
    }

    @Override
    public int getItemCount() {
        return audioFiles.size();
    }

    static class AudioViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtAudioName;
        private final ImageButton btnPlay, btnShare, btnDelete;

        AudioViewHolder(View view, AudioLibraryActivity.AudioActionListener listener) {
            super(view);
            txtAudioName = view.findViewById(R.id.txtAudioName);
            btnPlay = view.findViewById(R.id.btnPlay);
            btnShare = view.findViewById(R.id.btnShare);
            btnDelete = view.findViewById(R.id.btnDelete);

            view.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAudioAction(AudioLibraryActivity.AudioAction.PLAY,
                            (File) txtAudioName.getTag());
                }
            });
        }

        void bind(File audioFile) {
            txtAudioName.setText(audioFile.getName());
            txtAudioName.setTag(audioFile);

            btnPlay.setOnClickListener(v ->
                    actionListener.onAudioAction(AudioLibraryActivity.AudioAction.PLAY, audioFile));
            btnShare.setOnClickListener(v ->
                    actionListener.onAudioAction(AudioLibraryActivity.AudioAction.SHARE, audioFile));
            btnDelete.setOnClickListener(v ->
                    actionListener.onAudioAction(AudioLibraryActivity.AudioAction.DELETE, audioFile));
        }
    }
}