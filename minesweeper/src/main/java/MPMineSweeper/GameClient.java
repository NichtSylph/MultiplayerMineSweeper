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
public class GameClient extends Thread {
    private JFrame joinFrame;
    private JTextField ipTextField, portTextField, passwordTextField;
    private GameWindow gameWindow;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private Player player = new Player();
    private int score = 0;
    private boolean gameStarted = false;
    private boolean isCurrentActivePlayer;
    private JButton joinButton;
    private Boolean stopServerListener = false;

    public GameClient() {
        createJoinFrame();
    }

    public void getPlayerFromServer() {
        send("GETCURRENTPLAYER");
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
                processServerMessage(in.readLine());
    
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
        this.getPlayerFromServer();
        System.out.println("KKM: new window");
        this.gameWindow = new GameWindow(this);
        this.gameWindow.setVisible(true);
        joinFrame.dispose(); // Dispose the join frame after successful connection
    }

    public boolean isGameStarted() {
        return this.gameStarted;
    }

    public Integer getPlayerNumber() {
        return this.player.getPlayerNumber();
    }

    public void endTurn() {
        send("END_TURN");
        this.isCurrentActivePlayer = false;
    }

    public Boolean checkCurrentActivePlayer() {
        return isCurrentActivePlayer;
    }

    public void sendReady() {
        if (out != null) {
            out.println("READY");
            if (this.player != null) {
                this.player.setReady(true);
            }
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

    public Boolean isPlayerReady() {
        return this.player.isReady();
    }

    private Boolean processServerMessage(String inputLine) {
        // System.out.print("KKM: " + inputLine);
        if (inputLine != null) {
            String[] parts = inputLine.split(" ");

            // System.out.print("KKM parts: " + parts);
            // System.out.print("KKM parts[0]: " + parts[0]);
            if (parts.length > 0) {
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
                    case "GETCURRENTPLAYER":
                        if (parts.length == 4) {
                            this.player.setReady(Boolean.valueOf(parts[1]));
                            this.player.setPlayerNumber(Integer.valueOf(parts[2]));
                            this.player.setPassword(parts[3]);
                        }
                        break;
                    case "GAME_STARTED":
                        this.gameStarted = true;
                        SwingUtilities.invokeLater(() -> {
                            this.gameWindow.notifyGameStarted();
                            this.gameWindow.getReadyButton().setEnabled(false); // Disable the Ready button when the game starts
                        });
                        break;
                    case "ACTIVE_STATUS_UPDATE":
                        if (parts.length == 2) {
                            // if (this.isCurrentActivePlayer && !Boolean.valueOf(parts[1])) {
                            //     this.gameWindow.updateTurnStatus(false);
                            // }
                            this.isCurrentActivePlayer = Boolean.valueOf(parts[1]);
                            if (this.isCurrentActivePlayer) {
                                this.gameWindow.updateTurnStatus(true);
                            }
                        } 
                        break;
                    case "UPDATE_PLAYER_COUNT":
                        if (parts.length == 2) {
                            SwingUtilities.invokeLater(() -> {
                                System.out.println("KKM Gamewindow: " + this.gameWindow);
                                this.gameWindow.updatePlayerCount(Integer.valueOf(parts[1]));
                            });
                        }
                        break;
                    default:
                        System.err.println("Received unknown command: " + parts[0]);
                        break;
                }
            }
            
            if (!this.stopServerListener) {
                try {
                    processServerMessage(in.readLine());
                } catch (IOException e) {
                    System.err.println("Error handling command from client: " + e.getMessage());
                }
            }
        }
        return true;
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
