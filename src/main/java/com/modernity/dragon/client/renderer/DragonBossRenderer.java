package com.modernity.dragon.client.renderer;

import com.modernity.dragon.client.model.DragonBossModel;
import com.modernity.dragon.client.renderer.state.DragonBossRenderState;
import com.modernity.dragon.dragon;
import com.modernity.dragon.entity.DragonBossEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public final class DragonBossRenderer extends MobRenderer<DragonBossEntity, DragonBossRenderState, DragonBossModel> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(dragon.MODID, "chinese_dragon_boss"),
            "main"
    );
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            dragon.MODID,
            "textures/entity/chinese_dragon_boss.png"
    );

    public DragonBossRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonBossModel(context.bakeLayer(MODEL_LAYER)), 2.5F);
    }

    @Override
    public Identifier getTextureLocation(DragonBossRenderState state) {
        return TEXTURE;
    }

    @Override
    public DragonBossRenderState createRenderState() {
        return new DragonBossRenderState();
    }

    @Override
    public void extractRenderState(DragonBossEntity entity, DragonBossRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackState = entity.getAttackState();
        state.attackTicks = state.attackState == DragonBossEntity.ATTACK_IDLE
                ? 0.0F
                : entity.getAttackTicks() + partialTicks;
        state.flightSpeed = (float) entity.getDeltaMovement().horizontalDistance();
        state.verticalSpeed = (float) entity.getDeltaMovement().y;
    }

    @Override
    protected int getBlockLightLevel(DragonBossEntity entity, BlockPos blockPos) {
        int attack = entity.getAttackState();
        if (attack == DragonBossEntity.ATTACK_FIREBALL
                || attack == DragonBossEntity.ATTACK_SONIC_BOOM
                || attack == DragonBossEntity.ATTACK_LIGHTNING) {
            return 15;
        }
        return super.getBlockLightLevel(entity, blockPos);
    }

    @Override
    protected AABB getBoundingBoxForCulling(DragonBossEntity entity) {
        // The articulated visual is roughly fifteen blocks from whiskers to tail fan.
        return entity.getBoundingBox().inflate(9.0, 4.0, 9.0);
    }
}
