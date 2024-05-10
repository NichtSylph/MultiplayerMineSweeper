package MPMineSweeper;

public class Player {
    private String name;
    private int score;
    private boolean isCurrentTurn; // To track if it's the player's turn.
    private boolean isReady = false; // To track if the player is ready.

    /**
     * Constructor for Player.
     *
     * @param name The name of the player.
     */
    public Player(String name) {
        this.name = name;
        this.score = 0; // Initial score is set to 0.
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
     * Gets the name of the player.
     *
     * @return The name of the player.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the player.
     *
     * @param name The new name of the player.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the current score of the player.
     *
     * @return The current score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets the score of the player.
     *
     * @param score The new score.
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Increments the player's score.
     *
     * @param points The points to add to the current score.
     */
    public void incrementScore(int points) {
        this.score += points;
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
