package com.bg4u.coins4u.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsFragment extends Fragment {
    
    private View view;
    private RecyclerView recyclerView;
    private DatabaseReference chatRequestRef, userRef, contactRef;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private FirebaseRecyclerAdapter<Contacts, RequestViewHolder> adapter;
    
    public RequestsFragment() {
        // Required empty public constructor
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_requests, container, false);
        recyclerView = view.findViewById(R.id.chat_request_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatRequestRef.child(currentUserId), Contacts.class)
                .build();
        
        adapter = new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull Contacts model) {
                holder.acceptButton.setVisibility(View.VISIBLE);
                holder.rejectButton.setVisibility(View.VISIBLE);
                
                final String userId = getRef(position).getKey();
                
                DatabaseReference requestTypeRef = getRef(position).child("request_type").getRef();
                requestTypeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String type = dataSnapshot.getValue().toString();
                            if (type.equals("received")) {
                                // Received request
                                setReceivedRequestData(holder, userId);
                            } else if (type.equals("sent")) {
                                // Sent request
                                setSentRequestData(holder, userId);
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error
                    }
                });
            }
            
            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                return new RequestViewHolder(view);
            }
        };
        
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
    
    private void setReceivedRequestData(final RequestViewHolder holder, final String userId) {
        userRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    final String requestImage = dataSnapshot.hasChild("image") ?
                            dataSnapshot.child("image").getValue().toString() : "";
                    
                    final String requestUsername = dataSnapshot.child("name").getValue().toString();
                    
                    holder.username.setText(requestUsername);
                    holder.userStatus.setText("Wants to connect with you");
                    holder.acceptButton.setText("Accept Request");
                    holder.acceptButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            acceptChatRequest(userId);
                        }
                    });
                    
                    holder.rejectButton.setText("Reject Request");
                    holder.rejectButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cancelChatRequest(userId);
                        }
                    });
                    
                    if (!requestImage.isEmpty()) {
                        Picasso.get().load(requestImage)
                                .placeholder(R.drawable.user_icon_default)
                                .into(holder.profilePicture);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }
    
    private void setSentRequestData(final RequestViewHolder holder, final String userId) {
        userRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    final String requestImage = dataSnapshot.hasChild("image") ?
                            dataSnapshot.child("image").getValue().toString() : "";
                    
                    final String requestUsername = dataSnapshot.child("name").getValue().toString();
                    
                    holder.username.setText(requestUsername);
                    holder.userStatus.setText(getString(R.string.request_sent_to) + requestUsername);
                    holder.acceptButton.setVisibility(View.GONE);
                    
                    holder.rejectButton.setText("Delete Request");
                    holder.rejectButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cancelChatRequest(userId);
                        }
                    });
                    
                    if (!requestImage.isEmpty()) {
                        Picasso.get().load(requestImage)
                                .placeholder(R.drawable.user_icon_default)
                                .into(holder.profilePicture);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }
    
    private void acceptChatRequest(final String userId) {
        contactRef.child(currentUserId).child(userId).child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            contactRef.child(userId).child(currentUserId).child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                chatRequestRef.child(currentUserId).child(userId).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    chatRequestRef.child(userId).child(currentUserId).removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        Toast.makeText(getContext(), "New Contact Saved", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    
    private void cancelChatRequest(final String userId) {
        chatRequestRef.child(currentUserId).child(userId).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            chatRequestRef.child(userId).child(currentUserId).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getContext(), "Request Deleted", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
    
    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView username, userStatus;
        CircleImageView profilePicture;
        Button acceptButton, rejectButton;
        ImageView imageView;
        
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            
            username = itemView.findViewById(R.id.users_profile_name);
            userStatus = itemView.findViewById(R.id.users_status);
            profilePicture = itemView.findViewById(R.id.users_profile_image);
            acceptButton = itemView.findViewById(R.id.request_accept_button);
            rejectButton = itemView.findViewById(R.id.request_cancel_button);
            imageView = itemView.findViewById(R.id.users_online_status);
        }
    }
}
