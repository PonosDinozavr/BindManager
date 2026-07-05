package org.example.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.example.client.BindManagerClient;
import org.example.client.BindConfigStore;
import org.example.client.config.BindConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BindManagerScreen extends Screen {
    private static final int[] PALETTE = {
            0x555555, 0xFF5555, 0xFF9900, 0xFFFF55, 0x55FF55,
            0x55FFFF, 0x5555FF, 0xFF55FF, 0xFFFFFF, 0xAAAAAA,
            0xCC3333, 0xCC7700, 0xCCCC00, 0x33CC33, 0x33CCCC,
            0x3333CC, 0xCC33CC, 0xDDDDDD, 0x774400, 0xFF77FF
    };

    private final Screen parent;
    private List<BindConfig> profiles;
    private int scrollOffset;
    private int hoveredIndex = -1;
    private int sortMode = 0;
    private boolean dragging = false;
    private int dragIndex = -1;
    private int dragStartY;
    private int dragCurrentY;

    private static final int ENTRY_HEIGHT = 26;
    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 60;

    public BindManagerScreen(Screen parent) {
        super(Text.translatable("screen.bindmanager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        profiles = getSortedProfiles();

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
                                refreshProfiles();
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

    private List<BindConfig> getSortedProfiles() {
        List<BindConfig> list = new ArrayList<>(BindManagerClient.getConfigStore().getProfiles());
        switch (sortMode) {
            case 1:
                list.sort(Comparator.comparing(c -> c.getName().toLowerCase()));
                break;
            case 2:
                list.sort(Comparator.comparing((BindConfig c) -> !c.isFavorite()).thenComparing(c -> c.getName().toLowerCase()));
                break;
            case 3:
                list.sort(Comparator.comparingInt(BindConfig::getColor).thenComparing(c -> c.getName().toLowerCase()));
                break;
            default:
                break;
        }
        return list;
    }

    private void refreshProfiles() {
        scrollOffset = 0;
        profiles = getSortedProfiles();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        String sortLabel = Text.translatable("screen.bindmanager.sort." + sortMode).getString();
        context.drawText(textRenderer, Text.translatable("screen.bindmanager.sort", sortLabel), 10, 22, 0xAAAAAA, false);

        int startY = HEADER_HEIGHT + 4;
        int maxVisible = (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ENTRY_HEIGHT;
        hoveredIndex = -1;

        int listLeft = 8;
        int listRight = width - 8;

        for (int i = 0; i < maxVisible && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            BindConfig config = profiles.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            if (dragging && index == dragIndex) {
                y = dragCurrentY - ENTRY_HEIGHT / 2;
            }

            boolean hovered = mouseX >= listLeft && mouseX <= listRight && mouseY >= y && mouseY < y + ENTRY_HEIGHT - 1;
            if (hovered) hoveredIndex = index;

            int bgColor = hovered ? 0x44FFFFFF : 0x22FFFFFF;
            context.fill(listLeft, y, listRight, y + ENTRY_HEIGHT - 1, bgColor);

            int colorStrip = config.getColor();
            context.fill(listLeft, y, listLeft + 3, y + ENTRY_HEIGHT - 1, 0xFF000000 | colorStrip);

            int nameX = listLeft + 8;
            int nameColor = config.isFavorite() ? 0xFFFF55 : 0xFFFFFF;
            String displayName = config.getName();
            if (config.isFavorite()) {
                displayName = Formatting.YELLOW + "\u2605" + Formatting.RESET + " " + displayName;
            }
            context.drawText(textRenderer, Text.literal(displayName), nameX, y + 5, nameColor, false);

            int buttonY = y + 5;
            int btnW = 40;
            int gap = 4;
            int favX = listRight - btnW - gap;
            int colorX = favX - btnW - gap;
            int deleteX = colorX - btnW - gap;
            int renameX = deleteX - btnW - gap;
            int loadX = renameX - btnW - gap;

            drawBtn(context, loadX, buttonY, btnW, Text.translatable("screen.bindmanager.load"), 0x55FF55, hovered && mouseX >= loadX && mouseX < loadX + btnW);
            drawBtn(context, renameX, buttonY, btnW, Text.translatable("screen.bindmanager.rename"), 0xFFFF55, hovered && mouseX >= renameX && mouseX < renameX + btnW);
            drawBtn(context, deleteX, buttonY, btnW, Text.translatable("screen.bindmanager.delete"), 0xFF5555, hovered && mouseX >= deleteX && mouseX < deleteX + btnW);
            drawBtn(context, colorX, buttonY, btnW, Text.translatable("screen.bindmanager.color"), 0x55FFFF, hovered && mouseX >= colorX && mouseX < colorX + btnW);

            String favText = config.isFavorite() ? "\u2605" : "\u2606";
            int favColor = config.isFavorite() ? 0xFFFF55 : 0xAAAAAA;
            drawBtn(context, favX, buttonY, btnW, Text.literal(favText), favColor, hovered && mouseX >= favX && mouseX < favX + btnW);
        }

        if (profiles.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.bindmanager.empty"), width / 2, startY + 30, 0x888888);
        }
    }

    private void drawBtn(DrawContext ctx, int x, int y, int w, Text text, int color, boolean highlight) {
        int bg = highlight ? 0x44FFFFFF : 0x22FFFFFF;
        ctx.fill(x - 2, y - 1, x + w + 2, y + 15, bg);
        ctx.drawText(textRenderer, text, x, y + 2, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int startY = HEADER_HEIGHT + 4;
        int maxVisible = (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ENTRY_HEIGHT;
        int listLeft = 8;
        int listRight = width - 8;

        if (button == 0 && mouseX >= 10 && mouseX <= 100 && mouseY >= 18 && mouseY <= 28) {
            sortMode = (sortMode + 1) % 4;
            refreshProfiles();
            return true;
        }

        for (int i = 0; i < maxVisible && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            BindConfig config = profiles.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            if (mouseY >= y && mouseY < y + ENTRY_HEIGHT - 1 && mouseX >= listLeft && mouseX <= listRight) {
                int btnW = 40;
                int gap = 4;
                int favX = listRight - btnW - gap;
                int colorX = favX - btnW - gap;
                int deleteX = colorX - btnW - gap;
                int renameX = deleteX - btnW - gap;
                int loadX = renameX - btnW - gap;
                int buttonY = y + 5;

                if (button == 0) {
                    if (mouseX >= loadX && mouseX < loadX + btnW) {
                        BindManagerClient.getConfigStore().loadProfile(config.getName());
                        client.setScreen(null);
                        return true;
                    } else if (mouseX >= renameX && mouseX < renameX + btnW) {
                        String currentName = config.getName();
                        client.setScreen(new NameInputScreen(
                                this,
                                Text.translatable("screen.bindmanager.rename.title"),
                                Text.translatable("screen.bindmanager.rename.field"),
                                newName -> {
                                    if (!newName.isEmpty() && !newName.equals(currentName)) {
                                        BindManagerClient.getConfigStore().renameProfile(currentName, newName);
                                        refreshProfiles();
                                    }
                                    return null;
                                }
                        ));
                        return true;
                    } else if (mouseX >= deleteX && mouseX < deleteX + btnW) {
                        String name = config.getName();
                        client.setScreen(new ConfirmDeleteScreen(
                                this, name,
                                () -> {
                                    BindManagerClient.getConfigStore().deleteProfile(name);
                                    refreshProfiles();
                                }
                        ));
                        return true;
                    } else if (mouseX >= colorX && mouseX < colorX + btnW) {
                        openColorPicker(config);
                        return true;
                    } else if (mouseX >= favX && mouseX < favX + btnW) {
                        config.setFavorite(!config.isFavorite());
                        BindManagerClient.getConfigStore().saveExistingProfile(config);
                        refreshProfiles();
                        return true;
                    } else {
                        BindManagerClient.getConfigStore().loadProfile(config.getName());
                        client.setScreen(null);
                        return true;
                    }
                } else if (button == 1) {
                    dragging = true;
                    dragIndex = index;
                    dragStartY = (int) mouseY;
                    dragCurrentY = dragStartY;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 1) {
            dragging = false;
            int startY = HEADER_HEIGHT + 4;
            int dropIndex = Math.max(0, Math.min((int) ((mouseY - startY) / ENTRY_HEIGHT) + scrollOffset, profiles.size() - 1));
            if (dropIndex != dragIndex && dragIndex >= 0 && dragIndex < profiles.size()) {
                BindConfigStore store = BindManagerClient.getConfigStore();
                BindConfig dragged = profiles.get(dragIndex);
                BindConfig target = profiles.get(dropIndex);
                int storeFrom = store.getProfiles().indexOf(dragged);
                int storeTo = store.getProfiles().indexOf(target);
                if (storeFrom >= 0 && storeTo >= 0) {
                    store.moveProfile(storeFrom, storeTo);
                }
                refreshProfiles();
            }
            dragIndex = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 1) {
            dragCurrentY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxVisible = (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, profiles.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        return true;
    }

    private void openColorPicker(BindConfig config) {
        client.setScreen(new ColorPickerScreen(this, config));
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    public static class ColorPickerScreen extends Screen {
        private final Screen parent;
        private final BindConfig config;

        public ColorPickerScreen(Screen parent, BindConfig config) {
            super(Text.translatable("screen.bindmanager.color_picker"));
            this.parent = parent;
            this.config = config;
        }

        @Override
        protected void init() {
            super.init();
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.cancel"),
                    btn -> close()
            ).dimensions(width / 2 - 100, height / 2 + 50, 200, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 60, 0xFFFFFF);

            int cols = 5;
            int cellSize = 24;
            int gap = 4;
            int totalW = cols * cellSize + (cols - 1) * gap;
            int startX = width / 2 - totalW / 2;
            int startY = height / 2 - 35;

            for (int i = 0; i < PALETTE.length; i++) {
                int row = i / cols;
                int col = i % cols;
                int x = startX + col * (cellSize + gap);
                int y = startY + row * (cellSize + gap);

                boolean selected = config.getColor() == PALETTE[i];
                boolean hovered = mouseX >= x && mouseX < x + cellSize && mouseY >= y && mouseY < y + cellSize;

                int borderColor = selected ? 0xFFFFFF : (hovered ? 0xAAAAAA : 0x555555);
                context.fill(x - 1, y - 1, x + cellSize + 1, y + cellSize + 1, 0xFF000000 | borderColor);
                context.fill(x, y, x + cellSize, y + cellSize, 0xFF000000 | PALETTE[i]);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                int cols = 5;
                int cellSize = 24;
                int gap = 4;
                int totalW = cols * cellSize + (cols - 1) * gap;
                int startX = width / 2 - totalW / 2;
                int startY = height / 2 - 35;

                for (int i = 0; i < PALETTE.length; i++) {
                    int row = i / cols;
                    int col = i % cols;
                    int x = startX + col * (cellSize + gap);
                    int y = startY + row * (cellSize + gap);

                    if (mouseX >= x && mouseX < x + cellSize && mouseY >= y && mouseY < y + cellSize) {
                        config.setColor(PALETTE[i]);
                        BindManagerClient.getConfigStore().saveExistingProfile(config);
                        close();
                        return true;
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void close() {
            client.setScreen(parent);
        }
    }
}
