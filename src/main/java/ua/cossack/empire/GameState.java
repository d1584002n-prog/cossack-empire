package ua.cossack.empire;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class GameState {
    public static final int MAX_LEVEL = 5;
    public static final int SPY_GOLD = 30;
    public static final int SPY_HORSES = 5;
    public static final int REBELS = 6;
    private static final int LAST_STATE = 5;

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

    public static final int[] relation = new int[World.FACTIONS.length];
    public static final boolean[] atWar = new boolean[World.FACTIONS.length];
    public static final boolean[] allied = new boolean[World.FACTIONS.length];
    public static final List<String> history = new ArrayList<>();

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
        history.clear();
        for (int f = 0; f < World.FACTIONS.length; f++) {
            relation[f] = f >= 1 && f <= LAST_STATE ? RNG.nextInt(61) - 30 : 0;
            atWar[f] = false;
            allied[f] = false;
        }
    }

    public static void addLog(String s) {
        history.add(s);
        while (history.size() > 8) history.remove(0);
    }

    public static String logLine(int fromEnd) {
        int i = history.size() - 1 - fromEnd;
        return i >= 0 ? history.get(i) : "";
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

    // ---------- хід ----------

    public static void nextTurn() {
        for (World.ResourceType r : World.ResourceType.values()) {
            stock.merge(r, income(r), Integer::sum);
        }
        payUpkeep();
        turn++;
        updateRelations();
        updateUnrest();
        growGarrisons();
        enemyTurn();
        randomEvent();
    }

    public static int upkeepFood() {
        return (army[0] + army[1] + army[2]) / 2;
    }

    public static int upkeepGold() {
        return armyPower() / 5;
    }

    private static void desert(double ratio) {
        for (int i = 0; i < army.length; i++) {
            army[i] -= (int) Math.round(army[i] * ratio);
        }
    }

    private static void payUpkeep() {
        stock.merge(World.ResourceType.FOOD, -upkeepFood(), Integer::sum);
        stock.merge(World.ResourceType.GOLD, -upkeepGold(), Integer::sum);
        if (stock.get(World.ResourceType.FOOD) < 0) {
            stock.put(World.ResourceType.FOOD, 0);
            desert(0.2);
            addLog("Голод у війську: втрачено 20% армії");
        }
        if (stock.get(World.ResourceType.GOLD) < 0) {
            stock.put(World.ResourceType.GOLD, 0);
            desert(0.15);
            addLog("Немає золота: частина війська розійшлась");
        }
    }

    private static void updateRelations() {
        for (int f = 1; f <= LAST_STATE; f++) {
            if (atWar[f]) {
                relation[f] = Math.max(-100, relation[f] - 1);
            } else if (!allied[f]) {
                if (relation[f] > 0) relation[f]--;
                else if (relation[f] < 0) relation[f]++;
                if (relation[f] < 10 && RNG.nextInt(100) < 5) {
                    atWar[f] = true;
                    relation[f] = Math.max(-100, relation[f] - 20);
                    addLog(World.FACTIONS[f].name() + " оголосила війну!");
                }
            }
        }
    }

    private static void updateUnrest() {
        boolean warAny = false;
        for (int f = 1; f <= LAST_STATE; f++) {
            if (atWar[f]) warAny = true;
        }
        boolean hungry = stock.get(World.ResourceType.FOOD) < 10;
        int delta = (warAny ? 1 : 0) + (hungry ? 2 : 0) - 1;
        for (World.Province pr : world.provinces) {
            for (World.Point p : pr.points) {
                if (p.owner != 0) continue;
                p.unrest = Math.max(0, Math.min(100, p.unrest + delta));
                if (p.unrest >= 60 && RNG.nextInt(100) < 25) {
                    p.owner = REBELS;
                    p.garrison = 6 + p.level * 2;
                    pr.recalcOwner();
                    addLog("Повстання! Втрачено: " + p.type.label + " " + p.name);
                }
            }
        }
    }

    private static void growGarrisons() {
        for (World.Province pr : world.provinces) {
            for (World.Point p : pr.points) {
                if (p.owner != 0 && p.garrison < 10 + turn * 2) p.garrison++;
            }
        }
    }

    private static boolean bordersFaction(World.Province target, int f) {
        for (World.Province q : world.provinces) {
            if (World.dist(target.col, target.row, q.col, q.row) > 1) continue;
            for (World.Point p : q.points) {
                if (p.owner == f) return true;
            }
        }
        return false;
    }

    private static void enemyTurn() {
        for (int f = 1; f <= LAST_STATE; f++) {
            if (!atWar[f] || RNG.nextInt(100) >= 55) continue;

            List<World.Province> tProv = new ArrayList<>();
            List<World.Point> tPoint = new ArrayList<>();
            for (World.Province q : world.provinces) {
                if (!bordersFaction(q, f)) continue;
                for (World.Point p : q.points) {
                    if (p.owner == 0) {
                        tProv.add(q);
                        tPoint.add(p);
                    }
                }
            }
            if (tPoint.isEmpty()) continue;

            int k = RNG.nextInt(tPoint.size());
            World.Province prov = tProv.get(k);
            World.Point target = tPoint.get(k);
            double att = (20 + turn * 6 + RNG.nextInt(40)) * (0.8 + RNG.nextDouble() * 0.4);
            double def = (target.garrison + armyPower() / 12.0) * (1.0 + 0.1 * (target.level - 1))
                    * (0.8 + RNG.nextDouble() * 0.4);
            String where = target.type.label + " " + target.name;
            desert(0.08);
            if (att > def) {
                target.owner = f;
                target.garrison = 4;
                prov.recalcOwner();
                addLog(World.FACTIONS[f].name() + " захопила: " + where);
            } else {
                int caught = 1 + RNG.nextInt(3);
                prisoners += caught;
                addLog("Відбито напад: " + where + ". Полонених: " + caught);
            }
        }
    }

    private static void randomEvent() {
        if (RNG.nextInt(100) >= 35) return;
        switch (RNG.nextInt(5)) {
            case 0 -> {
                if (allied[4]) return;
                int g = Math.min(stock.get(World.ResourceType.GOLD), 30);
                int fd = Math.min(stock.get(World.ResourceType.FOOD), 15);
                stock.merge(World.ResourceType.GOLD, -g, Integer::sum);
                stock.merge(World.ResourceType.FOOD, -fd, Integer::sum);
                addLog("Набіг татар: -" + g + " золота, -" + fd + " їжі");
            }
            case 1 -> {
                int fd = stock.get(World.ResourceType.FOOD);
                int lost = fd / 4;
                stock.merge(World.ResourceType.FOOD, -lost, Integer::sum);
                addLog("Неврожай: -" + lost + " їжі");
            }
            case 2 -> {
                stock.merge(World.ResourceType.GOLD, 40, Integer::sum);
                addLog("Торговці принесли дари: +40 золота");
            }
            case 3 -> {
                stock.merge(World.ResourceType.FOOD, 40, Integer::sum);
                addLog("Добрий урожай: +40 їжі");
            }
            default -> {
                for (int t = 0; t < 40; t++) {
                    World.Province pr = world.provinces[RNG.nextInt(world.provinces.length)];
                    World.Point p = pr.points.get(RNG.nextInt(pr.points.size()));
                    if (p.owner >= 1 && p.owner <= LAST_STATE && p.unrest >= 15) {
                        p.owner = REBELS;
                        p.garrison = 5 + p.level * 2;
                        pr.recalcOwner();
                        if (pr.explored) addLog("Повстання в " + pr.name);
                        break;
                    }
                }
            }
        }
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

    public static boolean atWarWith(World.Point p) {
        return p.owner == REBELS || (p.owner >= 1 && p.owner <= LAST_STATE && atWar[p.owner]);
    }

    public static boolean canAttack(World.Province pr, World.Point p) {
        return pr.explored && p.owner != 0 && atWarWith(p) && armyPower() > 0 && isNearMine(pr);
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
        int oldOwner = p.owner;

        if (att > def) {
            double loss = Math.min(0.9, def / att * 0.6);
            desert(loss);
            int caught = Math.max(1, p.garrison / 4);
            prisoners += caught;
            p.owner = 0;
            p.garrison = 3;
            p.unrest = 25;
            pr.recalcOwner();
            revealAroundMine();
            if (oldOwner >= 1 && oldOwner <= LAST_STATE) {
                relation[oldOwner] = Math.max(-100, relation[oldOwner] - 5);
            }
            addLog("Захоплено: " + where + ". Полонених: " + caught);

            boolean all = true;
            for (World.Province q : world.provinces) {
                for (World.Point pt : q.points) {
                    if (pt.owner != 0) all = false;
                }
            }
            if (all) addLog("ПЕРЕМОГА! Усі землі під владою козаків");
        } else {
            desert(0.6);
            p.garrison = Math.max(2, p.garrison - (int) (att / 4));
            addLog("Поразка: " + where + ". Втрачено 60% війська");
        }
    }

    public static void ransomPrisoners() {
        if (prisoners <= 0) return;
        int gold = prisoners * 10;
        stock.merge(World.ResourceType.GOLD, gold, Integer::sum);
        addLog("Викуп за полонених: +" + gold + " золота");
        prisoners = 0;
    }

    public static void enlistPrisoners() {
        if (prisoners <= 0) return;
        army[0] += prisoners;
        addLog("Полонені вступили до війська: +" + prisoners);
        prisoners = 0;
    }

    public static void releasePrisoners() {
        if (prisoners <= 0) return;
        addLog("Полонених відпущено");
        prisoners = 0;
    }

    // ---------- дипломатія ----------

    public static boolean canGift(int f) {
        return stock.get(World.ResourceType.GOLD) >= 50 && relation[f] < 100;
    }

    public static void gift(int f) {
        if (!canGift(f)) return;
        stock.merge(World.ResourceType.GOLD, -50, Integer::sum);
        relation[f] = Math.min(100, relation[f] + 10);
        addLog("Дар для " + World.FACTIONS[f].name() + ": відносини +10");
    }

    public static boolean canAlly(int f) {
        return allied[f] || (!atWar[f] && relation[f] >= 50);
    }

    public static void toggleAlliance(int f) {
        if (!canAlly(f)) return;
        if (allied[f]) {
            allied[f] = false;
            relation[f] = Math.max(-100, relation[f] - 15);
            addLog("Союз розірвано: " + World.FACTIONS[f].name());
        } else {
            allied[f] = true;
            addLog("Союз укладено: " + World.FACTIONS[f].name());
        }
    }

    public static boolean canWarToggle(int f) {
        return !atWar[f] || stock.get(World.ResourceType.GOLD) >= 50;
    }

    public static void toggleWar(int f) {
        if (!canWarToggle(f)) return;
        String name = World.FACTIONS[f].name();
        if (atWar[f]) {
            if (relation[f] >= -60) {
                stock.merge(World.ResourceType.GOLD, -50, Integer::sum);
                atWar[f] = false;
                relation[f] = Math.max(relation[f], -20);
                addLog("Мир з: " + name);
            } else {
                addLog(name + " відмовилась від миру");
            }
        } else {
            atWar[f] = true;
            allied[f] = false;
            relation[f] = Math.max(-100, relation[f] - 30);
            addLog("Війна з: " + name);
        }
    }

    private GameState() {}
                        }
