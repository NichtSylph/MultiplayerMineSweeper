CSC 445 Project 3 Documentation

Consensus Protocol Documentation for Multiplayer Minesweeper Game

The consensus protocol for the multiplayer Minesweeper game ensures fair and secure gameplay among all participants, accommodating a minimum of one player and a maximum of four. It manages player interactions, turn order, player joins/quits, server shutdowns, and implements room security through key exchange.

Key Components:

1. Player Management:

- Players cannot join the game once it has started to maintain fairness.
- The server manages player connections and disconnections.
- If a player quits, they cannot rejoin the ongoing game session.

1. Turn-Based System:

- Gameplay follows a sequential turn-based system.
- Turn order is determined by the order in which players joined the server.
- The player who joined the server first gets priority for the first turn.

1. Key Exchange and Security:

- To ensure game room security, the server assigns a specific password.
- Players must know this password to join the game room.
- Upon joining, the server performs a key exchange with the player, establishing a secure connection.
- This security measure prevents unauthorized access and interference from external parties.

1. Consensus on Cell Reveals:

- Players wait for the current player to finish their turn before initiating their own.
- Consensus is implicit in the turn-based nature of the game.
- No specific cooldown period is enforced.

1. Error Handling:

- Players are notified if another player quits during the game.
- If the server is closed, all players are gracefully notified, and they can choose to quit the game.

Protocol Workflow:

1. Initialization:

- Players join the game before it starts.
- The Minesweeper grid is generated.
- Turn order is established based on the order of player connections.

1. Turn Execution:

- Players take turns sequentially based on their order of joining.
- Each player completes their turn before the next player can start theirs.
- There's no cooldown between turns; players wait for the current turn to finish.

1. Player Quits:

- If a player quits, all remaining players are notified.
- The quitted player cannot rejoin the ongoing game session.

1. Game Continuation:

- The game continues until a winning condition is met (e.g., all safe cells are revealed) or a losing condition occurs (e.g., a mine is revealed).
- If a player quits mid-game, the game continues without disruption, with remaining players continuing their turns.

1. Graceful Shutdown:

- If the server is closed, all players are informed.
- Players can choose to quit the game gracefully.

The consensus protocol for the multiplayer Minesweeper game ensures smooth and fair gameplay by managing player interactions, enforcing turn-based actions, and handling unexpected events such as player quits. By managing player interactions, turn order, player joins/quits, server shutdowns, and implementing room security through key exchange, the protocol enhances the overall gaming experience for all participants while safeguarding against unauthorized access and interference.