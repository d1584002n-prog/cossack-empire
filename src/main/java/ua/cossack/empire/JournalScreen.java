package ua.cossack.empire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Журнал подій: що відбулося (найновіше зверху). */
public class JournalScreen extends Screen {
    private static int page = 0;

    private final Screen parent;
    private ButtonWidget prev;
    private ButtonWidget next;

    public JournalScreen(Screen parent) {
        super(Text.literal("Журнал"));
        this.parent = parent;
    }

    private int perPage() {
        return Math.max(4, (this.height - 76) / 11);
    }

    private int pages() {
        return Math.max(1, (GameState.journal.size() + perPage() - 1) / perPage());
    }

    @Override
    protected void init() {
        prev = ButtonWidget.builder(Text.literal("<"), b -> page = Math.max(0, page - 1))
                .dimensions(this.width / 2 - 130, this.height - 28, 60, 20).build();
        next = ButtonWidget.builder(Text.literal(">"), b -> page = Math.min(pages() - 1, page + 1))
                .dimensions(this.width / 2 + 70, this.height - 28, 60, 20).build();
        addDrawableChild(prev);
        addDrawableChild(next);
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        page = Math.min(page, pages() - 1);
        prev.active = page > 0;
        next.active = page < pages() - 1;
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Журнал (" + (page + 1) + "/" + pages() + ")"), this.width / 2, 8, 0xFFD700);
        int n = GameState.journal.size();
        int per = perPage();
        int maxW = this.width - 24;
        for (int i = 0; i < per; i++) {
            int idx = n - 1 - (page * per + i);
            if (idx < 0) break;
            String line = textRenderer.trimToWidth(GameState.journal.get(idx), maxW);
            ctx.drawTextWithShadow(textRenderer, Text.literal(line), 12, 26 + i * 11, i == 0 && page == 0 ? 0xFFD700 : 0xDDDDDD);
        }
        if (n == 0) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Подій поки немає"), this.width / 2, 40, 0xAAAAAA);
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
