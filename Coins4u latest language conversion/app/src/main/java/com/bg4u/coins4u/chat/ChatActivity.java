package com.bg4u.coins4u.chat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivityChatBinding;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    private String messageReceiverId;
    private String messageSenderId;
    private TextView userLastSeen;
    private String receiverToken; // Variable to store the receiver's FCM token
    private EditText messageSentInput;
    private DatabaseReference rootRef;
    private final List<Messages> messagesList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private RecyclerView userMessageRecyclerView;
    
    private String saveCurrentTime, saveCurrentDate;
    private String checker = "", myUrl = "";
    private StorageTask<UploadTask.TaskSnapshot> uploadTask;
    FirebaseDatabase database;
    private Uri fileUri;
    private ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> resultLauncher;
    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "AAAAI_IXuck:APA91bF5jbU8Wp7Ryllviy4Ch_uuawzbaI9bm3pp7R-oKVFfe1rj-3JZkIiwhGIEsDhnXIdfh1qFTAB9hyJpni77s-BRywRXkHTmIusiJCddOM1Tw_PCf9g9LELnCE2IygqL4s-VXzdB"; // Replace with your Firebase Server Key
    private Context mContext;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityChatBinding binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize the resultLauncher using registerForActivityResult
        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Handle the result of the file selection here in onActivityResult
                        onActivityResult(555, RESULT_OK, result.getData());
                    }
                });
        
        progressDialog = new ProgressDialog(this);
        
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        rootRef = FirebaseDatabase.getInstance().getReference();
        
        messageReceiverId = getIntent().getStringExtra("visit_user_id");
        String getMessageReceiverName = getIntent().getStringExtra("visit_user_name");
        String messageReceiverImage = getIntent().getStringExtra("visit_image");
        
        // With this line
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
        
        // Set click listener for btnBack
        binding.backBtn.setOnClickListener(v -> {
            // Handle the click event here (e.g., go back to the previous screen)
            onBackPressed();
        });
        
        TextView username = binding.customProfileName;
        userLastSeen = binding.customUserLastSeen;
        CircleImageView profilePicture = binding.customProfileImage;
        ImageButton sendMessageButton = binding.sendMessageBtn;
        ImageButton sendFileButton = binding.sendFilesBtn;
        
        messageSentInput = binding.inputMessages;
        
        messageAdapter = new MessageAdapter(messagesList);
        userMessageRecyclerView = binding.privateMessageListOfUsers;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        userMessageRecyclerView.setLayoutManager(linearLayoutManager);
        userMessageRecyclerView.setAdapter(messageAdapter);
        // Fetch the receiver's FCM token from the Realtime Database
        fetchReceiverToken();
        
        Calendar calendar = Calendar.getInstance();
        
        DateFormat currentDate = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        saveCurrentDate = currentDate.format(calendar.getTime());
        
        DateFormat currentTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        saveCurrentTime = currentTime.format(calendar.getTime());
        
        username.setText(getMessageReceiverName);
        Glide.with(this).load(messageReceiverImage).placeholder(R.drawable.user_icon_default).into(profilePicture);
        displayLastSeen();
        sendMessageButton.setOnClickListener(v -> sendMessage());
        
        sendFileButton.setOnClickListener(v -> {
            checker = "image";
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            resultLauncher.launch(Intent.createChooser(intent, "Select Image"));
        });
        
        rootRef.child("Messages").child(messageSenderId).child(messageReceiverId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);
                messageAdapter.notifyDataSetChanged();
                userMessageRecyclerView.smoothScrollToPosition(Objects.requireNonNull(userMessageRecyclerView.getAdapter()).getItemCount());
            }
    
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
    
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
    
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
    
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 555 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            progressDialog.setTitle("Sending File");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            
            fileUri = data.getData();
            if (!TextUtils.isEmpty(checker)) {
                if (checker.equals("image")) {
                    // Handle image file upload
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
                    
                    final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverId;
                    final String messageReceiverRef = "Messages/" + messageReceiverId + "/" + messageSenderId;
                    
                    DatabaseReference userMessageKeyRef = rootRef.child("Messages").child(messageSenderId).child(messageReceiverId).push();
                    final String messagePushID = userMessageKeyRef.getKey();
                    
                    final StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");
                    uploadTask = filePath.putFile(fileUri);
                    uploadTask.continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw Objects.requireNonNull(task.getException());
                        }
                        return filePath.getDownloadUrl();
                    }).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUrl = task.getResult();
                            myUrl = downloadUrl.toString();
                            
                            Map<String, Object> messageTextBody = new HashMap<>();
                            messageTextBody.put("message", myUrl);
                            messageTextBody.put("name", fileUri.getLastPathSegment());
                            messageTextBody.put("type", checker);
                            messageTextBody.put("from", messageSenderId);
                            messageTextBody.put("to", messageReceiverId);
                            messageTextBody.put("messageID", messagePushID);
                            messageTextBody.put("time", saveCurrentTime);
                            messageTextBody.put("date", saveCurrentDate);
                            
                            Map<String, Object> messageBodyDetails = new HashMap<>();
                            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);
                            
                            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener((OnCompleteListener<Void>) task1 -> {
                                if (task1.isSuccessful()) {
                                    // If the message is sent successfully, call the sendNotification method
                                    sendNotification("New image", receiverToken);
                                    Toast.makeText(ChatActivity.this, "Message sent...", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ChatActivity.this, "Error:", Toast.LENGTH_SHORT).show();
                                }
                                progressDialog.dismiss();
                                messageSentInput.setText("");
                            });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(ChatActivity.this, "Error:", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Handle other file types (e.g., documents) if needed
                    // ...
                }
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public void displayLastSeen() {
        rootRef.child("Users").child(messageReceiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("userState")) {
                    // Changed the code to check for the child "userState" before accessing its properties
                    String state = Objects.requireNonNull(dataSnapshot.child("userState").child("state").getValue()).toString();
                    String date = Objects.requireNonNull(dataSnapshot.child("userState").child("date").getValue()).toString();
                    String time = Objects.requireNonNull(dataSnapshot.child("userState").child("time").getValue()).toString();
                    
                    if (state.equals("online")) {
                        userLastSeen.setText("online");
                        // Inside your activity or fragment's onCreate or onCreateView method:
                        int borderColor = ContextCompat.getColor(ChatActivity.this, R.color.green);

                        // Then, inside your ValueEventListener or any other method:
                        CircleImageView customProfileImage = findViewById(R.id.custom_profile_image);
                        customProfileImage.setBorderColor(borderColor);
                    } else if (state.equals("offline")) {
                        // Do something if the user is offline
                        userLastSeen.setText("Last seen :" + " " + date + " at " + time);
                    }
                } else {
                    userLastSeen.setText("offline");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors or failures in fetching last seen details
                // ...
            }
        });
    }
    
    private void sendMessage() {
        String messageText = messageSentInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Type a message...", Toast.LENGTH_SHORT).show();
        } else {
            String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverId;
            String messageReceiverRef = "Messages/" + messageReceiverId + "/" + messageSenderId;
            
            DatabaseReference userMessageKeyRef = rootRef.child("Messages").child(messageSenderId).child(messageReceiverId).push();
            String messagePushID = userMessageKeyRef.getKey();
            Map<String, Object> messageTextBody = new HashMap<>();
            messageTextBody.put("message", messageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderId);
            messageTextBody.put("to", messageReceiverId);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);
            
            Map<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);
            
            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // If the message is sent successfully, call the sendNotification method
                    sendNotification(messageText, receiverToken);
                    // Toast.makeText(ChatActivity.this, "Message sent Successfully...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatActivity.this, "Error:", Toast.LENGTH_SHORT).show();
                }
                messageSentInput.setText("");
            });
        }
    }
    
    // Method to send chat notification to another user
    private void fetchReceiverToken() {
        DatabaseReference receiverRef = FirebaseDatabase.getInstance().getReference().child("Users").child(messageReceiverId);
        receiverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("device_token")) {
                    receiverToken = Objects.requireNonNull(dataSnapshot.child("device_token").getValue()).toString();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors or failures in fetching the token
                // ...
            }
        });
    }
    
    // Assuming you have UserModel currentUser and String profileImageURL defined somewhere in your code
    // Updated sendNotification() method
    private void sendNotification(String message, String receiverToken) {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String senderName = dataSnapshot.child("name").getValue(String.class);
                    String profileImageURL = dataSnapshot.child("image").getValue(String.class);
                    try {
                        JSONObject jsonObject = new JSONObject();
                        
                        JSONObject notificationObj = new JSONObject();
                        notificationObj.put("title", senderName);
                        notificationObj.put("body", message);
                        
                        JSONObject dataObj = new JSONObject();
                        dataObj.put("senderName", currentUserId); // Add current user id
                        dataObj.put("profileImageURL", profileImageURL); // Add the profile image URL to the notification data.
                        
                        jsonObject.put("notification", notificationObj);
                        jsonObject.put("data", dataObj);
                        jsonObject.put("to", receiverToken);
                        
                        callApi(jsonObject);
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors or failures in fetching the user details
                // ...
            }
        });
    }
    
    void callApi(JSONObject jsonObject){
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(jsonObject.toString(),JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(FCM_API)
                .post(body)
                .header("Authorization","Bearer " + SERVER_KEY)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                String responseData = response.body().string();
                Log.d("FCM_RESPONSE", responseData);
            }
            
        });
        
    }
    
}
