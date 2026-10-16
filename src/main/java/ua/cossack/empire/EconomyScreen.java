package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Економіка: населення, податки, наука й торгівля ресурсами. */
public class EconomyScreen extends Screen {
    private static final World.ResourceType[] TRADE = {
        World.ResourceType.FOOD, World.ResourceType.WOOD, World.ResourceType.IRON,
        World.ResourceType.POWDER, World.ResourceType.HORSES
    };

    private final Screen parent;
    private final ButtonWidget[] taxBtn = new ButtonWidget[Realm.TAX_NAMES.length];
    private final ButtonWidget[] sellBtn = new ButtonWidget[TRADE.length];
    private final ButtonWidget[] buyBtn = new ButtonWidget[TRADE.length];

    public EconomyScreen(Screen parent) {
        super(Text.literal("Економіка"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int tw = 84;
        int tx = this.width / 2 - (tw * 4 + 12) / 2;
        for (int i = 0; i < taxBtn.length; i++) {
            final int lvl = i;
            taxBtn[i] = ButtonWidget.builder(Text.literal(Realm.TAX_NAMES[i]), b -> Realm.taxLevel = lvl)
                    .dimensions(tx + i * (tw + 4), 66, tw, 20).build();
            addDrawableChild(taxBtn[i]);
        }
        for (int i = 0; i < TRADE.length; i++) {
            final World.ResourceType r = TRADE[i];
            int y = 108 + i * 24;
            sellBtn[i] = ButtonWidget.builder(Text.literal("Продати"), b -> Realm.sell(r))
                    .dimensions(this.width / 2 - 20, y, 100, 20).build();
            buyBtn[i] = ButtonWidget.builder(Text.literal("Купити"), b -> Realm.buy(r))
                    .dimensions(this.width / 2 + 84, y, 100, 20).build();
            addDrawableChild(sellBtn[i]);
            addDrawableChild(buyBtn[i]);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        for (int i = 0; i < taxBtn.length; i++) {
            taxBtn[i].active = Realm.taxLevel != i;
        }
        for (int i = 0; i < TRADE.length; i++) {
            World.ResourceType r = TRADE[i];
            sellBtn[i].setMessage(Text.literal("Продати " + Realm.TRADE_LOT + " (+" + Realm.sellPrice(r) + "з)"));
            buyBtn[i].setMessage(Text.literal("Купити " + Realm.TRADE_LOT + " (-" + Realm.buyPrice(r) + "з)"));
            sellBtn[i].active = Realm.canSell(r);
            buyBtn[i].active = Realm.canBuy(r);
        }

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Економіка"), this.width / 2, 8, 0xFFD700);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Населення: " + Realm.population + "/" + Realm.popCap()
                        + "   Їсть: " + Realm.popFood() + " їжі/хід"),
                this.width / 2, 22, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Податки: +" + Realm.taxIncome() + " золота/хід   Наука: " + Realm.science
                        + " (+" + Realm.scienceIncome() + ")"),
                this.width / 2, 34, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Податки: " + Realm.TAX_NAMES[Realm.taxLevel]
                        + (Realm.taxLevel >= 2 ? " (зростає невдоволення)" : Realm.taxLevel == 0 ? " (спокій)" : "")),
                this.width / 2, 52, Realm.taxLevel >= 2 ? 0xFF8888 : 0xAAFFAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Торгівля"), this.width / 2, 94, 0xFFD700);
        for (int i = 0; i < TRADE.length; i++) {
            World.ResourceType r = TRADE[i];
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(r.label + ": " + GameState.stock.get(r)),
                    this.width / 2 - 190, 114 + i * 24, 0xFFFFFF);
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Золото: " + GameState.stock.get(World.ResourceType.GOLD)
                        + "   Ринки (" + Realm.count(Building.MARKET) + ") покращують ціни"),
                this.width / 2, this.height - 44, 0xFFD700);
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
