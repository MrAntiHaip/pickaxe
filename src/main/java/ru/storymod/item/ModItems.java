package ru.storymod.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import ru.storymod.StoryPickaxesMod;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    public static final List<StoryPickaxeItem> ALL_PICKAXES = new ArrayList<>();

    // Невыбрасываемый Компас Судьбы (Сюжет + Роли + Рулетка + События)
    public static final Item STORY_COMPASS = registerItem("story_compass",
            new StoryCompassItem(new FabricItemSettings().maxCount(1).fireproof().rarity(Rarity.EPIC)));

    // 10 Сюжетных Кирок с прогрессивным радиусом копания (до 13x13x13!) и уроном по мобам
    public static final StoryPickaxeItem TIER_1_DIRT = registerPickaxe("dirt_awakened_pickaxe",
            1, "§aЗемляная Кирка Пробуждения", 1, 2, 7.0f, 8.0f, Rarity.UNCOMMON);

    public static final StoryPickaxeItem TIER_2_ROOT = registerPickaxe("petrified_root_pickaxe",
            2, "§6Окаменелая Кирка Древних Корней", 3, 1, 10.0f, 12.0f, Rarity.UNCOMMON);

    public static final StoryPickaxeItem TIER_3_MAGMA = registerPickaxe("magma_iron_crusher",
            3, "§cМагмово-Железный Крушитель", 3, 3, 14.0f, 16.0f, Rarity.RARE);

    public static final StoryPickaxeItem TIER_4_CRYSTAL = registerPickaxe("crystal_echo_pickaxe",
            4, "§dКристальная Кирка Эхо-Резонанса", 5, 3, 18.0f, 22.0f, Rarity.RARE);

    public static final StoryPickaxeItem TIER_5_THUNDER = registerPickaxe("thunder_valkyrie_pickaxe",
            5, "§bГромовая Кирка Валькирии", 5, 5, 23.0f, 28.0f, Rarity.RARE);

    public static final StoryPickaxeItem TIER_6_BLOOD_RUBY = registerPickaxe("blood_ruby_reaper_pickaxe",
            6, "§4Кроваво-Рубиновый Пожиратель Душ", 7, 5, 29.0f, 36.0f, Rarity.EPIC);

    public static final StoryPickaxeItem TIER_7_CHRONOS = registerPickaxe("cryo_chronos_pickaxe",
            7, "§3Кирка Ледяного Абсолюта и Хроноса", 7, 7, 36.0f, 45.0f, Rarity.EPIC);

    public static final StoryPickaxeItem TIER_8_VOID = registerPickaxe("void_rift_pickaxe",
            8, "§5Пустотная Кирка Разрыва Энда", 9, 9, 46.0f, 60.0f, Rarity.EPIC);

    public static final StoryPickaxeItem TIER_9_NEBULA = registerPickaxe("nebula_star_breaker",
            9, "§d★ Звёздный Рассекатель Небулы ★", 11, 11, 62.0f, 85.0f, Rarity.EPIC);

    public static final StoryPickaxeItem TIER_10_WORLDBREAKER = registerPickaxe("dima_ulyana_worldbreaker_pickaxe",
            10, "§6§l⚡ АБСОЛЮТНЫЙ МИРОКРУШИТЕЛЬ ДИМЫ И УЛЬЯНЫ ⚡", 13, 13, 95.0f, 150.0f, Rarity.EPIC);

    public static final ItemGroup STORY_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(StoryPickaxesMod.MOD_ID, "story_group"),
            FabricItemGroup.builder()
                    .displayName(Text.literal("§6✦ Сюжет: Дима и Ульяна"))
                    .icon(() -> new ItemStack(STORY_COMPASS))
                    .entries((context, entries) -> {
                        entries.add(STORY_COMPASS);
                        for (StoryPickaxeItem pickaxe : ALL_PICKAXES) {
                            entries.add(pickaxe);
                        }
                    })
                    .build());

    private static StoryPickaxeItem registerPickaxe(String name, int tier, String displayTitle,
                                                    int areaSize, int depth, float attackDamage, float miningSpeed, Rarity rarity) {
        StoryPickaxeItem pickaxe = new StoryPickaxeItem(
                ToolMaterials.NETHERITE,
                (int) attackDamage,
                -2.2f,
                tier,
                displayTitle,
                areaSize,
                depth,
                miningSpeed,
                new FabricItemSettings().maxCount(1).fireproof().rarity(rarity)
        );
        StoryPickaxeItem registered = (StoryPickaxeItem) registerItem(name, pickaxe);
        ALL_PICKAXES.add(registered);
        return registered;
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(StoryPickaxesMod.MOD_ID, name), item);
    }

    public static boolean isStoryPickaxe(Item item) {
        return item instanceof StoryPickaxeItem;
    }

    public static StoryPickaxeItem getPickaxeByTier(int tier) {
        for (StoryPickaxeItem item : ALL_PICKAXES) {
            if (item.getTierLevel() == tier) return item;
        }
        return TIER_1_DIRT;
    }

    public static void registerModItems() {
        StoryPickaxesMod.LOGGER.info("Зарегистрировано 10 сюжетных кирок и Компас Судьбы!");
    }
}
