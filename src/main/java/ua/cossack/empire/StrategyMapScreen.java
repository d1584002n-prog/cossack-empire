package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;

public class StrategyMapScreen extends Screen {
    private static final float SQ3 = 1.7320508f;
    private static final int TOP = 34;
    private static final int BOTTOM = 66;
    private static final int MARGIN = 6;
    private static final int GAP = 6;
    private static final int LEFT_W = 96;
    private static final int RIGHT_W = 176;
    private static final int ROW_H = 12;
    private static final int MINI = 3;
    private static final int FOG_COLOR = 0xFF404040;
    private static final float MAX_ZOOM = 10f;
    private static final int ICON_SRC = 32; // розмір PNG-іконок у ресурсах

    private static final String[] MENU = {"Армія", "Міста", "Дипломатія", "Економіка", "Технології"};

    private int selected = -1;
    private int selectedPoint = -1;
    private ButtonWidget upgradeBtn;
    private ButtonWidget spyBtn;
    private ButtonWidget attackBtn;

    private float zoom = 1f;
    private float panX = 0f;
    private float panY = 0f;
    private boolean dragging = false;
    private double moved = 0;
    // За замовчуванням карта виглядає суцільною; внутрішню гекс-сітку можна ввімкнути кнопкою #.
    private boolean showHexGrid = true;

    public StrategyMapScreen() {
        super(Text.literal("Козацька Держава"));
    }

    /** Старт: наближений вид на територію гравця. */
    private void resetView() {
        zoom = 4f;
        float ts = tileSize();
        World.Province c = GameState.world.provinces[World.startRow() * World.COLS + World.startCol()];
        panX = mapW() / 2f - (c.tx + 0.5f) * ts;
        panY = mapH() / 2f - (c.ty + 0.5f) * ts;
        clampPan();
    }

    @Override
    protected void init() {
        resetView();
        addDrawableChild(ButtonWidget.builder(Text.literal("Нова гра"), b -> {
                    GameState.newGame();
                    selected = -1;
                    selectedPoint = -1;
                    resetView();
                })
                .dimensions(MARGIN, 4, 62, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.empty(), b -> GameState.nextTurn())
                .dimensions(this.width - 116, this.height - 26, 110, 20)
                .build());

        for (int i = 0; i < MENU.length; i++) {
            final int idx = i;
            ButtonWidget b = ButtonWidget.builder(Text.empty(), btn -> openTab(idx))
                    .dimensions(MARGIN + i * 73, this.height - 26, 70, 20)
                    .build();
            b.active = i == 0 || i == 2;
            addDrawableChild(b);
        }

        upgradeBtn = ButtonWidget.builder(Text.literal("Покращити"), b -> {
                    World.Point p = currentPoint();
                    if (p != null) GameState.upgrade(p);
                })
                .dimensions(rightX(), this.height - 50, RIGHT_W, 18)
                .build();
        addDrawableChild(upgradeBtn);

        spyBtn = ButtonWidget.builder(Text.literal("Надіслати шпигуна"), b -> {
                    if (selected >= 0) GameState.sendSpy(GameState.world.provinces[selected]);
                })
                .dimensions(rightX(), this.height - 50, RIGHT_W, 18)
                .build();
        addDrawableChild(spyBtn);

        attackBtn = ButtonWidget.builder(Text.literal("Атакувати"), b -> {
                    World.Point p = currentPoint();
                    if (p != null && selected >= 0) {
                        GameState.attack(GameState.world.provinces[selected], p);
                    }
                })
                .dimensions(rightX(), this.height - 50, RIGHT_W, 18)
                .build();
        addDrawableChild(attackBtn);
    }

    private void openTab(int i) {
        if (i == 0) this.client.setScreen(new ArmyScreen(this));
        else if (i == 2) this.client.setScreen(new DiplomacyScreen(this));
        else if (i == 4) this.client.setScreen(new TechScreen(this));
    }

    // ---------- геометрія і масштаб ----------

    private int mapX() { return MARGIN + LEFT_W + GAP; }
    private int rightX() { return this.width - RIGHT_W - MARGIN; }
    private int mapW() { return rightX() - GAP - mapX(); }
    private int mapH() { return this.height - TOP - BOTTOM; }
    private int miniW() { return World.COLS * MINI; }
    private int miniH() { return World.ROWS * MINI; }

