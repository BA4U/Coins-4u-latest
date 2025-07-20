package com.bg4u.coins4u.chat4u;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.bg4u.coins4u.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsFragment extends Fragment {

    private RecyclerView requestList;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<UserModel> receivedRequests;

    public RequestsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        // Updated to use the correct ID from your XML layout
        requestList = view.findViewById(R.id.chat_request_recyclerview);
        requestList.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        receivedRequests = new ArrayList<>();

        loadReceivedRequests();

        return view;
    }

    private void loadReceivedRequests() {
        db.collection("FriendRequests").document(currentUserId).collection("from")
                .get()
                .addOnSuccessListener(query -> {
                    receivedRequests.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String senderId = doc.getId();
                        fetchUserInfo(senderId);
                    }
                });
    }

    private void fetchUserInfo(String senderId) {
        db.collection("Users").document(senderId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                UserModel user = snapshot.toObject(UserModel.class);
                if (user != null) {
                    user.setUid(senderId);
                    receivedRequests.add(user);
                    requestList.setAdapter(new RequestAdapter(receivedRequests));
                }
            }
        });
    }

    public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

        private final List<UserModel> users;

        public RequestAdapter(List<UserModel> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.users_display_layout, parent, false);
            return new RequestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
            UserModel user = users.get(position);

            holder.username.setText(user.getName());
            holder.status.setText("Request received");
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfile())
                    .placeholder(R.drawable.user_icon_default)
                    .into(holder.profile);

            holder.acceptBtn.setVisibility(View.VISIBLE);
            holder.cancelBtn.setVisibility(View.VISIBLE);

            holder.acceptBtn.setOnClickListener(v -> acceptRequest(user.getUid(), position));
            holder.cancelBtn.setOnClickListener(v -> cancelRequest(user.getUid(), position));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("visit_user_id", user.getUid());
                intent.putExtra("visit_user_name", user.getName());
                intent.putExtra("visit_image", user.getProfile());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public class RequestViewHolder extends RecyclerView.ViewHolder {
            CircleImageView profile;
            TextView username, status;
            Button acceptBtn, cancelBtn;

            public RequestViewHolder(@NonNull View itemView) {
                super(itemView);
                profile = itemView.findViewById(R.id.users_profile_image);
                username = itemView.findViewById(R.id.users_profile_name);
                status = itemView.findViewById(R.id.users_status);
                acceptBtn = itemView.findViewById(R.id.request_accept_button);
                cancelBtn = itemView.findViewById(R.id.request_cancel_button);
            }
        }
    }

    private void acceptRequest(String senderId, int position) {
        Map<String, Object> friendMap = new HashMap<>();
        friendMap.put("timestamp", System.currentTimeMillis());

        db.collection("Friends").document(currentUserId).collection("List")
                .document(senderId).set(friendMap);

        db.collection("Friends").document(senderId).collection("List")
                .document(currentUserId).set(friendMap);

        db.collection("FriendRequests").document(currentUserId).collection("from")
                .document(senderId).delete();

        db.collection("FriendRequests").document(senderId).collection("to")
                .document(currentUserId).delete();

        receivedRequests.remove(position);
        requestList.getAdapter().notifyItemRemoved(position);
        Toast.makeText(getContext(), "Request Accepted", Toast.LENGTH_SHORT).show();
    }

    private void cancelRequest(String senderId, int position) {
        db.collection("FriendRequests").document(currentUserId).collection("from")
                .document(senderId).delete();

        db.collection("FriendRequests").document(senderId).collection("to")
                .document(currentUserId).delete();

        receivedRequests.remove(position);
        requestList.getAdapter().notifyItemRemoved(position);
        Toast.makeText(getContext(), "Request Cancelled", Toast.LENGTH_SHORT).show();
    }
}
