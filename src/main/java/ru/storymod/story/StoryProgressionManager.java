package ru.storymod.story;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import ru.storymod.item.ModItems;
import ru.storymod.item.StoryPickaxeItem;
import ru.storymod.network.ModNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StoryProgressionManager {
    private static final Map<UUID, String> PLAYER_ROLES = new HashMap<>();
    private static final Map<UUID, Integer> PLAYER_CHAPTER = new HashMap<>();
    private static final Map<UUID, Integer> PROLOGUE_DIRT_COUNT = new HashMap<>();
    private static final Map<UUID, Boolean> STORY_STARTED = new HashMap<>();

    public static void selectRole(ServerPlayerEntity player, String roleId) {
        PLAYER_ROLES.put(player.getUuid(), roleId);
        if ("DIMA".equalsIgnoreCase(roleId)) {
            player.sendMessage(Text.literal("§c⚔ [Роль Выбрана] §fВы играете за §c§lДИМУ§f! Бонус: §e+35% урона кирками по мобам§f и сокрушающие ударные волны!"), false);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 0, true, false));
        } else {
            player.sendMessage(Text.literal("§d✦ [Роль Выбрана] §fВы играете за §d§lУЛЬЯНУ§f! Бонус: §a+15% к удаче в Рулетке Депа§f, +1 Фортуна на руды и аура кристаллов!"), false);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 600, 1, true, false));
        }
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.9f, 1.3f);
        syncToClient(player);
    }

    public static boolean isDima(ServerPlayerEntity player) {
        return "DIMA".equalsIgnoreCase(PLAYER_ROLES.getOrDefault(player.getUuid(), "DIMA"));
    }

    public static boolean isUlyana(ServerPlayerEntity player) {
        return "ULYANA".equalsIgnoreCase(PLAYER_ROLES.getOrDefault(player.getUuid(), "NONE"));
    }

    /**
     * Нажатие кнопки «НАЧАТЬ СЮЖЕТ» в Компасе Судьбы!
     * Спавнит Жителя-Старейшину Валькора и запускает Главу 1 (без выдачи кирки заранее!).
     */
    public static void startStorylineAndSpawnElder(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!PLAYER_ROLES.containsKey(uuid)) {
            PLAYER_ROLES.put(uuid, "DIMA");
        }
        STORY_STARTED.put(uuid, true);
        if (PLAYER_CHAPTER.getOrDefault(uuid, 0) == 0) {
            PLAYER_CHAPTER.put(uuid, 1);
        }

        ServerWorld world = player.getServerWorld();
        BlockPos spawnPos = player.getBlockPos().add(player.getHorizontalFacing().getOffsetX() * 2, 0, player.getHorizontalFacing().getOffsetZ() * 2);

        VillagerEntity elder = EntityType.VILLAGER.create(world);
        if (elder != null) {
            elder.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYaw() + 180, 0);
            elder.setCustomName(Text.literal("§6§l✦ Старейшина Шахт Валькор §e[ПКМ — Сюжет]"));
            elder.setCustomNameVisible(true);
            elder.setInvulnerable(true);
            elder.setPersistent();
            elder.setVillagerData(elder.getVillagerData().withProfession(VillagerProfession.TOOLSMITH).withLevel(5));
            elder.addCommandTag("StoryElderVillager");
            elder.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 999999, 0, false, false));
            world.spawnEntity(elder);

            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, elder.getX(), elder.getY() + 1.2, elder.getZ(), 45, 0.5, 0.8, 0.5, 0.15);
            world.playSound(null, spawnPos, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        }

        player.sendMessage(Text.literal("§6============================================="), false);
        player.sendMessage(Text.literal("§e✦ СЮЖЕТ ЗАПУЩЕН! ГЛАВА 1: ПРОБУЖДЕНИЕ ЗЕМЛИ"), false);
        player.sendMessage(Text.literal("§6[Старейшина Валькор]: §f«Приветствую, Дима и Ульяна! Как видите, на старте у вас НЕТ кирки! Первую реликвию нужно заслужить!»"), false);
        player.sendMessage(Text.literal("§a➤ Задание Главы 1: §fДобудьте §e10 блоков земли§f голыми руками и нажмите §eПКМ по Старейшине§f (или кнопку сдачи в Компасе), чтобы получить §aЗемляную Кирку Пробуждения§f!"), false);
        player.sendMessage(Text.literal("§6============================================="), false);
        syncToClient(player);
    }

    public static void onDirtMinedInPrologue(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!STORY_STARTED.getOrDefault(uuid, false)) return;
        int chapter = PLAYER_CHAPTER.getOrDefault(uuid, 1);
        if (chapter == 1) {
            int count = PROLOGUE_DIRT_COUNT.getOrDefault(uuid, 0) + 1;
            PROLOGUE_DIRT_COUNT.put(uuid, count);
            if (count <= 10) {
                player.sendMessage(Text.literal("§a[Сюжет Гл. 1] §fДобыто земли руками: §e" + count + "/10"), true);
            }
            if (count == 10) {
                player.sendMessage(Text.literal("§a✦ Вы добыли 10 блоков земли! Нажмите ПКМ по Старейшине Валькору или откройте Компас, чтобы получить Земляную Кирку Пробуждения!"), false);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
            }
            syncToClient(player);
        }
    }

    public static void interactWithElder(ServerPlayerEntity player, VillagerEntity elder) {
        advanceChapterOrUpgradePickaxe(player);
    }

    /**
     * Выполнение текущей главы сюжета и получение / апгрейд кирки (от 1-й до 10-й!).
     */
    public static void advanceChapterOrUpgradePickaxe(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!STORY_STARTED.getOrDefault(uuid, false)) {
            startStorylineAndSpawnElder(player);
            return;
        }

        int chapter = PLAYER_CHAPTER.getOrDefault(uuid, 1);

        if (chapter == 1) {
            int dirtMined = PROLOGUE_DIRT_COUNT.getOrDefault(uuid, 0);
            int dirtInInv = countItem(player, Items.DIRT) + countItem(player, Items.GRASS_BLOCK);
            if (dirtMined < 10 && dirtInInv < 10) {
                player.sendMessage(Text.literal("§c[Старейшина Валькор]: §f«Сначала добудьте 10 блоков земли руками! Сейчас добыто: §e" + Math.max(dirtMined, dirtInInv) + "/10§f»"), false);
                return;
            }
            consumeItem(player, Items.DIRT, 10);
            // Выдаём 1-ю Земляную Кирку Пробуждения ПО СЮЖЕТУ!
            player.getInventory().insertStack(new ItemStack(ModItems.TIER_1_DIRT));
            player.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 64));
            PLAYER_CHAPTER.put(uuid, 2);
            player.sendMessage(Text.literal("§a§l★ ГЛАВА 1 ЗАВЕРШЕНА! ★ §fСтарейшина Валькор вручил вам §aЗемляную Кирку Пробуждения (Тир 1)§f и §764 Булыжника§f для первого Депа в Рулетку!"), false);
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
            syncToClient(player);
            return;
        }

        if (chapter > 10) {
            player.sendMessage(Text.literal("§6✦ Вы уже прошли все 10 глав сюжета и создали Абсолютный Мирокрушитель Димы и Ульяны (13×13×13)!"), false);
            return;
        }

        // Проверка ресурсов для апгрейда со 2-й по 10-ю кирку
        Item requiredItem = getChapterUpgradeMaterial(chapter);
        int requiredCount = getChapterUpgradeAmount(chapter);

        if (countItem(player, requiredItem) < requiredCount) {
            player.sendMessage(Text.literal("§e[Старейшина Валькор]: §f«Для пробуждения Кирки §6Тира " + chapter + "§f нужно: §b"
                    + requiredCount + "x " + requiredItem.getName().getString() + "§f! Накопайте их киркой или выиграйте в Рулетке Компаса!»"), false);
            return;
        }

        consumeItem(player, requiredItem, requiredCount);
        replaceOrGivePickaxe(player, chapter);
        PLAYER_CHAPTER.put(uuid, chapter + 1);

        StoryPickaxeItem newPick = ModItems.getPickaxeByTier(chapter);
        player.sendMessage(Text.literal("§6§l⚡ АПГРЕЙД СЮЖЕТА (ГЛАВА " + chapter + ")! ⚡ §fВы получили: " + newPick.getName().getString()
                + " §e(Масштаб копания до " + newPick.getMaxAreaSize() + "×" + newPick.getMaxAreaSize() + "!)"), false);
        player.getServerWorld().spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 0.8, 0.6, 0.2);
        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.15f);
        syncToClient(player);
    }

    private static void replaceOrGivePickaxe(ServerPlayerEntity player, int newTier) {
        StoryPickaxeItem targetItem = ModItems.getPickaxeByTier(newTier);
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() instanceof StoryPickaxeItem existing && existing.getTierLevel() == newTier - 1) {
                player.getInventory().setStack(i, new ItemStack(targetItem));
                return;
            }
        }
        player.getInventory().insertStack(new ItemStack(targetItem));
    }

    private static Item getChapterUpgradeMaterial(int chapter) {
        return switch (chapter) {
            case 2 -> Items.COBBLESTONE;
            case 3 -> Items.IRON_INGOT;
            case 4 -> Items.GOLD_INGOT;
            case 5 -> Items.GOLD_INGOT;
            case 6 -> Items.DIAMOND;
            case 7 -> Items.DIAMOND;
            case 8 -> Items.NETHERITE_INGOT;
            case 9 -> Items.NETHERITE_INGOT;
            default -> Items.NETHERITE_INGOT;
        };
    }

    private static int getChapterUpgradeAmount(int chapter) {
        return switch (chapter) {
            case 2 -> 64;
            case 3 -> 16;
            case 4 -> 16;
            case 5 -> 24;
            case 6 -> 12;
            case 7 -> 24;
            case 8 -> 2;
            case 9 -> 4;
            default -> 4;
        };
    }

    public static int countItem(ServerPlayerEntity player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(item)) total += s.getCount();
        }
        return total;
    }

    public static boolean consumeItem(ServerPlayerEntity player, Item item, int amount) {
        if (countItem(player, item) < amount) return false;
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(item)) {
                int take = Math.min(remaining, s.getCount());
                s.decrement(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        return true;
    }

    public static void syncToClient(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(PLAYER_ROLES.getOrDefault(uuid, "NONE"));
        buf.writeInt(PLAYER_CHAPTER.getOrDefault(uuid, 0));
        buf.writeInt(PROLOGUE_DIRT_COUNT.getOrDefault(uuid, 0));
        buf.writeBoolean(STORY_STARTED.getOrDefault(uuid, false));
        ServerPlayNetworking.send(player, ModNetworking.SYNC_STORY_S2C, buf);
    }

    public static String getRole(ServerPlayerEntity player) {
        return PLAYER_ROLES.getOrDefault(player.getUuid(), "NONE");
    }

    public static int getChapter(ServerPlayerEntity player) {
        return PLAYER_CHAPTER.getOrDefault(player.getUuid(), 0);
    }

    public static int getDirtMined(ServerPlayerEntity player) {
        return PROLOGUE_DIRT_COUNT.getOrDefault(player.getUuid(), 0);
    }

    public static boolean isStoryStarted(ServerPlayerEntity player) {
        return STORY_STARTED.getOrDefault(player.getUuid(), false);
    }
}
