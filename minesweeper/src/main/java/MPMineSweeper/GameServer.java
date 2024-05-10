package MPMineSweeper;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.io.BufferedReader;
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
    private AtomicInteger playerCount;
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final int MINES = 40;
    private static final int MAX_PLAYERS = 4;

    public GameServer(int port, String password) {
        this.port = port;
        this.password = password;
        clientHandlers = new ArrayList<>();
        gameBoard = new GameBoard(WIDTH, HEIGHT, MINES, this);
        players = new ArrayList<>();
        currentPlayerIndex = new AtomicInteger(0);
        readyPlayers = new AtomicInteger(0);
        gameStarted = false;
        playerCount = new AtomicInteger(0);
        isRunning = true;
    }

    public void updatePlayerScore(Integer score, Player player) {
        for (Player p : this.players) {
            if (p.getPlayerNumber() == player.getPlayerNumber()) {
                p.setScore(score);
                System.out.println("KKM: score: " + p.getScore());
            }
        }
        for (ClientHandler ch : this.clientHandlers) {
            if (ch.getPlayer().getPlayerNumber() == player.getPlayerNumber()) {
                ch.updatePlayerScore(score);
            }
        }
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server running on port " + port);

            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        this.handleNewConnection(clientSocket);

                        // String clientAddress = clientSocket.getInetAddress().getHostAddress();
                        // System.out.println("Client connected: " + clientAddress);

                        // synchronized (this) {
                        //     Player newPlayer = new Player(playerCount.incrementAndGet());
                        //     players.add(newPlayer);
                        //     System.out.println("New player added. Total players: " + playerCount.get());

                        //     // Create the ClientHandler
                        //     ClientHandler clientHandler = new ClientHandler(clientSocket, this, newPlayer);
                        //     clientHandlers.add(clientHandler);

                        //     // Start ClientHandler thread
                        //     new Thread(clientHandler).start();

                        //     // Send player number immediately after adding the client handler
                        //     clientHandler.sendMessage("PLAYER_NUMBER " + newPlayer.getPlayerNumber());
                        // }

                    } catch (IOException e) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }).start();

        } catch (IOException e) {
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

    private void handleNewConnection(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ClientHandler clientHandler;
        String message = in.readLine();
        String[] parts = message.split(" ");
        String clientPassword = parts[0]; // Receive password from client
        String encryptionKey = parts[1];

        if (this.password.equals(clientPassword)) {
            Player player = new Player(playerCount.incrementAndGet()); // Create a new Player object
            player.setPassword(clientPassword); // Set the password

            clientHandler = new ClientHandler(clientSocket, this, player, encryptionKey);
            new Thread(clientHandler).start();
            clientHandlers.add(clientHandler);
            this.players.add(player);

            String toRespond = "";

            if (this.gameStarted) {
                toRespond = "GAME_IN_PROGRESS";
            } else if (this.players.size() <= MAX_PLAYERS) {
                toRespond = "PASSWORD CORRECT";
            } else if (this.players.size() > MAX_PLAYERS) {
                toRespond = "SERVER_FULL";
            }


            String encryptedString = EncryptionUtil.encrypt(toRespond, encryptionKey);
            out.println(encryptedString);
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            clientHandler.sendMessage("PLAYER_NUMBER " + player.getPlayerNumber());

        } else {
            out.println("PASSWORD INCORRECT"); // Send response to client
            clientSocket.close();
            System.out.println("Incorrect password attempt or max players reached. Connection denied.");
        }
    }

    public void stopServer() {
        isRunning = false;
        closeServerSocket();
        System.out.println("Server stopped.");
        System.exit(0); // Exit the application
    }

    public void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(message);
        }
    }

    public void broadcastPlayerCount() {
        broadcastMessage("PLAYERS_CONNECTED " + players.size());
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

    private void sendGameStartedToAllClients() {
        for (ClientHandler handler : clientHandlers) {
            handler.sendMessage("GAME_STATE STARTED");
        }
    }

    public synchronized void startGame() {
        if (!gameStarted && readyPlayers.get() == players.size()) {
            gameStarted = true;
            currentPlayerIndex.set(0); // Always start with the first player who joined
            gameBoard.reset(); // Ensure the game board is fresh at start
            sendGameStartedToAllClients();
            switchTurns(); // Inform players whose turn it is
        }
    }

    public synchronized void processPlayerMove(Player player, int x, int y) {
        System.out.println("Processing move for player " + player.getPlayerNumber() + " at position " + x + ", " + y);
        if (!gameStarted) {
            System.out.println("Game has not started yet.");
            return;
        }

        if (players.get(currentPlayerIndex.get()).equals(player)) {
            boolean mineHit = gameBoard.revealCell(x, y, player);
            System.out.println("Mine hit: " + mineHit);
            if (mineHit) {
                int bombCount = gameBoard.getBombRevealedCount();
                if (bombCount >= 5) {
                    broadcastMessage("GAMEOVER");
                    endGame();
                } else {
                    updateAndBroadcastGameState();
                    switchTurns();
                }
            } else {
                updateAndBroadcastGameState();
                if (gameBoard.allNonMineCellsRevealed()) {
                    broadcastMessage("GAMEOVER AllCellsCleared");
                    endGame();
                } else {
                    switchTurns();
                }
            }
        } else {
            System.out.println("It's not " + player.getPlayerNumber() + "'s turn, it's "
                    + players.get(currentPlayerIndex.get()).getPlayerNumber() + "'s turn.");
        }
    }

    private void endGame() {
        gameStarted = false;
        updateAndBroadcastGameState();
        broadcastMessage("GAME_STATE OVER");

        // Close all client connections
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.closeConnection();
        }

        // Optionally, you can add a delay here if you want to give some time for
        // clients to process the game over message
        try {
            Thread.sleep(5000); // Wait for 5 seconds before stopping the server
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting to stop server: " + e.getMessage());
        }

        stopServer(); // Stop the server
    }

    public void sendCellState(ClientHandler clientHandler, int x, int y) {
        Cell cell = gameBoard.getCell(x, y);
        if (cell != null) {
            int cellState = cell.isRevealed() ? (cell.isMine() ? 2 : 1) : 0;
            clientHandler.sendMessage("CELL_STATE " + x + " " + y + " " + cellState);
        }
    }

    /**
     * Updates and broadcasts the current game state to all clients.
     */
    private void updateAndBroadcastGameState() {
        String gameStateMessage = createGameStateMessage();
        broadcastMessage("UPDATE " + gameStateMessage);
    }

    public synchronized void toggleFlag(int x, int y, boolean isFlagged, Player player) {
        if (!gameStarted) {
            System.out.println("The game has not started yet. You cannot flag cells.");
            return;
        }
        gameBoard.toggleFlag(x, y, isFlagged);
        updateAndBroadcastGameState();
    }

    private String createGameStateMessage() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Cell cell = gameBoard.getCell(x, y);
                int cellState = cell.isRevealed() ? (cell.isMine() ? 2 : 1) : (cell.isFlagged() ? 3 : 0);
                int minesCount = cell.isRevealed() && !cell.isMine() ? cell.getNeighboringMines() : 0;
                sb.append(x).append(",").append(y).append(",").append(cellState).append(",").append(minesCount)
                        .append(";");
            }
        }
        return sb.toString();
    }

    private void switchTurns() {
        if (players.size() > 1) {
            currentPlayerIndex.set((currentPlayerIndex.get() + 1) % players.size());
            while (!clientHandlers.stream()
                    .anyMatch(handler -> handler.getPlayer().equals(players.get(currentPlayerIndex.get())))) {
                currentPlayerIndex.set((currentPlayerIndex.get() + 1) % players.size()); // Skip missing players
            }
        }
        int currentPlayerNumber = players.get(currentPlayerIndex.get()).getPlayerNumber();
        System.out.println("Current player number: " + currentPlayerNumber);
        broadcastMessage("TURN_CHANGED " + currentPlayerNumber);
    }

    public synchronized void handlePlayerQuit(Player player) {
        int index = players.indexOf(player);
        boolean wasCurrentPlayer = index == currentPlayerIndex.get();
    
        players.remove(player);
        clientHandlers.removeIf(handler -> handler.getPlayer().equals(player));
        broadcastMessage("PLAYER_QUIT " + player.getPlayerNumber());
        broadcastPlayerCount();
    
        if (players.isEmpty()) {
            stopServer();
        } else {
            if (wasCurrentPlayer) {
                if (players.size() > 0) {
                    currentPlayerIndex.set((currentPlayerIndex.get() - 1 + players.size()) % players.size());
                    switchTurns(); // Move to the next player immediately
                    broadcastMessage("TURN_CHANGED " + players.get(currentPlayerIndex.get()).getPlayerNumber());
                }
            }
        }
    }

    public synchronized void removeClientHandler(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    public boolean isGameRunning() {
        return isRunning;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("You must provide a port number.");
            System.exit(1);
        }

        int port;
        String password;
        try {
            port = Integer.parseInt(args[0]);
            password = args[1];
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number.");
            System.exit(1);
            return;
        }

        GameServer server = new GameServer(port, password);
        server.startServer();
    }
}
