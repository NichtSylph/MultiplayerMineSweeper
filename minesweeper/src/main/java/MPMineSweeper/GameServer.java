package MPMineSweeper;

import java.io.IOException;
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
    private static final int MINES = 40;

    /**
     * Constructor to initialize the game server.
     * 
     * @param port The port number on which the server will run.
     */
    public GameServer(int port) {
        this.port = port;
        clientHandlers = new ArrayList<>();
        gameBoard = new GameBoard(WIDTH, HEIGHT, MINES);
        players = new ArrayList<>();
        currentPlayerIndex = new AtomicInteger(0);
        readyPlayers = new AtomicInteger(0);
        gameStarted = false;
        playerCount = new AtomicInteger(0);
        isRunning = true;
    }

    /**
     * Starts the game server and listens for incoming client connections.
     */
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server running on port " + port);

            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String clientAddress = clientSocket.getInetAddress().getHostAddress();
                        System.out.println("Client connected: " + clientAddress);

                        synchronized (this) {
                            Player newPlayer = new Player(playerCount.incrementAndGet());
                            players.add(newPlayer);
                            System.out.println("New player added. Total players: " + playerCount.get());

                            // Create the ClientHandler
                            ClientHandler clientHandler = new ClientHandler(clientSocket, this, newPlayer);
                            clientHandlers.add(clientHandler);

                            // Start ClientHandler thread
                            new Thread(clientHandler).start();

                            // Send player number immediately after adding the client handler
                            clientHandler.sendMessage("PLAYER_NUMBER " + newPlayer.getPlayerNumber());
                        }

                        broadcastPlayerCount();

                    } catch (IOException e) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Error creating server socket: " + e.getMessage());
        }
    }

    /**
     * Closes the server socket.
     */
    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    /**
     * Stops the server and exits the application.
     */
    public void stopServer() {
        isRunning = false;
        closeServerSocket();
        System.out.println("Server stopped.");
        System.exit(0); // Exit the application
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message The message to be sent.
     */
    public void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(message);
        }
    }

    /**
     * Broadcasts the current player count to all connected clients.
     */
    public void broadcastPlayerCount() {
        broadcastMessage("PLAYERS_CONNECTED " + players.size());
    }

    /**
     * Marks a player as ready and starts the game if all players are ready.
     *
     * @param player The player who is ready.
     */
    public synchronized void playerReady(Player player) {
        if (!gameStarted) {
            player.setReady(true);
            readyPlayers.incrementAndGet();
            if (readyPlayers.get() == players.size()) {
                startGame();
            }
        }
    }

    /**
     * Notifies all clients that the game has started.
     */
    private void sendGameStartedToAllClients() {
        for (ClientHandler handler : clientHandlers) {
            handler.sendMessage("GAME_STATE STARTED");
        }
    }

    /**
     * Starts the game if all players are ready.
     */
    public synchronized void startGame() {
        if (!gameStarted && readyPlayers.get() == players.size()) {
            gameStarted = true;
            currentPlayerIndex.set(0);  // Always start with the first player who joined
            gameBoard.reset(); // Ensure the game board is fresh at start
            sendGameStartedToAllClients();
            switchTurns();  // Inform players whose turn it is
        }
    }
    

    /**
     * Processes a player move and updates the game state accordingly.
     *
     * @param player The player who made the move.
     * @param x      The x-coordinate of the move.
     * @param y      The y-coordinate of the move.
     */
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
            System.out.println("It's not " + player.getPlayerNumber() + "'s turn, it's " + players.get(currentPlayerIndex.get()).getPlayerNumber() + "'s turn.");
        }
    }
    
    /**
     * Ends the game, notifying all clients and closing connections.
     */
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

    /**
     * Sends the state of a specific cell to a client.
     *
     * @param clientHandler The client handler to send the state to.
     * @param x             The x-coordinate of the cell.
     * @param y             The y-coordinate of the cell.
     */
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

    /**
     * Toggles a flag on a cell and updates game state.
     *
     * @param x         The x-coordinate of the cell.
     * @param y         The y-coordinate of the cell.
     * @param isFlagged The flag status to set.
     * @param player    The player who toggled the flag.
     */
    public synchronized void toggleFlag(int x, int y, boolean isFlagged, Player player) {
        if (!gameStarted) {
            System.out.println("The game has not started yet. You cannot flag cells.");
            return;
        }
        gameBoard.toggleFlag(x, y, isFlagged);
        updateAndBroadcastGameState();
    }

    /**
     * Creates a message representing the current state of the game board.
     * The message includes the cell's x, y coordinates, state, and the number of
     * neighboring mines.
     *
     * @return A string representing the game state.
     */
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

    /**
     * Switches the turn to the next player in the game.
     */
    private void switchTurns() {
        currentPlayerIndex.set((currentPlayerIndex.get() + 1) % players.size());
        System.out.println("Current player index: " + currentPlayerIndex.get());
        int currentPlayerNumber = players.get(currentPlayerIndex.get()).getPlayerNumber();
        System.out.println("Current player number: " + currentPlayerNumber);
        broadcastMessage("TURN_CHANGED " + currentPlayerNumber);
    }

    /**
     * Handles the case when a player quits the game.
     *
     * @param player The player who quit.
     */
    public synchronized void handlePlayerQuit(Player player) {
        players.remove(player);
        clientHandlers.removeIf(handler -> handler.getPlayer().equals(player));
        broadcastMessage("PLAYER_QUIT " + player.getPlayerNumber());
        broadcastPlayerCount();
        if (players.isEmpty()) {
            stopServer();
        }
    }

    /**
     * Removes a client handler when a client disconnects.
     *
     * @param handler The client handler to remove.
     */
    public synchronized void removeClientHandler(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    /**
     * Checks if the game is currently running.
     *
     * @return True if the game is running, false otherwise.
     */
    public boolean isGameRunning() {
        return isRunning;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("You must provide a port number.");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number.");
            System.exit(1);
            return;
        }

        GameServer server = new GameServer(port);
        server.startServer();
    }
}
