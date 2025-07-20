package com.bg4u.coins4u;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.databinding.ItemPromotionBinding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    private final Context context;
    private final ArrayList<PromotionModel> promotions;

    public PromotionAdapter(Context context, ArrayList<PromotionModel> promotions) {
        this.context = context;
        this.promotions = promotions;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_promotion layout using LayoutInflater
        ItemPromotionBinding binding = ItemPromotionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PromotionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        // Get the promotion at the given position
        PromotionModel promotion = promotions.get(position);

        // Use Glide to load the image into the ImageView
        Glide.with(context)
                .load(promotion.getPromotionImage())
                .into(holder.binding.promotionImage);

        // Set the text of the promotion button
        String buttonText = promotion.getPromotionButtonText();
        if (buttonText != null && !buttonText.isEmpty()) {
            // Button text is not null or empty
            holder.binding.promotionButton.setText(buttonText);
            holder.binding.promotionButton.setVisibility(View.VISIBLE);
        } else {
            // Button text is null or empty
            holder.binding.promotionButton.setVisibility(View.GONE);
        }
        // Set an OnClickListener for the promotion image and Button
        String url = promotion.getPromotionButtonUrl();
        if (url != null) {
            holder.binding.promotionImage.setOnClickListener(v -> {
                // Open the website in a browser when the image is clicked, only if the URL is not null
                if (url != null) {
                    openWebsite(url);
                }
            });
            holder.binding.promotionButton.setOnClickListener(v -> {
                // Open the website in a browser when the image is clicked, only if the URL is not null
                if (url != null) {
                    openWebsite(url);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    public static class PromotionViewHolder extends RecyclerView.ViewHolder {
        private final ItemPromotionBinding binding;

        public PromotionViewHolder(ItemPromotionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private void openWebsite(String url) {
        // Open the website in a browser using an Intent
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }
}
