package cheale14.savesync.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cheale14.savesync.Environment;
import cheale14.savesync.SaveSync;
import cheale14.savesync.client.gui.SyncLoginGui;
import cheale14.savesync.client.gui.SyncProgressGui;
import cheale14.savesync.client.gui.SyncProgressGui.SyncType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
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
		}
	}

	boolean didSync = false;
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
			SyncProgressGui syncGui = new SyncProgressGui(oldGui, SyncType.DOWNLOAD_ALL);
			oldGui.getMinecraft().setScreen(syncGui);
		}
	}
}
