package com.eyreland.enderlink.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class EnderLinkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.serverboundPlay().register(OpenEnderChest.TYPE, OpenEnderChest.CODEC);
    }
}
