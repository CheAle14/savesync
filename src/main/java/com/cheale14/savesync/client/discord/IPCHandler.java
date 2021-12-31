package com.cheale14.savesync.client.discord;

import java.io.IOException;

public interface IPCHandler {
	public void OnPacket(IPCPacket packet) throws IOException;
}
