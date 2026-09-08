package com.modernity.dragon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.modernity.dragon.entity.DragonBossEntity;
import com.modernity.dragon.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Registers the operator command: {@code /dragon-boss [x y z]}. */
public final class DragonBossCommand {
    private static final SimpleCommandExceptionType ERROR_INVALID_POSITION =
            new SimpleCommandExceptionType(Component.translatable("commands.dragon_boss.invalid_position"));
    private static final SimpleCommandExceptionType ERROR_SPAWN_FAILED =
            new SimpleCommandExceptionType(Component.translatable("commands.dragon_boss.failed"));

    private DragonBossCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dragon-boss")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> spawn(context.getSource(), context.getSource().getPosition()))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> spawn(context.getSource(), Vec3Argument.getVec3(context, "pos"))))
        );
    }

    private static int spawn(CommandSourceStack source, Vec3 position) throws CommandSyntaxException {
        BlockPos blockPos = BlockPos.containing(position);
        if (!Level.isInSpawnableBounds(blockPos)) {
            throw ERROR_INVALID_POSITION.create();
        }

        ServerLevel level = source.getLevel();
        DragonBossEntity boss = ModEntities.DRAGON_BOSS.get().create(
                level,
                created -> {
                    created.snapTo(position.x, position.y, position.z, source.getRotation().y, 0.0F);
                    created.setPersistenceRequired();
                },
                blockPos,
                EntitySpawnReason.COMMAND,
                false,
                false
        );
        if (boss == null) {
            throw ERROR_SPAWN_FAILED.create();
        }
        if (!level.tryAddFreshEntityWithPassengers(boss)) {
            throw ERROR_SPAWN_FAILED.create();
        }

        source.sendSuccess(() -> Component.translatable("commands.dragon_boss.success", boss.getDisplayName()), true);
        return 1;
    }
}
