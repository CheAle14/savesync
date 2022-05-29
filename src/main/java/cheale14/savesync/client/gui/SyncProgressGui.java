package cheale14.savesync.client.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;

import cheale14.savesync.SaveSync;
import cheale14.savesync.client.ClientEnvironment;
import cheale14.savesync.common.SaveInfo;
import cheale14.savesync.common.SyncThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public class SyncProgressGui extends Screen {
	
	public SyncProgressGui(Screen parent, SyncType type, File file, boolean closeOnEnd) {
		super(new StringTextComponent("Sync Progress"));
		Type = type;
		parentScreen = parent;
		dir = file;
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
	Screen parentScreen;
	Button cancelButton;
	Button doneButton;
	SyncTextList log;
	File dir;
	
	List<SyncThread> threads;
	boolean closeOnNormalEnd;

	
	@Override
	public void init() {
		
		cancelButton = new Button(5, 10, 100, 20, new StringTextComponent("Cancel " + (Type.Pull ? "Download" : "Upload")), new Button.IPressable() {
			@Override
			public void onPress(Button button) {
				button.active = false;
				button.setMessage(new StringTextComponent("Stopping"));
				stopCount = 0;
				stop();
			}
		});
		this.addButton(cancelButton);
		
		doneButton = new Button(cancelButton.getWidth() + 5, 10, 100, 20, new StringTextComponent("Close"), new Button.IPressable() {	
			@Override
			public void onPress(Button btn) {
				SaveSync.LOGGER.info("Closing!");
				onClose();
			}
		});


		doneButton.visible = false;
		this.addButton(doneButton);
		log = new SyncTextList(this, this.width, this.height, 35, this.height - 35, this.font.lineHeight + 2);
		log.setRenderBackground(false);
		this.addWidget(log);
		if(threads == null) {
			threads = new ArrayList<SyncThread>();
			try {
				Start();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.Append("Failed to start: " + e.getMessage());
			}
		}
	}
	
	void stop() {
		for(SyncThread t : threads)
			t.Cancel();
	}

	boolean isAlive() {
		for(SyncThread t : threads) {
			if(!t.isAlive()) return false;
		}
		return true;
	}
	boolean hasError() {
		for(SyncThread t : threads)
			if(t.hasError()) return true;
		return false;
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrix);
		super.render(matrix, mouseX, mouseY, partialTicks);
		this.log.render(matrix, mouseX, mouseY, partialTicks);
		
		if(stopCount >= 0) {
			stopCount ++;
			cancelButton.setMessage(new StringTextComponent("Stopping" + StringUtils.repeat('.', stopCount % 20)));

			if(!isAlive()) {
				SaveSync.LOGGER.info("Threads ended, closing UI");
				this.minecraft.setScreen(parentScreen);
				onClose();
				return;
			}
		} else if(!isAlive() && !hasError()) {
			SaveSync.LOGGER.info("Closing UI as success without issues.");
			onClose();
			return;
		}
	}

	int stopCount = -1;
	
	public boolean closed = false;
	public boolean ended() {
		if(threads == null)
			return true;
		return !isAlive();
	}
	
	@Override
	public void onClose() {
		SaveSync.LOGGER.info("SyncProgress onClose");
		closed = true;
		if(threads != null) {
			stop();
		}
		threads = null;
		ClientEnvironment proxy = (ClientEnvironment)SaveSync.PROXY;
		proxy.inProgress = null;
		proxy.didSync = true;
		this.minecraft.setScreen(new WorldSelectionScreen(new MainMenuScreen()));
	}
	
	public void Append(String message) {
		SaveSync.LOGGER.info("[gui] " + message);
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
	
	public void Start() throws FileNotFoundException {
		if(log != null) {
			log.Clear();
		}
		if(this.Type == SyncType.DOWNLOAD_ALL || this.Type == SyncType.UPLOAD_ALL) {
			String defaultRepo = SaveSync.CONFIG.DefaultRepository.get();
			boolean any = false;
			for(SaveInfo save : SaveInfo.LoadAll(dir)) {
				any = true;
				SyncThread t = new SyncThread(this, save);
				threads.add(t);
				t.start();
			}
			if(!any) {
				if(this.Type == SyncType.DOWNLOAD_ALL) {
					SaveInfo s = SaveSync.PROXY.GetDefaultSave();
					if(s != null) {
						SyncThread t = new SyncThread(this, s);
						threads.add(t);
						t.start();
					}
				}
			}
		} else {
			SyncThread t= new SyncThread(this, SaveInfo.Load(dir));
			threads.add(t);
			t.start();
		}
	}
	
	public FontRenderer getFont() { return font; }
}


