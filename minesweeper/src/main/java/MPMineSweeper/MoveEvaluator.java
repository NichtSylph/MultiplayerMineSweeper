package MPMineSweeper;

public class MoveEvaluator {
    private GameBoard gameBoard;
    private Player[] players;
    private int playerIndex; // Index to keep track of whose turn it is

    public MoveEvaluator(GameBoard gameBoard, Player[] players) {
        this.gameBoard = gameBoard;
        this.players = players;
        this.playerIndex = 0; // Start with the first player
    }

    public MoveResult evaluateMove(int x, int y, Player player) {
        if (!gameBoard.isGameStarted()) {
            return new MoveResult(false, "Game not started", 0, player);
        }
        if (!player.equals(players[playerIndex])) {
            return new MoveResult(false, "Not your turn", 0, player);
        }

        Cell cell = gameBoard.getCell(x, y);
        if (cell == null || cell.isRevealed()) {
            return new MoveResult(false, "Invalid move", 0, player);
        }

        cell.setRevealed(true);
        if (cell.isMine()) {
            gameBoard.setGameOver(true);
            return new MoveResult(true, "Mine hit", 0, player);
        }

        int neighboringMines = cell.getNeighboringMines();
        boolean allCleared = gameBoard.allNonMineCellsRevealed();
        if (allCleared) {
            gameBoard.setGameOver(true);
            return new MoveResult(true, "All cells cleared", neighboringMines, player);
        }

        switchPlayer();
        return new MoveResult(true, "Safe move", neighboringMines, player);
    }

    private void switchPlayer() {
        playerIndex = (playerIndex + 1) % players.length;
    }

    public static class MoveResult {
        private boolean isValid;
        private String message;
        private int neighboringMines;
        private Player player;

        public MoveResult(boolean isValid, String message, int neighboringMines, Player player) {
            this.isValid = isValid;
            this.message = message;
            this.neighboringMines = neighboringMines;
            this.player = player;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getMessage() {
            return message;
        }

        public int getNeighboringMines() {
            return neighboringMines;
        }

        public Player getPlayer() {
            return player;
        }
    }
}
