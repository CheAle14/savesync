package com.cheale14.savesync.client;

import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.cheale14.savesync.common.IWebSocketHandler;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.WSPacket;
import com.google.gson.Gson;

public class WSClient extends WebSocketClient {

	public WSClient(URI uri, IWebSocketHandler handle) {
		super(uri);
		
		logger = SaveSync.logger;
		logger.info("[WS] URI: " + uri.toString());
		handler = handle;
		gson = new Gson();
	}
	
	Logger logger;
	IWebSocketHandler handler;
	Gson gson;

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
		logger.warn("[WS] connection closed: " + arg0 + " " + arg1);
		handler.OnClose(arg0, arg1);
	}

	@Override
	public void onError(Exception arg0) {
		// TODO Auto-generated method stub

		logger.error("[WS] Error: " + arg0.toString());
		handler.OnError(arg0);
	}

	@Override
	public void onMessage(String arg0) {
		// TODO Auto-generated method stub
		
		logger.debug("[WS] << " + arg0);
		
		WSPacket packet = gson.fromJson(arg0, WSPacket.class);
		logger.info("Parsed packet of ID " + packet.Id());
		handler.OnPacket(packet);

	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		// TODO Auto-generated method stub

		logger.info("[WS] Connected");
		handler.OnOpen();
	}

}