    /** Розмір тайла в екранних px при zoom = 1 (вся карта вміщається у вікно). */
    private float baseT() {
        return Math.min(mapW() / (float) World.TILES, mapH() / (float) World.TILES);
    }

    private float tileSize() { return baseT() * zoom; }
    private float totalW() { return tileSize() * World.TILES; }
    private float totalH() { return tileSize() * World.TILES; }

    private void clampPan() {
        float tw = totalW();
        float th = totalH();
        panX = tw <= mapW() ? (mapW() - tw) / 2f : Math.max(mapW() - tw, Math.min(0f, panX));
        panY = th <= mapH() ? (mapH() - th) / 2f : Math.max(mapH() - th, Math.min(0f, panY));
    }

    private void zoomAt(double mx, double my, float factor) {
        float oldT = tileSize();
        float newZoom = Math.max(1f, Math.min(MAX_ZOOM, zoom * factor));
        if (newZoom == zoom) return;
        float u = (float) ((mx - mapX() - panX) / oldT);
        float v = (float) ((my - TOP - panY) / oldT);
        zoom = newZoom;
        float newT = tileSize();
        panX = (float) (mx - mapX() - u * newT);
        panY = (float) (my - TOP - v * newT);
        clampPan();
    }

    private boolean inMap(double mx, double my) {
        return mx >= mapX() && mx < mapX() + mapW() && my >= TOP && my < TOP + mapH();
    }

    private int provinceAt(double mx, double my) {
        if (!inMap(mx, my)) return -1;
        float ts = tileSize();
        int tx = (int) Math.floor((mx - mapX() - panX) / ts);
        int ty = (int) Math.floor((my - TOP - panY) / ts);
        if (tx < 0 || ty < 0 || tx >= World.TILES || ty >= World.TILES) return -1;
        return GameState.world.provOf[ty * World.TILES + tx];
    }

    private World.Point currentPoint() {
        if (selected < 0 || selectedPoint < 0) return null;
        return GameState.world.provinces[selected].points.get(selectedPoint);
    }

    private int listTop() { return TOP + miniH() + 30; }

    // ---------- малювання ----------

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255;
        int ag = (a >> 8) & 255;
        int ab = a & 255;
        int br = (b >> 16) & 255;
        int bg = (b >> 8) & 255;
        int bb = b & 255;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private int provColor(World.Province pr) {
        if (!pr.explored) return FOG_COLOR;
        if (pr.terrain == World.Terrain.WATER) return pr.terrain.color;
        return mix(pr.terrain.color, World.FACTIONS[pr.owner].color(), 0.6f);
    }

