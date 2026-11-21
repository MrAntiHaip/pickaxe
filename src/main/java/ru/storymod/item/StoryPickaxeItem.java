package ru.storymod.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import ru.storymod.particles.ParticleEffectEngine;
import ru.storymod.story.StoryProgressionManager;

import java.util.List;

public class StoryPickaxeItem extends PickaxeItem {
    private final int tierLevel;
    private final String displayTitle;
    private final int maxAreaSize; // 1, 3, 5, 7, 9, 11, 13
    private final int maxDepth;
    private final float customMiningSpeed;

    public StoryPickaxeItem(ToolMaterial material, int attackDamage, float attackSpeed,
                            int tierLevel, String displayTitle, int maxAreaSize, int maxDepth,
                            float customMiningSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
        this.tierLevel = tierLevel;
        this.displayTitle = displayTitle;
        this.maxAreaSize = maxAreaSize;
        this.maxDepth = maxDepth;
        this.customMiningSpeed = customMiningSpeed;
    }

    public int getTierLevel() {
        return tierLevel;
    }

    public int getMaxAreaSize() {
        return maxAreaSize;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return customMiningSpeed;
    }

    @Override
    public boolean isDamageable() {
        return false; // Все сюжетные реликвии неразрушимы!
    }

    /**
     * Переключение масштаба копания по Shift + ПКМ или активация боевой ульты по ПКМ!
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer && world instanceof ServerWorld serverWorld) {
            if (user.isSneaking() && maxAreaSize > 1) {
                int currentSize = getActiveAreaSize(stack);
                int nextSize = cycleAreaSize(currentSize, maxAreaSize);
                stack.getOrCreateNbt().putInt("ActiveAreaSize", nextSize);
                serverPlayer.sendMessage(Text.literal("§e⛏ [Режим Бура] §fМасштаб копания переключён на: §6§l" + nextSize + "×" + nextSize + "×" + Math.min(nextSize, maxDepth)), true);
                serverWorld.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.4f);
                return TypedActionResult.success(stack);
            } else {
                // Активная боевая способность против мобов вокруг!
                triggerActiveMobSkill(serverWorld, serverPlayer, stack);
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(stack);
    }

    private int getActiveAreaSize(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("ActiveAreaSize")) {
            return nbt.getInt("ActiveAreaSize");
        }
        return maxAreaSize;
    }

    private int cycleAreaSize(int current, int max) {
        int[] steps = {1, 3, 5, 7, 9, 11, 13};
        for (int s : steps) {
            if (s > current && s <= max) return s;
        }
        return 1;
    }

    /**
     * БОЕВЫЕ СПОСОБНОСТИ ПРОТИВ МОБОВ ПРИ УДАРЕ (postHit)
     */
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient && attacker instanceof ServerPlayerEntity player && attacker.getWorld() instanceof ServerWorld serverWorld) {
            float roleMultiplier = StoryProgressionManager.isDima(player) ? 1.35f : 1.15f;
            float bonusDamage = (tierLevel * 4.5f) * roleMultiplier;
            target.damage(serverWorld.getDamageSources().playerAttack(player), bonusDamage);

            // Отрисовка 3D-частиц удара в зависимости от уровня кирки
            ParticleEffectEngine.spawnTierEffect(serverWorld, target.getPos().add(0, 1.0, 0), player, tierLevel);

            switch (tierLevel) {
                case 1 -> {
                    // Земляная кирка: Корневой захват + Яд спор
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 3));
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 1));
                    target.setVelocity(0, 0.45, 0);
                }
                case 2 -> {
                    // Окаменелая кирка: Сейсмический нокаут по площади
                    knockbackNearbyMobs(serverWorld, player, target.getPos(), 4.5, 0.75, 8.0f);
                }
                case 3 -> {
                    // Магмовый крушитель: Поджог + Лавовый взрыв
                    target.setOnFireFor(7);
                    igniteNearbyMobs(serverWorld, player, target.getPos(), 5.5, 12.0f);
                }
                case 4 -> {
                    // Кристальная кирка: Звуковой импульс Вардена насквозь
                    ParticleEffectEngine.spawnSonicBeam(serverWorld, player.getEyePos(), target.getEyePos());
                    target.damage(serverWorld.getDamageSources().sonicBoom(player), 18.0f * roleMultiplier);
                }
                case 5 -> {
                    // Громовая Валькирия: Цепная молния Зевса по 6 мобам
                    chainLightningStrike(serverWorld, player, target, 6, 22.0f * roleMultiplier);
                }
                case 6 -> {
                    // Кроваво-Рубиновый Пожиратель: Вампиризм + Стягивание мобов
                    player.heal(8.0f);
                    pullMobsToCenter(serverWorld, player, target.getPos(), 8.0, 26.0f * roleMultiplier);
                }
                case 7 -> {
                    // Кирка Ледяного Хроноса: Остановка времени (Крио-Стазис)
                    freezeMobsInRadius(serverWorld, player, target.getPos(), 11.0, 34.0f * roleMultiplier);
                }
                case 8 -> {
                    // Пустотная Кирка Разрыва: Гравитационная Чёрная Дыра
                    triggerBlackHoleSingularity(serverWorld, player, target.getPos(), 13.0, 44.0f * roleMultiplier);
                }
                case 9 -> {
                    // Звёздный Рассекатель Небулы: Звездопад Сверхновой
                    triggerSupernovaStarfall(serverWorld, player, target.getPos(), 16.0, 60.0f * roleMultiplier);
                }
                case 10 -> {
                    // АБСОЛЮТНЫЙ МИРОКРУШИТЕЛЬ ДИМЫ И УЛЬЯНЫ: Судный Катаклизм (Аннигиляция)
                    triggerDuetWorldbreakerCataclysm(serverWorld, player, target.getPos());
                }
            }
        }
        return true;
    }

    /**
     * МАСШТАБНОЕ 3D-КОПАНИЕ ОТ 1x1 ДО 13x13x13 (2197 БЛОКОВ ЗА ВЗМАХ!)
     */
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && miner instanceof ServerPlayerEntity player && world instanceof ServerWorld serverWorld) {
            ParticleEffectEngine.spawnTierEffect(serverWorld, Vec3d.ofCenter(pos), player, tierLevel);

            int areaSize = getActiveAreaSize(stack);
            int half = (areaSize - 1) / 2;
            int depth = Math.min(areaSize, maxDepth);
            Direction facing = player.getHorizontalFacing();
            float pitch = player.getPitch();

            for (int dx = -half; dx <= half; dx++) {
                for (int dy = -half; dy <= half; dy++) {
                    for (int dz = 0; dz < depth; dz++) {
                        BlockPos targetPos;
                        if (Math.abs(pitch) > 60) {
                            int yDir = pitch > 0 ? -dz : dz;
                            targetPos = pos.add(dx, yDir, dy);
                        } else {
                            targetPos = pos.add(
                                    facing.getOffsetX() * dz + facing.RotateYClockwise().getOffsetX() * dx,
                                    dy,
                                    facing.getOffsetZ() * dz + facing.RotateYClockwise().getOffsetZ() * dx
                            );
                        }

                        if (targetPos.equals(pos)) {
                            processSpecialBlockRewards(serverWorld, player, pos, state);
                            continue;
                        }

                        BlockState targetState = serverWorld.getBlockState(targetPos);
                        if (canStoryPickaxeBreak(targetState)) {
                            processSpecialBlockRewards(serverWorld, player, targetPos, targetState);
                            // Для радиуса 9x9..13x13 отключаем ванильные частицы каждого блока, чтобы было 0 лагов,
                            // и рисуем наши дизайнерские 3D-частицы!
                            boolean dropVanilla = tierLevel < 3;
                            serverWorld.breakBlock(targetPos, dropVanilla, player);
                        }
                    }
                }
            }

            // На уровне 7+ замораживаем опасную лаву вокруг раскопок в обсидиан!
            if (tierLevel >= 7) {
                freezeAdjacentLava(serverWorld, pos, half + 1);
            }
        }
        return true;
    }

    private boolean canStoryPickaxeBreak(BlockState state) {
        if (state.isAir() || state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.END_PORTAL_FRAME)
                || state.isOf(Blocks.COMMAND_BLOCK) || state.isOf(Blocks.BARRIER) || state.isOf(Blocks.SPAWNER)) {
            return false;
        }
        return state.getHardness(null, BlockPos.ORIGIN) >= 0;
    }

    /**
     * Авто-Плавка (Тир 3+), Вакуумный Магнит (Тир 4+) и Множитель Удачи (до x5 на 10-й кирке!)
     */
    private void processSpecialBlockRewards(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (tierLevel < 3) return;

        int fortuneMult = tierLevel >= 10 ? 5 : (tierLevel >= 8 ? 3 : (tierLevel >= 6 ? 2 : 1));
        if (StoryProgressionManager.isUlyana(player)) {
            fortuneMult += 1; // Бонус роли Ульяны!
        }

        Block block = state.getBlock();
        ItemStack resultStack = ItemStack.EMPTY;

        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            resultStack = new ItemStack(Items.IRON_INGOT, fortuneMult);
            world.spawnEntity(new ExperienceOrbEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2));
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            resultStack = new ItemStack(Items.GOLD_INGOT, fortuneMult);
            world.spawnEntity(new ExperienceOrbEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3));
        } else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            resultStack = new ItemStack(Items.COPPER_INGOT, fortuneMult * 2);
        } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            resultStack = new ItemStack(Items.DIAMOND, fortuneMult);
            world.spawnEntity(new ExperienceOrbEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5));
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            resultStack = new ItemStack(Items.EMERALD, fortuneMult);
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            resultStack = new ItemStack(Items.NETHERITE_SCRAP, fortuneMult);
        } else if (block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.DEEPSLATE) {
            resultStack = new ItemStack(Items.COBBLESTONE, 1);
        } else {
            List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, world.getBlockEntity(pos), player, player.getMainHandStack());
            for (ItemStack d : drops) {
                giveOrDrop(world, player, pos, d);
            }
            return;
        }

        if (!resultStack.isEmpty()) {
            giveOrDrop(world, player, pos, resultStack);
        }
    }

    private void giveOrDrop(ServerWorld world, ServerPlayerEntity player, BlockPos pos, ItemStack stack) {
        if (tierLevel >= 4) {
            // Вакуумный магнит: сразу в инвентарь игрока!
            if (!player.getInventory().insertStack(stack)) {
                Block.dropStack(world, player.getBlockPos(), stack);
            }
        } else {
            Block.dropStack(world, pos, stack);
        }
    }

    private void freezeAdjacentLava(ServerWorld world, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.add(x, y, z);
                    if (world.getBlockState(p).isOf(Blocks.LAVA)) {
                        world.setBlockState(p, Blocks.OBSIDIAN.getDefaultState());
                    }
                }
            }
        }
    }

    // ======== ВСПОМОГАТЕЛЬНЫЕ БОЕВЫЕ МЕТОДЫ ПРОТИВ МОБОВ ========

    private void triggerActiveMobSkill(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
        if (player.getItemCooldownManager().isCoolingDown(this)) return;
        player.getItemCooldownManager().set(this, 60); // 3 сек КД

        Vec3d center = player.getPos().add(player.getRotationVector().multiply(3.0));
        ParticleEffectEngine.spawnTierEffect(world, center, player, tierLevel);

        if (tierLevel >= 10) {
            triggerDuetWorldbreakerCataclysm(world, player, player.getPos());
        } else if (tierLevel >= 8) {
            triggerBlackHoleSingularity(world, player, center, 14.0, 50.0f);
        } else if (tierLevel >= 6) {
            freezeMobsInRadius(world, player, player.getPos(), 12.0, 35.0f);
        } else {
            knockbackNearbyMobs(world, player, player.getPos(), 7.0, 0.9, 16.0f);
        }
    }

    private void knockbackNearbyMobs(ServerWorld world, ServerPlayerEntity player, Vec3d center, double radius, double lift, float dmg) {
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, radius * 2, radius * 2),
                e -> e != player && !(e instanceof PlayerEntity));
        for (LivingEntity mob : mobs) {
            mob.damage(world.getDamageSources().playerAttack(player), dmg);
            Vec3d dir = mob.getPos().subtract(player.getPos()).normalize();
            mob.setVelocity(dir.x * 1.1, lift, dir.z * 1.1);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1));
        }
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.7f, 1.3f);
    }

    private void igniteNearbyMobs(ServerWorld world, ServerPlayerEntity player, Vec3d center, double radius, float dmg) {
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, radius * 2, radius * 2),
                e -> e != player && !(e instanceof PlayerEntity));
        for (LivingEntity mob : mobs) {
            mob.setOnFireFor(8);
            mob.damage(world.getDamageSources().onFire(), dmg);
        }
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 0.9f);
    }

    private void chainLightningStrike(ServerWorld world, ServerPlayerEntity player, LivingEntity primary, int maxTargets, float dmg) {
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, primary.getBoundingBox().expand(10.0),
                e -> e != player && !(e instanceof PlayerEntity));
        int count = 0;
        Vec3d prevPos = player.getEyePos();
        for (LivingEntity mob : mobs) {
            if (count++ >= maxTargets) break;
            ParticleEffectEngine.spawnLightningArc(world, prevPos, mob.getEyePos());
            prevPos = mob.getEyePos();
            mob.damage(world.getDamageSources().lightningBolt(), dmg);

            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
            if (bolt != null) {
                bolt.refreshPositionAfterTeleport(mob.getPos());
                bolt.setCosmetic(true); // Не сжигает выпавший лут!
                world.spawnEntity(bolt);
            }
        }
    }

    private void pullMobsToCenter(ServerWorld world, ServerPlayerEntity player, Vec3d center, double radius, float dmg) {
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, radius * 2, radius * 2),
                e -> e != player && !(e instanceof PlayerEntity));
        for (LivingEntity mob : mobs) {
            Vec3d pull = center.subtract(mob.getPos()).multiply(0.35);
            mob.setVelocity(pull.x, 0.35, pull.z);
            mob.damage(world.getDamageSources().magic(), dmg);
        }
    }

    private void freezeMobsInRadius(ServerWorld world, ServerPlayerEntity player, Vec3d center, double radius, float dmg) {
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, radius * 2, radius * 2),
                e -> e != player && !(e instanceof PlayerEntity));
        for (LivingEntity mob : mobs) {
            mob.setFrozenTicks(300);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 10));
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 5));
            mob.damage(world.getDamageSources().freeze(), dmg);
        }
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.6f);
    }

    private void triggerBlackHoleSingularity(ServerWorld world, ServerPlayerEntity player, Vec3d center, double radius, float dmg) {
        ParticleEffectEngine.spawnBlackHoleTorus(world, center.add(0, 1.5, 0));
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, radius * 2, radius * 2),
                e -> e != player && !(e instanceof PlayerEntity));
        for (LivingEntity mob : mobs) {
            Vec3d pull = center.add(0, 2.5, 0).subtract(mob.getPos()).multiply(0.45);
            mob.setVelocity(pull);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 50, 2));
            mob.damage(world.getDamageSources().outOfWorld(), dmg);
        }
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.7f, 1.5f);
    }

    private void triggerSupernovaStarfall(ServerWorld world, ServerPlayerEntity player, Vec3d center, double radius, float dmg) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 240, 3));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 240, 4));
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, radius * 2, radius * 2),
                e -> e != player && !(e instanceof PlayerEntity));
        for (LivingEntity mob : mobs) {
            ParticleEffectEngine.spawnLightningArc(world, mob.getPos().add(0, 14, 0), mob.getPos());
            mob.damage(world.getDamageSources().magic(), dmg);
        }
    }

    private void triggerDuetWorldbreakerCataclysm(ServerWorld world, ServerPlayerEntity player, Vec3d center) {
        ParticleEffectEngine.spawnGalaxyCataclysm(world, center);
        List<LivingEntity> mobs = world.getEntitiesByClass(LivingEntity.class, Box.of(center, 40, 40, 40),
                e -> e != player && !(e instanceof PlayerEntity));
        for (LivingEntity mob : mobs) {
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 120, 0));
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 40, 4));
            mob.damage(world.getDamageSources().sonicBoom(player), 120.0f);
        }
        player.heal(player.getMaxHealth());
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    /**
     * Пассивные бафы и 3D-аура частиц при удержании кирки в руке!
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && selected && entity instanceof ServerPlayerEntity player && world instanceof ServerWorld serverWorld) {
            if (world.getTime() % 20 == 0) {
                int hasteAmp = Math.min(4, tierLevel / 2);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 45, hasteAmp, true, false));
                if (tierLevel >= 3) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 45, 0, true, false));
                }
                if (tierLevel >= 4) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 240, 0, true, false));
                }
                if (tierLevel >= 5) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 45, 1, true, false));
                }
                if (tierLevel >= 6) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 45, tierLevel >= 9 ? 3 : 1, true, false));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 45, 1, true, false));
                }
            }
            // Каждые 8 тиков рисуем плавную 3D-ауру частиц вокруг игрока!
            if (world.getTime() % 8 == 0) {
                ParticleEffectEngine.spawnAmbientAura(serverWorld, player, tierLevel);
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int activeArea = getActiveAreaSize(stack);
        tooltip.add(Text.literal(displayTitle + " §8[Тир " + tierLevel + "/10]"));
        tooltip.add(Text.literal("§7Масштаб Бура: §6§l" + activeArea + "×" + activeArea + "×" + Math.min(activeArea, maxDepth)
                + " §8(Макс: " + maxAreaSize + "×" + maxAreaSize + "×" + maxDepth + ")"));
        tooltip.add(Text.literal("§7Переключить масштаб: §eShift + ПКМ"));
        tooltip.add(Text.literal("§7Активная ульта по мобам: §cПКМ или Удар"));
        if (tierLevel >= 3) tooltip.add(Text.literal("§6✦ Авто-Переплавка руд в слитки (100%)"));
        if (tierLevel >= 4) tooltip.add(Text.literal("§b✦ Вакуумный Магнит (лут сразу в инвентарь)"));
        if (tierLevel >= 6) tooltip.add(Text.literal("§d✦ Встроенная Удача и Вампиризм ХП"));
        if (tierLevel == 10) tooltip.add(Text.literal("§e★ Венец Сюжета Димы и Ульяны (2197 блоков за взмах!)"));
    }
}
