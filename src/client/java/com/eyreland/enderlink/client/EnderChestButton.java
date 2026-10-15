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
    private static final Tooltip OPEN_TOOLTIP = Tooltip.create(Component.translatable("container.enderchest"));
    private static final Tooltip UNAVAILABLE_TOOLTIP = Tooltip.create(Component.translatable("ender-link.server_unavailable"));
    private final ItemStack icon = new ItemStack(Items.ENDER_CHEST);

    public EnderChestButton() {
        super(0, 0, 20, 18, Component.translatable("container.enderchest"), button -> {
            var player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu.getCarried().isEmpty() && ClientPlayNetworking.canSend(OpenEnderChest.TYPE)) {
                ClientPlayNetworking.send(OpenEnderChest.INSTANCE);
            }
        }, DEFAULT_NARRATION);
    }

    public void updateState() {
        var client = Minecraft.getInstance();
        boolean supported = client.getConnection() != null && ClientPlayNetworking.canSend(OpenEnderChest.TYPE);
        active = supported && client.player != null && client.player.isAlive()
            && !client.player.isSpectator() && client.player.containerMenu.getCarried().isEmpty();
        setTooltip(supported ? OPEN_TOOLTIP : UNAVAILABLE_TOOLTIP);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractDefaultSprite(graphics);
        graphics.item(icon, getX() + 2, getY() + 1);
    }
}
