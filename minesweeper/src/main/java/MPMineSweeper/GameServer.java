package MPMineSweeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import MPMineSweeper.MoveEvaluator.MoveResult;

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
    private static final int MIN_REQUIRED_PLAYERS = 1;

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

    public Boolean getGameStarted() {
        return gameStarted;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port + ". Waiting for clients...");

            new Thread(() -> {
                while (isRunning && players.size() < MAX_PLAYERS) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleNewConnection(clientSocket);
                    } catch (IOException e) {
                        System.out.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }

    private void handleNewConnection(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String clientPassword = in.readLine(); // Receive password from client
        if (players.size() < MAX_PLAYERS && this.password.equals(clientPassword)) {
            Player player = new Player(); // Create a new Player object
            player.setPlayerNumber(players.size() + 1); // Set the player number
            player.setPassword(clientPassword); // Set the password

            ClientHandler clientHandler = new ClientHandler(clientSocket, this, player);
            new Thread(clientHandler).start();
            clientHandlers.add(clientHandler);
            players.add(player);

            out.println("Password correct"); // Send response to client
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            broadcastPlayerCount();
        } else {
            out.println("Password incorrect"); // Send response to client
            clientSocket.close();
            System.out.println("Incorrect password attempt or max players reached. Connection denied.");
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

    public synchronized void addPlayer(Socket clientSocket, Player newPlayer, String clientPassword) {
        if (newPlayer.getPassword().equals(password) && players.size() < MAX_PLAYERS) {
            ClientHandler clientHandler = new ClientHandler(clientSocket, this, newPlayer);
            clientHandlers.add(clientHandler);
            players.add(newPlayer);
            new Thread(clientHandler).start();
            clientHandler.sendMessage("Password correct"); // Let ClientHandler manage the messaging
            broadcastPlayerCount(); // Broadcast player count when a player is added
        } else {
            // Since the ClientHandler has not been created yet, we need to send a message
            // directly.
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                out.println("Password incorrect or max players reached");
            } catch (IOException e) {
                System.err.println("Failed to send error message to client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Failed to close socket after denied connection: " + e.getMessage());
                }
            }
        }
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public void broadcastPlayerCount() {
        String message = "PLAYERS_CONNECTED " + players.size();
        broadcastMessage(message);
    }

    public synchronized void playerReady(Player player) {
        if (!gameStarted) {
            readyPlayers.incrementAndGet();
            System.out.println("Player " + player.getPlayerNumber() + " is ready. Total ready: " + readyPlayers.get());

            // Check if all players are ready. Adjust according to your game's minimum
            // player requirement.
            if (readyPlayers.get() >= players.size() && players.size() >= MIN_REQUIRED_PLAYERS) { // Assuming
                                                                                                  // MIN_REQUIRED_PLAYERS
                                                                                                  // is defined
                System.out.println("All players are ready. Starting game...");
                startGame();
            } else {
                System.out.println("Waiting for more players to be ready...");
                sendToPlayer(player, "WAITING_FOR_PLAYERS " + readyPlayers.get() + "/" + players.size());
            }
        } else {
            System.out.println("Game already started, notifying player...");
            sendToPlayer(player, "IS_GAME_STARTED true");
        }
    }

    private void startGame() {
        if (!gameStarted && players.size() > 1) { // Ensure there's more than one player
            gameStarted = true;
            System.out.println("Broadcasting GAME_STARTED message");
            broadcastMessage("GAME_STARTED");
            currentPlayerIndex.set(0);
            broadcastTurnChange();
            System.out.println("Game started with " + players.size() + " players.");
        }
    }

    private void broadcastTurnChange() {
        broadcastMessage("TURN_CHANGED " + currentPlayerIndex.get());
    }

    public void broadcastMessage(String message) {
        System.out.println("Broadcasting message: " + message);
        clientHandlers.forEach(handler -> {
            System.out.println("Sending to client: " + handler.getPlayer().getPlayerNumber() + " message: " + message);
            handler.sendMessage(message);
        });
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

        MoveResult result = gameBoard.evaluateMove(x, y, player);
        if (result.isMine()) {
            handleGameOver(player, false);
        }

        sendToPlayer(player, "MOVE_RESULT " + x + " " + y + " " + (result.isValid() ? "1" : "0"));
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

    public void switchTurns() {
        currentPlayerIndex.incrementAndGet();
        currentPlayerIndex.set(currentPlayerIndex.get() % players.size());
        broadcastTurnChange();
    }

    private void sendToPlayer(Player player, String message) {
        System.out.println("Sending to player: " + player.getPlayerNumber() + " message: " + message);
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
        broadcastPlayerCount(); // Broadcast player count when a player quits
    }

    public void handleGameOver(Player player, boolean won) {
        if (won) {
            System.out.println("Player " + player.getPlayerNumber() + " won the game!");
        } else {
            System.out.println("Player " + player.getPlayerNumber() + " lost the game!");
        }
        stopServer();
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public void toggleFlag(int x, int y, boolean isFlagged, Player player) {
        if (gameBoard.toggleFlag(x, y, isFlagged)) {
            checkGameOver();
        }
        broadcastUpdatedGameState();
    }

    public boolean isPlayerTurn(Player player) {
        return player.equals(players.get(currentPlayerIndex.get()));
    }

    public int getCurrentPlayerCount() {
        return players.size();
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