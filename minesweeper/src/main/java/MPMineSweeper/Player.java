package MPMineSweeper;

public class Player {
    private boolean isCurrentTurn; // To track if it's the player's turn.
    private boolean isReady = false; // To track if the player is ready.
    private int playerNumber; // Unique identifier for the player
    private String password; // Password for the player

    public Player() {
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public boolean isReady() {
        return this.isReady;
    }

    public boolean isCurrentTurn() {
        return isCurrentTurn;
    }

    public void setCurrentTurn(boolean isCurrentTurn) {
        this.isCurrentTurn = isCurrentTurn;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}