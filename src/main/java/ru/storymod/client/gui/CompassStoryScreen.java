package ru.storymod.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import ru.storymod.StoryPickaxesClient;
import ru.storymod.network.ModNetworking;

public class CompassStoryScreen extends Screen {
    private int currentTab = 0; // 0=Сюжет, 1=Роли, 2=Кирки, 3=Рулетка, 4=События
    private int rouletteSpinTicks = 0;
    private int pendingRouletteTier = 1;

    public CompassStoryScreen() {
        super(Text.literal("Компас Судьбы: Дима и Ульяна"));
    }

    @Override
    protected void init() {
        this.clearChildren();
        int centerX = this.width / 2;
        int topY = this.height / 2 - 105;

        // Верхние вкладки меню Компаса Судьбы
        String[] tabs = {"📜 Сюжет", "🎭 Роли", "⛏ 10 Кирок", "🎰 Рулетка", "⚡ События"};
        for (int i = 0; i < tabs.length; i++) {
            final int tabIdx = i;
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal((currentTab == i ? "§e§l" : "§f") + tabs[i]),
                    btn -> {
                        this.currentTab = tabIdx;
                        this.init();
                    }
            ).dimensions(centerX - 165 + (i * 67), topY, 64, 20).build());
        }

        if (currentTab == 0) {
            // ВКЛАДКА 0: СЮЖЕТ И СТАРЕЙШИНА
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§a▶ НАЧАТЬ СЮЖЕТ (Призвать Старейшину)"),
                    btn -> sendCompassPacket("START_STORY", 0)
            ).dimensions(centerX - 130, topY + 115, 260, 22).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§6✦ Сдать Задание Главы / Пробудить Кирку"),
                    btn -> sendCompassPacket("ADVANCE_CHAPTER", 0)
            ).dimensions(centerX - 130, topY + 143, 260, 22).build());

        } else if (currentTab == 1) {
            // ВКЛАДКА 1: ВЫБОР РОЛИ (ДИМА / УЛЬЯНА)
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§c⚔ Выбрать Роль: ДИМА (Сила и Разрушение)"),
                    btn -> sendCompassPacket("SELECT_ROLE_DIMA", 0)
            ).dimensions(centerX - 135, topY + 95, 270, 24).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§d✦ Выбрать Роль: УЛЬЯНА (Фортуна и Стихии)"),
                    btn -> sendCompassPacket("SELECT_ROLE_ULYANA", 0)
            ).dimensions(centerX - 135, topY + 130, 270, 24).build());

        } else if (currentTab == 2) {
            // ВКЛАДКА 2: КУЗНИЦА 10 КИРОК (до 13x13x13)
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§6⛏ Пробудить / Улучшить Сюжетную Кирку (След. Тир)"),
                    btn -> sendCompassPacket("ADVANCE_CHAPTER", 0)
            ).dimensions(centerX - 140, topY + 148, 280, 22).build());

        } else if (currentTab == 3) {
            // ВКЛАДКА 3: БЫСТРАЯ РУЛЕТКА ДЕПА (1.1 сек!)
            String[] tierButtons = {
                    "§7Тир 1: Деп 64x Булыжника (Нуб-дроп 55% | Куш: Железо/Алмаз)",
                    "§fТир 2: Деп 16x Железа (Нуб-дроп 40% | Куш: Золото/Алмазы)",
                    "§eТир 3: Деп 16x Золота (Нуб-дроп 28% | Куш: Алмазы/Незерит)",
                    "§bТир 4: Деп 8x Алмазов (Нуб-дроп 18% | Куш: х3 Алмазы/Незерит)",
                    "§dТир 5: Деп 2x Незерита (Нуб-дроп 10% | Куш: 10x Незерит!)"
            };
            for (int t = 0; t < tierButtons.length; t++) {
                final int tierLevel = t + 1;
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(tierButtons[t]),
                        btn -> {
                            this.pendingRouletteTier = tierLevel;
                            this.rouletteSpinTicks = 22; // Быстрый спин 1.1 секунды!
                        }
                ).dimensions(centerX - 150, topY + 48 + (t * 24), 300, 20).build());
            }

        } else if (currentTab == 4) {
            // ВКЛАДКА 4: ПРИЗЫВ МИРОВЫХ СОБЫТИЙ
            String[] events = {
                    "§a1. Призыв Старейшины и Оборона Лагеря",
                    "§62. Метеоритный Дождь Звёздных Руд",
                    "§43. Нашествие Короля Глубинных Големов (Босс)",
                    "§e4. Золотая Лихорадка Безумного Депа (x4 Руды)",
                    "§b5. Аномалия Гравитации и Замёрзшего Времени",
                    "§d6. Испытание Дуэта: Симфония Димы и Ульяны"
            };
            for (int e = 0; e < events.length; e++) {
                final int evId = e + 1;
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(events[e]),
                        btn -> sendCompassPacket("TRIGGER_EVENT", evId)
                ).dimensions(centerX - 145, topY + 38 + (e * 22), 290, 20).build());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (rouletteSpinTicks > 0) {
            rouletteSpinTicks--;
            if (rouletteSpinTicks == 0) {
                sendCompassPacket("SPIN_ROULETTE", pendingRouletteTier);
            }
        }
    }

    private void sendCompassPacket(String action, int param) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(action);
        buf.writeInt(param);
        ClientPlayNetworking.send(ModNetworking.COMPASS_ACTION_C2S, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int centerX = this.width / 2;
        int topY = this.height / 2 - 105;

        // Фон панели Компаса
        context.fill(centerX - 172, topY - 12, centerX + 172, topY + 182, 0xE814121F);
        context.drawBorder(centerX - 172, topY - 12, 344, 194, 0xFFD97706);

        if (currentTab == 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§6§l📜 СЮЖЕТНОЕ ПРОХОЖДЕНИЕ: ДИМА И УЛЬЯНА", centerX, topY + 30, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Текущая роль: §e" + StoryPickaxesClient.clientRole + " §8| §7Глава: §a" + StoryPickaxesClient.clientChapter + "/10", centerX, topY + 46, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§fНа старте кирка НЕ выдаётся! Нажмите «НАЧАТЬ СЮЖЕТ»,", centerX, topY + 68, 0xCCCCCC);
            context.drawCenteredTextWithShadow(this.textRenderer, "§fпоявится Житель-Старейшина, добудьте 10 земли руками", centerX, topY + 82, 0xCCCCCC);
            context.drawCenteredTextWithShadow(this.textRenderer, "§fи получите §a1-ю Земляную Кирку Пробуждения§f!", centerX, topY + 96, 0xCCCCCC);
        } else if (currentTab == 1) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§b§l🎭 ВЫБОР РОЛИ ДЛЯ СЮЖЕТКИ", centerX, topY + 32, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§cДима: §7+35% урона кирками по мобам, сейсмический удар, возврат камня", centerX, topY + 54, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§dУльяна: §7+15% шанс куша в Рулетке, +1 Удача на руды, аура кристаллов", centerX, topY + 70, 0xFFFFFF);
        } else if (currentTab == 2) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§6§l⛏ ЭВОЛЮЦИЯ 10 СЮЖЕТНЫХ КИРОК (ДО 13×13×13!)", centerX, topY + 28, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§a1.Земляная(1×1) §7→ §62.Корней(3×3) §7→ §c3.Магма(3×3×3) §7→ §d4.Кристалл(5×5×3)", centerX, topY + 48, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§b5.Гром(5×5×5) §7→ §46.Рубин(7×7×5) §7→ §37.Хронос(7×7×7) §7→ §58.Пустота(9×9×9)", centerX, topY + 66, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§d9.Небула(11×11×11) §7→ §6§l10.МИРОКРУШИТЕЛЬ ДИМЫ И УЛЬЯНЫ (13×13×13!)", centerX, topY + 84, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§eВсе кирки бьют мобов способностями и имеют 3D-эффекты частиц!", centerX, topY + 108, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Переключение радиуса бура в игре: §fShift + ПКМ", centerX, topY + 124, 0xFFFFFF);
        } else if (currentTab == 3) {
            String status = rouletteSpinTicks > 0
                    ? "§e§l⚡ КРУТИМ РУЛЕТКУ... (" + String.format("%.1f", rouletteSpinTicks / 20.0f) + " сек) ⚡"
                    : "§6§l🎰 БЫСТРАЯ РУЛЕТКА УДАЧИ (Спин 1.1 сек! Везде есть шанс на нуб-дроп!)";
            context.drawCenteredTextWithShadow(this.textRenderer, status, centerX, topY + 28, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
