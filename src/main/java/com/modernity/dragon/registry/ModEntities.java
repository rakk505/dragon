package com.modernity.dragon.registry;

import com.modernity.dragon.dragon;
import com.modernity.dragon.entity.DragonBossEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity registrations shared by the logical client and dedicated server. */
public final class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(dragon.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DragonBossEntity>> DRAGON_BOSS =
            ENTITY_TYPES.registerEntityType(
                    "dragon_boss",
                    DragonBossEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(3.5F, 3.5F)
                            .eyeHeight(2.8F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .canSpawnFarFromPlayer()
                            .fireImmune()
                            .notInPeaceful()
            );

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
