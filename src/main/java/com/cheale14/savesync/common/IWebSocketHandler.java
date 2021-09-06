package com.cheale14.savesync.common;

public interface IWebSocketHandler {
	public void OnPacket(WSPacket packet);
	public void OnOpen();
	public void OnClose(int errorCode, String reason);
	public void OnError(Exception error);
}
