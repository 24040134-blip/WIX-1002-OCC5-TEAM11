import java.util.*;
import java.util.stream.Collectors; // 新增：解决Collectors符号未解析问题

public class AIPlayer extends Player {
    private int targetPiece;
    private int maxAllowedMoves;
    private int currentMoveCount;
    private int[] diceSequence; // 完整骰子序列
    private int currentDiceIndex; // 当前骰子索引

    public AIPlayer() {
        super("AI Player");
    }

    // 设置目标棋子
    public void setTargetPiece(int targetPiece) {
        this.targetPiece = targetPiece;
    }

    // 设置最大允许步数（按关卡）
    public void setMaxAllowedMoves(int maxMoves) {
        this.maxAllowedMoves = maxMoves;
    }

    // 获取最大允许步数（GameMain调用）
    public int getMaxAllowedMoves() {
        return maxAllowedMoves;
    }

    // 设置骰子序列
    public void setDiceSequence(int[] diceSequence) {
        this.diceSequence = diceSequence;
    }

    // 设置当前骰子索引
    public void setCurrentDiceIndex(int index) {
        this.currentDiceIndex = index;
    }

    // 重置步数和骰子索引
    public void resetMoveCount() {
        this.currentMoveCount = 0;
        this.currentDiceIndex = 0;
    }

    @Override
    public Move chooseMove(GameState gameState) {
        currentMoveCount++;
        List<Move> possibleMoves = gameState.generatePossibleMoves();
        if (possibleMoves.isEmpty()) {
            return null;
        }

        int remainingMoves = maxAllowedMoves - currentMoveCount;
        if (remainingMoves <= 0) {
            // 无剩余步数，强制移动目标棋子（向0靠近）
            Move targetMove = getTargetPieceMove(possibleMoves, gameState);
            return targetMove != null ? targetMove : possibleMoves.get(0);
        }

        // A*搜索最优路径（深度=剩余步数，强化骰子预判）
        Move bestMove = aStarSearch(gameState, remainingMoves);
        if (bestMove != null) {
            return bestMove;
        }

        // 贪心策略：严格遵循用户最新需求
        return getGreedyBestMove(gameState, possibleMoves);
    }

