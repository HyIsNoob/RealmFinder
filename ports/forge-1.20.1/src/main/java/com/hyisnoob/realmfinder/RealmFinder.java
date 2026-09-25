package com.hyisnoob.realmfinder;

import com.hyisnoob.realmfinder.common.block.ModBlocks;
import com.hyisnoob.realmfinder.common.command.ModCommands;
import com.hyisnoob.realmfinder.common.item.CameraItem;
import com.hyisnoob.realmfinder.common.item.ModCreativeTabs;
import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.common.menu.ModMenus;
import com.hyisnoob.realmfinder.common.network.ModPackets;
import com.hyisnoob.realmfinder.client.RealmFinderClient;
import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;
import com.hyisnoob.realmfinder.core.engine.UndoManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RealmFinder.MOD_ID)
public final class RealmFinder {
    public static final String MOD_ID = "realmfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public RealmFinder() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        RealmFinderConfig.load();
        modBus.addListener(ModItems::register);
        modBus.addListener(ModBlocks::register);
        modBus.addListener(ModMenus::register);
        modBus.addListener(ModCreativeTabs::register);
        ModPackets.registerCommon();
        MinecraftForge.EVENT_BUS.addListener(RealmFinder::captureBlock);
        MinecraftForge.EVENT_BUS.addListener(RealmFinder::captureEntity);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> UndoManager.clear());
        MinecraftForge.EVENT_BUS.addListener(ModCommands::register);
        ModPackets.registerServerReceivers();
        if (FMLEnvironment.dist == Dist.CLIENT) RealmFinderClient.register(modBus);
    }

    private static void captureBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof CameraItem camera)) return;
        camera.capture(event.getLevel(), event.getEntity(), event.getItemStack());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void captureEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getItemStack().getItem() instanceof CameraItem camera)) return;
        camera.capture(event.getLevel(), event.getEntity(), event.getItemStack());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
