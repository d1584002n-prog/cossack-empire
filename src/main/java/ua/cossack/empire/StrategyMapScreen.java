package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StrategyMapScreen extends Screen {
    private static final float SQ3 = 1.7320508f;
    private static final int TOP = 44;
    private static final int BOTTOM = 34;
    private static final int MARGIN = 12;
    private static final int ROW_H = 12;
    private static final int FOG_COLOR = 0xFF404040;

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
        addDrawableChild(ButtonWidget.builder(Text.literal("Наступний хід"), b -> GameState.nextTurn())
                .dimensions(this.width - 126, this.height - 28, 114, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Нова гра"), b -> {
                    GameState.newGame();
                    selected = -1;
                    selectedPoint = -1;
                })
                .dimensions(this.width - 246, this.height - 28, 114, 20)
                .build());

        upgradeBtn = ButtonWidget.builder(Text.literal("Покращити"), b -> {
                    World.Point p = currentPoint();
                    if (p != null) GameState.upgrade(p);
                })
                .dimensions(px(), this.height - 54, 114, 20)
                .build();
        addDrawableChild(upgradeBtn);

        spyBtn = ButtonWidget.builder(Text.literal("Надіслати шпигуна"), b -> {
                    if (selected >= 0) GameState.sendSpy(GameState.world.provinces[selected]);
                })
                .dimensions(px(), this.height - 54, 114, 20)
                .build();
        addDrawableChild(spyBtn);

        attackBtn = ButtonWidget.builder(Text.literal("Атакувати"), b -> {
                    World.Point p = currentPoint();
                    if (p != null && selected >= 0) {
                        GameState.attack(GameState.world.provinces[selected], p);
                    }
                })
                .dimensions(px(), this.height - 54, 114, 20)
                .build();
        addDrawableChild(attackBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("Армія"), b -> this.client.setScreen(new ArmyScreen(this)))
                .dimensions(px() + 118, this.height - 54, 70, 20)
                .build());
    }

    private float radius() {
        float availW = (this.width - MARGIN * 2) * 0.64f;
        float availH = this.height - TOP - BOTTOM;
        float rw = availW / (SQ3 * (World.COLS + 0.5f));
        float rh = availH / (1.5f * World.ROWS + 0.5f);
        return Math.min(rw, rh);
    }

    private float centerX(int col, int row) {
        float r = radius();
        return MARGIN + r * SQ3 / 2f + r * SQ3 * (col + 0.5f * (row & 1));
    }

    private float centerY(int row) {
        float r = radius();
        return TOP + r + 1.5f * r * row;
    }

    private int px() {
        return (int) (MARGIN + radius() * SQ3 * (World.COLS + 0.5f)) + 12;
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

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int idx = provinceAt(mx, my);
            if (idx >= 0) {
                selected = idx;
                selectedPoint = -1;
                return true;
            }
            if (selected >= 0 && GameState.world.provinces[selected].explored
                    && mx >= px() && mx < this.width - MARGIN && my >= TOP + 16) {
                int row = (int) ((my - (TOP + 16)) / ROW_H);
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

        String line1 = "Хід " + GameState.turn + "   " + res(World.ResourceType.GOLD)
                + "   " + res(World.ResourceType.FOOD) + "   " + res(World.ResourceType.WOOD);
        String line2 = res(World.ResourceType.IRON) + "   " + res(World.ResourceType.POWDER)
                + "   " + res(World.ResourceType.HORSES);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line1), this.width / 2, 6, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line2), this.width / 2, 20, 0xFFD700);

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

        int px = px();
        int pw = this.width - px - MARGIN;
        String info = null;
        boolean infoOk = true;

        if (selProv != null && !hiddenSel) {
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(
                            selProv.name + " — " + World.FACTIONS[selProv.owner].name(), pw)),
                    px, TOP, 0xFFD700);
            int y = TOP + 16;
            for (int i = 0; i < selProv.points.size(); i++) {
                World.Point p = selProv.points.get(i);
                if (i == selectedPoint) {
                    ctx.fill(px - 2, y - 1, px + pw, y + ROW_H - 1, 0x40FFFFFF);
                }
                String s = p.type.label + " " + p.name + " ур." + p.level + "  +" + p.production() + " "
                        + p.type.resource.label + (p.unrest >= 20 ? " !" : "");
                int color = p.owner != 0 ? 0xAAAAAA : (p.unrest >= 20 ? 0xFF5555 : 0xFFFFFF);
                ctx.drawTextWithShadow(textRenderer,
                        Text.literal(textRenderer.trimToWidth(s, pw)),
                        px, y, color);
                y += ROW_H;
            }
            if (mine) {
                infoOk = upgradeBtn.active;
                if (sp.level >= GameState.MAX_LEVEL) {
                    info = "Максимальний рівень";
                } else {
                    info = "Ціна: Золото " + GameState.costGold(sp) + ", Дерево " + GameState.costWood(sp)
                            + ", Залізо " + GameState.costIron(sp);
                }
            } else if (enemy) {
                if (!GameState.isNearMine(selProv)) {
                    info = "Занадто далеко від твоїх земель";
                    infoOk = false;
                } else if (GameState.armyPower() == 0) {
                    info = "Немає війська";
                    infoOk = false;
                } else {
                    int approx = Math.round(sp.garrison / 5f) * 5;
                    info = "Сила: " + GameState.armyPower() + " проти гарнізону ~" + approx;
                }
            }
        } else if (hiddenSel) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Невідома територія"), px, TOP, 0xFFD700);
            if (!GameState.nextToExplored(selProv)) {
                info = "Спершу відкрий сусідню територію";
                infoOk = false;
            } else {
                info = "Шпигун: Золото " + GameState.SPY_GOLD + ", Коні " + GameState.SPY_HORSES;
                infoOk = spyBtn.active;
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Натисни на провінцію"), px, TOP, 0xFFFFFF);
        }

        ctx.drawTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth("Сила армії: " + GameState.armyPower()
                        + "   Полонених: " + GameState.prisoners, pw)),
                px, this.height - 92, 0xDDDDDD);
        if (!GameState.log.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(GameState.log, pw)),
                    px, this.height - 80, 0xFFD700);
        }
        if (info != null) {
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(info, pw)),
                    px, this.height - 68, infoOk ? 0xFFFFFF : 0xFF5555);
        }

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Відкрито: " + GameState.exploredCount() + "/" + provinces.length
                        + "   Зерно: " + GameState.world.seed),
                MARGIN, this.height - 22, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
