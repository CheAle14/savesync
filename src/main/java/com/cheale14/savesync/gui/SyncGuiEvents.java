package com.cheale14.savesync.gui;

import com.cheale14.savesync.SaveSync;
import com.cheale14.savesync.SaveSync.SaveConfig;
import com.cheale14.savesync.gui.SyncProgressGui.SyncType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

// Listens for Main Menu and writes text if didn't load properly
@Mod.EventBusSubscriber
public class SyncGuiEvents {
	@SubscribeEvent
	public static void Render(InitGuiEvent.Pre e) {
		GuiScreen gui = e.getGui();
		if(gui == null)
			return;
		SaveSync.logger.info("Init.Pre: " + gui.getClass().getName());
		if(gui instanceof GuiMainMenu) {
			renderMainMenu((GuiMainMenu)gui);
		} else if(gui instanceof GuiModList) {
			renderModList((GuiModList)gui);
		} else if(gui instanceof GuiWorldSelection) {
			renderSPScreen((GuiWorldSelection)gui);
		} else if(gui instanceof GuiIngameMenu) {
			if(gui instanceof SyncReplaceIngameMenu) {
				return;
			}
			renderIngameMenu((GuiIngameMenu)gui);
		}
		
	}
	
	static ModContainer findModIndex() {
		for (ModContainer mod : Loader.instance().getModList())
        {
			if(mod.getModId().equals(SaveSync.MODID))
				return mod;
        }
		return null;
	}
	
	static void renderIngameMenu(GuiIngameMenu igm) {
		igm.mc.displayGuiScreen(new SyncReplaceIngameMenu());
	}
	
	static boolean didSync = false;
	static void renderSPScreen(GuiWorldSelection sp) {
		SaveSync.logger.info("DidSync: " + didSync);
		if(didSync) {
			// We have just come from the syncing screen, so proceed as normal.
			didSync = false;
		} else {
			// We have not synced, so need to do so.
			didSync = true;
			SaveSync.logger.info("Not synced, opening progress GUI to sync..");
			SyncProgressGui syncGui = new SyncProgressGui(sp, SyncType.DOWNLOAD_ALL);
			sp.mc.displayGuiScreen(syncGui);
		}
	}
	
	static void renderModList(GuiModList ml) {
		if(SaveSync.SaveConfig.API_Key == null || SaveSync.SaveConfig.API_Key.equals("none")) {
			SaveSync.logger.info("Must set config");
			ModContainer mod = findModIndex();
            IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(mod);
            GuiScreen newScreen = guiFactory.createConfigGui(ml);
            ml.mc.displayGuiScreen(newScreen);
		}
	}
	
	static void renderMainMenu(GuiMainMenu mm) {
		FontRenderer renderer = mm.mc.fontRenderer;
		String text = "";
		int colour = 0;
		if(SaveSync.loadedSync) {
			text = "Sync loaded";
		} else {
			colour = 4;
			text = "Sync: Failed to load.";
		}
		if(SaveSync.SaveConfig.API_Key == null || SaveSync.SaveConfig.API_Key.equals("none")) {
			SaveSync.logger.info("Trying to show config... let's see how this works");
			GuiModList mods = new GuiModList(mm);
			mm.mc.displayGuiScreen(mods);
			

			
			return;
		}
		renderer.drawString(text, 1, 1, colour);
	}
}
