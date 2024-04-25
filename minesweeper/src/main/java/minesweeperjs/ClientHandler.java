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
            System.err.println("Error initializing streams: " + e.getMessage());
            closeConnection(); // Close connection if streams can't be initialized
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
                    break; // Break loop if interpretation signals to stop
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
            server.handlePlayerQuit(player);
        } finally {
            server.removeClientHandler(this);
            closeConnection();
        }
    }

    private boolean interpretClientMessage(String inputLine) {
        String[] parts = inputLine.split(" ");
        if (parts.length == 0) {
            return false; // Empty message, terminate connection
        }

        try {
            switch (parts[0]) {
                case "MOVE":
                    handleMoveCommand(parts);
                    break;
                case "FLAG":
                    handleFlagCommand(parts);
                    break;
                case "REQUEST_CELL_STATE":
                    handleRequestCellStateCommand(parts);
                    break;
                case "READY":
                    server.playerReady(player);
                    break;
                case "REQUEST_NEIGHBORING_MINES_COUNT":
                    handleRequestNeighboringMinesCount(parts);
                    break;
                default:
                    System.err.println("Unknown command from client: " + parts[0]);
                    break;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numeric values from client message: " + e.getMessage());
        }

        return true;
    }

    private void handleMoveCommand(String[] parts) {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        if (!server.isPlayerTurn(this.player)) {
            sendMessage("NOT_YOUR_TURN");
            return;
        }
        server.processPlayerMove(this.player, x, y);
    }

    private void handleFlagCommand(String[] parts) {
        if (parts.length == 4) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            boolean isFlagged = parts[3].equals("1");
            server.toggleFlag(x, y, isFlagged, player);
        }
    }

    private void handleRequestCellStateCommand(String[] parts) {
        if (parts.length == 3) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            server.requestCellState(this, x, y);
        }
    }

    private void handleRequestNeighboringMinesCount(String[] parts) {
        if (parts.length == 3) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            server.handleNeighboringMinesCountRequest(this, x, y);
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void notifyPlayerQuit(String playerName) {
        sendMessage("PLAYER_QUIT " + playerName);
    }

    public void notifyServerClosing() {
        sendMessage("SERVER_CLOSING");
    }

    public void closeConnection() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (clientSocket != null && !clientSocket.isClosed())
                clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }
}
