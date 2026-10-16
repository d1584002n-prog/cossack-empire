package ua.cossack.empire;

public enum Tech {
    FARMING("Тристільне землеробство", "+20% їжі", 70, 0, 3, null),
    CRAFT("Ремесла", "+20% дерева і заліза", 90, 20, 3, null),
    TRADE("Торгові шляхи", "+20% золота", 110, 30, 4, null),
    HORSES("Козацьке конярство", "+25% коней", 90, 20, 3, null),
    POWDER("Порохові млини", "+25% пороху", 140, 40, 4, CRAFT),
    FORTS("Паланки і фортеці", "Захист +15%", 160, 40, 5, CRAFT),
    DRILL("Мушкетна муштра", "Сила армії +15%", 220, 60, 6, POWDER),
    COUNCIL("Козацька рада", "Заворушення -1 за хід", 120, 30, 4, FARMING),
    SPIES("Розвідка", "Шпигун вдвічі дешевший", 70, 20, 3, TRADE);

    public final String title;
    public final String effect;
    public final int gold;
    public final int food;
    public final int turns;
    public final Tech requires;

    Tech(String title, String effect, int gold, int food, int turns, Tech requires) {
        this.title = title;
        this.effect = effect;
        this.gold = gold;
        this.food = food;
        this.turns = turns;
        this.requires = requires;
    }
}
