package com.modernity.dragon.entity;

import com.modernity.dragon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;

/** Deterministic server tests for registration, flight setup, phases, and special attacks. */
public final class DragonBossGameTests {
    private DragonBossGameTests() {
    }

    public static void spawnHasBossStatsAndFlight(GameTestHelper helper) {
        DragonBossEntity boss = helper.spawn(ModEntities.DRAGON_BOSS.get(), new BlockPos(2, 2, 2));
        helper.assertTrue(boss.isAlive(), "dragon boss should spawn alive");
        helper.assertTrue(boss.getHealth() == boss.getMaxHealth(), "dragon boss should spawn at full health");
        helper.assertTrue(boss.getMaxHealth() == 500.0F, "dragon boss max health should be 500");
        helper.assertTrue(boss.isNoGravity(), "dragon boss should fly without gravity");
        helper.assertTrue(boss.getAttackState() == DragonBossEntity.ATTACK_IDLE, "dragon boss should begin idle");
        helper.succeed();
    }

    public static void coilUltimateDamagesNearbyTargets(GameTestHelper helper) {
        DragonBossEntity boss = helper.spawn(ModEntities.DRAGON_BOSS.get(), new BlockPos(2, 4, 2));
        IronGolem victim = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 3));
        victim.setNoAi(true);
        float initialHealth = victim.getHealth();
        boss.setTarget(victim);
        boss.beginAttack(DragonBossEntity.ATTACK_COIL, victim);

        helper.runAfterDelay(78, () -> {
            helper.assertTrue(
                    !victim.isAlive() || victim.getHealth() < initialHealth,
                    "the coiled ultimate should damage a target inside its blast radius"
            );
            helper.assertTrue(
                    boss.getAttackState() == DragonBossEntity.ATTACK_COIL,
                    "the coil animation should remain active briefly after its impact"
            );
            helper.succeed();
        });
    }

    public static void specialAttacksRespectHalfHealthBoundary(GameTestHelper helper) {
        DragonBossEntity boss = helper.spawn(ModEntities.DRAGON_BOSS.get(), new BlockPos(2, 4, 2));
        IronGolem victim = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 3));
        victim.setNoAi(true);

        boss.setHealth(boss.getMaxHealth() * 0.5F);
        boss.beginAttack(DragonBossEntity.ATTACK_SONIC_BOOM, victim);
        helper.assertTrue(!boss.isSecondStage(), "exactly 50% health should still be stage one");
        helper.assertTrue(
                boss.getAttackState() == DragonBossEntity.ATTACK_LIGHTNING,
                "stage one should replace a requested sonic boom with lightning"
        );

        boss.setHealth(boss.getMaxHealth() * 0.5F - 1.0F);
        boss.beginAttack(DragonBossEntity.ATTACK_LIGHTNING, victim);
        helper.assertTrue(boss.isSecondStage(), "health below 50% should enter stage two");
        helper.assertTrue(
                boss.getAttackState() == DragonBossEntity.ATTACK_SONIC_BOOM,
                "stage two should replace a requested lightning attack with sonic boom"
        );

        boss.setHealth(boss.getMaxHealth() * 0.5F);
        boss.beginAttack(DragonBossEntity.ATTACK_LIGHTNING, victim);
        boss.setHealth(boss.getMaxHealth() * 0.5F - 1.0F);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    boss.getAttackState() == DragonBossEntity.ATTACK_SONIC_BOOM,
                    "crossing below 50% during a lightning windup should restart as sonic boom"
            );
            helper.succeed();
        });
    }

    public static void lightningAttackDamagesTarget(GameTestHelper helper) {
        DragonBossEntity boss = helper.spawn(ModEntities.DRAGON_BOSS.get(), new BlockPos(2, 6, 2));
        IronGolem victim = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 3));
        victim.setNoAi(true);
        float initialHealth = victim.getHealth();
        boss.setTarget(victim);
        boss.beginAttack(DragonBossEntity.ATTACK_LIGHTNING, victim);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    victim.getHealth() < initialHealth,
                    "the stage-one lightning strike should damage a target inside its telegraph"
            );
            helper.assertTrue(
                    boss.getAttackState() == DragonBossEntity.ATTACK_LIGHTNING,
                    "the lightning animation should remain active briefly after the strike"
            );
            helper.succeed();
        });
    }
}
