package cheale14.savesync.client.gui;

import java.lang.reflect.Field;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cheale14.savesync.SaveSync;
import cheale14.savesync.common.IWebSocketHandler;
import cheale14.savesync.common.WSPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

public class SyncReplaceGuiMP extends MultiplayerScreen implements IWebSocketHandler {

	public SyncReplaceGuiMP(Screen parentScreen) {
		super(parentScreen);
		// TODO Auto-generated constructor stub
		
		URI uri = URI.create(SaveSync.getWSUri(true));
		try {
			ws = new WSClient(uri, this);
			ws.connect();
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			SaveSync.LOGGER.error(e);
		}
	}
	
	
	@Override
	public void onClose() {
		super.onClose();
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

			ServerList servers = this.getServers();
			
			for(int index = 0; index < servers.size(); index++) {
				ServerData server = servers.get(index);
				if(server.name.startsWith("[MLAPI]")) {
					servers.remove(server);
					index--;
				}
			}
			
			
			for(JsonElement obj : array) {
				SaveSync.LOGGER.info("Server: " + obj.toString());
				servers.add(getServerData(obj.getAsJsonObject()));
				servers.save();
			}
			
			Field f;
			try {
				ServerSelectionList selector = null;
				Class clazz = MultiplayerScreen.class;
				for(Field field : clazz.getDeclaredFields()) {
					if(field.getType().getName().equals(ServerSelectionList.class.getName())) {
						field.setAccessible(true);
						selector = (ServerSelectionList)field.get(this);
						break;
					}
					SaveSync.LOGGER.debug("MpScreen: " + field.getType().getName() + " " + field.getName());
				}
				if(selector == null) {
					SaveSync.LOGGER.warn("Unable to find serverSelectionList.");
				} else {
					selector.updateOnlineServers(servers);
				}
							
				
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				SaveSync.LOGGER.error(e);
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
