import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

public class HumanPlayer extends Player {
    private Scanner scanner; // Read console input

    // Constructor: Initialize player name and input utility
    public HumanPlayer(String name) {
        super(name);
        scanner = new Scanner(System.in);
    }

    // Select move: Prompt user for input
    @Override
    public Move chooseMove(GameState gameState) {
        List<Move> possibleMoves = gameState.generatePossibleMoves();

        // List available pieces for move (remove duplicates)
        System.out.println("\nCurrent dice roll:" + gameState.getCurrentDice());
        System.out.print("Available pieces:");
        Set<Integer> movablePieceSet = new HashSet<>();
        for (Move move : possibleMoves) {
            movablePieceSet.add(move.getPieceNum());
        }
        for (int piece : movablePieceSet) {
            System.out.print(piece + " ");
        }
        System.out.println();

        // Prompt current piece positions
        int[] currentPos = gameState.getCurrentPositions();
        System.out.println("Current piece positions (IDs 1-6):");
        for (int i = 0; i < 6; i++) {
            System.out.println("Piece" + (i + 1) + "：" + currentPos[i]);
        }

        // Input piece number for movement
        int pieceNum;
        while (true) {
            System.out.print("Input the piece number to move (1-6):");
            pieceNum = scanner.nextInt();
            // Check if the piece is movable
            boolean isValid = false;
            for (int piece : movablePieceSet) {
                if (piece == pieceNum) {
                    isValid = true;
                    break;
                }
            }
            if (isValid) {
                break;
            }
            System.out.println("Invalid piece! Please select a movable piece.");
        }

        // Print all possible target positions for this piece
        List<Integer> validToPositions = new ArrayList<>();
        for (Move move : possibleMoves) {
            if (move.getPieceNum() == pieceNum) {
                validToPositions.add(move.getToPos());
            }
        }
        System.out.print("Possible destinations for this piece:");
        for (int pos : validToPositions) {
            System.out.print(pos + " ");
        }
        System.out.println();

        // Enter the target position
        int toPos;
        while (true) {
            System.out.print("Please enter the target position (choose from the options above):");
            toPos = scanner.nextInt();
            // Check if the target position is in the legal list
            if (validToPositions.contains(toPos)) {
                break;
            }
            System.out.println("Invalid position! Please choose from the available options above.");
        }

        // Return the player's chosen move
        return new Move(pieceNum, currentPos[pieceNum - 1], toPos);
    }
}
