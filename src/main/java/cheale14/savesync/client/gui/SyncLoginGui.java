package cheale14.savesync.client.gui;

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

import com.google.gson.Gson;
import com.mojang.blaze3d.matrix.MatrixStack;

import cheale14.savesync.BrowserUtil;
import cheale14.savesync.HttpError;
import cheale14.savesync.HttpUtil;
import cheale14.savesync.SaveSync;
import cheale14.savesync.client.GithubOauthCallback;
import cheale14.savesync.client.GithubUser;
import cheale14.savesync.client.OAuth2Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class SyncLoginGui extends Screen {
	private OAuth2Listener server;
	private Screen parent;
	private String _state;
	
	private static String id = "1e6305d27b4d770ddd30";
	private static String hid_den = "79659677cea38f57955bcb3cef0a05e7ebd4515d";
	
	private String displayMessage;
	
	public SyncLoginGui(Screen previous) {
		super(new StringTextComponent("SaveSync Login"));
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
		minecraft.submitAsync(() -> {
			minecraft.setScreen(this);
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
		SaveSync.CONFIG.ApiKey.set(data.access_token);
		SaveSync.CONFIG.ApiKey.save();
		
		minecraft.submitAsync(() -> {
			minecraft.setScreen(parent);
		});
		
		
		return 200;
	}
	
	@Override
	public void onClose() {
		server.Stop();
		super.onClose();
	}
	
	@Override
	public void init() {
		if(server == null) {
			try {
				displayMessage = "Listening for redirect...";
				server = new OAuth2Listener(this, 0); // 0 as port means OS will give us an unused one
				server.Start();
			
				
				BrowserUtil.Open(getRedirectUrl());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrix);
		AbstractGui.drawCenteredString(matrix, minecraft.font, displayMessage, this.width / 2, this.height / 2, 0);
	}
}
