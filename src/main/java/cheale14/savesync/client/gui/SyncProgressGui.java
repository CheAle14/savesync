package cheale14.savesync.client.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;

import cheale14.savesync.SaveSync;
import cheale14.savesync.common.SyncSave;
import cheale14.savesync.common.SyncThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public class SyncProgressGui extends Screen {
	
	public SyncProgressGui(Screen parent, SyncType type, File file, boolean closeOnEnd) {
		super(new StringTextComponent("Sync Progress"));
		mc = parent.getMinecraft();
		Type = type;
		parentScreen = parent;
		world = file;
		closeOnNormalEnd = closeOnEnd;
	}
	public SyncProgressGui(Screen parent, SyncType type) {
		this(parent, type, null, SaveSync.CONFIG.CloseUIOnSuccess.get());
	}
	public SyncProgressGui(Screen parent, SyncType type, File file) {
		this(parent, type, file, SaveSync.CONFIG.CloseUIOnSuccess.get());
	}
	public SyncProgressGui(Screen parent, SyncType type, boolean closeOnEnd) {
		this(parent, type, null, closeOnEnd);
	}
	
	public SyncType Type;
	Minecraft mc;
	Screen parentScreen;
	Button cancelButton;
	Button doneButton;
	SyncTextList log;
	SyncThread thread;
	boolean closeOnNormalEnd;
	File world;
	
	@Override
	public void init() {
		
		cancelButton = new Button(1, 1, 100, 25, new StringTextComponent("Cancel " + (Type.Pull ? "Download" : "Upload")), new Button.IPressable() {
			@Override
			public void onPress(Button button) {
				button.active = false;
				button.setMessage(new StringTextComponent("Stopping"));
				stopCount = 0;
				thread.Cancel();
			}
		});
		this.addButton(cancelButton);
		
		doneButton = new Button(cancelButton.getWidth() + 5, 1, 100, 25, new StringTextComponent("Close"), new Button.IPressable() {	
			@Override
			public void onPress(Button btn) {
				SaveSync.LOGGER.info("Closing!");
				thread = null;
				Minecraft.getInstance().setScreen(parentScreen);
			}
		});


		doneButton.visible = false;
		this.addButton(doneButton);
		log = new SyncTextList(this, this.width - 30, 5, 100);
		if(thread == null) {
			Start();
		}
	}
	
	

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		this.renderDirtBackground(0);
		this.log.render(matrix, mouseX, mouseY, partialTicks);
		
		if(stopCount >= 0) {
			stopCount ++;
			cancelButton.setMessage(new StringTextComponent("Stopping" + StringUtils.repeat('.', stopCount % 20)));
			if(!thread.isAlive()) {
				thread = null;
				SaveSync.LOGGER.info("Thread ended, closing UI");
				mc.setScreen(parentScreen);
				return;
			}
		} else if(!thread.isAlive() && !thread.hasError()) {
			SaveSync.LOGGER.info("Closing UI as success without issues.");
			thread = null;
			mc.setScreen(parentScreen);
			return;
		}
		
		super.render(matrix, mouseX, mouseY, partialTicks);
	}

	int stopCount = -1;
	
	public boolean closed = false;
	public boolean ended() {
		if(thread == null)
			return true;
		return !thread.isAlive();
	}
	
	@Override
	public void onClose() {
		closed = true;
		if(thread != null) {
			thread.Cancel();
		}
	}
	
	public void Append(String message) {
		log.add(message);
	}
	
	public void SetButtonDone() {
		this.cancelButton.active = false;
		this.doneButton.visible = true;
	}

	
	public enum SyncType {
		DOWNLOAD_ALL(true, true),
		DOWNLOAD_ONE(true, false),
		UPLOAD_ALL(false, true),
		UPLOAD_ONE(false, false);
		
		
		public boolean Pull;
		public boolean All;
		SyncType(boolean pull, boolean all) {
			Pull = pull;
			All = all;
		}
	}
	
	public void Start() {
		if(log != null) {
			log.Clear();
		}
		thread = new SyncThread(this, world);
		thread.start();
	}
	
	public FontRenderer getFont() { return font; }
}


