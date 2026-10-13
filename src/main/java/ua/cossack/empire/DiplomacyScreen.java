package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DiplomacyScreen extends Screen {
    private final Screen parent;
    private final ButtonWidget[] giftBtn = new ButtonWidget[6];
    private final ButtonWidget[] allyBtn = new ButtonWidget[6];
    private final ButtonWidget[] warBtn = new ButtonWidget[6];

    public DiplomacyScreen(Screen parent) {
        super(Text.literal("Дипломатія"));
        this.parent = parent;
    }

    private int rowY(int f) {
        return 22 + (f - 1) * 34;
    }

    @Override
    protected void init() {
        int bw = 100;
        int x0 = this.width / 2 - (bw * 3 + 12) / 2;
        for (int f = 1; f <= 5; f++) {
            final int fac = f;
            int y = rowY(f) + 11;
            giftBtn[f] = ButtonWidget.builder(Text.literal("Дар (50з)"), b -> GameState.gift(fac))
                    .dimensions(x0, y, bw, 18).build();
            allyBtn[f] = ButtonWidget.builder(Text.literal("Союз"), b -> GameState.toggleAlliance(fac))
                    .dimensions(x0 + bw + 6, y, bw, 18).build();
            warBtn[f] = ButtonWidget.builder(Text.literal("Війна"), b -> GameState.toggleWar(fac))
                    .dimensions(x0 + (bw + 6) * 2, y, bw, 18).build();
            addDrawableChild(giftBtn[f]);
            addDrawableChild(allyBtn[f]);
            addDrawableChild(warBtn[f]);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20)
                .build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        for (int f = 1; f <= 5; f++) {
            giftBtn[f].active = GameState.canGift(f);
            allyBtn[f].active = GameState.canAlly(f);
            allyBtn[f].setMessage(Text.literal(GameState.allied[f] ? "Розірвати союз" : "Союз (від +50)"));
            warBtn[f].active = GameState.canWarToggle(f);
            warBtn[f].setMessage(Text.literal(GameState.atWar[f] ? "Мир (50з)" : "Війна"));
        }

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Дипломатія"), this.width / 2, 6, 0xFFD700);
        for (int f = 1; f <= 5; f++) {
            String status = GameState.atWar[f] ? "ВІЙНА" : GameState.allied[f] ? "СОЮЗ" : "мир";
            int color = GameState.atWar[f] ? 0xFF5555 : GameState.allied[f] ? 0x55FF55 : 0xFFFFFF;
            String s = World.FACTIONS[f].name() + "   відносини: " + GameState.relation[f] + "   " + status;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(s), this.width / 2, rowY(f), color);
        }
        String last = GameState.logLine(0);
        if (!last.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(last), this.width / 2, this.height - 44, 0xFFD700);
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
