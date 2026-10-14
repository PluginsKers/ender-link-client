package com.eyreland.enderlink.client.mixin;

import com.eyreland.enderlink.client.EnderChestButton;
import com.eyreland.enderlink.client.OpenEnderChest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookScreenMixin extends Screen {
    @Shadow @Final public RecipeBookComponent<?> recipeBookComponent;
    @Shadow protected abstract ScreenPosition getRecipeBookButtonPosition();
    @Unique private EnderChestButton enderlink$button;

    protected RecipeBookScreenMixin(Component title) { super(title); }

    @Inject(method = "initButton", at = @At("TAIL"))
    private void enderlink$init(CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen) {
            enderlink$button = addRenderableWidget(new EnderChestButton());
            enderlink$update();
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void enderlink$render(CallbackInfo ci) {
        enderlink$update();
    }

    @Unique
    private void enderlink$update() {
        if (enderlink$button == null) return;
        var position = getRecipeBookButtonPosition();
        enderlink$button.setPosition(position.x() + 22, position.y());
        enderlink$button.visible = minecraft.getConnection() != null && ClientPlayNetworking.canSend(OpenEnderChest.TYPE)
            && !(width < 379 && recipeBookComponent.isVisible());
        enderlink$button.active = minecraft.player != null && minecraft.player.isAlive()
            && !minecraft.player.isSpectator() && minecraft.player.containerMenu.getCarried().isEmpty();
    }
}
