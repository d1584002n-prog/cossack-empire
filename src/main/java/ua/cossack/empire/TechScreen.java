package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Технології: 4 гілки по 3 рівні та епохи розвитку. */
public class TechScreen extends Screen {
    private final Screen parent;
    private final ButtonWidget[] btn = new ButtonWidget[Tech.values().length];
    private ButtonWidget epochBtn;

    public TechScreen(Screen parent) {
        super(Text.literal("Технології"));
        this.parent = parent;
    }

    private int bw() {
        return Math.min(118, (this.width - 48) / 4 - 4);
    }

    private int colX(int branch) {
        int total = bw() * 4 + 12;
        return this.width / 2 - total / 2 + branch * (bw() + 4);
    }

    @Override
    protected void init() {
        for (Tech t : Tech.values()) {
            btn[t.ordinal()] = ButtonWidget.builder(Text.literal(t.title), b -> GameState.startResearch(t))
                    .dimensions(colX(t.branch), 62 + (t.level - 1) * 24, bw(), 20).build();
            addDrawableChild(btn[t.ordinal()]);
        }
        epochBtn = ButtonWidget.builder(Text.literal("Нова епоха"), b -> Realm.advanceEpoch())
                .dimensions(this.width / 2 - 130, 62 + 3 * 24 + 6, 260, 20).build();
        addDrawableChild(epochBtn);
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        String hint = "";
        for (Tech t : Tech.values()) {
            ButtonWidget b = btn[t.ordinal()];
            String s;
            if (GameState.hasTech(t)) {
                s = "✔ " + t.title;
            } else if (GameState.researching == t) {
                s = "… " + t.title + " (" + GameState.researchLeft + ")";
            } else {
                s = t.title;
            }
            b.setMessage(Text.literal(s));
            b.active = GameState.canResearch(t);
            if (b.isMouseOver(mouseX, mouseY)) {
                String status;
                if (GameState.hasTech(t)) status = "вивчено";
                else if (t.requires != null && !GameState.hasTech(t.requires)) status = "потрібно: " + t.requires.title;
                else if (Realm.epoch < t.level) status = "потрібна епоха " + t.level;
                else status = t.gold + " золота, " + t.science + " науки, " + t.turns + " ходи";
                hint = t.title + ": " + t.effect + " [" + status + "]";
            }
        }
        if (Realm.maxEpoch()) {
            epochBtn.setMessage(Text.literal("Остання епоха: " + Realm.EPOCH_NAMES[Realm.epoch]));
            epochBtn.active = false;
        } else {
            epochBtn.setMessage(Text.literal("Епоха " + (Realm.epoch + 1) + ": " + Realm.EPOCH_NAMES[Realm.epoch + 1]
                    + " (" + Realm.nextEpochGold() + "з, " + Realm.nextEpochScience() + "н, "
                    + Realm.nextEpochTechs() + " техн.)"));
            epochBtn.active = Realm.canAdvanceEpoch();
        }

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Технології"), this.width / 2, 8, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Епоха: " + Realm.EPOCH_NAMES[Realm.epoch] + "   Золото: "
                        + GameState.stock.get(World.ResourceType.GOLD) + "   Наука: " + Realm.science
                        + " (+" + Realm.scienceIncome() + ")"),
                this.width / 2, 22, 0xFFFFFF);
        String cur = GameState.researching == null ? "Дослідження: немає"
                : "Дослідження: " + GameState.researching.title + ", ходів: " + GameState.researchLeft;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(cur), this.width / 2, 34, 0xAAAAAA);
        for (int c = 0; c < Tech.BRANCHES.length; c++) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(Tech.BRANCHES[c]), colX(c), 50, 0xFFD700);
        }
        if (!hint.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(hint), this.width / 2, this.height - 44, 0xFFD700);
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
