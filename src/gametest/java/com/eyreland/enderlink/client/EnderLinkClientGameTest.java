package com.eyreland.enderlink.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.concurrent.atomic.AtomicInteger;

public final class EnderLinkClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        try (var world = context.worldBuilder().create()) {
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitTick();
            context.runOnClient(client -> {
                check(button(client).visible && !button(client).active, "Unsupported servers show a disabled button");
                checkPosition(client);
            });
            clickEnderChest(context);
            context.waitTick();
            context.runOnClient(client -> check(client.gui.screen() instanceof InventoryScreen, "Unsupported servers cannot open a chest"));
            context.takeScreenshot("ender-link-unavailable");
        }
        var requests = new AtomicInteger();
        ServerPlayNetworking.registerGlobalReceiver(OpenEnderChest.TYPE, (payload, connection) -> {
            requests.incrementAndGet();
            connection.player().openMenu(new SimpleMenuProvider((id, inventory, player) ->
                ChestMenu.threeRows(id, inventory, player.getEnderChestInventory()), Component.translatable("container.enderchest")));
        });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.getInventory().clearContent();
                player.getEnderChestInventory().setItem(26, new ItemStack(Items.DIAMOND, 7));
                player.inventoryMenu.broadcastChanges();
            });
            world.getConnection().waitForClientboundPackets();
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitFor(client -> button(client).visible);
            context.runOnClient(EnderLinkClientGameTest::checkPosition);
            context.takeScreenshot("ender-link-inventory");
            hoverEnderChest(context);
            context.waitTick();
            context.takeScreenshot("ender-link-hovered");
            clickBook(context);
            context.waitTick();
            context.runOnClient(EnderLinkClientGameTest::checkPosition);
            context.takeScreenshot("ender-link-recipebook");
            if (!context.computeOnClient(client -> recipeBook(client).isVisible())) {
                clickBook(context);
                context.waitTick();
            }
            context.getInput().resizeWindow(640, 480);
            context.waitTick();
            context.runOnClient(client -> check(recipeBook(client).isVisible() && !button(client).visible, "Narrow recipe book hides inventory buttons"));
            context.getInput().resizeWindow(1280, 720);
            context.waitTick();
            context.runOnClient(client -> {
                check(button(client).visible, "Widening the window restores the button");
                checkPosition(client);
                client.player.containerMenu.setCarried(new ItemStack(Items.STONE));
            });
            context.waitTick();
            context.runOnClient(client -> check(!button(client).active, "Dragging items disables opening"));
            context.runOnClient(client -> client.player.containerMenu.setCarried(ItemStack.EMPTY));
            context.waitTick();
            clickEnderChest(context);
            context.waitFor(client -> client.gui.screen() instanceof ContainerScreen);
            world.getConnection().waitForServerboundPackets();
            context.runOnClient(client -> {
                check(requests.get() == 1, "One click sends one request");
                check(client.player.containerMenu.getSlot(26).getItem().getCount() == 7, "Native chest contents are synchronized");
                check(client.player.getInventory().isEmpty(), "No ender chest item is required");
            });
            context.takeScreenshot("ender-link-open");
        } finally {
            ServerPlayNetworking.unregisterGlobalReceiver(OpenEnderChest.TYPE.id());
        }
        try (var world = context.worldBuilder().create()) {
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitTick();
            context.runOnClient(client -> check(button(client).visible && !button(client).active, "Server support does not leak across connections"));
        }
    }

    private static EnderChestButton button(Minecraft client) {
        return (EnderChestButton) client.gui.screen().children().stream().filter(EnderChestButton.class::isInstance).findFirst().orElseThrow();
    }

    private static ImageButton book(Minecraft client) {
        return (ImageButton) client.gui.screen().children().stream().filter(ImageButton.class::isInstance).findFirst().orElseThrow();
    }

    private static RecipeBookComponent<?> recipeBook(Minecraft client) {
        return (RecipeBookComponent<?>) client.gui.screen().children().stream().filter(RecipeBookComponent.class::isInstance).findFirst().orElseThrow();
    }

    private static void checkPosition(Minecraft client) {
        check(button(client).getX() + 22 == book(client).getX() && button(client).getY() == book(client).getY(),
            "Ender button stays to the left of the recipe book");
    }

    private static void hoverEnderChest(ClientGameTestContext context) {
        double[] position = context.computeOnClient(client -> new double[] {(button(client).getX() + 10) * client.getWindow().getGuiScale(), (button(client).getY() + 9) * client.getWindow().getGuiScale()});
        context.getInput().setCursorPos(position[0], position[1]);
    }

    private static void clickEnderChest(ClientGameTestContext context) {
        hoverEnderChest(context);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void clickBook(ClientGameTestContext context) {
        double[] position = context.computeOnClient(client -> new double[] {(book(client).getX() + 10) * client.getWindow().getGuiScale(), (book(client).getY() + 9) * client.getWindow().getGuiScale()});
        context.getInput().setCursorPos(position[0], position[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
