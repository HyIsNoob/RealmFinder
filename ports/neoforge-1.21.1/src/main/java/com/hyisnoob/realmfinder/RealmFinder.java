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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RealmFinder.MOD_ID)
public final class RealmFinder {
    public static final String MOD_ID = "realmfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public RealmFinder(IEventBus modBus) {
        RealmFinderConfig.load();
        modBus.addListener(ModItems::register);
        modBus.addListener(ModBlocks::register);
        modBus.addListener(ModMenus::register);
        modBus.addListener(ModCreativeTabs::register);
        modBus.addListener(ModPackets::registerPayloads);
        NeoForge.EVENT_BUS.addListener(RealmFinder::captureBlock);
        NeoForge.EVENT_BUS.addListener(RealmFinder::captureEntity);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> UndoManager.clear());
        NeoForge.EVENT_BUS.addListener(ModCommands::register);
        ModPackets.registerServerEvents();
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
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
