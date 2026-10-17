package ua.cossack.empire;

/** Стан держави гравця: населення, податки, наука, епохи, будівлі та торгівля. */
public final class Realm {
    public static final String[] EPOCH_NAMES = {"", "Раннє середньовіччя", "Королівська доба", "Порохова доба"};
    public static final String[] TAX_NAMES = {"Низькі", "Звичайні", "Високі", "Жорсткі"};
    private static final int[] TAX_RATE = {0, 5, 9, 14}; // % населення золотом за хід
    private static final String[] TIER_NAMES = {"", "Село", "Містечко", "Велике місто", "Столиця"};
    private static final int[] TIER_CAP = {0, 60, 120, 220, 300};

    // епоха 2 і 3: золото, наука, мінімум технологій
    private static final int[] EPOCH_GOLD = {0, 0, 180, 350};
    private static final int[] EPOCH_SCIENCE = {0, 0, 40, 120};
    private static final int[] EPOCH_TECHS = {0, 0, 3, 7};

    // торгівля: порядок як у ResourceType (золото, їжа, дерево, залізо, порох, коні)
    private static final int[] BUY = {0, 2, 3, 5, 7, 7};
    private static final int[] SELL = {0, 1, 1, 2, 3, 3};
    public static final int TRADE_LOT = 5;

    public static int population;
    public static int science;
    public static int taxLevel;
    public static int epoch;

    private Realm() {}

    public static void reset() {
        population = 120;
        science = 30;
        taxLevel = 1;
        epoch = 1;
    }

    // ---------- провінції та будівлі ----------

    public static boolean isMine(World.Province pr) {
        return pr.owner == 0 && pr.terrain != World.Terrain.WATER;
    }

    public static int count(Building b) {
        int n = 0;
        for (World.Province pr : GameState.world.provinces) {
            if (pr.owner == 0 && pr.buildings.contains(b)) n++;
        }
        return n;
    }

    public static boolean isCapital(World.Province pr) {
        return pr.col == World.startCol() && pr.row == World.startRow();
    }

    public static int tier(World.Province pr) {
        if (isCapital(pr)) return 4;
        int n = pr.buildings.size();
        return n >= 5 ? 3 : n >= 2 ? 2 : 1;
    }

    public static String tierName(World.Province pr) {
        return TIER_NAMES[tier(pr)];
    }

    public static boolean coastal(World.Province pr) {
        for (World.Province q : GameState.world.provinces) {
            if (q.terrain == World.Terrain.WATER && World.dist(pr.col, pr.row, q.col, q.row) <= 1) return true;
        }
        return false;
    }

    public static int popCap() {
        int cap = 0;
        for (World.Province pr : GameState.world.provinces) {
            if (isMine(pr)) cap += TIER_CAP[tier(pr)];
        }
        return cap * (GameState.hasTech(Tech.CENSUS) ? 120 : 100) / 100;
    }

    // ---------- вартість і будівництво ----------

    private static int scaled(int base) {
        return GameState.hasTech(Tech.ROADS) ? (int) Math.ceil(base * 0.8) : base;
    }

    public static int costGold(Building b) { return scaled(b.gold); }
    public static int costWood(Building b) { return scaled(b.wood); }
    public static int costIron(Building b) { return scaled(b.iron); }

    public static boolean canBuild(World.Province pr, Building b) {
        return isMine(pr)
                && !pr.buildings.contains(b)
                && b.epoch <= epoch
                && (b != Building.PORT || coastal(pr))
                && GameState.stock.get(World.ResourceType.GOLD) >= costGold(b)
                && GameState.stock.get(World.ResourceType.WOOD) >= costWood(b)
                && GameState.stock.get(World.ResourceType.IRON) >= costIron(b);
    }

    private static int indexOf(World.Province pr) {
        return pr.row * World.COLS + pr.col;
    }

