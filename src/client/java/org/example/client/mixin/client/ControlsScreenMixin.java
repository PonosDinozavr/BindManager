package org.example.client.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.example.client.BindManagerClient;
import org.example.client.screen.NameInputScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlsScreen.class)
public class ControlsScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void addProfileButtons(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;

        screen.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.bindmanager.save_profile"),
                btn -> {
                    if (screen.getClient() != null) {
                        screen.getClient().setScreen(new NameInputScreen(
                                screen,
                                Text.translatable("screen.bindmanager.save_profile.title"),
                                Text.translatable("screen.bindmanager.save_profile.field"),
                                name -> {
                                    if (!name.isEmpty() && !BindManagerClient.getConfigStore().profileExists(name)) {
                                        BindManagerClient.getConfigStore().saveProfile(name);
                                    }
                                    return null;
                                }
                        ));
                    }
                }
        ).dimensions(screen.width / 2 - 155, screen.height - 28, 150, 20).build());

        screen.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.bindmanager.manage_profiles"),
                btn -> {
                    if (screen.getClient() != null) {
                        screen.getClient().setScreen(new org.example.client.screen.BindManagerScreen(screen));
                    }
                }
        ).dimensions(screen.width / 2 + 5, screen.height - 28, 150, 20).build());
    }
}