    /**
     * A*搜索：强化下一步骰子预判+目标棋子保护
     */
    private Move aStarSearch(GameState initialState, int maxDepth) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.totalCost));
        Map<String, Integer> visited = new HashMap<>();

        int initHeuristic = calculateHeuristic(initialState, currentDiceIndex);
        openSet.add(new Node(initialState, null, 0, initHeuristic, currentDiceIndex));
        visited.put(getStateKey(initialState), 0);

        while (!openSet.isEmpty()) {
            Node currentNode = openSet.poll();

            // 终止条件1：目标棋子到达0点（优先返回）
            if (currentNode.gameState.isWinning()) {
                return currentNode.firstMove;
            }

            // 终止条件2：达到搜索深度上限
            if (currentNode.gCost >= maxDepth) {
                continue;
            }

            List<Move> possibleMoves = currentNode.gameState.generatePossibleMoves();
            for (Move move : possibleMoves) {
                GameState nextState = copyGameState(currentNode.gameState);
                nextState.executeMove(move);
                String nextStateKey = getStateKey(nextState);
                int nextGCost = currentNode.gCost + 1;
                int nextDiceIndex = currentNode.diceIndex + 1;

                // 剪枝1：已访问且步数更多
                if (visited.containsKey(nextStateKey) && visited.get(nextStateKey) <= nextGCost) {
                    continue;
                }

                // 剪枝2：目标棋子被捕获（绝对禁止）
                int targetIndex = targetPiece - 1;
                if (nextState.getCurrentPositions()[targetIndex] == -1) {
                    continue;
                }

                // 剪枝3：吃目标棋子（双重校验，绝对禁止）
                if (isEatTargetMove(move, currentNode.gameState)) {
                    continue;
                }

                // 计算启发代价（强化下一步骰子适配）
                int nextHeuristic = calculateHeuristic(nextState, nextDiceIndex);

                // 剪枝4：启发代价>剩余步数
                if (nextHeuristic > (maxDepth - nextGCost)) {
                    continue;
                }

                // 剪枝5：非目标棋子阻挡目标路径且不吃子
                if (move.getPieceNum() != targetPiece) {
                    int[] targetPath = getManhattanPath(nextState.getCurrentPositions()[targetIndex], 0);
                    boolean blocksPath = Arrays.stream(targetPath).anyMatch(pos -> pos == move.getToPos());
                    boolean eatsPiece = isEatMove(move, currentNode.gameState);
                    if (blocksPath && !eatsPiece) {
                        continue;
                    }
                }

                Move firstMove = currentNode.firstMove == null ? move : currentNode.firstMove;
                openSet.add(new Node(nextState, firstMove, nextGCost, nextHeuristic, nextDiceIndex));
                visited.put(nextStateKey, nextGCost);
            }
        }
        return null;
    }

    /**
     * 启发函数：强化目标棋子向0移动+下一步骰子适配
     */
    private int calculateHeuristic(GameState state, int diceIndex) {
        int targetIndex = targetPiece - 1;
        int targetPos = state.getCurrentPositions()[targetIndex];

        // 目标棋子被吃（绝对无效）
        if (targetPos == -1) return Integer.MAX_VALUE;
        // 目标到达0点（代价为0）
        if (targetPos == 0) return 0;

        // 1. 核心代价：目标到0的曼哈顿距离（权重最高）
        int heuristic = calculateDistance(targetPos, 0) * 2;

        // 2. 奖励：目标棋子能吃非目标棋子（减3）
        List<Move> targetMoves = state.generatePossibleMoves().stream()
                .filter(m -> m.getPieceNum() == targetPiece)
                .filter(m -> isEatMove(m, state))
                .toList();
        if (!targetMoves.isEmpty()) {
            heuristic -= 3;
        }

        // 3. 奖励：下一步骰子能移动目标棋子（减4，权重高于吃子）
        if (diceIndex + 1 < diceSequence.length) {
            int nextDice = diceSequence[diceIndex + 1];
            if (canMoveTargetNextStep(state, nextDice)) {
                heuristic -= 4;
            }
        }

        // 4. 惩罚：非目标棋子阻挡目标路径（加2/个）
        heuristic += countObstaclesOnPath(state, targetPos, 0) * 2;

        // 5. 奖励：非目标棋子靠近可被吃的棋子（减1）
        heuristic -= calculateNonTargetProximityBonus(state);

        return Math.max(heuristic, 0);
    }

    /**
     * 贪心策略：严格遵循用户决策逻辑（修复：删除未使用变量targetIndex/currentPositions）
     */
    private Move getGreedyBestMove(GameState gameState, List<Move> possibleMoves) {
        // 步骤1：筛选可选移动的棋子（去重）
        Set<Integer> movablePieces = possibleMoves.stream()
                .map(Move::getPieceNum)
                .collect(Collectors.toSet());

        // 步骤2：若可选棋子包含目标棋子→优先移动目标（向0+吃子）
        if (movablePieces.contains(targetPiece)) {
            return getTargetPieceMove(possibleMoves, gameState);
        }

        // 步骤3：可选棋子无目标→预判下一步骰子，选对后续移动目标最有利的移动
        // 修复：删除未使用参数movablePieces
        return getOptimalNonTargetMove(gameState, possibleMoves);
    }

    /**
     * 移动目标棋子：向0靠近（优先吃非目标棋子）（修复：优化hasEatMove逻辑）
     */
    private Move getTargetPieceMove(List<Move> possibleMoves, GameState gameState) {
        Move bestMove = null;
        int minDistance = Integer.MAX_VALUE;
        boolean hasEatMove = false;

        for (Move move : possibleMoves) {
            if (move.getPieceNum() != targetPiece) {
                continue;
            }

            // 跳过吃目标棋子的移动（双重保护）
            if (isEatTargetMove(move, gameState)) {
                continue;
            }

            int toPos = move.getToPos();
            int distance = calculateDistance(toPos, 0);
            boolean isEat = isEatMove(move, gameState);

            // 修复：动态更新hasEatMove，避免始终为true
            if (isEat) {
                if (!hasEatMove || distance < minDistance) {
                    hasEatMove = true;
                    minDistance = distance;
                    bestMove = move;
                }
            } else {
                if (!hasEatMove && distance < minDistance) {
                    minDistance = distance;
                    bestMove = move;
                }
            }
        }

        // 修复：用getFirst简化Stream调用
        return bestMove != null ? bestMove : possibleMoves.stream()
                .filter(m -> m.getPieceNum() == targetPiece)
                .findFirst()
                .orElse(null);
    }

    /**
     * 无目标棋子时：预判下一步骰子，选最优非目标移动（修复：删除未使用参数movablePieces）
     */
    private Move getOptimalNonTargetMove(GameState gameState, List<Move> possibleMoves) {
        Move bestMove = null;
        int maxScore = Integer.MIN_VALUE;
        int nextDice = getNextDice(); // 重点：仅预判下一步骰子

        for (Move move : possibleMoves) {
            int score = 0;
            int pieceNum = move.getPieceNum();
            int toPos = move.getToPos();

            // 模拟移动后的游戏状态
            GameState simulatedState = copyGameState(gameState);
            simulatedState.executeMove(move);

            // 加分1：移动后下一步能移动目标棋子（权重最高，6分）
            if (canMoveTargetNextStep(simulatedState, nextDice)) {
                score += 6;
            }

            // 加分2：吃非目标棋子（5分）
            if (isEatMove(move, gameState)) {
                score += 5;
            }

            // 加分3：吃掉阻挡目标路径的棋子（额外加2分）
            if (isEatMove(move, gameState)) {
                int blockedPos = move.getToPos();
                if (isOnTargetPath(gameState, blockedPos)) {
                    score += 2;
                }
            }

            // 加分4：靠近其他棋子（方便后续吃子/被吃，3分）
            int minDistToOthers = calculateMinDistanceToOtherPieces(toPos, gameState.getCurrentPositions(), pieceNum);
            score += (3 - minDistToOthers);

            // 加分5：远离目标路径（2分）
            if (!isOnTargetPath(gameState, toPos)) {
                score += 2;
            }

            // 更新最优移动
            if (score > maxScore) {
                maxScore = score;
                bestMove = move;
            }
        }

        // 修复：用getFirst简化Stream调用
        return bestMove != null ? bestMove : possibleMoves.stream().findFirst().orElse(null);
    }

    // ---------------------- 新增/强化的辅助方法 ----------------------
    /**
     * 获取下一步骰子数（无则返回-1）
     */
    private int getNextDice() {
        int nextDiceIndex = currentDiceIndex + 1;
        return (nextDiceIndex < diceSequence.length) ? diceSequence[nextDiceIndex] : -1;
    }

    /**
     * 判断下一步是否能移动目标棋子
     */
    private boolean canMoveTargetNextStep(GameState state, int nextDice) {
        if (nextDice == -1) return false;

        int targetIndex = targetPiece - 1;
        // 目标棋子已被吃
        if (state.getCurrentPositions()[targetIndex] == -1) return false;

        List<Integer> existingPieces = getExistingPieces(state);

        // 情况1：下一步骰子直接匹配目标棋子
        if (nextDice == targetPiece) {
            return existingPieces.contains(targetPiece);
        }

        // 情况2：下一步骰子无匹配，目标是minBigger或maxSmaller
        if (!existingPieces.contains(nextDice)) {
            Integer minBigger = getMinBigger(existingPieces, nextDice);
            Integer maxSmaller = getMaxSmaller(existingPieces, nextDice);
            return (minBigger != null && minBigger == targetPiece) || (maxSmaller != null && maxSmaller == targetPiece);
        }

        return false;
    }

    /**
     * 双重校验：是否为吃目标棋子的移动（绝对禁止）
     */
    private boolean isEatTargetMove(Move move, GameState gameState) {
        int targetIndex = targetPiece - 1;
        int toPos = move.getToPos();
        int[] positions = gameState.getCurrentPositions();

        for (int i = 0; i < 6; i++) {
            if (i == targetIndex && positions[i] == toPos) {
                return true;
            }
        }
        return false;
    }

    // ---------------------- 原有辅助方法（保持不变，删除未使用的hasObstacleOnPath） ----------------------
    /**
     * 检查移动是否为吃非目标棋子
     */
    private boolean isEatMove(Move move, GameState gameState) {
        int targetIndex = targetPiece - 1;
        int toPos = move.getToPos();
        int[] positions = gameState.getCurrentPositions();

        for (int i = 0; i < 6; i++) {
            if (i != targetIndex && positions[i] == toPos) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算非目标棋子到其他棋子的最小距离
     */
    private int calculateMinDistanceToOtherPieces(int pos, int[] currentPositions, int currentPieceNum) {
        int minDist = Integer.MAX_VALUE;
        int currentPieceIndex = currentPieceNum - 1;

        for (int i = 0; i < 6; i++) {
            if (i == currentPieceIndex || currentPositions[i] == -1 || (i + 1) == targetPiece) {
                continue;
            }
            int dist = calculateDistance(pos, currentPositions[i]);
            if (dist < minDist) {
                minDist = dist;
            }
        }

        return minDist == Integer.MAX_VALUE ? 3 : minDist;
    }

    /**
     * 非目标棋子靠近其他棋子的奖励
     */
    private int calculateNonTargetProximityBonus(GameState state) {
        int bonus = 0;
        int[] positions = state.getCurrentPositions();

        for (int i = 0; i < 6; i++) {
            if ((i + 1) == targetPiece || positions[i] == -1) {
                continue;
            }

            int minDist = calculateMinDistanceToOtherPieces(positions[i], positions, i + 1);
            if (minDist == 0) bonus += 1;
            else if (minDist == 1) bonus += 1;
        }

        return bonus;
    }

    /**
     * 获取目标棋子到0点的曼哈顿路径
     */
    private int[] getManhattanPath(int fromPos, int toPos) {
        List<Integer> path = new ArrayList<>();
        int fromRow = fromPos / 10, fromCol = fromPos % 10;
        int toRow = toPos / 10, toCol = toPos % 10;

        int currRow = fromRow, currCol = fromCol;
        while (currRow > toRow || currCol > toCol) {
            if (currRow > toRow) currRow--;
            if (currCol > toCol) currCol--;
            int pos = currRow * 10 + currCol;
            if (pos != 22) path.add(pos);
        }
        return path.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 统计目标路径上的阻挡数
     */
    private int countObstaclesOnPath(GameState state, int fromPos, int toPos) {
        int[] path = getManhattanPath(fromPos, toPos);
        int targetIndex = targetPiece - 1;
        int[] positions = state.getCurrentPositions();
        int count = 0;

        for (int pos : path) {
            for (int i = 0; i < 6; i++) {
                if (i != targetIndex && positions[i] == pos) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /**
     * 检查位置是否在目标路径上
     */
    private boolean isOnTargetPath(GameState state, int pos) {
        int targetIndex = targetPiece - 1;
        int targetPos = state.getCurrentPositions()[targetIndex];
        if (targetPos == -1 || targetPos == 0) return false;

        int[] path = getManhattanPath(targetPos, 0);
        return Arrays.stream(path).anyMatch(p -> p == pos);
    }

    /**
     * 获取当前存在的棋子（非-1）
     */
    private List<Integer> getExistingPieces(GameState state) {
        List<Integer> pieces = new ArrayList<>();
        int[] positions = state.getCurrentPositions();
        for (int i = 0; i < 6; i++) {
            if (positions[i] != -1) {
                pieces.add(i + 1);
            }
        }
        return pieces;
    }

    /**
     * 获取大于骰子数的最小棋子
     */
    private Integer getMinBigger(List<Integer> pieces, int dice) {
        return pieces.stream().filter(p -> p > dice).min(Integer::compare).orElse(null);
    }

    /**
     * 获取小于骰子数的最大棋子
     */
    private Integer getMaxSmaller(List<Integer> pieces, int dice) {
        return pieces.stream().filter(p -> p < dice).max(Integer::compare).orElse(null);
    }

    /**
     * 复制游戏状态
     */
    private GameState copyGameState(GameState original) {
        int[] newPositions = original.getCurrentPositions().clone();
        GameState copy = new GameState(newPositions, targetPiece);
        copy.setCurrentDice(original.getCurrentDice());
        return copy;
    }

    /**
     * 生成游戏状态唯一键
     */
    private String getStateKey(GameState state) {
        StringBuilder sb = new StringBuilder();
        int[] positions = state.getCurrentPositions();
        for (int pos : positions) sb.append(pos).append(",");
        sb.append(state.getCurrentDice()).append(",").append(currentDiceIndex);
        return sb.toString();
    }

    /**
     * 计算曼哈顿距离
     */
    private int calculateDistance(int pos1, int pos2) {
        int row1 = pos1 / 10, col1 = pos1 % 10;
        int row2 = pos2 / 10, col2 = pos2 % 10;
        return Math.abs(row1 - row2) + Math.abs(col1 - col2);
    }

    /**
     * A*搜索节点类
     */
    private static class Node {
        GameState gameState;
        Move firstMove;
        int gCost;
        int hCost;
        int totalCost;
        int diceIndex;

        Node(GameState gameState, Move firstMove, int gCost, int hCost, int diceIndex) {
            this.gameState = gameState;
            this.firstMove = firstMove;
            this.gCost = gCost;
            this.hCost = hCost;
            this.totalCost = gCost + hCost;
            this.diceIndex = diceIndex;
        }
    }
}
