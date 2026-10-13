package ua.cossack.empire;

import java.util.EnumMap;
import java.util.Map;

public final class GameState {
    public static final int MAX_LEVEL = 5;
    public static final int SPY_GOLD = 30;
    public static final int SPY_HORSES = 5;

    public static World world;
    public static int turn;
    public static final Map<World.ResourceType, Integer> stock =
            new EnumMap<>(World.ResourceType.class);

    static {
        newGame();
    }

    public static void newGame() {
        world = new World(System.nanoTime());
        turn = 1;
        for (World.ResourceType r : World.ResourceType.values()) {
            stock.put(r, r == World.ResourceType.GOLD ? 100 : 20);
        }
    }

    public static int income(World.ResourceType r) {
        int total = 0;
        for (World.Province pr : world.provinces) {
            for (World.Point p : pr.points) {
                if (p.owner == 0 && p.type.resource == r) total += p.production();
            }
        }
        return total;
    }

    public static void nextTurn() {
        for (World.ResourceType r : World.ResourceType.values()) {
            stock.merge(r, income(r), Integer::sum);
        }
        turn++;
    }

    public static int costGold(World.Point p) { return 40 * p.level; }
    public static int costWood(World.Point p) { return 15 * p.level; }
    public static int costIron(World.Point p) { return 5 * p.level; }

    public static boolean canUpgrade(World.Point p) {
        return p.owner == 0
                && p.level < MAX_LEVEL
                && stock.get(World.ResourceType.GOLD) >= costGold(p)
                && stock.get(World.ResourceType.WOOD) >= costWood(p)
                && stock.get(World.ResourceType.IRON) >= costIron(p);
    }

    public static void upgrade(World.Point p) {
        if (!canUpgrade(p)) return;
        stock.merge(World.ResourceType.GOLD, -costGold(p), Integer::sum);
        stock.merge(World.ResourceType.WOOD, -costWood(p), Integer::sum);
        stock.merge(World.ResourceType.IRON, -costIron(p), Integer::sum);
        p.level++;
    }

    public static boolean nextToExplored(World.Province target) {
        for (World.Province q : world.provinces) {
            if (q.explored && World.dist(target.col, target.row, q.col, q.row) <= 1) return true;
        }
        return false;
    }

    public static boolean canSpy(World.Province target) {
        return !target.explored
                && nextToExplored(target)
                && stock.get(World.ResourceType.GOLD) >= SPY_GOLD
                && stock.get(World.ResourceType.HORSES) >= SPY_HORSES;
    }

    public static void sendSpy(World.Province target) {
        if (!canSpy(target)) return;
        stock.merge(World.ResourceType.GOLD, -SPY_GOLD, Integer::sum);
        stock.merge(World.ResourceType.HORSES, -SPY_HORSES, Integer::sum);
        target.explored = true;
    }

    public static int exploredCount() {
        int n = 0;
        for (World.Province p : world.provinces) {
            if (p.explored) n++;
        }
        return n;
    }

    private GameState() {}
}
