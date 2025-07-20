package com.bg4u.coins4u.chat;

import android.annotation.SuppressLint;
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
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {
    
    private String messageReceiverId;
    private String messageSenderId;
    private DatabaseReference rootRef;
    private RecyclerView recyclerView;
    private DatabaseReference chatRef,userRef;
    
    public ChatsFragment() {
        // Required empty public constructor
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chats, container, false);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        chatRef= FirebaseDatabase.getInstance().getReference().child("Contacts").child(userID);
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");
    
        messageSenderId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        recyclerView= view.findViewById(R.id.chats_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatRef, Contacts.class).build();
        FirebaseRecyclerAdapter<Contacts,ChatViewHolder> adapter=new FirebaseRecyclerAdapter<Contacts, ChatViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatViewHolder holder, int position, @NonNull Contacts model) {
                final String userid = getRef(position).getKey();
                userRef.child(Objects.requireNonNull(userid)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            final String name= Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                           // final String status= Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();
                            final String image = dataSnapshot.hasChild("image") ? Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString() : "default_image";
                            holder.username.setText(name);
    
                            if (dataSnapshot.exists() && dataSnapshot.hasChild("userState")) {
                                // Changed the code to check for the child "userState" before accessing its properties
                                String state = Objects.requireNonNull(dataSnapshot.child("userState").child("state").getValue()).toString();
                                String date = Objects.requireNonNull(dataSnapshot.child("userState").child("date").getValue()).toString();
                                String time = Objects.requireNonNull(dataSnapshot.child("userState").child("time").getValue()).toString();
                                
                                holder.lastMessageTime.setText(time);
                                
                                if (state.equals("online")) {
                                    holder.userLastSeen.setText("online now");
                                    // Find the CircleImageView by its ID
    
                                } else if (state.equals("offline")) {
                                    // Do something if the user is offline
                                    holder.userLastSeen.setText("Last seen :" + " " + date + " at " + time);
                                }
                            }
    
                            Picasso.get().load(image).placeholder(R.drawable.user_icon_default).into(holder.profile_image);
                            holder.itemView.setOnClickListener(v -> {
                                Intent chatIntent=new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("visit_user_id",userid);
                                chatIntent.putExtra("visit_user_name",name);
                                chatIntent.putExtra("visit_image", image);
                                startActivity(chatIntent);
                            });
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
            
            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view=LayoutInflater.from(getContext()).inflate(R.layout.users_display_layout,parent,false);
                return new ChatViewHolder(view);
            }
        };
        
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
    
    
//    public void displayLastSeen() {
//        rootRef.child("Users").child(messageReceiverId).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists() && dataSnapshot.hasChild("userState")) {
//                    // Changed the code to check for the child "userState" before accessing its properties
//                    String state = Objects.requireNonNull(dataSnapshot.child("userState").child("state").getValue()).toString();
//                    String date = Objects.requireNonNull(dataSnapshot.child("userState").child("date").getValue()).toString();
//                    String time = Objects.requireNonNull(dataSnapshot.child("userState").child("time").getValue()).toString();
//
//                    if (state.equals("online")) {
//                        holder.userLastSeen.setText("online");
//                    } else if (state.equals("offline")) {
//                        // Do something if the user is offline
//                        holder.userLastSeen.setText("Last seen :" + " " + date + " at " + time);
//                    }
//                } else {
//                    userLastSeen.setText("offline");
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                // Handle any errors or failures in fetching last seen details
//                // ...
//            }
//        });
//    }
    
   
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profile_image;
        TextView username, userLastSeen, lastMessageTime;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profile_image = itemView.findViewById(R.id.users_profile_image);
            username = itemView.findViewById(R.id.users_profile_name);
            userLastSeen = itemView.findViewById(R.id.users_status);
            lastMessageTime = itemView.findViewById(R.id.last_message_time);
        }
    }
}


// chatgpt code 2

