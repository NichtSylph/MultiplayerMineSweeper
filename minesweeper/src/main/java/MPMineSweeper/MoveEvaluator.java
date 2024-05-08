package MPMineSweeper;

public class MoveEvaluator {
    private GameBoard gameBoard;
    private Player[] players;
    private int playerIndex;

    public MoveEvaluator(GameBoard gameBoard, Player[] players) {
        this.gameBoard = gameBoard;
        this.players = players;
        this.playerIndex = 0;
    }

    public MoveResult evaluateMove(int x, int y, Player player) {
        // Ensure the move is being made by the current player and the game is started
        if (!player.equals(players[playerIndex]) || !gameBoard.isGameStarted()) {
            return new MoveResult(false, false, player);
        }

        Cell cell = gameBoard.getCell(x, y);
        if (cell == null || cell.isRevealed()) {
            return new MoveResult(false, false, player);
        }

        // Reveal the cell and check if it's a mine
        cell.setRevealed(true);
        boolean isMine = cell.isMine();
        if (isMine) {
            gameBoard.setGameOver(true);
        } else {
            // If the cell is not a mine, recursively reveal adjacent cells if it has zero
            // neighboring mines
            if (cell.getNeighboringMines() == 0) {
                revealAdjacentCells(x, y);
            }
        }

        return new MoveResult(true, isMine, player);
    }

    private void revealAdjacentCells(int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    int newX = x + dx;
                    int newY = y + dy;
                    if (isValidCell(newX, newY)) {
                        Cell adjacentCell = gameBoard.getCell(newX, newY);
                        if (!adjacentCell.isRevealed() && !adjacentCell.isMine()) {
                            adjacentCell.setRevealed(true);
                            if (adjacentCell.getNeighboringMines() == 0) {
                                revealAdjacentCells(newX, newY);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isValidCell(int x, int y) {
        return x >= 0 && y >= 0 && x < gameBoard.getWidth() && y < gameBoard.getHeight();
    }

    public static class MoveResult {
        private boolean isValid;
        private boolean isMine;
        private Player player;

        public MoveResult(boolean isValid, boolean isMine, Player player) {
            this.isValid = isValid;
            this.isMine = isMine;
            this.player = player;
        }

        public boolean isValid() {
            return isValid;
        }

        public boolean isMine() {
            return isMine;
        }

        public Player getPlayer() {
            return player;
        }

        public int getScore() {
            return player.getScore();
        }

        public void addScore(int score) {
            player.incrementScore(score);
        }
    }
}
