package ua.cossack.empire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class World {
    public static final int TILES = 128;   // карта 128 x 128 тайлів (16 px = 2048 x 2048 px)
    public static final int PS = 4;        // середній розмір провінції: 4 x 4 тайли
    public static final int COLS = TILES / PS;
    public static final int ROWS = TILES / PS; // 32 x 32 = 1024 провінції
    // координати країн у Countries.java задані для сітки 16 x 11 — масштабуємо
    private static final int BASE_COLS = 16;
    private static final int BASE_ROWS = 11;
    private static final double NS = 2.0; // масштаб шуму рельєфу

    private static int scCol(int c) {
        return Math.max(0, Math.min(COLS - 1, (int) Math.round((c + 0.5) * COLS / BASE_COLS - 0.5)));
    }

    private static int scRow(int r) {
        return Math.max(0, Math.min(ROWS - 1, (int) Math.round((r + 0.5) * ROWS / BASE_ROWS - 0.5)));
    }

    public static int startCol() { return scCol(Countries.ALL[0].col()); }
    public static int startRow() { return scRow(Countries.ALL[0].row()); }
    private static final int STATES = Countries.ALL.length;
    public static final int REBELS = STATES;
    public static final int WATER_OWNER = STATES + 1;

    public enum Terrain {
        WATER("Вода", 0xFF2C5F94),
        PLAINS("Рівнина", 0xFF7A9A4A),
        MEADOW("Поляна", 0xFF8DB35A),
        FOREST("Ліс", 0xFF3F6B34),
        MOUNTAIN("Гори", 0xFF7C7C7C),
        STEPPE("Степ", 0xFF9C8B5A);

        public final String label;
        public final int color;
        Terrain(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    public enum ResourceType {
        GOLD("Золото"), FOOD("Їжа"), WOOD("Дерево"),
        IRON("Залізо"), POWDER("Порох"), HORSES("Коні");

        public final String label;
        ResourceType(String label) { this.label = label; }
    }

    public enum PointType {
        TOWN("Місто", ResourceType.GOLD),
        VILLAGE("Село", ResourceType.FOOD),
        FOREST("Ліс", ResourceType.WOOD),
        SMITHY("Кузня", ResourceType.IRON),
        POWDER_WORKS("Порохова", ResourceType.POWDER),
        STEPPE("Степ", ResourceType.HORSES),
        FISHERY("Рибальство", ResourceType.FOOD);

        public final String label;
        public final ResourceType resource;
        PointType(String label, ResourceType resource) {
            this.label = label;
            this.resource = resource;
        }
    }

    public record Faction(String name, String shortName, int color, boolean player) {}

    public static final class Point {
        public final String name;
        public final PointType type;
        public final int output;
        public int owner;
        public int unrest;
        public int level = 1;
        public int garrison;
        public int px;
        public int py;

        Point(String name, PointType type, int output, int owner) {
            this.name = name;
            this.type = type;
            this.output = output;
            this.owner = owner;
        }

        public int production() {
            return output + (level - 1) * Math.max(1, output / 2);
        }
    }

    public static final class Province {
        public final String name;
        public final int col;
        public final int row;
        public int owner;
        public int tx;
        public int ty;
        public boolean explored;
        public Terrain terrain = Terrain.PLAINS;
        public boolean river;
        public int riverNext = -1;
        public final List<Point> points = new ArrayList<>();

        Province(String name, int col, int row, int owner) {
            this.name = name;
            this.col = col;
            this.row = row;
            this.owner = owner;
        }

        public void recalcOwner() {
            int[] cnt = new int[World.FACTIONS.length];
            for (Point p : points) cnt[p.owner]++;
            int best = owner;
            for (int f = 0; f < cnt.length; f++) {
                if (cnt[f] > cnt[best]) best = f;
            }
            owner = best;
        }
    }

    public static final Faction[] FACTIONS = buildFactions();

    private static Faction[] buildFactions() {
        Faction[] f = new Faction[STATES + 2];
        for (int i = 0; i < STATES; i++) {
            Countries.Country c = Countries.ALL[i];
            f[i] = new Faction(c.name(), c.shortName(), c.color(), c.player());
        }
        f[REBELS] = new Faction("Повстанці", "Повстанці", 0xFF8B4513, false);
        f[WATER_OWNER] = new Faction("Вода", "Вода", 0xFF2C5F94, false);
        return f;
    }

    private static final String[] SEA_NAMES = {
        "Північне море", "Балтійське море", "Атлантика", "Біскайська затока", "Середземне море",
        "Адріатика", "Чорне море", "Азовське море", "Каспійське море", "Егейське море", "Біле море"
    };
    private static final int[][] SEA_POS = {
        {2, 0}, {7, 1}, {0, 5}, {0, 8}, {3, 10}, {5, 9}, {11, 8}, {13, 6}, {15, 7}, {8, 10}, {12, 0}
    };

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n < r.length ? r[n] : String.valueOf(n);
    }

    private static final class NameQueue {
        private final List<String> order;
        private int idx;

        NameQueue(String[] base, Random rnd) {
            order = new ArrayList<>(List.of(base));
            Collections.shuffle(order, rnd);
        }

        String next() {
            String s = order.get(idx % order.size());
            int round = idx / order.size();
            idx++;
            return round == 0 ? s : s + " " + roman(round + 1);
        }
    }

    private static final String[][] NAMES = {
        {"Чигирин", "Корсунь", "Біла Церква", "Умань", "Канів", "Черкаси", "Лубни", "Переяслав"},
        {"Суботів", "Мотронівка", "Медведівка", "Бужин", "Лебедин", "Кам'янка", "Вільшана", "Орлик"},
        {"Чорний ліс", "Дубова гай", "Бучина", "Соснівка", "Липова гать", "Грабова"},
        {"Рудня", "Ковалівка", "Залізний брід", "Гамарня", "Ливарня", "Ковальська"},
        {"Селітряна", "Чорний тайник", "Чорна гора", "Сірчана", "Димова", "Вугільна"},
        {"Широка балка", "Біла балка", "Вовча долина", "Кінська левада", "Високий курган", "Ковила"},
        {"Рибальська затока"}
    };

    // ваги типів точок: село, ліс, кузня, порохова, степ — за типом рельєфу
    private static final int[][] WEIGHTS = {
        {0, 0, 0, 0, 0}, // вода
        {4, 1, 1, 2, 2}, // рівнина
        {5, 2, 1, 1, 1}, // поляна
        {2, 5, 1, 1, 0}, // ліс
        {1, 1, 5, 3, 0}, // гори
        {2, 0, 1, 1, 5}  // степ
    };

    public final long seed;
    public final Province[] provinces;
    public final byte[] base = new byte[TILES * TILES];
    public final byte[] over = new byte[TILES * TILES];
    public final short[] provOf = new short[TILES * TILES];

    public World(long seed) {
        this.seed = seed;
        Random rnd = new Random(seed);
        int n = COLS * ROWS;

        // ----- назви (справжні для кожної країни) -----
        NameQueue[] regionQ = new NameQueue[STATES];
        NameQueue[] townQ = new NameQueue[STATES];
        for (int f = 0; f < STATES; f++) {
            regionQ[f] = new NameQueue(Countries.ALL[f].regions(), rnd);
            townQ[f] = new NameQueue(Countries.ALL[f].towns(), rnd);
        }
        int[] seaCount = new int[SEA_NAMES.length];

        // ----- рельєф -----
        double[] elev = new double[n];
        double[] moist = new double[n];
        for (int i = 0; i < n; i++) {
            int col = i % COLS;
            int row = i / COLS;
            double x = col + 0.5 * (row & 1);
            double y = row * 0.87;
            elev[i] = 0.6 * vnoise(seed, 1, x / (4.0 * NS), y / (4.0 * NS))
                    + 0.3 * vnoise(seed, 2, x / (2.0 * NS), y / (2.0 * NS))
                    + 0.1 * vnoise(seed, 3, x, y);
            moist[i] = 0.6 * vnoise(seed, 4, x / (4.0 * NS), y / (4.0 * NS))
                    + 0.3 * vnoise(seed, 5, x / (2.0 * NS), y / (2.0 * NS))
                    + 0.1 * vnoise(seed, 6, x, y);
        }
        double[] se = elev.clone();
        Arrays.sort(se);
        double[] sm = moist.clone();
        Arrays.sort(sm);
        double waterT = se[(int) (n * 0.12)];
        double mountT = se[(int) (n * 0.88)];
        double medianE = se[n / 2];
        double dryT = sm[(int) (n * 0.30)];
        double wetT = sm[(int) (n * 0.68)];

        Terrain[] terr = new Terrain[n];
        for (int i = 0; i < n; i++) {
            if (elev[i] <= waterT) terr[i] = Terrain.WATER;
            else if (elev[i] >= mountT) terr[i] = Terrain.MOUNTAIN;
            else if (moist[i] >= wetT) terr[i] = Terrain.FOREST;
            else if (moist[i] <= dryT) terr[i] = Terrain.STEPPE;
            else terr[i] = elev[i] < medianE ? Terrain.MEADOW : Terrain.PLAINS;
        }
        int centerCol = startCol();
        int centerRow = startRow();
        for (int i = 0; i < n; i++) {
            if (dist(i % COLS, i / COLS, centerCol, centerRow) <= 1 && terr[i] == Terrain.WATER) {
                terr[i] = Terrain.MEADOW;
            }
        }

        for (Countries.Country c : Countries.ALL) {
            int ci = scRow(c.row()) * COLS + scCol(c.col());
            if (terr[ci] == Terrain.WATER) terr[ci] = Terrain.MEADOW;
        }

        // ----- річки -----
        int[] riverNext = new int[n];
        Arrays.fill(riverNext, -1);
        boolean[] isRiver = new boolean[n];
        List<Integer> land = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (terr[i] != Terrain.WATER) land.add(i);
        }
        land.sort((a, b) -> Double.compare(elev[b], elev[a]));
        List<Integer> candidates = new ArrayList<>(land.subList(0, Math.max(1, land.size() / 5)));
        Collections.shuffle(candidates, rnd);
        int sources = Math.min(14, candidates.size());
        for (int s = 0; s < sources; s++) {
            int cur = candidates.get(s);
            for (int step = 0; step < 70; step++) {
                isRiver[cur] = true;
                if (terr[cur] == Terrain.WATER) break;
                int best = -1;
                double bestE = elev[cur];
                for (int j = 0; j < n; j++) {
                    if (dist(cur % COLS, cur / COLS, j % COLS, j / COLS) == 1 && elev[j] < bestE) {
                        bestE = elev[j];
                        best = j;
                    }
                }
                if (best < 0) break;
                riverNext[cur] = best;
                cur = best;
            }
        }

        // ----- держави -----
        double[] weight = new double[STATES];
        for (int f = 0; f < STATES; f++) weight[f] = Countries.ALL[f].weight() + rnd.nextDouble() * 0.15;

        // ----- провінції -----
        provinces = new Province[n];
        for (int i = 0; i < n; i++) {
            int col = i % COLS;
            int row = i / COLS;
            Terrain t = terr[i];

            int owner = WATER_OWNER;
            if (t != Terrain.WATER) {
                double best = Double.MAX_VALUE;
                for (int f = 1; f < STATES; f++) { // Гетьманщина — лише стартовий клаптик гравця
                    double d = dist(col, row, scCol(Countries.ALL[f].col()), scRow(Countries.ALL[f].row())) * weight[f] + rnd.nextDouble() * 0.9 * NS;
                    if (d < best) {
                        best = d;
                        owner = f;
                    }
                }
                if (dist(col, row, centerCol, centerRow) <= 1) owner = 0;
            }

            String provName;
            if (t == Terrain.WATER) {
                int best = 0;
                int bd = Integer.MAX_VALUE;
                for (int k = 0; k < SEA_POS.length; k++) {
                    int dd = dist(col, row, scCol(SEA_POS[k][0]), scRow(SEA_POS[k][1]));
                    if (dd < bd) {
                        bd = dd;
                        best = k;
                    }
                }
                int c = seaCount[best]++;
                provName = c == 0 ? SEA_NAMES[best] : SEA_NAMES[best] + " " + roman(c + 1);
            } else {
                provName = regionQ[owner].next();
            }

            Province pr = new Province(provName, col, row, owner);
            pr.terrain = t;
            pr.river = isRiver[i];
            pr.riverNext = riverNext[i];

            if (t == Terrain.WATER) {
                Point p = new Point("Рибальська затока", PointType.FISHERY, 2, WATER_OWNER);
                pr.points.add(p);
            } else {
                int count = 1 + rnd.nextInt(3);
                for (int j = 0; j < count; j++) {
                    PointType pt = rnd.nextInt(4) == 0 ? PointType.TOWN : pickType(t, rnd);
                    String name;
                    if (pt == PointType.TOWN) {
                        name = townQ[owner].next();
                    } else {
                        String[] pool = NAMES[pt.ordinal()];
                        do {
                            name = pool[rnd.nextInt(pool.length)];
                        } while (usedName(pr, name));
                    }
                    int out = pt == PointType.TOWN ? 6 + rnd.nextInt(10) : 2 + rnd.nextInt(7);
                    Point p = new Point(name, pt, out, owner);
                    p.unrest = rnd.nextInt(30);
                    p.garrison = (pt == PointType.TOWN ? 8 + rnd.nextInt(14) : 3 + rnd.nextInt(9))
                            + (t == Terrain.MOUNTAIN ? 4 : 0);
                    pr.points.add(p);
                }
            }
            provinces[i] = pr;
        }

        for (Province pr : provinces) {
            if (pr.owner != 0) continue;
            for (Province q : provinces) {
                if (dist(pr.col, pr.row, q.col, q.row) <= 1) q.explored = true;
            }
        }
        buildTiles();
    }


    // ---------- тайлова карта ----------

    private boolean waterTile(int t) {
        return provinces[provOf[t]].terrain == Terrain.WATER;
    }

    private void buildTiles() {
        final int T = TILES;
        Random rnd = new Random(seed * 31L + 7L);
        int n = provinces.length;

        // насіння провінцій
        for (Province pr : provinces) {
            pr.tx = pr.col * PS + rnd.nextInt(PS);
            pr.ty = pr.row * PS + rnd.nextInt(PS);
        }

        // Вороной з викривленням країв: кожен тайл належить найближчому насінню
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                double wx = x + 0.5 + (vnoise(seed, 11, x / 5.0, y / 5.0) - 0.5) * 3.0;
                double wy = y + 0.5 + (vnoise(seed, 12, x / 5.0, y / 5.0) - 0.5) * 3.0;
                int cx = Math.max(0, Math.min(COLS - 1, (int) (wx / PS)));
                int cy = Math.max(0, Math.min(ROWS - 1, (int) (wy / PS)));
                int best = cy * COLS + cx;
                double bd = Double.MAX_VALUE;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int ccx = cx + dx;
                        int ccy = cy + dy;
                        if (ccx < 0 || ccy < 0 || ccx >= COLS || ccy >= ROWS) continue;
                        Province q = provinces[ccy * COLS + ccx];
                        double ex = wx - (q.tx + 0.5);
                        double ey = wy - (q.ty + 0.5);
                        double d = ex * ex + ey * ey;
                        if (d < bd) {
                            bd = d;
                            best = ccy * COLS + ccx;
                        }
                    }
                }
                provOf[y * T + x] = (short) best;
            }
        }

        // основа і накладки за типом місцевості провінції
        for (int t = 0; t < T * T; t++) {
            Terrain ter = provinces[provOf[t]].terrain;
            int b;
            int o = 0;
            int r = rnd.nextInt(100);
            switch (ter) {
                case WATER -> b = TileAtlas.WATER_DEEP;
                case PLAINS -> {
                    b = r < 28 ? TileAtlas.TEXGRASS + rnd.nextInt(3) : TileAtlas.GRASS_L;
                    if (r > 94) o = TileAtlas.TREE1 + rnd.nextInt(3);
                    else if (r > 84) o = TileAtlas.WHEAT1 + rnd.nextInt(3);
                }
                case MEADOW -> {
                    b = r < 35 ? TileAtlas.TEXGRASS + 3 + rnd.nextInt(3) : TileAtlas.GRASS_D;
                    if (r > 92) o = TileAtlas.TREE1 + rnd.nextInt(3);
                    else if (r > 82) o = TileAtlas.WHEAT1 + rnd.nextInt(3);
                }
                case FOREST -> {
                    b = r < 25 ? TileAtlas.TEXGRASS + 3 + rnd.nextInt(3) : TileAtlas.GRASS_D;
                    int q = rnd.nextInt(100);
                    if (q < 62) o = q < 20 ? TileAtlas.PINE : TileAtlas.TREE1 + rnd.nextInt(3);
                }
                case MOUNTAIN -> {
                    b = TileAtlas.GRASS_D;
                    if (r < 45) o = TileAtlas.ROCK1 + rnd.nextInt(3);
                }
                default -> { // STEPPE
                    b = TileAtlas.DEADGRASS + rnd.nextInt(6);
                    if (r > 93) o = rnd.nextBoolean() ? (rnd.nextBoolean() ? TileAtlas.CACTUS1 : TileAtlas.CACTUS2) : TileAtlas.BUSH1 + rnd.nextInt(2);
                    else if (r > 88) o = TileAtlas.DEADTREE1 + rnd.nextInt(2);
                }
            }
            base[t] = (byte) b;
            over[t] = (byte) o;
        }

        // дороги: між насінням сусідніх провінцій однієї держави
        for (Province pr : provinces) {
            if (pr.terrain == Terrain.WATER) continue;
            for (int k = 0; k < 2; k++) {
                int nc = pr.col + (k == 0 ? 1 : 0);
                int nr = pr.row + (k == 1 ? 1 : 0);
                if (nc >= COLS || nr >= ROWS) continue;
                Province q = provinces[nr * COLS + nc];
                if (q.terrain == Terrain.WATER || q.owner != pr.owner) continue;
                if (rnd.nextInt(100) < 55) continue;
                line(pr.tx, pr.ty, q.tx, q.ty, (x, y) -> {
                    int t = y * T + x;
                    if (!waterTile(t)) {
                        base[t] = (byte) TileAtlas.SAND;
                        over[t] = 0;
                    }
                });
            }
        }

        // берегова лінія: відстань води до суходолу
        int[] wd = new int[T * T];
        java.util.Arrays.fill(wd, -1);
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (int t = 0; t < T * T; t++) {
            if (!waterTile(t)) {
                wd[t] = 0;
                queue.add(t);
            }
        }
        int[] dx4 = {1, -1, 0, 0};
        int[] dy4 = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int t = queue.poll();
            int x = t % T;
            int y = t / T;
            for (int d = 0; d < 4; d++) {
                int nx = x + dx4[d];
                int ny = y + dy4[d];
                if (nx < 0 || ny < 0 || nx >= T || ny >= T) continue;
                int nt = ny * T + nx;
                if (wd[nt] != -1) continue;
                wd[nt] = wd[t] + 1;
                queue.add(nt);
            }
        }
        for (int t = 0; t < T * T; t++) {
            if (waterTile(t)) {
                int d = wd[t];
                base[t] = (byte) (d == 1 ? TileAtlas.SHORE1 : d == 2 ? TileAtlas.SHORE2
                        : d == 3 ? TileAtlas.WATER_SHALLOW : TileAtlas.WATER_DEEP);
            } else {
                int x = t % T;
                int y = t / T;
                boolean coast = false;
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx4[d];
                    int ny = y + dy4[d];
                    if (nx < 0 || ny < 0 || nx >= T || ny >= T) continue;
                    if (waterTile(ny * T + nx)) coast = true;
                }
                if (coast) {
                    base[t] = (byte) TileAtlas.SAND;
                    over[t] = 0;
                }
            }
        }

        // річки між насінням провінцій
        for (Province pr : provinces) {
            if (pr.riverNext < 0 || pr.terrain == Terrain.WATER) continue;
            Province nx = provinces[pr.riverNext];
            line(pr.tx, pr.ty, nx.tx, nx.ty, (x, y) -> {
                int t = y * T + x;
                if (!waterTile(t)) {
                    base[t] = (byte) TileAtlas.WATER_SHALLOW;
                    over[t] = 0;
                }
            });
        }

        // позиції точок (будівель) у межах провінції
        List<List<Integer>> tilesOf = new ArrayList<>();
        for (int i = 0; i < n; i++) tilesOf.add(new ArrayList<>());
        for (int t = 0; t < T * T; t++) tilesOf.get(provOf[t]).add(t);
        for (int i = 0; i < n; i++) {
            Province pr = provinces[i];
            List<Integer> cand = new ArrayList<>();
            for (int t : tilesOf.get(i)) {
                if (pr.terrain == Terrain.WATER || base[t] > TileAtlas.SHORE1) cand.add(t);
            }
            final int sxx = pr.tx;
            final int syy = pr.ty;
            cand.sort((a, b) -> Integer.compare(
                    Math.abs(a % T - sxx) + Math.abs(a / T - syy), Math.abs(b % T - sxx) + Math.abs(b / T - syy)));
            List<Integer> near = new ArrayList<>(cand.subList(0, Math.min(cand.size(), 10)));
            Collections.shuffle(near, rnd);
            for (int k = 0; k < pr.points.size(); k++) {
                Point p = pr.points.get(k);
                int t = k < near.size() ? near.get(k) : syy * T + sxx;
                p.px = t % T;
                p.py = t / T;
                over[t] = 0;
            }
        }
    }

    private interface TilePlot {
        void plot(int x, int y);
    }

    private static void line(int x0, int y0, int x1, int y1, TilePlot plot) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            if (x >= 0 && y >= 0 && x < TILES && y < TILES) plot.plot(x, y);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static PointType pickType(Terrain t, Random rnd) {
        int[] w = WEIGHTS[t.ordinal()];
        PointType[] types = {PointType.VILLAGE, PointType.FOREST, PointType.SMITHY,
                PointType.POWDER_WORKS, PointType.STEPPE};
        int sum = 0;
        for (int x : w) sum += x;
        if (sum <= 0) return PointType.VILLAGE;
        int k = rnd.nextInt(sum);
        for (int i = 0; i < w.length; i++) {
            if (k < w[i]) return types[i];
            k -= w[i];
        }
        return PointType.VILLAGE;
    }

    private static double lat(long seed, int salt, int xi, int yi) {
        long h = seed * 0x9E3779B97F4A7C15L + xi * 0xC2B2AE3D27D4EB4FL
                + yi * 0x165667B19E3779F9L + salt * 0x27D4EB2F165667C5L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }

    private static double vnoise(long seed, int salt, double x, double y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = lat(seed, salt, x0, y0);
        double b = lat(seed, salt, x0 + 1, y0);
        double c = lat(seed, salt, x0, y0 + 1);
        double d = lat(seed, salt, x0 + 1, y0 + 1);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    private static boolean tooClose(int[][] cap, int f, int c, int r) {
        for (int g = 0; g < f; g++) {
            if (dist(c, r, cap[g][0], cap[g][1]) < 4) return true;
        }
        return false;
    }

    /** Відстань між провінціями на квадратній сітці (8 сусідів). */
    public static int dist(int c1, int r1, int c2, int r2) {
        return Math.max(Math.abs(c1 - c2), Math.abs(r1 - r2));
    }

    private static boolean usedName(Province pr, String name) {
        for (Point p : pr.points) {
            if (p.name.equals(name)) return true;
        }
        return false;
    }
  }
