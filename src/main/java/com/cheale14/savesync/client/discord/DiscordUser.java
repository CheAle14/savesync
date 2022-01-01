package com.cheale14.savesync.client.discord;

public class DiscordUser {
	public String id;
	public String username;
	public String discriminator;
	
	public String getFullName() {
		return username + "#" + discriminator;
	}
}
