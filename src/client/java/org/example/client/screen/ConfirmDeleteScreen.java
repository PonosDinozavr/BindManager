package org.example.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteScreen extends Screen {
    private final Screen parent;
    private final String profileName;
    private final Runnable onConfirm;

    public ConfirmDeleteScreen(Screen parent, String profileName, Runnable onConfirm) {
        super(Component.translatable("screen.bindmanager.delete.title"));
        this.parent = parent;
        this.profileName = profileName;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(
                Component.translatable("screen.bindmanager.delete.confirm"),
                btn -> {
                    onConfirm.run();
                    onClose();
                }
        ).bounds(width / 2 - 100, height / 2 + 10, 95, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> onClose()
        ).bounds(width / 2 + 5, height / 2 + 10, 95, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, height / 2 - 40, 0xFFFFFF);
        graphics.centeredText(
                font,
                Component.translatable("screen.bindmanager.delete.warning", profileName),
                width / 2,
                height / 2 - 20,
                0xFF5555
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
