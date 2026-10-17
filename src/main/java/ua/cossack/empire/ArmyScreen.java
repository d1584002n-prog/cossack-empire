package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ArmyScreen extends Screen {
    private final Screen parent;
    private final ButtonWidget[] recruitBtn = new ButtonWidget[3];
    private ButtonWidget shipBtn;
    private ButtonWidget ransomBtn;
    private ButtonWidget enlistBtn;
    private ButtonWidget releaseBtn;

    public ArmyScreen(Screen parent) {
        super(Text.literal("Армія"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int bw = 320;
        int x = this.width / 2 - bw / 2;
        for (int t = 0; t < 3; t++) {
            final int type = t;
            recruitBtn[t] = ButtonWidget.builder(Text.literal(""), b -> GameState.recruit(type))
                    .dimensions(x, 44 + t * 24, bw, 20)
                    .build();
            addDrawableChild(recruitBtn[t]);
        }

        shipBtn = ButtonWidget.builder(Text.literal(""), b -> GameState.buildShip())
                .dimensions(x, 44 + 3 * 24, bw, 20)
                .build();
        addDrawableChild(shipBtn);

        int pw = 100;
        int px = this.width / 2 - (pw * 3 + 12) / 2;
        int y = 156;
        ransomBtn = ButtonWidget.builder(Text.literal("Викуп"), b -> GameState.ransomPrisoners())
                .dimensions(px, y, pw, 20).build();
        enlistBtn = ButtonWidget.builder(Text.literal("До війська"), b -> GameState.enlistPrisoners())
                .dimensions(px + pw + 6, y, pw, 20).build();
        releaseBtn = ButtonWidget.builder(Text.literal("Відпустити"), b -> GameState.releasePrisoners())
                .dimensions(px + (pw + 6) * 2, y, pw, 20).build();
        addDrawableChild(ransomBtn);
        addDrawableChild(enlistBtn);
        addDrawableChild(releaseBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20)
                .build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        for (int t = 0; t < 3; t++) {
            recruitBtn[t].setMessage(Text.literal(
                    GameState.UNIT_NAMES[t] + " (" + GameState.army[t] + ")  +1: " + GameState.costText(t)));
            recruitBtn[t].active = GameState.canRecruit(t);
        }
        if (!GameState.hasTech(Tech.SHIPS)) {
            shipBtn.setMessage(Text.literal("Флот: потрібна технологія «Кораблебудування»"));
        } else if (GameState.maxShips() == 0) {
            shipBtn.setMessage(Text.literal("Флот: збудуй порт у прибережній провінції"));
        } else {
            shipBtn.setMessage(Text.literal("Чайка (" + GameState.ships + "/" + GameState.maxShips() + ")  +1: Золото "
                    + GameState.SHIP_GOLD + ", Дерево " + GameState.SHIP_WOOD + ", Залізо " + GameState.SHIP_IRON));
        }
        shipBtn.active = GameState.canBuildShip();
        boolean has = GameState.prisoners > 0;
        ransomBtn.active = has;
        enlistBtn.active = has;
        releaseBtn.active = has;

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Армія"), this.width / 2, 10, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Сила: " + GameState.armyPower() + "   Утримання за хід: Їжа "
                        + GameState.upkeepFood() + ", Золото " + GameState.upkeepGold()),
                this.width / 2, 26, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Флот: " + GameState.ships + " (місткість " + GameState.shipCap() + " на корабель, дальність "
                        + GameState.seaRange() + ")"),
                this.width / 2, 120 + 2, 0xAAD4FF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Полонені: " + GameState.prisoners + "   (викуп: +" + GameState.prisoners * 10 + " золота)"),
                this.width / 2, 138, 0xFFD700);
        String last = GameState.logLine(0);
        if (!last.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(last),
                    this.width / 2, 186, 0xFFFFFF);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
