package ua.cossack.empire;

import java.util.EnumMap;
import java.util.Map;

public final class GameState {
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
                if (p.owner == 0 && p.type.resource == r) total += p.output;
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

    private GameState() {}
}
