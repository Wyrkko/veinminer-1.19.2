package com.jupkke.veinminer;

import com.jupkke.veinminer.client.ClientInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(JupkkeVeinminer.MODID)
public class JupkkeVeinminer {
    public static final String MODID = "jupkke_veinminer";

    public JupkkeVeinminer() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, VeinConfig.SPEC, "jupkke_veinminer-common.toml");
        ctx.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "jupkke_veinminer-client.toml");

        modBus.addListener(this::commonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init(modBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }
}
