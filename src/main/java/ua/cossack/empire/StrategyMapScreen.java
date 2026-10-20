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
    private static final int BOTTOM = 72;
    private static final int MARGIN = 6;
    private static final int GAP = 6;
    private static final int LEFT_W = 96;
    private static final int RIGHT_W = 176;
    private static final int ROW_H = 12;
    private Building placing = null; // будівля, яку гравець зараз ставить на карту
    private static final int FOG_COLOR = 0xFF404040;
    private static final float MAX_ZOOM = 14f;
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
    // політична карта: країни суцільним кольором (перемикач кнопкою П)
    private boolean politicalMode = false;
    private static final Building[] BLD = Building.values();

    public StrategyMapScreen() {
        super(Text.literal("Козацька Держава"));
    }

    /** Старт: наближений вид на територію гравця. */
    private void resetView() {
        zoom = 5f;
        float ts = tileSize();
        World.Province c = GameState.world.provinces[World.startRow() * World.COLS + World.startCol()];
        panX = mapW() / 2f - (c.tx + 0.5f) * ts;
        panY = mapH() / 2f - (c.ty + 0.5f) * ts;
        clampPan();
    }

    @Override
    protected void init() {
        resetView();
        addDrawableChild(ButtonWidget.builder(Text.literal("Журнал"), b -> this.client.setScreen(new JournalScreen(this)))
                .dimensions(MARGIN, TOP + 86 + 74, LEFT_W, 16).build());
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
            b.active = true;
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
        else if (i == 1) this.client.setScreen(new CitiesScreen(this));
        else if (i == 2) this.client.setScreen(new DiplomacyScreen(this));
        else if (i == 3) this.client.setScreen(new EconomyScreen(this));
        else if (i == 4) this.client.setScreen(new TechScreen(this));
    }

    // ---------- геометрія і масштаб ----------

    private int mapX() { return MARGIN + LEFT_W + GAP; }
    private int rightX() { return this.width - RIGHT_W - MARGIN; }
    private int mapW() { return rightX() - GAP - mapX(); }
    private int mapH() { return this.height - TOP - BOTTOM; }
    // мінікарта підлаштовується під висоту екрана, щоб права колонка не накладалась
    private int miniSize() { return Math.max(40, Math.min(88, this.height - 50 - TOP - 98)); }
    private int miniW() { return miniSize(); }
    private int miniH() { return miniSize() * World.ROWS / World.COLS; }

    /** Розмір тайла в екранних px при zoom = 1 (вся карта вміщається у вікно). */
    private float baseT() {
        return Math.min(mapW() / (float) World.TW, mapH() / (float) World.TH);
    }

    private float tileSize() { return baseT() * zoom; }
    private float totalW() { return tileSize() * World.TW; }
    private float totalH() { return tileSize() * World.TH; }

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
        if (tx < 0 || ty < 0 || tx >= World.TW || ty >= World.TH) return -1;
        return GameState.world.provOf[ty * World.TW + tx];
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
        return mix(0xFFFFFFFF, World.FACTIONS[pr.owner].color(), 0.12f);
    }

    private final int[] tintCache = new int[World.COLS * World.ROWS];
    private final int[] polCache = new int[World.COLS * World.ROWS];

    private int politicalColor(World.Province pr) {
        if (!pr.explored || pr.terrain == World.Terrain.WATER) return FOG_COLOR;
        int c = mix(0xFFE8D9A8, World.FACTIONS[pr.owner].color(), 0.72f);
        return switch (pr.terrain) {
            case MOUNTAIN -> mix(c, 0xFF5A4A3A, 0.22f);
            case FOREST -> mix(c, 0xFF2F4A2A, 0.14f);
            case STEPPE -> mix(c, 0xFFE8D9A8, 0.2f);
            default -> c;
        };
    }

    private int seaColor(int baseId) {
        return switch (baseId) {
            case 0 -> 0xFF8792A0;
            case 1 -> 0xFF95A0AD;
            case 2 -> 0xFFA2ACB8;
            default -> 0xFFB0B9C2;
        };
    }

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
        } else if (!pa.explored || !pb.explored) {
            return;
        } else if (pa.terrain == World.Terrain.WATER || pb.terrain == World.Terrain.WATER) {
            if (!politicalMode || pa.terrain == pb.terrain) return;
            ca = 0xFF4A3A28; // берегова лінія
            cb = 0xFF4A3A28;
            w = Math.max(1, th / 2);
        } else if (pa.owner != pb.owner) {
            if (politicalMode) {
                ca = 0xFF2E1F0E;
                cb = 0xFF2E1F0E;
            } else {
                ca = 0xFF000000 | (World.FACTIONS[pa.owner].color() & 0xFFFFFF);
                cb = 0xFF000000 | (World.FACTIONS[pb.owner].color() & 0xFFFFFF);
            }
        } else if (grid) {
            ca = politicalMode ? 0x40301C0A : 0x30FFFFFF;
            cb = ca;
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
        final int T = World.TW;
        float ts = tileSize();
        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            tintCache[i] = pr.explored && pr.terrain != World.Terrain.WATER ? ownerTint(pr) : 0xFFFFFFFF;
            if (politicalMode) polCache[i] = politicalColor(pr);
        }
        // LOD: на малому масштабі беремо кожен 2-й або 4-й тайл
        int k = ts < 2.6f ? 4 : ts < 5f ? 2 : 1;
        int x0 = Math.max(0, (int) Math.floor(-panX / ts));
        int y0 = Math.max(0, (int) Math.floor(-panY / ts));
        int x1 = Math.min(T - 1, (int) Math.ceil((mapW() - panX) / ts));
        int y1 = Math.min(World.TH - 1, (int) Math.ceil((mapH() - panY) / ts));
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
                if (politicalMode) {
                    int b = w.base[t];
                    int col;
                    if (pr.terrain == World.Terrain.WATER) col = seaColor(b);
                    else if (b == TileAtlas.WATER_SHALLOW) col = 0xFF7FA8C8; // річка
                    else col = polCache[pi];
                    ctx.fill(px, py, px + S, py + S, col);
                    if (pi == hovered) ctx.fill(px, py, px + S, py + S, 0x28FFFFFF);
                    continue;
                }
                TileAtlas.draw(ctx, w.base[t], px, py, S, S, tintCache[pi]);
                if (k == 1 && w.over[t] != 0) TileAtlas.draw(ctx, w.over[t], px, py, 16, 16, tintCache[pi]);
                if (placing != null && pi == selected && Realm.canPlace(pr, placing, t)) {
                    ctx.fill(px, py, px + S, py + S, 0x6055FF55);
                }
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
                if (y + k < World.TH) edge(ctx, pi, w.provOf[(y + k) * T + x], x * 16, (y + k) * 16, th, S, false, grid);
            }
        }

        // будівлі точок (на політичній карті — лише щити)
        if (!politicalMode && ts >= 4f) {
            for (int i = 0; i < provinces.length; i++) {
                World.Province pr = provinces[i];
                if (!pr.explored) continue;
                for (int j = 0; j < pr.points.size(); j++) {
                    World.Point p = pr.points.get(j);
                    if (p.type == World.PointType.FISHERY) continue;
                    if (p.px < x0 - 1 || p.px > x1 + 1 || p.py < y0 - 1 || p.py > y1 + 1) continue;
                    TileAtlas.drawSprite(ctx, TileAtlas.pointSprite(p), p.px, p.py, 0xFFFFFFFF);
                    if (p.type == World.PointType.TOWN && ts >= 5.5f) {
                        drawFlag(ctx, p.px * 16 + 8 + TileAtlas.spriteW(TileAtlas.pointSprite(p)) / 2 - 3,
                                p.py * 16 + 16 - TileAtlas.spriteH(TileAtlas.pointSprite(p)) - 6, p.owner);
                    }
                    if (i == selected && j == selectedPoint) {
                        ctx.drawBorder(p.px * 16 - 1, p.py * 16 - 1, 18, 18, 0xFFFFFFFF);
                    }
                }
                // будівлі, які поставив гравець
                for (java.util.Map.Entry<Building, Integer> e : pr.buildingTile.entrySet()) {
                    int bx = e.getValue() % T;
                    int by = e.getValue() / T;
                    if (bx < x0 - 4 || bx > x1 + 4 || by < y0 - 2 || by > y1 + 6) continue;
                    TileAtlas.drawSprite(ctx, TileAtlas.buildingSprite(e.getKey()), bx, by, 0xFFFFFFFF);
                }
            }
            drawCastles(ctx, x0, y0, x1, y1);
            drawArmy(ctx, x0, y0, x1, y1);
            drawShips(ctx, x0, y0, x1, y1);
        }
        ctx.getMatrices().pop();
    }

    /** Прапорець власника над містом (гравець — синьо-жовтий). */
    private void drawFlag(DrawContext ctx, int fx, int fy, int owner) {
        ctx.fill(fx, fy, fx + 1, fy + 10, 0xFF3B2A14);
        if (owner == 0) {
            ctx.fill(fx + 1, fy, fx + 7, fy + 2, 0xFF1E5BC6);
            ctx.fill(fx + 1, fy + 2, fx + 7, fy + 4, 0xFFFFD500);
        } else {
            ctx.fill(fx + 1, fy, fx + 7, fy + 4, 0xFF000000 | (World.FACTIONS[owner].color() & 0xFFFFFF));
        }
    }

    /** Замки на столицях держав: синьо-золотий — Гетьманщина, сірі — решта (з прапором держави). */
    private void drawCastles(DrawContext ctx, int x0, int y0, int x1, int y1) {
        World w = GameState.world;
        for (int f = 0; f < Countries.ALL.length; f++) {
            World.Province pr = w.provinces[World.capitalIndex(f)];
            if (pr.tx < x0 - 4 || pr.tx > x1 + 4 || pr.ty < y0 - 2 || pr.ty > y1 + 8) continue;
            int id = f == 0 ? TileAtlas.CASTLE : TileAtlas.CASTLE3D;
            TileAtlas.drawSprite(ctx, id, pr.tx, pr.ty, 0xFFFFFFFF);
            if (f > 0) drawFlag(ctx, pr.tx * 16 + 8 + TileAtlas.spriteW(id) / 2 - 6, pr.ty * 16 + 16 - TileAtlas.spriteH(id) - 6, f);
        }
    }

    /** Військо гравця біля столиці: козаки, мушкетери, кіннота й гармата. */
    private void drawArmy(DrawContext ctx, int x0, int y0, int x1, int y1) {
        World.Province cap = GameState.world.provinces[World.capitalIndex(0)];
        if (cap.tx < x0 - 8 || cap.tx > x1 + 8 || cap.ty < y0 - 4 || cap.ty > y1 + 8) return;
        int[] counts = {
            Math.min(4, (GameState.army[0] + 4) / 5),
            Math.min(4, (GameState.army[1] + 2) / 3),
            Math.min(3, (GameState.army[2] + 1) / 2)
        };
        int[] ids = {TileAtlas.UNIT_PIKE, TileAtlas.UNIT_MUSK, TileAtlas.UNIT_CAV};
        int x = cap.tx - 4;
        int y = cap.ty + 2;
        for (int g = 0; g < 3; g++) {
            for (int k = 0; k < counts[g]; k++) {
                TileAtlas.drawSprite(ctx, ids[g], x, y, 0xFFFFFFFF);
                x += 1;
            }
        }
        if (GameState.armyPower() >= 150) TileAtlas.drawSprite(ctx, TileAtlas.UNIT_CANNON, x + 1, y, 0xFFFFFFFF);
    }

    /** Кораблі стоять на воді біля портів гравця. */
    private void drawShips(DrawContext ctx, int x0, int y0, int x1, int y1) {
        if (GameState.ships <= 0) return;
        World w = GameState.world;
        final int T = World.TW;
        java.util.List<Integer> ports = new java.util.ArrayList<>();
        for (World.Province pr : w.provinces) {
            Integer t = pr.buildingTile.get(Building.PORT);
            if (t != null && pr.owner == 0) ports.add(t);
        }
        if (ports.isEmpty()) return;
        int per = GameState.ships / ports.size();
        int extra = GameState.ships % ports.size();
        for (int k = 0; k < ports.size(); k++) {
            int n = per + (k < extra ? 1 : 0);
            int cx = ports.get(k) % T;
            int cy = ports.get(k) / T;
            int placed = 0;
            for (int r = 1; r <= 4 && placed < n; r++) {
                for (int dy = -r; dy <= r && placed < n; dy++) {
                    for (int dx = -r; dx <= r && placed < n; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                        int nx = cx + dx;
                        int ny = cy + dy;
                        if (nx < 0 || ny < 0 || nx >= T || ny >= World.TH || w.base[ny * T + nx] > TileAtlas.SHORE1) continue;
                        placed++;
                        if (nx < x0 - 1 || nx > x1 + 1 || ny < y0 - 1 || ny > y1 + 1) continue;
                        TileAtlas.drawSprite(ctx, TileAtlas.SHIP, nx, ny, 0xFFFFFFFF);
                    }
                }
            }
        }
    }

    private int tileAt(double mx, double my) {
        if (!inMap(mx, my)) return -1;
        float ts = tileSize();
        int tx = (int) Math.floor((mx - mapX() - panX) / ts);
        int ty = (int) Math.floor((my - TOP - panY) / ts);
        if (tx < 0 || ty < 0 || tx >= World.TW || ty >= World.TH) return -1;
        return ty * World.TW + tx;
    }

    /** Напівпрозорий «привид» будівлі під курсором під час розміщення. */
    private void drawPlacementGhost(DrawContext ctx, int mouseX, int mouseY) {
        World.Province pr = buildProvince();
        if (placing == null || pr == null) return;
        int t = tileAt(mouseX, mouseY);
        if (t < 0) return;
        float k = tileSize() / 16f;
        int spr = TileAtlas.buildingSprite(placing);
        int sw = TileAtlas.spriteW(spr);
        int sh = TileAtlas.spriteH(spr);
        int tx = t % World.TW;
        int ty = t / World.TW;
        int x = Math.round(mapX() + panX + (tx * 16 + 8 - sw / 2f) * k);
        int y = Math.round(TOP + panY + (ty * 16 + 16 - sh) * k);
        boolean ok = Realm.canPlace(pr, placing, t);
        TileAtlas.draw(ctx, spr, x, y, Math.max(2, Math.round(sw * k)), Math.max(2, Math.round(sh * k)),
                ok ? 0xB0FFFFFF : 0x80FF6060);
    }

    private void handlePlacementClick(double mx, double my) {
        World.Province pr = buildProvince();
        int t = tileAt(mx, my);
        if (pr == null) {
            placing = null;
            selectAt(mx, my);
            return;
        }
        if (t < 0) return;
        if (GameState.world.provOf[t] != selected) {
            placing = null;
            selectAt(mx, my);
            return;
        }
        if (Realm.buildAt(pr, placing, t)) placing = null;
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

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF1F150B);
        for (int y = 0; y < this.height; y += 5) {
            ctx.fill(0, y, this.width, y + 1, 0x16000000);
        }
    }

    /** Пергаментна рамка навколо карти з кутовими прикрасами. */
    private void parchmentFrame(DrawContext ctx, int x1, int y1, int x2, int y2) {
        int f = 4;
        ctx.fill(x1 - f - 1, y1 - f - 1, x2 + f + 1, y2 + f + 1, 0xFF3B2A14);
        ctx.fill(x1 - f, y1 - f, x2 + f, y2 + f, 0xFFD9C690);
        ctx.drawBorder(x1 - f + 1, y1 - f + 1, (x2 - x1) + 2 * f - 2, (y2 - y1) + 2 * f - 2, 0xFF9B7B3C);
        int[] cx = {x1 - f - 1, x2 + f - 4};
        int[] cy = {y1 - f - 1, y2 + f - 4};
        for (int a : cx) {
            for (int b : cy) {
                ctx.fill(a, b, a + 5, b + 5, 0xFF8A6A2E);
                ctx.fill(a + 1, b + 1, a + 4, b + 4, 0xFFE9D9A4);
            }
        }
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF5A4122);
    }

    /** Гербовий щит 7x9 «пікселів» (по s px), малюнок залежить від pattern. */
    private void drawShield(DrawContext ctx, int cx, int top, int s, int color, int pattern) {
        int[] widths = {7, 7, 7, 7, 7, 5, 5, 3, 1};
        int dark = 0xFF2A1A0A;
        int light = 0xFFF5E6B8;
        int col = 0xFF000000 | (color & 0xFFFFFF);
        for (int r = 0; r < widths.length; r++) {
            int w = widths[r] * s;
            int x0 = cx - w / 2;
            int y0 = top + r * s;
            ctx.fill(x0 - 1, y0, x0 + w + 1, y0 + s, dark);
        }
        ctx.fill(cx - (7 * s) / 2 - 1, top - 1, cx + (7 * s) / 2 + 2, top, dark);
        ctx.fill(cx - s / 2 - 1, top + 9 * s, cx - s / 2 + s + 1, top + 9 * s + 1, dark);
        for (int r = 0; r < widths.length; r++) {
            int w = widths[r] * s;
            ctx.fill(cx - w / 2, top + r * s, cx - w / 2 + w, top + (r + 1) * s, col);
        }
        int mid = cx - s / 2;
        switch (pattern) {
            case 0 -> { // хрест
                ctx.fill(mid, top + s, mid + s, top + 7 * s, light);
                ctx.fill(cx - (5 * s) / 2, top + 3 * s, cx + (5 * s) / 2 + (s % 2), top + 4 * s, light);
            }
            case 1 -> ctx.fill(cx - (7 * s) / 2, top + 3 * s, cx + (7 * s) / 2 + (s % 2), top + 5 * s, light); // смуга
            case 2 -> { // шеврон
                ctx.fill(mid, top + 2 * s, mid + s, top + 3 * s, light);
                ctx.fill(mid - s, top + 3 * s, mid + 2 * s, top + 4 * s, light);
                ctx.fill(mid - 2 * s, top + 4 * s, mid + 3 * s, top + 5 * s, light);
            }
            case 3 -> ctx.fill(mid - s, top + 2 * s, mid + 2 * s, top + 5 * s, light); // коло
            default -> ctx.fill(mid, top, mid + s, top + 8 * s, light); // вертикальна смуга
        }
    }

    private int shieldColor(int owner) {
        return owner == 0 ? 0xFFD83A32 : World.FACTIONS[owner].color();
    }

    /** Гербові щити на столицях держав і на містах. */
    private void drawShields(DrawContext ctx) {
        if (!politicalMode) return;
        World w = GameState.world;
        float ts = tileSize();
        int mx1 = mapX();
        int my1 = TOP;
        int mx2 = mapX() + mapW();
        int my2 = TOP + mapH();
        if (ts >= 5f) {
            int s = 1;
            for (World.Province pr : w.provinces) {
                if (!pr.explored) continue;
                for (World.Point p : pr.points) {
                    if (p.type != World.PointType.TOWN) continue;
                    int sx = Math.round(mapX() + panX + (p.px + 0.5f) * ts);
                    int sy = Math.round(TOP + panY + (p.py - 0.2f) * ts);
                    if (sx < mx1 - 8 || sx > mx2 + 8 || sy < my1 - 12 || sy > my2 + 4) continue;
                    drawShield(ctx, sx, sy - 9 * s, s, shieldColor(p.owner), Math.floorMod(p.owner, 5));
                }
            }
        }
        int s = ts < 4f ? 2 : 3;
        for (int f = 0; f < Countries.ALL.length; f++) {
            World.Province pr = w.provinces[World.capitalIndex(f)];
            if (!pr.explored) continue;
            int sx = Math.round(mapX() + panX + (pr.tx + 0.5f) * ts);
            int sy = Math.round(TOP + panY + (pr.ty + 0.5f) * ts);
            if (sx < mx1 - 12 || sx > mx2 + 12 || sy < my1 - 16 || sy > my2 + 12) continue;
            drawShield(ctx, sx, sy - 5 * s, s, shieldColor(f), Math.floorMod(f, 5));
        }
    }

    // ---------- нижня панель будівель ----------

    private int stripX() { return mapX(); }
    private int stripY() { return this.height - 64; }
    private int slotSize() { return Math.max(14, Math.min(22, (mapW() - 104) / BLD.length - 2)); }
    private int slotX(int i) { return stripX() + 98 + i * (slotSize() + 2); }

    private World.Province buildProvince() {
        if (selected < 0) return null;
        World.Province pr = GameState.world.provinces[selected];
        return Realm.isMine(pr) ? pr : null;
    }

    private int buildingIcon(Building b) {
        return TileAtlas.buildingSprite(b);
    }

    private void drawBuildStrip(DrawContext ctx, int mouseX, int mouseY) {
        int x = stripX();
        int y = stripY();
        panel(ctx, x, y, mapW(), 24);
        World.Province pr = buildProvince();
        if (placing != null) {
            text(ctx, textRenderer.trimToWidth("Постав: " + placing.label, 92), x + 5, y + 3, 0x88FF88);
            text(ctx, textRenderer.trimToWidth("ПКМ — скасувати", 92), x + 5, y + 13, 0xE9D9A4);
        } else if (pr == null) {
            text(ctx, textRenderer.trimToWidth("Обери свою провінцію", 90), x + 5, y + 8, 0xAA9966);
        } else {
            text(ctx, textRenderer.trimToWidth(pr.name, 90), x + 5, y + 3, 0xFFD700);
            text(ctx, Realm.tierName(pr) + " " + pr.buildings.size() + "/" + BLD.length, x + 5, y + 13, 0xE9D9A4);
        }
        int ss = slotSize();
        String tip = null;
        int tipX = 0;
        for (int i = 0; i < BLD.length; i++) {
            Building b = BLD[i];
            int sx = slotX(i);
            int sy = y + 2;
            boolean built = pr != null && pr.buildings.contains(b);
            boolean can = pr != null && Realm.canBuild(pr, b);
            ctx.fill(sx, sy, sx + ss, sy + ss, 0xFF3A2A18);
            TileAtlas.drawIcon(ctx, buildingIcon(b), sx + 1, sy + 1, ss - 2, 0xFFFFFFFF);
            if (!built) ctx.fill(sx + 1, sy + 1, sx + ss - 1, sy + ss - 1, can ? 0x30000000 : 0xB0000000);
            ctx.drawBorder(sx, sy, ss, ss, placing == b ? 0xFFFFFFFF : built ? 0xFF55AA55 : can ? 0xFFD9B04A : 0xFF5A4122);
            if (mouseX >= sx && mouseX < sx + ss && mouseY >= sy && mouseY < sy + ss) {
                tipX = sx;
                if (built) tip = b.label + ": " + b.effect;
                else if (b.epoch > Realm.epoch) tip = b.label + " (потрібна епоха " + b.epoch + ")";
                else if (pr != null && b == Building.PORT && !Realm.coastal(pr)) tip = b.label + " (потрібне море)";
                else tip = b.label + ": " + b.effect + " — " + Realm.costGold(b) + "з " + Realm.costWood(b) + "д"
                        + (Realm.costIron(b) > 0 ? " " + Realm.costIron(b) + "з." : "");
            }
        }
        if (tip != null) {
            int tw = textRenderer.getWidth(tip) + 8;
            int tx = Math.max(MARGIN, Math.min(tipX, this.width - tw - MARGIN));
            panel(ctx, tx, y - 14, tw, 13);
            text(ctx, tip, tx + 4, y - 11, 0xFFE9B8);
        }
    }

    private int miniColor(World.Province pr) {
        if (!pr.explored) return FOG_COLOR;
        if (pr.terrain == World.Terrain.WATER) return 0xFF9AA4AE;
        return mix(0xFFE8D9A8, World.FACTIONS[pr.owner].color(), 0.85f);
    }

    private void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0xEE2A1C10);
        ctx.drawBorder(x, y, w, h, 0xFF9B7B3C);
        ctx.drawBorder(x + 1, y + 1, w - 2, h - 2, 0xFF4A3418);
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
        if (button == 1 && placing != null) {
            placing = null;
            return true;
        }
        if (button != 0) return false;

        int zy = TOP + mapH() - 17;
        if (my >= zy && my < zy + 15) {
            for (int k = 0; k < 5; k++) {
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
                    } else if (k == 3) {
                        showHexGrid = !showHexGrid;
                    } else {
                        politicalMode = !politicalMode;
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

        World.Province bp = buildProvince();
        if (bp != null) {
            for (int i = 0; i < BLD.length; i++) {
                int sx = slotX(i);
                int sy = stripY() + 2;
                if (mx >= sx && mx < sx + slotSize() && my >= sy && my < sy + slotSize()) {
                    if (placing == BLD[i]) {
                        placing = null;
                    } else if (Realm.canBuild(bp, BLD[i])) {
                        placing = BLD[i]; // далі клікни на зелену клітинку своєї провінції
                    }
                    return true;
                }
            }
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
            if (moved < 4) {
                if (placing != null) handlePlacementClick(mx, my);
                else selectAt(mx, my);
            }
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
        int barW = Math.max(textRenderer.getWidth(line1), textRenderer.getWidth(line2)) + 16;
        panel(ctx, this.width / 2 - barW / 2, 2, barW, 28);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line1), this.width / 2, 6, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line2), this.width / 2, 18, 0xFFD700);

        // ----- ліва панель: території -----
        int mineProv = 0;
        for (World.Province pr : provinces) {
            if (pr.owner == 0) mineProv++;
        }
        int explored = GameState.exploredCount();
        panel(ctx, MARGIN, TOP, LEFT_W, 80);
        text(ctx, "Території", MARGIN + 4, TOP + 4, 0xFFD700);
        text(ctx, "Твоїх: " + mineProv, MARGIN + 4, TOP + 16, 0xFFFFFF);
        text(ctx, "Відкрито: " + explored + "/" + provinces.length, MARGIN + 4, TOP + 26, 0xFFFFFF);
        text(ctx, "Нас.: " + Realm.population + "/" + Realm.popCap(), MARGIN + 4, TOP + 36, 0xFFFFFF);
        text(ctx, "Наука: " + Realm.science + " (+" + Realm.scienceIncome() + ")", MARGIN + 4, TOP + 46, 0x88CCFF);
        text(ctx, "Сила армії: " + GameState.armyPower(), MARGIN + 4, TOP + 56, 0xFFFFFF);
        text(ctx, "Полонених: " + GameState.prisoners, MARGIN + 4, TOP + 66, 0xFFFFFF);

        // ----- ліва панель: відносини -----
        int ry = TOP + 86;
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

        parchmentFrame(ctx, vx1, vy1, vx2, vy2);
        ctx.fill(vx1, vy1, vx2, vy2, 0xFF0B0B0B);
        ctx.enableScissor(vx1, vy1, vx2, vy2);

        drawWorld(ctx, hovered);
        drawShields(ctx);
        drawPlacementGhost(ctx, mouseX, mouseY);
        drawLabels(ctx, hovered);
        ctx.disableScissor();
        drawBuildStrip(ctx, mouseX, mouseY);

        // ----- кнопки масштабу -----
        String[] zl = {"+", "-", "0", "#", "П"};
        int zy = TOP + mapH() - 17;
        for (int k = 0; k < 5; k++) {
            int bx = mapX() + 3 + k * 17;
            ctx.fill(bx, zy, bx + 15, zy + 15, 0xFF2A1C10);
            ctx.drawBorder(bx, zy, 15, 15, 0xFF9B7B3C);
            boolean on = (k == 3 && showHexGrid) || (k == 4 && politicalMode);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(zl[k]), bx + 8, zy + 4,
                    on ? 0xFFFF55 : 0xFFFFFF);
        }
        text(ctx, String.format("x%.1f", zoom), mapX() + 91, zy + 4, 0xDDDDDD);

        // ----- мінікарта -----
        int rx = rightX();
        panel(ctx, rx - 2, TOP - 2, miniW() + 4, miniH() + 4);
        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            int x = rx + pr.col * miniW() / World.COLS;
            int y = TOP + pr.row * miniH() / World.ROWS;
            int x2 = rx + (pr.col + 1) * miniW() / World.COLS;
            int y2 = TOP + (pr.row + 1) * miniH() / World.ROWS;
            ctx.fill(x, y, Math.max(x2, x + 1), Math.max(y2, y + 1), miniColor(pr));
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
                if (!GameState.isNearMine(selProv) && !GameState.seaReach(selProv)) {
                    info1 = "Занадто далеко";
                    info2 = GameState.ships > 0 ? "(потрібен порт поруч)" : "(потрібен флот)";
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
                    boolean sea = !GameState.isNearMine(selProv);
                    info1 = (sea ? "Десант " + GameState.landingPower() : "Сила " + GameState.armyPower())
                            + " проти ~" + approx;
                }
            }
        } else {
            text(ctx, "Натисни на провінцію", rx, hy, 0xFFFFFF);
        }

        int infoY = listTop() + 3 * ROW_H + 4;
        if (info1 != null) text(ctx, info1, rx, infoY, infoOk ? 0xFFFFFF : 0xFF5555);
        if (info2 != null) text(ctx, info2, rx, infoY + 12, infoOk ? 0xFFFFFF : 0xFF5555);

        // ----- журнал подій -----
        int logW = rightX() - GAP - MARGIN;
        String last = GameState.logLine(0);
        if (!last.isEmpty()) {
            text(ctx, textRenderer.trimToWidth(last, logW), MARGIN, this.height - 38, 0xFFD700);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
                                                   }
