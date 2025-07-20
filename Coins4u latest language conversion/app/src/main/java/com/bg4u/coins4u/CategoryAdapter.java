package com.bg4u.coins4u;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_CATEGORY = 0;
    private static final int VIEW_TYPE_NATIVE_AD = 1;
    
    private Context context;
    private ArrayList<CategoryModel> categoryModels;
    private ArrayList<NativeAd> nativeAds;
    
    public CategoryAdapter(Context context, ArrayList<CategoryModel> categoryModels) {
        this.context = context;
        this.categoryModels = categoryModels;
        this.nativeAds = new ArrayList<>();
    }
    
    public void addNativeAd(NativeAd nativeAd) {
        nativeAds.add(nativeAd);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        if (viewType == VIEW_TYPE_CATEGORY) {
            View view = inflater.inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_native_ad, parent, false);
            return new NativeAdViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        
        if (viewType == VIEW_TYPE_CATEGORY) {
            CategoryViewHolder categoryViewHolder = (CategoryViewHolder) holder;
            CategoryModel model = categoryModels.get(position);
            
            categoryViewHolder.textView.setText(model.getCategoryName());
            Glide.with(context)
                    .load(model.getCategoryImage())
                    .into(categoryViewHolder.imageView);
            
            // Show the categoryCoin on the category item xml
            // Corrected: Convert the categoryCoin to a String and set it to the coinTextView
            categoryViewHolder.winCoinTextView.setText(String.valueOf(model.getCategoryCoin()));
            categoryViewHolder.lostCoinTextView.setText(String.valueOf(model.getCategoryLostCoin()));
            
            
            categoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        if (model.getCategoryPromotionLink() != null ) {
                            showConfirmationDialog(context, model);
                        }else {
                            openQuizActivity(model);
                        }
                }
            });
        } else {
            NativeAdViewHolder nativeAdViewHolder = (NativeAdViewHolder) holder;
            NativeAd nativeAd = nativeAds.get(position - categoryModels.size());
            nativeAdViewHolder.populateNativeAdView(nativeAd);
        }
    }

    private void showConfirmationDialog(Context context, CategoryModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Learn Before Earn")
                .setIcon(R.drawable.coinslogolowsize)
                .setMessage("If you want to know about this quiz. Click on learn more button.")
                .setPositiveButton("Learn More", (dialog, which) -> openPromotionLink(context, model.getCategoryPromotionLink()))
                .setNegativeButton("Go to Quiz", (dialog, which) -> openQuizActivity(model))
                .setCancelable(true);

        AlertDialog dialog = builder.create();
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_background, null);
        if (drawable != null) {
            dialog.getWindow().setBackgroundDrawable(drawable);
        }

        if (context instanceof MainActivity) {
            ((MainActivity) context).runOnUiThread(dialog::show);
        }
    }

    private void openPromotionLink(Context context, String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        context.startActivity(intent);
    }
    
    private void openQuizActivity(CategoryModel model) {
        ClickSoundHelper.playClickSound();
        Intent intent = new Intent(context, QuizActivity.class);
        intent.putExtra("catId", model.getCategoryId());
        intent.putExtra("catName", model.getCategoryName());
        intent.putExtra("catCoin", model.getCategoryCoin());
        intent.putExtra("catLostCoin", model.getCategoryLostCoin());
        intent.putExtra("catImage", model.getCategoryImage());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return categoryModels.size() + nativeAds.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        if (position < categoryModels.size()) {
            return VIEW_TYPE_CATEGORY;
        } else {
            return VIEW_TYPE_NATIVE_AD;
        }
    }
    
    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;
        TextView winCoinTextView; // TextView to show the categoryCoin
        TextView lostCoinTextView; // TextView to show the lost categoryCoin
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.categoryImage);
            textView = itemView.findViewById(R.id.categoryText);
            winCoinTextView = itemView.findViewById(R.id.categoryCoin); // Initialize the coinTextView
            lostCoinTextView = itemView.findViewById(R.id.categoryLostCoin); // Initialize the coinTextView
        }
    }
    
    public class NativeAdViewHolder extends RecyclerView.ViewHolder {
        private NativeAdView nativeAdView;
        
        public NativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            nativeAdView = itemView.findViewById(R.id.nativeAdView);
        }
        
        public void populateNativeAdView(NativeAd nativeAd) {
            nativeAdView.setNativeAd(nativeAd);
        }
    }

    private class Builder {
        public Builder(Context context) {
        }
    }
}
