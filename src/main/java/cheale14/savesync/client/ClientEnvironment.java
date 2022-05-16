package cheale14.savesync.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cheale14.savesync.Environment;
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
			
		}
	}
}
