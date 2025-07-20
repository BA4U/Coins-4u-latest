package com.bg4u.coins4u;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.RewardViewHolder> {

    private Context context;
    private ArrayList<RewardModel> rewardModels;
    private OnRewardClickListener listener;
    private int userCoins;
    private int taskCompleted;

    public ShopAdapter(Context context, ArrayList<RewardModel> rewardModels, OnRewardClickListener listener, int userCoins, int taskCompleted) {
        this.context = context;
        this.rewardModels = rewardModels;
        this.listener = listener;
        this.userCoins = userCoins;
        this.taskCompleted = taskCompleted;
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        RewardModel model = rewardModels.get(position);

        holder.button.setText(model.getRewardText());

        Glide.with(context)
                .load(model.getRewardImage())
                .into(holder.imageView);

        // Corrected null pointer issue by initializing lostCoinTextView
        holder.lostCoinTextView.setText(String.valueOf(model.getRewardLostCoin()));

       // Check if the user has enough coins to redeem the reward
        if (userCoins >= model.getRewardLostCoin()) {
            // User has enough coins, so change its drawable
            holder.button.setBackground(ContextCompat.getDrawable(context, R.drawable.button_2));
            Toast.makeText(context, "green color button.", Toast.LENGTH_SHORT).show();
        } else {
            // User does not have enough coins, so set the button to be dim
            holder.button.setBackground(ContextCompat.getDrawable(context, R.drawable.button_4));
            holder.button.setAlpha(0.5f);
        }

        // Set click listener for the rewardAmount button
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSnackbar(v, "Earn" + " " + model.getRewardLostCoin() + " coins and complete" + " " + (model.getTotalTask() - taskCompleted) + " task");
            }
        });

        // Set click listener for the rewardAmount button
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRewardButtonClick(model);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return rewardModels.size();
    }

    public class RewardViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        Button button;
        TextView lostCoinTextView;

        public RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.rewardImage);
            button = itemView.findViewById(R.id.rewardAmount);
            // Initialize lostCoinTextView
            lostCoinTextView = itemView.findViewById(R.id.rewardLostCoin);
        }

    }

    public interface OnRewardClickListener {
        void onRewardButtonClick(RewardModel rewardModel);
    }

    private void showSnackbar(View view, String message){
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_light));
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.BLACK);
        snackbar.show();
    }

}
