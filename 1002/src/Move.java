// Store move information: piece ID, starting position, and target position
public class Move {
    private int pieceNum;   // Piece to move (1-6)
    private int fromPos;    // Starting position (0-99, excluding 22)
    private int toPos;      // Target position (0-99, excluding 22)

    // Constructor: Initialize move information
    public Move(int pieceNum, int fromPos, int toPos) {
        this.pieceNum = pieceNum;
        this.fromPos = fromPos;
        this.toPos = toPos;
    }
    
    // Getter Method: Getters for private variables (Setters not required for beginners).
    public int getPieceNum() {
        return pieceNum;
    }

    public int getFromPos() {
        return fromPos;
    }

    public int getToPos() {
        return toPos;
    }
}
