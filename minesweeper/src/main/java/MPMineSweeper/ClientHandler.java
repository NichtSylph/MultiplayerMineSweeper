package MPMineSweeper;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;
    private String encryptionKey;

    /**
     * Constructs a ClientHandler for managing client-server communication.
     *
     * @param socket The socket through which the client is connected.
     * @param server The game server instance.
     * @param player The player associated with this client.
     */
    public ClientHandler(Socket socket, GameServer server, Player player, String encryptionKey) {
        this.clientSocket = socket;
        this.server = server;
        this.player = player;
        this.encryptionKey = encryptionKey;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error initializing streams: " + e.getMessage());
        }
    }

    /**
     * Returns the player associated with this client handler.
     *
     * @return The player object.
     */
    public Player getPlayer() {
        return player;
    }

    public void updatePlayerScore(Integer score) {
        this.player.setScore(score);
        sendMessage("SCORE " + score);
    }

    /**
     * The main run method of the runnable. Listens for messages from the client and
     * processes them.
     */
    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                boolean shouldContinue = interpretClientMessage(inputLine);
                if (!shouldContinue) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        } finally {
            server.handlePlayerQuit(player);
            closeConnection();
        }
    }

    /**
     * Interprets the message received from the client.
     *
     * @param inputLine The message received from the client.
     * @return true if the connection should continue, false otherwise.
     */
    private boolean interpretClientMessage(String inputLine) {
        String decryptedString = EncryptionUtil.decrypt(inputLine, this.encryptionKey);

        System.out.println("KKM: decryptedString: " + decryptedString);

        String[] parts = decryptedString.split(" ");
        // String[] parts = inputLine.split(" ");
        if (parts.length == 0) {
            return false; // Empty message, terminate connection
        }
    
        String command = parts[0];
        switch (command) {
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
            case "PLAYER_QUIT":
                handlePlayerQuitCommand(parts);
                break;
            default:
                System.err.println("Unknown command from client: " + command);
                break;
        }
        return true;
    }

  private void handlePlayerQuitCommand(String[] parts) {
    server.handlePlayerQuit(player);
}

    
    /**
     * Handles the 'MOVE' command from the client.
     *
     * @param parts The parts of the message, split by spaces.
     */
    private void handleMoveCommand(String[] parts) {
        if (parts.length == 4) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            server.processPlayerMove(player, x, y);
        }
    }

    /**
     * Handles the 'FLAG' command from the client.
     *
     * @param parts The parts of the message, split by spaces.
     */
    private void handleFlagCommand(String[] parts) {
        if (parts.length == 4) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            boolean isFlagged = parts[3].equals("1");
            server.toggleFlag(x, y, isFlagged, player);
        }
    }

    /**
     * Handles the 'REQUEST_CELL_STATE' command from the client.
     *
     * @param parts The parts of the message, split by spaces.
     */
    private void handleRequestCellStateCommand(String[] parts) {
        if (parts.length == 3) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            server.sendCellState(this, x, y);
        }
    }

    /**
     * Sends the player number to the client.
     */
    public void sendPlayerNumber() {
        sendMessage("PLAYER_NUMBER " + player.getPlayerNumber());
    }

    /**
     * Sends a message to the client.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(String message) {
        String encryptedMessage = EncryptionUtil.encrypt(message, this.encryptionKey);
        if (out != null) {
            out.println(encryptedMessage);
        }
    }

    /**
     * Closes the connection with the client.
     */
    public void closeConnection() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }
}
