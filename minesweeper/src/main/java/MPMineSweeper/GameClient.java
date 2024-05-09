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
        joinButton.setEnabled(false); // Disable the join button to prevent multiple clicks
    
        new Thread(() -> {
            try {
                String serverIP = ipTextField.getText().trim();
                int serverPort = Integer.parseInt(portTextField.getText().trim());
                String password = passwordTextField.getText().trim();
    
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIP, serverPort), 5000); // 5000 ms timeout
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
                // Send password to server and flush to ensure it's sent immediately
                out.println(password);
                out.flush();
    
                // Read response from server
                String response = in.readLine();
    
                SwingUtilities.invokeLater(() -> {
                    if ("Password incorrect".equals(response)) {
                        JOptionPane.showMessageDialog(joinFrame, "Password incorrect. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    } else if ("Password correct".equals(response)) {
                        getPlayerNumber();
                        openGameWindow(); // Opens the game window if the password is correct
                    } else {
                        JOptionPane.showMessageDialog(joinFrame, "Unexpected response from server.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    joinButton.setEnabled(true); // Re-enable the join button irrespective of the server response
                });
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

    public boolean isGameStarted() {
        send("IS_GAME_STARTED");
        
        try {
            processServerMessage(in.readLine());
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
            out.println("MOVE " + x + " " + y);
        }
    }

    public void sendFlagChange(int x, int y, boolean isFlagged) {
        if (out != null) {
            out.println("FLAG " + x + " " + y + " " + isFlagged);
        }
    }

    private void processServerMessage(String inputLine) {
        if (inputLine != null) {
            String[] parts = inputLine.split(" ");
            if (parts.length > 0) {
                switch (parts[0]) {
                    case "CURRENT_PLAYER_NUMBER":
                        if (parts.length == 2) {
                            Player currentPlayer = getCurrentPlayer();
                            if (currentPlayer != null) {
                                currentPlayer.setPlayerNumber(Integer.valueOf(parts[1]));
                            }
                        }
                        break;
                    case "IS_GAME_STARTED":
                        if (parts.length == 1) {
                            gameStarted = Boolean.valueOf(parts[1]);
                        }
                        break;
                    case "IS_CURRENT_ACTIVE_PLAYER":
                        if (parts.length == 1) {
                            isCurrentActivePlayer = Boolean.valueOf(parts[1]);
                        }
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
            out.println(message);
        }
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
