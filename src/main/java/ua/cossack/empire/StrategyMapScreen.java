package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class StrategyMapScreen extends Screen {
    private static final int COLS = 4;
    private static final int ROWS = 3;
    private static final int TOP = 44;
    private static final int BOTTOM = 34;
    private static final int MARGIN = 12;

    private int selected = -1;

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
                })
                .dimensions(this.width - 246, this.height - 28, 114, 20)
                .build());
    }

    private int gridW() { return (int) ((this.width - MARGIN * 2) * 0.55); }
    private int cellW() { return gridW() / COLS; }
    private int cellH() { return (this.height - TOP - BOTTOM) / ROWS; }

    private int cellAt(double mx, double my) {
        if (mx < MARGIN || my < TOP) return -1;
        int cx = (int) ((mx - MARGIN) / cellW());
        int cy = (int) ((my - TOP) / cellH());
        if (cx >= COLS || cy >= ROWS) return -1;
        return cy * COLS + cx;
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

    private String res(World.ResourceType r) {
        return r.label + " " + GameState.stock.get(r) + " (+" + GameState.income(r) + ")";
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        String line1 = "Хід " + GameState.turn + "   " + res(World.ResourceType.GOLD)
                + "   " + res(World.ResourceType.FOOD) + "   " + res(World.ResourceType.WOOD);
        String line2 = res(World.ResourceType.IRON) + "   " + res(World.ResourceType.POWDER)
                + "   " + res(World.ResourceType.HORSES);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line1), this.width / 2, 6, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line2), this.width / 2, 20, 0xFFD700);

        int w = cellW();
        int h = cellH();
        int hovered = cellAt(mouseX, mouseY);
        World.Province[] provinces = GameState.world.provinces;

        for (int i = 0; i < provinces.length; i++) {
            World.Province pr = provinces[i];
            int x = MARGIN + (i % COLS) * w;
            int y = TOP + (i / COLS) * h;

            ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, World.FACTIONS[pr.owner].color());
            if (i == hovered) {
                ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x40FFFFFF);
            }
            if (i == selected) {
                ctx.drawBorder(x + 2, y + 2, w - 4, h - 4, 0xFFFFFFFF);
            }
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(pr.name, w - 4)),
                    x + w / 2, y + h / 2 - 4, 0xFFFFFF);
        }

        int px = MARGIN + w * COLS + 12;
        int pw = this.width - px - MARGIN;
        if (selected >= 0) {
            World.Province pr = provinces[selected];
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(pr.name + " — " + World.FACTIONS[pr.owner].name(), pw)),
                    px, TOP, 0xFFD700);
            int y = TOP + 16;
            for (World.Point p : pr.points) {
                String s = p.type.label + " " + p.name + "  +" + p.output + " "
                        + p.type.resource.label + (p.unrest >= 20 ? " !" : "");
                ctx.drawTextWithShadow(textRenderer,
                        Text.literal(textRenderer.trimToWidth(s, pw)),
                        px, y, p.unrest >= 20 ? 0xFF5555 : 0xFFFFFF);
                y += 12;
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Натисни на провінцію"), px, TOP, 0xFFFFFF);
        }

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Зерно: " + GameState.world.seed), MARGIN, this.height - 22, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
