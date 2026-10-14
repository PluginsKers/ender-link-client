package com.eyreland.enderlink.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenEnderChest() implements CustomPacketPayload {
    public static final OpenEnderChest INSTANCE = new OpenEnderChest();
    public static final Type<OpenEnderChest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("enderlink", "open_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEnderChest> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenEnderChest> type() {
        return TYPE;
    }
}
