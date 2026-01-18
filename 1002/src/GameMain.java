import java.util.Scanner;

public class GameMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // Step 1: 选择游戏模式
        System.out.println("===== Einstein würfelt nicht! =====");
        System.out.println("Select a game mode:");
        System.out.println("1. Human Player");
        System.out.println("2. Random Player");
        System.out.println("3. AI Player");
        System.out.print("Input mode number (1/2/3):");
        int mode = scanner.nextInt();
        scanner.nextLine();

        // Step 2: 创建玩家对象
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

        // Step 3: 选择关卡
        System.out.println("\nSelect a level:");
        System.out.println("1. level1");
        System.out.println("2. level2");
        System.out.println("3. level3");
        System.out.println("4. level4");
        System.out.print("Enter level number (1/4):");
        int level = scanner.nextInt();
        String levelFileName = "level" + level + ".txt";

        // Step 4: 加载游戏数据
        GameLoader gameLoader = new GameLoader(levelFileName);
        targetPiece = gameLoader.getTargetPiece();
        int[] initialPositions = gameLoader.getInitialPositions();
        int[] diceSequence = gameLoader.getDiceSequence();

        // 给AI玩家设置关键参数
        if (player instanceof AIPlayer) {
            ((AIPlayer) player).setTargetPiece(targetPiece);
            ((AIPlayer) player).setDiceSequence(diceSequence);
            // 按关卡设置最大允许步数
            int maxMoves = switch (level) {
                case 1 -> 6;
                case 2, 3 -> 10;
                case 4 -> 15;
                default -> 30;
            };
            ((AIPlayer) player).setMaxAllowedMoves(maxMoves);
        }

        // 打印游戏信息到moves.txt
        gameLoader.printGameDetails(player.getName());

        // Step 5: 初始化游戏状态
        GameState gameState = new GameState(initialPositions, targetPiece);
        // 仅AI玩家需要重置步数和骰子索引（修复报错）
        if (player instanceof AIPlayer) {
            ((AIPlayer) player).resetMoveCount();
        }
        boolean isWin = false;
        int moveCount = 0;
        final int MAX_GLOBAL_MOVES = 30; // 全局最大步数上限

        // Step 6: 主游戏循环
        System.out.println("\n===== Game Start =====");
        while (moveCount < MAX_GLOBAL_MOVES) {
            // 检查是否获胜
            if (gameState.isWinning()) {
                isWin = true;
                break;
            }
            // 检查骰子序列是否用尽
            if (moveCount >= diceSequence.length) {
                System.out.println("Dice sequence is exhausted!");
                break;
            }

            // 设置当前回合骰子数
            int currentDice = diceSequence[moveCount];
            gameState.setCurrentDice(currentDice);

            // 更新AI的当前骰子索引
            if (player instanceof AIPlayer) {
                ((AIPlayer) player).setCurrentDiceIndex(moveCount);
            }

            // 玩家选择移动
            Move move = player.chooseMove(gameState);
            if (move == null) {
                System.out.println("No legal move! The game is over.");
                break;
            }

            // 执行移动并打印
            gameState.executeMove(move);
            player.printMove(gameState.getCurrentPositions());
            moveCount++;
            // 修复拼写错误：AIPLayer → AIPlayer（核心报错修复）
            System.out.println("Completed " + moveCount + " steps（Up to " +
                    (player instanceof AIPlayer ? ((AIPlayer) player).getMaxAllowedMoves() : MAX_GLOBAL_MOVES) + " steps）");
        }

        // Step 7: 显示游戏结果
        System.out.println("\n===== Game Over =====");
        if (isWin) {
            System.out.println("Congratulations! You won! The target piece has reached position 0!");
        } else {
            System.out.println("Unfortunately, the target piece could not reach position 0 within the allowed moves. The game failed.");
        }
        scanner.close();
    }
}