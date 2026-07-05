package org.example.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfirmDeleteScreen extends Screen {
    private final Screen parent;
    private final String profileName;
    private final Runnable onConfirm;

    public ConfirmDeleteScreen(Screen parent, String profileName, Runnable onConfirm) {
        super(Text.translatable("screen.bindmanager.delete.title"));
        this.parent = parent;
        this.profileName = profileName;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.bindmanager.delete.confirm"),
                btn -> {
                    onConfirm.run();
                    close();
                }
        ).dimensions(width / 2 - 100, height / 2 + 10, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                btn -> close()
        ).dimensions(width / 2 + 5, height / 2 + 10, 95, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 40, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.bindmanager.delete.warning", profileName),
                width / 2,
                height / 2 - 20,
                0xFF5555
        );
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
