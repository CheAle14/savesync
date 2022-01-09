package com.cheale14.savesync.client.discord;

public enum IPCState {
	DISCONNECTED,
	CONNECTING,
	CONNECTED,
	PENDING_AUTH,
	AUTHORIZED,
	AUTHENTICATED,
	ERRORED
	;
}	
