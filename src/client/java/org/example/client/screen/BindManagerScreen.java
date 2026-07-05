package org.example.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.example.client.BindManagerClient;
import org.example.client.config.BindConfig;

import java.util.List;

public class BindManagerScreen extends Screen {
    private final Screen parent;
    private List<BindConfig> profiles;
    private int scrollOffset;
    private static final int ENTRY_HEIGHT = 30;

    public BindManagerScreen(Screen parent) {
        super(Text.translatable("screen.bindmanager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        profiles = BindManagerClient.getConfigStore().getProfiles();

        int bottomY = height - 28;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.bindmanager.create"),
                btn -> client.setScreen(new NameInputScreen(
                        this,
                        Text.translatable("screen.bindmanager.create.title"),
                        Text.translatable("screen.bindmanager.create.field"),
                        name -> {
                            if (!name.isEmpty()) {
                                BindManagerClient.getConfigStore().saveProfile(name);
                            }
                            return null;
                        }
                ))
        ).dimensions(width / 2 - 100, bottomY - 30, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                btn -> close()
        ).dimensions(width / 2 - 100, bottomY, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);

        int startY = 35;
        int visibleEntries = (height - 130) / ENTRY_HEIGHT;

        for (int i = 0; i < visibleEntries && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            BindConfig config = profiles.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            int listLeft = 10;
            int listRight = width - 10;

            context.fill(listLeft, y, listRight, y + ENTRY_HEIGHT - 2, 0x33FFFFFF);
            context.drawText(textRenderer, Text.literal(config.getName()), listLeft + 5, y + 6, 0xFFFFFF, false);

            int buttonY = y + 5;
            int deleteX = listRight - 65;
            int renameX = deleteX - 70;
            int loadX = renameX - 70;

            context.drawText(textRenderer, Text.translatable("screen.bindmanager.load"), loadX + 5, buttonY + 2, 0x00FF00, false);
            context.drawText(textRenderer, Text.translatable("screen.bindmanager.rename"), renameX + 5, buttonY + 2, 0xFFFF55, false);
            context.drawText(textRenderer, Text.translatable("screen.bindmanager.delete"), deleteX + 5, buttonY + 2, 0xFF5555, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int startY = 35;
        int visibleEntries = (height - 130) / ENTRY_HEIGHT;
        int listLeft = 10;
        int listRight = width - 10;

        for (int i = 0; i < visibleEntries && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            BindConfig config = profiles.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            if (mouseY >= y && mouseY < y + ENTRY_HEIGHT - 2 && mouseX >= listLeft && mouseX <= listRight) {
                int buttonY = y + 5;
                int deleteX = listRight - 65;
                int renameX = deleteX - 70;
                int loadX = renameX - 70;

                if (mouseX >= loadX && mouseX < loadX + 65) {
                    BindManagerClient.getConfigStore().loadProfile(config.getName());
                    client.setScreen(null);
                    return true;
                } else if (mouseX >= renameX && mouseX < renameX + 70) {
                    String currentName = config.getName();
                    client.setScreen(new NameInputScreen(
                            this,
                            Text.translatable("screen.bindmanager.rename.title"),
                            Text.translatable("screen.bindmanager.rename.field"),
                            newName -> {
                                if (!newName.isEmpty() && !newName.equals(currentName)) {
                                    BindManagerClient.getConfigStore().renameProfile(currentName, newName);
                                }
                                return null;
                            }
                    ));
                    return true;
                } else if (mouseX >= deleteX && mouseX < deleteX + 65) {
                    String name = config.getName();
                    client.setScreen(new ConfirmDeleteScreen(
                            this, name,
                            () -> BindManagerClient.getConfigStore().deleteProfile(name)
                    ));
                    return true;
                } else {
                    BindManagerClient.getConfigStore().loadProfile(config.getName());
                    client.setScreen(null);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
