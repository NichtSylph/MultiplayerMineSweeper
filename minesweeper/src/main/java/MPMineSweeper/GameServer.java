package main.java.MPMineSweeper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private int port;
    private String password;
    private List<ClientHandler> clientHandlers;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private boolean gameStarted;
    private GameBoard gameBoard;
    private AtomicInteger currentPlayerIndex;
    private List<Player> players;
    private AtomicInteger readyPlayers;
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final int MINES = 32;
    private static final int MAX_PLAYERS = 4;

    public GameServer(int port, String password) {
        this.port = port;
        this.password = password;
        clientHandlers = new ArrayList<>();
        players = new ArrayList<>();
        gameBoard = new GameBoard(WIDTH, HEIGHT, MINES, players.toArray(new Player[0]));
        currentPlayerIndex = new AtomicInteger(0);
        readyPlayers = new AtomicInteger(0);
        gameStarted = false;
        isRunning = true;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port + ". Waiting for clients...");
    
            new Thread(() -> {
                while (isRunning && players.size() < MAX_PLAYERS) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        if (players.size() < MAX_PLAYERS) {
                            Player player = new Player(); // Create a new Player object
                            player.setPlayerNumber(players.size() + 1); // Set the player number
                            player.setPassword(password); // Set the password
                            ClientHandler clientHandler = new ClientHandler(clientSocket, this, player);
                            new Thread(clientHandler).start();
                        } else {
                            System.out.println("Maximum player limit reached. Rejecting additional clients.");
                        }
                    } catch (IOException e) {
                        System.out.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }
    public void stopServer() {
        isRunning = false;
        clientHandlers.forEach(ClientHandler::closeConnection);
        closeServerSocket();
        System.exit(0);
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }

    public synchronized void addPlayer(Socket clientSocket, Player newPlayer) {
        if (newPlayer.getPassword().equals(password) && players.size() < MAX_PLAYERS) {
            ClientHandler clientHandler = new ClientHandler(clientSocket, this, newPlayer);
            clientHandlers.add(clientHandler);
            players.add(newPlayer);
            clientHandler.sendMessage("PLAYER_NUMBER " + newPlayer.getPlayerNumber());
            broadcastPlayerCount();
        } else {
            System.out.println("Incorrect password attempt or max players reached. Connection denied.");
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Failed to close socket after denied connection.");
            }
        }
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    private void broadcastPlayerCount() {
        broadcastMessage("PLAYERS_CONNECTED " + players.size());
    }

    public synchronized void playerReady(Player player) {
        if (!gameStarted && readyPlayers.incrementAndGet() == players.size()) {
            startGame();
        } else if (!gameStarted && players.size() > 1) {
            sendToPlayer(player, "WAITING_FOR_PLAYERS");
        }
    }

    private void startGame() {
        if (!gameStarted) {
            gameStarted = true;
            broadcastMessage("GAME_STARTED");
            gameBoard.startGame();
            currentPlayerIndex.set(0);
            broadcastTurnChange();
        }
    }

    private void broadcastTurnChange() {
        broadcastMessage("TURN_CHANGED " + currentPlayerIndex.get());
    }

    private void broadcastMessage(String message) {
        clientHandlers.forEach(handler -> handler.sendMessage(message));
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
    
        if (gameBoard.revealCell(x, y, player)) {
            checkGameOver();
        }
       
        sendToPlayer(player, "MOVE_RESULT " + x + " " + y + " " + (gameBoard.getCell(x, y).isRevealed() ? "1" : "0"));
        broadcastUpdatedGameState();
    }   

    private void checkGameOver() {
        if (gameBoard.getBombRevealedCount() >= MINES) {
            broadcastMessage("GAMEOVER");
            stopServer();
        } else {
            switchTurns();
        }
    }

    private void switchTurns() {
        currentPlayerIndex.incrementAndGet();
        currentPlayerIndex.set(currentPlayerIndex.get() % players.size());
        broadcastTurnChange();
    }

    private void sendToPlayer(Player player, String message) {
        clientHandlers.stream()
                .filter(h -> h.getPlayer().equals(player))
                .findFirst()
                .ifPresent(h -> h.sendMessage(message));
    }

    private void broadcastUpdatedGameState() {
        String state = createGameStateMessage();
        broadcastMessage("GAME_STATE_UPDATE " + state);
    }

    private String createGameStateMessage() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Cell cell = gameBoard.getCell(x, y);
                int state = cell.isRevealed() ? (cell.isMine() ? 2 : 1) : (cell.isFlagged() ? 3 : 0);
                sb.append(x).append(",").append(y).append(",").append(state).append(";");
            }
        }
        return sb.toString();
    }

    public void handlePlayerQuit(Player player) {
        players.remove(player);
        clientHandlers.removeIf(handler -> handler.getPlayer().equals(player));
        broadcastPlayerCount();
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public void toggleFlag(int x, int y, boolean isFlagged, Player player) {
        if (gameBoard.toggleFlag(x, y, isFlagged, player)) {
            checkGameOver();
        }
        broadcastUpdatedGameState();
    }

    public boolean isPlayerTurn(Player player) {
        return player.equals(players.get(currentPlayerIndex.get()));
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java GameServer <port> <password>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String password = args[1];

        GameServer server = new GameServer(port, password);
        server.startServer();
    }
}