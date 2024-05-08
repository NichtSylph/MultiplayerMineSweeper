package MPMineSweeper;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;
    private String verifiedPassword;

    public ClientHandler(Socket socket, GameServer server, Player newPlayer) {
        this.clientSocket = socket;
        this.server = server;
        this.verifiedPassword = newPlayer.getPassword();
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            verifyPassword();
        } catch (IOException e) {
            System.err.println("Error initializing streams: " + e.getMessage());
            closeConnection();  // Close connection if streams can't be initialized or password is incorrect
        }
    }

    private void verifyPassword() throws IOException {
        String receivedPassword = in.readLine();
        if (this.verifiedPassword.equals(receivedPassword)) {
            player = new Player();
            player.setPlayerNumber(server.getPlayers().size() + 1); // Set player number
            server.addPlayer(clientSocket, player); // Add player if password matches
        } else {
            out.println("Incorrect password");
            closeConnection(); // Close the connection if the password is incorrect
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (!interpretClientMessage(inputLine)) {
                    break; // Exit if message handling signals to stop
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            server.handlePlayerQuit(player);
            closeConnection();
        }
    }

    private boolean interpretClientMessage(String inputLine) {
        String[] parts = inputLine.split(" ");
        if (parts.length == 0) return false; // Handle empty messages

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

    public void sendMessage(String message) {
        out.println(message);
    }

    public void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection for Player " + player.getPlayerNumber() + ": " + e.getMessage());
        }
    }
}
