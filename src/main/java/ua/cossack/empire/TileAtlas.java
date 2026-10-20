package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Атлас спрайтів (tile_atlas.png, 512x512), нарізаний зі спрайт-аркуша автора:
 * тайли ландшафту 16x16 та окремі спрайти (споруди, фортеця, замок, юніти, кораблі, прапори) довільного розміру.
 * Розмір спрайта в атласі = його розмір на карті в одиницях, де 16 = один тайл.
 */
public final class TileAtlas {
    public static final Identifier TEX = Identifier.of("cossack_empire", "tile_atlas.png");
    public static final int AW = 512, AH = 512;
    public static final int TILE = 16;

    // ---- id (групи TEXGRASS.. і DEADGRASS.. займають по 6 підряд) ----
    public static final int WATER_DEEP = 0;
    public static final int WATER_SHALLOW = 1;
    public static final int SHORE2 = 2;
    public static final int SHORE1 = 3;
    public static final int SAND = 4;
    public static final int GRASS_L = 5;
    public static final int GRASS_D = 6;
    public static final int TEXGRASS = 7;
    public static final int DEADGRASS = 13;
    public static final int PATH = 19;
    public static final int TREE1 = 20;
    public static final int TREE2 = 21;
    public static final int TREE3 = 22;
    public static final int PINE = 23;
    public static final int ROCK1 = 24;
    public static final int ROCK2 = 25;
    public static final int ROCK3 = 26;
    public static final int WHEAT1 = 27;
    public static final int WHEAT2 = 28;
    public static final int WHEAT3 = 29;
    public static final int CACTUS1 = 30;
    public static final int CACTUS2 = 31;
    public static final int BUSH1 = 32;
    public static final int BUSH2 = 33;
    public static final int DEADTREE1 = 34;
    public static final int DEADTREE2 = 35;
    public static final int FLOWER1 = 36;
    public static final int FLOWER2 = 37;
    public static final int FLOWER3 = 38;
    public static final int FLOWER4 = 39;
    public static final int FOREST1 = 40;
    public static final int FOREST2 = 41;
    public static final int FOREST3 = 42;
    public static final int PINEF1 = 43;
    public static final int PINEF2 = 44;
    public static final int PINEF3 = 45;
    public static final int MOUNT1 = 46;
    public static final int MOUNT2 = 47;
    public static final int MOUNT3 = 48;
    public static final int MOUNT4 = 49;
    public static final int MILL = 50;
    public static final int SHOP = 51;
    public static final int FARMB = 52;
    public static final int WELL1 = 53;
    public static final int WELL2 = 54;
    public static final int WINDMILL = 55;
    public static final int FORTRESS = 56;
    public static final int CASTLE = 57;
    public static final int HOUSE3D = 58;
    public static final int SHOP3D = 59;
    public static final int MILL3D = 60;
    public static final int CHURCH = 61;
    public static final int PORT3D = 62;
    public static final int BASTION = 63;
    public static final int CAMP = 64;
    public static final int CASTLE3D = 65;
    public static final int UNIT_SWORD = 66;
    public static final int UNIT_RED1 = 67;
    public static final int UNIT_RED2 = 68;
    public static final int UNIT_BOW = 69;
    public static final int UNIT_PIKE = 70;
    public static final int UNIT_MUSK = 71;
    public static final int UNIT_CAV = 72;
    public static final int UNIT_CAV2 = 73;
    public static final int UNIT_CANNON = 74;
    public static final int SHIP_BOAT = 75;
    public static final int SHIP = 76;
    public static final int SHIP_WAR = 77;
    public static final int SHIP_TRADE = 78;
    public static final int SHIP_GALLEY = 79;
    public static final int SHIP_SAIL = 80;
    public static final int BANNER_BLUE = 81;
    public static final int BANNER_RED = 82;

    public static final int COUNT = 83;
    private static final int[] U = new int[COUNT];
    private static final int[] V = new int[COUNT];
    private static final int[] W = new int[COUNT];
    private static final int[] H = new int[COUNT];

    private static void set(int id, int u, int v, int w, int h) {
        U[id] = u;
        V[id] = v;
        W[id] = w;
        H[id] = h;
    }

