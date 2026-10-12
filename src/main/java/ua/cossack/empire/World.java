package ua.cossack.empire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class World {

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

        Point(String name, PointType type, int output, int owner) {
            this.name = name;
            this.type = type;
            this.output = output;
            this.owner = owner;
        }
    }

    public static final class Province {
        public final String name;
        public int owner;
        public final List<Point> points = new ArrayList<>();

        Province(String name, int owner) {
            this.name = name;
            this.owner = owner;
        }
    }

    public static final Faction[] FACTIONS = {
        new Faction("Козаки", 0xFFC8A200, true),
        new Faction("Польща", 0xFFB22222, false),
        new Faction("Московія", 0xFF7B4FA3, false),
        new Faction("Османи", 0xFF2E8B57, false),
        new Faction("Кримське ханство", 0xFF2F7FB5, false),
        new Faction("Повстанці", 0xFF8B4513, false)
    };

    private static final String[] PROVINCE_NAMES = {
        "Волинь", "Київщина", "Лівобережжя", "Слобожанщина",
        "Галичина", "Брацлавщина", "Запоріжжя", "Дике поле",
        "Поділля", "Буджак", "Очаків", "Крим"
    };

    private static final String[][] NAMES = {
        {"Чигирин", "Корсунь", "Біла Церква", "Умань", "Канів", "Черкаси", "Лубни", "Переяслав"},
        {"Суботів", "Мотронівка", "Медведівка", "Бужин", "Лебедин", "Кам'янка", "Вільшана", "Орлик"},
        {"Чорний ліс", "Дубова гай", "Бучина", "Соснівка", "Липова гать", "Грабова"},
        {"Рудня", "Ковалівка", "Залізний брід", "Гамарня", "Ливарня", "Кузня Ковалів"},
        {"Селітряна", "Порохівня", "Чорна гора", "Сірчана", "Димова", "Вугільна"},
        {"Широка балка", "Біла балка", "Вовча долина", "Кінська левада", "Високий курган", "Ковила"}
    };

    public final long seed;
    public final Province[] provinces;

    public World(long seed) {
        this.seed = seed;
        Random rnd = new Random(seed);

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < PROVINCE_NAMES.length; i++) order.add(i);
        Collections.shuffle(order, rnd);

        int[] owners = new int[PROVINCE_NAMES.length];
        for (int k = 0; k < order.size(); k++) {
            owners[order.get(k)] = k < 3 ? 0 : 1 + (k - 3) % 4;
        }

        provinces = new Province[PROVINCE_NAMES.length];
        PointType[] types = PointType.values();

        for (int i = 0; i < provinces.length; i++) {
            Province pr = new Province(PROVINCE_NAMES[i], owners[i]);
            int count = 3 + rnd.nextInt(4);
            for (int j = 0; j < count; j++) {
                PointType t = j == 0 ? PointType.TOWN : types[1 + rnd.nextInt(types.length - 1)];
                String[] pool = NAMES[t.ordinal()];
                String name = pool[rnd.nextInt(pool.length)];
                int out = t == PointType.TOWN ? 6 + rnd.nextInt(10) : 2 + rnd.nextInt(7);
                Point p = new Point(name, t, out, owners[i]);
                p.unrest = rnd.nextInt(30);
                pr.points.add(p);
            }
            provinces[i] = pr;
        }
    }
}
