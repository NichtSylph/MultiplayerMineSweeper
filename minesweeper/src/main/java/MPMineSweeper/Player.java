package MPMineSweeper;

public class Player {
    private int playerNumber;
    private boolean isCurrentTurn; // To track if it's the player's turn.
    private boolean isReady = false; // To track if the player is ready.

    /**
     * Constructor for Player.
     *
     * @param playerNumber The Number of the player.
     */
    public Player(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    /**
     * Sets the player's readiness for the game.
     *
     * @param ready The readiness status.
     */
    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    /**
     * Checks if the player is ready.
     *
     * @return True if the player is ready, false otherwise.
     */
    public boolean isReady() {
        return this.isReady;
    }

    /**
     * Gets the number of the player.
     *
     * @return The number of the player.
     */
    public int getPlayerNumber() {
        return playerNumber;
    }

    /**
     * Sets the number of the player.
     *
     * @param playerNumber The new number of the player.
     */
    public void setCurrentPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    /**
     * Checks if it's currently this player's turn.
     *
     * @return True if it's the player's turn, false otherwise.
     */
    public boolean isCurrentTurn() {
        return isCurrentTurn;
    }

    /**
     * Sets the turn status for this player.
     *
     * @param isCurrentTurn True if it's the player's turn, false otherwise.
     */
    public void setCurrentTurn(boolean isCurrentTurn) {
        this.isCurrentTurn = isCurrentTurn;
    }
}
