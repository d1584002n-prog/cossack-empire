package ua.cossack.empire;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public final class GameState {
    public static final int MAX_LEVEL = 5;
    public static final int SPY_GOLD = 30;
    public static final int SPY_HORSES = 5;

    public static final String[] UNIT_NAMES = {"Козаки", "Мушкетери", "Кіннота"};
    public static final int[] UNIT_POWER = {3, 5, 6};
    // порядок: золото, їжа, дерево, залізо, порох, коні
    private static final int[][] RECRUIT_COST = {
        {10, 5, 0, 0, 0, 0},
        {15, 0, 0, 5, 5, 0},
        {20, 0, 0, 3, 0, 8}
    };
    private static final Random RNG = new Random();

    public static World world;
    public static int turn;
    public static final Map<World.ResourceType, Integer> stock =
            new EnumMap<>(World.ResourceType.class);
    public static final int[] army = new int[3];
    public static int prisoners;
    public static String log = "";

    static {
        newGame();
    }

    public static void newGame() {
        world = new World(System.nanoTime());
        turn = 1;
        for (World.ResourceType r : World.ResourceType.values()) {
            stock.put(r, r == World.ResourceType.GOLD ? 100 : 20);
        }
        army[0] = 5;
        army[1] = 0;
        army[2] = 0;
        prisoners = 0;
        log = "";
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

    // ---------- покращення ----------

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

    // ---------- туман і шпигун ----------

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

    // ---------- армія ----------

    public static String costText(int type) {
        World.ResourceType[] rs = World.ResourceType.values();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rs.length; i++) {
            int c = RECRUIT_COST[type][i];
            if (c > 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(rs[i].label).append(" ").append(c);
            }
        }
        return sb.toString();
    }

    public static boolean canRecruit(int type) {
        World.ResourceType[] rs = World.ResourceType.values();
        for (int i = 0; i < rs.length; i++) {
            if (stock.get(rs[i]) < RECRUIT_COST[type][i]) return false;
        }
        return true;
    }

    public static void recruit(int type) {
        if (!canRecruit(type)) return;
        World.ResourceType[] rs = World.ResourceType.values();
        for (int i = 0; i < rs.length; i++) {
            stock.merge(rs[i], -RECRUIT_COST[type][i], Integer::sum);
        }
        army[type]++;
    }

    public static int armyPower() {
        int s = 0;
        for (int i = 0; i < army.length; i++) s += army[i] * UNIT_POWER[i];
        return s;
    }

    public static boolean isNearMine(World.Province target) {
        for (World.Province q : world.provinces) {
            if (World.dist(target.col, target.row, q.col, q.row) > 1) continue;
            for (World.Point p : q.points) {
                if (p.owner == 0) return true;
            }
        }
        return false;
    }

    public static boolean canAttack(World.Province pr, World.Point p) {
        return pr.explored && p.owner != 0 && armyPower() > 0 && isNearMine(pr);
    }

    private static void revealAroundMine() {
        for (World.Province a : world.provinces) {
            boolean mine = false;
            for (World.Point p : a.points) {
                if (p.owner == 0) mine = true;
            }
            if (!mine) continue;
            for (World.Province b : world.provinces) {
                if (World.dist(a.col, a.row, b.col, b.row) <= 1) b.explored = true;
            }
        }
    }

    public static void attack(World.Province pr, World.Point p) {
        if (!canAttack(pr, p)) return;
        double att = armyPower() * (0.8 + RNG.nextDouble() * 0.4);
        double def = p.garrison * (1.0 + 0.15 * (p.level - 1)) * (0.8 + RNG.nextDouble() * 0.4);
        String where = p.type.label + " " + p.name;

        if (att > def) {
            double loss = Math.min(0.9, def / att * 0.6);
            for (int i = 0; i < army.length; i++) {
                army[i] -= (int) Math.round(army[i] * loss);
            }
            int caught = Math.max(1, p.garrison / 4);
            prisoners += caught;
            p.owner = 0;
            p.garrison = 3;
            p.unrest = 25;
            pr.recalcOwner();
            revealAroundMine();
            log = "Захоплено: " + where + ". Полонених: " + caught;
        } else {
            for (int i = 0; i < army.length; i++) {
                army[i] -= (int) Math.round(army[i] * 0.6);
            }
            p.garrison = Math.max(2, p.garrison - (int) (att / 4));
            log = "Поразка: " + where + ". Втрачено 60% війська";
        }
    }

    public static void ransomPrisoners() {
        if (prisoners <= 0) return;
        int gold = prisoners * 10;
        stock.merge(World.ResourceType.GOLD, gold, Integer::sum);
        log = "Викуп за полонених: +" + gold + " золота";
        prisoners = 0;
    }

    public static void enlistPrisoners() {
        if (prisoners <= 0) return;
        army[0] += prisoners;
        log = "Полонені вступили до війська: +" + prisoners + " козаків";
        prisoners = 0;
    }

    public static void releasePrisoners() {
        if (prisoners <= 0) return;
        log = "Полонених відпущено";
        prisoners = 0;
    }

    private GameState() {}
        }
