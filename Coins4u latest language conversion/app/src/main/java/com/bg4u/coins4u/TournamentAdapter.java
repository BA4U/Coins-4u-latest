package com.bg4u.coins4u;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.TournamentViewHolder> {

    private final Context context;
    private final List<TournamentModel> tournamentList;

    public TournamentAdapter(Context context, List<TournamentModel> tournamentList) {
        this.context = context;
        this.tournamentList = filterActiveTournaments(tournamentList);
    }

    private List<TournamentModel> filterActiveTournaments(List<TournamentModel> tournamentList) {
        List<TournamentModel> filteredList = new ArrayList<>();
        for (TournamentModel tournament : tournamentList) {
            if (tournament.isStatus()) {
                filteredList.add(tournament);
            }
        }
        return filteredList;
    }

    @NonNull
    @Override
    public TournamentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tournament_matchmaking, parent, false);
        return new TournamentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TournamentViewHolder holder, int position) {
        TournamentModel tournament = tournamentList.get(position);

        holder.name.setText(tournament.getName());
        holder.description.setText(tournament.getDescription());
        holder.mode.setText(tournament.getMode());
        holder.map.setText(tournament.getMap());
        holder.entryFee.setText(String.format("Entry Fee: %s", tournament.getEntryFee()));
        holder.win.setText(String.valueOf(tournament.getWin()));
        holder.date.setText(tournament.getDate());
        holder.filledSlot.setText(String.valueOf(tournament.getFilledSlot()));
        holder.totalSlot.setText(String.valueOf(tournament.getTotalSlot()));

        Glide.with(context)
                .load(tournament.getPlayerPic())
                .apply(new RequestOptions()
                        .placeholder(R.drawable.hard_bot)
                        .error(R.drawable.easy_bot))
                .into(holder.playerPic);

        String thumbnailUrl = tournament.getThumbnail();
        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            // Hide the ImageView if the thumbnail URL is null or empty
            holder.thumbnail.setVisibility(View.GONE);
        } else {
            // Make the ImageView visible if the thumbnail URL is not null or empty
            holder.thumbnail.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(thumbnailUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.hard_bot)
                            .error(R.drawable.easy_bot))
                    .into(holder.thumbnail);
        }
    }

    @Override
    public int getItemCount() {
        return tournamentList.size();
    }

    public static class TournamentViewHolder extends RecyclerView.ViewHolder {
        TextView name, description, mode, map, entryFee, win, date, filledSlot, totalSlot;
        ImageView playerPic, thumbnail;

        public TournamentViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            playerPic = itemView.findViewById(R.id.playerPic);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            description = itemView.findViewById(R.id.description);
            mode = itemView.findViewById(R.id.mode);
            map = itemView.findViewById(R.id.map);
            entryFee = itemView.findViewById(R.id.entryFeeBtn);
            win = itemView.findViewById(R.id.win);
            date = itemView.findViewById(R.id.date);
            filledSlot = itemView.findViewById(R.id.filledSlot);
            totalSlot = itemView.findViewById(R.id.totalSlot);
        }
    }
}