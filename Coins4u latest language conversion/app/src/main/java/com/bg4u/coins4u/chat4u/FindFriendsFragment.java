package com.bg4u.coins4u.chat4u;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Fragment for finding and adding new friends.
 * Features:
 * - Search users by name
 * - Show all users
 * - Send friend requests
 * - View user profiles
 */
public class FindFriendsFragment extends Fragment {

    private RecyclerView findFriendsRecyclerView;
    private EditText searchInput;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<UserModel> usersList;
    private FindFriendsAdapter adapter;

    // Cache of friend request status
    private final Map<String, String> requestsCache = new HashMap<>();
    private static final String STATUS_NOT_FRIENDS = "not_friends";
    private static final String STATUS_REQUEST_SENT = "request_sent";
    private static final String STATUS_REQUEST_RECEIVED = "request_received";
    private static final String STATUS_FRIENDS = "friends";

    public FindFriendsFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_friends, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Initialize UI components
        findFriendsRecyclerView = view.findViewById(R.id.find_friends_recyclerlist);
        searchInput = view.findViewById(R.id.search_username_input);

        // Setup RecyclerView
        findFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize user list and adapter
        usersList = new ArrayList<>();
        adapter = new FindFriendsAdapter(usersList);
        findFriendsRecyclerView.setAdapter(adapter);

        // Setup search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load all users initially
        loadAllUsers();

