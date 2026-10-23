package com.jupkke.veinminer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.regex.Pattern;

/** Gameplay settings (common config). */
public final class VeinConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    private static final List<String> NONE = List.of();
    private static final Pattern ORE_NAME = Pattern.compile("(^|_)ores?(_|$)");

    public static final ForgeConfigSpec.BooleanValue AUTO_DETECT_TAGS = B
            .comment("Veinmine every block tagged forge:ores (vanilla and modded).")
            .define("autoDetectOres", true);

    public static final ForgeConfigSpec.BooleanValue AUTO_DETECT_NAMES = B
            .comment("Also veinmine blocks whose id contains 'ore' (for mods that forgot to tag their ores).")
            .define("autoDetectByName", true);

    public static final ForgeConfigSpec.BooleanValue INCLUDE_LOGS = B
            .comment("Veinmine logs (trees).")
            .define("includeLogs", false);

    public static final ForgeConfigSpec.BooleanValue INCLUDE_LEAVES = B
            .comment("Veinmine leaves.")
            .define("includeLeaves", false);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST = B
            .comment("Extra blocks to veinmine. Entries: modid:block, #modid:tag, or modid:* for a whole mod.")
            .defineListAllowEmpty("whitelist", NONE, o -> o instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = B
            .comment("Blocks that are never veinmined. Same entry format as the whitelist. Overrides everything.")
            .defineListAllowEmpty("blacklist", NONE, o -> o instanceof String);

    public static final ForgeConfigSpec.IntValue MAX_VEIN_SIZE = B
            .comment("Maximum number of blocks broken per vein (including the one you mined).")
            .defineInRange("maxVeinSize", 64, 1, 512);

    public static final ForgeConfigSpec.DoubleValue EXTRA_EXHAUSTION = B
            .comment("Extra hunger exhaustion per extra block broken (vanilla breaking already costs 0.005).")
            .defineInRange("extraExhaustionPerBlock", 0.0, 0.0, 1.0);

    public static final ForgeConfigSpec.BooleanValue REQUIRE_CORRECT_TOOL = B
            .comment("Only veinmine when your tool is the correct one for the block (e.g. a pickaxe for ores).")
            .define("requireCorrectTool", true);

    public static final ForgeConfigSpec.BooleanValue PREVENT_TOOL_BREAK = B
            .comment("Stop veinmining before your tool would break.")
            .define("preventToolBreak", true);

    public static final ForgeConfigSpec.BooleanValue DIAGONALS = B
            .comment("Include diagonally touching blocks when searching for the vein.")
            .define("includeDiagonals", true);

    public static final ForgeConfigSpec.BooleanValue MERGE_VARIANTS = B
            .comment("Treat ore variants as one vein (e.g. stone iron ore and deepslate iron ore).")
            .define("mergeOreVariants", true);

    public static final ForgeConfigSpec SPEC = B.build();

    private static BlockFilter whiteCache;
    private static BlockFilter blackCache;

    private VeinConfig() {
    }

    private static synchronized BlockFilter whitelist() {
        List<? extends String> raw = WHITELIST.get();
        if (whiteCache == null || !whiteCache.sameSource(raw)) whiteCache = new BlockFilter(raw);
        return whiteCache;
    }

    private static synchronized BlockFilter blacklist() {
        List<? extends String> raw = BLACKLIST.get();
        if (blackCache == null || !blackCache.sameSource(raw)) blackCache = new BlockFilter(raw);
        return blackCache;
    }

    public static boolean isVeinMineable(BlockState state) {
        if (state.isAir()) return false;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;

        if (blacklist().matches(state, id)) return false;
        if (whitelist().matches(state, id)) return true;

        if (AUTO_DETECT_TAGS.get() && state.is(Tags.Blocks.ORES)) return true;
        if (AUTO_DETECT_NAMES.get()
                && ORE_NAME.matcher(id.getPath()).find()
                && !state.is(Tags.Blocks.STORAGE_BLOCKS)) return true;
        if (INCLUDE_LOGS.get() && state.is(BlockTags.LOGS)) return true;
        if (INCLUDE_LEAVES.get() && state.is(BlockTags.LEAVES)) return true;
        return false;
    }

    /** Blocks with the same key belong to the same vein. */
    public static String veinKey(BlockState state, boolean merge) {
        if (merge) {
            String ore = state.getTags()
                    .map(t -> t.location())
                    .filter(l -> l.getNamespace().equals("forge") && l.getPath().startsWith("ores/"))
                    .map(ResourceLocation::toString)
                    .min(String::compareTo)
                    .orElse(null);
            if (ore != null) return ore;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id == null ? "unknown" : id.toString();
    }
}
