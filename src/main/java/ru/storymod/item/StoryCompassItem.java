package ru.storymod.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import ru.storymod.network.ModNetworking;

import java.util.List;

public class StoryCompassItem extends Item {
    public StoryCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 1.3f);
            ModNetworking.openCompassScreen(serverPlayer);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("§d✦ Невыбрасываемая Реликвия Димы и Ульяны"));
        tooltip.add(Text.literal("§7Нажмите §eПКМ§7, чтобы открыть Главное Меню Компаса:"));
        tooltip.add(Text.literal(" §a• Сюжет и Призыв Старейшины (Глава 1 — 10)"));
        tooltip.add(Text.literal(" §b• Выбор Роли: Дима (Сила) или Ульяна (Фортуна)"));
        tooltip.add(Text.literal(" §6• Кузница 10 Кирок (Апгрейд до 13×13×13!)"));
        tooltip.add(Text.literal(" §e• Быстрая Рулетка Депа (64 Булыжника, 16 Железа и др.)"));
        tooltip.add(Text.literal(" §c• Призыв Событий и Боссов"));
    }
}