        return view;
    }

    /**
     * Load all users from Firestore
     */
    private void loadAllUsers() {
        db.collection("Users")
                .whereNotEqualTo("uid", currentUserId)  // Exclude current user
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    usersList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UserModel user = document.toObject(UserModel.class);
                        user.setUid(document.getId());
                        usersList.add(user);

                        // Check friendship status for each user
                        checkFriendshipStatus(user.getUid());
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Search users by name
     * @param searchText Text to search for in user names
     */
    private void searchUsers(String searchText) {
        if (searchText.isEmpty()) {
            // Load all users if search is empty
            loadAllUsers();
            return;
        }

        // Convert search text to lowercase
        String searchTextLowerCase = searchText.toLowerCase();

        // Search by nameLowerCase (case-insensitive)
        db.collection("Users")
                .whereGreaterThanOrEqualTo("nameLowerCase", searchTextLowerCase)
                .whereLessThanOrEqualTo("nameLowerCase", searchTextLowerCase + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    usersList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Skip current user
                        if (document.getId().equals(currentUserId)) continue;

                        UserModel user = document.toObject(UserModel.class);
                        user.setUid(document.getId());
                        usersList.add(user);

                        // Check friendship status for each user
                        checkFriendshipStatus(user.getUid());
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error searching users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Check friendship status with another user
     * @param userId User ID to check
     */
    private void checkFriendshipStatus(String userId) {
        // First check if they are already friends
        db.collection("Friends")
                .document(currentUserId)
                .collection("List")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Already friends
                        requestsCache.put(userId, STATUS_FRIENDS);
                        adapter.notifyDataSetChanged();
                    } else {
                        // Not friends, check if request is sent
                        checkSentRequests(userId);
                    }
                });
    }

    /**
     * Check if a friend request has been sent to a user
     * @param userId User ID to check
     */
    private void checkSentRequests(String userId) {
        db.collection("FriendRequests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Request sent
                        requestsCache.put(userId, STATUS_REQUEST_SENT);
                    } else {
                        // Check if request is received
                        checkReceivedRequests(userId);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Check if a friend request has been received from a user
     * @param userId User ID to check
     */
    private void checkReceivedRequests(String userId) {
        db.collection("FriendRequests")
                .whereEqualTo("senderId", userId)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Request received
                        requestsCache.put(userId, STATUS_REQUEST_RECEIVED);
                    } else {
                        // Not friends, no requests
                        requestsCache.put(userId, STATUS_NOT_FRIENDS);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Send a friend request to a user
     * @param userId ID of the user to send request to
     */
    private void sendFriendRequest(String userId) {
        // Create a new friend request
        FriendRequest request = new FriendRequest(currentUserId, userId);

        // Save the request in Firestore
        db.collection("FriendRequests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    // Update cache
                    requestsCache.put(userId, STATUS_REQUEST_SENT);
                    adapter.notifyDataSetChanged();

                    Toast.makeText(getContext(), "Friend request sent", Toast.LENGTH_SHORT).show();

                    // Send notification
                    sendNotification(userId, "New Friend Request",
                            "You have received a new friend request", "friend_request");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cancel a sent friend request
     * @param userId ID of the user to cancel request to
     */
    private void cancelFriendRequest(String userId) {
        // Find and delete the request
        db.collection("FriendRequests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    // Update cache
                    requestsCache.put(userId, STATUS_NOT_FRIENDS);
                    adapter.notifyDataSetChanged();

                    Toast.makeText(getContext(), "Friend request cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to cancel request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Accept a received friend request
     * @param userId ID of the user who sent the request
     */
    private void acceptFriendRequest(String userId) {
        // Find the request
        db.collection("FriendRequests")
                .whereEqualTo("senderId", userId)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Update request status
                        document.getReference().update("status", FriendRequest.STATUS_ACCEPTED);

                        // Add to friends collection for both users
                        Map<String, Object> friendData = new HashMap<>();
                        friendData.put("timestamp", new Date());

                        db.collection("Friends").document(currentUserId)
                                .collection("List").document(userId).set(friendData);

                        db.collection("Friends").document(userId)
                                .collection("List").document(currentUserId).set(friendData);

                        // Update cache
                        requestsCache.put(userId, STATUS_FRIENDS);
                        adapter.notifyDataSetChanged();

                        Toast.makeText(getContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();

                        // Send notification
                        sendNotification(userId, "Friend Request Accepted",
                                "Your friend request has been accepted", "friend_accepted");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to accept request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Send notification to a user
     * @param targetUserId User to notify
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type
     */
    private void sendNotification(String targetUserId, String title, String message, String type) {
        // Get the target user's FCM token
        db.collection("Users").document(targetUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user != null && user.getToken() != null) {
                        // Here you would implement the FCM notification sending
                        // This would typically be handled by a server or Cloud Function
                        // For now, we'll just create a placeholder

                        // Example placeholder for FCM notification logic
                        // Normally you'd use a server-side solution or Cloud Functions
                  /*
                  FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(user.getToken())
                      .setMessageId(String.valueOf(System.currentTimeMillis()))
                      .addData("title", title)
                      .addData("message", message)
                      .addData("type", type)
                      .addData("senderId", currentUserId)
                      .build());
                  */
                    }
                });
    }

    /**
     * Adapter for displaying users in the find friends screen
     */
    private class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.FindFriendsViewHolder> {

        private final List<UserModel> users;

        FindFriendsAdapter(List<UserModel> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.users_display_layout, parent, false);
            return new FindFriendsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FindFriendsViewHolder holder, int position) {
            UserModel user = users.get(position);

            // Set user info
            holder.username.setText(user.getName());

            if (user.getStatus() != null && !user.getStatus().isEmpty()) {
                holder.status.setText(user.getStatus());
            } else {
                holder.status.setText("Hi there! I'm using Coins4U Chat");
            }

            // Load profile image
            if (user.getProfile() != null && !user.getProfile().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(user.getProfile())
                        .placeholder(R.drawable.user_icon_default)
                        .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.user_icon_default);
            }

            // Set buttons based on friendship status
            String status = requestsCache.getOrDefault(user.getUid(), STATUS_NOT_FRIENDS);

            switch (status) {
                case STATUS_FRIENDS:
                    // Already friends
                    holder.acceptBtn.setVisibility(View.GONE);
                    holder.cancelBtn.setVisibility(View.GONE);
                    holder.status.setText("Connected");
                    break;

                case STATUS_REQUEST_SENT:
                    // Request sent, show cancel button
                    holder.acceptBtn.setVisibility(View.GONE);
                    holder.cancelBtn.setVisibility(View.VISIBLE);
                    holder.cancelBtn.setText("Cancel Request");

                    holder.cancelBtn.setOnClickListener(v -> cancelFriendRequest(user.getUid()));
                    break;

                case STATUS_REQUEST_RECEIVED:
                    // Request received, show accept button
                    holder.acceptBtn.setVisibility(View.VISIBLE);
                    holder.cancelBtn.setVisibility(View.VISIBLE);
                    holder.acceptBtn.setText("Accept");
                    holder.cancelBtn.setText("Decline");

                    holder.acceptBtn.setOnClickListener(v -> acceptFriendRequest(user.getUid()));
                    holder.cancelBtn.setOnClickListener(v -> {
                        // Implement decline logic here
                    });
                    break;

                default:
                    // Not friends, show send request button
                    holder.acceptBtn.setVisibility(View.VISIBLE);
                    holder.cancelBtn.setVisibility(View.GONE);
                    holder.acceptBtn.setText("Send Request");

                    holder.acceptBtn.setOnClickListener(v -> sendFriendRequest(user.getUid()));
                    break;
            }

            // Set click listener to view profile
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

        class FindFriendsViewHolder extends RecyclerView.ViewHolder {
            CircleImageView profileImage;
            TextView username, status;
            Button acceptBtn, cancelBtn;

            FindFriendsViewHolder(@NonNull View itemView) {
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