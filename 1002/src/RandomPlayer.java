import java.util.List;
import java.util.Random;

public class RandomPlayer extends Player {
    private Random random; // Random generator

    // Constructor: Initialize player name and random tool
    public RandomPlayer() {
        super("Random Player"); // Default Name
        random = new Random();
    }

    // Select move: Pick a random legal move
    @Override
    public Move chooseMove(GameState gameState) {
        List<Move> possibleMoves = gameState.generatePossibleMoves();

        // If no legal moves exist, return null (Game Over/Fail)
        if (possibleMoves.isEmpty()) {
            return null;
        }

        // Pick a random move
        int randomIndex = random.nextInt(possibleMoves.size());
        return possibleMoves.get(randomIndex);
    }
}
