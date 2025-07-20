package com.bg4u.coins4u.chat4u;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.chat.MessageAdapter;
import com.bg4u.coins4u.chat.Messages;
import com.bg4u.coins4u.databinding.ActivityChatBinding;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import okhttp3.*;

public class ChatActivity extends AppCompatActivity {

    private String messageSenderId, messageReceiverId, receiverToken;
    private ActivityChatBinding binding;
    private EditText messageInput;
    private MessageAdapter adapter;
    private List<Messages> messageList = new ArrayList<>();
    private FirebaseFirestore db;
    private RecyclerView messagesRecyclerView;
    private Uri fileUri;
    private ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> resultLauncher;
    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "YOUR_FCM_SERVER_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        messageSenderId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        messageReceiverId = getIntent().getStringExtra("visit_user_id");
        String receiverName = getIntent().getStringExtra("visit_user_name");
        String receiverImage = getIntent().getStringExtra("visit_image");

        setupToolbar(receiverName);
        setupUI(receiverImage);
        fetchReceiverToken();
        loadMessages();
        markMessagesAsSeen();
    }

    private void setupToolbar(String receiverName) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
        binding.customProfileName.setText(receiverName);
    }

    private void setupUI(String receiverImage) {
        Glide.with(this).load(receiverImage).placeholder(R.drawable.user_icon_default)
                .into(binding.customProfileImage);

        messageInput = binding.inputMessages;
        messagesRecyclerView = binding.privateMessageListOfUsers;
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(messageList);
        messagesRecyclerView.setAdapter(adapter);

        binding.backBtn.setOnClickListener(v -> onBackPressed());
        binding.sendMessageBtn.setOnClickListener(v -> sendMessage());
        binding.sendFilesBtn.setOnClickListener(v -> chooseFile());

        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                fileUri = result.getData().getData();
                uploadFile();
            }
        });
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        resultLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void uploadFile() {
        if (fileUri == null) return;
        progressDialog = ProgressDialog.show(this, "Uploading", "Sending image...", true);

        StorageReference ref = FirebaseStorage.getInstance().getReference().child("chat_images").child(UUID.randomUUID() + ".jpg");
        ref.putFile(fileUri).continueWithTask(task -> ref.getDownloadUrl()).addOnSuccessListener(uri -> {
            sendMessage(uri.toString(), "image");
            progressDialog.dismiss();
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (!text.isEmpty()) sendMessage(text, "text");
    }

    private void sendMessage(String content, String type) {
        String conversationId = generateConversationId(messageSenderId, messageReceiverId);
        DocumentReference newMsgRef = db.collection("users")
                .document(messageSenderId)
                .collection("chats")
                .document(messageReceiverId)
                .collection("messages")
                .document();

        Messages msg = new Messages(messageSenderId, messageReceiverId, content, type,
                newMsgRef.getId(), new Date(), false);

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("message", content);
        msgMap.put("type", type);
        msgMap.put("from", messageSenderId);
        msgMap.put("to", messageReceiverId);
        msgMap.put("messageID", newMsgRef.getId());
        msgMap.put("timestamp", FieldValue.serverTimestamp());
        msgMap.put("seen", false);

        newMsgRef.set(msgMap);

        // Duplicate for receiver
        db.collection("users").document(messageReceiverId)
                .collection("chats").document(messageSenderId)
                .collection("messages").document(newMsgRef.getId())
                .set(msgMap);

        messageInput.setText("");
        sendNotification(content);
    }

    private void loadMessages() {
        db.collection("users")
                .document(messageSenderId)
                .collection("chats")
                .document(messageReceiverId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Messages msg = doc.toObject(Messages.class);
                            if (msg != null) messageList.add(msg);
                        }
                        adapter.notifyDataSetChanged();
                        messagesRecyclerView.smoothScrollToPosition(messageList.size());
                    }
                });
    }

    private void markMessagesAsSeen() {
        db.collection("users")
                .document(messageSenderId)
                .collection("chats")
                .document(messageReceiverId)
                .collection("messages")
                .whereEqualTo("to", messageSenderId)
                .whereEqualTo("seen", false)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        doc.getReference().update("seen", true);
                    }
                });
    }

    private void fetchReceiverToken() {
        db.collection("users").document(messageReceiverId)
                .get()
                .addOnSuccessListener(doc -> receiverToken = doc.getString("device_token"));
    }

    private void sendNotification(String message) {
        try {
            JSONObject json = new JSONObject();
            JSONObject notif = new JSONObject();
            notif.put("title", "New Message");
            notif.put("body", message);

            json.put("notification", notif);
            json.put("to", receiverToken);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(FCM_API)
                    .post(body)
                    .header("Authorization", "Bearer " + SERVER_KEY)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("FCM", "Send failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    assert response.body() != null;
                    Log.d("FCM", "Notification sent: " + response.body().string());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateConversationId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}