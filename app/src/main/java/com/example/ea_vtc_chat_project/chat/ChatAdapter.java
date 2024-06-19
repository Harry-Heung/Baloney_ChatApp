package com.example.ea_vtc_chat_project.chat;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ea_vtc_chat_project.MemoryData;
import com.example.ea_vtc_chat_project.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatList> chatLists;
    private final Context context;
    private final MediaPlayer mediaPlayer;
    private final Handler handler = new Handler();
    private ViewHolder currentlyPlayingHolder;

    public ChatAdapter(List<ChatList> chatLists, Context context) {
        this.chatLists = chatLists;
        this.context = context;
        this.mediaPlayer = new MediaPlayer();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_adapter_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatList chat = chatLists.get(position);

        if (chat.getUniqueName().equals(MemoryData.getUniqueName(context))) {
            setupMyMessage(holder, chat);
        } else {
            setupOppoMessage(holder, chat);
        }
    }

    @Override
    public int getItemCount() {
        return chatLists.size();
    }

    public void updateChatList(List<ChatList> chatLists) {
        this.chatLists = chatLists;
        notifyDataSetChanged();
    }

    private void setupMyMessage(ViewHolder holder, ChatList chat) {
        holder.myLayout.setVisibility(View.VISIBLE);
        holder.oppoLayout.setVisibility(View.GONE);

        setupMessageContent(holder.myImage, holder.myMessage, holder.myAudioLayout, holder.myAudio, holder.myAudioTime, holder.myMsgTime, chat);
    }

    private void setupOppoMessage(ViewHolder holder, ChatList chat) {
        holder.myLayout.setVisibility(View.GONE);
        holder.oppoLayout.setVisibility(View.VISIBLE);

        setupMessageContent(holder.oppoImage, holder.oppoMessage, holder.oppoAudioLayout, holder.oppoAudio, holder.oppoAudioTime, holder.oppoMsgTime, chat);
    }

    private void setupMessageContent(ImageView imageView, TextView messageView, LinearLayout audioLayout, ImageView audioButton, TextView audioTime, TextView msgTime, ChatList chat) {
        if (chat.getImageUrl() != null && !chat.getImageUrl().isEmpty()) {
            imageView.setVisibility(View.VISIBLE);
            Glide.with(context).load(chat.getImageUrl()).into(imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }

        if (chat.getAudioUrl() != null && !chat.getAudioUrl().isEmpty()) {
            audioLayout.setVisibility(View.VISIBLE);
            audioButton.setOnClickListener(v -> handleAudioPlayPause(chat.getAudioUrl(), audioButton, audioTime));
            audioTime.setText(formatAudioTime(0, getAudioDuration(chat.getAudioUrl())));
        } else {
            audioLayout.setVisibility(View.GONE);
        }

        if (chat.getMessage() != null && !chat.getMessage().isEmpty()) {
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(chat.getMessage());
        } else {
            messageView.setVisibility(View.GONE);
        }

        msgTime.setText(chat.getTime());
    }

    private void handleAudioPlayPause(String audioUrl, ImageView audioButton, TextView audioTime) {
        if (currentlyPlayingHolder != null) {
            stopCurrentAudio();
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setPlayButton(audioButton);
        } else {
            playAudio(audioUrl, audioButton, audioTime);
        }
    }

    private void playAudio(String audioUrl, ImageView audioButton, TextView audioTime) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepare();
            mediaPlayer.start();

            setPauseButton(audioButton);
            updateProgress(audioButton, audioTime);

            mediaPlayer.setOnCompletionListener(mp -> {
                setPlayButton(audioButton);
                audioTime.setText(formatAudioTime(0, mediaPlayer.getDuration()));
                handler.removeCallbacksAndMessages(null);
            });

        } catch (IOException e) {
            Toast.makeText(context, "Audio playback failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopCurrentAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            setPlayButton(currentlyPlayingHolder.getAudioButton());
            currentlyPlayingHolder = null;
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void updateProgress(ImageView audioButton, TextView audioTime) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    audioTime.setText(formatAudioTime(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration()));
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    private String formatAudioTime(int currentPosition, int duration) {
        return String.format(Locale.getDefault(), "%02d:%02d/%02d:%02d",
                (currentPosition / 1000) / 60,
                (currentPosition / 1000) % 60,
                (duration / 1000) / 60,
                (duration / 1000) % 60);
    }

    private int getAudioDuration(String audioUrl) {
        MediaPlayer tempMediaPlayer = new MediaPlayer();
        try {
            tempMediaPlayer.setDataSource(audioUrl);
            tempMediaPlayer.prepare();
            return tempMediaPlayer.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            tempMediaPlayer.release();
        }
        return 0;
    }

    private void setPlayButton(ImageView audioButton) {
        audioButton.setImageResource(R.drawable.baseline_play_arrow_24);
    }

    private void setPauseButton(ImageView audioButton) {
        audioButton.setImageResource(R.drawable.baseline_pause_24);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView oppoMessage, oppoMsgTime, myMessage, myMsgTime, oppoAudioTime, myAudioTime;
        ImageView oppoImage, myImage, oppoAudio, myAudio;
        LinearLayout oppoLayout, myLayout, oppoAudioLayout, myAudioLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            oppoMessage = itemView.findViewById(R.id.oppoMessage);
            oppoMsgTime = itemView.findViewById(R.id.oppoMsgTime);
            myMessage = itemView.findViewById(R.id.myMessage);
            myMsgTime = itemView.findViewById(R.id.myMsgTime);
            oppoImage = itemView.findViewById(R.id.oppoImage);
            myImage = itemView.findViewById(R.id.myImage);
            oppoAudio = itemView.findViewById(R.id.oppoAudio);
            myAudio = itemView.findViewById(R.id.myAudio);
            oppoLayout = itemView.findViewById(R.id.oppoLayout);
            myLayout = itemView.findViewById(R.id.myLayout);
            oppoAudioLayout = itemView.findViewById(R.id.oppoAudioLayout);
            myAudioLayout = itemView.findViewById(R.id.myAudioLayout);
            oppoAudioTime = itemView.findViewById(R.id.oppoAudioTime);
            myAudioTime = itemView.findViewById(R.id.myAudioTime);
        }

        public ImageView getAudioButton() {
            return myAudio.getVisibility() == View.VISIBLE ? myAudio : oppoAudio;
        }
    }
}