package com.cheale14.savesync.client.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.Desktop;

import org.apache.commons.lang3.StringUtils;

import com.cheale14.savesync.client.GithubOauthCallback;
import com.cheale14.savesync.client.GithubUser;
import com.cheale14.savesync.client.OAuth2Listener;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.SaveSync.SaveConfig;
import com.cheale14.savesync.http.HttpError;
import com.cheale14.savesync.http.HttpUtil;
import com.google.gson.Gson;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class SyncLoginGui extends GuiScreen {
	private OAuth2Listener server;
	private GuiScreen parent;
	private String _state;
	
	private static String id = "1e6305d27b4d770ddd30";
	private static String hid_den = "79659677cea38f57955bcb3cef0a05e7ebd4515d";
	
	private String displayMessage;
	
	public SyncLoginGui(GuiScreen previous) {
		parent = previous;
		SaveSync.PurgeCachedUser();
		_state = StringUtils.leftPad(new Random().nextInt() + "", 15, "0");
	}
	
	String encode(String text) {
			return text
					.replace(" ", "%20")
					.replace(":", "%3A")
					.replace("/", "%2F");
	}
	
	String getRedirectUrl() {
		String url = "https://github.com/login/oauth/authorize";
		url += "?client_id=" + id;
		int port = server.Port;
		url += "&redirect_uri=" + encode("http://localhost:" + port + "/callback");
		url += "&scope=" + encode("repo read:user");
		url += "&state=" + encode(_state);
		return url;
	}
	
	public void scheduleUI() {
		mc.addScheduledTask(() -> {
			this.updateScreen();
		});
	}
	
	public int GotLoginData(String code, String state) {
		if(!_state.equals(state)) {
			displayMessage = "State did not match; CORS attack?";
			return 400;
		}
		displayMessage = "Exchanging code for access token...";
		
		// make POST to github;
		Gson gson = new Gson();
		GithubOauthCallback data;
		try {

			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			
			
			Map<String, String> form = new HashMap<>();
			form.put("client_id", id);
			form.put("client" + "_" + "secret", hid_den);
			form.put("code", code);
			
			String json = HttpUtil.POST("https://github.com/login/oauth/access_token", form, headers);
			
			displayMessage = "Token exchange";
			
			data = gson.fromJson(json, GithubOauthCallback.class);
			
			displayMessage = "Access token: " + data.access_token;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 500;
		} catch(HttpError e) {
			return e.getStatusCode();
		}

		GithubUser user;
		// test access token
		try {
			user = SaveSync.GetCurrentUser(data.access_token);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 500;
		}
		displayMessage = "Logged in as " + user.login + "; saving token to config file";
		
		
		// save access token
		SaveConfig.API_Key = data.access_token;
		Configuration config = new Configuration(SaveSync.configFile);
		config.load();
		ConfigCategory cat = config.getCategory(Configuration.CATEGORY_GENERAL);
		cat.get("API Key").set(data.access_token);
		config.save();
		
		mc.addScheduledTask(() -> {
			mc.displayGuiScreen(parent);
		});
		
		
		return 200;
	}
	
	@Override
	public void onGuiClosed() {
		server.Stop();
	}
	
	@Override
	public void initGui() {
		if(server == null) {
			try {
				displayMessage = "Listening for redirect...";
				server = new OAuth2Listener(this, 0); // 0 as port means OS will give us an unused one
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
		this.drawCenteredString(fontRenderer, displayMessage, this.width / 2, this.height / 2, 0);
	}
}
