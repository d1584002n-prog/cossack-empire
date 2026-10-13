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

    private static final String[] SHORT =
            {"Козаки", "Польща", "Московія", "Османи", "Татари", "Швеція", "Повстанці"};
    private static final String[] SEASONS = {"Весна", "Літо", "Осінь", "Зима"};
    private static final String[] MENU = {"Армія", "Міста", "Дипломатія", "Економіка", "Технології"};

    private int selected = -1;
    private int selectedPoint = -1;
    private ButtonWidget upgradeBtn;
    private ButtonWidget spyBtn;
    private ButtonWidget attackBtn;

    public StrategyMapScreen() {
        super(Text.literal("Козацька Держава"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Нова гра"), b -> {
                    GameState.newGame();
                    selected = -1;
                    selectedPoint = -1;
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

    // ---------- геометрія ----------

    private int mapX() { return MARGIN + LEFT_W + GAP; }
    private int rightX() { return this.width - RIGHT_W - MARGIN; }
    private int mapW() { return rightX() - GAP - mapX(); }
    private int listTop() { return TOP + 60; }

    private float radius() {
        float availH = this.height - TOP - BOTTOM;
        float rw = mapW() / (SQ3 * (World.COLS + 0.5f));
        float rh = availH / (1.5f * World.ROWS + 0.5f);
        return Math.min(rw, rh);
    }

    private float centerX(int col, int row) {
        float r = radius();
        return mapX() + r * SQ3 / 2f + r * SQ3 * (col + 0.5f * (row & 1));
    }

    private float centerY(int row) {
        float r = radius();
        return TOP + r + 1.5f * r * row;
    }

    private int provinceAt(double mx, double my) {
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

    // ---------- малювання ----------

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

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int idx = provinceAt(mx, my);
            if (idx >= 0) {
                selected = idx;
                selectedPoint = -1;
                World.Province pr = GameState.world.provinces[idx];
                if (pr.explored) {
                    for (int i = 0; i < pr.points.size(); i++) {
                        if (pr.points.get(i).owner != 0) {
                            selectedPoint = i;
                            break;
                        }
                    }
                    if (selectedPoint < 0 && !pr.points.isEmpty()) selectedPoint = 0;
                }
                return true;
            }
            if (selected >= 0 && GameState.world.provinces[selected].explored
                    && mx >= rightX() && mx < rightX() + RIGHT_W && my >= listTop()) {
                int row = (int) ((my - listTop()) / ROW_H);
                if (row < GameState.world.provinces[selected].points.size()) {
                    selectedPoint = row;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private String res(World.ResourceType r) {
        return r.label + " " + GameState.stock.get(r) + " (+" + GameState.income(r) + ")";
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        World.Province[] provinces = GameState.world.provinces;
        World.Province selProv = selected >= 0 ? provinces[selected] : null;
        boolean hiddenSel = selProv != null && !selProv.explored;
        World.Point sp = hiddenSel ? null : currentPoint();
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
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < provinces.length; i++) {
                boolean isSel = i == selected;
                if ((pass == 0) == isSel) continue;
                World.Province pr = provinces[i];
                float cx = centerX(pr.col, pr.row);
                float cy = centerY(pr.row);
                hex(ctx, cx, cy, r, isSel ? 0xFFFFFFFF : 0xFF101010);
                hex(ctx, cx, cy, r - 1.5f, pr.explored ? World.FACTIONS[pr.owner].color() : FOG_COLOR);
                if (i == hovered) {
                    hex(ctx, cx, cy, r - 1.5f, 0x40FFFFFF);
                }
                if (!pr.explored) {
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("?"),
                            Math.round(cx), Math.round(cy) - 4, 0xAAAAAA);
                }
            }
        }

        // ----- права колонка: мінікарта і дата -----
        int rx = rightX();
        panel(ctx, rx - 2, TOP - 2, World.COLS * MINI + 7, World.ROWS * MINI + 4);
        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            int x = rx + pr.col * MINI + (pr.row & 1) * 2;
            int y = TOP + pr.row * MINI;
            ctx.fill(x, y, x + 4, y + 4,
                    pr.explored ? World.FACTIONS[pr.owner].color() : FOG_COLOR);
            if (i == selected) {
                ctx.drawBorder(x - 1, y - 1, 6, 6, 0xFFFFFFFF);
            }
        }
        int year = 1572 + (GameState.turn - 1) / 4;
        String season = SEASONS[(GameState.turn - 1) % 4];
        text(ctx, "Рік " + year, rx + 66, TOP, 0xFFD700);
        text(ctx, season, rx + 66, TOP + 12, 0xFFFFFF);
        text(ctx, "Хід " + GameState.turn, rx + 66, TOP + 24, 0xAAAAAA);

        // ----- права колонка: вибрана провінція -----
        String info1 = null;
        String info2 = null;
        boolean infoOk = true;
        int hy = TOP + 44;

        if (selProv != null && !hiddenSel) {
            text(ctx, selProv.name + " — " + SHORT[selProv.owner], rx, hy, 0xFFD700);
            int y = listTop();
            for (int i = 0; i < selProv.points.size(); i++) {
                World.Point p = selProv.points.get(i);
                if (i == selectedPoint) {
                    ctx.fill(rx - 2, y - 1, rx + RIGHT_W, y + ROW_H - 1, 0x40FFFFFF);
                }
                String right = "ур." + p.level + " +" + p.production() + " " + p.type.resource.label
                        + (p.unrest >= 20 ? " !" : "");
                int rw = textRenderer.getWidth(right);
                int color = p.owner != 0 ? 0xAAAAAA : (p.unrest >= 20 ? 0xFF5555 : 0xFFFFFF);
                text(ctx, textRenderer.trimToWidth(p.name, RIGHT_W - rw - 6), rx, y, color);
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
        } else if (hiddenSel) {
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
