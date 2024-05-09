package minesweeperjs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameServer {
    private int port;
    private List<ClientHandler> clientHandlers;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private boolean gameStarted;
    private GameBoard gameBoard;
    private AtomicInteger currentPlayerIndex;
    private List<Player> players;
    private Player currentPlayer;
    private AtomicInteger readyPlayers;
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final int MINES = 32;

    public GameServer(int port) {
        this.port = port;
        clientHandlers = new ArrayList<>();
        players = new ArrayList<>();
        gameBoard = new GameBoard(WIDTH, HEIGHT, MINES, players.toArray(new Player[0]));
        currentPlayerIndex = new AtomicInteger(0);
        readyPlayers = new AtomicInteger(0);
        gameStarted = false;
        isRunning = true;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);

            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        addPlayer(clientSocket);
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.out.println("Error accepting client connection: " + e.getMessage());
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void stopServer() {
        isRunning = false;
        clientHandlers.forEach(ClientHandler::closeConnection);
        closeServerSocket();
        System.exit(0);
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }

    public synchronized void addPlayer(Socket clientSocket) {
        Player newPlayer = new Player("Player " + (players.size() + 1));
        players.add(newPlayer); 
        if (currentPlayer == null) {
            setCurrentPlayer(newPlayer);
        }
        ClientHandler clientHandler = new ClientHandler(clientSocket, this, newPlayer);
        clientHandlers.add(clientHandler);
        new Thread(clientHandler).start();
    
        newPlayer.setPlayerNumber(players.size() - 1);
        clientHandler.sendMessage("PLAYER_NUMBER " + newPlayer.getPlayerNumber());
        clientHandler.sendMessage("WELCOME Welcome to Lobby, you are Player Number " + newPlayer.getPlayerNumber());
        broadcastPlayerCount();
    }

    public void setCurrentPlayer(Player player) {
        currentPlayer = player;
    }

    private void broadcastPlayerCount() {
        broadcastMessage("PLAYERS_CONNECTED " + players.size());
    }

    public synchronized void playerReady(Player player) {
        if (!gameStarted && readyPlayers.incrementAndGet() == players.size()) {
            startGame();
        } else if (!gameStarted && players.size() > 1) {
            sendToPlayer(player, "WAITING_FOR_PLAYERS");
        }
    }

    private void startGame() {
        if (!gameStarted) {
            gameStarted = true;
            broadcastMessage("GAME_STARTED");
            gameBoard.startGame();
            currentPlayerIndex.set(0);
            broadcastTurnChange();
        }
    }

    private void broadcastTurnChange() {
        broadcastMessage("TURN_CHANGED " + currentPlayerIndex.get());
    }

    private void broadcastMessage(String message) {
        clientHandlers.forEach(handler -> handler.sendMessage(message));
    }

    public synchronized void processPlayerMove(Player player, int x, int y) {
        if (!gameStarted) {
            sendToPlayer(player, "GAME_NOT_STARTED");
            return;
        }
    
        if (!isPlayerTurn(player)) {
            sendToPlayer(player, "NOT_YOUR_TURN");
            return;
        }
    
        if (gameBoard.revealCell(x, y, player)) {
            checkGameOver();
        }
       
        sendToPlayer(player, "MOVE_RESULT " + x + " " + y + " " + (gameBoard.getCell(x, y).isRevealed() ? "1" : "0"));
        broadcastUpdatedGameState();
    }   

    public Player getNextPlayer(Player currentPlayer) {
    //    players
      Player p = null;
      Integer playerCounts = players.size();

      Integer nextPlayerIndex = null;

      for (Player player : this.players) {
        if (player == currentPlayer) {
            Integer index = this.players.indexOf(player);
            if (index < (playerCounts - 1)) {
                nextPlayerIndex = index + 1;
            } else {
                nextPlayerIndex = 0;
            }
        }
      }

      if (nextPlayerIndex != null) {
        p = players.get(nextPlayerIndex);
      }

      return p;
    }

    private void checkGameOver() {
        if (gameBoard.getBombRevealedCount() >= MINES) {
            broadcastMessage("GAMEOVER");
            stopServer();
        } else {
            switchTurns();
        }
    }

    private void switchTurns() {
        currentPlayerIndex.incrementAndGet();
        currentPlayerIndex.set(currentPlayerIndex.get() % players.size());
        broadcastTurnChange();
    }

    private void sendToPlayer(Player player, String message) {
        clientHandlers.stream()
                .filter(h -> h.getPlayer().equals(player))
                .findFirst()
                .ifPresent(h -> h.sendMessage(message));
    }

    private void broadcastUpdatedGameState() {
        String state = createGameStateMessage();
        broadcastMessage("GAME_STATE_UPDATE " + state);
    }

    private String createGameStateMessage() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Cell cell = gameBoard.getCell(x, y);
                int state = cell.isRevealed() ? (cell.isMine() ? 2 : 1) : (cell.isFlagged() ? 3 : 0);
                sb.append(x).append(",").append(y).append(",").append(state).append(";");
            }
        }
        return sb.toString();
    }

    public void handlePlayerQuit(Player player) {
        players.remove(player);
        clientHandlers.removeIf(handler -> handler.getPlayer().equals(player));
        broadcastPlayerCount();
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public void toggleFlag(int x, int y, boolean isFlagged, Player player) {
        if (gameBoard.toggleFlag(x, y, isFlagged, player)) {
            checkGameOver();
        }
        broadcastUpdatedGameState();
    }

    public boolean isPlayerTurn(Player player) {
        return player == currentPlayer;
    }

    public static void main(String[] args) {
        String portString = JOptionPane.showInputDialog(null, "Enter the port to start Server:");
        if (portString == null) {
            System.out.println("No port entered. Exiting...");
            System.exit(0);
        }
        int port = Integer.parseInt(portString);
    
        GameServer server = new GameServer(port);
        server.startServer();
    
        String serverIP;
        try {
            serverIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIP = "Unknown";
        }
    
        JFrame frame = new JFrame("Status");
        JLabel label = new JLabel("Server running on IP: " + serverIP + " Port: " + port);
        label.setFont(new Font("Arial",Font.PLAIN ,14));
    
        JButton button = new JButton("Close Connection");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                server.stopServer();
                frame.dispose();
            }
        });
    
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.CENTER);
        panel.add(button, BorderLayout.SOUTH);
    
        frame.getContentPane().add(panel);
        frame.setSize(340, 150);
        frame.setVisible(true);
    }
}