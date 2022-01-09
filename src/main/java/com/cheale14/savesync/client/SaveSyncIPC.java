package com.cheale14.savesync.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;

import com.cheale14.savesync.client.discord.*;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.http.HttpError;
import com.cheale14.savesync.http.HttpUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SaveSyncIPC implements IPCHandler {
	public SaveSyncIPC(IPCClient _ipc) {
		ipc = _ipc;
		logger = _ipc.logger;
	}
	private IPCClient ipc;
	private Logger logger;
	
	@Override
	public void OnPacket(IPCPacket packet) throws IOException {
		if(packet.op == IPCOpCode.PING) {
			ipc.Pong();
		} else if(packet.op == IPCOpCode.FRAME) {
			if(packet.data == null) {
				logger.info("FRAME has no data??");
				return;
			}
			IPCPayload payload = ((IPCFramePacket)packet).payload;
			if(payload.cmd.equals("DISPATCH")) {
				OnMessage(payload.evt, payload.data.getAsJsonObject());
			}
		} else if(packet.op == IPCOpCode.CLOSE) {
			ipc.Stop();
		}
    }
	
	public void OnMessage(String event, JsonObject data) throws IOException {
		if("READY".equals(event)) {
			JsonObject usr = data.get("user").getAsJsonObject();
			ipc.user = new DiscordUser();
			ipc.user.id = usr.get("id").getAsString();
			ipc.user.username = usr.get("username").getAsString();
			ipc.user.discriminator = usr.get("discriminator").getAsString();
			
			logger.info("Connected to Discord with user " + ipc.user.getFullName() + " (" + ipc.user.id + ")");
			
			// We want to authenticate.
			IPCPayload auth_payload = new IPCPayload();
			auth_payload.cmd = "AUTHORIZE";
			
			JsonObject sendArgs = new JsonObject();
			sendArgs.addProperty("client_id", SaveSync.DS_CLIENT_ID);
			
			JsonArray sendArgsScopes = new JsonArray();
			sendArgsScopes.add("rpc");
			sendArgsScopes.add("identify");
			sendArgs.add("scopes", sendArgsScopes);

			auth_payload.args = sendArgs;
			ipc.state(IPCState.PENDING_AUTH);
			IPCFramePacket response = ipc.SendWaitResponse(auth_payload);
			if("ERROR".equals(response.payload.evt)) {
				ipc.state(IPCState.ERRORED);
				logger.warn("User refused to authenticate?? How rude.");
			} else {
				ipc.state(IPCState.AUTHORIZED);
				String token = response.payload.data.getAsJsonObject().get("code").getAsString();
				logger.info("Token received as: " + token);

				Map<String, String> headers = new HashMap<String, String>();
				headers.put("User-Agent", "DiscordBot (save-sync, " + SaveSync.VERSION + ")");
				
				Map<String, String> body = new HashMap<String, String>();
				body.put("code", token);
				body.put("grant_type", "authorization_code");
				body.put("client_id", SaveSync.DS_CLIENT_ID);
				body.put("client_secret", SaveSync.DS_CLIENT_SECRET);
				String oauth = null;
				try {
					oauth = HttpUtil.POST("https://discord.com/api/oauth2/token", body, headers);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch(HttpError e) {
					e.printStackTrace();
					logger.error(e.getStatusCode() + ": " + e.getErrorBody());
				}
				if(oauth == null) {
					logger.warn("Token exchange failed.");
					return;
				}
				JsonObject obj = new JsonParser().parse(oauth).getAsJsonObject();
				ipc.auth_token = obj.get("access_token").getAsString();
				
				JsonObject auth = new JsonObject();
				auth.addProperty("access_token", ipc.auth_token);
				
				IPCFramePacket authResponse = ipc.SendWaitResponse(IPCPayload.Command("AUTHENTICATE", auth));
				ipc.state(IPCState.AUTHENTICATED);
			}
		}
	}
}
