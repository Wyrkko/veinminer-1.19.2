package com.jupkke.veinminer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: is this player's veinmine currently active? */
public final class ActivePacket {
    private final boolean active;

    public ActivePacket(boolean active) {
        this.active = active;
    }

    public static void encode(ActivePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static ActivePacket decode(FriendlyByteBuf buf) {
        return new ActivePacket(buf.readBoolean());
    }

    public static void handle(ActivePacket msg, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) VeinHandler.setActive(player, msg.active);
        });
        ctx.setPacketHandled(true);
    }
}
