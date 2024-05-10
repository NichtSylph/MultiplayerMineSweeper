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
    private int currentPlayerNumber = -1; // Initialize as -1 to denote unset
    private int playerNumber = -1; // Initialize as -1 to denote unset
    private JFrame joinFrame;
    private boolean gameStarted = false;

    public GameClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.joinFrame = new JFrame(); // Initialize joinFrame
        connectToServer();
    }

    public void connectToServer() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Initialize the game window before starting the listener thread
            gameWindow = new GameWindow(this);
            gameWindow.setVisible(true);
            joinFrame.setVisible(false);

            // Start listening to the server after the window is visible
            new Thread(new ServerListener()).start();
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            System.exit(1);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendStartGame() {
        sendMessage("START_GAME");
    }

    public void sendPlayerMove(int x, int y) {
        if (!gameStarted || playerNumber == -1) {
            System.out.println("The game has not started yet or player number not set.");
            return;
        }
        sendMessage("MOVE " + x + " " + y + " " + playerNumber);
    }

    public void sendFlagChange(int x, int y, boolean isFlagged) {
        if (!gameStarted) {
            JOptionPane.showMessageDialog(null, "The game has not started yet. You cannot flag cells.", "Notification",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        sendMessage("FLAG " + x + " " + y + " " + (isFlagged ? "1" : "0"));
    }

    public void sendReady() {
        sendMessage("READY " + playerNumber);
    }

    public void requestCellState(int x, int y) {
        sendMessage("REQUEST_CELL_STATE " + x + " " + y);
    }

    public int getCurrentPlayerNumber() {
        return currentPlayerNumber;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }
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

    private void processServerMessage(String message) {
        String[] parts = message.split(" ");
        try {
            SwingUtilities.invokeLater(() -> {
                switch (parts[0]) {
                    case "PLAYERS_CONNECTED":
                        int playerCount = Integer.parseInt(parts[1]);
                        if (gameWindow != null)
                            gameWindow.updatePlayerCount(playerCount);
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
                        currentPlayerNumber = Integer.parseInt(parts[1]);
                        System.out
                                .println("TURN_CHANGED received, current player number set to: " + currentPlayerNumber);
                        if (gameWindow != null) {
                            gameWindow.handleTurnChange(currentPlayerNumber);
                        }
                        break;
                    case "PLAYER_NUMBER":
                        playerNumber = Integer.parseInt(parts[1]);
                        if (gameWindow != null)
                            gameWindow.updatePlayerNumber(playerNumber);
                        System.out.println("Player number set to: " + playerNumber);
                        break;
                    case "CELL_STATE":
                        processCellStateResponse(parts);
                        break;
                    default:
                        System.out.println("Unknown server message: " + message);
                        break;
                }
            });
        } catch (NumberFormatException e) {
            System.err.println("Error parsing server message: " + e.getMessage());
        }
    }

    private void handleGameState(String gameState) {
        switch (gameState) {
            case "STARTED":
                gameStarted = true;
                SwingUtilities.invokeLater(() -> gameWindow.updateGameState("STARTED"));
                break;
            case "OVER":
                gameStarted = false;
                SwingUtilities.invokeLater(() -> gameWindow.updateGameState("OVER"));
                break;
            default:
                System.out.println("Unknown game state: " + gameState);
                break;
        }
    }

    private void handleGameOverMessage(String[] parts) {
        SwingUtilities.invokeLater(gameWindow::displayGameOver);
    }

    private void processCellStateResponse(String[] parts) {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int state = Integer.parseInt(parts[3]);
        int minesCount = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
        SwingUtilities.invokeLater(() -> gameWindow.updateCell(x, y, state, minesCount));
    }

    private void parseGameStateAndUpdateBoard(String gameState) {
        String[] updates = gameState.split(";");
        for (String update : updates) {
            String[] cellData = update.split(",");
            int x = Integer.parseInt(cellData[0]);
            int y = Integer.parseInt(cellData[1]);
            int state = Integer.parseInt(cellData[2]);
            int minesCount = Integer.parseInt(cellData[3]);
            SwingUtilities.invokeLater(() -> gameWindow.updateCell(x, y, state, minesCount));
        }
    }

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
