package ru.storymod.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.storymod.StoryPickaxesMod;
import ru.storymod.events.WorldEventDirector;
import ru.storymod.roulette.RouletteEngine;
import ru.storymod.story.StoryProgressionManager;

public class ModNetworking {
    public static final Identifier OPEN_COMPASS_SCREEN_S2C = new Identifier(StoryPickaxesMod.MOD_ID, "open_compass_screen");
    public static final Identifier SYNC_STORY_S2C = new Identifier(StoryPickaxesMod.MOD_ID, "sync_story");
    public static final Identifier COMPASS_ACTION_C2S = new Identifier(StoryPickaxesMod.MOD_ID, "compass_action");

    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(COMPASS_ACTION_C2S, (server, player, handler, buf, responseSender) -> {
            String actionType = buf.readString();
            int param = buf.readInt();

            server.execute(() -> {
                switch (actionType) {
                    case "START_STORY" -> StoryProgressionManager.startStorylineAndSpawnElder(player);
                    case "ADVANCE_CHAPTER" -> StoryProgressionManager.advanceChapterOrUpgradePickaxe(player);
                    case "SELECT_ROLE_DIMA" -> StoryProgressionManager.selectRole(player, "DIMA");
                    case "SELECT_ROLE_ULYANA" -> StoryProgressionManager.selectRole(player, "ULYANA");
                    case "SPIN_ROULETTE" -> RouletteEngine.spinRouletteTier(player, param);
                    case "TRIGGER_EVENT" -> WorldEventDirector.triggerEvent(player, param);
                }
            });
        });
    }

    public static void openCompassScreen(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(StoryProgressionManager.getRole(player));
        buf.writeInt(StoryProgressionManager.getChapter(player));
        buf.writeInt(StoryProgressionManager.getDirtMined(player));
        buf.writeBoolean(StoryProgressionManager.isStoryStarted(player));
        ServerPlayNetworking.send(player, OPEN_COMPASS_SCREEN_S2C, buf);
    }
}
