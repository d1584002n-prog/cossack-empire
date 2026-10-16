package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Атлас тайлів 16x16 (tile_atlas.png, 256x256), зібраний зі спрайтів MiniWorld Sprites by Shade.
 * Кожен тайл має числовий id; World зберігає id для кожної клітинки карти.
 */
public final class TileAtlas {
    public static final Identifier TEX = Identifier.of("cossack_empire", "tile_atlas.png");
    public static final int AW = 256, AH = 256;
    public static final int TILE = 16;

    // ---- основа (ground) ----
    public static final int WATER_DEEP = 0;
    public static final int WATER_SHALLOW = 1;
    public static final int SHORE2 = 2;
    public static final int SHORE1 = 3;
    public static final int SAND = 4;
    public static final int GRASS_L = 5;
    public static final int GRASS_D = 6;
    public static final int TEXGRASS = 7;   // 7..12 — трава з пучками
    public static final int DEADGRASS = 13; // 13..18 — степ
    // ---- накладки (на прозорому фоні) ----
    public static final int TREE1 = 20, TREE2 = 21, TREE3 = 22, PINE = 23;
    public static final int ROCK1 = 24, ROCK2 = 25, ROCK3 = 26;
    public static final int WHEAT1 = 27, WHEAT2 = 28, WHEAT3 = 29;
    public static final int CACTUS1 = 30, CACTUS2 = 31, BUSH1 = 32, BUSH2 = 33;
    public static final int DEADTREE1 = 34, DEADTREE2 = 35;
    // ---- будівлі точок ----
    public static final int HOUSE0 = 40; // 40..45 — міські будинки
    public static final int HUT0 = 46;   // 46..48 — хати
    public static final int WORKSHOP0 = 49, WORKSHOP1 = 50;
    public static final int MINE0 = 51, MINE1 = 52;
    public static final int DOCK0 = 53, DOCK1 = 54;
    public static final int MARKET = 55, CHAPEL = 56, TOWER = 57;

    public static final int COUNT = 64;
    private static final int[] U = new int[COUNT];
    private static final int[] V = new int[COUNT];

    private static void set(int id, int u, int v) {
        U[id] = u;
        V[id] = v;
    }

    static {
        set(WATER_DEEP, 64, 16);
        set(WATER_SHALLOW, 48, 16);
        set(SHORE2, 32, 16);
        set(SHORE1, 16, 16);
        set(SAND, 0, 16);
        set(GRASS_L, 16, 0);
        set(GRASS_D, 32, 0);
        for (int i = 0; i < 6; i++) set(TEXGRASS + i, 96 + (i % 3) * 16, (i / 3) * 16);
        for (int i = 0; i < 6; i++) set(DEADGRASS + i, 96 + (i % 3) * 16, 32 + (i / 3) * 16);
        set(TREE1, 16, 32);
        set(TREE2, 32, 32);
        set(TREE3, 48, 32);
        set(PINE, 16, 48);
        set(ROCK1, 160, 0);
        set(ROCK2, 176, 0);
        set(ROCK3, 192, 0);
        set(WHEAT1, 16, 64);
        set(WHEAT2, 32, 64);
        set(WHEAT3, 48, 64);
        set(CACTUS1, 0, 80);
        set(CACTUS2, 48, 80);
        set(BUSH1, 16, 96);
        set(BUSH2, 32, 96);
        set(DEADTREE1, 32, 112);
        set(DEADTREE2, 48, 112);
        set(HOUSE0, 0, 144);
        set(HOUSE0 + 1, 16, 144);
        set(HOUSE0 + 2, 32, 144);
        set(HOUSE0 + 3, 0, 160);
        set(HOUSE0 + 4, 16, 160);
        set(HOUSE0 + 5, 32, 160);
        set(HUT0, 80, 128);
        set(HUT0 + 1, 96, 128);
        set(HUT0 + 2, 112, 128);
        set(WORKSHOP0, 64, 144);
        set(WORKSHOP1, 80, 144);
        set(MINE0, 128, 192);
        set(MINE1, 144, 192);
        set(DOCK0, 0, 192);
        set(DOCK1, 16, 192);
        set(MARKET, 64, 208);
        set(CHAPEL, 192, 128);
        set(TOWER, 176, 160);
    }

    public static boolean isWaterTile(int id) {
        return id <= SHORE1;
    }

    /** Малює тайл id у (x, y) з розміром w x h (у поточних координатах матриці). */
    public static void draw(DrawContext g, int id, int x, int y, int w, int h, int color) {
        g.drawTexture(RenderLayer::getGuiTextured, TEX, x, y, U[id], V[id], w, h, TILE, TILE, AW, AH, color);
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

    private TileAtlas() {}
}
