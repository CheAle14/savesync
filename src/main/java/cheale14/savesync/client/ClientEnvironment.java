package cheale14.savesync.client;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cheale14.savesync.Environment;
import cheale14.savesync.SaveSync;
import cheale14.savesync.client.gui.SyncLoginGui;
import cheale14.savesync.client.gui.SyncProgressGui;
import cheale14.savesync.client.gui.SyncProgressGui.SyncType;
import cheale14.savesync.client.gui.SyncReplaceGuiMP;
import cheale14.savesync.client.gui.SyncReplaceIngameMenu;
import cheale14.savesync.common.SyncSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEnvironment implements Environment {

	Logger logger = LogManager.getLogger("savesync-client");
	@SubscribeEvent
	public void RenderPre(InitGuiEvent.Pre event) {
		Screen gui = event.getGui();
		if(gui == null) return;
		logger.info("Init.Pre: " + gui.getClass().getName());
		
		if(gui instanceof WorldSelectionScreen) {
			showSpSceen(gui);
		} else if (gui instanceof CreateWorldScreen) {
			if(inProgress != null) {
				didSync = true;
				gui.getMinecraft().setScreen(inProgress);
			}
		} else if(gui instanceof IngameMenuScreen) {
			if(gui instanceof SyncReplaceIngameMenu) return;
			
			SyncReplaceIngameMenu replace = new SyncReplaceIngameMenu(true);
			gui.getMinecraft().setScreen(replace);
		} else if(gui instanceof MultiplayerScreen) {
			
			if(gui instanceof SyncReplaceGuiMP) return;
			
			SyncReplaceGuiMP mp = new SyncReplaceGuiMP(gui);
			gui.getMinecraft().setScreen(mp);
		} else if(gui instanceof MainMenuScreen) {
			if(toUpload != null) {
				File temp = toUpload;
				toUpload = null;
				logger.info("toUpload not null, doing upload.");
				gui.getMinecraft().setScreen(new SyncProgressGui(gui, SyncType.UPLOAD_ONE, temp));
			}
		}
	}

	public SyncProgressGui inProgress;
	public boolean didSync = false;
	void showSpSceen(Screen oldGui) {
		try {
			if(SaveSync.GetCurrentUser() == null) {
				logger.info("API key gives null user, might be missing - forcing login");
				SaveSync.CONFIG.ApiKey.set("none");
				oldGui.getMinecraft().setScreen(new SyncLoginGui(oldGui));
				return;
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.warn("API key results in error; forcing re-login");
			SaveSync.CONFIG.ApiKey.set("none");
			oldGui.getMinecraft().setScreen(new SyncLoginGui(oldGui));
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
			File saveFolder = new File(oldGui.getMinecraft().gameDirectory, "saves");
			logger.info("Save direcotry: " + saveFolder);
			inProgress = new SyncProgressGui(oldGui, SyncType.DOWNLOAD_ALL, saveFolder);
			oldGui.getMinecraft().setScreen(inProgress);
		}
	}
	

	@Override
    public SyncSave GetDefaultSave() {
    	String repo = SaveSync.CONFIG.DefaultRepository.get();
    	if(repo == null || repo == "none") return null;
    	File saveDir = new File(Minecraft.getInstance().gameDirectory, "saves");
    	File worldDir = new File(saveDir, "default");
    	return new SyncSave(repo, "main", worldDir);
    }

	File toUpload = null;
	@Override
	public void OnServerStopped(MinecraftServer server) throws NoSuchFieldException, IllegalAccessException {
    	File f = SaveSync.getWorldFolder(server.overworld());
    	if(SyncSave.IsSyncedDirectory(f)) {
    		logger.info("Integrated server stopped, directory is being synced - scheudling upload.");
    		toUpload = f;
    	}
	}
	
}
