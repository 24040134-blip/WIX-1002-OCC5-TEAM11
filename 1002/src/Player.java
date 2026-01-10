import java.io.PrintWriter;
import java.io.IOException;

public abstract class Player {
    protected String name; // Player Name

    // Constructor: Initialize player name
    public Player(String name) {
        this.name = name;
    }

    // Print move to moves.txt (append to the end of the file)
    public void printMove(int[] currentPositions) {
        try {
            // Append mode (will not overwrite existing content)
            PrintWriter pw = new PrintWriter(new java.io.FileWriter("moves.txt", true));

            // Print the current positions of all pieces (separated by spaces, -1 indicates captured)
            for (int i = 0; i < 6; i++) {
                pw.print(currentPositions[i]);
                if (i != 5) {
                    pw.print(" ");
                }
            }
            pw.println();

            pw.flush();
            pw.close();
        } catch (IOException e) {
            System.err.println("Print move failed！");
            e.printStackTrace();
        }
    }

    // Abstract Method: Choose Move (must be implemented by subclasses)
    public abstract Move chooseMove(GameState gameState);

    // Get player name
    public String getName() {
        return name;
    }
}
