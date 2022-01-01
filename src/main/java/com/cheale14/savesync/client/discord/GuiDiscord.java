package com.cheale14.savesync.client.discord;

import java.awt.Color;
import java.io.IOException;

import com.cheale14.savesync.common.SaveSync;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class GuiDiscord extends GuiScreen implements IPCHandler  {
	public GuiDiscord(GuiScreen parentScr, IPCClient ds) {
		ipc = ds;
		parent = parentScr;
		ipc.addHandler(this);
	}
	private GuiScreen parent;
	private IPCClient ipc;

	private GuiButton btnChatSetup;
	private GuiButton btnBack;
	
	private GuiTextField txtState;
	private GuiTextField txtName;
	
	@Override
	public void initGui() {
		int _ID_ = 0;
		int midX = this.width / 2;
		
		int runY = 5;
		txtState = new GuiTextField(_ID_++, this.fontRenderer, midX + 5, runY, 80, 20);
		txtState.setEnabled(false);
		runY += txtState.height + 3;
		txtName = new GuiTextField(_ID_++, this.fontRenderer, midX + 5, runY, 80, 20);
		runY += txtName.height + 3;
		txtName.setEnabled(false);
		
		btnChatSetup = this.addButton(new GuiButton(_ID_++, midX - 40, runY, 80, 20, "Chat Relay"));
		btnBack = this.addButton(new GuiButton(_ID_++, midX - 20, this.height - 25, 40, 20, "Back"));
		
		update();
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(button.id == btnBack.id) {
			mc.displayGuiScreen(parent);
		} else if(button.id == btnChatSetup.id) {
			
		}
	}
	
	void update() {
		IPCState state = ipc.state();
		txtState.setText(state.name());
		btnChatSetup.visible = state == IPCState.AUTHORIZED;
		if(ipc.user == null) {
			txtName.setText("");
		} else {
			txtName.setText(ipc.user.getFullName());
		}
	}
	

	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		txtState.drawTextBox();
		txtName.drawTextBox();
		
		int white = Color.white.getRGB();
		this.drawString(this.fontRenderer, "State:", this.width / 2 - 60, 10, white);
		this.drawString(this.fontRenderer, "User:", this.width / 2 - 60, 35, white);
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}


	@Override
	public void OnPacketPre(IPCPacket packet) {
		SaveSync.logger.info("GuiDiscord.OnPacketPre");
		if(!(packet instanceof IPCFramePacket)) 
			return;
		update();
	}
	@Override
	public void OnState(IPCState st) {
		update();
	}
	
	@Override
	public void onGuiClosed() {
		ipc.removeHandler(this);
	}
}
