package cheale14.savesync.client.gui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import cheale14.savesync.SaveSync;
import cheale14.savesync.client.gui.SyncProgressGui.SyncType;
import cheale14.savesync.common.SyncSave;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.command.impl.PublishCommand;
import net.minecraft.realms.RealmsBridgeScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;

public class SyncReplaceIngameMenu extends IngameMenuScreen {

	public SyncReplaceIngameMenu(boolean p_i51519_1_) {
		super(p_i51519_1_);
	}
	
	void removeButton(Button button) {
		this.buttons.remove(button);
		this.children.remove(button);
	}
	
	File getWorldFolder() {
		try {
			return SaveSync.getWorldFolder(this.minecraft.getSingleplayerServer().overworld());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void init() {
		super.init();
		if(this.minecraft.hasSingleplayerServer()) {
			Button close = null;
			Button shareLan = null;
			for(Widget wid : this.buttons) {
				if(!(wid instanceof Button)) continue;
				Button btn = (Button)wid;
				String text = GuiUtils.getWidgetText(btn);
				if(text.equals("menu.returnToMenu") && this.minecraft.isLocalServer()) {
					close = btn;
				} else if(text.equals("menu.shareToLan")) {
					shareLan = btn;
				}
			}
			File w = this.getWorldFolder();
			if(close != null && SyncSave.IsSyncedDirectory(w)) {
				this.removeButton(close);
				this.addButton(new SyncReplaceButton(close, new StringTextComponent("Close & Sync")));
			}
			if(shareLan != null) {
				this.removeButton(shareLan);
				
				this.addButton(new SyncReplaceButton(shareLan, new StringTextComponent("Publish to MLAPI"), (x) -> {
					this.minecraft.setScreen(new SyncPublishGui(this));
					return true;
				}));
			}
		}
	}
}
