package com.cheale14.savesync.client;

import com.cheale14.savesync.common.CommonProxy;
import com.cheale14.savesync.common.Icon;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.SaveSync.SaveConfig;

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

import com.cheale14.savesync.client.gui.SyncProgressGui;
import com.cheale14.savesync.client.gui.SyncProgressGui.SyncType;
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
	
	boolean didSync = false;
	void renderSPScreen(GuiWorldSelection sp) {
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
	
	static boolean didConfig = false;
	void renderModList(GuiModList ml) {
		if(!SaveSync.HasAPIKey()) {
			if(didConfig) {
				logger.info("No API key set, but already displayed config. Not redirecting.");
				return;
			}
			didConfig = true;
			logger.info("Must set config");
			ModContainer mod = findModIndex();
            IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(mod);
            GuiScreen newScreen = guiFactory.createConfigGui(ml);
            ml.mc.displayGuiScreen(newScreen);
		}
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
		if(!SaveSync.HasAPIKey()) {
			logger.info("Trying to show config... let's see how this works");
			GuiModList mods = new GuiModList(mm);
			mm.mc.displayGuiScreen(mods);
			return;
		}
		renderer.drawString(text, 1, 1, colour);
	}
	
	@SubscribeEvent
    public void guiOpened(GuiOpenEvent event) {
    	if(!SaveConfig.SyncServerConnect) {
    		return;
    	}
    		
    	GuiScreen gui = event.getGui();
    	if(gui == null) {
    		logger.info("Opening null gui?");
    		return;
    	}
    	if (!(gui instanceof GuiMultiplayer)) {
    		return;
    	}
    	logger.info("Opening multiplayer screen, getting hamachi server");
    	try {
			AddServer();
		} catch(SSLHandshakeException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
    }
	
	String getServer() throws IOException {
    	URL url = new URL(SaveSync.MLAPI + "/mc/hamIp");
		logger.info("GETing to " + url.toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		
		int code = con.getResponseCode();
		logger.info("Response: " + code);
		Reader streamReader;
		if(code > 299) {
			streamReader = new InputStreamReader(con.getErrorStream());
		} else {
			streamReader = new InputStreamReader(con.getInputStream());
		}
		StringBuffer content = new StringBuffer();
		try(BufferedReader bf = new BufferedReader(streamReader)) {
			String line;
			while((line = bf.readLine()) != null)
				content.append(line);
		}
		if(code > 299) {
			logger.error("Failed GET with " + code + ": " + content);
			return null;
		}
		if(code == 204) {
			return null;
		}
		return content.toString();
    }
	
	public void AddServer() throws IOException {
    	if(!SaveConfig.SyncServerConnect) {
    		return;
    	}
    	String connInfo = getServer();
    	ListTag<CompoundTag> ls = new ListTag<>(CompoundTag.class);
    	if(connInfo == null || connInfo.length() == 0) {
    		logger.info("No server known to be running already.");
    	} else {
        	logger.info("Fetched info: " + connInfo);
	    	CompoundTag serverInfo = new CompoundTag();
	    	serverInfo.putString("icon", Icon.B64);
	    	serverInfo.putString("ip", connInfo);
	    	serverInfo.putString("name", "Omnifactory Hamachi");
	    	ls.add(serverInfo);
    	}
    	CompoundTag innerRoot = new CompoundTag();
    	innerRoot.put("servers", ls);
    	NamedTag root = new NamedTag("", innerRoot);
    	logger.info("Getting file location");
    	
    	File serverDat = new File(Minecraft.getMinecraft().mcDataDir, "servers.dat");
    	logger.info("Writing NBT to " + serverDat.getAbsolutePath());
    	NBTUtil.write(root, serverDat, false); // false -> uncompressed
    	logger.info("Done.");
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
