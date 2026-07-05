package org.example.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
    private static final String[] SORT_KEYS = {
            "screen.bindmanager.sort.0", "screen.bindmanager.sort.1",
            "screen.bindmanager.sort.2", "screen.bindmanager.sort.3"
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

    // Bolvanchik
    private Bolvanchik bolvanchik;

    private static final int ENTRY_HEIGHT = 28;
    private static final int HEADER_H = 16;
    private static final int SORT_PANEL_H = 24;
    private static final int FOOTER_HEIGHT = 60;

    private int getListTop() {
        return HEADER_H + SORT_PANEL_H + 4;
    }

    public BindManagerScreen(Screen parent) {
        super(Text.translatable("screen.bindmanager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        refreshProfiles();
        bolvanchik = new Bolvanchik(width - 120, 50, 48, 48);

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
        ).dimensions(width / 2 - 130, bottomY - 30, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.bindmanager.filter_fav"),
                btn -> {
                    showFavoritesOnly = !showFavoritesOnly;
                    refreshProfiles();
                }
        ).dimensions(width / 2 - 40, bottomY - 30, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                btn -> close()
        ).dimensions(width / 2 + 50, bottomY - 30, 80, 20).build());
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        bolvanchik.update(width, height);
        renderHeader(context, mouseX, mouseY);
        renderProfileList(context, mouseX, mouseY, delta);
        bolvanchik.render(context);
    }

    private void renderHeader(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 6, 0xFFFFFF);

        String activeName = BindManagerClient.getConfigStore().getActiveProfileName();
        if (activeName != null) {
            Text activeText = Text.translatable("screen.bindmanager.active_profile", activeName);
            ctx.drawText(textRenderer, activeText, 8, 6, 0x55FF55, false);
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
            Text label = Text.translatable(SORT_KEYS[i]);
            ctx.drawCenteredTextWithShadow(textRenderer, label, bx + sBtnW / 2, panelY + 5, 0xFFFFFF);
        }
    }

    private void renderProfileList(DrawContext ctx, int mouseX, int mouseY, float delta) {
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
                displayName = Formatting.YELLOW + "\u2605 " + Formatting.RESET + displayName;
            }

            // Draw active indicator
            String activeName = BindManagerClient.getConfigStore().getActiveProfileName();
            boolean isActive = config.getName().equals(activeName);
            if (isActive) {
                ctx.drawText(textRenderer, Text.literal("> "), nameX - 10, y + 6, 0x55FF55, false);
                ctx.fill(listLeft + 3, y, listLeft + 5, y + ENTRY_HEIGHT - 1, 0xFF55FF55);
            }

            float scale = delta;
            ctx.drawText(textRenderer, Text.literal(displayName), nameX + 2, y + 6, nameColor, false);

            int btnW = 38;
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
            drawHoverBtn(ctx, loadX, buttonY, btnW, "screen.bindmanager.load", 0x55FF55, mouseX, mouseY);
            drawHoverBtn(ctx, renX, buttonY, btnW, "screen.bindmanager.rename", 0xFFFF55, mouseX, mouseY);
            drawHoverBtn(ctx, delX, buttonY, btnW, "screen.bindmanager.delete", 0xFF5555, mouseX, mouseY);
            drawHoverBtn(ctx, colX, buttonY, btnW, "screen.bindmanager.color", 0x55FFFF, mouseX, mouseY);
        }

        if (profiles.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.bindmanager.empty"), width / 2, getListTop() + 40, 0x888888);
        }

        // Draw entry drag ghost
        if (dragging && dragIndex >= 0 && dragIndex < profiles.size()) {
            BindConfig ghostConfig = profiles.get(dragIndex);
            int ghostY = dragVisualY;
            ctx.fill(listLeft, ghostY, listRight, ghostY + ENTRY_HEIGHT - 1, 0x66AAFF88);
            ctx.fill(listLeft, ghostY, listLeft + 3, ghostY + ENTRY_HEIGHT - 1, 0xFF55FF55);
            ctx.drawText(textRenderer, Text.literal("\u2261 " + ghostConfig.getName()), listLeft + 8, ghostY + 6, 0xFFFFFF, false);
        }

        // Scroll indicator
        if (profiles.size() > maxVisible) {
            String scrollText = (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxVisible, profiles.size()) + "/" + profiles.size();
            ctx.drawText(textRenderer, Text.literal(scrollText), width / 2 - textRenderer.getWidth(scrollText) / 2, height - 30, 0x888888, false);
        }
    }

    private void drawHoverBtn(DrawContext ctx, int x, int y, int w, String langKey, int color, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + 14;
        int bg = hovered ? (0x88 << 24) : 0x22FFFFFF;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 13, bg);
        if (hovered) ctx.fill(x - 1, y - 1, x + w + 1, y, 0xFF000000 | color);
        ctx.drawText(textRenderer, Text.translatable(langKey), x + 2, y + 2, hovered ? 0xFFFFFF : color, false);
    }

    private void drawHoverBtnLiteral(DrawContext ctx, int x, int y, int w, String literal, int color, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + 14;
        int bg = hovered ? (0x88 << 24) : 0x22FFFFFF;
        ctx.fill(x - 1, y - 1, x + w + 1, y + 13, bg);
        if (hovered) ctx.fill(x - 1, y - 1, x + w + 1, y, 0xFF000000 | color);
        ctx.drawText(textRenderer, Text.literal(literal), x + 2, y + 2, hovered ? 0xFFFFFF : color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Bolvanchik interaction
        if (bolvanchik.contains(mouseX, mouseY)) {
            if (button == 1) {
                bolvanchik.grab(mouseX, mouseY);
                return true;
            } else if (button == 0) {
                bolvanchik.playSound();
                return true;
            }
        }

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
                int btnW = 38;
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            int startY = getListTop();
            int dropIndex = MathHelper.clamp((int) ((mouseY - startY) / ENTRY_HEIGHT) + scrollOffset, 0, profiles.size() - 1);
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
        if (button == 1) {
            bolvanchik.release(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            dragMouseY = (int) mouseY;
            dragVisualY = MathHelper.clamp(dragMouseY, getListTop(), height - FOOTER_HEIGHT) - ENTRY_HEIGHT / 2;
            return true;
        }
        if (button == 1 && bolvanchik.grabbed) {
            bolvanchik.drag(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxVisible = getMaxVisible();
        int maxScroll = Math.max(0, profiles.size() - maxVisible);
        scrollOffset = MathHelper.clamp(scrollOffset - (int) verticalAmount, 0, maxScroll);
        return true;
    }

    private void loadProfile(String name) {
        BindManagerClient.getConfigStore().loadProfile(name);
        String msg = Text.translatable("screen.bindmanager.loaded", name).getString();
        BindManagerClient.showToast(msg);
    }

    private void renameProfile(BindConfig config) {
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
    }

    private void deleteProfile(BindConfig config) {
        String name = config.getName();
        client.setScreen(new ConfirmDeleteScreen(
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
        client.setScreen(new ColorPickerScreen(this, config));
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    // --- Bolvanchik physics object ---
    private static class Bolvanchik {
        private static final Identifier TEXTURE = Identifier.of("bind-manager", "bolvanchik");
        private static final String[] FROG_SOUNDS = {
                "Frog_idle1", "Frog_idle2", "Frog_idle3", "Frog_idle4",
                "Frog_idle5", "Frog_idle6", "Frog_idle7", "Frog_idle8"
        };

        double x, y, w, h;
        double vx, vy;
        boolean grabbed;
        double grabOffX, grabOffY;

        Bolvanchik(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        void update(int screenW, int screenH) {
            if (!grabbed) {
                vy += 0.4;
                vx *= 0.97;
                vy *= 0.97;
                x += vx;
                y += vy;
                if (x < 0) { x = 0; vx = -vx * 0.6; }
                if (x + w > screenW) { x = screenW - w; vx = -vx * 0.6; }
                if (y < 0) { y = 0; vy = -vy * 0.6; }
                if (y + h > screenH) { y = screenH - h; vy = -vy * 0.6; }
            }
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        void grab(double mx, double my) {
            grabbed = true;
            grabOffX = mx - x;
            grabOffY = my - y;
            vx = 0;
            vy = 0;
        }

        void drag(double mx, double my) {
            x = mx - grabOffX;
            y = my - grabOffY;
        }

        void release(double mx, double my) {
            if (grabbed) {
                grabbed = false;
                vx = (mx - x - grabOffX) * 0.3;
                vy = (my - y - grabOffY) * 0.3;
            }
        }

        void playSound() {
            try {
                int idx = (int) (Math.random() * FROG_SOUNDS.length);
                Identifier soundId = Identifier.of("bind-manager", FROG_SOUNDS[idx]);
                var mc = MinecraftClient.getInstance();
                if (mc != null && mc.getSoundManager() != null) {
                    SoundEvent soundEvent = SoundEvent.of(soundId);
                    mc.getSoundManager().play(PositionedSoundInstance.master(soundEvent, 1.0F));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void render(DrawContext ctx) {
            try {
                ctx.drawTexture(RenderLayer::getGuiTextured, TEXTURE, (int) x, (int) y, (int) w, (int) h, 0, 0, (int) w, (int) h, (int) w, (int) h);
            } catch (Exception e) {
                // silently ignore texture errors
            }
        }
    }

    // --- Color picker ---
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
