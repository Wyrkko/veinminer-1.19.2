package com.jupkke.veinminer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = JupkkeVeinminer.MODID)
public final class VeinHandler {
    /** Players that have told us whether their veinmine is currently active. */
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
    /** Players currently inside a veinmine (prevents recursion). */
    private static final Set<UUID> MINING = ConcurrentHashMap.newKeySet();

    private VeinHandler() {
    }

    public static void setActive(ServerPlayer player, boolean active) {
        ACTIVE.put(player.getUUID(), active);
    }

    private static boolean isActive(ServerPlayer player) {
        Boolean state = ACTIVE.get(player.getUUID());
        return state != null ? state : player.isShiftKeyDown();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
        MINING.remove(event.getEntity().getUUID());
    }

    // LOWEST so protection mods that cancel the break get the final say first.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player p = event.getPlayer();
        if (!(p instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        UUID id = player.getUUID();
        if (MINING.contains(id) || !isActive(player)) return;

        BlockState state = event.getState();
        if (!VeinConfig.isVeinMineable(state)) return;

        boolean creative = player.isCreative();
        boolean needTool = VeinConfig.REQUIRE_CORRECT_TOOL.get() && !creative;
        if (needTool && !player.hasCorrectToolForDrops(state)) return;

        List<BlockPos> targets = VeinFinder.find(level, event.getPos(), state, VeinConfig.MAX_VEIN_SIZE.get() - 1);
        if (targets.isEmpty()) return;

        boolean protectTool = VeinConfig.PREVENT_TOOL_BREAK.get() && !creative;
        double extra = VeinConfig.EXTRA_EXHAUSTION.get();

        MINING.add(id);
        try {
            for (BlockPos pos : targets) {
                BlockState target = level.getBlockState(pos);
                if (target.isAir()) continue;
                if (needTool && !player.hasCorrectToolForDrops(target)) break;

                if (protectTool) {
                    ItemStack tool = player.getMainHandItem();
                    // Keep 1 use for the block you actually mined, and 1 so the tool survives.
                    if (tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= 2) break;
                }

                if (player.gameMode.destroyBlock(pos) && extra > 0) {
                    player.causeFoodExhaustion((float) extra);
                }
            }
        } finally {
            MINING.remove(id);
        }
    }
}
