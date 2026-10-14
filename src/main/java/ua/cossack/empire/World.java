package ua.cossack.empire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class World {
    public static final int COLS = 16;
    public static final int ROWS = 11;
    public static final int WATER_OWNER = 7;
    private static final int STATES = 6;

    public enum Terrain {
        WATER("Вода", 0xFF2C5F94),
        PLAINS("Рівнина", 0xFF7A9A4A),
        MEADOW("Поляна", 0xFF8DB35A),
        FOREST("Ліс", 0xFF3F6B34),
        MOUNTAIN("Гори", 0xFF7C7C7C),
        STEPPE("Степ", 0xFFB5A44F);

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

    public record Faction(String name, int color, boolean player) {}

    public static final class Point {
        public final String name;
        public final PointType type;
        public final int output;
        public int owner;
        public int unrest;
        public int level = 1;
        public int garrison;

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

    public static final Faction[] FACTIONS = {
        new Faction("Козаки", 0xFFC8A200, true),
        new Faction("Польща", 0xFFB22222, false),
        new Faction("Московія", 0xFF7B4FA3, false),
        new Faction("Османи", 0xFF2E8B57, false),
        new Faction("Кримське ханство", 0xFF2F7FB5, false),
        new Faction("Швеція", 0xFF4FA3A3, false),
        new Faction("Повстанці", 0xFF8B4513, false),
        new Faction("Вода", 0xFF2C5F94, false)
    };

    private static final String[] PROVINCE_NAMES = {
        "Волинь", "Київщина", "Лівобережжя", "Слобожанщина", "Галичина",
        "Брацлавщина", "Запоріжжя", "Дике поле", "Поділля", "Буджак",
        "Очаків", "Крим", "Полтавщина", "Чернігівщина", "Сіверщина",
        "Покуття", "Закарпаття", "Буковина", "Полісся", "Побужжя",
        "Подніпров'я", "Степова", "Донеччина", "Азовщина", "Таврія",
        "Херсонщина", "Одещина", "Миколаївщина", "Черкащина", "Уманщина",
        "Сумщина", "Харківщина", "Луганщина", "Холмщина", "Підляшшя",
        "Берестейщина", "Турівщина", "Гетьманщина", "Переяславщина", "Лубенщина",
        "Миргородщина", "Ніжинщина", "Корсунщина", "Чигиринщина", "Вінниччина",
        "Житомирщина", "Рівненщина", "Луцьчина", "Львівщина", "Тернопільщина",
        "Бессарабія", "Молдова", "Мазовія", "Литва", "Смоленщина",
        "Курщина", "Брянщина", "Орловщина", "Воронежчина", "Ростовщина",
        "Кубань", "Тамань", "Кабарда", "Валахія", "Трансильванія",
        "Сілезія", "Малопольща", "Познань", "Сяноччина", "Перемишльщина"
    };

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

    public World(long seed) {
        this.seed = seed;
        Random rnd = new Random(seed);
        int n = COLS * ROWS;

        // ----- назви -----
        List<String> names = new ArrayList<>();
        List<String> base = new ArrayList<>(List.of(PROVINCE_NAMES));
        String[] suffix = {"", " II", " III"};
        for (int c = 0; names.size() < n; c++) {
            Collections.shuffle(base, rnd);
            for (String nm : base) {
                if (names.size() < n) names.add(nm + suffix[Math.min(c, 2)]);
            }
        }
        Collections.shuffle(names, rnd);

        // ----- рельєф -----
        double[] elev = new double[n];
        double[] moist = new double[n];
        for (int i = 0; i < n; i++) {
            int col = i % COLS;
            int row = i / COLS;
            double x = col + 0.5 * (row & 1);
            double y = row * 0.87;
            elev[i] = 0.6 * vnoise(seed, 1, x / 4.0, y / 4.0)
                    + 0.3 * vnoise(seed, 2, x / 2.0, y / 2.0)
                    + 0.1 * vnoise(seed, 3, x, y);
            moist[i] = 0.6 * vnoise(seed, 4, x / 4.0, y / 4.0)
                    + 0.3 * vnoise(seed, 5, x / 2.0, y / 2.0)
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
        int centerCol = COLS / 2;
        int centerRow = ROWS / 2;
        for (int i = 0; i < n; i++) {
            if (dist(i % COLS, i / COLS, centerCol, centerRow) <= 1 && terr[i] == Terrain.WATER) {
                terr[i] = Terrain.MEADOW;
            }
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
        int sources = Math.min(5, candidates.size());
        for (int s = 0; s < sources; s++) {
            int cur = candidates.get(s);
            for (int step = 0; step < 30; step++) {
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
        int[][] cap = new int[STATES][2];
        cap[0] = new int[]{centerCol, centerRow};
        for (int f = 1; f < STATES; f++) {
            int c;
            int r;
            int tries = 0;
            do {
                c = rnd.nextInt(COLS);
                r = rnd.nextInt(ROWS);
                tries++;
            } while ((terr[r * COLS + c] == Terrain.WATER || tooClose(cap, f, c, r)) && tries < 400);
            cap[f] = new int[]{c, r};
        }
        double[] weight = new double[STATES];
        weight[0] = 3.0;
        for (int f = 1; f < STATES; f++) weight[f] = 0.8 + rnd.nextDouble() * 0.6;

        // ----- провінції -----
        provinces = new Province[n];
        for (int i = 0; i < n; i++) {
            int col = i % COLS;
            int row = i / COLS;
            Terrain t = terr[i];

            int owner = WATER_OWNER;
            if (t != Terrain.WATER) {
                double best = Double.MAX_VALUE;
                for (int f = 0; f < STATES; f++) {
                    double d = dist(col, row, cap[f][0], cap[f][1]) * weight[f] + rnd.nextDouble() * 0.9;
                    if (d < best) {
                        best = d;
                        owner = f;
                    }
                }
            }

            Province pr = new Province(names.get(i), col, row, owner);
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
                    String[] pool = NAMES[pt.ordinal()];
                    String name;
                    do {
                        name = pool[rnd.nextInt(pool.length)];
                    } while (usedName(pr, name));
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

    public static int dist(int c1, int r1, int c2, int r2) {
        int x1 = c1 - (r1 - (r1 & 1)) / 2;
        int z1 = r1;
        int y1 = -x1 - z1;
        int x2 = c2 - (r2 - (r2 & 1)) / 2;
        int z2 = r2;
        int y2 = -x2 - z2;
        return Math.max(Math.abs(x1 - x2), Math.max(Math.abs(y1 - y2), Math.abs(z1 - z2)));
    }

    private static boolean usedName(Province pr, String name) {
        for (Point p : pr.points) {
            if (p.name.equals(name)) return true;
        }
        return false;
    }
        }
