package com.jupkke.veinminer;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player settings (stored in the client config). */
public final class ClientConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = B
            .comment("Master switch for your own veinmining.")
            .define("enabled", true);

    public static final ForgeConfigSpec.EnumValue<ActivationMode> MODE = B
            .comment("How veinmining is activated: SNEAK, ALWAYS or KEYBIND.")
            .defineEnum("activationMode", ActivationMode.SNEAK);

    public static final ForgeConfigSpec SPEC = B.build();

    private ClientConfig() {
    }
}
