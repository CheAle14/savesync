package com.cheale14.savesync.client.discord;

import com.google.gson.JsonElement;

public class IPCPayload {
	public String cmd;
	public String evt;
	public String nonce;
	public JsonElement data;
	public JsonElement args;

	public static IPCPayload Command(String name, JsonElement args) {
		IPCPayload x = new IPCPayload();
		x.cmd = name;
		x.args = args;
		return x;
	}
}