package com.cheale14.savesync.client.gui;

import java.lang.reflect.Field;
import java.net.URI;

import com.cheale14.savesync.client.WSClient;
import com.cheale14.savesync.common.IWebSocketHandler;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.WSPacket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

public class SyncReplaceGuiMP extends GuiMultiplayer implements IWebSocketHandler {

	public SyncReplaceGuiMP(GuiScreen parentScreen) {
		super(parentScreen);
		// TODO Auto-generated constructor stub
		
		URI uri = URI.create(SaveSync.WS_URI(true));
		ws = new WSClient(uri, this);
		ws.connect();
	}
	
	
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		ws.close(1000, "User exit");
	}
	
	ServerData getServerData(JsonObject object) {
		String name = "[MLAPI] " + object.get("name").getAsString();
		String ip = object.get("ip").getAsString() + ":" + object.get("port").getAsInt();
		return new ServerData(name, ip, false);
	}
	
	WSClient ws;

	@Override
	public void OnPacket(WSPacket packet) {
		if(packet.Id().equals("SendServers")) {
			JsonArray array = (JsonArray)packet.Content();

			ServerList servers = this.getServerList();
			
			for(Integer index = 0; index < servers.countServers(); index++) {
				ServerData server = servers.getServerData(index);
				if(server.serverName.startsWith("[MLAPI]")) {
					servers.removeServerData(index);
					index--;
				}
			}
			
			
			for(JsonElement obj : array) {
				SaveSync.logger.info("Server: " + obj.toString());
				servers.addServerData(getServerData(obj.getAsJsonObject()));
				servers.saveServerList();
			}
			
			Field f;
			try {
				f = GuiMultiplayer.class.getDeclaredField("serverListSelector");
				f.setAccessible(true);
				ServerSelectionList selector = (ServerSelectionList)f.get(this);
				
				selector.updateOnlineServers(servers);
				
				
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				SaveSync.logger.error(e);
			}
			
			
			
		}
	}

	@Override
	public void OnOpen() {
		
	}

	@Override
	public void OnClose(int errorCode, String reason) {
		
		
	}

	@Override
	public void OnError(Exception error) {
		
	}

}
