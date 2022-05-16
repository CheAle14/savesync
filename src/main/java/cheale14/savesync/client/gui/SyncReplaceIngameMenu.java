package cheale14.savesync.client.gui;

import java.io.File;
import java.io.IOException;

import com.cheale14.savesync.SaveSync;
import com.cheale14.savesync.client.gui.SyncProgressGui.SyncType;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.realms.RealmsBridge;

public class SyncReplaceIngameMenu extends IngameMenuScreen {

	@Override
	public void initGui() {
		super.initGui();
		if(this.mc.isIntegratedServerRunning()) {
			for(GuiButton btn : this.buttonList) {
				if(btn.id == 1) {
					btn.displayString = "Close & Sync";
					break;
				}
			}
		}
	}
	
	@Override
    protected void actionPerformed(GuiButton button) throws IOException {
		if(button.id == 1) {			
	    	SaveSync.isUploading = true;
	    	boolean flag = this.mc.isIntegratedServerRunning();
	        boolean flag1 = this.mc.isConnectedToRealms();
	        button.enabled = false;
	        File oldSave = this.mc.world.getSaveHandler().getWorldDirectory();
	        SyncType type = oldSave == null ? SyncType.UPLOAD_ALL : SyncType.UPLOAD_ONE;
	        this.mc.world.sendQuittingDisconnectingPacket();
	        this.mc.loadWorld((WorldClient)null);
	
	        if (flag)
	        {
	        	SyncProgressGui sync = new SyncProgressGui(new GuiMainMenu(), type, oldSave);
	            this.mc.displayGuiScreen(sync);
	        }
	        else if (flag1)
	        {
	            RealmsBridge realmsbridge = new RealmsBridge();
	            realmsbridge.switchToRealms(new GuiMainMenu());
	        }
	        else
	        {
	        	SyncProgressGui sync = new SyncProgressGui(new GuiMultiplayer(new GuiMainMenu()), type, oldSave);
	            this.mc.displayGuiScreen(sync);
	        }
		} else if (button.id == 7) {
			// open to lan
			SyncPublishGui publish = new SyncPublishGui(this);
			this.mc.displayGuiScreen(publish);
		} else {
			super.actionPerformed(button);
		}
    }
}
