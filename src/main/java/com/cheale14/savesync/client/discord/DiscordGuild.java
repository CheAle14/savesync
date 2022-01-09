package com.cheale14.savesync.client.discord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DiscordGuild {
	public DiscordGuild(IPCClient ds, String _id, String _name, String _icon_url) {
		discord = ds;
		id = _id;
		name = _name;
		icon_url = _icon_url;
	}
	private IPCClient discord;
	public String id;
	public String name;
	public String icon_url;
	private Map<String, DiscordChannel> _channels = new HashMap<String, DiscordChannel>();
	
	public void addChannel(DiscordChannel chnl) {
		_channels.put(chnl.id, chnl);
	}
	
	public Collection<DiscordChannel> channels() {
		return _channels.values();
	}
	
	public String getIconUrl(int size) {
		if(this.icon_url == null)
			return null;
		int sizeIndex = this.icon_url.indexOf("?size=");
		sizeIndex += "?size=".length();
		return this.icon_url.substring(0, sizeIndex) + size;
	}
	
}
