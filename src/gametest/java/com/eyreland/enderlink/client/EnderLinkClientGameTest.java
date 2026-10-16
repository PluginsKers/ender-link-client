package com.eyreland.enderlink.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
//? if >=26.3 {
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
//?}
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
        context.runOnClient(client -> client.options.menuBackgroundBlurriness().set(0));
        context.getInput().resizeWindow(1280, 720);
        //? if >=26.3 {
        String address = System.getProperty("enderlink.test.address");
        if (address != null) {
            runExternalTest(context, address);
            return;
        }
        //?}
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
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().clearContent();
                player.getEnderChestInventory().setItem(26, new ItemStack(Items.DIAMOND, 7));
                player.inventoryMenu.broadcastChanges();
            });
            sync(context, world);
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
            sync(context, world);
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

    //? if >=26.3 {
    private static void runExternalTest(ClientGameTestContext context, String address) {
        check(address.matches("127\\.0\\.0\\.1:[0-9]{1,5}"), "External tests require an explicit 127.0.0.1 port");
        int port = Integer.parseInt(address.substring(address.indexOf(':') + 1));
        check(port > 0 && port <= 65535, "Invalid external test port");
        context.runOnClient(client -> check(client.getUser().getName().equals("EnderLinkTest"), "Use the dedicated EnderLinkTest actor"));
        try {
            connect(context, address);
            context.waitTicks(60);
            openExternalChest(context);
            if (context.computeOnClient(client -> !client.player.containerMenu.getSlot(0).getItem().isEmpty())) {
                ItemStack savedItem = context.computeOnClient(client -> client.player.containerMenu.getSlot(0).getItem().copy());
                check(isFixtureItem(savedItem), "Existing ender chest slot 0 must contain the fixture chestplate");
                int emptyInventorySlot = context.computeOnClient(client -> {
                    check(fixtureInventorySlot(client) == -1, "The fixture chestplate must be unique");
                    for (int i = 27; i < client.player.containerMenu.slots.size(); i++) {
                        if (client.player.containerMenu.getSlot(i).getItem().isEmpty()) return i;
                    }
                    throw new AssertionError("Fixture inventory needs an empty slot");
                });
                List<Tag> prepared = new ArrayList<>(context.computeOnClient(EnderLinkClientGameTest::snapshot));
                Tag saved = prepared.get(0);
                prepared.set(0, prepared.get(emptyInventorySlot));
                prepared.set(emptyInventorySlot, saved);
                moveSlot(context, 0, emptyInventorySlot, savedItem);
                waitForSnapshot(context, prepared);
            }
            context.runOnClient(client -> check(client.player.containerMenu.getSlot(0).getItem().isEmpty(), "Fixture ender chest slot 0 must be empty"));
            context.takeScreenshot("external-ender-link-ready");
            System.out.println("ENDER_LINK_FIXTURE_READY: give EnderLinkTest one enchanted diamond chestplate named Ecosystem 26.3 with damage 7 within 90 seconds");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
            context.waitFor(client -> {
                check(System.nanoTime() < deadline, "Timed out waiting for the external fixture item");
                return client.player != null && client.gui.screen() instanceof ContainerScreen
                        && fixtureInventorySlot(client) >= 27;
            }, ClientGameTestContext.NO_TIMEOUT);
            int destination = context.computeOnClient(EnderLinkClientGameTest::fixtureInventorySlot);
            List<Tag> initial = new ArrayList<>(context.computeOnClient(EnderLinkClientGameTest::snapshot));
            ItemStack item = context.computeOnClient(client -> client.player.containerMenu.getSlot(destination).getItem().copy());
            moveSlot(context, destination, 0, item);
            Tag emptySlot = initial.get(0);
            initial.set(0, initial.get(destination));
            initial.set(destination, emptySlot);
            reopenAndCheck(context, initial);
            context.takeScreenshot("external-ender-link-before");
            moveSlot(context, 0, destination, item);
            List<Tag> moved = new ArrayList<>(initial);
            moved.set(0, emptySlot);
            moved.set(destination, initial.get(0));
            reopenAndCheck(context, moved);
            context.takeScreenshot("external-ender-link-moved");
            disconnect(context);
            connect(context, address);
            openExternalChest(context);
            waitForSnapshot(context, moved);
            context.takeScreenshot("external-ender-link-reconnected");
            item = context.computeOnClient(client -> client.player.containerMenu.getSlot(destination).getItem().copy());
            moveSlot(context, destination, 0, item);
            reopenAndCheck(context, initial);
            context.takeScreenshot("external-ender-link-restored");
            System.out.println("ENDER_LINK_EXTERNAL_PASS: real server menu, item components, reconnect, and restored slots");
        } finally {
            disconnect(context);
        }
    }

    private static void connect(ClientGameTestContext context, String address) {
        context.runOnClient(client -> ConnectScreen.startConnecting(new TitleScreen(), client,
                ServerAddress.parseString(address), new ServerData("Ender Link fixture", address, ServerData.Type.OTHER), false, null));
        context.waitFor(client -> client.player != null && client.level != null && client.gui.screen() == null, 1200);
    }

    private static void disconnect(ClientGameTestContext context) {
        context.runOnClient(client -> client.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE));
        context.waitFor(client -> client.player == null && client.level == null);
        context.setScreen(TitleScreen::new);
    }

    private static void openExternalChest(ClientGameTestContext context) {
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitFor(client -> button(client).active);
        clickEnderChest(context);
        context.waitFor(client -> client.gui.screen() instanceof ContainerScreen);
        context.runOnClient(client -> {
            check(client.player.containerMenu instanceof ChestMenu chest && chest.getRowCount() == 3,
                    "The server must open a 27-slot ender chest");
            check(client.player.containerMenu.getCarried().isEmpty(), "The menu cursor must be empty");
        });
    }

    private static List<Tag> snapshot(Minecraft client) {
        // Registry holder instances change across connections; compare their serialized keys.
        var ops = client.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return client.player.containerMenu.slots.stream()
                .map(slot -> ItemStack.OPTIONAL_CODEC.encodeStart(ops, slot.getItem()).getOrThrow()).toList();
    }

    private static int fixtureInventorySlot(Minecraft client) {
        int result = -1;
        for (int i = 27; i < client.player.containerMenu.slots.size(); i++) {
            ItemStack item = client.player.containerMenu.getSlot(i).getItem();
            if (isFixtureItem(item)) {
                check(result == -1, "Fixture inventory must contain exactly one matching test item");
                result = i;
            }
        }
        return result;
    }

    private static boolean isFixtureItem(ItemStack item) {
        return item.is(Items.DIAMOND_CHESTPLATE) && item.getCount() == 1
                && item.has(DataComponents.CUSTOM_NAME)
                && item.get(DataComponents.CUSTOM_NAME).getString().equals("Ecosystem 26.3")
                && !item.getEnchantments().isEmpty() && item.getDamageValue() == 7;
    }

    private static void waitForSnapshot(ClientGameTestContext context, List<Tag> expected) {
        try {
            context.waitFor(client -> client.gui.screen() instanceof ContainerScreen
                    && client.player.containerMenu.getCarried().isEmpty()
                    && snapshot(client).equals(expected), 1200);
        } catch (AssertionError failure) {
            context.takeScreenshot("external-ender-link-mismatch");
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof ContainerScreen) {
                    List<Tag> actual = snapshot(client);
                    for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
                        if (!expected.get(i).equals(actual.get(i))) System.err.println("ENDER_LINK_MISMATCH slot=" + i
                                + " expected=" + expected.get(i) + " actual=" + actual.get(i));
                    }
                }
            });
            throw failure;
        }
    }

    private static void reopenAndCheck(ClientGameTestContext context, List<Tag> expected) {
        context.runOnClient(client -> client.player.closeContainer());
        context.waitFor(client -> !(client.gui.screen() instanceof ContainerScreen));
        openExternalChest(context);
        waitForSnapshot(context, expected);
    }

    private static void moveSlot(ClientGameTestContext context, int source, int destination, ItemStack item) {
        clickSlot(context, source);
        context.waitFor(client -> client.player.containerMenu.getSlot(source).getItem().isEmpty()
                && ItemStack.matches(client.player.containerMenu.getCarried(), item));
        clickSlot(context, destination);
        context.waitFor(client -> client.player.containerMenu.getCarried().isEmpty()
                && ItemStack.matches(client.player.containerMenu.getSlot(destination).getItem(), item));
    }

    private static void clickSlot(ClientGameTestContext context, int index) {
        double[] position = context.computeOnClient(client -> {
            ContainerScreen screen = (ContainerScreen) client.gui.screen();
            var slot = client.player.containerMenu.getSlot(index);
            // Vanilla ContainerScreen dimensions: 176 by 114 + 18 per row.
            int left = (screen.width - 176) / 2;
            int top = (screen.height - (114 + 18 * screen.getMenu().getRowCount())) / 2;
            return new double[] {(left + slot.x + 8) * client.getWindow().getGuiScale(),
                    (top + slot.y + 8) * client.getWindow().getGuiScale()};
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(6);
    }
    //?}

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

    private static void sync(ClientGameTestContext context, TestSingleplayerContext world) {
        //? if >=26.2 {
        world.getConnection().waitForServerboundPackets();
        world.getConnection().waitForClientboundPackets();
        //?} else {
        /*context.waitTicks(2);
        *///?}
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
