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
<<<<<<< HEAD
=======
    private boolean isCurrentActivePlayer;
    private JButton joinButton;
    private Boolean stopServerListener = false;
>>>>>>> 7b3d4b5 (working)

    /**
     * Constructor for GameClient. Sets up the GUI for joining the game lobby.
     */
    public GameClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.joinFrame = new JFrame(); // initialize joinFrame
        connectToServer();
    }

<<<<<<< HEAD
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
=======
    public void getPlayerFromServer() {
        send("GETCURRENTPLAYER");
>>>>>>> 7b3d4b5 (working)
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(String message) {
        out.println(message);
    }

<<<<<<< HEAD
    /**
     * Sends a signal to the server to start the game.
     */
    public void sendStartGame() {
        sendMessage("START_GAME");
=======
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
        this.gameWindow = new GameWindow(this);
        this.gameWindow.setVisible(true);
        joinFrame.dispose(); // Dispose the join frame after successful connection
    }

    public boolean isGameStarted() {
        if (!gameStarted) {
            send("IS_GAME_STARTED");
        }

        return gameStarted;
    }

    public Integer getPlayerNumber() {
        return this.player.getPlayerNumber();
    }

    public void endTurn() {
        send("END_TURN");
    }

    public Boolean checkCurrentActivePlayer() {
        return isCurrentActivePlayer;
    }

    public void sendReady() {
        if (out != null) {
            out.println("READY");
            if (this.player != null) {
                this.player.isReady();
            }
        }
>>>>>>> 7b3d4b5 (working)
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

<<<<<<< HEAD
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
=======
            System.out.print("KKM parts: " + parts);
            System.out.print("KKM parts[0]: " + parts[0]);
            if (parts.length > 0) {
                switch (parts[0]) {
                    case "PASSWORD":
                        if (parts.length == 2) {
                            SwingUtilities.invokeLater(() -> {
                                System.out.print("KKM parts[1]: " + parts[1]);
                                System.out.print("KKM parts[1]: INVOKELATER");
                                if (parts[1].equalsIgnoreCase("CORRECT")) {
                                    System.out.print("KKM parts[1]: CORRECT <<<<<<");
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

                            System.out.println("KKM: currentplayer: " + this.player.toString());
                        }
                        break;
                    case "GAME_STARTED":
                        this.gameStarted = true;
                        SwingUtilities.invokeLater(() -> {
                            this.gameWindow.notifyGameStarted();
                            this.gameWindow.getReadyButton().setEnabled(false); // Disable the Ready button when the game starts
                        });

                        // if (!this.gameStarted) {
                        //     SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(gameWindow,
                        //         "Waiting for other players to be ready.", "Wait",
                        //         JOptionPane.INFORMATION_MESSAGE));
                        // } else {
                        //     SwingUtilities.invokeLater(() -> {
                        //         gameWindow.notifyGameStarted();
                        //         gameWindow.getReadyButton().setEnabled(false); // Disable the Ready button when the game starts
                        //     });
                        // }
                        break;
                    case "IS_GAME_STARTED":
                        System.out.println("KKM in game started: <<<<<");
                        if (parts.length == 2) {
                            this.gameStarted = Boolean.valueOf(parts[1]);
                            System.out.println("KKM in game started: " + String.valueOf(gameStarted));
                        }
                        break;
                    case "ACTIVE_STATUS_UPDATE":
                        if (parts.length == 2) {
                            this.isCurrentActivePlayer = Boolean.valueOf(parts[1]);
                        } 
                        break;
                    default:
                        System.err.println("Received unknown command: " + parts[0]);
                        break;
>>>>>>> 7b3d4b5 (working)
                }
            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
            } finally {
                closeConnection();
            }
            
            if (!this.stopServerListener) {
                try {
                    processServerMessage(in.readLine());
                } catch (IOException e) {
                    System.err.println("Error handling command from client: " + e.getMessage());
                }
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
