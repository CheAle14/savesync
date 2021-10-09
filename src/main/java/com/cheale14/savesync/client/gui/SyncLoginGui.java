package com.cheale14.savesync.client.gui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.awt.Desktop;

import org.apache.commons.lang3.StringUtils;

import com.cheale14.savesync.client.OAuth2Listener;

import net.minecraft.client.gui.GuiScreen;

public class SyncLoginGui extends GuiScreen {
	private OAuth2Listener server;
	private GuiScreen parent;
	private String _state;
	
	public SyncLoginGui(GuiScreen previous) {
		parent = previous;
		_state = StringUtils.leftPad(new Random().nextInt() + "", 15, "0");
	}
	
	String encode(String text) {
		try {
			return URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	String getRedirectUrl() {
		String url = "https://github.com/login/oauth/authorize";
		url += "?client_id=" + "1e6305d27b4d770ddd30";
		int port = server.Port;
		url += "&redirect_uri=" + encode("http://localhost:" + port + "/callback");
		url += "&scopes=" + encode("repo read:user");
		url += "&state=" + encode(_state);
		return url;
	}
	
	@Override
	public void initGui() {
		if(server == null) {
			try {
				server = new OAuth2Listener(0);
				server.Start();
				
				Desktop d = Desktop.getDesktop();
				d.browse(URI.create(getRedirectUrl()));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(fontRenderer, "Listening", 0,0, 0);
	}
}
