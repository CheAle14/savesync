package com.cheale14.savesync.client;

import com.cheale14.savesync.common.CommonProxy;
import com.cheale14.savesync.common.Icon;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.SaveSync.SaveConfig;
import com.cheale14.savesync.common.SyncThread;
import com.mojang.realmsclient.dto.RealmsServer.McoServerComparator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.lang3.StringUtils;

import com.cheale14.savesync.client.gui.SyncLoginGui;
import com.cheale14.savesync.client.gui.SyncProgressGui;
import com.cheale14.savesync.client.gui.SyncProgressGui.SyncType;
import com.cheale14.savesync.client.gui.SyncReplaceGuiMP;
import com.cheale14.savesync.client.gui.SyncReplaceIngameMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class ClientProxy extends CommonProxy {
	@SubscribeEvent
	public void Render(InitGuiEvent.Pre e) {
		GuiScreen gui = e.getGui();
		if(gui == null)
			return;
		logger.info("Init.Pre: " + gui.getClass().getName());
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
		} else if(gui instanceof GuiMultiplayer) {
			if(gui instanceof SyncReplaceGuiMP)
				return;
			renderMultiplayerMenu((GuiMultiplayer)gui);
		} 
		
	}
	
	ModContainer findModIndex() {
		for (ModContainer mod : Loader.instance().getModList())
        {
			if(mod.getModId().equals(SaveSync.MODID))
				return mod;
        }
		return null;
	}
	
	void renderIngameMenu(GuiIngameMenu igm) {
		if(Minecraft.getMinecraft().isIntegratedServerRunning())
			igm.mc.displayGuiScreen(new SyncReplaceIngameMenu());
	}
	
	void renderMultiplayerMenu(GuiMultiplayer mp) {
		if(SaveConfig.SyncServerConnect) {
			mp.mc.displayGuiScreen(new SyncReplaceGuiMP(null));
		}
	}
	
	boolean didSync = false;
	void renderSPScreen(GuiWorldSelection sp) {
		try {
			if(SaveSync.GetCurrentUser() == null) {
				logger.info("API key gives null user, might be missing - forcing login");
				SaveConfig.API_Key = "none";
				sp.mc.displayGuiScreen(new SyncLoginGui(sp));
				return;
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.warn("API key results in error; forcing re-login");
			SaveConfig.API_Key = "none";
			sp.mc.displayGuiScreen(new SyncLoginGui(sp));
			return;
		}
		
		logger.info("DidSync: " + didSync);
		if(didSync) {
			// We have just come from the syncing screen, so proceed as normal.
			didSync = false;
		} else {
			// We have not synced, so need to do so.
			didSync = true;
			logger.info("Not synced, opening progress GUI to sync..");
			SyncProgressGui syncGui = new SyncProgressGui(sp, SyncType.DOWNLOAD_ALL);
			sp.mc.displayGuiScreen(syncGui);
		}
	}
	


	void renderModList(GuiModList ml) {
	}
	
	void renderMainMenu(GuiMainMenu mm) {
		FontRenderer renderer = mm.mc.fontRenderer;
		String text = "";
		int colour = 0;
		if(SaveSync.loadedSync) {
			text = "Sync loaded";
		} else {
			colour = 4;
			text = "Sync: Failed to load.";
		}
		renderer.drawString(text, 1, 1, colour);
	}

	
	static SyncProgressGui syncGui;


	@Override
	public void backupDone() {
		if(syncGui != null) {
			if(syncGui.closed) {
				syncGui = null;
			} else {
				logger.info("Previous syncGui has not closed?");
				logger.info("We'll stop - something's going on here.");
				return;
			}
		}
		logger.info("Automatically syncing as backup has been done.");
		Minecraft mc = Minecraft.getMinecraft();
		GuiScreen parent = mc.currentScreen;
		SyncProgressGui gui = new SyncProgressGui(parent, SyncType.UPLOAD_ALL, true);
		mc.displayGuiScreen(gui);
	}
	
	@Override
	public List<File> GetSyncFolders() {
    	File saveFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves");
    	List<File> files = new LinkedList<File>();
    	for(File worldFolder : saveFolder.listFiles()) {
    		if(SaveSync.IsSyncFolder(worldFolder)) {
        		logger.info(worldFolder.getAbsolutePath());
        		files.add(worldFolder);
    		}
    	}
    	return files;
    }
	
	@Override
	public File GetDefaultBranchFolder() {
		return new File(GetSaveFolder(), SaveConfig.RepositoryName + "-main");
	}
	
	public void serverStopped(FMLServerStoppedEvent event) {
		logger.info("Server stopped. As this is the client, we've already synced via GUI.");
		
	}
}
