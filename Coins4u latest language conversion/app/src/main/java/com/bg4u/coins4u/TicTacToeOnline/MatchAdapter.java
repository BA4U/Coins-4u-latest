package com.bg4u.coins4u.TicTacToeOnline;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.ViewHolder> {
    
    private List<MatchModel> matchList;
    private Context context;
    
    public MatchAdapter(Context context, List<MatchModel> matchList) {
        this.context = context;
        this.matchList = matchList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_matchmaking, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchModel match = matchList.get(position);
        
        // Set data to your views here
        holder.userNameTextView.setText(match.getUserName());
        holder.entryFee.setText(String.valueOf(match.getCoins()));
        
        if (match.getUserProfilePic() != null) {
            // Load and display the profile picture using the URL
            Picasso.get().load(match.getUserProfilePic()).into(holder.userProfilePic);
        } else {
            // Handle the case where profilePicUrl is null, load default pic
            Picasso.get().load(R.drawable.easy_bot).into(holder.userProfilePic);
        }
    
        // Add an OnClickListener to the "Join" button
        holder.joinButton.setOnClickListener(view -> {
        
            // Update the session record to indicate that the second player has joined
            DatabaseReference sessionRef = FirebaseDatabase.getInstance().getReference("sessions").child(match.getCode());
            String currentUserUid = match.getCurrentUserUid();
    
            Log.d("MatchAdapter", "Setting p2 to: " + currentUserUid);
    
            sessionRef.child("p2").setValue(currentUserUid).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Handle the "Join" button click here
                    // Start the GameActivity and pass necessary data
                    Intent intent = new Intent(context, GameActivity.class);
                    intent.putExtra("session_code", match.getCode());
                    intent.putExtra("my_player", "O");
                    intent.putExtra("friendUid", match.getFriendUid()); // Pass the friend user's UID
                    intent.putExtra("currentUserUid", match.getCurrentUserUid()); // Pass the current user's UID
                    context.startActivity(intent);
                } else {
                    // Handle the error case
                    Toast.makeText(context, "Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    
    }
    
    @Override
    public int getItemCount() {
        Log.d("Adapter", "Item count: " + matchList.size());
        return matchList.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userProfilePic;
        TextView userNameTextView;
        TextView entryFee;
        Button joinButton;
        
        public ViewHolder(View itemView) {
            super(itemView);
            userProfilePic = itemView.findViewById(R.id.userProfilePic);
            userNameTextView = itemView.findViewById(R.id.userName);
            entryFee = itemView.findViewById(R.id.coins);
            joinButton = itemView.findViewById(R.id.joinbtn);
        }
    }
}