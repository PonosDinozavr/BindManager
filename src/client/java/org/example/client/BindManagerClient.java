package org.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.sound.SoundEvent;
import org.example.client.mixin.client.ScreenAccessor;
import org.example.client.config.BindConfig;
import org.example.client.screen.BindManagerScreen;
import org.example.client.screen.NameInputScreen;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class BindManagerClient implements ClientModInitializer {
    private static KeyBinding openManagerKey;
    private static BindConfigStore configStore;

    private static String toastMessage;
    private static int toastTimer;
    private static final int TOAST_DURATION = 80;

    private static boolean changesDetected;
    private static boolean shouldShowSaveBtn;
    private static int changeCheckTimer;
    private static ButtonWidget saveButton;

    @Override
    public void onInitializeClient() {
        configStore = new BindConfigStore(MinecraftClient.getInstance());

        openManagerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bindmanager.open_manager",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                "category.bindmanager"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openManagerKey.wasPressed()) {
                client.setScreen(new BindManagerScreen(client.currentScreen));
            }
            if (toastTimer > 0) toastTimer--;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof KeybindsScreen ks) {
                ScreenAccessor accessor = (ScreenAccessor) screen;

                accessor.invokeAddDrawableChild(
                        ButtonWidget.builder(
                                Text.literal("+"),
                                btn -> client.setScreen(new NameInputScreen(
                                        screen,
                                        Text.translatable("screen.bindmanager.save_profile.title"),
                                        Text.translatable("screen.bindmanager.save_profile.field"),
                                        name -> {
                                            if (!name.isEmpty()) {
                                                configStore.saveProfile(name);
                                            }
                                            return null;
                                        }
                                ))
                        ).dimensions(scaledWidth - 22, 2, 20, 20).build()
                );

                saveButton = ButtonWidget.builder(
                        Text.translatable("screen.bindmanager.save_changes"),
                        btn -> {
                            String activeName = configStore.getActiveProfileName();
                            if (activeName != null) {
                                BindConfig activeConfig = configStore.getActiveProfile();
                                if (activeConfig != null) {
                                    activeConfig.getKeyBindings().clear();
                                    activeConfig.getKeyBindings().putAll(getCurrentBindingsSnapshot());
                                    configStore.saveExistingProfile(activeConfig);
                                }
                                showToast(Text.translatable("screen.bindmanager.saved", activeName).getString());
                                changesDetected = false;
                                shouldShowSaveBtn = false;
                            }
                        }
                ).dimensions(scaledWidth - 130, 2, 100, 20).build();

                accessor.invokeAddDrawableChild(new SaveButtonController());
                accessor.invokeAddDrawableChild(new ToastWidget());
            }
        });
    }

    private static Map<String, String> getCurrentBindingsSnapshot() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<String, String> snapshot = new java.util.LinkedHashMap<>();
        if (client != null && client.options != null) {
            for (KeyBinding binding : client.options.allKeys) {
                String boundKey = binding.getBoundKeyTranslationKey();
                if (boundKey != null && !boundKey.isEmpty() && !"key.keyboard.unknown".equals(boundKey)) {
                    snapshot.put(binding.getTranslationKey(), boundKey);
                }
            }
        }
        return snapshot;
    }

    private static boolean hasChanges() {
        String activeName = configStore.getActiveProfileName();
        if (activeName == null) return false;
        BindConfig activeProfile = configStore.getActiveProfile();
        if (activeProfile == null) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return false;

        for (KeyBinding binding : client.options.allKeys) {
            String currentKey = binding.getBoundKeyTranslationKey();
            String storedKey = activeProfile.getBoundKey(binding.getTranslationKey());
            if (currentKey == null && (storedKey == null || storedKey.isEmpty())) continue;
            if (currentKey == null) return true;
            if (storedKey == null || storedKey.isEmpty()) {
                if (!"key.keyboard.unknown".equals(currentKey)) return true;
                continue;
            }
            if (!currentKey.equals(storedKey)) return true;
        }
        return false;
    }

    public static void showToast(String message) {
        toastMessage = message;
        toastTimer = TOAST_DURATION;
    }

    public static BindConfigStore getConfigStore() {
        return configStore;
    }

    private static class SaveButtonController implements Element, net.minecraft.client.gui.Drawable, Selectable {
        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            changeCheckTimer++;
            if (changeCheckTimer % 10 == 0) {
                changesDetected = hasChanges();
                shouldShowSaveBtn = changesDetected && configStore.getActiveProfileName() != null;
            }
            if (shouldShowSaveBtn) {
                saveButton.render(ctx, mx, my, delta);
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) {
            if (shouldShowSaveBtn) return saveButton.mouseClicked(mx, my, b);
            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int b) { return false; }
        @Override
        public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { return false; }
        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) { return false; }
        @Override
        public boolean keyPressed(int k, int s, int m) { return false; }
        @Override
        public boolean keyReleased(int k, int s, int m) { return false; }
        @Override
        public boolean charTyped(char c, int m) { return false; }
        @Override
        public void setFocused(boolean f) {}
        @Override
        public boolean isFocused() { return false; }
        @Override
        public boolean isMouseOver(double mx, double my) { return false; }
        @Override
        public SelectionType getType() { return SelectionType.NONE; }
        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {}
    }

    private static class ToastWidget implements Element, net.minecraft.client.gui.Drawable, Selectable {
        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            if (toastTimer > 0 && toastMessage != null) {
                int alpha = MathHelper.clamp(toastTimer * 4, 0, 255);
                int color = (alpha << 24) | 0x55FF55;
                int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
                int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
                ctx.drawCenteredTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        Text.literal(toastMessage),
                        w / 2, h - 40, color
                );
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) { return false; }
        @Override
        public boolean mouseReleased(double mx, double my, int b) { return false; }
        @Override
        public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { return false; }
        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) { return false; }
        @Override
        public boolean keyPressed(int k, int s, int m) { return false; }
        @Override
        public boolean keyReleased(int k, int s, int m) { return false; }
        @Override
        public boolean charTyped(char c, int m) { return false; }
        @Override
        public void setFocused(boolean f) {}
        @Override
        public boolean isFocused() { return false; }
        @Override
        public boolean isMouseOver(double mx, double my) { return false; }
        @Override
        public SelectionType getType() { return SelectionType.NONE; }
        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {}
    }
}
