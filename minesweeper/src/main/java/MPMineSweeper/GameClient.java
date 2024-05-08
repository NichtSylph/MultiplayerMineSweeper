package MPMineSweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameClient {
    private JFrame joinFrame;
    private JTextField ipTextField, portTextField, passwordTextField;
    private GameWindow gameWindow;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private List<Player> players = new ArrayList<>();

    public GameClient() {
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
        new Thread(() -> {
            try {
                String serverIP = ipTextField.getText().trim();
                int serverPort = Integer.parseInt(portTextField.getText().trim());
                String password = passwordTextField.getText().trim();
    
                socket = new Socket(serverIP, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
                // Send password to server and flush to ensure it's sent immediately
                out.println(password);
                out.flush();

                // Read response from server
                String response = in.readLine();
                System.out.println("Server response: " + response); // Debugging statement
    
                if ("Password incorrect".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(joinFrame, "Password incorrect.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                        try {
                            socket.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    });
                } else if ("Password correct".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        gameWindow = new GameWindow(this);
                        gameWindow.setVisible(true);
                        joinFrame.dispose();
                    });
                    listenForServerMessages();
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(joinFrame, "Unexpected response from server.", "Error", JOptionPane.ERROR_MESSAGE);
                        try {
                            socket.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    });
                }
            } catch (NumberFormatException | IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(joinFrame, "Error connecting to the server: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
       
    
    private void listenForServerMessages() {
        new Thread(() -> {
            try {
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    processServerMessage(fromServer);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(gameWindow, "Lost connection to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }).start();
    }

    public Player getCurrentPlayer() {
        for (Player player : players) {
            if (player.isCurrentTurn()) {
                return player;
            }
        }
        return null; // return null if no player's turn is currently true, handle this case appropriately in your code
    }

    public boolean isGameStarted() {
        for (Player player : players) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
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
    
    private void processServerMessage(String message) {
        // Handle messages from the server here
        System.out.println("Server says: " + message);
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }
}
