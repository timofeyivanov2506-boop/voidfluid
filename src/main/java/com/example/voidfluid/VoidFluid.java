package com.example.voidfluid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

public class VoidFluid implements ModInitializer {
    /** Радиус очистки вокруг игрока (в блоках). */
    public static final int RADIUS = 3;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isSpectator()) continue;
                if (touchesFluid(world, player.getBoundingBox())) {
                    clearFluid(world, player.getBlockPos());
                }
            }
        });
    }

    /** true, если хоть часть тела игрока касается воды или лавы. */
    private static boolean touchesFluid(ServerWorld world, Box box) {
        Box b = box.expand(0.2);
        BlockPos min = BlockPos.ofFloored(b.minX, b.minY, b.minZ);
        BlockPos max = BlockPos.ofFloored(b.maxX, b.maxY, b.maxZ);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (isWaterOrLava(world.getFluidState(pos))) return true;
        }
        return false;
    }

    private static boolean isWaterOrLava(FluidState fs) {
        return !fs.isEmpty() && (fs.isIn(FluidTags.WATER) || fs.isIn(FluidTags.LAVA));
    }

    private static void clearFluid(ServerWorld world, BlockPos center) {
        BlockPos min = center.add(-RADIUS, -RADIUS, -RADIUS);
        BlockPos max = center.add(RADIUS, RADIUS, RADIUS);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (!isWaterOrLava(world.getFluidState(pos))) continue;

            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof FluidBlock) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            } else if (state.contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED)) {
                world.setBlockState(pos, state.with(Properties.WATERLOGGED, false), Block.NOTIFY_LISTENERS);
            }
        }
    }
}
