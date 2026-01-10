import java.util.ArrayList;
import java.util.List;

public class GameState {
    private int[] currentPositions; // Current positions of the 6 pieces (-1 indicates captured)
    private int targetPiece;        //Target piece
    private int currentDice;        //Number of dice in the current turn

    // Constructor: Initialize game state
    public GameState(int[] initialPositions, int targetPiece) {
        // Copy the initial position (to avoid modifying the original array directly)
        this.currentPositions = new int[6];
        System.arraycopy(initialPositions, 0, this.currentPositions, 0, 6);
        this.targetPiece = targetPiece;
    }

    // Set the number of dice for the current turn
    public void setCurrentDice(int currentDice) {
        this.currentDice = currentDice;
    }

    //Added: Method to get the current number of dice (fixes error when HumanPlayer is called)
    public int getCurrentDice() {
        return currentDice;
    }

    // Generate all legal moves (core method)
    public List<Move> generatePossibleMoves() {
        List<Move> possibleMoves = new ArrayList<>();

        // Step 1: Identify the currently movable pieces (according to the rules of the problem)
        List<Integer> movablePieces = findMovablePieces();

        // Step 2: For each movable piece, generate all legal target positions
        for (int pieceNum : movablePieces) {
            int pieceIndex = pieceNum - 1; // Piece 1 corresponds to index 0, piece 2 corresponds to index 1...
            int currentPos = currentPositions[pieceIndex];

            // Skip captured pieces (position is -1)
            if (currentPos == -1) {
                continue;
            }

            // Step 3: Generate 8 adjacent positions (up, down, left, right + diagonals, similar to a chess king)
            int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1}; // Row Change (Tens Digit)
            int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1}; // Column change (units digit)

            // Calculate the tens digit (row) and units digit (column) of the current position
            int currentRow = currentPos / 10;
            int currentCol = currentPos % 10;

            // Traverse in 8 directions
            for (int i = 0; i < 8; i++) {
                int newRow = currentRow + dx[i];
                int newCol = currentCol + dy[i];

                // Check if the new position is valid: both the row and column are between 0 and 9, and it is not 22
                if (newRow >= 0 && newRow <= 9 && newCol >= 0 && newCol <= 9) {
                    int newPos = newRow * 10 + newCol;
                    if (newPos != 22) { // Position 22 has been removed and cannot be accessed.
                        possibleMoves.add(new Move(pieceNum, currentPos, newPos));
                    }
                }
            }
        }
        return possibleMoves;
    }

    //Find the currently movable pieces (according to the game rules)
    private List<Integer> findMovablePieces() {
        List<Integer> movablePieces = new ArrayList<>();
        List<Integer> existingPieces = new ArrayList<>();

        // Step 1: Collect the existing chess pieces (those whose positions are not -1)
        for (int i = 0; i < 6; i++) {
            if (currentPositions[i] != -1) {
                existingPieces.add(i + 1); //The chess piece is numbered i+1 (1-6)
            }
        }

        // Step 2: Determine movable pieces according to the rules
        if (existingPieces.contains(currentDice)) {
            // Rule 2: You can only move a piece that matches the number on the dice.
            movablePieces.add(currentDice);
        } else {
            // Rule 3: Find the smallest piece larger than the dice + the largest piece smaller than the dice
            Integer minBigger = null;
            Integer maxSmaller = null;

            for (int piece : existingPieces) {
                if (piece > currentDice) {
                    if (minBigger == null || piece < minBigger) {
                        minBigger = piece;
                    }
                } else if (piece < currentDice) {
                    if (maxSmaller == null || piece > maxSmaller) {
                        maxSmaller = piece;
                    }
                }
            }

            // If available, add to the removable list
            if (minBigger != null) {
                movablePieces.add(minBigger);
            }
            if (maxSmaller != null) {
                movablePieces.add(maxSmaller);
            }
        }
        return movablePieces;
    }

    // Execute move (update piece position, handle captures)
    public void executeMove(Move move) {
        int pieceIndex = move.getPieceNum() - 1;
        int fromPos = move.getFromPos();
        int toPos = move.getToPos();

        // Step 1: Check if there are any other pieces at the target location (capture if necessary)
        for (int i = 0; i < 6; i++) {
            if (currentPositions[i] == toPos) {
                currentPositions[i] = -1; // Capture: Set to -1
                break;
            }
        }

        //Step 2: Update the current piece's position
        currentPositions[pieceIndex] = toPos;
    }

    // Determine if victory is achieved (target piece reaches position 0)
    public boolean isWinning() {
        int targetIndex = targetPiece - 1;
        return currentPositions[targetIndex] == 0;
    }

    // Get the current piece position (for external printing)
    public int[] getCurrentPositions() {
        return currentPositions.clone(); // Return a copy to avoid external modifications
    }

    // Check if there are any legal moves left (to avoid a stalemate)
    public boolean hasPossibleMoves() {
        return !generatePossibleMoves().isEmpty();
    }
}
