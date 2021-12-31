package com.cheale14.savesync.client.discord;

import com.cheale14.savesync.common.SaveSync;
import com.google.gson.JsonObject;

public class IPCHandshakePacket extends IPCPacket {
	public IPCHandshakePacket() {
		this.op = IPCOpCode.HANDSHAKE;
		JsonObject o = new JsonObject();
		o.addProperty("v", 1);
		o.addProperty("client_id", SaveSync.DS_CLIENT_ID);
		this.data = o;
	}
}
