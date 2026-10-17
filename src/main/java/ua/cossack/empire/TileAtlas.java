package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Атлас тайлів у стилі Minecraft (tile_atlas.png, 256x256): текстури 16x16 намальовані з нуля
 * (блокові палітри трави, води, піску, дерева, каменю, дошок). Фортеця займає 48x48 (3x3 тайли).
 * Кожен тайл має числовий id; World зберігає id для кожної клітинки карти.
 */
public final class TileAtlas {
    public static final Identifier TEX = Identifier.of("cossack_empire", "tile_atlas.png");
    public static final int AW = 256, AH = 256;
    public static final int TILE = 16;

    // ---- id спрайтів (порядок = порядок у атласі; групи йдуть підряд) ----
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
    public static final int HOUSE0 = 40;
    public static final int HOUSE1 = 41;
    public static final int HOUSE2 = 42;
    public static final int HOUSE3 = 43;
    public static final int HOUSE4 = 44;
    public static final int HOUSE5 = 45;
    public static final int HUT0 = 46;
    public static final int HUT1 = 47;
    public static final int HUT2 = 48;
    public static final int WORKSHOP0 = 49;
    public static final int WORKSHOP1 = 50;
    public static final int MINE0 = 51;
    public static final int MINE1 = 52;
    public static final int DOCK0 = 53;
    public static final int DOCK1 = 54;
    public static final int MARKET = 55;
    public static final int CHAPEL = 56;
    public static final int TOWER = 57;
    public static final int LIBRARY = 58;
    public static final int BARRACKS = 59;
    public static final int BARN = 60;
    public static final int WINDMILL = 61;
    public static final int SHIP = 62;
    public static final int FORTRESS = 63;

    public static final int COUNT = 64;
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
        set(16, 0, 16, 16, 16);
        set(17, 16, 16, 16, 16);
        set(18, 32, 16, 16, 16);
        set(19, 48, 16, 16, 16);
        set(20, 64, 16, 16, 16);
        set(21, 80, 16, 16, 16);
        set(22, 96, 16, 16, 16);
        set(23, 112, 16, 16, 16);
        set(24, 128, 16, 16, 16);
        set(25, 144, 16, 16, 16);
        set(26, 160, 16, 16, 16);
        set(27, 176, 16, 16, 16);
        set(28, 192, 16, 16, 16);
        set(29, 208, 16, 16, 16);
        set(30, 224, 16, 16, 16);
        set(31, 240, 16, 16, 16);
        set(32, 0, 32, 16, 16);
        set(33, 16, 32, 16, 16);
        set(34, 32, 32, 16, 16);
        set(35, 48, 32, 16, 16);
        set(36, 64, 32, 16, 16);
        set(37, 80, 32, 16, 16);
        set(38, 96, 32, 16, 16);
        set(39, 112, 32, 16, 16);
        set(40, 128, 32, 16, 16);
        set(41, 144, 32, 16, 16);
        set(42, 160, 32, 16, 16);
        set(43, 176, 32, 16, 16);
        set(44, 192, 32, 16, 16);
        set(45, 208, 32, 16, 16);
        set(46, 224, 32, 16, 16);
        set(47, 240, 32, 16, 16);
        set(48, 0, 48, 16, 16);
        set(49, 16, 48, 16, 16);
        set(50, 32, 48, 16, 16);
        set(51, 48, 48, 16, 16);
        set(52, 64, 48, 16, 16);
        set(53, 80, 48, 16, 16);
        set(54, 96, 48, 16, 16);
        set(55, 112, 48, 16, 16);
        set(56, 128, 48, 16, 16);
        set(57, 144, 48, 16, 16);
        set(58, 160, 48, 16, 16);
        set(59, 176, 48, 16, 16);
        set(60, 192, 48, 16, 16);
        set(61, 208, 48, 16, 16);
        set(62, 224, 48, 16, 16);
        set(63, 0, 64, 48, 48);
    }

    public static boolean isWaterTile(int id) {
        return id <= SHORE1;
    }

    /** Малює спрайт id у (x, y), розтягуючи його до w x h (у поточних координатах матриці). */
    public static void draw(DrawContext g, int id, int x, int y, int w, int h, int color) {
        g.drawTexture(RenderLayer::getGuiTextured, TEX, x, y, U[id], V[id], w, h, W[id], H[id], AW, AH, color);
    }

    /** Ширина спрайта в тайлах (1 для звичайних, 3 для фортеці). */
    public static int tilesWide(int id) {
        return W[id] / TILE;
    }

    public static int pointSprite(World.Point p) {
        return switch (p.type) {
            case TOWN -> HOUSE0 + Math.floorMod(p.px * 7 + p.py * 3, 6);
            case VILLAGE -> HUT0 + Math.floorMod(p.px + p.py, 3);
            case SMITHY -> WORKSHOP0 + Math.floorMod(p.px + p.py, 2);
            case POWDER_WORKS -> MINE0 + Math.floorMod(p.px + p.py, 2);
            case FISHERY -> DOCK0;
            case FOREST -> PINE;
            case STEPPE -> WHEAT3;
        };
    }

    /** Спрайт збудованої будівлі. */
    public static int buildingSprite(Building b) {
        return switch (b) {
            case FARM -> BARN;
            case MARKET -> MARKET;
            case MINE -> MINE0;
            case WALLS -> TOWER;
            case BARRACKS -> BARRACKS;
            case LIBRARY -> LIBRARY;
            case PORT -> DOCK0;
            case FORGE -> WORKSHOP0;
            case FORTRESS -> FORTRESS;
        };
    }

    private TileAtlas() {}
}