// package com.bg4u.coins4u.chat;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bg4u.coins4u.R;
//import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.squareup.picasso.Picasso;
//
//import de.hdodenhof.circleimageview.CircleImageView;
//
//public class ChatsFragment extends Fragment {
//
//    private RecyclerView recyclerView;
//    private DatabaseReference chatRef, userRef;
//    private FirebaseRecyclerAdapter<Contacts, ChatViewHolder> adapter;
//
//    public ChatsFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View view = inflater.inflate(R.layout.fragment_chats, container, false);
//        FirebaseAuth mAuth = FirebaseAuth.getInstance();
//        String userID = mAuth.getCurrentUser().getUid();
//        chatRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(userID);
//        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
//
//        recyclerView = view.findViewById(R.id.chats_list);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        // Optimize RecyclerView performance by setting fixed size to true
//        recyclerView.setHasFixedSize(true);
//
//        // Set up FirebaseRecyclerOptions
//        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
//                .setQuery(chatRef, Contacts.class).build();
//
//        // Set up the FirebaseRecyclerAdapter
//        adapter = new FirebaseRecyclerAdapter<Contacts, ChatViewHolder>(options) {
//            @Override
//            protected void onBindViewHolder(@NonNull final ChatViewHolder holder, int position, @NonNull Contacts model) {
//                final String userid = getRef(position).getKey();
//                if (userid != null) {
//                    // Fetch user data for all contacts in a single query
//                    userRef.child(userid).addValueEventListener(new ValueEventListener() {
//                        @SuppressLint("SetTextI18n")
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            if (dataSnapshot.exists()) {
//                                final String name = dataSnapshot.child("name").getValue(String.class);
//                                final String status = dataSnapshot.child("status").getValue(String.class);
//                                final String image = dataSnapshot.hasChild("image") ? dataSnapshot.child("image").getValue(String.class) : "default_image";
//
//                                if (name != null) {
//                                    holder.username.setText(name);
//                                }
//
//                                holder.lastMessage.setText("Last seen:\nDate Time");
//
//                                if (dataSnapshot.child("userState").hasChild("state")) {
//                                    String state = dataSnapshot.child("userState").child("state").getValue(String.class);
//                                    String date = dataSnapshot.child("userState").child("date").getValue(String.class);
//                                    String time = dataSnapshot.child("userState").child("time").getValue(String.class);
//
//                                    if ("online".equals(state)) {
//                                        holder.lastMessage.setText("online");
//                                    } else if ("offline".equals(state) && date != null && time != null) {
//                                        holder.lastMessage.setText("Last seen:\n" + date + "  " + time);
//                                    }
//                                } else {
//                                    holder.lastMessage.setText("offline");
//                                }
//
//                                Picasso.get().load(image).placeholder(R.drawable.profile_image).into(holder.profile_image);
//                                holder.itemView.setOnClickListener(v -> {
//                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
//                                    chatIntent.putExtra("visit_user_id", userid);
//                                    chatIntent.putExtra("visit_user_name", name);
//                                    chatIntent.putExtra("visit_image", image);
//                                    startActivity(chatIntent);
//                                });
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//                        }
//                    });
//                }
//            }
//
//            @NonNull
//            @Override
//            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//                View view = LayoutInflater.from(getContext()).inflate(R.layout.users_display_layout, parent, false);
//                return new ChatViewHolder(view);
//            }
//        };
//
//        recyclerView.setAdapter(adapter);
//        return view;
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        adapter.startListening();
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        adapter.stopListening();
//    }
//
//    public static class ChatViewHolder extends RecyclerView.ViewHolder {
//        CircleImageView profile_image;
//        TextView username, lastMessage;
//
//        public ChatViewHolder(@NonNull View itemView) {
//            super(itemView);
//            profile_image = itemView.findViewById(R.id.users_profile_image);
//            username = itemView.findViewById(R.id.users_profile_name);
//            lastMessage = itemView.findViewById(R.id.users_status);
//        }
//    }
//}


//Original code



//package com.bg4u.coins4u.chat;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bg4u.coins4u.R;
//import com.bg4u.coins4u.chat.ChatActivity;
//import com.bg4u.coins4u.chat.Contacts;
//import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.squareup.picasso.Picasso;
//
//import de.hdodenhof.circleimageview.CircleImageView;
//
//public class ChatsFragment extends Fragment {
//
//    private RecyclerView recyclerView;
//    private DatabaseReference chatRef, userRef;
//    private FirebaseRecyclerAdapter<Contacts, ChatViewHolder> adapter;
//
//    public ChatsFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View view = inflater.inflate(R.layout.fragment_chats, container, false);
//        FirebaseAuth mAuth = FirebaseAuth.getInstance();
//        String userID = mAuth.getCurrentUser().getUid();
//        chatRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(userID);
//        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
//
//        recyclerView = view.findViewById(R.id.chats_list);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        // Optimize RecyclerView performance by setting fixed size to true
//        recyclerView.setHasFixedSize(true);
//
//        // Set up the FirebaseRecyclerOptions and FirebaseRecyclerAdapter
//        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
//                .setQuery(chatRef, Contacts.class).build();
//        adapter = new FirebaseRecyclerAdapter<Contacts, ChatViewHolder>(options) {
//            @Override
//            protected void onBindViewHolder(@NonNull final ChatViewHolder holder, int position, @NonNull Contacts model) {
//                final String userid = getRef(position).getKey();
//                if (userid != null) {
//                    userRef.child(userid).addListenerForSingleValueEvent(new ValueEventListener() {
//                        @SuppressLint("SetTextI18n")
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            if (dataSnapshot.exists()) {
//                                final String name = dataSnapshot.child("name").getValue(String.class);
//                                final String status = dataSnapshot.child("status").getValue(String.class);
//                                final String image = dataSnapshot.hasChild("image") ? dataSnapshot.child("image").getValue(String.class) : "default_image";
//
//                                if (name != null) {
//                                    holder.username.setText(name);
//                                }
//
//                                holder.lastMessage.setText("Last seen:\nDate Time");
//
//                                if (dataSnapshot.child("userState").hasChild("state")) {
//                                    String state = dataSnapshot.child("userState").child("state").getValue(String.class);
//                                    String date = dataSnapshot.child("userState").child("date").getValue(String.class);
//                                    String time = dataSnapshot.child("userState").child("time").getValue(String.class);
//
//                                    if ("online".equals(state)) {
//                                        holder.lastMessage.setText("online");
//                                    } else if ("offline".equals(state) && date != null && time != null) {
//                                        holder.lastMessage.setText("Last seen:\n" + date + "  " + time);
//                                    }
//                                } else {
//                                    holder.lastMessage.setText("offline");
//                                }
//
//                                Picasso.get().load(image).placeholder(R.drawable.profile_image).into(holder.profile_image);
//                                holder.itemView.setOnClickListener(v -> {
//                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
//                                    chatIntent.putExtra("visit_user_id", userid);
//                                    chatIntent.putExtra("visit_user_name", name);
//                                    chatIntent.putExtra("visit_image", image);
//                                    startActivity(chatIntent);
//                                });
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//                        }
//                    });
//                }
//            }
//
//            @NonNull
//            @Override
//            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//                View view = LayoutInflater.from(getContext()).inflate(R.layout.users_display_layout, parent, false);
//                return new ChatViewHolder(view);
//            }
//        };
//
//        recyclerView.setAdapter(adapter);
//        return view;
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        adapter.startListening();
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        adapter.stopListening();
//    }
//
//    public static class ChatViewHolder extends RecyclerView.ViewHolder {
//        CircleImageView profile_image;
//        TextView username, lastMessage;
//
//        public ChatViewHolder(@NonNull View itemView) {
//            super(itemView);
//            profile_image = itemView.findViewById(R.id.users_profile_image);
//            username = itemView.findViewById(R.id.users_profile_name);
//            lastMessage = itemView.findViewById(R.id.users_status);
//        }
//    }
//}