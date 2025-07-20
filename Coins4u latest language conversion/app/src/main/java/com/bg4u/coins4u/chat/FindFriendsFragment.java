package com.bg4u.coins4u.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsFragment extends Fragment {
    
    public FindFriendsFragment() {
        // Required empty public constructor
    }
    private EditText searchInput;
    private RecyclerView recyclerView;
    private DatabaseReference userRef;
    private FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder> adapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_friends, container, false);
        
        // Find the AdView and load an ad
//        AdView mAdView = view.findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
        
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        
        recyclerView = view.findViewById(R.id.find_friends_recyclerlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        searchInput = view.findViewById(R.id.search_username_input);
        ImageView searchButton = view.findViewById(R.id.search_user_btn);
        
        searchButton.setOnClickListener(v -> {
            String searchTerm = searchInput.getText().toString();
            if (searchTerm.isEmpty() || searchTerm.length() < 3) {
                searchInput.setError("Invalid Username");
                return;
            }
            setupSearchRecyclerView(searchTerm);
        });
        
        setupSearchRecyclerView("");
        
        // Inside your onCreate method
        searchInput.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler();
            
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            
            @Override
            public void afterTextChanged(Editable editable) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> {
                    String searchTerm = editable.toString().trim();
                    setupSearchRecyclerView(searchTerm);
                }, 500);
            }
        });
        
        return view;
    }
    
    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView username, userstatus;
        CircleImageView profile;
        
        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.users_profile_name);
            userstatus = itemView.findViewById(R.id.users_status);
            profile = itemView.findViewById(R.id.users_profile_image);
        }
    }
    
    private void setupSearchRecyclerView(String searchTerm) {
        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(userRef.orderByChild("name")
                        .startAt(searchTerm)
                        .endAt(searchTerm + "\uf8ff"), Contacts.class)
                .build();
        
        adapter = new FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, final int position, @NonNull Contacts model) {
                holder.username.setText(model.getName());
                holder.userstatus.setText(model.getStatus());
                Picasso.get().load(model.getImage()).placeholder(R.drawable.user_icon_default).into(holder.profile);
                
                holder.itemView.setOnClickListener(v -> {
                    String visit_user_id = getRef(position).getKey();
                    Intent profileIntent = new Intent(requireContext(), ProfileActivity.class);
                    profileIntent.putExtra("visit_user_id", visit_user_id);
                    startActivity(profileIntent);
                });
            }
            
            @NonNull
            @Override
            public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                return new FindFriendsViewHolder(view);
            }
        };
        
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Call your data loading method here
        setupSearchRecyclerView("");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}