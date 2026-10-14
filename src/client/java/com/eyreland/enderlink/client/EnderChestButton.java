package com.eyreland.enderlink.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EnderChestButton extends Button {
    private final ItemStack icon = new ItemStack(Items.ENDER_CHEST);

    public EnderChestButton() {
        super(0, 0, 20, 18, Component.translatable("container.enderchest"), button -> {
            var player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu.getCarried().isEmpty() && ClientPlayNetworking.canSend(OpenEnderChest.TYPE)) {
                ClientPlayNetworking.send(OpenEnderChest.INSTANCE);
            }
        }, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractDefaultSprite(graphics);
        graphics.item(icon, getX() + 2, getY() + 1);
    }
}
