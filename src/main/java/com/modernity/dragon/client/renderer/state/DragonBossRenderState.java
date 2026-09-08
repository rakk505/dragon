package com.modernity.dragon.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Snapshot of server-synchronised boss state consumed by the render thread. */
public final class DragonBossRenderState extends LivingEntityRenderState {
    public int attackState;
    public float attackTicks;
    public float flightSpeed;
    public float verticalSpeed;
}
