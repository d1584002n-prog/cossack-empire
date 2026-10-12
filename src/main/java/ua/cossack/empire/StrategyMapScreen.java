package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StrategyMapScreen extends Screen {
    private static final int TOP = 30;
    private static final int BOTTOM_PANEL = 70;
    private static final int MARGIN = 20;

    private int selected = -1;

    public StrategyMapScreen() {
        super(Text.literal("Козацька Держава"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Наступний хід"), b -> GameState.nextTurn())
                .dimensions(this.width - 130, this.height - 50, 110, 20)
                .build());
    }

    private int cellW() { return (this.width - MARGIN * 2) / GameState.COLS; }
    private int cellH() { return (this.height - TOP - BOTTOM_PANEL) / GameState.ROWS; }

    private int cellAt(double mx, double my) {
        int cx = (int) ((mx - MARGIN) / cellW());
        int cy = (int) ((my - TOP) / cellH());
        if (mx < MARGIN || my < TOP) return -1;
        if (cx < 0 || cx >= GameState.COLS || cy < 0 || cy >= GameState.ROWS) return -1;
        return cy * GameState.COLS + cx;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int idx = cellAt(mx, my);
            if (idx >= 0) {
                selected = idx;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        String top = "Хід " + GameState.turn
                + "   Золото: " + GameState.gold + " (+" + GameState.incomeGold() + ")"
                + "   Їжа: " + GameState.food + " (+" + GameState.incomeFood() + ")"
                + "   Дерево: " + GameState.wood + " (+" + GameState.incomeWood() + ")";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(top), this.width / 2, 10, 0xFFD700);

        int w = cellW();
        int h = cellH();
        int hovered = cellAt(mouseX, mouseY);

        for (int i = 0; i < GameState.PROVINCES.length; i++) {
            GameState.Province p = GameState.PROVINCES[i];
            int x = MARGIN + (i % GameState.COLS) * w;
            int y = TOP + (i / GameState.COLS) * h;

            context.fill(x + 2, y + 2, x + w - 2, y + h - 2, GameState.OWNER_COLORS[p.owner()]);
            if (i == hovered) {
                context.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x40FFFFFF);
            }
            if (i == selected) {
                context.drawBorder(x + 2, y + 2, w - 4, h - 4, 0xFFFFFFFF);
            }
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(p.name()),
                    x + w / 2, y + h / 2 - 4, 0xFFFFFF);
        }

        int py = this.height - BOTTOM_PANEL + 10;
        if (selected >= 0) {
            GameState.Province p = GameState.PROVINCES[selected];
            context.drawTextWithShadow(textRenderer,
                    Text.literal(p.name() + " — " + GameState.OWNERS[p.owner()]), MARGIN, py, 0xFFD700);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("Дохід за хід: золото " + p.gold() + ", їжа " + p.food() + ", дерево " + p.wood()),
                    MARGIN, py + 14, 0xFFFFFF);
        } else {
            context.drawTextWithShadow(textRenderer,
                    Text.literal("Натисни на провінцію"), MARGIN, py, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
