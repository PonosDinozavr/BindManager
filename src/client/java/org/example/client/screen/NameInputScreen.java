package org.example.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class NameInputScreen extends Screen {
    private final Screen parent;
    private final Component fieldLabel;
    private final Function<String, Void> callback;
    private EditBox textField;

    public NameInputScreen(Screen parent, Component title, Component fieldLabel, Function<String, Void> callback) {
        super(title);
        this.parent = parent;
        this.fieldLabel = fieldLabel;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();

        textField = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, fieldLabel);
        textField.setMaxLength(32);
        addRenderableWidget(textField);
        setInitialFocus(textField);

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> confirm()
        ).bounds(width / 2 - 100, height / 2 + 10, 95, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> onClose()
        ).bounds(width / 2 + 5, height / 2 + 10, 95, 20).build());
    }

    private void confirm() {
        String name = textField.getValue().trim();
        if (!name.isEmpty()) {
            callback.apply(name);
            onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, height / 2 - 50, 0xFFFFFFFF);
        graphics.text(font, fieldLabel, width / 2 - 100, height / 2 - 40, 0xFFA0A0A0);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            confirm();
            return true;
        }
        if (textField != null && textField.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (textField != null && textField.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
