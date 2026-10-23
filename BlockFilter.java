package com.jupkke.veinminer;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parsed whitelist/blacklist. Entries can be:
 * "modid:block", "#modid:tag", or "modid:*" (every block of a mod).
 */
public final class BlockFilter {
    private final List<String> source;
    private final Set<ResourceLocation> blocks = new HashSet<>();
    private final Set<String> namespaces = new HashSet<>();
    private final Set<TagKey<Block>> tags = new HashSet<>();

    public BlockFilter(List<? extends String> raw) {
        this.source = new ArrayList<>(raw);
        for (String entry : raw) {
            String s = entry.trim();
            if (s.isEmpty()) continue;
            if (s.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(s.substring(1));
                if (rl != null) tags.add(TagKey.create(Registry.BLOCK_REGISTRY, rl));
            } else if (s.endsWith(":*")) {
                namespaces.add(s.substring(0, s.length() - 2));
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl != null) blocks.add(rl);
            }
        }
    }

    public boolean sameSource(List<? extends String> raw) {
        return source.equals(raw);
    }

    public boolean matches(BlockState state, ResourceLocation id) {
        if (blocks.contains(id) || namespaces.contains(id.getNamespace())) return true;
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) return true;
        }
        return false;
    }
}
