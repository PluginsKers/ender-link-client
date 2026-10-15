package com.eyreland.enderlink.client.mixin;

import com.eyreland.enderlink.client.EnderChestButton;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

    @ModifyExpressionValue(method = {"initButton", "lambda$initButton$0"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;getRecipeBookButtonPosition()Lnet/minecraft/client/gui/navigation/ScreenPosition;"))
    private ScreenPosition enderlink$bookPosition(ScreenPosition position) {
        // 给末影箱留出原按钮的位置，避免向左覆盖副手槽。
        return (Object) this instanceof InventoryScreen ? new ScreenPosition(position.x() + 22, position.y()) : position;
    }

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
        enderlink$button.setPosition(position.x(), position.y());
        enderlink$button.visible = !(width < 379 && recipeBookComponent.isVisible());
        enderlink$button.updateState();
    }
}
