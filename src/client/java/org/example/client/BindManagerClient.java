package org.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.example.client.screen.BindManagerScreen;
import org.lwjgl.glfw.GLFW;

public class BindManagerClient implements ClientModInitializer {
    private static KeyBinding openManagerKey;
    private static BindConfigStore configStore;

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
        });
    }

    public static BindConfigStore getConfigStore() {
        return configStore;
    }
}
