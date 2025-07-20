package com.bg4u.coins4u.chat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.FindFriendsViewHolder> {
    
    private final Context context;
    private final List<Contacts> contactsList;
    
    public FindFriendsAdapter(Context context, List<Contacts> contactsList) {
        this.context = context;
        this.contactsList = contactsList;
    }
    
    @NonNull
    @Override
    public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
        return new FindFriendsViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FindFriendsViewHolder holder, int position) {
        Contacts contact = contactsList.get(position);
        
        holder.username.setText(contact.getName());
        holder.userStatus.setText(contact.getStatus());
        
        // Assuming you have a method to get the user's profile image URL
        String profileImageUrl = contact.getImage();
        Picasso.get().load(profileImageUrl).placeholder(R.drawable.user_icon_default).into(holder.profileImage);
    
        if (contact.getImage() != null && !contact.getImage().isEmpty()) {
            // User has uploaded a profile image
            Glide.with(context)
                    .load(contact.getImage())
                    .into(holder.profileImage);
        } else {
            // User has not uploaded a profile image, set default image
            Glide.with(context)
                    .load(R.drawable.user_icon_default)
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.profileImage);
        }
        
        holder.itemView.setOnClickListener(v -> {
            String visitUserId = contact.getUserId();
            Intent profileIntent = new Intent(context, ProfileActivity.class);
            profileIntent.putExtra("visit_user_id", visitUserId);
            context.startActivity(profileIntent);
        });
    }
    
    @Override
    public int getItemCount() {
        return contactsList.size();
    }
    
    static class FindFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView username, userStatus;
        CircleImageView profileImage;
        
        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.users_profile_name);
            userStatus = itemView.findViewById(R.id.users_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
        }
    }
}
