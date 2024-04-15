package minesweeperjs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private int port;
    private List<ClientHandler> clientHandlers;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private boolean gameStarted;
    private GameBoard gameBoard;
    private AtomicInteger currentPlayerIndex;
    private List<Player> players;
    private AtomicInteger readyPlayers;
    private AtomicInteger playerCount;
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final int MINES = 32;
    private JFrame frame;
    private JLabel serverStatusLabel;

    public GameServer(int port) {
        this.port = port;
        clientHandlers = new ArrayList<>();
        players = new ArrayList<>();
        gameBoard = new GameBoard(WIDTH, HEIGHT, MINES, players.toArray(new Player[0]));
        currentPlayerIndex = new AtomicInteger(0);
        readyPlayers = new AtomicInteger(0);
        gameStarted = false;
        playerCount = new AtomicInteger(0);
        isRunning = true;
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Minesweeper Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setLayout(new BorderLayout());

        serverStatusLabel = new JLabel("Server not running", SwingConstants.CENTER);
        frame.add(serverStatusLabel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close Server");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });
        frame.add(closeButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            String serverStatusText = "Server running on IP: " + InetAddress.getLocalHost().getHostAddress() + " Port: "
                    + port;
            serverStatusLabel.setText(serverStatusText);

            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String clientAddress = clientSocket.getInetAddress().getHostAddress();
                        System.out.println("Client connected: " + clientAddress);

                        synchronized (this) {
                            Player newPlayer = new Player("Player " + playerCount.incrementAndGet());
                            players.add(newPlayer);

                            ClientHandler clientHandler = new ClientHandler(clientSocket, this, newPlayer);
                            clientHandlers.add(clientHandler);
                            new Thread(clientHandler).start();
                        }

                        broadcastPlayerCount();
                    } catch (IOException e) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            serverStatusLabel.setText("Error: " + e.getMessage());
            System.err.println("Error creating server socket: " + e.getMessage());
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    public void stopServer() {
        // Notify clients that the server is closing before shutting down
        broadcastMessage("SERVER_CLOSING");

        // Close all client connections gracefully
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.notifyServerClosing();
            clientHandler.closeConnection();
        }

        // Stop the server and close the server socket
        isRunning = false;
        closeServerSocket();

        // Give the server a moment to send all messages before exiting
        try {
            Thread.sleep(1000); // Wait for 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.exit(0);
    }

    private void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(message);
        }
    }

    public void broadcastPlayerCount() {
        broadcastMessage("PLAYERS_CONNECTED " + players.size());
    }

    private void broadcastUpdatedGameState() {
        String gameStateMessage = createGameStateMessage();
        broadcastMessage("GAME_STATE_UPDATE " + gameStateMessage);
    }

    public synchronized void playerReady(Player player) {
        if (!gameStarted) {
            player.setReady(true);
            readyPlayers.incrementAndGet();
            if (readyPlayers.get() == players.size()) {
                startGame();
            }
        }
    }

    private void broadcastGameStarted() {
        broadcastMessage("GAME_STARTED");
    }

    public synchronized void startGame() {
        if (!gameStarted && readyPlayers.get() == players.size()) {
            gameStarted = true;
            currentPlayerIndex.set(0); // Starting with the first player
            gameBoard.reset();
            broadcastGameStarted();
            broadcastTurnChange(); // Ensure clients are ready before this is called
        }
    }

    private void broadcastTurnChange() {
        String currentPlayerName = players.get(currentPlayerIndex.get()).getName();
        broadcastMessage("TURN_CHANGED " + currentPlayerName);
    }

    public synchronized void processPlayerMove(Player player, int x, int y) {
        if (!gameStarted) {
            sendToPlayer(player, "GAME_NOT_STARTED");
            return;
        }
    
        if (!isPlayerTurn(player)) {
            sendToPlayer(player, "NOT_YOUR_TURN");
            return;
        }
    
        // Now process the move since it is the correct player's turn
        boolean mineHit = gameBoard.revealCell(x, y, player);
    
        // Handle if a mine is hit
        if (mineHit) {
            player.incrementScore(-1); // Assuming you want to decrement score on mine hit
            broadcastScoreUpdate(player);
            broadcastMessage("PLAYER_HIT_MINE " + player.getName());
            
            if (gameBoard.getBombRevealedCount() >= MINES) {
                endGame();
            } else {
                switchTurns();
            }
        } else {
            // Handle if a mine is not hit
            Cell cell = gameBoard.getCell(x, y);
            if (cell != null && !cell.isMine()) {
                int scoreIncrement = cell.getNeighboringMines();
                if (scoreIncrement > 0) {
                    player.incrementScore(scoreIncrement);
                    broadcastScoreUpdate(player); // Broadcast score update
                }
            }
            if (gameBoard.allNonMineCellsRevealed()) {
                broadcastMessage("GAME_WON");
                endGame();
            } else {
                switchTurns();
            }
        }
        broadcastUpdatedGameState();
    }

    private void sendToPlayer(Player player, String message) {
        clientHandlers.stream()
                .filter(handler -> handler.getPlayer().equals(player))
                .findFirst()
                .ifPresent(handler -> handler.sendMessage(message));
    }

    private void endGame() {
        gameStarted = false;
        broadcastMessage("GAMEOVER");
        updateAndBroadcastGameState();

        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.closeConnection();
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting to stop server: " + e.getMessage());
        }

        stopServer();
    }

    public void sendCellState(ClientHandler clientHandler, int x, int y) {
        Cell cell = gameBoard.getCell(x, y);
        if (cell != null) {
            int cellState = cell.isRevealed() ? (cell.isMine() ? 2 : 1) : 0;
            int neighboringMines = cell.isRevealed() ? cell.getNeighboringMines() : -1;
            clientHandler.sendMessage("CELL_STATE " + x + " " + y + " " + cellState + " " + neighboringMines);
        }
    }

    public void requestCellState(ClientHandler clientHandler, int x, int y) {
        int neighboringMines = gameBoard.getNeighboringMinesCount(x, y);
        // You can now use this count to send back a response to the client
        clientHandler.sendMessage("CELL_STATE " + x + " " + y + " " + neighboringMines);
    }

    private void updateAndBroadcastGameState() {
        String gameStateMessage = createGameStateMessage();
        broadcastMessage("UPDATE " + gameStateMessage);
    }

    private void broadcastScoreUpdate(Player player) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getPlayer().equals(player)) {
                clientHandler.sendMessage("SCORE_UPDATE " + player.getScore());
            }
        }
    }

    public synchronized void toggleFlag(int x, int y, boolean isFlagged, Player player) {
        gameBoard.toggleFlag(x, y, isFlagged);
        updateAndBroadcastGameState();
    }

    private String createGameStateMessage() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < gameBoard.getHeight(); y++) {
            for (int x = 0; x < gameBoard.getWidth(); x++) {
                Cell cell = gameBoard.getCell(x, y);
                int cellState = cell.isRevealed() ? (cell.isMine() ? 2 : 1) : (cell.isFlagged() ? 3 : 0);
                sb.append(x).append(",").append(y).append(",").append(cellState).append(";");
            }
        }
        return sb.toString();
    }

    private void switchTurns() {
        currentPlayerIndex.set((currentPlayerIndex.get() + 1) % players.size());
        Player nextPlayer = players.get(currentPlayerIndex.get());
        broadcastMessage("TURN_CHANGED " + nextPlayer.getName());
    }

    public synchronized void handlePlayerQuit(Player player) {
        players.remove(player);
        clientHandlers.removeIf(handler -> handler.getPlayer().equals(player));
        broadcastMessage("PLAYER_QUIT " + player.getName());
        broadcastPlayerCount();
        if (players.isEmpty()) {
            broadcastMessage("SERVER_CLOSING");
            stopServer();
        }
    }

    public void handleNeighboringMinesCountRequest(ClientHandler clientHandler, int x, int y) {
        int count = gameBoard.getNeighboringMinesCount(x, y);
        clientHandler.sendMessage("NEIGHBORING_MINES_COUNT_RESPONSE " + x + " " + y + " " + count);
    }

    public synchronized void removeClientHandler(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    public boolean isGameRunning() {
        return isRunning;
    }

    public boolean isPlayerTurn(Player player) {
        return gameStarted && players.get(currentPlayerIndex.get()).equals(player);
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameServer server = new GameServer(2805);
                server.startServer();
            }
        });
    }
}
