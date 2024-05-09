package minesweeperjs;

public class Player {
    private String name;
    private int score;
    private boolean isCurrentTurn; // To track if it's the player's turn.
    private boolean isReady = false; // To track if the player is ready.
    private int playerNumber; // Unique identifier for the player

    public Player(String name) {
        this.name = name;
        this.score = 0; // Initial score is set to 0.
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public boolean isReady() {
        return this.isReady;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void incrementScore(int points) {
        this.score += points;
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
}