    /** Чи можна поставити будівлю b на тайл провінції pr (суша, вільне місце, порт — біля води). */
    public static boolean canPlace(World.Province pr, Building b, int tile) {
        World w = GameState.world;
        final int T = World.TILES;
        if (tile < 0 || tile >= T * T || w.provOf[tile] != indexOf(pr)) return false;
        if (w.base[tile] <= TileAtlas.SHORE1) return false; // вода чи річка
        for (World.Point p : pr.points) {
            if (p.py * T + p.px == tile) return false;
        }
        for (int t : pr.buildingTile.values()) {
            if (t == tile) return false;
        }
        if (b == Building.PORT) {
            int x = tile % T;
            int y = tile / T;
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            for (int d = 0; d < 4; d++) {
                int nx = x + dx[d];
                int ny = y + dy[d];
                if (nx >= 0 && ny >= 0 && nx < T && ny < T && w.base[ny * T + nx] <= TileAtlas.SHORE1) return true;
            }
            return false;
        }
        return true;
    }

    /** Будує b саме на тайлі tile (гравець сам обирає місце). */
    public static boolean buildAt(World.Province pr, Building b, int tile) {
        if (!canBuild(pr, b) || !canPlace(pr, b, tile)) return false;
        GameState.stock.merge(World.ResourceType.GOLD, -costGold(b), Integer::sum);
        GameState.stock.merge(World.ResourceType.WOOD, -costWood(b), Integer::sum);
        GameState.stock.merge(World.ResourceType.IRON, -costIron(b), Integer::sum);
        pr.buildings.add(b);
        pr.buildingTile.put(b, tile);
        GameState.addLog("Збудовано: " + b.label + " (" + pr.name + ")");
        return true;
    }

    /** Автоматично обирає найближче до центру провінції місце (для екрана «Міста»). */
    public static void build(World.Province pr, Building b) {
        if (!canBuild(pr, b)) return;
        World w = GameState.world;
        final int T = World.TILES;
        int best = -1;
        int bestD = Integer.MAX_VALUE;
        for (int t = 0; t < T * T; t++) {
            if (w.provOf[t] != indexOf(pr) || !canPlace(pr, b, t)) continue;
            int d = Math.abs(t % T - pr.tx) + Math.abs(t / T - pr.ty);
            if (d < bestD) {
                bestD = d;
                best = t;
            }
        }
        if (best >= 0) buildAt(pr, b, best);
    }

    // ---------- дохід ----------

    public static int buildingIncome(World.ResourceType r) {
        int total = 0;
        for (World.Province pr : GameState.world.provinces) {
            if (pr.owner != 0 || pr.buildings.isEmpty()) continue;
            for (Building b : pr.buildings) {
                switch (b) {
                    case FARM -> { if (r == World.ResourceType.FOOD) total += 6; }
                    case MARKET -> { if (r == World.ResourceType.GOLD) total += 6; }
                    case MINE -> { if (r == World.ResourceType.IRON || r == World.ResourceType.WOOD) total += 3; }
                    case BARRACKS -> { if (r == World.ResourceType.HORSES) total += 3; }
                    case FORGE -> {
                        if (r == World.ResourceType.POWDER) total += 4;
                        if (r == World.ResourceType.IRON) total += 1;
                    }
                    case PORT -> {
                        if ((r == World.ResourceType.GOLD || r == World.ResourceType.FOOD) && coastal(pr)) total += 4;
                    }
                    default -> { }
                }
            }
        }
        return total;
    }

    public static int taxIncome() {
        return population * TAX_RATE[taxLevel] / 100 * (GameState.hasTech(Tech.CENSUS) ? 120 : 100) / 100;
    }

    public static int scienceIncome() {
        return 2 + count(Building.LIBRARY) * 3 + population / 50;
    }

    public static int popFood() {
        return population / 25;
    }

    public static int ports() {
        return count(Building.PORT);
    }

    /** Морська торгівля: кожна пара порт + корабель приносить золото. */
    public static int seaTrade() {
        return Math.min(ports(), GameState.ships) * 6;
    }

    public static int armyBonus() {
        return count(Building.BARRACKS) * 2 + count(Building.FORTRESS) * 3;
    }

