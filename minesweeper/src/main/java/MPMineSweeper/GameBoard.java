package MPMineSweeper;

import java.util.Random;

public class GameBoard {
    private Cell[][] cells;
    private int width;
    private int height;
    private int mineCount;
    private boolean gameStarted;
    private boolean gameOver;
    private int bombRevealedCount;
    private MoveEvaluator moveEvaluator;

    public GameBoard(int width, int height, int mineCount, Player[] players) {
        this.width = width;
        this.height = height;
        this.mineCount = mineCount;
        this.gameStarted = false;
        this.gameOver = false;
        this.bombRevealedCount = 0;
        this.cells = new Cell[height][width];
        initializeCells();
        placeMines();
        calculateNeighboringMines();
        this.moveEvaluator = new MoveEvaluator(this, players);
    }

    private void initializeCells() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                cells[i][j] = new Cell();
            }
        }
    }

    private void placeMines() {
        Random random = new Random();
        int minesPlaced = 0;
        while (minesPlaced < mineCount) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (!cells[y][x].isMine()) {
                cells[y][x].setMine(true);
                minesPlaced++;
            }
        }
    }

    public boolean toggleFlag(int x, int y, boolean isFlagged) {
        if (x < 0 || x >= width || y < 0 || y >= height || gameOver) {
            return false; // Cell is out of bounds or game is over
        }
    
        Cell cell = cells[y][x];
        if (cell.isRevealed()) {
            return false; // Can't flag a cell that's already revealed
        }
    
        cell.setFlagged(isFlagged);
        
        return true;
    }
    
    private void calculateNeighboringMines() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (!cells[i][j].isMine()) {
                    int mines = countAdjacentMines(j, i);
                    cells[i][j].setNeighboringMines(mines);
                }
            }
        }
    }

    private int countAdjacentMines(int x, int y) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nx = x + j;
                int ny = y + i;
                if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
                    if (cells[ny][nx].isMine()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public boolean revealCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height || gameOver) {
            return false; // Cell is out of bounds or game is over
        }
    
        Cell cell = cells[y][x];
        if (cell.isRevealed()) {
            return false; // Cell is already revealed
        }
    
        cell.setRevealed(true);
        if (cell.isMine()) {
            bombRevealedCount++;
            if (bombRevealedCount >= mineCount) {
                gameOver = true;
            }
            return true;
        }
    
        if (cell.getNeighboringMines() == 0) {
            revealSurroundingCells(x, y);
        }
    
        return false;
    }

    private void revealSurroundingCells(int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0)
                    continue; // Skip the current cell
                int nx = x + j;
                int ny = y + i;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    revealCell(nx, ny);
                }
            }
        }
    }

    public MoveEvaluator.MoveResult evaluateMove(int x, int y, Player player) {
        return moveEvaluator.evaluateMove(x, y, player);
    }

    public void startGame() {
        gameStarted = true;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return cells[y][x];
    }

    public int getBombRevealedCount() {
        return bombRevealedCount;
    }

    public boolean allNonMineCellsRevealed() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Cell cell = cells[i][j];
                if (!cell.isMine() && !cell.isRevealed()) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getNeighboringMinesCount(int x, int y) {
        return countAdjacentMines(x, y);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setCells(Cell[][] cells) {
        this.cells = cells;
    }
}
