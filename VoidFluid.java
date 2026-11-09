package com.example.voidfluid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

public class VoidFluid implements ModInitializer {
    /** Радиус очистки вокруг игрока (в блоках). */
    public static final int RADIUS = 3;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isSpectator()) continue;
                if (isFullySubmerged(world, player)) {
                    clearFluid(world, player.getBlockPos());
                }
            }
        });
    }

    private static boolean isFullySubmerged(ServerWorld world, PlayerEntity player) {
        BlockPos eyes = BlockPos.ofFloored(player.getEyePos());
        BlockPos feet = player.getBlockPos();
        return isWaterOrLava(world, eyes) && isWaterOrLava(world, feet);
    }

    private static boolean isWaterOrLava(ServerWorld world, BlockPos pos) {
        FluidState fs = world.getFluidState(pos);
        return fs.isIn(FluidTags.WATER) || fs.isIn(FluidTags.LAVA);
    }

    private static void clearFluid(ServerWorld world, BlockPos center) {
        BlockPos min = center.add(-RADIUS, -RADIUS, -RADIUS);
        BlockPos max = center.add(RADIUS, RADIUS, RADIUS);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            FluidState fs = world.getFluidState(pos);
            if (fs.isEmpty()) continue;
            if (!(fs.isIn(FluidTags.WATER) || fs.isIn(FluidTags.LAVA))) continue;

            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof FluidBlock) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            } else if (state.contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED)) {
                world.setBlockState(pos, state.with(Properties.WATERLOGGED, false), Block.NOTIFY_LISTENERS);
            }
        }
    }
}
