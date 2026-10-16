package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DiplomacyScreen extends Screen {
    private static final int ROW_H = 34;
    private static int page = 0;

    private final Screen parent;
    private int perPage;
    private ButtonWidget[] giftBtn;
    private ButtonWidget[] allyBtn;
    private ButtonWidget[] warBtn;
    private ButtonWidget prevBtn;
    private ButtonWidget nextBtn;

    public DiplomacyScreen(Screen parent) {
        super(Text.literal("Дипломатія"));
        this.parent = parent;
    }

    private int states() {
        return Countries.ALL.length - 1; // без гравця
    }

    private int pages() {
        return (states() + perPage - 1) / perPage;
    }

    private int factionAt(int slot) {
        int f = page * perPage + slot + 1;
        return f <= states() ? f : -1;
    }

    private int rowY(int slot) {
        return 22 + slot * ROW_H;
    }

    @Override
    protected void init() {
        perPage = Math.max(1, (this.height - 22 - 34) / ROW_H);
        page = Math.min(page, pages() - 1);
        giftBtn = new ButtonWidget[perPage];
        allyBtn = new ButtonWidget[perPage];
        warBtn = new ButtonWidget[perPage];
        int bw = 100;
        int x0 = this.width / 2 - (bw * 3 + 12) / 2;
        for (int s = 0; s < perPage; s++) {
            final int slot = s;
            int y = rowY(s) + 11;
            giftBtn[s] = ButtonWidget.builder(Text.literal("Дар (50з)"), b -> {
                int f = factionAt(slot);
                if (f > 0) GameState.gift(f);
            }).dimensions(x0, y, bw, 18).build();
            allyBtn[s] = ButtonWidget.builder(Text.literal("Союз"), b -> {
                int f = factionAt(slot);
                if (f > 0) GameState.toggleAlliance(f);
            }).dimensions(x0 + bw + 6, y, bw, 18).build();
            warBtn[s] = ButtonWidget.builder(Text.literal("Війна"), b -> {
                int f = factionAt(slot);
                if (f > 0) GameState.toggleWar(f);
            }).dimensions(x0 + (bw + 6) * 2, y, bw, 18).build();
            addDrawableChild(giftBtn[s]);
            addDrawableChild(allyBtn[s]);
            addDrawableChild(warBtn[s]);
        }
        prevBtn = ButtonWidget.builder(Text.literal("<"), b -> page = Math.max(0, page - 1))
                .dimensions(this.width / 2 - 130, this.height - 28, 60, 20).build();
        nextBtn = ButtonWidget.builder(Text.literal(">"), b -> page = Math.min(pages() - 1, page + 1))
                .dimensions(this.width / 2 + 70, this.height - 28, 60, 20).build();
        addDrawableChild(prevBtn);
        addDrawableChild(nextBtn);
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        prevBtn.active = page > 0;
        nextBtn.active = page < pages() - 1;
        for (int s = 0; s < perPage; s++) {
            int f = factionAt(s);
            boolean on = f > 0;
            giftBtn[s].visible = on;
            allyBtn[s].visible = on;
            warBtn[s].visible = on;
            if (!on) continue;
            giftBtn[s].active = GameState.canGift(f);
            allyBtn[s].active = GameState.canAlly(f);
            allyBtn[s].setMessage(Text.literal(GameState.allied[f] ? "Розірвати союз" : "Союз (від +50)"));
            warBtn[s].active = GameState.canWarToggle(f);
            warBtn[s].setMessage(Text.literal(GameState.atWar[f] ? "Мир (50з)" : "Війна"));
        }

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Дипломатія (" + (page + 1) + "/" + pages() + ")"), this.width / 2, 6, 0xFFD700);
        for (int s = 0; s < perPage; s++) {
            int f = factionAt(s);
            if (f < 0) continue;
            String status = GameState.atWar[f] ? "ВІЙНА" : GameState.allied[f] ? "СОЮЗ" : "мир";
            int color = GameState.atWar[f] ? 0xFF5555 : GameState.allied[f] ? 0x55FF55 : 0xFFFFFF;
            String line = World.FACTIONS[f].name() + "   відносини: " + GameState.relation[f] + "   " + status;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line), this.width / 2, rowY(s), color);
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
