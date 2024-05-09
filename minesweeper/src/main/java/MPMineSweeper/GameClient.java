package MPMineSweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class GameClient {
    private JFrame joinFrame;
    private JTextField ipTextField, portTextField, passwordTextField;
    private GameWindow gameWindow;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private Player player;
    private int score = 0;
    private boolean gameStarted;
    private boolean isCurrentActivePlayer;

    public GameClient() {
        this.player = new Player();
        createJoinFrame();
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
        JButton joinButton = (JButton) e.getSource();
        joinButton.setEnabled(false); // Disable the join button to prevent multiple clicks immediately
    
        new Thread(() -> {
            try {
                String serverIP = ipTextField.getText().trim();
                int serverPort = Integer.parseInt(portTextField.getText().trim());
                String password = passwordTextField.getText().trim();
    
                System.out.println("Attempting to connect to server at IP: " + serverIP + ", port: " + serverPort);
    
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIP, serverPort), 5000); // 5000 ms timeout
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
                // Send password to server and flush to ensure it's sent immediately
                System.out.println("Sending password to server: " + password);
                out.println(password);
                out.flush();
    
                // Read response from server
                String response = in.readLine();
                System.out.println("Received response from server: " + response);
    
                SwingUtilities.invokeLater(() -> {
                    if ("Password incorrect".equals(response)) {
                        JOptionPane.showMessageDialog(joinFrame, "Password incorrect. Please try again.",
                                "Login Failed", JOptionPane.ERROR_MESSAGE);
                    } else if ("Password correct".equals(response)) {
                        openGameWindow(); // Opens the game window if the password is correct
                    } else if (response.startsWith("Game Full")) {
                        JOptionPane.showMessageDialog(joinFrame, "The game is full. Please try again later.",
                                "Game Full", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(joinFrame, "Unexpected response from server.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    joinButton.setEnabled(true); // Re-enable the join button irrespective of the server response
                });
            } catch (SocketTimeoutException ex) {
                System.out.println("Connection timed out.");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(joinFrame,
                            "Connection timed out. Please check the IP and port and try again.",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    joinButton.setEnabled(true);
                });
            } catch (IOException ex) {
                System.out.println("Unable to connect to server.");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(joinFrame,
                            "Unable to connect to server. Please check your network connection and server status.",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    joinButton.setEnabled(true);
                });
            } catch (NumberFormatException ex) {
                System.out.println("Invalid port number entered.");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(joinFrame, "Please enter a valid port number.",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    joinButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void openGameWindow() {
        gameWindow = new GameWindow(this);
        gameWindow.setVisible(true);
        joinFrame.dispose(); // Dispose the join frame after successful connection
    }

    public Player getCurrentPlayer() {
        return isCurrentActivePlayer ? player : null;
    }

    public Integer getPlayerNumber() {
        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer != null) {
            return currentPlayer.getPlayerNumber();
        } else {
            send("GET_CURRENT_PLAYER_NUMBER");
            try {
                processServerMessage(in.readLine());
            } catch (IOException e) {
                System.err.println("Error handling command from client: " + e.getMessage());
            }
            return currentPlayer != null ? currentPlayer.getPlayerNumber() : null;
        }
    }

    private void handleDisconnection() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(gameWindow,
                    "Player " + (player.getPlayerNumber() + 1) + " has quit the game.",
                    "Player Disconnected", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public synchronized boolean isGameStarted() {
        send("IS_GAME_STARTED");
    
        try {
            String response = in.readLine();
            processServerMessage(response);
        } catch (IOException e) {
            System.err.println("Error handling command from client: " + e.getMessage());
        }
    
        return gameStarted;
    }

    public void endTurn() {
        send("END_TURN");
    }

    public Boolean checkCurrentActivePlayer() {
        send("IS_CURRENT_ACTIVE_PLAYER");

        try {
            processServerMessage(in.readLine());
        } catch (IOException e) {
            System.err.println("Error handling command from client: " + e.getMessage());
        }

        return isCurrentActivePlayer;
    }

    public void sendReady() {
        if (out != null) {
            out.println("READY");
        }
    }

    public void sendPlayerMove(int x, int y) {
        if (out != null) {
            System.out.println("Sending MOVE command to server with coordinates: " + x + ", " + y);
            out.println("MOVE " + x + " " + y);
        }
    }

    public void sendFlagChange(int x, int y, boolean isFlagged) {
        if (out != null) {
            System.out.println("Sending FLAG command to server with coordinates: " + x + ", " + y + " and flag status: "
                    + isFlagged);
            out.println("FLAG " + x + " " + y + " " + isFlagged);
        }
    }

    private void processServerMessage(String inputLine) {
        if (inputLine != null) {
            System.out.println("Received message from server: " + inputLine);
            String[] parts = inputLine.split(" ");
            if (parts.length > 0) {
                switch (parts[0]) {
                    case "WAITING_FOR_PLAYERS":
                        if (parts.length == 3) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(gameWindow,
                                        "Waiting for more players to be ready... (" + parts[1] + "/" + parts[2] + ")",
                                        "Waiting", JOptionPane.INFORMATION_MESSAGE);
                            });
                        }
                        break;
                    case "CURRENT_PLAYER_NUMBER":
                        if (parts.length == 2) {
                            int num = Integer.parseInt(parts[1]);
                            if (player != null) {
                                player.setPlayerNumber(num);
                                SwingUtilities.invokeLater(() -> {
                                    gameWindow.setPlayerNumber(num);  // Make sure GameWindow has a method to update this
                                });
                            }
                        }
                        break;
                    case "GAME_STARTED":
                        SwingUtilities.invokeLater(() -> {
                            gameStarted = true;
                            JOptionPane.showMessageDialog(gameWindow, "Game has started!", "Game Started",
                                    JOptionPane.INFORMATION_MESSAGE);
                            gameWindow.notifyGameStarted(); // Ensures all UI elements are updated accordingly
                        });
                        break;
                    case "IS_GAME_STARTED":
                        if (parts.length == 2) {
                            gameStarted = Boolean.parseBoolean(parts[1]);
                            if (gameStarted) {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(gameWindow, "The game is already in progress.",
                                            "Info", JOptionPane.INFORMATION_MESSAGE);
                                });
                            }
                        }
                        break;
                    case "IS_CURRENT_ACTIVE_PLAYER":
                        if (parts.length == 2) {
                            isCurrentActivePlayer = Boolean.parseBoolean(parts[1]);
                            SwingUtilities.invokeLater(() -> {
                                gameWindow.updateTurnStatus(isCurrentActivePlayer); // Make sure GameWindow handles this
                            });
                        }
                        break;
                    case "PLAYERS_CONNECTED":
                        if (parts.length > 1) {
                            int count = Integer.parseInt(parts[1]);
                            SwingUtilities.invokeLater(() -> {
                                gameWindow.updatePlayerCount(count);
                            });
                        }
                        break;
                    case "DISCONNECT":
                        handleDisconnection();
                        break;
                    case "TURN_CHANGED":
                        int currentPlayerNumber = Integer.parseInt(parts[1]);
                        handleTurnChange(currentPlayerNumber);
                        break;
                    case "SCORE_UPDATE":
                        int newScore = Integer.parseInt(parts[1]);
                        gameWindow.updateScore(newScore);
                        break;
                    case "CELL_UPDATE":
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        boolean isRevealed = Boolean.parseBoolean(parts[3]);
                        gameWindow.updateCellState(x, y, isRevealed);
                        break;
                    case "FLAG_UPDATE":
                        x = Integer.parseInt(parts[1]);
                        y = Integer.parseInt(parts[2]);
                        boolean isFlagged = Boolean.parseBoolean(parts[3]);
                        gameWindow.updateFlagState(x, y, isFlagged);
                        break;
                    default:
                        System.err.println("Received unknown command: " + parts[0]);
                        break;
                }
            }
        }
    }
    

    public void send(String message) {
        if (out != null) {
            System.out.println("Sending message to server: " + message);
            out.println(message);
        }
    }

    private void handleTurnChange(int currentPlayerNumber) {
        boolean isCurrentTurn = this.player.getPlayerNumber() == currentPlayerNumber;
        SwingUtilities.invokeLater(() -> gameWindow.handleTurnChange(currentPlayerNumber));
        this.isCurrentActivePlayer = isCurrentTurn;
    }

    public void incrementScore(int increment) {
        score += increment;
    }

    public void decrementScore(int decrement) {
        score -= decrement;
    }

    public int getScore() {
        return score;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }
}
