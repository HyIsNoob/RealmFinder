package com.hyisnoob.realmfinder;

import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.common.network.ModPackets;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmFinder implements ModInitializer {
	public static final String MOD_ID = "realmfinder";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing RealmFinder (Viewfinder Mod)...");
		ModItems.register();
		com.hyisnoob.realmfinder.common.block.ModBlocks.register();
		com.hyisnoob.realmfinder.common.menu.ModMenus.register();
		ModPackets.registerCommon();
		ModPackets.registerServerReceivers();
		com.hyisnoob.realmfinder.common.command.ModCommands.register();
		LOGGER.info("RealmFinder initialized successfully!");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
