package ru.storymod.events;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.storymod.story.StoryProgressionManager;

import java.util.List;
import java.util.Random;

public class WorldEventDirector {
    private static final Random RANDOM = new Random();
    private static int meteorShowerTicks = 0;
    private static int goldRushTicks = 0;
    private static ServerPlayerEntity activeEventHost = null;

    public static void triggerEvent(ServerPlayerEntity player, int eventId) {
        ServerWorld world = player.getServerWorld();
        activeEventHost = player;

        switch (eventId) {
            case 1 -> {
                // Событие 1: Караван Старейшины и Оборона Лагеря
                StoryProgressionManager.startStorylineAndSpawnElder(player);
                for (int i = 0; i < 4; i++) {
                    ZombieEntity scout = EntityType.ZOMBIE.create(world);
                    if (scout != null) {
                        scout.refreshPositionAndAngles(player.getX() + (RANDOM.nextDouble() - 0.5) * 10, player.getY(), player.getZ() + (RANDOM.nextDouble() - 0.5) * 10, 0, 0);
                        scout.setCustomName(Text.literal("§cРазведчик Глубин"));
                        scout.setCustomNameVisible(true);
                        world.spawnEntity(scout);
                    }
                }
            }
            case 2 -> {
                // Событие 2: Метеоритный Дождь Звёздных Руд
                meteorShowerTicks = 20 * 45;
                player.sendMessage(Text.literal("§6☄ [Событие Компаса] §eНачался МЕТЕОРИТНЫЙ ДОЖДЬ РУД! Вокруг вас падают кратеры с железом, золотом и алмазами!"), false);
                spawnOreMeteor(world, player.getBlockPos().add(4, 0, 4));
            }
            case 3 -> {
                // Событие 3: Нашествие Короля Глубинных Големов (Босс)
                IronGolemEntity boss = EntityType.IRON_GOLEM.create(world);
                if (boss != null) {
                    BlockPos pos = player.getBlockPos().add(5, 0, 5);
                    boss.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
                    boss.setCustomName(Text.literal("§4§l⚔ БОСС: Король Глубинных Големов §c[Уязвим к Киркам!]"));
                    boss.setCustomNameVisible(true);
                    if (boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) != null) {
                        boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(300.0);
                        boss.setHealth(300.0f);
                    }
                    boss.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 99999, 0));
                    boss.setTarget(player);
                    world.spawnEntity(boss);
                }
                player.sendMessage(Text.literal("§4⚔ [БОСС-СОБЫТИЕ] §cКороль Глубинных Големов пробудился! Используйте боевые способности ваших Сюжетных Кирок!"), false);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.0f, 0.8f);
            }
            case 4 -> {
                // Событие 4: Золотая Лихорадка Безумного Депа
                goldRushTicks = 20 * 90;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 90, 3));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 90, 2));
                player.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 64));
                player.getInventory().insertStack(new ItemStack(Items.IRON_INGOT, 16));
                player.sendMessage(Text.literal("§e🎰 [Событие Компаса] §6ЗОЛОТАЯ ЛИХОРАДКА ДЕПА! Все блоки сыплют ресурсы для ставок, +64 Булыжника и +16 Железа выданы!"), false);
            }
            case 5 -> {
                // Событие 5: Аномалия Гравитации и Замёрзшего Времени
                List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(player.getPos(), 40, 40, 40),
                        e -> e != player && !(e instanceof PlayerEntity));
                for (LivingEntity mob : mobs) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 160, 1));
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 10));
                    mob.setFrozenTicks(300);
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 300, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 300, 3));
                player.sendMessage(Text.literal("§b⏳ [Событие Компаса] §3Аномалия Времени и Гравитации! Все враги вокруг подняты в воздух и заморожены!"), false);
            }
            case 6 -> {
                // Событие 6: Испытание Дуэта: Симфония Димы и Ульяны
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 3));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 2));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 2));
                player.getInventory().insertStack(new ItemStack(Items.DIAMOND, 16));
                player.getInventory().insertStack(new ItemStack(Items.NETHERITE_INGOT, 2));
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 80, 0.8, 1.0, 0.8, 0.25);
                player.sendMessage(Text.literal("§d★ [Испытание Дуэта] §5Симфония Димы и Ульяны активирована! Мощь Разрушения и Звёздная Фортуна объединены!"), false);
            }
        }
    }

    public static void tickEvents(MinecraftServer server) {
        if (meteorShowerTicks > 0) {
            meteorShowerTicks--;
            if (meteorShowerTicks % 100 == 0 && activeEventHost != null && !activeEventHost.isDisconnected()) {
                BlockPos target = activeEventHost.getBlockPos().add(RANDOM.nextInt(16) - 8, 0, RANDOM.nextInt(16) - 8);
                spawnOreMeteor(activeEventHost.getServerWorld(), target);
            }
        }
        if (goldRushTicks > 0) {
            goldRushTicks--;
        }
    }

    public static void onBlockBrokenDuringEvents(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (goldRushTicks > 0) {
            player.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 4));
            if (RANDOM.nextInt(4) == 0) {
                player.getInventory().insertStack(new ItemStack(Items.IRON_INGOT, 1));
            }
        }
    }

    private static void spawnOreMeteor(ServerWorld world, BlockPos groundPos) {
        BlockPos impact = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, groundPos);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, impact.getX(), impact.getY() + 1, impact.getZ(), 2, 0, 0, 0, 0);
        world.playSound(null, impact, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 0.8f);

        BlockState[] ores = {
                Blocks.IRON_ORE.getDefaultState(),
                Blocks.GOLD_ORE.getDefaultState(),
                Blocks.DIAMOND_ORE.getDefaultState(),
                Blocks.AMETHYST_BLOCK.getDefaultState(),
                Blocks.OBSIDIAN.getDefaultState()
        };
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = impact.add(dx, dy, dz);
                    world.setBlockState(p, ores[RANDOM.nextInt(ores.length)]);
                }
            }
        }
    }
}