    public static double wallsMult(World.Province pr) {
        double m = pr.buildings.contains(Building.WALLS) ? 1.25 : 1.0;
        return pr.buildings.contains(Building.FORTRESS) ? m * 1.5 : m;
    }

    public static int ownedProvinces() {
        int n = 0;
        for (World.Province pr : GameState.world.provinces) {
            if (isMine(pr)) n++;
        }
        return n;
    }

    /** Чим більша держава, тим більше невдоволення: ~+1 за кожні 12 провінцій понад 18. */
    public static int sprawlUnrest() {
        return Math.max(0, (ownedProvinces() - 18) / 12);
    }

    /** Витрати на управління: 3 золота за кожну провінцію понад 20. */
    public static int adminCost() {
        return Math.max(0, ownedProvinces() - 20) * 3;
    }

    public static int taxUnrest() {
        return taxLevel - 1;
    }

    /** Викликається щоходу після виплат: ріст або спад населення, наука. */
    public static void turn() {
        if (GameState.stock.get(World.ResourceType.FOOD) >= 10) {
            int growth = Math.max(1, population * (GameState.hasTech(Tech.CENSUS) ? 5 : 3) / 100);
            population = Math.min(popCap(), population + growth);
        } else {
            population = Math.max(30, population - population * 3 / 100);
        }
        science += scienceIncome();
    }

    // ---------- епохи ----------

    public static int techsDone() {
        int n = 0;
        for (Tech t : Tech.values()) if (GameState.hasTech(t)) n++;
        return n;
    }

    public static boolean maxEpoch() { return epoch >= 3; }
    public static int nextEpochGold() { return maxEpoch() ? 0 : EPOCH_GOLD[epoch + 1]; }
    public static int nextEpochScience() { return maxEpoch() ? 0 : EPOCH_SCIENCE[epoch + 1]; }
    public static int nextEpochTechs() { return maxEpoch() ? 0 : EPOCH_TECHS[epoch + 1]; }

    public static boolean canAdvanceEpoch() {
        return !maxEpoch()
                && GameState.stock.get(World.ResourceType.GOLD) >= nextEpochGold()
                && science >= nextEpochScience()
                && techsDone() >= nextEpochTechs();
    }

    public static void advanceEpoch() {
        if (!canAdvanceEpoch()) return;
        GameState.stock.merge(World.ResourceType.GOLD, -nextEpochGold(), Integer::sum);
        science -= nextEpochScience();
        epoch++;
        GameState.addLog("Нова епоха: " + EPOCH_NAMES[epoch]);
    }

    // ---------- торгівля ----------

    private static int marketDiscount() {
        return 5 * Math.min(4, count(Building.MARKET));
    }

    public static int buyPrice(World.ResourceType r) {
        return (int) Math.ceil(TRADE_LOT * BUY[r.ordinal()] * (100 - marketDiscount()) / 100.0);
    }

    public static int sellPrice(World.ResourceType r) {
        return TRADE_LOT * SELL[r.ordinal()] * (100 + marketDiscount()) / 100;
    }

    public static boolean canBuy(World.ResourceType r) {
        return r != World.ResourceType.GOLD && GameState.stock.get(World.ResourceType.GOLD) >= buyPrice(r);
    }

    public static boolean canSell(World.ResourceType r) {
        return r != World.ResourceType.GOLD && GameState.stock.get(r) >= TRADE_LOT;
    }

    public static void buy(World.ResourceType r) {
        if (!canBuy(r)) return;
        GameState.stock.merge(World.ResourceType.GOLD, -buyPrice(r), Integer::sum);
        GameState.stock.merge(r, TRADE_LOT, Integer::sum);
    }

    public static void sell(World.ResourceType r) {
        if (!canSell(r)) return;
        GameState.stock.merge(r, -TRADE_LOT, Integer::sum);
        GameState.stock.merge(World.ResourceType.GOLD, sellPrice(r), Integer::sum);
    }
}
