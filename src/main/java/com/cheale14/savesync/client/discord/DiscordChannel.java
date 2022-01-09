package com.cheale14.savesync.client.discord;

public class DiscordChannel {
	public DiscordChannel(IPCClient ds, String _id, ChannelType _type) {
		discord = ds;
		id = _id;
		type = _type;
	}
	private IPCClient discord;
	public String id;
	public String name;
	public ChannelType type;
}
