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
import org.example.client.screen.BindManagerScreen;
import org.example.client.screen.NameInputScreen;
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

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof KeybindsScreen) {
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
            }
        });
    }

    public static BindConfigStore getConfigStore() {
        return configStore;
    }
}
