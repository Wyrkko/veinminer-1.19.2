package com.jupkke.veinminer.client;

import com.jupkke.veinminer.ActivationMode;
import com.jupkke.veinminer.ActivePacket;
import com.jupkke.veinminer.ClientConfig;
import com.jupkke.veinminer.Network;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Everything client-only lives here (keybind, command, config screen hook, activation sync). */
public final class ClientInit {
    public static final KeyMapping KEY = new KeyMapping(
            "key.jupkke_veinminer.veinmine",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.jupkke_veinminer");

    private static Boolean lastSent = null;
    private static boolean openPending = false;

    private ClientInit() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientInit::onRegisterKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::onLogin);

        // Adds the "Config" button to this mod in the Forge Mods list.
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new VeinConfigScreen(parent)));
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    private static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        lastSent = null;
    }

    private static boolean desired(Minecraft mc) {
        if (!ClientConfig.ENABLED.get()) return false;
        return switch (ClientConfig.MODE.get()) {
            case SNEAK -> mc.player.isShiftKeyDown();
            case ALWAYS -> true;
            case KEYBIND -> KEY.isDown();
        };
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        // /veinminer asks for the screen; open it once the chat box has closed.
        if (openPending && mc.screen == null) {
            openPending = false;
            mc.setScreen(new VeinConfigScreen(null));
        }

        if (mc.player == null || mc.getConnection() == null) {
            lastSent = null;
            return;
        }

        boolean want = desired(mc);
        if (lastSent == null || lastSent != want) {
            lastSent = want;
            Network.CHANNEL.sendToServer(new ActivePacket(want));
        }
    }

    private static void saveClientConfig() {
        try {
            ClientConfig.SPEC.save();
        } catch (Exception ignored) {
        }
    }

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(Component.literal(text), false);
    }

    private static void onRegisterCommands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("veinminer")
                .executes(ctx -> {
                    openPending = true;
                    return 1;
                })
                .then(Commands.literal("config").executes(ctx -> {
                    openPending = true;
                    return 1;
                }))
                .then(Commands.literal("toggle").executes(ctx -> {
                    boolean now = !ClientConfig.ENABLED.get();
                    ClientConfig.ENABLED.set(now);
                    saveClientConfig();
                    say(ctx.getSource(), "Veinminer " + (now ? "enabled" : "disabled"));
                    return 1;
                }));

        LiteralArgumentBuilder<CommandSourceStack> mode = Commands.literal("mode");
        for (ActivationMode m : ActivationMode.values()) {
            mode.then(Commands.literal(m.name().toLowerCase(Locale.ROOT)).executes(ctx -> {
                ClientConfig.MODE.set(m);
                saveClientConfig();
                say(ctx.getSource(), "Veinminer activation: " + m.label());
                return 1;
            }));
        }
        root.then(mode);

        event.getDispatcher().register(root);
    }
}
