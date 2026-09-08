package com.modernity.dragon.entity;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A persistent, server-authoritative mountain boss. The renderer owns the
 * articulated body, while this entity supplies a smooth serpentine flight path
 * and synchronized attack state for animation.
 */
public class DragonBossEntity extends Monster {
    public static final int ATTACK_IDLE = 0;
    public static final int ATTACK_RUSH = 1;
    public static final int ATTACK_COIL = 2;
    public static final int ATTACK_FIREBALL = 3;
    public static final int ATTACK_SONIC_BOOM = 4;
    public static final int ATTACK_LIGHTNING = 5;

    public static final int RUSH_DURATION = 60;
    public static final int COIL_DURATION = 100;
    public static final int FIREBALL_DURATION = 65;
    public static final int SONIC_BOOM_DURATION = 80;
    public static final int LIGHTNING_DURATION = 60;

    private static final EntityDataAccessor<Integer> DATA_ATTACK_STATE =
            SynchedEntityData.defineId(DragonBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICKS =
            SynchedEntityData.defineId(DragonBossEntity.class, EntityDataSerializers.INT);

    private static final double NATURAL_SPAWN_SEPARATION = 256.0;
    private static final double MAX_SONIC_BOOM_DISTANCE = 48.0;
    private static final double SONIC_BOOM_RADIUS = 1.35;
    private static final double LIGHTNING_RADIUS = 3.5;
    private static final float SECOND_STAGE_HEALTH_FRACTION = 0.5F;
    private static final int LIGHTNING_STRIKE_TICK = 32;
    private static final PowerParticleOption DRAGON_BREATH = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);

    private final ServerBossEvent bossEvent = Util.make(
            new ServerBossEvent(
                    Mth.createInsecureUUID(this.random),
                    this.getDisplayName(),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.NOTCHED_10
            ),
            event -> {
                event.setDarkenScreen(true);
                event.setCreateWorldFog(true);
                event.setPlayBossMusic(true);
            }
    );
    private final Set<Integer> hitEntitiesThisAttack = new HashSet<>();

    private int attackCooldown = 80;
    private boolean homeInitialized;
    private double homeX;
    private double homeY;
    private double homeZ;
    private double attackTargetX;
    private double attackTargetY;
    private double attackTargetZ;

