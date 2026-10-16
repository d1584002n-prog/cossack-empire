package ua.cossack.empire;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Міста: вибір своєї провінції та будівництво будівель. */
public class CitiesScreen extends Screen {
    private static int index = 0;

    private final Screen parent;
    private final Building[] buildings = Building.values();
    private final ButtonWidget[] btn = new ButtonWidget[buildings.length];
    private ButtonWidget prev;
    private ButtonWidget next;

    public CitiesScreen(Screen parent) {
        super(Text.literal("Міста"));
        this.parent = parent;
    }

    private List<World.Province> owned() {
        List<World.Province> list = new ArrayList<>();
        for (World.Province pr : GameState.world.provinces) {
            if (Realm.isMine(pr)) list.add(pr);
        }
        list.sort((a, b) -> Integer.compare(Realm.tier(b), Realm.tier(a)));
        return list;
    }

    private World.Province current() {
        List<World.Province> list = owned();
        if (list.isEmpty()) return null;
        index = Math.max(0, Math.min(index, list.size() - 1));
        return list.get(index);
    }

    @Override
    protected void init() {
        int bw = 170;
        int x1 = this.width / 2 - bw - 4;
        int x2 = this.width / 2 + 4;
        for (int i = 0; i < buildings.length; i++) {
            final Building b = buildings[i];
            int x = i % 2 == 0 ? x1 : x2;
            int y = 66 + (i / 2) * 24;
            btn[i] = ButtonWidget.builder(Text.literal(b.label), bt -> {
                World.Province pr = current();
                if (pr != null) Realm.build(pr, b);
            }).dimensions(x, y, bw, 20).build();
            addDrawableChild(btn[i]);
        }
        prev = ButtonWidget.builder(Text.literal("<"), b -> index--)
                .dimensions(this.width / 2 - 150, 24, 30, 20).build();
        next = ButtonWidget.builder(Text.literal(">"), b -> index++)
                .dimensions(this.width / 2 + 120, 24, 30, 20).build();
        addDrawableChild(prev);
        addDrawableChild(next);
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        List<World.Province> list = owned();
        World.Province pr = current();
        prev.active = index > 0;
        next.active = index < list.size() - 1;

        String hint = "";
        for (int i = 0; i < buildings.length; i++) {
            Building b = buildings[i];
            if (pr == null) {
                btn[i].active = false;
                continue;
            }
            String label;
            if (pr.buildings.contains(b)) {
                label = "✔ " + b.label;
            } else if (b.epoch > Realm.epoch) {
                label = b.label + " (епоха " + b.epoch + ")";
            } else if (b == Building.PORT && !Realm.coastal(pr)) {
                label = b.label + " (немає моря)";
            } else {
                label = b.label + " " + Realm.costGold(b) + "з " + Realm.costWood(b) + "д"
                        + (Realm.costIron(b) > 0 ? " " + Realm.costIron(b) + "з." : "");
            }
            btn[i].setMessage(Text.literal(label));
            btn[i].active = Realm.canBuild(pr, b);
            if (btn[i].isMouseOver(mouseX, mouseY)) hint = b.label + ": " + b.effect;
        }

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Міста"), this.width / 2, 8, 0xFFD700);
        if (pr == null) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("У тебе немає провінцій"),
                    this.width / 2, 30, 0xFF5555);
            return;
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(pr.name + " — " + Realm.tierName(pr) + " (" + (index + 1) + "/" + list.size() + ")"),
                this.width / 2, 26, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Будівель: " + pr.buildings.size() + "/" + buildings.length
                        + "   Населення: " + Realm.population + "/" + Realm.popCap()
                        + "   Епоха: " + Realm.EPOCH_NAMES[Realm.epoch]),
                this.width / 2, 44, 0xAAAAAA);
        if (!hint.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(hint), this.width / 2, 66 + 4 * 24 + 6, 0xFFD700);
        }
        String log = GameState.logLine(0);
        if (!log.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(log), this.width / 2, this.height - 44, 0xDDDDDD);
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
