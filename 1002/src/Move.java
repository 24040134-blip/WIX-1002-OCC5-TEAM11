// 存储一步移动的信息：移动的棋子编号、起始位置、目标位置
public class Move {
    private int pieceNum;   // 要移动的棋子（1-6）
    private int fromPos;    // 起始位置（0-99，除了22）
    private int toPos;      // 目标位置（0-99，除了22）

    // 构造方法：初始化移动信息
    public Move(int pieceNum, int fromPos, int toPos) {
        this.pieceNum = pieceNum;
        this.fromPos = fromPos;
        this.toPos = toPos;
    }

    // Getter方法：获取私有变量（初学者无需理解Setter，暂时用不到）
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
