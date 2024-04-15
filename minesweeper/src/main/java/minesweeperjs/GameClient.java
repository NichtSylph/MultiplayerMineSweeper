package minesweeperjs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class GameClient {
    private String serverAddress;
    private int serverPort = 2805;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GameWindow gameWindow;
    private int currentPlayerNumber;
    private int playerNumber;
    private JFrame joinFrame;
    private JTextField ipTextField;
    private boolean gameStarted = false;
    private TriConsumer<Integer, Integer, Integer> neighboringMinesCountResponseHandler;

    public GameClient() {
        createJoinFrame();
    }

    private void createJoinFrame() {
        joinFrame = new JFrame("Join Game Lobby");
        joinFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        joinFrame.setSize(300, 150);
        joinFrame.setLayout(new BorderLayout());

        JLabel instructionLabel = new JLabel("Enter Game Lobby IP:");
        joinFrame.add(instructionLabel, BorderLayout.NORTH);

        ipTextField = new JTextField();
        joinFrame.add(ipTextField, BorderLayout.CENTER);

        JButton joinButton = new JButton("Join");
        joinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleJoinButtonAction();
            }
        });
        joinFrame.add(joinButton, BorderLayout.SOUTH);
    }

    private void handleJoinButtonAction() {
        String ip = ipTextField.getText();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(joinFrame, "Please enter a valid IP address.");
        } else {
            serverAddress = ip;
            joinFrame.setVisible(false);
            connectToServer();
        }
    }

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

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendStartGame() {
        sendMessage("START_GAME");
    }

    public void sendPlayerMove(int x, int y) {
        sendMessage("MOVE " + x + " " + y);
    }

    public void sendFlagChange(int x, int y, boolean isFlagged) {
        sendMessage("FLAG " + x + " " + y + " " + (isFlagged ? "1" : "0"));
    }

    public void sendReady() {
        sendMessage("READY " + playerNumber);
    }

    public void requestCellState(int x, int y) {
        sendMessage("REQUEST_CELL_STATE " + x + " " + y);
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    public int getCurrentPlayerNumber() {
        return currentPlayerNumber;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void getNeighboringMinesCount(int x, int y, TriConsumer<Integer, Integer, Integer> responseHandler) {
        sendMessage("REQUEST_NEIGHBORING_MINES_COUNT " + x + " " + y);
        this.neighboringMinesCountResponseHandler = responseHandler;
    }

    public void updateCellWithMinesCount(int x, int y, int count) {
        if (gameWindow != null) {
            gameWindow.updateCellWithMinesCount(x, y, count);
        }
    }

    private void handleNeighboringMinesCountResponse(String[] parts) {
        int responseX = Integer.parseInt(parts[1]);
        int responseY = Integer.parseInt(parts[2]);
        int count = Integer.parseInt(parts[3]);

        if (neighboringMinesCountResponseHandler != null) {
            SwingUtilities.invokeLater(() -> neighboringMinesCountResponseHandler.accept(responseX, responseY, count));
        }
    }

    private class ServerListener implements Runnable {
        public void run() {
            try {
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    // Process each message received from the server
                    processServerMessage(fromServer);
                }
            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
                // Handle potential cleanup or UI notification here
            } finally {
                closeConnection();
                // Handle the server closing connection here, update UI accordingly
                SwingUtilities.invokeLater(() -> gameWindow.notifyServerClosing());
            }
        }
    }

    private void processServerMessage(String message) {
        String[] parts = message.split(" ");
        System.out.println("Received message: " + message);
        try {
            switch (parts[0]) {
                case "GAME_STARTED":
                    gameStarted = true;
                    SwingUtilities.invokeLater(() -> gameWindow.notifyGameStarted());
                    break;
                case "GAME_NOT_STARTED":
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(gameWindow,
                            "The game has not started.", "Wait", JOptionPane.INFORMATION_MESSAGE));
                    break;
                case "GAME_STATE":
                    handleGameState(parts[1]);
                    break;
                case "UPDATE":
                    SwingUtilities.invokeLater(() -> parseGameStateAndUpdateBoard(message.substring(7)));
                    break;
                case "PLAYERS_CONNECTED":
                    int playerCount = Integer.parseInt(parts[1]);
                    SwingUtilities.invokeLater(() -> gameWindow.updatePlayerCount(playerCount));
                    break;
                case "TURN_CHANGED":
                    currentPlayerNumber = extractPlayerNumber(parts[1]);
                    SwingUtilities.invokeLater(() -> gameWindow.handleTurnChange(parts[1]));
                    break;
                case "NOT_YOUR_TURN":
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(gameWindow,
                            "It's not your turn yet!", "Wait", JOptionPane.INFORMATION_MESSAGE));
                    break;
                case "PLAYER_NUMBER":
                    playerNumber = parts.length > 1 && isNumeric(parts[1]) ? Integer.parseInt(parts[1]) : playerNumber;
                    SwingUtilities.invokeLater(() -> gameWindow.updatePlayerNumber(playerNumber));
                    break;
                case "CELL_STATE":
                    if (gameStarted) {
                        processCellStateResponse(parts);
                    }
                    break;
                case "NEIGHBORING_MINES_COUNT_RESPONSE":
                    handleNeighboringMinesCountResponse(parts);
                    break;

                case "SCORE_UPDATE":
                    int newScore = Integer.parseInt(parts[1]);
                    SwingUtilities.invokeLater(() -> gameWindow.updateScore(newScore));
                    break;
                case "GAME_STATE_UPDATE":
                    if (gameStarted) {
                        String gameState = message.substring(message.indexOf(" ") + 1);
                        SwingUtilities.invokeLater(() -> parseGameStateAndUpdateBoard(gameState));
                    }
                    break;
                case "PLAYER_QUIT":
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(gameWindow,
                            parts[1] + " has quit the game", "Player Quit", JOptionPane.INFORMATION_MESSAGE));
                    break;
                case "GAMEOVER":
                    handleGameOverMessage(parts);
                    break;
                default:
                    System.out.println("Unknown server message: " + message);
                    break;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing server message: " + e.getMessage());
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int extractPlayerNumber(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return -1; // Return an invalid number if the string is null or empty
        }
        try {
            // Assuming the player number is at the end of the string after a space
            String[] parts = playerName.split(" ");
            if (parts.length > 1) {
                return Integer.parseInt(parts[parts.length - 1]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error extracting player number: " + e.getMessage());
        }
        return -1; // Return an invalid number if parsing fails
    }

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

    private void handleGameOverMessage(String[] parts) {
        gameWindow.displayGameOver();
    }

    private void processCellStateResponse(String[] parts) {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int state = Integer.parseInt(parts[3]);
        SwingUtilities.invokeLater(() -> gameWindow.updateCell(x, y, state));
    }

    private void parseGameStateAndUpdateBoard(String gameState) {
        String[] updates = gameState.split(";");
        for (String update : updates) {
            String[] cellData = update.split(",");
            if (cellData.length == 3) { // Expecting 3 parts: x, y, state
                int x = Integer.parseInt(cellData[0]);
                int y = Integer.parseInt(cellData[1]);
                int state = Integer.parseInt(cellData[2]);
                SwingUtilities.invokeLater(() -> gameWindow.updateCell(x, y, state));
            }
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
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
            client.joinFrame.setVisible(true); // Show the join frame
        });
    }

}
