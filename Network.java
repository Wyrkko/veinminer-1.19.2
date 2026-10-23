package com.jupkke.veinminer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(JupkkeVeinminer.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(v -> true)
            .serverAcceptedVersions(v -> true)
            .simpleChannel();

    private Network() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ActivePacket.class,
                ActivePacket::encode, ActivePacket::decode, ActivePacket::handle);
    }
}
