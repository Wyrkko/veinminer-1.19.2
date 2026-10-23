package com.jupkke.veinminer.client;

import com.jupkke.veinminer.VeinConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Edits a whitelist/blacklist: add by typing, by holding a block, or by looking at one. */
public class BlockListScreen extends Screen {
    private static final int LIST_TOP = 34;
    private static final int ROW = 22;
    private static final String HINT = "modid:block, #tag or modid:*";

    private final Screen parent;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> value;
    private final List<String> entries;

    private int page = 0;
    private int perPage = 4;
    private String input = "";
    private Component status = null;
    private EditBox box;

    public BlockListScreen(Screen parent, String name, ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        super(Component.literal(name));
        this.parent = parent;
        this.value = value;
        this.entries = new ArrayList<>(value.get());
    }

    private int pageCount() {
        return Math.max(1, (entries.size() + perPage - 1) / perPage);
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        int x0 = this.width / 2 - 130;
        perPage = Math.max(1, (this.height - 98 - LIST_TOP - 6) / ROW);
        int pages = pageCount();
        page = Mth.clamp(page, 0, pages - 1);

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < entries.size(); i++) {
            final int index = start + i;
            addRenderableWidget(new Button(x0 + 240, LIST_TOP + i * ROW, 20, 20, Component.literal("X"), b -> {
                entries.remove(index);
                commit();
                rebuild();
            }));
        }

        int navY = this.height - 98;
        Button prev = new Button(x0, navY, 30, 20, Component.literal("<"), b -> {
            page--;
            rebuild();
        });
        prev.active = page > 0;
        addRenderableWidget(prev);

        Button next = new Button(x0 + 230, navY, 30, 20, Component.literal(">"), b -> {
            page++;
            rebuild();
        });
        next.active = page < pages - 1;
        addRenderableWidget(next);

        int inY = this.height - 74;
        box = new EditBox(this.font, x0, inY, 190, 20, Component.literal("Block id"));
        box.setMaxLength(128);
        box.setValue(input);
        box.setResponder(s -> {
            input = s;
            box.setSuggestion(s.isEmpty() ? HINT : "");
        });
        box.setSuggestion(input.isEmpty() ? HINT : "");
        addRenderableWidget(box);
        setInitialFocus(box);

        addRenderableWidget(new Button(x0 + 194, inY, 66, 20, Component.literal("Add"), b -> addEntry(input)));

        int quickY = this.height - 50;
        addRenderableWidget(new Button(x0, quickY, 128, 20, Component.literal("+ Held block"), b -> addHeld()));
        addRenderableWidget(new Button(x0 + 132, quickY, 128, 20, Component.literal("+ Looked-at block"), b -> addLookedAt()));

        addRenderableWidget(new Button(this.width / 2 - 100, this.height - 26, 200, 20,
                CommonComponents.GUI_DONE, b -> onClose()));
    }

    private void commit() {
        value.set(new ArrayList<>(entries));
        try {
            VeinConfig.SPEC.save();
        } catch (Exception ignored) {
        }
    }

    private void fail(String text) {
        status = Component.literal(text).withStyle(ChatFormatting.RED);
    }

    private static String normalize(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) return null;
        boolean tag = s.startsWith("#");
        if (tag) s = s.substring(1);
        if (s.endsWith(":*")) {
            if (tag) return null;
            String ns = s.substring(0, s.length() - 2);
            return ResourceLocation.tryParse(ns + ":a") != null ? ns + ":*" : null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(s);
        if (rl == null) return null;
        return (tag ? "#" : "") + rl;
    }

    private void addEntry(String raw) {
        String norm = normalize(raw);
        if (norm == null) {
            fail("That isn't a valid id.");
            return;
        }
        boolean plainBlock = !norm.startsWith("#") && !norm.endsWith(":*");
        if (plainBlock && !ForgeRegistries.BLOCKS.containsKey(ResourceLocation.tryParse(norm))) {
            fail("Unknown block: " + norm);
            return;
        }
        if (entries.contains(norm)) {
            fail("Already in the list.");
            return;
        }
        entries.add(norm);
        commit();
        status = Component.literal("Added " + norm).withStyle(ChatFormatting.GREEN);
        input = "";
        page = pageCount() - 1;
        rebuild();
    }

    private void addHeld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            fail("Join a world first.");
            return;
        }
        if (mc.player.getMainHandItem().getItem() instanceof BlockItem item) {
            addEntry(String.valueOf(ForgeRegistries.BLOCKS.getKey(item.getBlock())));
        } else {
            fail("Hold a block in your main hand.");
        }
    }

    private void addLookedAt() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            fail("Join a world first.");
            return;
        }
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            var block = mc.level.getBlockState(hit.getBlockPos()).getBlock();
            addEntry(String.valueOf(ForgeRegistries.BLOCKS.getKey(block)));
        } else {
            fail("You aren't looking at a block.");
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && box != null && box.isFocused()) {
            addEntry(input);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        Component line = status != null
                ? status
                : Component.literal("Blocks, #tags, or modid:* for a whole mod").withStyle(ChatFormatting.GRAY);
        drawCenteredString(poseStack, this.font, line, this.width / 2, 21, 0xFFFFFF);

        int x0 = this.width / 2 - 130;
        if (entries.isEmpty()) {
            drawCenteredString(poseStack, this.font, "Nothing here yet", this.width / 2, LIST_TOP + 6, 0x888888);
        } else {
            int start = page * perPage;
            for (int i = 0; i < perPage && start + i < entries.size(); i++) {
                String text = this.font.plainSubstrByWidth(entries.get(start + i), 232);
                drawString(poseStack, this.font, text, x0 + 2, LIST_TOP + i * ROW + 6, 0xFFFFFF);
            }
        }

        drawCenteredString(poseStack, this.font, (page + 1) + " / " + pageCount(),
                this.width / 2, this.height - 98 + 6, 0xAAAAAA);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
