package com.bg4u.coins4u;

public class TicTacToeModel {
    private int win;
    private int lost;
    private int draw;
    
    public TicTacToeModel() {
        // Default constructor
    }
    
    public TicTacToeModel(int win, int lost, int draw) {
        this.win = win;
        this.lost = lost;
        this.draw = draw;
    }
    
    public int getWin() {
        return win;
    }
    
    public void setWin(int win) {
        this.win = win;
    }
    
    public int getLost() {
        return lost;
    }
    
    public void setLost(int lost) {
        this.lost = lost;
    }
    
    public int getDraw() {
        return draw;
    }
    
    public void setDraw(int draw) {
        this.draw = draw;
    }
}
