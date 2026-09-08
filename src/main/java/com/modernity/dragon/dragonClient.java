package com.modernity.dragon;

import com.modernity.dragon.client.model.DragonBossModel;
import com.modernity.dragon.client.renderer.DragonBossRenderer;
import com.modernity.dragon.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registration for the dragon model layer and renderer. */
@Mod(value = dragon.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = dragon.MODID, value = Dist.CLIENT)
public class dragonClient {
    public dragonClient(ModContainer ignoredContainer) {
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DragonBossRenderer.MODEL_LAYER, DragonBossModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRAGON_BOSS.get(), DragonBossRenderer::new);
    }
}
