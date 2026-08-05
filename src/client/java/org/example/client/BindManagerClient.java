package org.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.example.client.config.BindConfig;
import org.example.client.mixin.client.ScreenAccessor;
import org.example.client.screen.BindManagerScreen;
import org.example.client.screen.NameInputScreen;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public class BindManagerClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("changeofcontrol", "category"));

    private static KeyMapping openManagerKey;
    private static BindConfigStore configStore;

    private static int autoSaveCheckTimer;

    @Override
    public void onInitializeClient() {
        configStore = new BindConfigStore(Minecraft.getInstance());

        openManagerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.changeofcontrol.open_manager",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openManagerKey.consumeClick()) {
                client.gui.setScreen(new BindManagerScreen(client.gui.screen()));
            }

            // Auto-save active profile changes while in KeyBindsScreen
            if (client.gui.screen() instanceof KeyBindsScreen) {
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
            if (screen instanceof KeyBindsScreen) {
                ScreenAccessor accessor = (ScreenAccessor) screen;

                accessor.invokeAddRenderableWidget(
                        Button.builder(
                                Component.literal("+"),
                                btn -> client.gui.setScreen(new NameInputScreen(
                                        screen,
                                        Component.translatable("screen.changeofcontrol.save_profile.title"),
                                        Component.translatable("screen.changeofcontrol.save_profile.field"),
                                        name -> {
                                            if (!name.isEmpty()) {
                                                configStore.saveProfile(name);
                                            }
                                            return null;
                                        }
                                ))
                        ).bounds(scaledWidth - 22, 2, 20, 20).build()
                );
            }
        });
    }

    private static Map<String, String> getCurrentBindingsSnapshot() {
        Minecraft client = Minecraft.getInstance();
        Map<String, String> snapshot = new LinkedHashMap<>();
        if (client != null && client.options != null) {
            for (KeyMapping binding : client.options.keyMappings) {
                String boundKey = BindConfigStore.getBoundKeyTranslationKey(binding);
                if (boundKey != null && !boundKey.isEmpty() && !"key.keyboard.unknown".equals(boundKey)) {
                    snapshot.put(binding.getName(), boundKey);
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

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return false;

        for (KeyMapping binding : client.options.keyMappings) {
            String currentKey = BindConfigStore.getBoundKeyTranslationKey(binding);
            String storedKey = activeProfile.getBoundKey(binding.getName());
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
