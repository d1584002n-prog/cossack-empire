package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TechScreen extends Screen {
    private final Screen parent;
    private final ButtonWidget[] btn = new ButtonWidget[Tech.values().length];

    public TechScreen(Screen parent) {
        super(Text.literal("Технології"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int bw = 340;
        int x = this.width / 2 - bw / 2;
        for (Tech t : Tech.values()) {
            btn[t.ordinal()] = ButtonWidget.builder(Text.literal(""), b -> GameState.startResearch(t))
                    .dimensions(x, 30 + t.ordinal() * 22, bw, 20).build();
            addDrawableChild(btn[t.ordinal()]);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        for (Tech t : Tech.values()) {
            ButtonWidget b = btn[t.ordinal()];
            String s;
            if (GameState.hasTech(t)) {
                s = "✔ " + t.title + " — " + t.effect;
            } else if (GameState.researching == t) {
                s = "… " + t.title + " (ходів: " + GameState.researchLeft + ")";
            } else if (t.requires != null && !GameState.hasTech(t.requires)) {
                s = t.title + " (потрібно: " + t.requires.title + ")";
            } else {
                s = t.title + " — " + t.effect + " [" + t.gold + "з" + (t.food > 0 ? ", " + t.food + " їжі" : "") + ", " + t.turns + " х.]";
            }
            b.setMessage(Text.literal(s));
            b.active = GameState.canResearch(t);
        }
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Технології"), this.width / 2, 8, 0xFFD700);
        String cur = GameState.researching == null ? "Дослідження: немає"
                : "Дослідження: " + GameState.researching.title + ", ходів: " + GameState.researchLeft;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(cur), this.width / 2, 19, 0xFFFFFF);
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
