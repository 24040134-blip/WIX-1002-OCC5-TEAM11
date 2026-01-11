import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class GameLoader {
    // Save game data read from the file
    private int targetPiece;       // Target piece (the piece to reach position 0)
    private int[] initialPositions; // Initial positions of 6 pieces
    private int[] diceSequence;     // dice sequence

    // Take the level filename and read the data
    public GameLoader(String filename) {
        try {
            // File reading tool
            BufferedReader br = new BufferedReader(new FileReader(filename));

            // Line 1: Target piece (converted to integer)
            targetPiece = Integer.parseInt(br.readLine().trim());

            // Line 2: Initial positions of 6 pieces (separated by spaces)
            String[] posStr = br.readLine().trim().split(" ");
            initialPositions = new int[6];
            for (int i = 0; i < 6; i++) {
                initialPositions[i] = Integer.parseInt(posStr[i]);
            }

            // Line 3: Dice sequence (separated by spaces)
            String[] diceStr = br.readLine().trim().split(" ");
            diceSequence = new int[diceStr.length];
            for (int i = 0; i < diceStr.length; i++) {
                diceSequence[i] = Integer.parseInt(diceStr[i]);
            }

            br.close();
        } catch (IOException e) {

            System.err.println("Failed to read the file. Check the file name:" + filename);
            e.printStackTrace();
        }
    }

    // Print game details to moves.txt
    public void printGameDetails(String playerName) {
        try {
            // Overwrite existing content
            PrintWriter pw = new PrintWriter("moves.txt");

            // Line 1: Player name
            pw.println(playerName);

            // Line 2: Dice sequence (connected by spaces)
            for (int i = 0; i < diceSequence.length; i++) {
                pw.print(diceSequence[i]);
                if (i != diceSequence.length - 1) {
                    pw.print(" ");
                }
            }
            pw.println();

            // Line 3: Target piece
            pw.println(targetPiece);

            // Line 4: Initial position (separated by spaces)
            for (int i = 0; i < 6; i++) {
                pw.print(initialPositions[i]);
                if (i != 5) {
                    pw.print(" ");
                }
            }
            pw.println();

            pw.flush();
            pw.close();
        } catch (IOException e) {
            System.err.println("Failed to write to moves.txt!");
            e.printStackTrace();
        }
    }

    // Getter method: Allows other classes to access the read data
    public int getTargetPiece() {
        return targetPiece;
    }

    public int[] getInitialPositions() {
        return initialPositions;
    }

    public int[] getDiceSequence() {
        return diceSequence;
    }
}