package MPMineSweeper;

public class MoveEvaluator {
    private GameBoard gameBoard;
    private Player[] players;
    private int playerIndex; // Index to keep track of whose turn it is

    /**
     * Constructor for MoveEvaluator.
     *
     * @param gameBoard The game board.
     * @param players The array of players.
     */
    public MoveEvaluator(GameBoard gameBoard, Player[] players) {
        this.gameBoard = gameBoard;
        this.players = players;
        this.playerIndex = 0; // Start with the first player
    }

    /**
     * Evaluates a move made by a player at the given coordinates.
     *
     * @param x The x-coordinate of the move.
     * @param y The y-coordinate of the move.
     * @param player The player making the move.
     * @return The result of the move.
     */
    public MoveResult evaluateMove(int x, int y, Player player) {
        // Check if the game has started and if it's the player's turn
        if (!gameBoard.isGameStarted() || !player.equals(players[playerIndex])) {
            return new MoveResult(false, false, 0, player); // Game not started or not the player's turn
        }

        Cell cell = gameBoard.getCell(x, y);

        if (cell == null || cell.isRevealed()) {
            return new MoveResult(false, false, 0, player); // Invalid move
        }

        cell.setRevealed(true);
        if (cell.isMine()) {
            // Game over scenario
            gameBoard.setGameOver(true); // Set the game over state
            return new MoveResult(true, true, 0, player); // Hit a mine
        }

        int neighboringMines = cell.getNeighboringMines();
        // Update the score based on your scoring rules.
        player.incrementScore(neighboringMines); // Increment score based on neighboring mines

        switchPlayer(); // Switch to the next player for the next turn

        return new MoveResult(true, false, neighboringMines, player); // Safe move
    }

    /**
     * Switches the turn to the next player.
     */
    private void switchPlayer() {
        playerIndex = (playerIndex + 1) % players.length;
    }

    /**
     * Inner class to represent the result of a move.
     */
    public static class MoveResult {
        private boolean isValid;
        private boolean isMine;
        private int neighboringMines;
        private Player player;

        /**
         * Constructor for MoveResult.
         *
         * @param isValid Indicates if the move is valid.
         * @param isMine Indicates if the move hit a mine.
         * @param neighboringMines The number of neighboring mines.
         * @param player The player who made the move.
         */
        public MoveResult(boolean isValid, boolean isMine, int neighboringMines, Player player) {
            this.isValid = isValid;
            this.isMine = isMine;
            this.neighboringMines = neighboringMines;
            this.player = player;
        }

        public boolean isValid() {
            return isValid;
        }

        public boolean isMine() {
            return isMine;
        }

        public int getNeighboringMines() {
            return neighboringMines;
        }

        public Player getPlayer() {
            return player;
        }
    }
}
