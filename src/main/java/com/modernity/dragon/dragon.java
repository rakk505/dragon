package com.modernity.dragon;

import com.modernity.dragon.command.DragonBossCommand;
import com.modernity.dragon.entity.DragonBossEntity;
import com.modernity.dragon.entity.DragonBossGameTests;
import com.modernity.dragon.registry.ModEntities;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge entry point for the Celestial Dragon boss. */
@Mod(dragon.MODID)
public class dragon {
    public static final String MODID = "dragon";

    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRAGON_BOSS_SPAWN_TEST =
            TEST_FUNCTIONS.register("dragon_boss_spawn", () -> DragonBossGameTests::spawnHasBossStatsAndFlight);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRAGON_BOSS_COIL_TEST =
            TEST_FUNCTIONS.register("dragon_boss_coil", () -> DragonBossGameTests::coilUltimateDamagesNearbyTargets);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRAGON_BOSS_PHASE_TEST =
            TEST_FUNCTIONS.register("dragon_boss_phase", () -> DragonBossGameTests::specialAttacksRespectHalfHealthBoundary);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRAGON_BOSS_LIGHTNING_TEST =
            TEST_FUNCTIONS.register("dragon_boss_lightning", () -> DragonBossGameTests::lightningAttackDamagesTarget);

    public dragon(IEventBus modEventBus, ModContainer ignoredContainer) {
        TEST_FUNCTIONS.register(modEventBus);
        ModEntities.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::registerGameTests);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::registerSpawnPlacements);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        DragonBossCommand.register(event.getDispatcher());
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DRAGON_BOSS.get(), DragonBossEntity.createAttributes().build());
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.DRAGON_BOSS.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DragonBossEntity::checkDragonBossSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> bossEnvironment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "boss_tests"),
                new TestEnvironmentDefinition.AllOf(List.of())
        );

        TestData<Holder<TestEnvironmentDefinition<?>>> bossData = new TestData<>(
                bossEnvironment,
                Identifier.fromNamespaceAndPath("minecraft", "empty"),
                180,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                16
        );

        event.registerTest(
                Identifier.fromNamespaceAndPath(MODID, "dragon_boss_spawn"),
                new FunctionGameTestInstance(DRAGON_BOSS_SPAWN_TEST.getKey(), bossData)
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(MODID, "dragon_boss_coil"),
                new FunctionGameTestInstance(DRAGON_BOSS_COIL_TEST.getKey(), bossData)
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(MODID, "dragon_boss_phase"),
                new FunctionGameTestInstance(DRAGON_BOSS_PHASE_TEST.getKey(), bossData)
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(MODID, "dragon_boss_lightning"),
                new FunctionGameTestInstance(DRAGON_BOSS_LIGHTNING_TEST.getKey(), bossData)
        );
    }
}
