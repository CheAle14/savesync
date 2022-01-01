package com.cheale14.savesync.client.discord;

import java.io.IOException;

public interface IPCHandler {
	public default void OnPacketPre(IPCPacket packet) {
	}
	public default void OnPacket(IPCPacket packet) throws IOException {
	}
	public default void OnState(IPCState state) {
	}
}
