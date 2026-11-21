package ru.storymod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import ru.storymod.client.gui.CompassStoryScreen;
import ru.storymod.network.ModNetworking;

public class StoryPickaxesClient implements ClientModInitializer {
    public static String clientRole = "NONE";
    public static int clientChapter = 0;
    public static int clientDirtMined = 0;
    public static boolean clientStoryStarted = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_COMPASS_SCREEN_S2C, (client, handler, buf, responseSender) -> {
            String role = buf.readString();
            int chapter = buf.readInt();
            int dirtMined = buf.readInt();
            boolean storyStarted = buf.readBoolean();

            client.execute(() -> {
                clientRole = role;
                clientChapter = chapter;
                clientDirtMined = dirtMined;
                clientStoryStarted = storyStarted;
                MinecraftClient.getInstance().setScreen(new CompassStoryScreen());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SYNC_STORY_S2C, (client, handler, buf, responseSender) -> {
            String role = buf.readString();
            int chapter = buf.readInt();
            int dirtMined = buf.readInt();
            boolean storyStarted = buf.readBoolean();

            client.execute(() -> {
                clientRole = role;
                clientChapter = chapter;
                clientDirtMined = dirtMined;
                clientStoryStarted = storyStarted;
            });
        });
    }
}
