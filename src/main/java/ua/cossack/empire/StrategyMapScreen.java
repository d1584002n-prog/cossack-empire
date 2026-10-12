package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Заготовка стратегічного екрана. Далі: карта провінцій, ресурси, туман війни. */
public class StrategyMapScreen extends Screen {

    public StrategyMapScreen() {
        super(Text.literal("Козацька Держава"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 20, 0xFFD700);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Тут буде карта провінцій"), this.width / 2, this.height / 2, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
