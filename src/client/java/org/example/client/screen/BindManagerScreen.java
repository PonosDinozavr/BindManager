package org.example.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.example.client.BindManagerClient;
import org.example.client.BindConfigStore;
import org.example.client.config.BindConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BindManagerScreen extends Screen {
    private static final int[] PALETTE = {
            0x55FFFF, 0x5555FF, 0xFF55FF, 0xFFFFFF, 0xAAAAAA,
            0xCC3333, 0xCC7700, 0xCCCC00, 0x33CC33, 0x33CCCC,
            0x3333CC, 0xCC33CC, 0xDDDDDD, 0x774400, 0xFF77FF
    };
    private static final String[] SORT_KEYS = {
            "screen.changeofcontrol.sort.0", "screen.changeofcontrol.sort.1",
            "screen.changeofcontrol.sort.2", "screen.changeofcontrol.sort.3"
    };
    private static final int[] SORT_COLORS = {
            0xAAAAAA, 0x55FF55, 0xFFFF55, 0x55FFFF
    };

    private final Screen parent;
    private List<BindConfig> profiles;
    private int scrollOffset;
    private int hoveredIndex = -1;
    private boolean showFavoritesOnly;

    // Sort
    private int sortMode = 0;

    // Entry drag
    private boolean dragging = false;
    private int dragIndex = -1;
    private int dragMouseY;
    private int dragVisualY;

    private static final int ENTRY_HEIGHT = 28;
    private static final int HEADER_H = 22;
    private static final int SORT_PANEL_H = 24;
    private static final int FOOTER_HEIGHT = 60;

    private int getListTop() {
        return HEADER_H + SORT_PANEL_H + 4;
    }

    public BindManagerScreen(Screen parent) {
        super(Component.translatable("screen.changeofcontrol.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        refreshProfiles();

        int bottomY = height - 28;
        addRenderableWidget(Button.builder(
                Component.translatable("screen.changeofcontrol.controls"),
                btn -> minecraft.setScreen(new KeyBindsScreen(this, minecraft.options))
        ).bounds(width / 2 - 149, bottomY - 30, 70, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.changeofcontrol.create"),
                btn -> minecraft.setScreen(new NameInputScreen(
                        this,
                        Component.translatable("screen.changeofcontrol.create.title"),
                        Component.translatable("screen.changeofcontrol.create.field"),
                        name -> {
                            if (!name.isEmpty()) {
                                BindManagerClient.getConfigStore().saveProfile(name);
                                refreshProfiles();
                            }
                            return null;
                        }
                ))
        ).bounds(width / 2 - 73, bottomY - 30, 70, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.changeofcontrol.filter_fav"),
                btn -> {
                    showFavoritesOnly = !showFavoritesOnly;
                    refreshProfiles();
                }
        ).bounds(width / 2 + 3, bottomY - 30, 70, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> onClose()
        ).bounds(width / 2 + 79, bottomY - 30, 70, 20).build());
    }

    private void refreshProfiles() {
        scrollOffset = 0;
        profiles = getSortedProfiles();
    }

    private List<BindConfig> getSortedProfiles() {
        List<BindConfig> list = new ArrayList<>(BindManagerClient.getConfigStore().getProfiles());
        if (showFavoritesOnly) {
            list.removeIf(c -> !c.isFavorite());
        }
        switch (sortMode) {
            case 1 -> list.sort(Comparator.comparing(c -> c.getName().toLowerCase()));
            case 2 -> list.sort(Comparator.comparing((BindConfig c) -> !c.isFavorite()).thenComparing(c -> c.getName().toLowerCase()));
            case 3 -> list.sort(Comparator.comparingInt(BindConfig::getColor).thenComparing(c -> c.getName().toLowerCase()));
        }
        return list;
    }

    private int getListLeft() { return 8; }
    private int getListRight() { return width - 8; }
    private int getMaxVisible() { return (height - getListTop() - FOOTER_HEIGHT) / ENTRY_HEIGHT; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        renderHeader(graphics, mouseX, mouseY);
        renderProfileList(graphics, mouseX, mouseY, delta);
    }

    private void renderHeader(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        ctx.centeredText(font, title, width / 2, 12, 0xFFFFFF);

        String activeName = BindManagerClient.getConfigStore().getActiveProfileName();
        if (activeName != null) {
            Component activeText = Component.translatable("screen.changeofcontrol.active_profile", activeName);
            ctx.text(font, activeText, 8, 12, 0x55FF55);
        }

        // Sort panel (always shown)
        int panelY = HEADER_H + 2;
        int btnH = 18;
        int gap = 6;
        int startX = 10;
        int totalW = width - 20;
        int sBtnW = (totalW - gap * 3) / 4;

        for (int i = 0; i < 4; i++) {
            int bx = startX + i * (sBtnW + gap);
            boolean sel = i == sortMode;
            boolean hp = mouseX >= bx && mouseX < bx + sBtnW && mouseY >= panelY && mouseY < panelY + btnH;
            int bg = sel ? 0xFF000000 | SORT_COLORS[i] : (hp ? 0x66FFFFFF : 0x33FFFFFF);
            ctx.fill(bx, panelY, bx + sBtnW, panelY + btnH, bg);
            if (sel) ctx.fill(bx, panelY, bx + 2, panelY + btnH, 0xFFFFFFFF);
            Component label = Component.translatable(SORT_KEYS[i]);
            ctx.centeredText(font, label, bx + sBtnW / 2, panelY + 5, 0xFFFFFF);
        }
    }

    private void renderProfileList(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int startY = getListTop();
        int maxVisible = getMaxVisible();
        int listLeft = getListLeft();
        int listRight = getListRight();
        hoveredIndex = -1;

        ctx.fill(listLeft, startY - 2, listRight, startY - 1, 0x33FFFFFF);

        for (int i = 0; i < maxVisible && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            BindConfig config = profiles.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            boolean isDraggingThis = dragging && index == dragIndex;

            // Calculate target drop position for indicator
            int effectiveY = isDraggingThis ? dragVisualY : y;
            boolean hovered = !dragging && mouseX >= listLeft && mouseX <= listRight && mouseY >= y && mouseY < y + ENTRY_HEIGHT - 1;
            if (hovered) hoveredIndex = index;

            if (isDraggingThis) {
                ctx.fill(listLeft, dragVisualY - 1, listRight, dragVisualY + ENTRY_HEIGHT - 1, 0x44AAFF88);
                ctx.fill(listLeft, dragVisualY - 1, listRight, dragVisualY, 0xFF55FF55);
                ctx.fill(listLeft, dragVisualY + ENTRY_HEIGHT - 2, listRight, dragVisualY + ENTRY_HEIGHT - 1, 0xFF55FF55);
            }

            int bgColor = hovered ? 0x44FFFFFF : 0x22FFFFFF;
            int borderColor = isDraggingThis && index == dragIndex ? 0xFF55FF55 : 0x00000000;
            if (borderColor != 0) {
                ctx.fill(listLeft, y, listRight, y + ENTRY_HEIGHT - 1, 0x33AAFF88);
            } else {
                ctx.fill(listLeft, y, listRight, y + ENTRY_HEIGHT - 1, bgColor);
            }

            int colorStrip = config.getColor();
            ctx.fill(listLeft, y, listLeft + 3, y + ENTRY_HEIGHT - 1, 0xFF000000 | colorStrip);

            int nameX = listLeft + 8;
            int nameColor = config.isFavorite() ? 0xFFFF55 : 0xFFFFFF;
            String displayName = config.getName();
            if (config.isFavorite()) {
                displayName = ChatFormatting.YELLOW + "\u2605 " + ChatFormatting.RESET + displayName;
            }

            // Draw active indicator
            String activeName = BindManagerClient.getConfigStore().getActiveProfileName();
            boolean isActive = config.getName().equals(activeName);
            if (isActive) {
                ctx.text(font, Component.literal("> "), nameX - 10, y + 6, 0x55FF55);
                ctx.fill(listLeft + 3, y, listLeft + 5, y + ENTRY_HEIGHT - 1, 0xFF55FF55);
            }

            float scale = delta;
            ctx.text(font, Component.literal(displayName), nameX + 2, y + 6, nameColor);

            int btnW = 46;
            int gap = 3;
            int bx = listRight;
            int buttonY = y + 4;

            int delX = bx - btnW;
            int colX = delX - btnW - gap;
            int renX = colX - btnW - gap;
            int loadX = renX - btnW - gap;
            int favX = loadX - btnW - gap;

            String star = config.isFavorite() ? "\u2605" : "\u2606";
            int favColor = config.isFavorite() ? 0xFFFF55 : 0xAAAAAA;

            drawHoverBtnLiteral(ctx, favX, buttonY, btnW, star, favColor, mouseX, mouseY);
            drawHoverBtn(ctx, loadX, buttonY, btnW, "screen.changeofcontrol.load", 0x55FF55, mouseX, mouseY);
            drawHoverBtn(ctx, renX, buttonY, btnW, "screen.changeofcontrol.rename", 0xFFFF55, mouseX, mouseY);
            drawHoverBtn(ctx, delX, buttonY, btnW, "screen.changeofcontrol.delete", 0xFF5555, mouseX, mouseY);
            drawHoverBtn(ctx, colX, buttonY, btnW, "screen.changeofcontrol.color", 0x55FFFF, mouseX, mouseY);
        }

        if (profiles.isEmpty()) {
            ctx.centeredText(font, Component.translatable("screen.changeofcontrol.empty"), width / 2, getListTop() + 40, 0x888888);
        }

        // Draw entry drag ghost
        if (dragging && dragIndex >= 0 && dragIndex < profiles.size()) {
            BindConfig ghostConfig = profiles.get(dragIndex);
            int ghostY = dragVisualY;
            ctx.fill(listLeft, ghostY, listRight, ghostY + ENTRY_HEIGHT - 1, 0x66AAFF88);
            ctx.fill(listLeft, ghostY, listLeft + 3, ghostY + ENTRY_HEIGHT - 1, 0xFF55FF55);
            ctx.text(font, Component.literal("\u2261 " + ghostConfig.getName()), listLeft + 8, ghostY + 6, 0xFFFFFF);
        }

        // Scroll indicator
        if (profiles.size() > maxVisible) {
            String scrollText = (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxVisible, profiles.size()) + "/" + profiles.size();
            ctx.text(font, Component.literal(scrollText), width / 2 - font.width(scrollText) / 2, height - 30, 0x888888);
        }
    }

    private void drawHoverBtn(GuiGraphicsExtractor ctx, int x, int y, int w, String langKey, int color, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + 14;
        int bg = hovered ? (0x88 << 24) : 0x22FFFFFF;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 13, bg);
        if (hovered) ctx.fill(x - 1, y - 1, x + w + 1, y, 0xFF000000 | color);
        ctx.text(font, Component.translatable(langKey), x + 2, y + 2, hovered ? 0xFFFFFF : color);
    }

    private void drawHoverBtnLiteral(GuiGraphicsExtractor ctx, int x, int y, int w, String literal, int color, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + 14;
        int bg = hovered ? (0x88 << 24) : 0x22FFFFFF;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 13, bg);
        if (hovered) ctx.fill(x - 1, y - 1, x + w + 1, y, 0xFF000000 | color);
        ctx.text(font, Component.literal(literal), x + 2, y + 2, hovered ? 0xFFFFFF : color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        if (super.mouseClicked(event, focused)) return true;

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // Sort panel button clicks
        int panelY = HEADER_H + 2;
        int bH = 18;
        int gap = 6;
        int startX = 10;
        int totalW = width - 20;
        int sBtnW = (totalW - gap * 3) / 4;

        for (int i = 0; i < 4; i++) {
            int bx = startX + i * (sBtnW + gap);
            if (mouseX >= bx && mouseX < bx + sBtnW && mouseY >= panelY && mouseY < panelY + bH) {
                if (button == 0) {
                    sortMode = i;
                    refreshProfiles();
                    return true;
                }
            }
        }

        // Profile list
        int startY = getListTop();
        int maxVisible = getMaxVisible();
        int listLeft = getListLeft();
        int listRight = getListRight();

        for (int i = 0; i < maxVisible && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            BindConfig config = profiles.get(index);
            int y = startY + i * ENTRY_HEIGHT;

            if (mouseY >= y && mouseY < y + ENTRY_HEIGHT - 1 && mouseX >= listLeft && mouseX <= listRight) {
                int btnW = 46;
                int gap2 = 3;
                int bx = listRight;
                int buttonY = y + 4;

                int delX = bx - btnW;
                int colX = delX - btnW - gap2;
                int renX = colX - btnW - gap2;
                int loadX = renX - btnW - gap2;
                int favX = loadX - btnW - gap2;

                if (button == 0) {
                    if (mouseX >= favX && mouseX < favX + btnW) {
                        toggleFavorite(config);
                        return true;
                    } else if (mouseX >= loadX && mouseX < loadX + btnW) {
                        loadProfile(config.getName());
                        return true;
                    } else if (mouseX >= renX && mouseX < renX + btnW) {
                        renameProfile(config);
                        return true;
                    } else if (mouseX >= delX && mouseX < delX + btnW) {
                        deleteProfile(config);
                        return true;
                    } else if (mouseX >= colX && mouseX < colX + btnW) {
                        openColorPicker(config);
                        return true;
                    } else {
                        // Start drag on LMB on entry area
                        dragging = true;
                        dragIndex = index;
                        dragMouseY = (int) mouseY;
                        dragVisualY = dragMouseY - ENTRY_HEIGHT / 2;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == 0) {
            dragging = false;
            int startY = getListTop();
            int dropIndex = Mth.clamp((int) ((event.y() - startY) / ENTRY_HEIGHT) + scrollOffset, 0, profiles.size() - 1);
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
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            dragMouseY = (int) event.y();
            dragVisualY = Mth.clamp(dragMouseY, getListTop(), height - FOOTER_HEIGHT) - ENTRY_HEIGHT / 2;
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxVisible = getMaxVisible();
        int maxScroll = Math.max(0, profiles.size() - maxVisible);
        scrollOffset = Mth.clamp(scrollOffset - (int) verticalAmount, 0, maxScroll);
        return true;
    }

    private void loadProfile(String name) {
        BindManagerClient.getConfigStore().loadProfile(name);
    }

    private void renameProfile(BindConfig config) {
        String currentName = config.getName();
        minecraft.setScreen(new NameInputScreen(
                this,
                Component.translatable("screen.changeofcontrol.rename.title"),
                Component.translatable("screen.changeofcontrol.rename.field"),
                newName -> {
                    if (!newName.isEmpty() && !newName.equals(currentName)) {
                        BindManagerClient.getConfigStore().renameProfile(currentName, newName);
                        refreshProfiles();
                    }
                    return null;
                }
        ));
    }

    private void deleteProfile(BindConfig config) {
        String name = config.getName();
        minecraft.setScreen(new ConfirmDeleteScreen(
                this, name,
                () -> {
                    BindManagerClient.getConfigStore().deleteProfile(name);
                    refreshProfiles();
                }
        ));
    }

    private void toggleFavorite(BindConfig config) {
        config.setFavorite(!config.isFavorite());
        BindManagerClient.getConfigStore().saveExistingProfile(config);
        refreshProfiles();
    }

    private void openColorPicker(BindConfig config) {
        minecraft.setScreen(new ColorPickerScreen(this, config));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // --- Color picker ---
    public static class ColorPickerScreen extends Screen {
        private final Screen parent;
        private final BindConfig config;

        public ColorPickerScreen(Screen parent, BindConfig config) {
            super(Component.translatable("screen.changeofcontrol.color_picker"));
            this.parent = parent;
            this.config = config;
        }

        @Override
        protected void init() {
            super.init();
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.cancel"),
                    btn -> onClose()
            ).bounds(width / 2 - 100, height / 2 + 55, 200, 20).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.centeredText(font, title, width / 2, height / 2 - 55, 0xFFFFFF);

            int cols = 5;
            int cellSize = 22;
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
                graphics.fill(x - 1, y - 1, x + cellSize + 1, y + cellSize + 1, 0xFF000000 | borderColor);
                graphics.fill(x, y, x + cellSize, y + cellSize, 0xFF000000 | PALETTE[i]);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
            if (event.button() == 0) {
                double mouseX = event.x();
                double mouseY = event.y();
                int cols = 5;
                int cellSize = 22;
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
                        onClose();
                        return true;
                    }
                }
            }
            return super.mouseClicked(event, focused);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }
    }
}
