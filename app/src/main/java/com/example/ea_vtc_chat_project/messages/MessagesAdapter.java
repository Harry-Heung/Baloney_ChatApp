package com.example.ea_vtc_chat_project.messages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ea_vtc_chat_project.R;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private List<MessagesList> messagesLists;
    private final Context context;
    private OnItemClickListener onItemClickListener;

    public MessagesAdapter(List<MessagesList> messagesLists, Context context) {
        this.messagesLists = messagesLists;
        this.context = context;
    }

    // 定义接口
    public interface OnItemClickListener {
        void onItemClick(int position, View v);
    }

    // 设置监听器
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.messages_adapter_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessagesList messagesList = messagesLists.get(position);

        // 显示用户名和最后一条消息
        holder.userName.setText(messagesList.getUserFullName());
        holder.lastMessage.setText(messagesList.getLastMessage());

        // 更新未读消息计数
        updateUnseenMessages(holder, messagesList.getUnseenMessagesCount());

        // 加载用户头像
        loadImage(holder.profilePic, messagesList.getImageUrl());

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messagesLists.size();
    }

    public void updateMessages(List<MessagesList> newMessagesLists) {
        this.messagesLists = newMessagesLists;
        notifyDataSetChanged();
    }

    private void updateUnseenMessages(ViewHolder holder, int unseenMessagesCount) {
        if (unseenMessagesCount > 0) {
            holder.unseenMessages.setText(String.valueOf(unseenMessagesCount));
            holder.unseenMessages.setVisibility(View.VISIBLE);
        } else {
            holder.unseenMessages.setVisibility(View.GONE);
        }
    }

    private void loadImage(ImageView imageView, String imageUrl) {
        if (imageUrl != null && !imageUrl.equals("default")) {
            Glide.with(context).load(imageUrl).into(imageView);
        } else {
            imageView.setImageResource(R.drawable.usericon_default);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView userName;
        private final TextView lastMessage;
        private final TextView unseenMessages;
        private final ImageView profilePic;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.UniqueName); // 修改这里的ID以匹配你的布局文件
            lastMessage = itemView.findViewById(R.id.lastMessage);
            unseenMessages = itemView.findViewById(R.id.unseenMessages);
            profilePic = itemView.findViewById(R.id.profilePic);
        }
    }
}