package com.bg4u.coins4u.chat4u;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    private RecyclerView chatsList;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<ChatItem> chats;
    private ChatsAdapter adapter;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    public ChatsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        chatsList = view.findViewById(R.id.chats_list);
        chatsList.setLayoutManager(new LinearLayoutManager(getContext()));

        chats = new ArrayList<>();
        adapter = new ChatsAdapter(chats);
        chatsList.setAdapter(adapter);

        loadChats();

        return view;
    }

    private void loadChats() {
        db.collection("users")
                .document(currentUserId)
                .collection("chats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chats.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String friendId = doc.getId();
                        loadFriendInfo(friendId);
                    }
                });
    }

    private void loadFriendInfo(String friendId) {
        db.collection("users")
                .document(friendId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        UserModel user = userDoc.toObject(UserModel.class);
                        if (user != null) {
                            user.setUid(userDoc.getId());
                            fetchLastMessage(user);
                        }
                    }
                });
    }

    private void fetchLastMessage(UserModel friend) {
        db.collection("users")
                .document(currentUserId)
                .collection("chats")
                .document(friend.getUid())
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Messages lastMessage = null;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        lastMessage = doc.toObject(Messages.class);
                    }

                    ChatItem chatItem = new ChatItem(friend, lastMessage);

                    boolean exists = false;
                    for (int i = 0; i < chats.size(); i++) {
                        if (chats.get(i).friend.getUid().equals(friend.getUid())) {
                            chats.set(i, chatItem);
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        chats.add(chatItem);
                    }

                    chats.sort((a, b) -> {
                        if (a.lastMessage == null && b.lastMessage == null) return 0;
                        if (a.lastMessage == null) return 1;
                        if (b.lastMessage == null) return -1;
                        return b.lastMessage.getTimestamp().compareTo(a.lastMessage.getTimestamp());
                    });

                    adapter.notifyDataSetChanged();
                });
    }

    private String formatTime(Date timestamp) {
        if (timestamp == null) return "";

        Date now = new Date();
        long diff = now.getTime() - timestamp.getTime();
        long days = diff / (24 * 60 * 60 * 1000);

        return days < 1 ? timeFormat.format(timestamp) : dateFormat.format(timestamp);
    }

    private static class ChatItem {
        UserModel friend;
        Messages lastMessage;

        ChatItem(UserModel friend, Messages lastMessage) {
            this.friend = friend;
            this.lastMessage = lastMessage;
        }
    }

    private class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

        private final List<ChatItem> chats;

        ChatsAdapter(List<ChatItem> chats) {
            this.chats = chats;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatItem chatItem = chats.get(position);
            UserModel friend = chatItem.friend;
            Messages lastMessage = chatItem.lastMessage;

            holder.username.setText(friend.getName());
            if (friend.isOnline()) {
                holder.status.setText("Online");
            } else if (friend.getLastSeen() != null) {
                holder.status.setText("Last seen: " + formatTime(friend.getLastSeen()));
            } else {
                holder.status.setText("Offline");
            }

            if (lastMessage != null && !lastMessage.getMessage().isEmpty()) {
                String preview = lastMessage.getType().equals("image") ? "📷 Image" : lastMessage.getMessage();
                if (preview.length() > 30) {
                    preview = preview.substring(0, 27) + "...";
                }
                holder.status.setText(preview);
            }

            if (friend.getProfile() != null && !friend.getProfile().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(friend.getProfile())
                        .placeholder(R.drawable.user_icon_default)
                        .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.user_icon_default);
            }

            holder.acceptBtn.setVisibility(View.GONE);
            holder.cancelBtn.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("visit_user_id", friend.getUid());
                intent.putExtra("visit_user_name", friend.getName());
                intent.putExtra("visit_image", friend.getProfile());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            CircleImageView profileImage;
            TextView username, status;
            TextView acceptBtn, cancelBtn;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.users_profile_image);
                username = itemView.findViewById(R.id.users_profile_name);
                status = itemView.findViewById(R.id.users_status);
                acceptBtn = itemView.findViewById(R.id.request_accept_button);
                cancelBtn = itemView.findViewById(R.id.request_cancel_button);
            }
        }
    }
}
