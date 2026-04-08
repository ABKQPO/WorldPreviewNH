package com.hfstudio.preview.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Packet sent from server to client on login, carrying the world seed,
 * world type name, and generator options so the client can preview the map
 * without entering the world's chunks.
 */
public class SeedDataMessage implements IMessage {

    private long seed;
    private String worldTypeName;
    private String generatorOptions;

    public SeedDataMessage() {}

    public SeedDataMessage(long seed, String worldTypeName, String generatorOptions) {
        this.seed = seed;
        this.worldTypeName = worldTypeName != null ? worldTypeName : "DEFAULT";
        this.generatorOptions = generatorOptions != null ? generatorOptions : "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.seed = buf.readLong();
        this.worldTypeName = ByteBufUtils.readUTF8String(buf);
        this.generatorOptions = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(seed);
        ByteBufUtils.writeUTF8String(buf, worldTypeName);
        ByteBufUtils.writeUTF8String(buf, generatorOptions);
    }

    public long getSeed() {
        return seed;
    }

    public String getWorldTypeName() {
        return worldTypeName;
    }

    public String getGeneratorOptions() {
        return generatorOptions;
    }

    /**
     * Client-side handler: stores received seed data so the preview button
     * can appear in the in-game menu for multiplayer.
     */
    public static class Handler implements IMessageHandler<SeedDataMessage, IMessage> {

        @Override
        public IMessage onMessage(SeedDataMessage message, MessageContext ctx) {
            ServerSeedData.set(message.getSeed(), message.getWorldTypeName(), message.getGeneratorOptions());
            return null;
        }
    }
}
