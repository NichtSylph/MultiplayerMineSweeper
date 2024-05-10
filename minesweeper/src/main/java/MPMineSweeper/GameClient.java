package MPMineSweeper;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;

public class GameClient {
    private JTextField ipTextField, portTextField, passwordTextField;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GameWindow gameWindow;
    private int currentPlayerNumber = -1; // Initialize as -1 to denote unset
    private int playerNumber = -1; // Initialize as -1 to denote unset
    private Integer playerScore = 0; // Initialize as -1 to denote unset
    private JFrame joinFrame;
    private JButton joinButton;
    private boolean gameStarted = false;
    private String encryptionKey;

    public GameClient() {
        this.createJoinFrame();
    }

    private void createJoinFrame() {
        joinFrame = new JFrame("Join Game Lobby");
        joinFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        joinFrame.setSize(400, 200);
        joinFrame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2));
        joinFrame.add(inputPanel, BorderLayout.CENTER);

        inputPanel.add(new JLabel("Server IP:"));
        ipTextField = new JTextField();
        inputPanel.add(ipTextField);

        inputPanel.add(new JLabel("Port:"));
        portTextField = new JTextField();
        inputPanel.add(portTextField);

        inputPanel.add(new JLabel("Password:"));
        passwordTextField = new JTextField();
        inputPanel.add(passwordTextField);

        JPanel buttonPanel = new JPanel();
        JButton joinButton = new JButton("Join");
        joinButton.addActionListener(this::handleJoinAction);
        buttonPanel.add(joinButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> System.exit(0));
        buttonPanel.add(cancelButton);

        joinFrame.add(buttonPanel, BorderLayout.SOUTH);

        joinFrame.setLocationRelativeTo(null);
        joinFrame.setVisible(true);
    }

    private void handleJoinAction(ActionEvent e) {
        this.joinButton = (JButton) e.getSource();
        this.joinButton.setEnabled(false); // Disable the join button to prevent multiple clicks

        connectToServer();
    }

    public void connectToServer() {
        try {

            String serverIP = ipTextField.getText().trim();
            int serverPort = Integer.parseInt(portTextField.getText().trim());
            String password = passwordTextField.getText().trim();

            socket = new Socket();
            socket.connect(new InetSocketAddress(serverIP, serverPort), 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            this.encryptionKey = EncryptionUtil.createKey();

            // Start listening to the server after the window is visible
            out.println(password + " " + this.encryptionKey);
            out.flush();
            

            String response = in.readLine(); 

            if (response != null) {
                String decryptedString = EncryptionUtil.decrypt(response, this.encryptionKey);
                String[] parts = decryptedString.split(" ");

                if (parts[0].equals("PASSWORD") && parts.length == 2) {
                    SwingUtilities.invokeLater(() -> {
                        if (parts[1].equalsIgnoreCase("CORRECT")) {
                            this.openGameWindow();
                        } else if (parts[1].equalsIgnoreCase("INCORRECT")) {
                            JOptionPane.showMessageDialog(joinFrame, "Password incorrect. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                        }

                        this.joinButton.setEnabled(true);
                    });
                } else if (parts[0].equals("GAME_IN_PROGRESS")) {
                    SwingUtilities.invokeLater(() -> {
                        this.joinFrame.dispose(); 
                        JOptionPane.showMessageDialog(null, "Sorry, a game is currently in progress.", "Notification",
                                    JOptionPane.INFORMATION_MESSAGE);
                        closeConnection();
                        System.exit(0);
                    });
                }  else if (parts[0].equals("SERVER_FULL")) {
                    SwingUtilities.invokeLater(() -> {
                        this.joinFrame.dispose(); 
                        JOptionPane.showMessageDialog(null, "Sorry, the server is full.", "Notification",
                                    JOptionPane.INFORMATION_MESSAGE);
                        closeConnection();
                        System.exit(0);
                    });
                }

            }
        } catch (SocketTimeoutException ex) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(joinFrame, "Connection timed out. Please check the IP and port and try again.",
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                joinButton.setEnabled(true);
            });
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(joinFrame, "Unable to connect to server. Please check your network connection and server status.",
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                joinButton.setEnabled(true);
            });
        } catch (NumberFormatException ex) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(joinFrame, "Please enter a valid port number.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                joinButton.setEnabled(true);
            });
        }
    }

    public void sendMessage(String message) {
        String encryptedMessage = EncryptionUtil.encrypt(message, this.encryptionKey);
        out.println(encryptedMessage);
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

    private void openGameWindow() {
        // Initialize the game window before starting the listener thread
        
        gameWindow = new GameWindow(this);
        gameWindow.setVisible(true);
        joinFrame.dispose(); 

        new Thread(new ServerListener()).start();
    }

    private void processServerMessage(String message) {
        String decryptedString = EncryptionUtil.decrypt(message, this.encryptionKey);
        String[] parts = decryptedString.split(" ");
        try {
            SwingUtilities.invokeLater(() -> {
                switch (parts[0]) {
                    case "PASSWORD":
                        if (parts.length == 2) {
                            SwingUtilities.invokeLater(() -> {
                                if (parts[1].equalsIgnoreCase("CORRECT")) {
                                    this.openGameWindow();
                                } else if (parts[1].equalsIgnoreCase("INCORRECT")) {
                                    JOptionPane.showMessageDialog(joinFrame, "Password incorrect. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                                }

                                this.joinButton.setEnabled(true);
                            });
                        }
                        break;
                    case "SERVER_FULL":
                        JOptionPane.showMessageDialog(null, "Sorry, the server is full.", "Notification",
                                JOptionPane.INFORMATION_MESSAGE);
                        closeConnection();
                        System.exit(0);
                        break;
                    case "GAME_IN_PROGRESS":
                        JOptionPane.showMessageDialog(null, "Sorry, a game is currently in progress.", "Notification",
                                JOptionPane.INFORMATION_MESSAGE);
                        closeConnection();
                        System.exit(0);
                        break;
                    case "PLAYERS_CONNECTED":
                        int playerCount = Integer.parseInt(parts[1]);
                        if (gameWindow != null)
                            gameWindow.updatePlayerCount(playerCount);
                        break;
                    case "PLAYER_QUIT":
                        int quitPlayerNumber = Integer.parseInt(parts[1]);
                        System.out.println("Player " + quitPlayerNumber + " has quit the game.");
                        if (gameWindow != null)
                            gameWindow.displayPlayerQuit(quitPlayerNumber);
                        break;
                    case "GAME_STATE":
                        handleGameState(parts[1]);
                        break;
                    case "UPDATE":
                        parseGameStateAndUpdateBoard(decryptedString.substring(7));
                        break;
                    case "SCORE":
                        this.playerScore = Integer.valueOf(parts[1]);
                        this.gameWindow.updatePlayerScore(this.playerScore);
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
                        if (gameWindow != null) {
                            gameWindow.updatePlayerNumber(playerNumber);
                        }
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

    public void sendQuitMessage() {
        if (playerNumber != -1) { // Only send quit message if player number is set
            sendMessage("QUIT " + playerNumber);
        }
    }

    public void closeConnection() {
        sendQuitMessage(); // Send quit message before closing the connection
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
        SwingUtilities.invokeLater(GameClient::new);
    }
}
