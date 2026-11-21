package ru.storymod.particles;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class ParticleEffectEngine {

    public static void spawnTierEffect(ServerWorld world, Vec3d center, ServerPlayerEntity player, int tier) {
        switch (tier) {
            case 1 -> spawnDnaHelix(world, center);
            case 2 -> spawnRunePentagram(world, center);
            case 3 -> spawnFireVortex(world, center);
            case 4 -> spawnFibonacciSphere(world, center, 1.8);
            case 5 -> spawnLightningBurst(world, center);
            case 6 -> spawnCrimsonWings(world, player);
            case 7 -> spawnChronoClock(world, center);
            case 8 -> spawnBlackHoleTorus(world, center);
            case 9 -> spawnSolarOrbit(world, center);
            case 10 -> spawnGalaxyCataclysm(world, center);
        }
    }

    public static void spawnAmbientAura(ServerWorld world, ServerPlayerEntity player, int tier) {
        double time = world.getTime() * 0.15;
        Vec3d pos = player.getPos();

        if (tier == 6) {
            spawnCrimsonWings(world, player);
        } else if (tier >= 9) {
            // Орбитальные кольца вокруг игрока
            for (int i = 0; i < 6; i++) {
                double angle = time + (i * Math.PI / 3.0);
                double rx = Math.cos(angle) * 1.4;
                double rz = Math.sin(angle) * 1.4;
                world.spawnParticles(ParticleTypes.END_ROD, pos.x + rx, pos.y + 1.0 + Math.sin(angle * 2) * 0.3, pos.z + rz, 1, 0, 0, 0, 0);
            }
            if (tier == 10) {
                // Золотая корона над головой
                for (int i = 0; i < 8; i++) {
                    double a = time * 1.5 + (i * Math.PI / 4.0);
                    world.spawnParticles(new DustParticleEffect(new Vector3f(1.0f, 0.84f, 0.0f), 1.2f),
                            pos.x + Math.cos(a) * 0.45, pos.y + 2.35, pos.z + Math.sin(a) * 0.45, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    // Тир 1: Изумрудная двойная ДНК-спираль
    public static void spawnDnaHelix(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 24; i++) {
            double y = i * 0.12;
            double angle = i * 0.45;
            double r = 0.85;
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    center.x + Math.cos(angle) * r, center.y + y, center.z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    center.x + Math.cos(angle + Math.PI) * r, center.y + y, center.z + Math.sin(angle + Math.PI) * r, 1, 0, 0, 0, 0);
        }
    }

    // Тир 2: Геометрическая пентаграмма рун
    public static void spawnRunePentagram(ServerWorld world, Vec3d center) {
        int points = 5;
        double radius = 1.6;
        for (int i = 0; i < 36; i++) {
            double a = (i / 36.0) * Math.PI * 2;
            world.spawnParticles(ParticleTypes.WAX_ON,
                    center.x + Math.cos(a) * radius, center.y + 0.2, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < points; i++) {
            double a1 = (i * 2 * Math.PI) / points;
            double a2 = (((i + 2) % points) * 2 * Math.PI) / points;
            Vec3d p1 = new Vec3d(center.x + Math.cos(a1) * radius, center.y + 0.2, center.z + Math.sin(a1) * radius);
            Vec3d p2 = new Vec3d(center.x + Math.cos(a2) * radius, center.y + 0.2, center.z + Math.sin(a2) * radius);
            drawParticleLine(world, p1, p2, 10, new DustParticleEffect(new Vector3f(1.0f, 0.75f, 0.2f), 1.1f));
        }
    }

    // Тир 3: Огненный конический торнадо-вихрь
    public static void spawnFireVortex(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 40; i++) {
            double progress = i / 40.0;
            double y = progress * 2.8;
            double r = 0.3 + progress * 1.6;
            double angle = i * 0.55;
            world.spawnParticles(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                    center.x + Math.cos(angle) * r, center.y + y, center.z + Math.sin(angle) * r, 1, 0, 0.02, 0, 0.01);
        }
    }

    // Тир 4: Кристаллическая сфера Фибоначчи
    public static void spawnFibonacciSphere(ServerWorld world, Vec3d center, double radius) {
        int samples = 48;
        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < samples; i++) {
            double y = 1 - (i / (double) (samples - 1)) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = phi * i;
            double x = Math.cos(theta) * r * radius;
            double z = Math.sin(theta) * r * radius;
            world.spawnParticles(ParticleTypes.END_ROD, center.x + x, center.y + y * radius, center.z + z, 1, 0, 0, 0, 0.02);
        }
    }

    public static void spawnSonicBeam(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        int steps = (int) (diff.length() * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3d p = from.add(diff.multiply(i / (double) Math.max(1, steps)));
            world.spawnParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // Тир 5: Ветвящиеся дуги Теслы
    public static void spawnLightningBurst(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 6; i++) {
            double a = (i * Math.PI) / 3.0;
            Vec3d target = center.add(Math.cos(a) * 3.0, (Math.random() - 0.5) * 2.0, Math.sin(a) * 3.0);
            spawnLightningArc(world, center, target);
        }
    }

    public static void spawnLightningArc(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        int segments = 12;
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double jitterX = (i == 0 || i == segments) ? 0 : (Math.random() - 0.5) * 0.45;
            double jitterY = (i == 0 || i == segments) ? 0 : (Math.random() - 0.5) * 0.45;
            double jitterZ = (i == 0 || i == segments) ? 0 : (Math.random() - 0.5) * 0.45;
            Vec3d p = from.add(diff.multiply(t)).add(jitterX, jitterY, jitterZ);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0, 0, 0, 0.02);
        }
    }

    // Тир 6: Багровые Крылья Серафима за спиной игрока (кривая Бабочки)
    public static void spawnCrimsonWings(ServerWorld world, ServerPlayerEntity player) {
        float yawRad = (float) Math.toRadians(player.getYaw());
        Vec3d back = new Vec3d(Math.sin(yawRad) * 0.45, 1.15, -Math.cos(yawRad) * 0.45);
        Vec3d origin = player.getPos().add(back);
        DustParticleEffect crimsonDust = new DustParticleEffect(new Vector3f(0.95f, 0.08f, 0.28f), 1.15f);

        for (double t = 0; t < Math.PI * 2; t += 0.18) {
            double r = Math.exp(Math.cos(t)) - 2 * Math.cos(4 * t) - Math.pow(Math.sin(t / 12.0), 5);
            double lx = Math.sin(t) * r * 0.55;
            double ly = Math.cos(t) * r * 0.45;
            double wx = lx * Math.cos(yawRad);
            double wz = lx * Math.sin(yawRad);
            world.spawnParticles(crimsonDust, origin.x + wx, origin.y + ly, origin.z + wz, 1, 0, 0, 0, 0);
        }
    }

    // Тир 7: Циферблат Хроноса со стрелками
    public static void spawnChronoClock(ServerWorld world, Vec3d center) {
        double radius = 2.6;
        for (int i = 0; i < 48; i++) {
            double a = (i / 48.0) * Math.PI * 2;
            world.spawnParticles(ParticleTypes.SNOWFLAKE,
                    center.x + Math.cos(a) * radius, center.y + 0.25, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
        double handAngle = (world.getTime() * 0.1) % (Math.PI * 2);
        Vec3d handEnd = new Vec3d(center.x + Math.cos(handAngle) * (radius * 0.85), center.y + 0.25, center.z + Math.sin(handAngle) * (radius * 0.85));
        drawParticleLine(world, center.add(0, 0.25, 0), handEnd, 14, new DustParticleEffect(new Vector3f(0.15f, 0.9f, 1.0f), 1.3f));
    }

    // Тир 8: 3D-Тор Аккреционного Диска Чёрной Дыры
    public static void spawnBlackHoleTorus(ServerWorld world, Vec3d center) {
        double R = 2.2;
        double r = 0.65;
        for (int i = 0; i < 64; i++) {
            double u = (i / 64.0) * Math.PI * 2;
            double v = u * 4.0 + world.getTime() * 0.2;
            double x = (R + r * Math.cos(v)) * Math.cos(u);
            double y = r * Math.sin(v);
            double z = (R + r * Math.cos(v)) * Math.sin(u);
            world.spawnParticles(ParticleTypes.DRAGON_BREATH, center.x + x, center.y + y, center.z + z, 1, 0, 0, 0, 0.01);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x + x * 0.5, center.y, center.z + z * 0.5, 1, 0, 0, 0, 0.02);
            }
        }
    }

    // Тир 9: 3-осевая Солнечная Орбита Небулы
    public static void spawnSolarOrbit(ServerWorld world, Vec3d center) {
        double radius = 2.8;
        for (int ring = 0; ring < 3; ring++) {
            double tilt = (ring - 1) * 0.65;
            for (int i = 0; i < 32; i++) {
                double a = (i / 32.0) * Math.PI * 2;
                double x = Math.cos(a) * radius;
                double y = Math.sin(a) * Math.sin(tilt) * radius;
                double z = Math.sin(a) * Math.cos(tilt) * radius;
                world.spawnParticles(ParticleTypes.END_ROD, center.x + x, center.y + 1.0 + y, center.z + z, 1, 0, 0, 0, 0.01);
            }
        }
    }

    // Тир 10: Двухрукавная Галактическая Спираль Димы и Ульяны
    public static void spawnGalaxyCataclysm(ServerWorld world, Vec3d center) {
        for (int arm = 0; arm < 2; arm++) {
            double armOffset = arm * Math.PI;
            for (int i = 0; i < 55; i++) {
                double t = i / 55.0;
                double r = t * 5.0;
                double angle = armOffset + t * Math.PI * 3.5;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, center.x + x, center.y + 0.3 + t * 1.2, center.z + z, 1, 0, 0.05, 0, 0.03);
                world.spawnParticles(ParticleTypes.END_ROD, center.x + x, center.y + 0.2, center.z + z, 1, 0, 0, 0, 0.01);
            }
        }
    }

    private static void drawParticleLine(ServerWorld world, Vec3d start, Vec3d end, int steps, DustParticleEffect effect) {
        Vec3d diff = end.subtract(start);
        for (int i = 0; i <= steps; i++) {
            Vec3d p = start.add(diff.multiply(i / (double) steps));
            world.spawnParticles(effect, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
