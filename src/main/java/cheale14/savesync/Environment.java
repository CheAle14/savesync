package cheale14.savesync;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cheale14.savesync.common.IWebSocketHandler;
import cheale14.savesync.common.WSPacket;

public interface Environment {
	Logger logger = LogManager.getLogger("savesync-proxy");
	String wsId = null;
	default void PublishServer(JsonObject serverData, IWebSocketHandler handler) throws KeyManagementException, NoSuchAlgorithmException, InterruptedException {
		if(wsId != null) {
			serverData.addProperty("id", wsId);
		}
		logger.info("UpsertServer: " + serverData.toString());
		URI uri = URI.create(SaveSync.WS_URI(false));
		

		WSPacket packet = new WSPacket();
		packet.Id("UpsertServer");
		packet.Content(serverData);
		Gson gson = new Gson();
		String sending = gson.toJson(packet);
		
		if(websocket == null) {
			websocket = new WSClient(uri, new IWebSocketHandler() {
				@Override
				public void OnPacket(WSPacket packet) {
					if(packet.Id().equals("UpsertServer")) {
						if(packet.Content() instanceof JsonObject) {
	
							wsId = ((JsonObject)packet.Content()).get("id").getAsString();
						}
					}
					if(handler != null)
						handler.OnPacket(packet);
				}
				@Override
				public void OnOpen() {
					websocket.send(sending);
					
					if(handler != null)
						handler.OnOpen();
				}
				@Override
				public void OnClose(int errorCode, String reason) {
					if(handler != null)
						handler.OnClose(errorCode, reason);
					
					
				}
				@Override
				public void OnError(Exception error) {
					if(handler != null)
						handler.OnError(error);
				}
				
			});
			websocket.connect();
		} else {
			if(websocket.isClosing() || websocket.isClosed()) {
				logger.info("[WS] Reconnecting websocket");
				websocket.reconnectBlocking();
			}
			websocket.send(sending);
		}
	}
}
