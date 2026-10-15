package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Атлас спрайтів карти (map_atlas.png, 128x136, база: MiniWorld Sprites by Shade).
 * Усе малюється через одну текстуру, тому порядок викликів = порядок шарів.
 */
public final class MapSprites {
    public static final Identifier ATLAS = Identifier.of("cossack_empire", "map_atlas.png");
    private static final int AW = 128, AH = 136;
    /** Гекс у атласі: 28x32 px, "радіус" 16 px (pointy-top). */
    public static final float HEX_R = 16f;

    public record Region(int u, int v, int w, int h) {}

    public static final Region TILE_WATER = new Region(0, 0, 28, 32);
    public static final Region TILE_PLAINS = new Region(29, 0, 28, 32);
    public static final Region TILE_MEADOW = new Region(58, 0, 28, 32);
    public static final Region TILE_FOREST = new Region(87, 0, 28, 32);
    public static final Region TILE_MOUNTAIN = new Region(0, 33, 28, 32);
    public static final Region TILE_STEPPE = new Region(29, 33, 28, 32);
    public static final Region TILE_FOG = new Region(58, 33, 28, 32);
    public static final Region TILE_MASK = new Region(87, 33, 28, 32);
    public static final Region TREE1 = new Region(116, 33, 12, 14);
    public static final Region TREE2 = new Region(0, 66, 14, 14);
    public static final Region TREE3 = new Region(15, 66, 12, 15);
    public static final Region PINE1 = new Region(28, 66, 14, 15);
    public static final Region PINE2 = new Region(43, 66, 14, 15);
    public static final Region ROCK1 = new Region(58, 66, 14, 10);
    public static final Region ROCK2 = new Region(73, 66, 14, 11);
    public static final Region ROCK3 = new Region(88, 66, 14, 13);
    public static final Region WHEAT = new Region(103, 66, 14, 13);
    public static final Region CACTUS = new Region(118, 66, 10, 13);
    public static final Region TUMBLE = new Region(0, 82, 10, 8);
    public static final Region KEEP = new Region(11, 82, 32, 32);
    public static final Region HOUSES = new Region(44, 82, 16, 16);
    public static final Region HUT = new Region(61, 82, 16, 16);
    public static final Region WORKSHOP = new Region(78, 82, 16, 16);
    public static final Region MINE = new Region(95, 82, 16, 16);
    public static final Region POWDER = new Region(112, 82, 16, 16);
    public static final Region DOCK = new Region(0, 115, 16, 16);

    public static Region tileFor(World.Terrain t) {
        return switch (t) {
            case WATER -> TILE_WATER;
            case PLAINS -> TILE_PLAINS;
            case MEADOW -> TILE_MEADOW;
            case FOREST -> TILE_FOREST;
            case MOUNTAIN -> TILE_MOUNTAIN;
            case STEPPE -> TILE_STEPPE;
        };
    }

    public static Region pointSprite(World.PointType t) {
        return switch (t) {
            case TOWN -> KEEP;
            case VILLAGE -> HOUSES;
            case SMITHY -> WORKSHOP;
            case POWDER_WORKS -> POWDER;
            case FISHERY -> DOCK;
            default -> null; // FOREST / STEPPE точки не мають окремого будинку
        };
    }

    /** Гекс-текстура з центром у (cx, cy) і "радіусом" radius (екранні px). color = ARGB-множник (тінт). */
    public static void hex(DrawContext g, Region reg, float cx, float cy, float radius, int color) {
        float s = radius / HEX_R;
        g.getMatrices().push();
        g.getMatrices().translate(cx, cy, 0f);
        g.getMatrices().scale(s, s, 1f);
        g.drawTexture(RenderLayer::getGuiTextured, ATLAS, -reg.w() / 2, -reg.h() / 2,
                reg.u(), reg.v(), reg.w(), reg.h(), reg.w(), reg.h(), AW, AH, color);
        g.getMatrices().pop();
    }

    /**
     * Спрайт-об'єкт: якір — середина НИЗУ спрайта в точці (x, y), тому він росте вгору
     * і може виступати за верхню межу гекса. scale — множник відносно масштабу гекса.
     */
    public static void sprite(DrawContext g, Region reg, float x, float y, float radius, float scale, int color) {
        float s = radius / HEX_R * scale;
        g.getMatrices().push();
        g.getMatrices().translate(x, y, 0f);
        g.getMatrices().scale(s, s, 1f);
        g.drawTexture(RenderLayer::getGuiTextured, ATLAS, -reg.w() / 2, -reg.h(),
                reg.u(), reg.v(), reg.w(), reg.h(), reg.w(), reg.h(), AW, AH, color);
        g.getMatrices().pop();
    }

    private MapSprites() {}
}
