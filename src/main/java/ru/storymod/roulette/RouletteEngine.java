package ru.storymod.roulette;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import ru.storymod.story.StoryProgressionManager;

import java.util.Random;

public class RouletteEngine {
    private static final Random RANDOM = new Random();

    public static void spinRouletteTier(ServerPlayerEntity player, int tier) {
        Item costItem;
        int costAmount;
        int noobChance; // В каждой рулетке есть шанс на нубский ресурс, но с каждым уровнем он меньше!

        switch (tier) {
            case 1 -> {
                costItem = Items.COBBLESTONE;
                costAmount = 64;
                noobChance = 55;
            }
            case 2 -> {
                costItem = Items.IRON_INGOT;
                costAmount = 16;
                noobChance = 40;
            }
            case 3 -> {
                costItem = Items.GOLD_INGOT;
                costAmount = 16;
                noobChance = 28;
            }
            case 4 -> {
                costItem = Items.DIAMOND;
                costAmount = 8;
                noobChance = 18;
            }
            case 5 -> {
                costItem = Items.NETHERITE_INGOT;
                costAmount = 2;
                noobChance = 10;
            }
            default -> {
                return;
            }
        }

        // Бонус роли Ульяны: снижает шанс на нубский дроп на 10%!
        if (StoryProgressionManager.isUlyana(player)) {
            noobChance = Math.max(5, noobChance - 10);
        }

        if (!StoryProgressionManager.consumeItem(player, costItem, costAmount)) {
            player.sendMessage(Text.literal("§c🎰 [Рулетка Депа] §fНедостаточно ресурсов для ставки! Нужно: §e"
                    + costAmount + "x " + costItem.getName().getString()), false);
            player.playSound(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.8f, 1.0f);
            return;
        }

        int roll = RANDOM.nextInt(100);
        if (roll < noobChance) {
            // Выпал НУБСКИЙ РЕСУРС (есть на любом уровне рулетки!)
            ItemStack[] noobDrops = {
                    new ItemStack(Items.ROTTEN_FLESH, 1),
                    new ItemStack(Items.DIRT, 2),
                    new ItemStack(Items.DEAD_BUSH, 1),
                    new ItemStack(Items.POISONOUS_POTATO, 1),
                    new ItemStack(Items.WOODEN_HOE, 1)
            };
            ItemStack noobPrize = noobDrops[RANDOM.nextInt(noobDrops.length)];
            player.getInventory().insertStack(noobPrize);

            player.sendMessage(Text.literal("§c§l🎰 [НУБ-ДРОП РУЛЕТКИ!] §7Вы депнули §f" + costAmount + "x " + costItem.getName().getString()
                    + "§7, а выиграли... §c1x " + noobPrize.getName().getString() + "§7! Ха-ха!"), false);
            player.playSound(SoundEvents.ENTITY_VILLAGER_HURT, SoundCategory.PLAYERS, 0.9f, 0.7f);

            // Пассивный бонус Димы: от злости возвращает 16 булыжника при нуб-дропе
            if (StoryProgressionManager.isDima(player)) {
                player.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 16));
                player.sendMessage(Text.literal("§c[Бонус Димы]: §7От ярости вы выбили §f16x Булыжника§7 обратно из автомата!"), true);
            }
            return;
        }

        // Выигрыш окупа или Джекпота (чем выше уровень рулетки — тем круче награда!)
        boolean isJackpot = roll >= 78;
        ItemStack reward;
        if (tier == 1) {
            reward = isJackpot ? new ItemStack(Items.DIAMOND, 3) : new ItemStack(Items.IRON_INGOT, 16);
        } else if (tier == 2) {
            reward = isJackpot ? new ItemStack(Items.DIAMOND, 10) : new ItemStack(Items.GOLD_INGOT, 20);
        } else if (tier == 3) {
            reward = isJackpot ? new ItemStack(Items.NETHERITE_INGOT, 2) : new ItemStack(Items.DIAMOND, 12);
        } else if (tier == 4) {
            reward = isJackpot ? new ItemStack(Items.NETHERITE_INGOT, 5) : new ItemStack(Items.DIAMOND, 24);
        } else {
            reward = isJackpot ? new ItemStack(Items.NETHERITE_INGOT, 10) : new ItemStack(Items.NETHERITE_INGOT, 6);
        }

        player.getInventory().insertStack(reward);
        if (isJackpot) {
            player.sendMessage(Text.literal("§6§l🎰 ДЖЕКПОТ РУЛЕТКИ! §eВы сорвали куш: §b§l"
                    + reward.getCount() + "x " + reward.getName().getString() + "§e!"), false);
            player.getServerWorld().spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 0.8, 0.5, 0.2);
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.3f);
        } else {
            player.sendMessage(Text.literal("§a🎰 [Окуп в Рулетке!] §fВы выиграли: §e"
                    + reward.getCount() + "x " + reward.getName().getString() + "§f!"), false);
            player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.9f, 1.5f);
        }
    }
}
