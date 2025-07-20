package com.bg4u.coins4u.chat4u;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.google.firebase.Timestamp;
import java.text.DateFormat;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> chatMessages;
    private String currentUserId;

    public ChatAdapter(List<ChatMessage> chatMessages, String currentUserId) {
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // For simplicity, using one layout; you can create separate layouts for sent and received messages.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        holder.textViewMessage.setText(message.getMessage());
        Timestamp timestamp = message.getTimestamp();
        String formattedTime = DateFormat.getTimeInstance().format(timestamp.toDate());
        holder.textViewTimestamp.setText(formattedTime);
        // Optionally, change appearance depending on whether the message is sent or received.
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage, textViewTimestamp;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}
