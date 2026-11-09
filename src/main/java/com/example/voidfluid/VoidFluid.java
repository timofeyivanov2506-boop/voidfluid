package com.example.voidfluid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Удаляет ВСЮ лаву (источники и потоки) во всех загруженных чанках
 * вокруг игроков, во всех измерениях.
 */
public class VoidFluid implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("voidfluid");

    /** Радиус сканирования в чанках вокруг каждого игрока. */
    public static final int SCAN_CHUNKS = 8;
    /** Как часто сканировать (в тиках). 5 тиков = 4 раза в секунду. */
    public static final int INTERVAL = 5;

    @Override
    public void onInitialize() {
        LOGGER.info("VoidFluid: lava purge loaded");
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % INTERVAL != 0) return;
            for (ServerPlayerEntity player : world.getPlayers()) {
                ChunkPos c = player.getChunkPos();
                for (int dx = -SCAN_CHUNKS; dx <= SCAN_CHUNKS; dx++) {
                    for (int dz = -SCAN_CHUNKS; dz <= SCAN_CHUNKS; dz++) {
                        WorldChunk chunk = world.getChunkManager().getWorldChunk(c.x + dx, c.z + dz);
                        if (chunk != null) purgeLava(world, chunk);
                    }
                }
            }
        });
    }

    private static void purgeLava(ServerWorld world, WorldChunk chunk) {
        ChunkSection[] sections = chunk.getSectionArray();
        int baseSection = chunk.getBottomSectionCoord();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            // быстрая проверка: есть ли в секции лава вообще
            if (section == null || section.isEmpty()) continue;
            if (!section.hasAny(s -> s.isOf(Blocks.LAVA))) continue;

            int y0 = (baseSection + i) << 4;
            int x0 = chunk.getPos().getStartX();
            int z0 = chunk.getPos().getStartZ();
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        pos.set(x0 + x, y0 + y, z0 + z);
                        BlockState state = chunk.getBlockState(pos);
                        if (state.isOf(Blocks.LAVA)) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState(),
                                    Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }
    }
}
