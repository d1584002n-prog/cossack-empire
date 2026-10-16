package ua.cossack.empire;

public enum Tech {
    // Економіка
    FARMING("Тристільне землеробство", "+20% їжі", 0, 1, 60, 15, 3, null),
    CRAFT("Ефективні шахти", "+20% дерева і заліза", 0, 2, 110, 35, 4, FARMING),
    TRADE("Торгові шляхи", "+20% золота", 0, 3, 180, 70, 5, CRAFT),
    // Військова справа
    HORSES("Козацьке конярство", "+25% коней", 1, 1, 60, 15, 3, null),
    POWDER("Порохові млини", "+25% пороху", 1, 2, 130, 40, 4, HORSES),
    DRILL("Мушкетна муштра", "Сила армії +15%", 1, 3, 200, 80, 6, POWDER),
    // Будівництво
    FORTS("Паланки і фортеці", "Захист +15%", 2, 1, 70, 15, 3, null),
    ROADS("Дороги і мости", "Будівництво дешевше на 20%", 2, 2, 110, 35, 4, FORTS),
    CASTLES("Великі замки", "Захист ще +15%", 2, 3, 190, 75, 6, ROADS),
    // Державне управління
    COUNCIL("Козацька рада", "Заворушення -1 за хід", 3, 1, 60, 15, 3, null),
    CENSUS("Перепис і податки", "+20% податків і місць", 3, 2, 120, 35, 4, COUNCIL),
    SPIES("Дипломатія і розвідка", "Шпигун вдвічі дешевший", 3, 3, 160, 65, 5, CENSUS);

    public static final String[] BRANCHES = {"Економіка", "Військова справа", "Будівництво", "Управління"};

    public final String title;
    public final String effect;
    public final int branch;
    public final int level; // 1..3, потребує епохи не нижче цього рівня
    public final int gold;
    public final int science;
    public final int turns;
    public final Tech requires;

    Tech(String title, String effect, int branch, int level, int gold, int science, int turns, Tech requires) {
        this.title = title;
        this.effect = effect;
        this.branch = branch;
        this.level = level;
        this.gold = gold;
        this.science = science;
        this.turns = turns;
        this.requires = requires;
    }
}
