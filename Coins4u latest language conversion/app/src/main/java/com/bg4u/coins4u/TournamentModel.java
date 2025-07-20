package com.bg4u.coins4u;

public class TournamentModel {
    private String tournamentId;
    private String playerPic;
    private String thumbnail;
    private String name = "BG4U User";
    private String description = "Free Fire Tournament";
    private String map = "Bermuda";
    private String mode = "One Tap";
    private boolean status = true;
    private boolean paid = true;
    private int entryFee = 100;
    private int win = 175;
    private int filledSlot = 1;
    private int totalSlot = 1;
    private String date = "Coming soon";

    // Default constructor
    public TournamentModel() {
    }

    // Constructor
    public TournamentModel(String tournamentId, String playerPic, String thumbnail, String name, String description, String mode, String map, boolean status, boolean paid, int entryFee, int win, int filledSlot, int totalSlot, String date) {
        this.tournamentId = tournamentId;
        this.playerPic = playerPic;
        this.thumbnail = thumbnail;
        this.name = name;
        this.description = description;
        this.mode = mode;
        this.map = map;
        this.status = status;
        this.paid = paid;
        this.entryFee = entryFee;
        this.win = win;
        this.filledSlot = filledSlot;
        this.totalSlot = totalSlot;
        this.date = date;
    }

    // Getters
    public String getTournamentId() {
        return tournamentId;
    }

    public String getPlayerPic() {
        return playerPic;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMode() {
        return mode;
    }

    public String getMap() {
        return map;
    }

    public boolean isStatus() {
        return status;
    }

    public boolean isPaid() {
        return paid;
    }

    public int getEntryFee() {
        return entryFee;
    }

    public int getWin() {
        return win;
    }

    public int getFilledSlot() {
        return filledSlot;
    }

    public int getTotalSlot() {
        return totalSlot;
    }

    public String getDate() {
        return date;
    }

    // Setters if needed (optional)
    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    public void setPlayerPic(String playerPic) {
        this.playerPic = playerPic;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void setEntryFee(int entryFee) {
        this.entryFee = entryFee;
    }

    public void setFilledSlot(int filledSlot) {
        this.filledSlot = filledSlot;
    }

    public void setTotalSlot(int totalSlot) {
        this.totalSlot = totalSlot;
    }

    public void setWin(int win) {
        this.win = win;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
