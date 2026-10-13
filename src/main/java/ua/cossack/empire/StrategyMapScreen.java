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

    private int selected = -1;
    private int selectedPoint = -1;
    private ButtonWidget upgradeBtn;

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
            if (selected >= 0 && mx >= px() && mx < this.width - MARGIN && my >= TOP + 16) {
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
        World.Point sp = currentPoint();
        upgradeBtn.active = sp != null && GameState.canUpgrade(sp);
        upgradeBtn.setMessage(Text.literal(
                sp != null && sp.level < GameState.MAX_LEVEL ? "Покращити до ур." + (sp.level + 1) : "Покращити"));

        super.render(ctx, mouseX, mouseY, delta);

        String line1 = "Хід " + GameState.turn + "   " + res(World.ResourceType.GOLD)
                + "   " + res(World.ResourceType.FOOD) + "   " + res(World.ResourceType.WOOD);
        String line2 = res(World.ResourceType.IRON) + "   " + res(World.ResourceType.POWDER)
                + "   " + res(World.ResourceType.HORSES);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line1), this.width / 2, 6, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line2), this.width / 2, 20, 0xFFD700);

        float r = radius();
        int hovered = provinceAt(mouseX, mouseY);
        World.Province[] provinces = GameState.world.provinces;

        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < provinces.length; i++) {
                boolean isSel = i == selected;
                if ((pass == 0) == isSel) continue;
                World.Province pr = provinces[i];
                float cx = centerX(pr.col, pr.row);
                float cy = centerY(pr.row);
                hex(ctx, cx, cy, r, isSel ? 0xFFFFFFFF : 0xFF101010);
                hex(ctx, cx, cy, r - 1.5f, World.FACTIONS[pr.owner].color());
                if (i == hovered) {
                    hex(ctx, cx, cy, r - 1.5f, 0x40FFFFFF);
                }
            }
        }

        int px = px();
        int pw = this.width - px - MARGIN;
        if (selected >= 0) {
            World.Province pr = provinces[selected];
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(pr.name + " — " + World.FACTIONS[pr.owner].name(), pw)),
                    px, TOP, 0xFFD700);
            int y = TOP + 16;
            for (int i = 0; i < pr.points.size(); i++) {
                World.Point p = pr.points.get(i);
                if (i == selectedPoint) {
                    ctx.fill(px - 2, y - 1, px + pw, y + ROW_H - 1, 0x40FFFFFF);
                }
                String s = p.type.label + " " + p.name + " ур." + p.level + "  +" + p.production() + " "
                        + p.type.resource.label + (p.unrest >= 20 ? " !" : "");
                ctx.drawTextWithShadow(textRenderer,
                        Text.literal(textRenderer.trimToWidth(s, pw)),
                        px, y, p.unrest >= 20 ? 0xFF5555 : 0xFFFFFF);
                y += ROW_H;
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Натисни на провінцію"), px, TOP, 0xFFFFFF);
        }

        if (sp != null) {
            String info;
            if (sp.owner != 0) {
                info = "Це не твоя точка";
            } else if (sp.level >= GameState.MAX_LEVEL) {
                info = "Максимальний рівень";
            } else {
                info = "Ціна: Золото " + GameState.costGold(sp) + ", Дерево " + GameState.costWood(sp)
                        + ", Залізо " + GameState.costIron(sp);
            }
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(info, pw)),
                    px, this.height - 68, upgradeBtn.active ? 0xFFFFFF : 0xFF5555);
        }

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Зерно: " + GameState.world.seed), MARGIN, this.height - 22, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
