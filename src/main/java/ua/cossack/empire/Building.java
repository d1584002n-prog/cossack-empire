package ua.cossack.empire;

public enum Building {
    FARM("Ферма", "+6 їжі", 40, 15, 0, 1),
    MARKET("Ринок", "+6 золота, вигідніша торгівля", 50, 20, 0, 1),
    MINE("Шахта", "+3 заліза, +3 дерева", 45, 10, 5, 1),
    WALLS("Стіни", "Захист провінції +25%", 60, 30, 10, 1),
    BARRACKS("Казарми", "+2 сили армії, +3 коней", 70, 25, 10, 2),
    LIBRARY("Бібліотека", "+3 науки за хід", 80, 25, 0, 2),
    PORT("Порт", "+4 золота, +4 їжі (біля моря)", 90, 40, 10, 2),
    FORGE("Кузня", "+4 пороху, +1 заліза", 110, 20, 25, 3);

    public final String label;
    public final String effect;
    public final int gold;
    public final int wood;
    public final int iron;
    public final int epoch;

    Building(String label, String effect, int gold, int wood, int iron, int epoch) {
        this.label = label;
        this.effect = effect;
        this.gold = gold;
        this.wood = wood;
        this.iron = iron;
        this.epoch = epoch;
    }
}
