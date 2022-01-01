package com.cheale14.savesync.client.discord;

import net.minecraft.client.gui.GuiScreen;

public class GuiDiscordChatRelay extends GuiScreen {
	public GuiDiscordChatRelay(GuiScreen _parent, IPCClient ds) {
		parent = _parent;
		ipc = ds;
	}
	private GuiScreen parent;
	private IPCClient ipc;
	
	@Override
	public void initGui() {
		
	}
}
