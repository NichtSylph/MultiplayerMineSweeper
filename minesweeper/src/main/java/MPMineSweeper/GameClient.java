package MPMineSweeper;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class GameClient {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GameWindow gameWindow;
    private int currentPlayerNumber;
    private int playerNumber;
    private JFrame joinFrame;
    private boolean gameStarted = false;

    /**
     * Constructor for GameClient. Sets up the GUI for joining the game lobby.
     */
    public GameClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.joinFrame = new JFrame(); // initialize joinFrame
        connectToServer();
    }

    /**
     * Establishes connection to the Minesweeper game server.
     */
    public void connectToServer() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(new ServerListener()).start();
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);

            gameWindow = new GameWindow(this);
            gameWindow.setVisible(true);
            joinFrame.setVisible(false);
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Sends a signal to the server to start the game.
     */
    public void sendStartGame() {
        sendMessage("START_GAME");
    }

    /**
     * Sends the player's move to the server.
     *
     * @param x The x-coordinate of the move.
     * @param y The y-coordinate of the move.
     */
    public void sendPlayerMove(int x, int y) {
        if (!gameStarted) {
            System.out.println("The game has not started yet.");
            return;
        }
        sendMessage("MOVE " + x + " " + y + " " + playerNumber);
    }

    /**
     * Sends a change of flag status for a cell to the server.
     *
     * @param x         The x-coordinate of the cell.
     * @param y         The y-coordinate of the cell.
     * @param isFlagged The new flag status.
     */
    public void sendFlagChange(int x, int y, boolean isFlagged) {
        if (!gameStarted) {
            JOptionPane.showMessageDialog(null, "The game has not started yet. You cannot flag cells.", "Notification",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String flagState = isFlagged ? "1" : "0";
        sendMessage("FLAG " + x + " " + y + " " + flagState);
    }

    /**
     * Notifies the server that the player is ready to start the game.
     */
    public void sendReady() {
        sendMessage("READY " + playerNumber);
    }

    /**
     * Requests the current state of a specific cell from the server.
     *
     * @param x The x-coordinate of the cell.
     * @param y The y-coordinate of the cell.
     */
    public void requestCellState(int x, int y) {
        sendMessage("REQUEST_CELL_STATE " + x + " " + y);
    }

    /**
     * Gets the number representing the current player.
     *
     * @return The number of the current player.
     */
    public int getCurrentPlayerNumber() {
        return currentPlayerNumber;
    }

    /**
     * Gets the player number assigned to this client.
     *
     * @return The player number.
     */
    public int getPlayerNumber() {
        return playerNumber;
    }

    /**
     * Inner class to listen for messages from the server.
     */
    private class ServerListener implements Runnable {
        public void run() {
            try {
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    processServerMessage(fromServer);
                }
            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }
    }

    /**
     * Processes incoming messages from the server.
     *
     * @param message The message received from the server.
     */
    private void processServerMessage(String message) {
        String[] parts = message.split(" ");
        System.out.println("Received message: " + message);
        try {
            switch (parts[0]) {
                case "PLAYERS_CONNECTED":
                    int playerCount = Integer.parseInt(parts[1]);
                    if (gameWindow != null) {
                        SwingUtilities.invokeLater(() -> gameWindow.updatePlayerCount(playerCount));
                    }
                    break;
                case "GAME_STATE":
                    handleGameState(parts[1]);
                    break;
                case "UPDATE":
                    parseGameStateAndUpdateBoard(message.substring(7));
                    break;
                case "GAMEOVER":
                    handleGameOverMessage(parts);
                    break;
                case "TURN_CHANGED":
                    if (parts.length > 1 && isNumeric(parts[1])) {
                        currentPlayerNumber = Integer.parseInt(parts[1]);
                        SwingUtilities
                                .invokeLater(() -> gameWindow.handleTurnChange(String.valueOf(currentPlayerNumber)));
                    }
                    break;
                case "PLAYER_NUMBER":
                    if (parts.length > 1 && isNumeric(parts[1])) {
                        playerNumber = Integer.parseInt(parts[1]);
                        SwingUtilities.invokeLater(() -> gameWindow.updatePlayerNumber(playerNumber));
                    }
                    break;
                case "CELL_STATE":
                    processCellStateResponse(parts);
                    break;
                default:
                    System.out.println("Unknown server message: " + message);
                    break;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing server message: " + e.getMessage());
        }
    }

    /**
     * Handles changes in the game state as reported by the server.
     *
     * @param gameState The current state of the game.
     */
    private void handleGameState(String gameState) {
        switch (gameState) {
            case "STARTED":
                gameStarted = true;
                gameWindow.updateGameState("STARTED");
                break;
            case "OVER":
                gameStarted = false;
                gameWindow.updateGameState("OVER");
                break;
            default:
                System.out.println("Unknown game state: " + gameState);
                break;
        }
    }

    /**
     * Checks if a string is numeric.
     *
     * @param str The string to check.
     * @return true if the string is numeric, false otherwise.
     */
    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Handles the game over message from the server.
     *
     * @param parts The message parts received from the server.
     */
    private void handleGameOverMessage(String[] parts) {
        gameWindow.displayGameOver();
    }

    /**
     * Processes and updates the cell state based on the server's response.
     *
     * @param parts The message parts received from the server.
     */
    private void processCellStateResponse(String[] parts) {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int state = Integer.parseInt(parts[3]);
        int minesCount = parts.length > 4 ? Integer.parseInt(parts[4]) : 0; // Assuming server sends mine counts
        SwingUtilities.invokeLater(() -> gameWindow.updateCell(x, y, state, minesCount));
    }

    /**
     * Parses the game state update from the server and updates the board
     * accordingly.
     *
     * @param gameState The updated game state from the server.
     */
    private void parseGameStateAndUpdateBoard(String gameState) {
        String[] updates = gameState.split(";");
        for (String update : updates) {
            String[] cellData = update.split(",");
            if (cellData.length == 4) { // Expecting four parts: x, y, state, minesCount
                int x = Integer.parseInt(cellData[0]);
                int y = Integer.parseInt(cellData[1]);
                int state = Integer.parseInt(cellData[2]);
                int minesCount = Integer.parseInt(cellData[3]); // Get the count of neighboring mines
                SwingUtilities.invokeLater(() -> gameWindow.updateCell(x, y, state, minesCount));
            }
        }
    }

    /**
     * Closes the network connection with the server.
     */
    public void closeConnection() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            System.err.println("Error closing the connection: " + e.getMessage());
        }
    }

    /**
     * The main method to start the GameClient.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java GameClient <server address> <server port>");
            System.exit(1);
        }

        String serverAddress = args[0];
        int serverPort;

        try {
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid server port number.");
            System.exit(1);
            return;
        }

        new GameClient(serverAddress, serverPort);
    }
}
