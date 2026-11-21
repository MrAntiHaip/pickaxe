package ru.storymod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.storymod.events.WorldEventDirector;
import ru.storymod.item.ModItems;
import ru.storymod.network.ModNetworking;
import ru.storymod.story.StoryProgressionManager;

public class StoryPickaxesMod implements ModInitializer {
    public static final String MOD_ID = "storypickaxes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Инициализация сюжетного мода Димы и Ульяны [Fabric 1.20.1]...");

        ModItems.registerModItems();
        ModNetworking.registerServerPackets();

        // При входе в мир выдаём ТОЛЬКО Невыбрасываемый Компас Судьбы!
        // Земляная кирка НЕ выдаётся на старте — её нужно заслужить в 1-й главе у Старейшины!
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ensurePlayerHasSoulboundCompass(player);
            StoryProgressionManager.syncToClient(player);
        });

        // Сохранение Компаса и Сюжетных Кирок при смерти
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            for (int i = 0; i < oldPlayer.getInventory().size(); i++) {
                ItemStack stack = oldPlayer.getInventory().getStack(i);
                if (stack.isOf(ModItems.STORY_COMPASS) || ModItems.isStoryPickaxe(stack.getItem())) {
                    newPlayer.getInventory().setStack(i, stack.copy());
                }
            }
            ensurePlayerHasSoulboundCompass(newPlayer);
        });

        // Отслеживание добычи 10 блоков земли голыми руками для 1-й Главы Сюжета
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                if (state.isOf(Blocks.DIRT) || state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.COARSE_DIRT)) {
                    StoryProgressionManager.onDirtMinedInPrologue(serverPlayer);
                }
                WorldEventDirector.onBlockBrokenDuringEvents(serverPlayer, pos, state);
            }
        });

        // Диалог со Старейшиной Шахт по ПКМ по Жителю
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof VillagerEntity villager) {
                if (villager.getCommandTags().contains("StoryElderVillager")) {
                    StoryProgressionManager.interactWithElder((ServerPlayerEntity) player, villager);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        // Серверный тик для кастомных мировых событий
        ServerTickEvents.END_SERVER_TICK.register(WorldEventDirector::tickEvents);
    }

    public static void ensurePlayerHasSoulboundCompass(ServerPlayerEntity player) {
        boolean hasCompass = false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(ModItems.STORY_COMPASS)) {
                hasCompass = true;
                break;
            }
        }
        if (!hasCompass) {
            ItemStack compass = new ItemStack(ModItems.STORY_COMPASS);
            player.getInventory().insertStack(0, compass);
            player.sendMessage(Text.literal("§d✦ [Легенда Глубин] §fВы получили §6Невыбрасываемый Компас Судьбы§f! Нажмите §eПКМ§f, чтобы выбрать роль (§bДима§f / §dУльяна§f) и нажать §a«НАЧАТЬ СЮЖЕТ»§f!"), false);
            player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.2f);
        }
    }
}
