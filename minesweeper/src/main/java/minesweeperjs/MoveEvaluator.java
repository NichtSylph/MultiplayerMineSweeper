package minesweeperjs;

import java.util.concurrent.*;

public class MoveEvaluator {
    private GameBoard gameBoard;
    private Player[] players;
    private int playerIndex;

    public MoveEvaluator(GameBoard gameBoard, Player[] players) {
        this.gameBoard = gameBoard;
        this.players = players;
        this.playerIndex = 0;
    }

    public MoveResult evaluateMoveWithDepthAndParallelism(int x, int y, Player player, int depth, int parallelism) {
        if (!gameBoard.isGameStarted() || !player.equals(players[playerIndex])) {
            return new MoveResult(false, false, player);
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            return recursiveEvaluate(x, y, player, depth, executor);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private MoveResult recursiveEvaluate(int x, int y, Player player, int depth, ExecutorService executor) {
        if (depth == 0 || gameBoard.isGameOver()) {
            return new MoveResult(true, false, player);
        }

        Cell cell = gameBoard.getCell(x, y);
        if (cell == null || cell.isRevealed() || cell.isMine()) {
            return new MoveResult(false, false, player);
        }

        cell.setRevealed(true);
        if (cell.getNeighboringMines() == 0) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        int newX = x + dx;
                        int newY = y + dy;
                        executor.submit(() -> recursiveEvaluate(newX, newY, player, depth - 1, executor));
                    }
                }
            }
        }

        // If we've reached here, it means the move was valid and the cell was not a
        // mine.
        return new MoveResult(true, false, player);
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
