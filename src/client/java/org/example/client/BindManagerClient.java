package org.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.example.client.mixin.client.ScreenAccessor;
import org.example.client.config.BindConfig;
import org.example.client.screen.BindManagerScreen;
import org.example.client.screen.NameInputScreen;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public class BindManagerClient implements ClientModInitializer {
    private static KeyBinding openManagerKey;
    private static BindConfigStore configStore;

    private static int autoSaveCheckTimer;

    @Override
    public void onInitializeClient() {
        configStore = new BindConfigStore(MinecraftClient.getInstance());

        openManagerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.changeofcontrol.open_manager",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                "category.bindmanager"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openManagerKey.wasPressed()) {
                client.setScreen(new BindManagerScreen(client.currentScreen));
            }

            // Auto-save active profile changes while in KeybindsScreen
            if (client.currentScreen instanceof KeybindsScreen) {
                autoSaveCheckTimer++;
                if (autoSaveCheckTimer % 10 == 0) {
                    String activeName = configStore.getActiveProfileName();
                    if (activeName != null && hasChanges()) {
                        BindConfig activeConfig = configStore.getActiveProfile();
                        if (activeConfig != null) {
                            activeConfig.getKeyBindings().clear();
                            activeConfig.getKeyBindings().putAll(getCurrentBindingsSnapshot());
                            configStore.saveExistingProfile(activeConfig);
                        }
                    }
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof KeybindsScreen) {
                ScreenAccessor accessor = (ScreenAccessor) screen;

                accessor.invokeAddDrawableChild(
                        ButtonWidget.builder(
                                Text.literal("+"),
                                btn -> client.setScreen(new NameInputScreen(
                                        screen,
                                        Text.translatable("screen.changeofcontrol.save_profile.title"),
                                        Text.translatable("screen.changeofcontrol.save_profile.field"),
                                        name -> {
                                            if (!name.isEmpty()) {
                                                configStore.saveProfile(name);
                                            }
                                            return null;
                                        }
                                ))
                        ).dimensions(scaledWidth - 22, 2, 20, 20).build()
                );
            }
        });
    }

    private static Map<String, String> getCurrentBindingsSnapshot() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<String, String> snapshot = new LinkedHashMap<>();
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

    public static BindConfigStore getConfigStore() {
        return configStore;
    }

}