    static {
        set(0, 0, 0, 16, 16);
        set(1, 16, 0, 16, 16);
        set(2, 32, 0, 16, 16);
        set(3, 48, 0, 16, 16);
        set(4, 64, 0, 16, 16);
        set(5, 80, 0, 16, 16);
        set(6, 96, 0, 16, 16);
        set(7, 112, 0, 16, 16);
        set(8, 128, 0, 16, 16);
        set(9, 144, 0, 16, 16);
        set(10, 160, 0, 16, 16);
        set(11, 176, 0, 16, 16);
        set(12, 192, 0, 16, 16);
        set(13, 208, 0, 16, 16);
        set(14, 224, 0, 16, 16);
        set(15, 240, 0, 16, 16);
        set(16, 256, 0, 16, 16);
        set(17, 272, 0, 16, 16);
        set(18, 288, 0, 16, 16);
        set(19, 304, 0, 16, 16);
        set(20, 320, 0, 16, 16);
        set(21, 336, 0, 16, 16);
        set(22, 352, 0, 16, 16);
        set(23, 368, 0, 16, 16);
        set(24, 384, 0, 16, 16);
        set(25, 400, 0, 16, 16);
        set(26, 416, 0, 16, 16);
        set(27, 432, 0, 16, 16);
        set(28, 448, 0, 16, 16);
        set(29, 464, 0, 16, 16);
        set(30, 480, 0, 16, 16);
        set(31, 496, 0, 16, 16);
        set(32, 0, 16, 16, 16);
        set(33, 16, 16, 16, 16);
        set(34, 32, 16, 16, 16);
        set(35, 48, 16, 16, 16);
        set(36, 64, 16, 16, 16);
        set(37, 80, 16, 16, 16);
        set(38, 96, 16, 16, 16);
        set(39, 112, 16, 16, 16);
        set(40, 128, 16, 16, 16);
        set(41, 144, 16, 16, 16);
        set(42, 160, 16, 16, 16);
        set(43, 176, 16, 16, 16);
        set(44, 192, 16, 16, 16);
        set(45, 208, 16, 16, 16);
        set(46, 224, 16, 16, 16);
        set(47, 240, 16, 16, 16);
        set(48, 256, 16, 16, 16);
        set(49, 272, 16, 16, 16);
        set(50, 0, 32, 26, 30);
        set(51, 27, 32, 29, 28);
        set(52, 57, 32, 35, 26);
        set(53, 93, 32, 17, 26);
        set(54, 111, 32, 22, 24);
        set(55, 134, 32, 27, 32);
        set(56, 162, 32, 61, 50);
        set(57, 224, 32, 37, 46);
        set(58, 262, 32, 22, 24);
        set(59, 285, 32, 33, 26);
        set(60, 319, 32, 26, 30);
        set(61, 346, 32, 19, 34);
        set(62, 366, 32, 25, 30);
        set(63, 392, 32, 26, 26);
        set(64, 419, 32, 26, 28);
        set(65, 446, 32, 42, 40);
        set(66, 489, 32, 7, 17);
        set(67, 497, 32, 8, 17);
        set(68, 0, 83, 8, 17);
        set(69, 9, 83, 7, 17);
        set(70, 17, 83, 7, 18);
        set(71, 25, 83, 7, 18);
        set(72, 33, 83, 16, 22);
        set(73, 50, 83, 22, 22);
        set(74, 73, 83, 22, 14);
        set(75, 96, 83, 17, 24);
        set(76, 114, 83, 34, 40);
        set(77, 149, 83, 40, 44);
        set(78, 190, 83, 37, 44);
        set(79, 228, 83, 33, 40);
        set(80, 262, 83, 29, 42);
        set(81, 292, 83, 12, 20);
        set(82, 305, 83, 10, 20);
    }

    public static boolean isWaterTile(int id) {
        return id <= SHORE1;
    }

    public static int spriteW(int id) { return W[id]; }
    public static int spriteH(int id) { return H[id]; }

    /** Малює спрайт id у (x, y), розтягуючи його до w x h (у поточних координатах матриці). */
    public static void draw(DrawContext g, int id, int x, int y, int w, int h, int color) {
        g.drawTexture(RenderLayer::getGuiTextured, TEX, x, y, U[id], V[id], w, h, W[id], H[id], AW, AH, color);
    }

    /** Малює спрайт у власному розмірі, прив'язаний до низу середини тайла (tx, ty); координати в одиницях тайла (16 = тайл). */
    public static void drawSprite(DrawContext g, int id, int tx, int ty, int color) {
        int x = tx * TILE + TILE / 2 - W[id] / 2;
        int y = ty * TILE + TILE - H[id];
        draw(g, id, x, y, W[id], H[id], color);
    }

    /** Малює спрайт, вписаний у квадрат size x size (для іконок в інтерфейсі). */
    public static void drawIcon(DrawContext g, int id, int x, int y, int size, int color) {
        float s = Math.min((float) size / W[id], (float) size / H[id]);
        int w = Math.max(1, Math.round(W[id] * s));
        int h = Math.max(1, Math.round(H[id] * s));
        draw(g, id, x + (size - w) / 2, y + (size - h) / 2, w, h, color);
    }

    public static int pointSprite(World.Point p) {
        int hsh = Math.floorMod(p.px * 7 + p.py * 3, 6);
        return switch (p.type) {
            case TOWN -> hsh % 2 == 0 ? HOUSE3D : SHOP3D;
            case VILLAGE -> hsh % 3 == 0 ? WELL1 : HOUSE3D;
            case SMITHY -> CAMP;
            case POWDER_WORKS -> WELL2;
            case FISHERY -> PORT3D;
            case FOREST -> PINE;
            case STEPPE -> WHEAT3;
        };
    }

    /** Спрайт збудованої будівлі. */
    public static int buildingSprite(Building b) {
        return switch (b) {
            case FARM -> FARMB;
            case MARKET -> SHOP;
            case MINE -> WELL2;
            case WALLS -> BASTION;
            case BARRACKS -> CAMP;
            case LIBRARY -> CHURCH;
            case PORT -> PORT3D;
            case FORGE -> MILL;
            case FORTRESS -> FORTRESS;
        };
    }

    private TileAtlas() {}
}
