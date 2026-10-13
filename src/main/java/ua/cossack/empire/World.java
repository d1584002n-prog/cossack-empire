package ua.cossack.empire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class World {
    public static final int COLS = 10;
    public static final int ROWS = 7;
    private static final int STATES = 6;

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
        STEPPE("Степ", ResourceType.HORSES);

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
        new Faction("Повстанці", 0xFF8B4513, false)
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
        {"Широка балка", "Біла балка", "Вовча долина", "Кінська левада", "Високий курган", "Ковила"}
    };

    public final long seed;
    public final Province[] provinces;

    public World(long seed) {
        this.seed = seed;
        Random rnd = new Random(seed);

        List<String> names = new ArrayList<>(List.of(PROVINCE_NAMES));
        Collections.shuffle(names, rnd);

        int[][] cap = new int[STATES][2];
        cap[0] = new int[]{COLS / 2, ROWS / 2};
        for (int f = 1; f < STATES; f++) {
            int c;
            int r;
            int tries = 0;
            do {
                c = rnd.nextInt(COLS);
                r = rnd.nextInt(ROWS);
                tries++;
            } while (tooClose(cap, f, c, r) && tries < 200);
            cap[f] = new int[]{c, r};
        }

        double[] weight = new double[STATES];
        weight[0] = 2.0;
        for (int f = 1; f < STATES; f++) weight[f] = 0.8 + rnd.nextDouble() * 0.6;

        provinces = new Province[COLS * ROWS];
        PointType[] types = PointType.values();

        for (int i = 0; i < provinces.length; i++) {
            int row = i / COLS;
            int col = i % COLS;

            int owner = 0;
            double best = Double.MAX_VALUE;
            for (int f = 0; f < STATES; f++) {
                double d = dist(col, row, cap[f][0], cap[f][1]) * weight[f] + rnd.nextDouble() * 0.9;
                if (d < best) {
                    best = d;
                    owner = f;
                }
            }

            Province pr = new Province(names.get(i % names.size()), col, row, owner);
            int count = 1 + rnd.nextInt(3);
            for (int j = 0; j < count; j++) {
                PointType t = rnd.nextInt(4) == 0
                        ? PointType.TOWN
                        : types[1 + rnd.nextInt(types.length - 1)];
                String[] pool = NAMES[t.ordinal()];
                String name;
                do {
                    name = pool[rnd.nextInt(pool.length)];
                } while (usedName(pr, name));
                int out = t == PointType.TOWN ? 6 + rnd.nextInt(10) : 2 + rnd.nextInt(7);
                Point p = new Point(name, t, out, owner);
                p.unrest = rnd.nextInt(30);
                p.garrison = t == PointType.TOWN ? 8 + rnd.nextInt(14) : 3 + rnd.nextInt(9);
                pr.points.add(p);
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

    private static boolean tooClose(int[][] cap, int f, int c, int r) {
        for (int g = 0; g < f; g++) {
            if (dist(c, r, cap[g][0], cap[g][1]) < 3) return true;
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