    public DragonBossEntity(EntityType<? extends DragonBossEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setHealth(this.getMaxHealth());
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FLYING_SPEED, 0.75)
                .add(Attributes.FOLLOW_RANGE, 128.0);
    }

    /** Spawn only on open, high mountain surfaces and keep bosses far apart. */
    public static boolean checkDragonBossSpawnRules(
            EntityType<DragonBossEntity> type,
            ServerLevelAccessor level,
            EntitySpawnReason reason,
            BlockPos pos,
            RandomSource random
    ) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (reason != EntitySpawnReason.NATURAL) {
            return true;
        }
        if (pos.getY() < level.getSeaLevel() + 24 || !level.canSeeSky(pos.above()) || random.nextInt(96) != 0) {
            return false;
        }
        AABB exclusionArea = new AABB(pos).inflate(NATURAL_SPAWN_SEPARATION);
        return level.getLevel().getEntitiesOfClass(DragonBossEntity.class, exclusionArea, Entity::isAlive).isEmpty();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanOpenDoors(false);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, false, false, null));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ATTACK_STATE, ATTACK_IDLE);
        entityData.define(DATA_ATTACK_TICKS, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("AttackState", this.getAttackState());
        output.putInt("AttackTicks", this.getAttackTicks());
        output.putInt("AttackCooldown", this.attackCooldown);
        output.putBoolean("HomeInitialized", this.homeInitialized);
        output.putDouble("HomeX", this.homeX);
        output.putDouble("HomeY", this.homeY);
        output.putDouble("HomeZ", this.homeZ);
        output.putDouble("AttackTargetX", this.attackTargetX);
        output.putDouble("AttackTargetY", this.attackTargetY);
        output.putDouble("AttackTargetZ", this.attackTargetZ);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int loadedState = Mth.clamp(input.getIntOr("AttackState", ATTACK_IDLE), ATTACK_IDLE, ATTACK_LIGHTNING);
        loadedState = this.attackForCurrentStage(loadedState);
        this.entityData.set(DATA_ATTACK_STATE, loadedState);
        this.entityData.set(DATA_ATTACK_TICKS, Math.max(0, input.getIntOr("AttackTicks", 0)));
        this.attackCooldown = Math.max(0, input.getIntOr("AttackCooldown", 80));
        this.homeInitialized = input.getBooleanOr("HomeInitialized", false);
        this.homeX = input.getDoubleOr("HomeX", this.getX());
        this.homeY = input.getDoubleOr("HomeY", this.getY());
        this.homeZ = input.getDoubleOr("HomeZ", this.getZ());
        this.attackTargetX = input.getDoubleOr("AttackTargetX", this.getX());
        this.attackTargetY = input.getDoubleOr("AttackTargetY", this.getY());
        this.attackTargetZ = input.getDoubleOr("AttackTargetZ", this.getZ());
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.setNoGravity(true);
        this.resetFallDistance();
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));

        if (!this.homeInitialized) {
            this.homeInitialized = true;
            this.homeX = this.getX();
            this.homeY = Math.max(this.getY() + 14.0, level.getSeaLevel() + 34.0);
            this.homeZ = this.getZ();
        }

        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || target.isSpectator() || this.distanceToSqr(target) > 19600.0)) {
            this.setTarget(null);
            target = null;
        }

        if (this.getAttackState() == ATTACK_IDLE) {
            this.tickIdleFlight(target);
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            } else if (target != null) {
                this.beginAttack(this.selectAttack(target), target);
            }
        } else {
            this.tickAttack(level, target);
        }

        this.faceMovement();
    }

    private void tickIdleFlight(@Nullable LivingEntity target) {
        double time = this.tickCount * 0.055;
        Vec3 destination;
        double speed;
        if (target == null) {
            destination = new Vec3(
                    this.homeX + Math.cos(time * 0.72) * 18.0,
                    this.homeY + Math.sin(time * 1.33) * 4.0,
                    this.homeZ + Math.sin(time * 0.72) * 18.0
            );
            speed = 0.42;
        } else {
            double distance = this.distanceTo(target);
            double radius = distance > 42.0 ? 4.0 : 13.0;
            destination = target.position().add(
                    Math.cos(time) * radius,
                    7.5 + Math.sin(time * 1.7) * 3.0,
                    Math.sin(time) * radius
            );
            speed = distance > 42.0 ? 0.78 : 0.56;
        }
        this.steerToward(destination, speed, 0.12);
    }

    private int selectAttack(LivingEntity target) {
        double distanceSqr = this.distanceToSqr(target);
        int stageSpecial = this.isSecondStage() ? ATTACK_SONIC_BOOM : ATTACK_LIGHTNING;
        if (distanceSqr > 625.0) {
            return this.random.nextBoolean() ? ATTACK_RUSH : stageSpecial;
        }
        if (distanceSqr < 100.0 && this.random.nextFloat() < 0.45F) {
            return ATTACK_COIL;
        }
        return switch (this.random.nextInt(4)) {
            case 0 -> ATTACK_RUSH;
            case 1 -> ATTACK_COIL;
            case 2 -> ATTACK_FIREBALL;
            default -> stageSpecial;
        };
    }

    void beginAttack(int attack, @Nullable LivingEntity target) {
        int requestedAttack = Mth.clamp(attack, ATTACK_RUSH, ATTACK_LIGHTNING);
        this.entityData.set(DATA_ATTACK_STATE, this.attackForCurrentStage(requestedAttack));
        this.entityData.set(DATA_ATTACK_TICKS, 0);
        this.hitEntitiesThisAttack.clear();
        if (target != null) {
            this.rememberTarget(target);
        } else {
            Vec3 forward = this.getLookAngle().scale(24.0);
            this.rememberTarget(this.position().add(forward));
        }
    }

    private void tickAttack(ServerLevel level, @Nullable LivingEntity target) {
        int attack = this.getAttackState();
        int ticks = this.getAttackTicks();
        int phaseAttack = this.attackForCurrentStage(attack);
        if (phaseAttack != attack) {
            // Latch only while the attack remains legal. Crossing the health
            // boundary during a windup restarts the other phase's special,
            // so an out-of-phase strike can never leak through.
            this.beginAttack(phaseAttack, target);
            attack = this.getAttackState();
            ticks = 0;
        }

        switch (attack) {
            case ATTACK_RUSH -> this.tickRush(level, target, ticks);
            case ATTACK_COIL -> this.tickCoil(level, target, ticks);
            case ATTACK_FIREBALL -> this.tickFireballs(level, target, ticks);
            case ATTACK_SONIC_BOOM -> this.tickSonicBoom(level, target, ticks);
            case ATTACK_LIGHTNING -> this.tickLightning(level, target, ticks);
            default -> this.finishAttack();
        }

        if (this.getAttackState() != ATTACK_IDLE) {
            this.entityData.set(DATA_ATTACK_TICKS, ticks + 1);
        }
    }

    private void tickRush(ServerLevel level, @Nullable LivingEntity target, int ticks) {
        if (ticks < 20) {
            if (target != null) {
                this.rememberTarget(target);
            }
            Vec3 aim = this.attackTarget().subtract(this.position()).normalize();
            this.steerToward(this.position().subtract(aim.scale(4.0)).add(0.0, 2.0, 0.0), 0.28, 0.22);
            if (ticks % 4 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getEyeY(), this.getZ(), 8, 1.5, 1.0, 1.5, 0.03);
            }
        } else if (ticks < 50) {
            Vec3 direction = this.attackTarget().subtract(this.position()).normalize();
            if (direction.lengthSqr() < 1.0E-5) {
                direction = this.getLookAngle();
            }
            this.setDeltaMovement(direction.scale(1.65));
            this.damageRushContacts(level);
            if (ticks % 2 == 0) {
                level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.5, this.getZ(), 10, 1.2, 0.9, 1.2, 0.04);
            }
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.78));
        }

        if (ticks >= RUSH_DURATION) {
            this.finishAttack();
        }
    }

    private void damageRushContacts(ServerLevel level) {
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(1.4),
                entity -> entity != this && !(entity instanceof DragonBossEntity) && entity.isAlive() && !this.isAlliedTo(entity)
        )) {
            if (this.hitEntitiesThisAttack.add(victim.getId())) {
                victim.hurtServer(level, this.damageSources().mobAttack(this), 22.0F);
                Vec3 away = victim.position().subtract(this.position());
                if (away.lengthSqr() > 1.0E-5) {
                    victim.push(away.normalize().scale(2.4).add(0.0, 0.45, 0.0));
                }
            }
        }
    }

    private void tickCoil(ServerLevel level, @Nullable LivingEntity target, int ticks) {
        if (ticks < 45 && target != null) {
            this.rememberTarget(target);
        }

        Vec3 center = this.attackTarget();
        if (ticks < 60) {
            double progress = ticks / 60.0;
            double radius = Mth.lerp(progress, 15.0, 5.0);
            double angle = ticks * 0.27;
            Vec3 destination = center.add(
                    Math.cos(angle) * radius,
                    3.0 + Math.sin(angle * 1.8) * (3.5 - progress * 2.0),
                    Math.sin(angle) * radius
            );
            this.steerToward(destination, 0.82, 0.24);
            if (ticks % 3 == 0) {
                level.sendParticles(DRAGON_BREATH, center.x, center.y + 1.0, center.z, 5, radius * 0.45, 1.2, radius * 0.45, 0.01);
            }
        } else {
            this.steerToward(center.add(0.0, 5.0, 0.0), 0.35, 0.3);
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1.0, center.z, 7, 2.8, 1.2, 2.8, 0.02);
        }

        if (ticks == 70) {
            this.releaseCoilBlast(level, center);
        }
        if (ticks >= COIL_DURATION) {
            this.finishAttack();
        }
    }

    private void releaseCoilBlast(ServerLevel level, Vec3 center) {
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 0.65F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0, center.z, 2, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(DRAGON_BREATH, center.x, center.y + 1.0, center.z, 180, 8.0, 3.5, 8.0, 0.22);
        AABB blastArea = AABB.ofSize(center.add(0.0, 1.0, 0.0), 22.0, 10.0, 22.0);
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class,
                blastArea,
                entity -> entity != this && !(entity instanceof DragonBossEntity) && entity.isAlive() && !this.isAlliedTo(entity)
        )) {
            double distance = Math.sqrt(victim.distanceToSqr(center));
            if (distance <= 11.0) {
                float damage = (float)Mth.lerp(distance / 11.0, 38.0, 18.0);
                victim.hurtServer(level, level.damageSources().source(DamageTypes.DRAGON_BREATH, this), damage);
                Vec3 away = victim.position().subtract(center);
                if (away.lengthSqr() > 1.0E-5) {
                    victim.push(away.normalize().scale(3.0).add(0.0, 0.65, 0.0));
                }
            }
        }
    }

    private void tickFireballs(ServerLevel level, @Nullable LivingEntity target, int ticks) {
        if (target != null) {
            this.rememberTarget(target);
        }
        Vec3 targetPos = this.attackTarget();
        Vec3 side = targetPos.subtract(this.position()).cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        this.steerToward(targetPos.add(side.scale(16.0)).add(0.0, 9.0, 0.0), 0.46, 0.12);

        if (ticks < 20 && ticks % 3 == 0) {
            level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getEyeY(), this.getZ(), 8, 0.7, 0.7, 0.7, 0.02);
        }
        if (ticks == 20 || ticks == 32 || ticks == 44) {
            this.launchFireball(level, targetPos);
        }
        if (ticks >= FIREBALL_DURATION) {
            this.finishAttack();
        }
    }

    private void launchFireball(ServerLevel level, Vec3 targetPos) {
        Vec3 muzzle = this.getEyePosition().add(this.getLookAngle().scale(1.8));
        Vec3 direction = targetPos.add(0.0, 1.0, 0.0).subtract(muzzle).normalize();
        LargeFireball fireball = new LargeFireball(level, this, direction, 2);
        fireball.setPos(muzzle);
        level.addFreshEntity(fireball);
        level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 2.5F, 0.8F + this.random.nextFloat() * 0.2F);
    }

    private void tickSonicBoom(ServerLevel level, @Nullable LivingEntity target, int ticks) {
        if (ticks < 30 && target != null) {
            this.rememberTarget(target);
        }
        Vec3 targetPos = this.attackTarget();
        this.steerToward(targetPos.add(0.0, 11.0, 0.0), 0.34, 0.16);

        if (ticks < 30) {
            if (ticks % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getEyeY(), this.getZ(), 4, 0.6, 0.6, 0.6, 0.01);
            }
        } else if (ticks < 65) {
            this.fireSonicBoom(level, targetPos, ticks % 5 == 0);
        }
        if (ticks >= SONIC_BOOM_DURATION) {
            this.finishAttack();
        }
    }

    private void fireSonicBoom(ServerLevel level, Vec3 targetPos, boolean dealDamage) {
        Vec3 start = this.getEyePosition();
        Vec3 direction = targetPos.add(0.0, 1.0, 0.0).subtract(start).normalize();
        if (direction.lengthSqr() < 1.0E-6) {
            direction = this.getLookAngle();
        }
        Vec3 requestedEnd = start.add(direction.scale(MAX_SONIC_BOOM_DISTANCE));
        HitResult blockHit = level.clip(new ClipContext(start, requestedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? requestedEnd : blockHit.getLocation();
        double length = start.distanceTo(end);

        int beamStep = 0;
        for (double d = 0.5; d < length; d += 1.25, beamStep++) {
            Vec3 point = start.add(direction.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
            if (beamStep % 2 == 0) {
                level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0.08, 0.08, 0.08, 0.0);
            }
        }

        if (!dealDamage) {
            return;
        }
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 1.25F);
        AABB beamBounds = new AABB(start, end).inflate(SONIC_BOOM_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class,
                beamBounds,
                entity -> entity != this && !(entity instanceof DragonBossEntity) && entity.isAlive() && !this.isAlliedTo(entity)
        )) {
            Vec3 relative = victim.getBoundingBox().getCenter().subtract(start);
            double alongBeam = Mth.clamp(relative.dot(direction), 0.0, length);
            Vec3 closest = start.add(direction.scale(alongBeam));
            if (victim.getBoundingBox().inflate(SONIC_BOOM_RADIUS).contains(closest)) {
                victim.hurtServer(level, this.damageSources().sonicBoom(this), 8.0F);
            }
        }
    }

    private void tickLightning(ServerLevel level, @Nullable LivingEntity target, int ticks) {
        if (ticks < 24 && target != null) {
            this.rememberTarget(target);
        }

        Vec3 strikePos = this.attackTarget();
        this.steerToward(strikePos.add(0.0, 13.0, 0.0), 0.38, 0.15);
        if (ticks < LIGHTNING_STRIKE_TICK && ticks % 2 == 0) {
            double spread = Math.max(0.15, 2.8 * (1.0 - ticks / (double)LIGHTNING_STRIKE_TICK));
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    strikePos.x,
                    strikePos.y + 0.2,
                    strikePos.z,
                    8,
                    spread,
                    0.35,
                    spread,
                    0.04
            );
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    this.getX(),
                    this.getEyeY(),
                    this.getZ(),
                    3,
                    0.65,
                    0.45,
                    0.65,
                    0.01
            );
        }

        if (ticks == LIGHTNING_STRIKE_TICK) {
            this.releaseLightningStrike(level, strikePos);
        }
        if (ticks >= LIGHTNING_DURATION) {
            this.finishAttack();
        }
    }

    private void releaseLightningStrike(ServerLevel level, Vec3 strikePos) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setPos(strikePos);
            // Keep the full lightning presentation without allowing a boss
            // attack to ignite terrain or transform unrelated mobs.
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        DamageSource lightningDamage = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, bolt, this);
        AABB strikeArea = AABB.ofSize(strikePos.add(0.0, 2.0, 0.0), LIGHTNING_RADIUS * 2.0, 7.0, LIGHTNING_RADIUS * 2.0);
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class,
                strikeArea,
                entity -> entity != this && !(entity instanceof DragonBossEntity) && entity.isAlive() && !this.isAlliedTo(entity)
        )) {
            double horizontalDistanceSqr = victim.position().subtract(strikePos).horizontalDistanceSqr();
            if (horizontalDistanceSqr <= LIGHTNING_RADIUS * LIGHTNING_RADIUS) {
                victim.hurtServer(level, lightningDamage, 24.0F);
            }
        }
    }

    boolean isSecondStage() {
        return this.getHealth() < this.getMaxHealth() * SECOND_STAGE_HEALTH_FRACTION;
    }

    private int attackForCurrentStage(int attack) {
        if (attack == ATTACK_SONIC_BOOM && !this.isSecondStage()) {
            return ATTACK_LIGHTNING;
        }
        if (attack == ATTACK_LIGHTNING && this.isSecondStage()) {
            return ATTACK_SONIC_BOOM;
        }
        return attack;
    }

    private void rememberTarget(LivingEntity target) {
        this.rememberTarget(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
    }

    private void rememberTarget(Vec3 target) {
        this.attackTargetX = target.x;
        this.attackTargetY = target.y;
        this.attackTargetZ = target.z;
    }

    private Vec3 attackTarget() {
        return new Vec3(this.attackTargetX, this.attackTargetY, this.attackTargetZ);
    }

    private void steerToward(Vec3 destination, double maxSpeed, double responsiveness) {
        Vec3 offset = destination.subtract(this.position());
        if (offset.lengthSqr() < 1.0E-6) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            return;
        }
        Vec3 desiredVelocity = offset.normalize().scale(Math.min(maxSpeed, 0.08 + offset.length() * 0.045));
        this.setDeltaMovement(this.getDeltaMovement().lerp(desiredVelocity, responsiveness));
    }

    private void faceMovement() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() <= 1.0E-5) {
            return;
        }
        double horizontal = movement.horizontalDistance();
        float yaw = (float)(Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float)(-(Mth.atan2(movement.y, horizontal) * Mth.RAD_TO_DEG));
        this.setYRot(Mth.rotLerp(0.22F, this.getYRot(), yaw));
        this.setXRot(Mth.rotLerp(0.18F, this.getXRot(), pitch));
        this.setYHeadRot(this.getYRot());
        this.yBodyRot = this.getYRot();
    }

    private void finishAttack() {
        this.entityData.set(DATA_ATTACK_STATE, ATTACK_IDLE);
        this.entityData.set(DATA_ATTACK_TICKS, 0);
        this.attackCooldown = 55 + this.random.nextInt(46);
        this.hitEntitiesThisAttack.clear();
    }

    @Override
    public void travel(Vec3 input) {
        this.setNoGravity(true);
        this.travelFlying(input, 0.02F);
    }

    @Override
    protected void checkFallDamage(double ySpeed, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
            this.discard();
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof DragonBossEntity) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    public int getAttackState() {
        return this.entityData.get(DATA_ATTACK_STATE);
    }

    /** Elapsed ticks within the current attack; zero while idle. */
    public int getAttackTicks() {
        return this.entityData.get(DATA_ATTACK_TICKS);
    }
}
