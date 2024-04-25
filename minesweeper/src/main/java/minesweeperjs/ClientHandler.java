package minesweeperjs;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;

    public ClientHandler(Socket socket, GameServer server, Player player) {
        this.clientSocket = socket;
        this.server = server;
        this.player = player;
        initializeStreams();
    }

    private void initializeStreams() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error initializing streams for " + player.getName() + ": " + e.getMessage());
            closeConnection();
        }
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (!interpretClientMessage(inputLine)) {
                    break; // Interpretation signaled to end the connection
                }
            }
        } catch (IOException e) {
            System.err.println("Error with client " + player.getName() + " connection: " + e.getMessage());
        } finally {
            server.handlePlayerQuit(player);
            closeConnection();
        }
    }

    private boolean interpretClientMessage(String inputLine) {
        String[] parts = inputLine.split(" ");
        if (parts.length == 0) {
            return false;
        }

        try {
            switch (parts[0]) {
                case "MOVE":
                    handleMoveCommand(parts);
                    break;
                case "FLAG":
                    handleFlagCommand(parts);
                    break;
                case "READY":
                    server.playerReady(player);
                    break;
                case "REQUEST_NEIGHBORING_MINES_COUNT":
                    handleRequestNeighboringMinesCount(parts);
                    break;
                default:
                    System.err.println("Received unknown command: " + parts[0]);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error handling command from client: " + e.getMessage());
        }

        return true;
    }

    private void handleMoveCommand(String[] parts) {
        if (!server.getGameBoard().isGameStarted()) {
            sendMessage("GAME_NOT_STARTED");
            return;
        }
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        if (server.isPlayerTurn(player)) {
            server.processPlayerMove(player, x, y);
        } else {
            sendMessage("NOT_YOUR_TURN");
        }
    }

    private void handleFlagCommand(String[] parts) {
        if (!server.getGameBoard().isGameStarted()) {
            sendMessage("GAME_NOT_STARTED");
            return;
        }
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        boolean isFlagged = parts[3].equals("1");
        if (server.isPlayerTurn(player)) {
            server.toggleFlag(x, y, isFlagged, player);
        } else {
            sendMessage("NOT_YOUR_TURN");
        }
    }

    private void handleRequestNeighboringMinesCount(String[] parts) {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int count = server.getGameBoard().getNeighboringMinesCount(x, y);
        sendMessage("NEIGHBORING_MINES_COUNT_RESPONSE " + x + " " + y + " " + count);
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection for " + player.getName() + ": " + e.getMessage());
        }
    }
}
