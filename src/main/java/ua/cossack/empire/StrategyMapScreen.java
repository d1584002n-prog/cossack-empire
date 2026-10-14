package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StrategyMapScreen extends Screen {
    private static final float SQ3 = 1.7320508f;
    private static final int TOP = 34;
    private static final int BOTTOM = 66;
    private static final int MARGIN = 6;
    private static final int GAP = 6;
    private static final int LEFT_W = 96;
    private static final int RIGHT_W = 176;
    private static final int ROW_H = 12;
    private static final int MINI = 5;
    private static final int FOG_COLOR = 0xFF404040;
    private static final float MAX_ZOOM = 4f;

    private static final String[] SHORT =
            {"Козаки", "Польща", "Московія", "Османи", "Татари", "Швеція", "Повстанці", "Вода"};
    private static final String[] SEASONS = {"Весна", "Літо", "Осінь", "Зима"};
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

    public StrategyMapScreen() {
        super(Text.literal("Козацька Держава"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Нова гра"), b -> {
                    GameState.newGame();
                    selected = -1;
                    selectedPoint = -1;
                    zoom = 1f;
                    panX = 0f;
                    panY = 0f;
                })
                .dimensions(MARGIN, 4, 62, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Наступний хід"), b -> GameState.nextTurn())
                .dimensions(this.width - 116, this.height - 26, 110, 20)
                .build());

        for (int i = 0; i < MENU.length; i++) {
            final int idx = i;
            ButtonWidget b = ButtonWidget.builder(Text.literal(MENU[i]), btn -> openTab(idx))
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
    }

    // ---------- геометрія і масштаб ----------

    private int mapX() { return MARGIN + LEFT_W + GAP; }
    private int rightX() { return this.width - RIGHT_W - MARGIN; }
    private int mapW() { return rightX() - GAP - mapX(); }
    private int mapH() { return this.height - TOP - BOTTOM; }
    private int miniW() { return World.COLS * MINI + MINI / 2; }
    private int miniH() { return World.ROWS * MINI; }

    private float baseR() {
        return Math.min(mapW() / (SQ3 * (World.COLS + 0.5f)), mapH() / (1.5f * World.ROWS + 0.5f));
    }

    private float radius() { return baseR() * zoom; }
    private float totalW() { return radius() * SQ3 * (World.COLS + 0.5f); }
    private float totalH() { return radius() * (1.5f * World.ROWS + 0.5f); }

    private float centerX(int col, int row) {
        float r = radius();
        return mapX() + panX + r * SQ3 / 2f + r * SQ3 * (col + 0.5f * (row & 1));
    }

    private float centerY(int row) {
        float r = radius();
        return TOP + panY + r + 1.5f * r * row;
    }

    private void clampPan() {
        float tw = totalW();
        float th = totalH();
        panX = tw <= mapW() ? (mapW() - tw) / 2f : Math.max(mapW() - tw, Math.min(0f, panX));
        panY = th <= mapH() ? (mapH() - th) / 2f : Math.max(mapH() - th, Math.min(0f, panY));
    }

    private void zoomAt(double mx, double my, float factor) {
        float oldR = radius();
        float newZoom = Math.max(1f, Math.min(MAX_ZOOM, zoom * factor));
        if (newZoom == zoom) return;
        float u = (float) ((mx - mapX() - panX) / oldR);
        float v = (float) ((my - TOP - panY) / oldR);
        zoom = newZoom;
        float newR = radius();
        panX = (float) (mx - mapX() - u * newR);
        panY = (float) (my - TOP - v * newR);
        clampPan();
    }

    private boolean inMap(double mx, double my) {
        return mx >= mapX() && mx < mapX() + mapW() && my >= TOP && my < TOP + mapH();
    }

    private int provinceAt(double mx, double my) {
        if (!inMap(mx, my)) return -1;
        float r = radius();
        int best = -1;
        double bestD = r * r;
        World.Province[] provinces = GameState.world.provinces;
        for (int i = 0; i < provinces.length; i++) {
            double dx = mx - centerX(provinces[i].col, provinces[i].row);
            double dy = my - centerY(provinces[i].row);
            double d = dx * dx + dy * dy;
            if (d <= bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
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
        return mix(pr.terrain.color, World.FACTIONS[pr.owner].color(), 0.45f);
    }

    private void hex(DrawContext ctx, float cx, float cy, float r, int color) {
        float hw = SQ3 / 2f * r;
        int ir = (int) Math.ceil(r);
        for (int dy = -ir; dy < ir; dy++) {
            float a = Math.abs(dy + 0.5f);
            float half = a <= r / 2f ? hw : hw * (r - a) / (r / 2f);
            if (half <= 0f) continue;
            int y = Math.round(cy + dy);
            ctx.fill(Math.round(cx - half), y, Math.round(cx + half), y + 1, color);
        }
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

    private void decorate(DrawContext ctx, World.Province pr, float cx, float cy, float r) {
        int s = Math.max(1, Math.round(r / 7f));
        int x = Math.round(cx);
        int y = Math.round(cy);
        switch (pr.terrain) {
            case FOREST -> {
                ctx.fill(x - 2 * s, y - s, x - s, y + s, 0xFF1F4A1C);
                ctx.fill(x + s, y, x + 2 * s, y + 2 * s, 0xFF1F4A1C);
                ctx.fill(x - s, y + s, x, y + 2 * s, 0xFF2A5E26);
            }
            case MOUNTAIN -> {
                ctx.fill(x, y - 2 * s, x + s, y - s, 0xFFF0F0F0);
                ctx.fill(x - s, y - s, x + 2 * s, y, 0xFF5A5A5A);
                ctx.fill(x - 2 * s, y, x + 3 * s, y + s, 0xFF4A4A4A);
            }
            case WATER -> ctx.fill(x - 2 * s, y, x + 2 * s, y + 1, 0xFF6FA0D0);
            case MEADOW -> {
                ctx.fill(x - 2 * s, y - s, x - 2 * s + 1, y - s + 1, 0xFFFFE066);
                ctx.fill(x + s, y + s, x + s + 1, y + s + 1, 0xFFFFFFFF);
            }
            case STEPPE -> {
                ctx.fill(x - s, y, x - s + 1, y + 2 * s, 0xFFD8C96A);
                ctx.fill(x + s, y - s, x + s + 1, y + s, 0xFFD8C96A);
            }
            default -> { }
        }
    }

    private void flag(DrawContext ctx, World.Province pr, float cx, float cy, float r) {
        boolean town = false;
        for (World.Point p : pr.points) {
            if (p.type == World.PointType.TOWN) town = true;
        }
        if (!town || pr.owner == World.WATER_OWNER || r < 6f) return;
        int fs = Math.max(1, Math.round(r / 8f));
        int fx = Math.round(cx + r * 0.25f);
        int fy = Math.round(cy - r * 0.6f);
        ctx.fill(fx, fy, fx + 1, fy + 5 * fs, 0xFF3B2A14);
        ctx.fill(fx + 1, fy, fx + 1 + 3 * fs, fy + 2 * fs, World.FACTIONS[pr.owner].color());
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
            for (int k = 0; k < 3; k++) {
                int bx = mapX() + 3 + k * 17;
                if (mx >= bx && mx < bx + 15) {
                    double cx = mapX() + mapW() / 2.0;
                    double cy = TOP + mapH() / 2.0;
                    if (k == 0) {
                        zoomAt(cx, cy, 1.4f);
                    } else if (k == 1) {
                        zoomAt(cx, cy, 1f / 1.4f);
                    } else {
                        zoom = 1f;
                        panX = 0f;
                        panY = 0f;
                        clampPan();
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
        for (int f = 1; f <= 5; f++) {
            int y = ry + 16 + (f - 1) * 10;
            int nameColor = GameState.atWar[f] ? 0xFF5555 : GameState.allied[f] ? 0x55FF55 : 0xFFFFFF;
            int rel = GameState.relation[f];
            int relColor = rel < 0 ? 0xFF8888 : rel > 0 ? 0x88FF88 : 0xDDDDDD;
            text(ctx, SHORT[f], MARGIN + 4, y, nameColor);
            textRight(ctx, String.valueOf(rel), MARGIN + LEFT_W - 4, y, relColor);
        }

        // ----- карта -----
        float r = radius();
        int hovered = provinceAt(mouseX, mouseY);
        int vx1 = mapX();
        int vy1 = TOP;
        int vx2 = mapX() + mapW();
        int vy2 = TOP + mapH();

        ctx.fill(vx1 - 1, vy1 - 1, vx2 + 1, vy2 + 1, 0xFF6B5A2E);
        ctx.fill(vx1, vy1, vx2, vy2, 0xFF0B0B0B);
        ctx.enableScissor(vx1, vy1, vx2, vy2);

        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < provinces.length; i++) {
                boolean isSel = i == selected;
                if ((pass == 1) != isSel) continue;
                World.Province pr = provinces[i];
                float cx = centerX(pr.col, pr.row);
                float cy = centerY(pr.row);
                if (!visible(cx, cy, r)) continue;
                if (isSel) {
                    hex(ctx, cx, cy, r, 0xFFFFFFFF);
                    hex(ctx, cx, cy, r - 1.6f, provColor(pr));
                } else {
                    hex(ctx, cx, cy, r - 0.6f, provColor(pr));
                }
                if (i == hovered) hex(ctx, cx, cy, r - 0.6f, 0x30FFFFFF);
            }
        }

        int rw = Math.max(1, Math.round(r / 9f));
        for (World.Province pr : provinces) {
            if (!pr.explored || pr.riverNext < 0) continue;
            World.Province nx = provinces[pr.riverNext];
            if (!nx.explored) continue;
            line(ctx, centerX(pr.col, pr.row), centerY(pr.row),
                    centerX(nx.col, nx.row), centerY(nx.row), rw, 0xFF5CA0E0);
        }

        for (World.Province pr : provinces) {
            float cx = centerX(pr.col, pr.row);
            float cy = centerY(pr.row);
            if (!visible(cx, cy, r)) continue;
            if (!pr.explored) {
                if (r >= 6f) {
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("?"),
                            Math.round(cx), Math.round(cy) - 4, 0xAAAAAA);
                }
                continue;
            }
            decorate(ctx, pr, cx, cy, r);
            flag(ctx, pr, cx, cy, r);
            if (r >= 15f && pr.terrain != World.Terrain.WATER) {
                int tw = textRenderer.getWidth(pr.name);
                float sc = Math.min(0.8f, (r * SQ3 * 0.9f) / tw);
                if (sc >= 0.45f) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(cx, cy + r * 0.3f, 0f);
                          ctx.getMatrices().scale(sc, sc, 1f);
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(pr.name), 0, 0, 0xFFFFFF);
                    ctx.getMatrices().pop();
                }
            }
        }
        ctx.disableScissor();

        // ----- кнопки масштабу -----
        String[] zl = {"+", "-", "0"};
        int zy = TOP + mapH() - 17;
        for (int k = 0; k < 3; k++) {
            int bx = mapX() + 3 + k * 17;
            panel(ctx, bx, zy, 15, 15);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(zl[k]), bx + 8, zy + 4, 0xFFFFFF);
        }
        text(ctx, String.format("x%.1f", zoom), mapX() + 57, zy + 4, 0xDDDDDD);

        // ----- мінікарта -----
        int rx = rightX();
        panel(ctx, rx - 2, TOP - 2, miniW() + 4, miniH() + 4);
        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            int x = rx + pr.col * MINI + (pr.row & 1) * (MINI / 2);
            int y = TOP + pr.row * MINI;
            ctx.fill(x, y, x + MINI - 1, y + MINI - 1, provColor(pr));
        }
        float tw = totalW();
        float th = totalH();
        float fx0 = Math.max(0f, -panX / tw);
        float fy0 = Math.max(0f, -panY / th);
        float fw = Math.min(1f - fx0, mapW() / tw);
        float fh = Math.min(1f - fy0, mapH() / th);
        ctx.drawBorder(rx + Math.round(fx0 * miniW()), TOP + Math.round(fy0 * miniH()),
                Math.max(3, Math.round(fw * miniW())), Math.max(3, Math.round(fh * miniH())), 0xFFFFFFFF);

        int year = 1572 + (GameState.turn - 1) / 4;
        String season = SEASONS[(GameState.turn - 1) % 4];
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
                info1 = "Шпигун: Золото " + GameState.SPY_GOLD + ",";
                info2 = "Коні " + GameState.SPY_HORSES;
                infoOk = spyBtn.active;
            }
        } else if (waterSel) {
            text(ctx, "Озеро / море", rx, hy, 0xFFD700);
            text(ctx, "Непрохідна територія", rx, hy + 11, 0xAAAAAA);
        } else if (selProv != null) {
            text(ctx, selProv.name + " — " + SHORT[selProv.owner], rx, hy, 0xFFD700);
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
