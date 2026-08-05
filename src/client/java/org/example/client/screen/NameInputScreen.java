package org.example.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Function;

public class NameInputScreen extends Screen {
    private final Screen parent;
    private final Text fieldLabel;
    private final Function<String, Void> callback;
    private TextFieldWidget textField;

    public NameInputScreen(Screen parent, Text title, Text fieldLabel, Function<String, Void> callback) {
        super(title);
        this.parent = parent;
        this.fieldLabel = fieldLabel;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();

        textField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 20, 200, 20, fieldLabel);
        textField.setMaxLength(32);
        addSelectableChild(textField);
        setInitialFocus(textField);

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                btn -> confirm()
        ).dimensions(width / 2 - 100, height / 2 + 10, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                btn -> close()
        ).dimensions(width / 2 + 5, height / 2 + 10, 95, 20).build());
    }

    private void confirm() {
        String name = textField.getText().trim();
        if (!name.isEmpty()) {
            callback.apply(name);
            close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 50, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, fieldLabel, width / 2 - 100, height / 2 - 40, 0xA0A0A0);
        textField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            confirm();
            return true;
        }
        if (textField != null && textField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (textField != null && textField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
