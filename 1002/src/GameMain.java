import java.util.Scanner;

public class GameMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // Step 1: Select game mode
        System.out.println("===== Einstein würfelt nicht! =====");
        System.out.println("Select a game mode:");
        System.out.println("1. Human Player");
        System.out.println("2. Random Player");
        System.out.println("3. AI Player");
        System.out.print("Input mode number (1/2/3):");
        int mode = scanner.nextInt();
        scanner.nextLine();

        // Step 2: Create a player
        Player player = null;
        int targetPiece = 0;
        switch (mode) {
            case 1:
                System.out.print("Enter your name:");
                String userName = scanner.nextLine();
                player = new HumanPlayer(userName);
                break;
            case 2:
                player = new RandomPlayer();
                System.out.println("Random player selected");
                break;
            case 3:
                player = new AIPlayer();
                System.out.println("AI player selected");
                break;
            default:
                System.err.println("Invalid mode! The program exits.");
                return;
        }

        // Step 3: Select level
        System.out.println("\nSelect a level:");
        System.out.println("1. level1");
        System.out.println("2. level2");
        System.out.println("3. level3");
        System.out.println("4. level4");
        System.out.print("Enter level number (1/4):");
        int level = scanner.nextInt();
        String levelFileName = "level" + level + ".txt";

        // Step 4: Load game data
        GameLoader gameLoader = new GameLoader(levelFileName);
        targetPiece = gameLoader.getTargetPiece();
        int[] initialPositions = gameLoader.getInitialPositions();
        int[] diceSequence = gameLoader.getDiceSequence();

        // Set key parameters for AI players
        if (player instanceof AIPlayer) {
            ((AIPlayer) player).setTargetPiece(targetPiece);
            ((AIPlayer) player).setDiceSequence(diceSequence);
            // Set maximum allowed steps by level
            int maxMoves = switch (level) {
                case 1 -> 6;
                case 2, 3 -> 10;
                case 4 -> 15;
                default -> 30;
            };
            ((AIPlayer) player).setMaxAllowedMoves(maxMoves);
        }

        // Print game information to moves.txt
        gameLoader.printGameDetails(player.getName());

        // Step 5: Initialize game state
        GameState gameState = new GameState(initialPositions, targetPiece);
        // Only AI players need to reset the step count and dice index
        if (player instanceof AIPlayer) {
            ((AIPlayer) player).resetMoveCount();
        }
        boolean isWin = false;
        int moveCount = 0;
        final int MAX_GLOBAL_MOVES = 30; // Maximum global step count

        // Step 6: main game loop
        System.out.println("\n===== Game Start =====");
        while (moveCount < MAX_GLOBAL_MOVES) {
            // Check if you won
            if (gameState.isWinning()) {
                isWin = true;
                break;
            }
            // Check if the dice sequence is exhausted
            if (moveCount >= diceSequence.length) {
                System.out.println("Dice sequence is exhausted!");
                break;
            }

            // Set the number of dice for the current round
            int currentDice = diceSequence[moveCount];
            gameState.setCurrentDice(currentDice);

            // Update the current AI dice index
            if (player instanceof AIPlayer) {
                ((AIPlayer) player).setCurrentDiceIndex(moveCount);
            }

            // Player chooses to move
            Move move = player.chooseMove(gameState);
            if (move == null) {
                System.out.println("No legal move! The game is over.");
                break;
            }

            // Move and print
            gameState.executeMove(move);
            player.printMove(gameState.getCurrentPositions());
            moveCount++;

            System.out.println("Completed " + moveCount + " steps（Up to " +
                    (player instanceof AIPlayer ? ((AIPlayer) player).getMaxAllowedMoves() : MAX_GLOBAL_MOVES) + " steps）");
        }

        // Step 7: Display game results
        System.out.println("\n===== Game Over =====");
        if (isWin) {
            System.out.println("Congratulations! You won! The target piece has reached position 0!");
        } else {
            System.out.println("Unfortunately, the target piece could not reach position 0 within the allowed moves. The game failed.");
        }
        scanner.close();
    }
}