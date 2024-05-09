package MPMineSweeper;

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
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error initializing streams: " + e.getMessage());
            closeConnection();
        }
        System.out.println("ClientHandler created for player number: " + player.getPlayerNumber()); // Debugging line
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (!interpretClientMessage(inputLine)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            server.handlePlayerQuit(player);
            server.broadcastPlayerCount(); // Broadcast player count when a player quits
            server.broadcastMessage("Player " + player.getPlayerNumber() + " has quit the game.");
            closeConnection();
        }
    }

    private boolean interpretClientMessage(String inputLine) {
        System.out.println("Received message from client: " + inputLine); // Debugging line
        String[] parts = inputLine.split(" ");
        if (parts.length == 0)
            return false; // Handle empty messages

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
                case "UPDATE_PLAYER_COUNT":
                    handleUpdatePlayerCount();
                    break;
                case "GET_CURRENT_PLAYER_NUMBER":
                    sendMessage("CURRENT_PLAYER_NUMBER " + String.valueOf(player.getPlayerNumber()));
                    break;
                case "IS_GAME_STARTED":
                    sendMessage("IS_GAME_STARTED " + String.valueOf(server.getGameStarted()));
                    break;
                case "GAME_STARTED":
                    sendMessage("GAME_STARTED " + String.valueOf(server.getGameStarted()));
                    break;
                case "IS_CURRENT_ACTIVE_PLAYER":
                    sendMessage("IS_CURRENT_ACTIVE_PLAYER " + String.valueOf(server.isPlayerTurn(player)));
                    break;
                case "END_TURN":
                    server.switchTurns();
                    break;
                case "REQUEST_NEIGHBORING_MINES_COUNT":
                    handleRequestNeighboringMinesCount(parts);
                    break;
                case "GAMEOVER":
                    handleGameOverCommand(parts);
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
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        server.processPlayerMove(player, x, y);
    }

    private void handleFlagCommand(String[] parts) {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        boolean isFlagged = parts[3].equals("1");
        server.toggleFlag(x, y, isFlagged, player);
    }

    private void handleRequestNeighboringMinesCount(String[] parts) {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int count = server.getGameBoard().getNeighboringMinesCount(x, y);
        sendMessage("NEIGHBORING_MINES_COUNT_RESPONSE " + x + " " + y + " " + count);
    }

    private void handleUpdatePlayerCount() {
        int playerCount = server.getCurrentPlayerCount();
        sendMessage("UPDATE_PLAYER_COUNT " + playerCount);
    }

    private void handleGameOverCommand(String[] parts) {
        boolean isWinner = parts[1].equals("1");
        server.handleGameOver(player, isWinner);
    }

    public void sendMessage(String message) {
        out.println(message);
        System.out.println("Sent message to client: " + message); // Debugging line
    }

    public Player getPlayer() {
        return player;
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
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
