package com.jupkke.veinminer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class VeinFinder {
    private VeinFinder() {
    }

    /** Flood fill from origin. Returns up to {@code limit} connected blocks (origin not included). */
    public static List<BlockPos> find(ServerLevel level, BlockPos origin, BlockState originState, int limit) {
        List<BlockPos> result = new ArrayList<>();
        if (limit <= 0) return result;

        boolean merge = VeinConfig.MERGE_VARIANTS.get();
        boolean diagonals = VeinConfig.DIAGONALS.get();
        String key = VeinConfig.veinKey(originState, merge);

        LongOpenHashSet seen = new LongOpenHashSet();
        seen.add(origin.asLong());
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (!diagonals && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;

                        BlockPos next = cur.offset(dx, dy, dz);
                        if (!seen.add(next.asLong())) continue;
                        if (!level.isLoaded(next)) continue;

                        BlockState s = level.getBlockState(next);
                        if (s.isAir() || s.getDestroySpeed(level, next) < 0) continue;

                        boolean same = s.getBlock() == originState.getBlock()
                                || VeinConfig.veinKey(s, merge).equals(key);
                        if (!same || !VeinConfig.isVeinMineable(s)) continue;

                        result.add(next);
                        if (result.size() >= limit) return result;
                        queue.add(next);
                    }
                }
            }
        }
        return result;
    }
}
