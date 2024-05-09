package minesweeperjs;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.function.BooleanSupplier;

public class GameClient {
    private String serverAddress;
    private Socket socket;
    private int serverPort;
    private BufferedReader in;
    private PrintWriter out;
    private GameWindow gameWindow;
    private Integer playerNumber;
    private Boolean currentClient;
    private JFrame joinFrame;
    private JTextField ipTextField;
    private boolean gameStarted = false;

    public GameClient() {
        createJoinFrame();
    }

    private void createJoinFrame() {
        joinFrame = new JFrame("Join Game Lobby");
        joinFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        joinFrame.setSize(425, 175); // Increase the width of the box
        joinFrame.setLayout(new BorderLayout());

        JLabel instructionLabel = new JLabel("Enter Game Lobby Details:");
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 2, 0));
        joinFrame.add(instructionLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 2, 10));

        JLabel ipLabel = new JLabel("Server IP:");
        ipTextField = new JTextField();
        inputPanel.add(ipLabel);
        inputPanel.add(ipTextField);

        JLabel portLabel = new JLabel("Port:");
        JTextField portTextField = new JTextField();
        inputPanel.add(portLabel);
        inputPanel.add(portTextField);

        joinFrame.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JButton joinButton = new JButton("Join");
        joinButton.addActionListener(e -> handleJoinButtonAction(portTextField));
        buttonPanel.add(joinButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            joinFrame.dispose();
            System.exit(0);
        });
        buttonPanel.add(cancelButton);

        joinFrame.add(buttonPanel, BorderLayout.SOUTH);

        joinFrame.setLocationRelativeTo(null); // Center on screen
    }

    private void handleJoinButtonAction(JTextField portTextField) {
        String ip = ipTextField.getText().trim();
        String portString = portTextField.getText().trim();
        if (ip.isEmpty() || portString.isEmpty()) {
            JOptionPane.showMessageDialog(joinFrame, "Please enter a valid IP address and port.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            serverAddress = ip;
            serverPort = Integer.parseInt(portString);
            joinFrame.setVisible(false);

            currentPlayer = new Player("PlayerName");
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
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error connecting to server: " + e.getMessage(), "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            joinFrame.setVisible(true); // Show join frame again to allow reconnection attempt
        }
    }

    // KKM
    public void endTurn() {
        sendMessage("ENDTURN");
    }

    public Boolean isCurrentClient() {
        sendMessage("ISMYTURN");
        try {
            String res = in.readLine();
            if (res == "T") {
                currentClient = true;
            } else if (res == "F") {
                currentClient = false;
            }
        } catch (IOException e) {
            currentClient = false;
        }

        return currentClient;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendPlayerMove(int x, int y) {
        sendMessage("MOVE " + x + " " + y);
    }

    public void sendFlagChange(int x, int y, boolean isFlagged) {
        sendMessage("FLAG " + x + " " + y + " " + (isFlagged ? "1" : "0"));
    }

    public void sendReady() {
        sendMessage("READY");
        gameWindow.getReadyButton().setEnabled(false); // Disable the Ready button when the Ready message is sent
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
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
                SwingUtilities.invokeLater(() -> gameWindow.notifyServerClosing());
            } finally {
                closeConnection();
            }
        }

        private void processServerMessage(String message) {
            String[] parts = message.split(" ", 2);
            switch (parts[0]) {
                case "WELCOME":
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(gameWindow, parts[1], "Welcome",
                            JOptionPane.INFORMATION_MESSAGE));
                    break;
                case "PLAYER_NUMBER":
                    currentPlayer.setPlayerNumber(Integer.parseInt(parts[1]));
                    break;
                case "PLAYERS_CONNECTED":
                    int connectedPlayers = Integer.parseInt(parts[1]);
                    SwingUtilities.invokeLater(() -> gameWindow.updatePlayerCount(connectedPlayers));
                    break;
                case "WAITING_FOR_PLAYERS":
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(gameWindow,
                            "Waiting for other players to be ready.", "Wait",
                            JOptionPane.INFORMATION_MESSAGE));
                    break;
                case "GAME_STARTED":
                    gameStarted = true;
                    SwingUtilities.invokeLater(() -> {
                        gameWindow.notifyGameStarted();
                        gameWindow.getReadyButton().setEnabled(false); // Disable the Ready button when the game starts
                    });
                    break;
                case "MOVE_RESULT":
                    String[] moveResultParts = parts[1].split(" ");
                    int x = Integer.parseInt(moveResultParts[0]);
                    int y = Integer.parseInt(moveResultParts[1]);
                    boolean isRevealed = moveResultParts[2].equals("1");
                    SwingUtilities.invokeLater(() -> gameWindow.updateCellState(x, y, isRevealed));
                    break;
                case "TURN_CHANGED":
                    int newPlayerNumber = Integer.parseInt(parts[1]);
                    if (currentPlayer.getPlayerNumber() == newPlayerNumber) {
                        currentPlayer.setCurrentTurn(true);
                    } else {
                        currentPlayer.setCurrentTurn(false);
                    }
                    if (gameStarted) {
                        SwingUtilities.invokeLater(() -> gameWindow.updateTurnStatus(currentPlayer.isCurrentTurn()));
                    }
                    break;
                case "GAMEOVER":
                    SwingUtilities.invokeLater(gameWindow::displayGameOver);
                    break;
                default:
                    System.out.println("Unknown server message: " + message);
                    break;
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
            client.joinFrame.setVisible(true);
        });
    }
}
