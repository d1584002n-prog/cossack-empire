package ua.cossack.empire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class World {
    // Фіксована карта Європи: 256 x 200 тайлів (16 px = 4096 x 3200 px), провінції ≈ 4 x 4 тайли
    public static final int TW = EuropeData.TW;
    public static final int TH = EuropeData.TH;
    public static final int PS = 4;
    public static final int COLS = TW / PS;  // 64
    public static final int ROWS = TH / PS;  // 50 => 3200 провінцій

    /** Індекс провінції-столиці держави f (0 — Гетьманщина): клітинка сітки, що містить столицю. */
    public static int capitalIndex(int f) {
        double[] c = EuropeData.CAPITALS[f];
        return (EuropeData.tileY(c[1]) / PS) * COLS + EuropeData.tileX(c[0]) / PS;
    }

    public static int startCol() { return capitalIndex(0) % COLS; }
    public static int startRow() { return capitalIndex(0) / COLS; }
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
        public final java.util.EnumSet<Building> buildings = java.util.EnumSet.noneOf(Building.class);
        // де гравець поставив будівлю: тип -> індекс тайла (y * TW + x)
        public final java.util.EnumMap<Building, Integer> buildingTile = new java.util.EnumMap<>(Building.class);
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
    public final byte[] base = new byte[TW * TH];
    public final byte[] over = new byte[TW * TH];
    public final short[] provOf = new short[TW * TH];
    private final boolean[] tileWater = new boolean[TW * TH]; // вода за маскою береги

    public World(long seed) {
        this.seed = seed;
        Random rnd = new Random(seed);
        final int n = COLS * ROWS;

        // ----- назви міст кожної держави -----
        NameQueue[] townQ = new NameQueue[STATES];
        for (int f = 0; f < STATES; f++) townQ[f] = new NameQueue(Countries.ALL[f].towns(), rnd);

        // ----- маска суходолу (береги Європи) -----
        for (int y = 0; y < TH; y++) {
            for (int x = 0; x < TW; x++) tileWater[y * TW + x] = !EuropeData.land(x, y);
        }

        // ----- насіння провінцій (фіксоване) і Вороной -----
        Random grnd = new Random(0xE0409EL);
        int[] sx = new int[n];
        int[] sy = new int[n];
        for (int i = 0; i < n; i++) {
            sx[i] = (i % COLS) * PS + grnd.nextInt(PS);
            sy[i] = (i / COLS) * PS + grnd.nextInt(PS);
        }
        for (int f = 0; f < STATES; f++) { // столиці стоять точно на своєму місці
            int ci = capitalIndex(f);
            sx[ci] = EuropeData.tileX(EuropeData.CAPITALS[f][0]);
            sy[ci] = EuropeData.tileY(EuropeData.CAPITALS[f][1]);
        }
        fillVoronoi(sx, sy);

        // ----- місцевість -----
        int[] cnt = new int[n];
        int[] wat = new int[n];
        for (int t = 0; t < TW * TH; t++) {
            cnt[provOf[t]]++;
            if (tileWater[t]) wat[provOf[t]]++;
        }
        Terrain[] terr = new Terrain[n];
        for (int i = 0; i < n; i++) {
            if (wat[i] * 100 > cnt[i] * 55) {
                terr[i] = Terrain.WATER;
            } else {
                int b = EuropeData.biome(EuropeData.lonOf(sx[i]), EuropeData.latOf(sy[i]), grnd.nextDouble());
                terr[i] = switch (b) {
                    case 1 -> Terrain.MEADOW;
                    case 2 -> Terrain.FOREST;
                    case 3 -> Terrain.MOUNTAIN;
                    case 4 -> Terrain.STEPPE;
                    default -> Terrain.PLAINS;
                };
            }
        }
        for (int f = 0; f < STATES; f++) {
            if (terr[capitalIndex(f)] == Terrain.WATER) terr[capitalIndex(f)] = Terrain.MEADOW;
        }

        // ----- держави: найближчий (з вагою) центр впливу -----
        int[] owners = new int[n];
        for (int i = 0; i < n; i++) {
            if (terr[i] == Terrain.WATER) {
                owners[i] = WATER_OWNER;
                continue;
            }
            double lon = EuropeData.lonOf(sx[i]);
            double lat = EuropeData.latOf(sy[i]);
            double best = Double.MAX_VALUE;
            int own = 1;
            for (double[] sd : EuropeData.SEEDS) {
                double d = EuropeData.km(lon, lat, sd[1], sd[2]) / sd[3];
                if (d < best) {
                    best = d;
                    own = (int) sd[0];
                }
            }
            owners[i] = own;
            if (dist(i % COLS, i / COLS, startCol(), startRow()) <= 1) owners[i] = 0; // стартовий клаптик гравця
        }

        // ----- назви провінцій за справжніми місцями -----
        String[] names = nameProvinces(terr, sx, sy);

        // ----- провінції -----
        provinces = new Province[n];
        for (int i = 0; i < n; i++) {
            int col = i % COLS;
            int row = i / COLS;
            Terrain t = terr[i];
            int owner = owners[i];
            Province pr = new Province(names[i], col, row, owner);
            pr.terrain = t;
            pr.tx = sx[i];
            pr.ty = sy[i];

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
            pr.explored = true; // туман війни прибрано
            provinces[i] = pr;
        }
        buildTiles();
    }

    /** Назва провінції — найближче справжнє місце; повторні отримують римські цифри. */
    private static String[] nameProvinces(Terrain[] terr, int[] sx, int[] sy) {
        int n = terr.length;
        String[] names = new String[n];
        int P = EuropeData.PLACE_NAMES.length;
        int S = EuropeData.SEA_NAMES.length;
        List<List<int[]>> land = new ArrayList<>();
        List<List<int[]>> sea = new ArrayList<>();
        for (int k = 0; k < P; k++) land.add(new ArrayList<>());
        for (int k = 0; k < S; k++) sea.add(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            double lon = EuropeData.lonOf(sx[i]);
            double lat = EuropeData.latOf(sy[i]);
            boolean water = terr[i] == Terrain.WATER;
            double[][] pos = water ? EuropeData.SEA_POS : EuropeData.PLACE_POS;
            int best = 0;
            double bd = Double.MAX_VALUE;
            for (int k = 0; k < pos.length; k++) {
                double d = EuropeData.km(lon, lat, pos[k][0], pos[k][1]);
                if (d < bd) {
                    bd = d;
                    best = k;
                }
            }
            (water ? sea : land).get(best).add(new int[] {i, (int) bd});
        }
        for (int pass = 0; pass < 2; pass++) {
            List<List<int[]>> groups = pass == 0 ? land : sea;
            String[] labels = pass == 0 ? EuropeData.PLACE_NAMES : EuropeData.SEA_NAMES;
            for (int k = 0; k < groups.size(); k++) {
                List<int[]> g = groups.get(k);
                g.sort((a, b) -> Integer.compare(a[1], b[1]));
                for (int j = 0; j < g.size(); j++) {
                    names[g.get(j)[0]] = j == 0 ? labels[k] : labels[k] + " " + roman(j + 1);
                }
            }
        }
        return names;
    }

    /** Кожен тайл належить найближчому насінню провінції (краї викривлені шумом). */
    private void fillVoronoi(int[] sx, int[] sy) {
        for (int y = 0; y < TH; y++) {
            for (int x = 0; x < TW; x++) {
                double wx = x + 0.5 + (vnoise(4242L, 11, x / 5.0, y / 5.0) - 0.5) * 3.0;
                double wy = y + 0.5 + (vnoise(4242L, 12, x / 5.0, y / 5.0) - 0.5) * 3.0;
                int cx = Math.max(0, Math.min(COLS - 1, (int) (wx / PS)));
                int cy = Math.max(0, Math.min(ROWS - 1, (int) (wy / PS)));
                int best = cy * COLS + cx;
                double bd = Double.MAX_VALUE;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int ccx = cx + dx;
                        int ccy = cy + dy;
                        if (ccx < 0 || ccy < 0 || ccx >= COLS || ccy >= ROWS) continue;
                        int q = ccy * COLS + ccx;
                        double ex = wx - (sx[q] + 0.5);
                        double ey = wy - (sy[q] + 0.5);
                        double d = ex * ex + ey * ey;
                        if (d < bd) {
                            bd = d;
                            best = q;
                        }
                    }
                }
                provOf[y * TW + x] = (short) best;
            }
        }
    }

    // ---------- тайлова карта ----------

    private boolean waterTile(int t) {
        return tileWater[t];
    }

    private void buildTiles() {
        final int T = TW;
        Random rnd = new Random(seed * 31L + 7L);
        int n = provinces.length;

        // суходіл, що потрапив у «водну» провінцію (береги), відходить до найближчої сухопутної провінції
        java.util.ArrayDeque<Integer> fix = new java.util.ArrayDeque<>();
        for (int t = 0; t < T * TH; t++) {
            if (!tileWater[t] && provinces[provOf[t]].terrain != Terrain.WATER) fix.add(t);
        }
        int[] fdx = {1, -1, 0, 0};
        int[] fdy = {0, 0, 1, -1};
        while (!fix.isEmpty()) {
            int t = fix.poll();
            for (int d = 0; d < 4; d++) {
                int nx = t % T + fdx[d];
                int ny = t / T + fdy[d];
                if (nx < 0 || ny < 0 || nx >= T || ny >= TH) continue;
                int nt = ny * T + nx;
                if (!tileWater[nt] && provinces[provOf[nt]].terrain == Terrain.WATER) {
                    provOf[nt] = provOf[t];
                    fix.add(nt);
                }
            }
        }

        // основа і накладки за типом місцевості провінції
        for (int t = 0; t < T * TH; t++) {
            Terrain ter = provinces[provOf[t]].terrain;
            if (tileWater[t]) ter = Terrain.WATER;
            else if (ter == Terrain.WATER) ter = Terrain.PLAINS;
            int b;
            int o = 0;
            int r = rnd.nextInt(100);
            switch (ter) {
                case WATER -> b = TileAtlas.WATER_DEEP;
                case PLAINS -> {
                    b = r < 55 ? TileAtlas.TEXGRASS + rnd.nextInt(3) : TileAtlas.GRASS_L;
                    if (r > 95) o = TileAtlas.TREE1 + rnd.nextInt(3);
                    else if (r > 92) o = TileAtlas.BUSH1 + rnd.nextInt(2);
                    else if (r > 87) o = TileAtlas.FLOWER1 + rnd.nextInt(4);
                    else if (r > 82) o = TileAtlas.WHEAT1 + rnd.nextInt(3);
                }
                case MEADOW -> {
                    b = r < 60 ? TileAtlas.TEXGRASS + 3 + rnd.nextInt(3) : TileAtlas.GRASS_D;
                    if (r > 90) o = TileAtlas.TREE1 + rnd.nextInt(3);
                    else if (r > 84) o = TileAtlas.FLOWER1 + rnd.nextInt(4);
                    else if (r > 76) o = TileAtlas.WHEAT1 + rnd.nextInt(3);
                    else if (r > 72) o = TileAtlas.BUSH1 + rnd.nextInt(2);
                }
                case FOREST -> {
                    // густий ліс суцільними тайлами, як на еталонних картах
                    b = TileAtlas.GRASS_D;
                    int q = rnd.nextInt(100);
                    if (q < 55) o = TileAtlas.FOREST1 + rnd.nextInt(3);
                    else if (q < 75) o = TileAtlas.TREE1 + rnd.nextInt(3);
                    else if (q < 92) o = q < 84 ? TileAtlas.PINE : TileAtlas.PINEF1 + rnd.nextInt(3);
                }
                case MOUNTAIN -> {
                    b = TileAtlas.GRASS_D;
                    o = r < 80 ? TileAtlas.MOUNT1 + rnd.nextInt(4) : TileAtlas.ROCK1 + rnd.nextInt(3);
                }
                default -> { // STEPPE: світла трава з плямами землі та сіном
                    b = r < 18 ? TileAtlas.DEADGRASS + rnd.nextInt(6) : r < 70 ? TileAtlas.TEXGRASS + rnd.nextInt(3) : TileAtlas.GRASS_L;
                    if (r > 95) o = TileAtlas.ROCK1 + rnd.nextInt(3);
                    else if (r > 80) o = TileAtlas.WHEAT3;
                    else if (r > 75) o = TileAtlas.BUSH1 + rnd.nextInt(2);
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
                if (rnd.nextInt(100) < 75) continue;
                line(pr.tx, pr.ty, q.tx, q.ty, (x, y) -> {
                    int t = y * T + x;
                    if (!waterTile(t)) {
                        base[t] = (byte) TileAtlas.PATH;
                        over[t] = 0;
                    }
                });
            }
        }

        // берегова лінія: відстань води до суходолу
        int[] wd = new int[T * TH];
        java.util.Arrays.fill(wd, -1);
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (int t = 0; t < T * TH; t++) {
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
                if (nx < 0 || ny < 0 || nx >= T || ny >= TH) continue;
                int nt = ny * T + nx;
                if (wd[nt] != -1) continue;
                wd[nt] = wd[t] + 1;
                queue.add(nt);
            }
        }
        for (int t = 0; t < T * TH; t++) {
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
                    if (nx < 0 || ny < 0 || nx >= T || ny >= TH) continue;
                    if (waterTile(ny * T + nx)) coast = true;
                }
                if (coast) {
                    base[t] = (byte) TileAtlas.SAND;
                    over[t] = 0;
                }
            }
        }

        // річки за справжніми руслами (Дніпро, Дунай, Рейн, Вісла, Волга, Дон тощо)
        for (double[] rv : EuropeData.RIVERS) {
            final int width = (int) rv[0];
            for (int k = 1; k + 3 < rv.length; k += 2) {
                int x0 = EuropeData.tileX(rv[k]);
                int y0 = EuropeData.tileY(rv[k + 1]);
                int x1 = EuropeData.tileX(rv[k + 2]);
                int y1 = EuropeData.tileY(rv[k + 3]);
                line(x0, y0, x1, y1, (x, y) -> {
                    for (int w = 0; w < width; w++) {
                        int xx = x + (w == 1 ? 1 : 0);
                        int yy = y + (w == 1 ? 1 : 0);
                        if (xx >= TW || yy >= TH) continue;
                        int t = yy * TW + xx;
                        if (!waterTile(t)) {
                            base[t] = (byte) TileAtlas.WATER_SHALLOW;
                            over[t] = 0;
                        }
                    }
                });
            }
        }

        // позиції точок (будівель) у межах провінції
        List<List<Integer>> tilesOf = new ArrayList<>();
        for (int i = 0; i < n; i++) tilesOf.add(new ArrayList<>());
        for (int t = 0; t < T * TH; t++) tilesOf.get(provOf[t]).add(t);
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
                if (pr.terrain != Terrain.WATER) {
                    // стежка від будівлі до центру провінції (до міст завжди, до решти не завжди)
                    if ((p.type == PointType.TOWN && rnd.nextInt(100) < 60) || rnd.nextInt(100) < 10) line(p.px, p.py, sxx, syy, (x, y) -> {
                        int q = y * T + x;
                        if (!waterTile(q) && base[q] > TileAtlas.SHORE1) {
                            base[q] = (byte) TileAtlas.PATH;
                            over[q] = 0;
                        }
                    });
                    // поля навколо сіл і міст
                    if (p.type == PointType.TOWN || p.type == PointType.VILLAGE) {
                        for (int fy = -2; fy <= 2; fy++) {
                            for (int fx = -2; fx <= 2; fx++) {
                                int ax = p.px + fx;
                                int ay = p.py + fy;
                                if (ax < 0 || ay < 0 || ax >= T || ay >= TH || (fx == 0 && fy == 0)) continue;
                                int q = ay * T + ax;
                                if (provOf[q] != i || waterTile(q) || base[q] <= TileAtlas.SAND || base[q] == TileAtlas.PATH) continue;
                                if (rnd.nextInt(100) < 38) over[q] = (byte) (TileAtlas.WHEAT1 + rnd.nextInt(3));
                            }
                        }
                    }
                }
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
            if (x >= 0 && y >= 0 && x < TW && y < TH) plot.plot(x, y);
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
