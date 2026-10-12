package ua.cossack.empire;

public final class GameState {
    public record Province(String name, int owner, int gold, int food, int wood) {}

    public static final String[] OWNERS = {"Козаки", "Польща", "Османи", "Нейтральна"};
    public static final int[] OWNER_COLORS = {0xFFC8A200, 0xFFB22222, 0xFF2E8B57, 0xFF777777};

    public static final int COLS = 4;
    public static final int ROWS = 3;

    public static final Province[] PROVINCES = {
        new Province("Волинь", 1, 8, 12, 10),
        new Province("Київщина", 0, 10, 14, 8),
        new Province("Лівобережжя", 3, 6, 10, 9),
        new Province("Слобожанщина", 3, 5, 9, 12),
        new Province("Галичина", 1, 9, 11, 7),
        new Province("Брацлавщина", 1, 7, 13, 6),
        new Province("Запоріжжя", 0, 6, 8, 10),
        new Province("Дике поле", 3, 3, 6, 4),
        new Province("Поділля", 1, 8, 12, 5),
        new Province("Буджак", 2, 7, 9, 3),
        new Province("Очаків", 2, 9, 5, 4),
        new Province("Крим", 2, 12, 7, 3)
    };

    public static int gold = 100;
    public static int food = 50;
    public static int wood = 50;
    public static int turn = 1;

    public static int incomeGold() { return sum(0); }
    public static int incomeFood() { return sum(1); }
    public static int incomeWood() { return sum(2); }

    private static int sum(int type) {
        int total = 0;
        for (Province p : PROVINCES) {
            if (p.owner() != 0) continue;
            total += type == 0 ? p.gold() : type == 1 ? p.food() : p.wood();
        }
        return total;
    }

    public static void nextTurn() {
        gold += incomeGold();
        food += incomeFood();
        wood += incomeWood();
        turn++;
    }

    private GameState() {}
    }
