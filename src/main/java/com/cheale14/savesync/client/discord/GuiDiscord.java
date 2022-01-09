package com.cheale14.savesync.client.discord;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import org.lwjgl.input.Mouse;

import com.cheale14.savesync.common.SaveSync;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.JsonUtils;

public class GuiDiscord extends GuiScreen implements IPCHandler  {
	public GuiDiscord(GuiScreen parentScr, IPCClient ds) {
		ipc = ds;
		parent = parentScr;
		ipc.addHandler(this);
	}
	private GuiScreen parent;
	private IPCClient ipc;

	//private GuiButton btnChatSetup;
	private GuiButton btnBack;
	
	private GuiTextField txtState;
	private GuiTextField txtName;
	
	private GuiDiscordList guildList;
	
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
		
		guildList = new GuiDiscordList(this, runY, this.height - 30, this.width, this.height);
		
		
		//btnChatSetup = this.addButton(new GuiButton(_ID_++, midX - 40, runY, 80, 20, "Chat Relay"));
		btnBack = this.addButton(new GuiButton(_ID_++, midX - 20, this.height - 25, 40, 20, "Back"));
		
		update();
	}
	

    @Override
    public void handleMouseInput() throws IOException
    {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        super.handleMouseInput();

        this.guildList.handleMouseInput(mouseX, mouseY);
    }
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(button.id == btnBack.id) {
			mc.displayGuiScreen(parent);
		/*} else if(button.id == btnChatSetup.id) {*/
			
		}
	}
	
	boolean sent_guilds = false;
	void update() {
		IPCState state = ipc.state();
		txtState.setText(state.name());
		//btnChatSetup.visible = state == IPCState.AUTHORIZED;
		if(ipc.user == null) {
			txtName.setText("");
		} else {
			txtName.setText(ipc.user.getFullName());
		}
		if(state == IPCState.AUTHENTICATED && !sent_guilds) {
			sent_guilds = true;
			try {
				ipc.Send(IPCPayload.Command("GET_GUILDS", null));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		guildList.drawScreen(mouseX, mouseY, partialTicks);
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
	public void OnPacket(IPCPacket packet) {
		if(!(packet instanceof IPCFramePacket)) 
			return;
		IPCFramePacket frame = (IPCFramePacket)packet;
		IPCPayload payload = frame.payload;
		if("GET_GUILDS".equals(payload.cmd)) {
			JsonArray glds = payload.data.getAsJsonObject().get("guilds").getAsJsonArray();
			for(JsonElement elem : glds) {
				JsonObject gld = elem.getAsJsonObject();
				DiscordGuild guild = new DiscordGuild(ipc, 
						JsonUtils.getString(gld, "id"), 
						JsonUtils.getString(gld, "name"),
						JsonUtils.getString(gld, "icon_url", null));
				ipc.guilds.put(guild.id, guild);
				ipc.logger.info("New guild: " + guild.id + ", " + guild.name);
			}
			guildList.updateServers(ipc.guilds.values());
		}
	}
	
	@Override
	public void onGuiClosed() {
		ipc.removeHandler(this);
	}
}
