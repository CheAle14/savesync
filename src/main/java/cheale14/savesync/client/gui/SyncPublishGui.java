package cheale14.savesync.client.gui;


import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;

import cheale14.savesync.HttpError;
import cheale14.savesync.SaveSync;
import cheale14.savesync.common.IWebSocketHandler;
import cheale14.savesync.common.WSPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ShareToLanScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.HTTPUtil;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;

public class SyncPublishGui extends ShareToLanScreen {

	private TextFieldWidget txtServerName;
	private TextFieldWidget txtIpAddress;
	private TextFieldWidget txtGameKind;
	
	public SyncPublishGui(Screen lastScreen) {
		super(lastScreen);
	}

	public JsonObject getJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", txtServerName.getValue());
		obj.addProperty("ip", txtIpAddress.getValue());
		obj.addProperty("mode", txtGameKind.getValue());
		obj.addProperty("game", "minecraft");
		return obj;
	}

	Field gamemodeField;
	Field commandsField;
	
	boolean getCommands() {
		if(commandsField == null); {
			for(Field field : ShareToLanScreen.class.getDeclaredFields()) {
				if(field.getType().getName().equals(boolean.class.getTypeName())) {
					commandsField = field;
					break;
				}
			}
		}
		try {
			return commandsField.getBoolean(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return true;
		}
	}
	
	String getGameMode() {
		if(gamemodeField == null); {
			for(Field field : ShareToLanScreen.class.getDeclaredFields()) {
				if(field.getType().getName().equals(String.class.getTypeName())) {
					gamemodeField = field;
					break;
				}
			}
		}
		try {
			return (String)gamemodeField.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return "survival";
		}
	}
	
	
	@Override
	public void init() {

		txtServerName = new TextFieldWidget(font, this.width / 2 - 155, 125, 300, 20, new StringTextComponent("Name"));
		txtServerName.setMaxLength(32);
		txtServerName.setValue(this.minecraft.player.getName().getContents() + "'s server");
		txtIpAddress = new TextFieldWidget(font, this.width / 2 - 155, 150, 150, 20, new StringTextComponent("127.0.0.1"));
		
		if(!setHamachi()) setExternal();
		
		txtGameKind = new TextFieldWidget(font, this.width / 2 - 155, 175, 150, 20, new StringTextComponent("modded/omnifactory"));
		txtGameKind.setValue(SaveSync.CONFIG.GameMode.get());
		txtGameKind.active = false;
		
		this.addWidget(txtServerName);
		this.addWidget(txtIpAddress);
		this.addWidget(txtGameKind);
		
		
		super.init();
		Button startBtn = null;
		for(Widget wid : this.buttons) {
			if(!(wid instanceof Button)) continue;
			Button btn = (Button)wid;
			String text = GuiUtils.getWidgetText(btn);
			if(text.equals("lanServer.start")) {
				startBtn = btn;
				break;
			}
		}
		if(startBtn == null) {
			SaveSync.LOGGER.warn("PublishGui start button was null");
		} else {
			SaveSync.LOGGER.info("Replaced regular start button");
			this.buttons.remove(startBtn);
			this.children.remove(startBtn);
			this.addButton(new SyncReplaceButton(startBtn, new StringTextComponent("Publish LAN and MLAPI"), (x) -> {
				SaveSync.LOGGER.info("Button publish pressed, doing stuff!");
				done();
				return true;
			}));
		}
		
		
		
		
		this.addButton(new Button(this.width / 2 + 5, 150, 70, 20, new StringTextComponent("Hamachi"), new Button.IPressable() {
			@Override
			public void onPress(Button p_onPress_1_) {
				setHamachi();
			}
		}));
		this.addButton(new Button(this.width / 2 + 75, 150, 70, 20, new StringTextComponent("External"), new Button.IPressable() {
			@Override
			public void onPress(Button p_onPress_1_) {
				setExternal();
			}
		}));
		
	}
	
	
	@Override
	public void render(MatrixStack pMatrixStack, int pMouseX, int pMouseY, float pPartialTicks) {
		this.renderBackground(pMatrixStack);
		this.txtServerName.render(pMatrixStack, pMouseX, pMouseY, pPartialTicks);
		this.txtIpAddress.render(pMatrixStack, pMouseX, pMouseY, pPartialTicks);
		this.txtGameKind.render(pMatrixStack, pMouseX, pMouseY, pPartialTicks);
		
		super.render(pMatrixStack, pMouseX, pMouseY, pPartialTicks);
    }
	
	boolean setHamachi() {
		String ip;
		try {
			ip = SaveSync.getHamachiIP();
		} catch (Exception e) {
			SaveSync.LOGGER.error(e);
			ip = e.getMessage();
		}
		boolean re = ip == null;
		if(re) {
			ip = "failed to get";
		}
		txtIpAddress.setValue(ip);
		return !re;
	}
	
	boolean setExternal() {
		try {
			String ip = SaveSync.getExternalIp();
			txtIpAddress.setValue(ip);
			return true;
		} catch (IOException | HttpError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			SaveSync.LOGGER.error("Could not get external IP");
			txtIpAddress.setValue(":error:");
			return false;
		}
	}
	
	void done() {
		InetAddress addr = null;
		try {
			
    		addr = InetAddress.getByName(txtIpAddress.getValue());
		} catch(UnknownHostException e) {
			SaveSync.LOGGER.error(e);
		}
		if(addr == null || addr.isLoopbackAddress()) {
			txtIpAddress.setFocus(true);
			txtIpAddress.setTextColor(0xff0000);
			return;
		}

        this.minecraft.setScreen((Screen)null);


        int port = HTTPUtil.getAvailablePort();
        boolean success = this.minecraft.getSingleplayerServer().publishServer(
        		GameType.byName(getGameMode()), 
        		getCommands(), port);
        

        ITextComponent itextcomponent;
        if(success) {
        	itextcomponent = new StringTextComponent("Successfully published server on port " + port);
        } else {
        	itextcomponent = new StringTextComponent("Failed to publish server").withStyle(Style.EMPTY.withColor(Color.fromRgb(0xff0000)));
        }
        this.minecraft.gui.getChat().addMessage(itextcomponent);
        
        Minecraft mc = this.minecraft;
        
        if(success) {
        	// i know we're doing this if twice
        	JsonObject obj = getJson();
        	obj.addProperty("port", port);

        	try {
				SaveSync.PublishServer(obj, new IWebSocketHandler() {

					@Override
					public void OnPacket(WSPacket packet) {
				    	mc.gui.getChat().addMessage(
				    			new StringTextComponent(TextFormatting.GREEN + "Server should now be on the masterlist."));
					}

					@Override
					public void OnOpen() {
				    	mc.gui.getChat().addMessage(
				    			new StringTextComponent("Connection established, sending details"));
					}

					@Override
					public void OnClose(int errorCode, String reason) {
						// TODO Auto-generated method stub

				    	mc.gui.getChat().addMessage(
				    			new StringTextComponent("Connection closed: " + errorCode + ", " + reason));
					}

					@Override
					public void OnError(Exception error) {
						// TODO Auto-generated method stub

				    	mc.gui.getChat().addMessage(
				    			new StringTextComponent(TextFormatting.RED + "Connection errored: " + error.toString()));
					}
					
				});
			} catch (KeyManagementException | NoSuchAlgorithmException | InterruptedException e) {
				SaveSync.LOGGER.error(e);
			}
        	this.minecraft.gui.getChat().addMessage(
        			new StringTextComponent("Attempting to publish server details on https://ml-api.uk.ms/masterlist"));
        }
	}
}
