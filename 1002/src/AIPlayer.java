import java.util.List;

public class AIPlayer extends Player {
    private int targetPiece; // Target piece (to reach position 0)

    // Constructor: Initialize player name (no parameters)
    public AIPlayer() {
        super("AI Player"); // Default Name
    }

    // New: Method to set the target piece (fixes GameMain assignment error)
    public void setTargetPiece(int targetPiece) {
        this.targetPiece = targetPiece;
    }

    // Choose Move: Greedy Strategy
    @Override
    public Move chooseMove(GameState gameState) {
        List<Move> possibleMoves = gameState.generatePossibleMoves();

        // No legal moves, return null
        if (possibleMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int minDistance = Integer.MAX_VALUE; // Goal: The closer to position 0, the better

        for (Move move : possibleMoves) {
            // Prioritize moving the target piece
            if (move.getPieceNum() == targetPiece) {
                // Calculate the distance to position 0 after moving (Manhattan distance: row difference + column difference, simple and effective)
                int newPos = move.getToPos();
                int distance = calculateDistance(newPos, 0);

                // Choose the nearest move
                if (distance < minDistance) {
                    minDistance = distance;
                    bestMove = move;
                }
            }
        }

        // If the target piece cannot move, choose any legal move of another piece (preferably one that does not block the path).
        if (bestMove == null) {
            bestMove = possibleMoves.get(0);
            // Simple optimization: avoid moving near the 0 position (leave a path for the target piece)
            for (Move move : possibleMoves) {
                int distance = calculateDistance(move.getToPos(), 0);
                if (distance > 5) { // Stay away from the zero position, don’t block the way
                    bestMove = move;
                    break;
                }
            }
        }

        return bestMove;
    }

    // Calculate the Manhattan distance between two positions (the absolute value of the row difference + the absolute value of the column difference)
    private int calculateDistance(int pos1, int pos2) {
        int row1 = pos1 / 10;
        int col1 = pos1 % 10;
        int row2 = pos2 / 10;
        int col2 = pos2 % 10;
        return Math.abs(row1 - row2) + Math.abs(col1 - col2);
    }
}