    private void line(DrawContext ctx, float x1, float y1, float x2, float y2, int w, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(2, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy)));
        for (int k = 0; k <= steps; k++) {
            int x = Math.round(x1 + dx * k / steps);
            int y = Math.round(y1 + dy * k / steps);
            ctx.fill(x, y, x + w, y + w, color);
        }
    }

    private boolean visible(float cx, float cy, float r) {
        return cx + r >= mapX() && cx - r <= mapX() + mapW()
                && cy + r >= TOP && cy - r <= TOP + mapH();
    }


    private record Item(float sortY, Runnable draw) {}

    private int ownerTint(World.Province pr) {
        if (pr.owner == World.WATER_OWNER) return 0xFFFFFFFF;
        return mix(0xFFFFFFFF, World.FACTIONS[pr.owner].color(), 0.35f);
    }

    private final int[] tintCache = new int[World.COLS * World.ROWS];

    private void edge(DrawContext ctx, int a, int b, int ex, int ey, int th, int len,
                      boolean vertical, boolean grid) {
        if (a == b) return;
        World.Province[] provinces = GameState.world.provinces;
        World.Province pa = provinces[a];
        World.Province pb = provinces[b];
        int ca;
        int cb;
        int w = th;
        if (a == selected || b == selected) {
            ca = 0xFFFFFFFF;
            cb = 0xFFFFFFFF;
            w = th + 1;
        } else if (!pa.explored || !pb.explored
                || pa.terrain == World.Terrain.WATER || pb.terrain == World.Terrain.WATER) {
            return;
        } else if (pa.owner != pb.owner) {
            ca = 0xFF000000 | (World.FACTIONS[pa.owner].color() & 0xFFFFFF);
            cb = 0xFF000000 | (World.FACTIONS[pb.owner].color() & 0xFFFFFF);
        } else if (grid) {
            ca = 0x30FFFFFF;
            cb = 0x30FFFFFF;
        } else {
            return;
        }
        if (vertical) {
            ctx.fill(ex - w, ey, ex, ey + len, ca);
            ctx.fill(ex, ey, ex + w, ey + len, cb);
        } else {
            ctx.fill(ex, ey - w, ex + len, ey, ca);
            ctx.fill(ex, ey, ex + len, ey + w, cb);
        }
    }

    /** Тайлова карта: земля, накладки, межі, будівлі й прапорці (усе в одиницях тайла: 16 = 1 тайл). */
    private void drawWorld(DrawContext ctx, int hovered) {
        World w = GameState.world;
        World.Province[] provinces = w.provinces;
        final int T = World.TILES;
        float ts = tileSize();
        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            tintCache[i] = pr.explored && pr.terrain != World.Terrain.WATER ? ownerTint(pr) : 0xFFFFFFFF;
        }
        // LOD: на малому масштабі беремо кожен 2-й або 4-й тайл
        int k = ts < 2.6f ? 4 : ts < 5f ? 2 : 1;
        int x0 = Math.max(0, (int) Math.floor(-panX / ts));
        int y0 = Math.max(0, (int) Math.floor(-panY / ts));
        int x1 = Math.min(T - 1, (int) Math.ceil((mapW() - panX) / ts));
        int y1 = Math.min(T - 1, (int) Math.ceil((mapH() - panY) / ts));
        x0 -= x0 % k;
        y0 -= y0 % k;
        int S = 16 * k;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(mapX() + panX, TOP + panY, 0f);
        ctx.getMatrices().scale(ts / 16f, ts / 16f, 1f);

        for (int y = y0; y <= y1; y += k) {
            for (int x = x0; x <= x1; x += k) {
                int t = y * T + x;
                int pi = w.provOf[t];
                World.Province pr = provinces[pi];
                int px = x * 16;
                int py = y * 16;
                if (!pr.explored) {
                    ctx.fill(px, py, px + S, py + S, FOG_COLOR);
                    continue;
                }
                TileAtlas.draw(ctx, w.base[t], px, py, S, S, tintCache[pi]);
                if (k == 1 && w.over[t] != 0) TileAtlas.draw(ctx, w.over[t], px, py, 16, 16, tintCache[pi]);
                if (pi == hovered) ctx.fill(px, py, px + S, py + S, 0x28FFFFFF);
            }
        }

        // межі провінцій і держав
        int th = Math.max(1, (int) Math.ceil(6f / ts));
        boolean grid = showHexGrid && ts >= 5f;
        for (int y = y0; y <= y1; y += k) {
            for (int x = x0; x <= x1; x += k) {
                int pi = w.provOf[y * T + x];
                if (x + k < T) edge(ctx, pi, w.provOf[y * T + x + k], (x + k) * 16, y * 16, th, S, true, grid);
                if (y + k < T) edge(ctx, pi, w.provOf[(y + k) * T + x], x * 16, (y + k) * 16, th, S, false, grid);
            }
        }

        // будівлі точок і прапорці
        if (ts >= 4f) {
            for (int i = 0; i < provinces.length; i++) {
                World.Province pr = provinces[i];
                if (!pr.explored) continue;
                for (int j = 0; j < pr.points.size(); j++) {
                    World.Point p = pr.points.get(j);
                    if (p.type == World.PointType.FISHERY) continue;
                    if (p.px < x0 - 1 || p.px > x1 + 1 || p.py < y0 - 1 || p.py > y1 + 1) continue;
                    TileAtlas.draw(ctx, TileAtlas.pointSprite(p), p.px * 16, p.py * 16, 16, 16, 0xFFFFFFFF);
                    if (p.type == World.PointType.TOWN && ts >= 5.5f) {
                        int fx = p.px * 16 + 11;
                        int fy = p.py * 16 - 5;
                        int flag = p.owner == 0 ? 0xFFD83A32 : World.FACTIONS[p.owner].color();
                        ctx.fill(fx, fy, fx + 1, fy + 10, 0xFF3B2A14);
                        ctx.fill(fx + 1, fy, fx + 7, fy + 4, flag);
                    }
                    if (i == selected && j == selectedPoint) {
                        ctx.drawBorder(p.px * 16 - 1, p.py * 16 - 1, 18, 18, 0xFFFFFFFF);
                    }
                }
            }
        }
        ctx.getMatrices().pop();
    }

    private void drawLabels(DrawContext ctx, int hovered) {
        World.Province[] provinces = GameState.world.provinces;
        float ts = tileSize();
        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            if (!pr.explored || pr.terrain == World.Terrain.WATER) continue;
            if (!(ts >= 11f || i == selected || i == hovered)) continue;
            float cx = mapX() + panX + (pr.tx + 0.5f) * ts;
            float cy = TOP + panY + (pr.ty + 1.25f) * ts;
            if (cx < mapX() - 40 || cx > mapX() + mapW() + 40 || cy < TOP - 10 || cy > TOP + mapH() + 10) continue;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx, cy, 0f);
            ctx.getMatrices().scale(0.7f, 0.7f, 1f);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(pr.name), 0, 0, 0xFFFFFF);
            ctx.getMatrices().pop();
        }
    }

    private void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0x99000000);
        ctx.drawBorder(x, y, w, h, 0xFF6B5A2E);
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(s), x, y, color);
    }

    private void textRight(DrawContext ctx, String s, int xRight, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(s), xRight - textRenderer.getWidth(s), y, color);
    }

    // ---------- ввід ----------

    private void selectAt(double mx, double my) {
        int idx = provinceAt(mx, my);
        if (idx < 0) return;
        selected = idx;
        selectedPoint = -1;
        World.Province pr = GameState.world.provinces[idx];
        if (pr.explored && pr.terrain != World.Terrain.WATER) {
            for (int i = 0; i < pr.points.size(); i++) {
                if (pr.points.get(i).owner != 0) {
                    selectedPoint = i;
                    break;
                }
            }
            if (selectedPoint < 0 && !pr.points.isEmpty()) selectedPoint = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;

        int zy = TOP + mapH() - 17;
        if (my >= zy && my < zy + 15) {
            for (int k = 0; k < 4; k++) {
                int bx = mapX() + 3 + k * 17;
                if (mx >= bx && mx < bx + 15) {
                    double cx = mapX() + mapW() / 2.0;
                    double cy = TOP + mapH() / 2.0;
                    if (k == 0) {
                        zoomAt(cx, cy, 1.4f);
                    } else if (k == 1) {
                        zoomAt(cx, cy, 1f / 1.4f);
                    } else if (k == 2) {
                        zoom = 1f;
                        panX = 0f;
                        panY = 0f;
                        clampPan();
                    } else {
                        showHexGrid = !showHexGrid;
                    }
                    return true;
                }
            }
        }

        int rx = rightX();
        if (mx >= rx && mx < rx + miniW() && my >= TOP && my < TOP + miniH()) {
            float fx = (float) ((mx - rx) / miniW());
            float fy = (float) ((my - TOP) / miniH());
            panX = mapW() / 2f - fx * totalW();
            panY = mapH() / 2f - fy * totalH();
            clampPan();
            return true;
        }

        if (inMap(mx, my)) {
            dragging = true;
            moved = 0;
            return true;
        }

        if (selected >= 0) {
            World.Province sel = GameState.world.provinces[selected];
            if (sel.explored && sel.terrain != World.Terrain.WATER
                    && mx >= rx && mx < rx + RIGHT_W && my >= listTop()) {
                int row = (int) ((my - listTop()) / ROW_H);
                if (row < sel.points.size()) {
                    selectedPoint = row;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            panX += (float) dx;
            panY += (float) dy;
            moved += Math.abs(dx) + Math.abs(dy);
            clampPan();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && button == 0) {
            dragging = false;
            if (moved < 4) selectAt(mx, my);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (inMap(mx, my)) {
            zoomAt(mx, my, vAmount > 0 ? 1.25f : 0.8f);
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    private String res(World.ResourceType r) {
        return r.label + " " + GameState.stock.get(r) + " (+" + GameState.income(r) + ")";
    }

    // ---------- відмальовка ----------

    private void drawRpgIcon(DrawContext ctx, String name, int x, int y, int size) {
        Identifier texture = Identifier.of("cossack_empire", "textures/gui/icons/" + name + ".png");
        ctx.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0.0f, 0.0f,
                size, size, ICON_SRC, ICON_SRC, ICON_SRC, ICON_SRC, 0xFFFFFFFF);
    }

    /** Напис, який стискається, якщо не вміщується в maxW. */
    private void menuLabel(DrawContext ctx, String s, int x, int y, int maxW) {
        int w = textRenderer.getWidth(s);
        float sc = w > maxW ? (float) maxW / w : 1f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y + (1f - sc) * 4f, 0f);
        ctx.getMatrices().scale(sc, sc, 1f);
        ctx.drawTextWithShadow(textRenderer, Text.literal(s), 0, 0, 0xFFFFFFFF);
        ctx.getMatrices().pop();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        clampPan();
        World.Province[] provinces = GameState.world.provinces;
        World.Province selProv = selected >= 0 ? provinces[selected] : null;
        boolean hiddenSel = selProv != null && !selProv.explored;
        boolean waterSel = selProv != null && !hiddenSel && selProv.terrain == World.Terrain.WATER;
        World.Point sp = (hiddenSel || waterSel) ? null : currentPoint();
        boolean mine = sp != null && sp.owner == 0;
        boolean enemy = sp != null && sp.owner != 0;

        upgradeBtn.visible = mine;
        attackBtn.visible = enemy;
        spyBtn.visible = hiddenSel;
        upgradeBtn.active = mine && GameState.canUpgrade(sp);
        attackBtn.active = enemy && GameState.canAttack(selProv, sp);
        spyBtn.active = hiddenSel && GameState.canSpy(selProv);
        upgradeBtn.setMessage(Text.literal(
                mine && sp.level < GameState.MAX_LEVEL ? "Покращити до ур." + (sp.level + 1) : "Покращити"));

        super.render(ctx, mouseX, mouseY, delta);

        // RPG-іконки на панелі керування. Текстури завантажуються з assets/cossack_empire/textures/gui/icons.
        String[] iconNames = {"army", "cities", "diplomacy", "economy", "technology"};
        for (int i = 0; i < iconNames.length; i++) {
            int bx = MARGIN + i * 73;
            drawRpgIcon(ctx, iconNames[i], bx + 3, this.height - 24, 16);
            menuLabel(ctx, MENU[i], bx + 21, this.height - 20, 46);
        }
        int nextX = this.width - 116;
        drawRpgIcon(ctx, "next_turn", nextX + 5, this.height - 24, 16);
        menuLabel(ctx, "Наступний хід", nextX + 24, this.height - 20, 84);

        // ----- верхня панель ресурсів -----
        String line1 = res(World.ResourceType.GOLD) + "   " + res(World.ResourceType.FOOD)
                + "   " + res(World.ResourceType.WOOD);
        String line2 = res(World.ResourceType.IRON) + "   " + res(World.ResourceType.POWDER)
                + "   " + res(World.ResourceType.HORSES);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line1), this.width / 2, 6, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line2), this.width / 2, 18, 0xFFD700);

        // ----- ліва панель: території -----
        int mineProv = 0;
        for (World.Province pr : provinces) {
            if (pr.owner == 0) mineProv++;
        }
        int explored = GameState.exploredCount();
        panel(ctx, MARGIN, TOP, LEFT_W, 68);
        text(ctx, "Території", MARGIN + 4, TOP + 4, 0xFFD700);
        text(ctx, "Твоїх: " + mineProv, MARGIN + 4, TOP + 16, 0xFFFFFF);
        text(ctx, "Відкрито: " + explored + "/" + provinces.length, MARGIN + 4, TOP + 26, 0xFFFFFF);
        text(ctx, "Невідомо: " + (provinces.length - explored), MARGIN + 4, TOP + 36, 0xAAAAAA);
        text(ctx, "Сила армії: " + GameState.armyPower(), MARGIN + 4, TOP + 46, 0xFFFFFF);
        text(ctx, "Полонених: " + GameState.prisoners, MARGIN + 4, TOP + 56, 0xFFFFFF);

        // ----- ліва панель: відносини -----
        int ry = TOP + 74;
        panel(ctx, MARGIN, ry, LEFT_W, 70);
        text(ctx, "Відносини", MARGIN + 4, ry + 4, 0xFFD700);
        Integer[] order = new Integer[Countries.ALL.length - 1];
        for (int i = 0; i < order.length; i++) order[i] = i + 1;
        java.util.Arrays.sort(order, (x, y2) -> {
            int kx = GameState.atWar[x] ? 0 : GameState.allied[x] ? 1 : 2;
            int ky = GameState.atWar[y2] ? 0 : GameState.allied[y2] ? 1 : 2;
            if (kx != ky) return Integer.compare(kx, ky);
            return Integer.compare(GameState.relation[x], GameState.relation[y2]);
        });
        for (int i = 0; i < 5; i++) {
            int f = order[i];
            int y = ry + 16 + i * 10;
            int nameColor = GameState.atWar[f] ? 0xFF5555 : GameState.allied[f] ? 0x55FF55 : 0xFFFFFF;
            int rel = GameState.relation[f];
            int relColor = rel < 0 ? 0xFF8888 : rel > 0 ? 0x88FF88 : 0xDDDDDD;
            text(ctx, World.FACTIONS[f].shortName(), MARGIN + 4, y, nameColor);
            textRight(ctx, String.valueOf(rel), MARGIN + LEFT_W - 4, y, relColor);
        }

        // ----- карта -----
        int hovered = provinceAt(mouseX, mouseY);
        int vx1 = mapX();
        int vy1 = TOP;
        int vx2 = mapX() + mapW();
        int vy2 = TOP + mapH();

        ctx.fill(vx1 - 1, vy1 - 1, vx2 + 1, vy2 + 1, 0xFF6B5A2E);
        ctx.fill(vx1, vy1, vx2, vy2, 0xFF0B0B0B);
        ctx.enableScissor(vx1, vy1, vx2, vy2);

        drawWorld(ctx, hovered);
        drawLabels(ctx, hovered);
        ctx.disableScissor();

        // ----- кнопки масштабу -----
        String[] zl = {"+", "-", "0", "#"};
        int zy = TOP + mapH() - 17;
        for (int k = 0; k < 4; k++) {
            int bx = mapX() + 3 + k * 17;
            ctx.fill(bx, zy, bx + 15, zy + 15, 0xFF000000);
            ctx.drawBorder(bx, zy, 15, 15, 0xFF6B5A2E);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(zl[k]), bx + 8, zy + 4,
                    k == 3 && showHexGrid ? 0xFFFF55 : 0xFFFFFF);
        }
        text(ctx, String.format("x%.1f", zoom), mapX() + 74, zy + 4, 0xDDDDDD);

        // ----- мінікарта -----
        int rx = rightX();
        panel(ctx, rx - 2, TOP - 2, miniW() + 4, miniH() + 4);
        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            int x = rx + pr.col * MINI;
            int y = TOP + pr.row * MINI;
            ctx.fill(x, y, x + MINI, y + MINI, provColor(pr));
        }
        float tw = totalW();
        float th = totalH();
        float fx0 = Math.max(0f, -panX / tw);
        float fy0 = Math.max(0f, -panY / th);
        float fw = Math.min(1f - fx0, mapW() / tw);
        float fh = Math.min(1f - fy0, mapH() / th);
        ctx.drawBorder(rx + Math.round(fx0 * miniW()), TOP + Math.round(fy0 * miniH()),
                Math.max(3, Math.round(fw * miniW())), Math.max(3, Math.round(fh * miniH())), 0xFFFFFFFF);

        int year = GameState.year();
        String season = GameState.seasonLabel();
        text(ctx, "Рік " + year, rx + miniW() + 8, TOP, 0xFFD700);
        text(ctx, season, rx + miniW() + 8, TOP + 12, 0xFFFFFF);
        text(ctx, "Хід " + GameState.turn, rx + miniW() + 8, TOP + 24, 0xAAAAAA);

        // ----- права колонка: вибрана провінція -----
        String info1 = null;
        String info2 = null;
        boolean infoOk = true;
        int hy = TOP + miniH() + 6;

        if (selProv != null && hiddenSel) {
            text(ctx, "Невідома територія", rx, hy, 0xFFD700);
            if (!GameState.nextToExplored(selProv)) {
                info1 = "Спершу відкрий";
                info2 = "сусідню територію";
                infoOk = false;
            } else {
                info1 = "Шпигун: Золото " + GameState.spyCost();
                infoOk = spyBtn.active;
            }
        } else if (waterSel) {
            text(ctx, "Озеро / море", rx, hy, 0xFFD700);
            text(ctx, "Непрохідна територія", rx, hy + 11, 0xAAAAAA);
        } else if (selProv != null) {
            text(ctx, selProv.name + " — " + World.FACTIONS[selProv.owner].shortName(), rx, hy, 0xFFD700);
            text(ctx, selProv.terrain.label + (selProv.river ? ", річка" : ""), rx, hy + 11, 0xAAAAAA);
            int y = listTop();
            for (int i = 0; i < selProv.points.size(); i++) {
                World.Point p = selProv.points.get(i);
                if (i == selectedPoint) {
                    ctx.fill(rx - 2, y - 1, rx + RIGHT_W, y + ROW_H - 1, 0x40FFFFFF);
                }
                String right = "ур." + p.level + " +" + p.production() + " " + p.type.resource.label
                        + (p.unrest >= 20 ? " !" : "");
                int rwid = textRenderer.getWidth(right);
                int color = p.owner != 0 ? 0xAAAAAA : (p.unrest >= 20 ? 0xFF5555 : 0xFFFFFF);
                text(ctx, textRenderer.trimToWidth(p.name, RIGHT_W - rwid - 6), rx, y, color);
                textRight(ctx, right, rx + RIGHT_W, y, color);
                y += ROW_H;
            }
            if (mine) {
                infoOk = upgradeBtn.active;
                if (sp.level >= GameState.MAX_LEVEL) {
                    info1 = "Максимальний рівень";
                } else {
                    info1 = "Ціна: Золото " + GameState.costGold(sp) + ", Дерево " + GameState.costWood(sp);
                    info2 = "Залізо " + GameState.costIron(sp);
                }
            } else if (enemy) {
                if (!GameState.isNearMine(selProv)) {
                    info1 = "Занадто далеко";
                    info2 = "від твоїх земель";
                    infoOk = false;
                } else if (!GameState.atWarWith(sp)) {
                    info1 = "Спершу оголоси війну";
                    info2 = "(Дипломатія)";
                    infoOk = false;
                } else if (GameState.armyPower() == 0) {
                    info1 = "Немає війська";
                    infoOk = false;
                } else {
                    int approx = Math.round(sp.garrison / 5f) * 5;
                    info1 = "Сила " + GameState.armyPower() + " проти ~" + approx;
                }
            }
        } else {
            text(ctx, "Натисни на провінцію", rx, hy, 0xFFFFFF);
        }

        if (info1 != null) text(ctx, info1, rx, this.height - 76, infoOk ? 0xFFFFFF : 0xFF5555);
        if (info2 != null) text(ctx, info2, rx, this.height - 64, infoOk ? 0xFFFFFF : 0xFF5555);

        // ----- журнал подій -----
        int logW = rightX() - GAP - MARGIN;
        for (int k = 2; k >= 0; k--) {
            String s = GameState.logLine(k);
            if (!s.isEmpty()) {
                text(ctx, textRenderer.trimToWidth(s, logW), MARGIN, this.height - 38 - k * 12,
                        k == 0 ? 0xFFD700 : 0xAAAAAA);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
                                                   }
