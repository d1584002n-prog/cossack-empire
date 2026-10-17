package ua.cossack.empire;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class GameState {
    public static final int MAX_LEVEL = 5;
    public static final int SPY_GOLD = 40;
    public static final int SPY_HORSES = 0;
    public static final int REBELS = World.REBELS;
    private static final int LAST_STATE = Countries.ALL.length - 1;

    public static final String[] UNIT_NAMES = {"Козаки", "Мушкетери", "Кіннота"};
    public static final int[] UNIT_POWER = {3, 5, 6};

    // ---------- флот ----------
    public static int ships;
    public static final int SHIP_GOLD = 60;
    public static final int SHIP_WOOD = 40;
    public static final int SHIP_IRON = 10;
    private static final int SHIP_UPKEEP = 3;
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

    public static final boolean[] techDone = new boolean[Tech.values().length];
    public static Tech researching;
    public static int researchLeft;

    public static final int[] relation = new int[World.FACTIONS.length];
    public static final boolean[] atWar = new boolean[World.FACTIONS.length];
    public static final boolean[] allied = new boolean[World.FACTIONS.length];
    public static final List<String> history = new ArrayList<>();
    public static final List<String> journal = new ArrayList<>();
    public static int attacksLeft;
    // війни між державами ШІ: ключ a*100+b (a<b) -> хід закінчення
    private static final Map<Integer, Integer> aiWars = new HashMap<>();

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
        Realm.reset();
        ships = 0;
        java.util.Arrays.fill(techDone, false);
        researching = null;
        researchLeft = 0;
        history.clear();
        journal.clear();
        aiWars.clear();
        attacksLeft = attacksPerTurn();
        for (int f = 0; f < World.FACTIONS.length; f++) {
            relation[f] = f >= 1 && f <= LAST_STATE ? RNG.nextInt(61) - 30 : 0;
            atWar[f] = false;
            allied[f] = false;
        }
    }

    public static void addLog(String s) {
        history.add(s);
        while (history.size() > 8) history.remove(0);
        journal.add("Хід " + turn + ": " + s);
        while (journal.size() > 400) journal.remove(0);
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
        total += Realm.buildingIncome(r);
        if (r == World.ResourceType.GOLD) total += Realm.taxIncome() + Realm.seaTrade();
        return total * Math.max(0, 100 + resourceBonus(r)) / 100;
    }

    // ---------- пори року (по 10 ходів) ----------

    public static final int SEASON_TURNS = 10;
    public static final String[] SEASON_NAMES = {"Весна", "Літо", "Осінь", "Зима"};
    // зміна врожаю їжі, %
    private static final int[] SEASON_FOOD = {0, 15, 30, -30};

    public static int season() {
        return ((turn - 1) / SEASON_TURNS) % 4;
    }

    public static int year() {
        return 1572 + (turn - 1) / (SEASON_TURNS * 4);
    }

    public static String seasonLabel() {
        int b = SEASON_FOOD[season()];
        return SEASON_NAMES[season()] + (b != 0 ? " (їжа " + (b > 0 ? "+" : "") + b + "%)" : "");
    }

    // ---------- технології ----------

    public static boolean hasTech(Tech t) {
        return techDone[t.ordinal()];
    }

    private static int resourceBonus(World.ResourceType r) {
        return switch (r) {
            case FOOD -> (hasTech(Tech.FARMING) ? 20 : 0) + SEASON_FOOD[season()];
            case WOOD, IRON -> hasTech(Tech.CRAFT) ? 20 : 0;
            case GOLD -> hasTech(Tech.TRADE) ? 20 : 0;
            case HORSES -> hasTech(Tech.HORSES) ? 25 : 0;
            case POWDER -> hasTech(Tech.POWDER) ? 25 : 0;
        };
    }

    public static boolean canResearch(Tech t) {
        return !hasTech(t)
                && researching == null
                && (t.requires == null || hasTech(t.requires))
                && Realm.epoch >= t.level
                && stock.get(World.ResourceType.GOLD) >= t.gold
                && Realm.science >= t.science;
    }

    public static void startResearch(Tech t) {
        if (!canResearch(t)) return;
        stock.merge(World.ResourceType.GOLD, -t.gold, Integer::sum);
        Realm.science -= t.science;
        researching = t;
        researchLeft = t.turns;
        addLog("Почато дослідження: " + t.title);
    }

    private static void researchTurn() {
        if (researching == null) return;
        researchLeft--;
        if (researchLeft <= 0) {
            techDone[researching.ordinal()] = true;
            addLog("Відкрито технологію: " + researching.title);
            researching = null;
        }
    }

    public static int spyCost() {
        return hasTech(Tech.SPIES) ? SPY_GOLD / 2 : SPY_GOLD;
    }

    // ---------- хід ----------

    public static void nextTurn() {
        for (World.ResourceType r : World.ResourceType.values()) {
            stock.merge(r, income(r), Integer::sum);
        }
        attacksLeft = attacksPerTurn();
        payUpkeep();
        Realm.turn();
        researchTurn();
        turn++;
        if ((turn - 1) % SEASON_TURNS == 0) addLog("Настала пора року: " + SEASON_NAMES[season()]);
        updateRelations();
        updateUnrest();
        growGarrisons();
        enemyTurn();
        aiTurn();
        randomEvent();
    }

    public static int upkeepFood() {
        return (army[0] + army[1] + army[2]) / 2;
    }

    public static int upkeepGold() {
        return armyPower() / 5 + ships * SHIP_UPKEEP;
    }

    private static void desert(double ratio) {
        for (int i = 0; i < army.length; i++) {
            army[i] -= (int) Math.round(army[i] * ratio);
        }
    }

    private static void payUpkeep() {
        stock.merge(World.ResourceType.FOOD, -upkeepFood() - Realm.popFood(), Integer::sum);
        stock.merge(World.ResourceType.GOLD, -upkeepGold() - Realm.adminCost(), Integer::sum);
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

    /** Чи межує держава f з територіями гравця (війну оголошують лише сусіди). */
    private static boolean touchesPlayer(int f) {
        World.Province[] ps = world.provinces;
        for (World.Province pr : ps) {
            if (pr.owner != 0) continue;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int c = pr.col + dc;
                    int r = pr.row + dr;
                    if (c < 0 || r < 0 || c >= World.COLS || r >= World.ROWS) continue;
                    if (ps[r * World.COLS + c].owner == f) return true;
                }
            }
        }
        return false;
    }

    private static void updateRelations() {
        for (int f = 1; f <= LAST_STATE; f++) {
            if (atWar[f]) {
                relation[f] = Math.max(-100, relation[f] - 1);
            } else if (!allied[f]) {
                if (relation[f] > 0) relation[f]--;
                else if (relation[f] < 0) relation[f]++;
                if (relation[f] < 10 && RNG.nextInt(100) < 4 && touchesPlayer(f)) {
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
        int delta = (warAny ? 1 : 0) + (hungry ? 2 : 0) - 1 - (hasTech(Tech.COUNCIL) ? 1 : 0) + Realm.taxUnrest() + Realm.sprawlUnrest();
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
            double att = (15 + turn * 2.5 + RNG.nextInt(25)) * (0.8 + RNG.nextDouble() * 0.4);
            double def = (target.garrison + armyPower() / 2.0) * (1.0 + 0.1 * (target.level - 1))
                    * (hasTech(Tech.FORTS) ? 1.15 : 1.0) * (hasTech(Tech.CASTLES) ? 1.15 : 1.0)
                    * Realm.wallsMult(prov)
                    * (0.8 + RNG.nextDouble() * 0.4);
            String where = target.type.label + " " + target.name;
            desert(0.04);
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

    // ---------- війни між іншими державами ----------

    private static boolean isState(int o) {
        return o >= 1 && o <= LAST_STATE;
    }

    private static World.Province neighborProvince(World.Province pr) {
        int c = pr.col + RNG.nextInt(3) - 1;
        int r = pr.row + RNG.nextInt(3) - 1;
        if (c < 0 || r < 0 || c >= World.COLS || r >= World.ROWS) return null;
        return world.provinces[r * World.COLS + c];
    }

    private static void aiTurn() {
        aiWars.values().removeIf(end -> end <= turn);

        // нова війна між сусідами
        if (RNG.nextInt(100) < 30) {
            for (int t = 0; t < 60; t++) {
                World.Province pr = world.provinces[RNG.nextInt(world.provinces.length)];
                World.Province q = neighborProvince(pr);
                if (q == null || !isState(pr.owner) || !isState(q.owner) || pr.owner == q.owner) continue;
                int a = Math.min(pr.owner, q.owner);
                int b = Math.max(pr.owner, q.owner);
                int key = a * 100 + b;
                if (aiWars.containsKey(key)) break;
                aiWars.put(key, turn + 10 + RNG.nextInt(20));
                if (pr.explored || q.explored) {
                    addLog(World.FACTIONS[pr.owner].name() + " воює з " + World.FACTIONS[q.owner].name());
                }
                break;
            }
        }
        if (aiWars.isEmpty()) return;

        int[] size = new int[World.FACTIONS.length];
        for (World.Province pr : world.provinces) size[pr.owner]++;

        for (int key : new ArrayList<>(aiWars.keySet())) {
            if (RNG.nextInt(100) >= 60) continue;
            int att = RNG.nextBoolean() ? key / 100 : key % 100;
            int def = att == key / 100 ? key % 100 : key / 100;
            // шукаємо кордон: провінція нападника поруч з провінцією захисника
            for (int t = 0; t < 120; t++) {
                World.Province pr = world.provinces[RNG.nextInt(world.provinces.length)];
                if (pr.owner != att) continue;
                World.Province q = neighborProvince(pr);
                if (q == null || q.owner != def) continue;
                List<World.Point> own = new ArrayList<>();
                for (World.Point p : q.points) {
                    if (p.owner == def) own.add(p);
                }
                if (own.isEmpty()) continue;
                double chance = (size[att] + 1.0) / (size[att] + size[def] + 2.0);
                if (RNG.nextDouble() < chance) {
                    boolean whole = RNG.nextInt(100) < 60;
                    for (World.Point p : whole ? own : own.subList(0, 1)) {
                        p.owner = att;
                        p.garrison = 4;
                    }
                    q.recalcOwner();
                    if (q.explored) {
                        addLog(World.FACTIONS[att].name() + " захопила " + q.name + " у " + World.FACTIONS[def].name());
                    }
                }
                break;
            }
        }
    }

    private static void randomEvent() {
        if (RNG.nextInt(100) >= 35) return;
        switch (RNG.nextInt(8)) {
            case 0 -> {
                if (allied[Countries.CRIMEA]) return;
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
            case 5 -> {
                int g = 25 + 8 * Realm.count(Building.MARKET);
                stock.merge(World.ResourceType.GOLD, g, Integer::sum);
                addLog("Торговий бум: +" + g + " золота");
            }
            case 6 -> {
                stock.merge(World.ResourceType.IRON, 25, Integer::sum);
                addLog("Відкрито родовище: +25 заліза");
            }
            case 7 -> {
                int lost = Realm.population / 12;
                Realm.population = Math.max(30, Realm.population - lost);
                addLog("Епідемія: -" + lost + " населення");
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

    public static int costGold(World.Point p) { return cheaper(40 * p.level); }
    public static int costWood(World.Point p) { return cheaper(15 * p.level); }
    public static int costIron(World.Point p) { return cheaper(5 * p.level); }
    private static int cheaper(int v) { return hasTech(Tech.ROADS) ? (int) Math.ceil(v * 0.8) : v; }

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

    // ---------- флот і морські походи ----------

    public static int maxShips() {
        return hasTech(Tech.SHIPS) ? Realm.ports() * 3 : 0;
    }

    public static int shipCap() {
        return hasTech(Tech.GALLEONS) ? 50 : 25;
    }

    public static int seaRange() {
        return hasTech(Tech.NAVIGATION) ? 16 : 9;
    }

    public static boolean canBuildShip() {
        return ships < maxShips()
                && stock.get(World.ResourceType.GOLD) >= SHIP_GOLD
                && stock.get(World.ResourceType.WOOD) >= SHIP_WOOD
                && stock.get(World.ResourceType.IRON) >= SHIP_IRON;
    }

    public static void buildShip() {
        if (!canBuildShip()) return;
        stock.merge(World.ResourceType.GOLD, -SHIP_GOLD, Integer::sum);
        stock.merge(World.ResourceType.WOOD, -SHIP_WOOD, Integer::sum);
        stock.merge(World.ResourceType.IRON, -SHIP_IRON, Integer::sum);
        ships++;
        addLog("Збудовано чайку. Флот: " + ships);
    }

    /** Чи може флот дістатися цієї провінції: потрібен корабель, порт поруч і берег у цілі. */
    public static boolean seaReach(World.Province target) {
        if (ships <= 0 || target.terrain == World.Terrain.WATER || !Realm.coastal(target)) return false;
        for (World.Province pr : world.provinces) {
            if (pr.owner == 0 && pr.buildings.contains(Building.PORT)
                    && World.dist(pr.col, pr.row, target.col, target.row) <= seaRange()) {
                return true;
            }
        }
        return false;
    }

    /** Сила десанту: скільки війська вміщують кораблі, з поправкою на висадку. */
    public static int landingPower() {
        int carried = Math.min(armyPower(), ships * shipCap());
        return carried * (hasTech(Tech.NAVIGATION) ? 80 : 60) / 100;
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
                && (nextToExplored(target) || seaReach(target))
                && stock.get(World.ResourceType.GOLD) >= spyCost()
                && stock.get(World.ResourceType.HORSES) >= SPY_HORSES;
    }

    public static void sendSpy(World.Province target) {
        if (!canSpy(target)) return;
        stock.merge(World.ResourceType.GOLD, -spyCost(), Integer::sum);
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
        s += Realm.armyBonus();
        return hasTech(Tech.DRILL) ? s * 115 / 100 : s;
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

    public static int attacksPerTurn() {
        return 3 + Realm.count(Building.BARRACKS) / 4;
    }

    public static boolean canAttack(World.Province pr, World.Point p) {
        return attacksLeft > 0 && pr.explored && p.owner != 0 && atWarWith(p) && armyPower() > 0
                && (isNearMine(pr) || seaReach(pr));
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
        attacksLeft--;
        boolean naval = !isNearMine(pr);
        // у похід іде лише частина армії (при десанті — скільки вміщують кораблі), міста захищені краще
        double att = (naval ? landingPower() : armyPower() * 0.6) * (0.8 + RNG.nextDouble() * 0.4);
        double def = p.garrison * (1.0 + 0.15 * (p.level - 1)) * (p.type == World.PointType.TOWN ? 1.4 : 1.0)
                * (1.0 + Realm.ownedProvinces() / 40.0) // чим більша держава, тим важче завойовувати
                * (0.8 + RNG.nextDouble() * 0.4);
        String where = p.type.label + " " + p.name;
        int oldOwner = p.owner;

        if (att > def) {
            double loss = Math.max(0.10, Math.min(0.9, def / att * 0.9));
            desert(loss);
            int caught = Math.max(1, p.garrison / 6);
            prisoners += caught;
            p.owner = 0;
            p.garrison = 3;
            p.unrest = 35;
            pr.recalcOwner();
            revealAroundMine();
            if (oldOwner >= 1 && oldOwner <= LAST_STATE) {
                relation[oldOwner] = Math.max(-100, relation[oldOwner] - 5);
            }
            addLog("Захоплено: " + where + ". Полонених: " + caught);

            boolean all = true;
            for (World.Province q : world.provinces) {
                for (World.Point pt : q.points) {
                    if (pt.owner != 0 && pt.owner != World.WATER_OWNER) all = false;
                }
            }
            if (all) addLog("ПЕРЕМОГА! Усі землі під владою Гетьманщини");
        } else {
            desert(0.6);
            p.garrison = Math.max(2, p.garrison - (int) (att / 4));
            addLog("Поразка: " + where + ". Втрачено 60% війська");
            if (naval && ships > 0) {
                ships--;
                addLog("Десант відбито: корабель потоплено");
            }
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
        int joined = Math.max(1, prisoners / 4);
        army[0] += joined;
        addLog("До війська вступила чверть полонених: +" + joined);
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
