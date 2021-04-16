package com.cheale14.savesync.gui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Mouse;

import com.cheale14.savesync.SaveSync;
import com.cheale14.savesync.SyncThread;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;

public class SyncProgressGui extends GuiScreen {
	
	public SyncProgressGui(GuiScreen parent, SyncType type, File file) {
		mc = parent.mc;
		Type = type;
		parentScreen = parent;
		world = file;
	}
	public SyncProgressGui(GuiScreen parent, SyncType type) {
		this(parent, type, null);
	}
	
	public SyncType Type;
	Minecraft mc;
	GuiScreen parentScreen;
	GuiButton cancelButton;
	SyncGuiLog log;
	SyncThread thread;
	File world;
	
	@Override
	public void initGui() {
		cancelButton = new GuiButton(1, 1, 1, "Cancel " + (Type.Pull ? "Download" : "Upload"));
		this.buttonList.add(cancelButton);
		log = new SyncGuiLog(this, this.width - 30, 10);
		if(thread == null) {
			Start();
		}
	}
	
	

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.log.drawScreen(mouseX, mouseY, partialTicks);
		
		if(stopCount >= 0) {
			stopCount ++;
			cancelButton.displayString = "Stopping" + StringUtils.repeat('.', stopCount % 20);
			if(!thread.isAlive()) {
				thread = null;
				SaveSync.logger.info("Thread ended, closing UI");
				Minecraft.getMinecraft().displayGuiScreen(parentScreen);
				return;
			}
		}
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	int stopCount = -1;
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(!button.enabled)
			return;
		if(button.id == cancelButton.id) {
			button.enabled = false;
			button.displayString = "Stopping";
			stopCount = 0;
			thread.Cancel();
		}
	}
	
	@Override
	public void handleMouseInput() throws IOException {
        super.handleMouseInput();
		Minecraft _mc = this.mc;
		if(_mc == null) {
			_mc = Minecraft.getMinecraft();
			if(_mc == null) {
				SaveSync.logger.error("MC was still null!");
				return;
			}
		}
		try {
	        int mouseX = Mouse.getEventX() * this.width / _mc.displayWidth;
	        int mouseY = this.height - Mouse.getEventY() * this.height / _mc.displayHeight - 1;
	        this.log.handleMouseInput(mouseX, mouseY);
		} catch (NullPointerException e) {
			SaveSync.logger.error(e);
		}
	}
	
	@Override
	public void onGuiClosed() {
		if(thread != null) {
			thread.Cancel();
		}
	}
	
	public void Append(String message) {
		log.Add(message);
	}
	
	public void SetButtonDone() {
		this.cancelButton.displayString = "Close - Done";
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
		thread = new SyncThread(this, world);
		thread.start();
	}
}


