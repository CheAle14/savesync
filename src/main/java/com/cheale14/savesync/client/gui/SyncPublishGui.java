package com.cheale14.savesync.client.gui;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.lwjgl.input.Keyboard;

import com.cheale14.savesync.client.WSClient;
import com.cheale14.savesync.common.IWebSocketHandler;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.SaveSync.SaveConfig;
import com.cheale14.savesync.common.WSPacket;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiShareToLan;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;

public class SyncPublishGui extends GuiShareToLan {

	private GuiTextField txtServerName;
	private GuiTextField txtIpAddress;
	private GuiTextField txtGameKind;
	
	public SyncPublishGui(GuiScreen lastScreen) {
		super(lastScreen);
	}

	public JsonObject getJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", txtServerName.getText());
		obj.addProperty("ip", txtIpAddress.getText());
		obj.addProperty("mode", txtGameKind.getText());
		obj.addProperty("game", "minecraft");
		return obj;
	}
	
	public Object getProperty(String name) {
		try {
			Field f = GuiShareToLan.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.get(this);
		} catch (Exception e) {
			SaveSync.logger.error(e);
			return null;
		}
	}
	
	
	@Override
	public void initGui() {

		txtServerName = new GuiTextField(1, fontRenderer, this.width / 2 - 155, 125, 300, 20);
		txtServerName.setMaxStringLength(32);

		txtIpAddress = new GuiTextField(2, fontRenderer, this.width / 2 - 155, 150, 150, 20);
		
		txtGameKind = new GuiTextField(3, fontRenderer, this.width / 2 - 155, 175, 150, 20);
		txtGameKind.setText(SaveConfig.MLGameMode);
		txtGameKind.setEnabled(false);
		
		super.initGui();
		this.addButton(new GuiButton(10, this.width / 2 + 5, 150, 70, 20, "Hamachi"));
		this.addButton(new GuiButton(11, this.width / 2 + 75, 150, 70, 20, "External"));
	}
	

	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    	this.txtServerName.drawTextBox();
    	this.txtIpAddress.drawTextBox();
    	this.txtGameKind.drawTextBox();
    	
    	int white = Color.white.getRGB();
		this.drawString(fontRenderer, "Name: ", this.txtServerName.x - 30, this.txtServerName.y + 10, white);
		this.drawString(fontRenderer, "IP: ", this.txtIpAddress.x - 30, this.txtIpAddress.y + 10, white);
		this.drawString(fontRenderer, "Kind: ", this.txtGameKind.x - 30, this.txtGameKind.y + 10, white);
    	
    	super.drawScreen(mouseX, mouseY, partialTicks);
    }

	@Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    	this.txtServerName.textboxKeyTyped(typedChar, keyCode);
    	this.txtIpAddress.textboxKeyTyped(typedChar, keyCode);
    	this.txtGameKind.textboxKeyTyped(typedChar, keyCode);
    	
    	if(keyCode == Keyboard.KEY_TAB) {
    		if(txtServerName.isFocused()) {
    			txtServerName.setFocused(false);
    			txtIpAddress.setFocused(true);
    		} else if(txtIpAddress.isFocused()) {
    			txtIpAddress.setFocused(false);
    			txtGameKind.setFocused(true);
    		} else {
    			this.txtGameKind.setFocused(false);
    			txtServerName.setFocused(true);
    		}
    		return;
    	}
    	
    	super.keyTyped(typedChar, keyCode);
    }

	@Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    	this.txtServerName.mouseClicked(mouseX, mouseY, mouseButton);
    	this.txtIpAddress.mouseClicked(mouseX, mouseY, mouseButton);
    	this.txtGameKind.mouseClicked(mouseX, mouseY, mouseButton);
    	
    	super.mouseClicked(mouseX, mouseY, mouseButton);
    }
	

	@Override
    protected void actionPerformed(GuiButton button) throws IOException {
    	if(button.id == 101) {
    		InetAddress addr = null;
    		try {
    			
        		addr = InetAddress.getByName(txtIpAddress.getText());
    		} catch(UnknownHostException e) {
    			SaveSync.logger.error(e);
    		}
    		if(addr == null || addr.isLoopbackAddress()) {
    			txtIpAddress.setFocused(true);
    			txtIpAddress.setTextColor(Color.red.getRGB());
    			return;
    		}

            this.mc.displayGuiScreen((GuiScreen)null);
            String s = this.mc.getIntegratedServer().shareToLAN(
            		GameType.getByName((String)getProperty("gameMode")), 
            		(boolean)getProperty("allowCheats"));
            
            ITextComponent itextcomponent;
            if (s != null)
            {
                itextcomponent = new TextComponentTranslation("commands.publish.started", new Object[] {s});
            }
            else
            {
                itextcomponent = new TextComponentString("commands.publish.failed");
            }
            this.mc.ingameGUI.getChatGUI().printChatMessage(itextcomponent);
            
            Minecraft mc = this.mc;
            
            if(s != null) {
            	// i know we're doing this if twice
            	JsonObject obj = getJson();
            	obj.addProperty("port", s);

            	SaveSync.proxy.PublishServer(obj, new IWebSocketHandler() {

					@Override
					public void OnPacket(WSPacket packet) {
		            	mc.ingameGUI.getChatGUI().printChatMessage(
		            			new TextComponentString(TextFormatting.GREEN + "Server should now be on the masterlist."));
					}

					@Override
					public void OnOpen() {
		            	mc.ingameGUI.getChatGUI().printChatMessage(
		            			new TextComponentString("Connection established, sending details"));
					}

					@Override
					public void OnClose(int errorCode, String reason) {
						// TODO Auto-generated method stub

		            	mc.ingameGUI.getChatGUI().printChatMessage(
		            			new TextComponentString("Connection closed: " + errorCode + ", " + reason));
					}

					@Override
					public void OnError(Exception error) {
						// TODO Auto-generated method stub

		            	mc.ingameGUI.getChatGUI().printChatMessage(
		            			new TextComponentString(TextFormatting.RED + "Connection errored: " + error.toString()));
					}
            		
            	});
            	this.mc.ingameGUI.getChatGUI().printChatMessage(
            			new TextComponentString("Attempting to publish server details on " + SaveSync.MLAPI + "/masterlist"));
            }
    	} else if(button.id == 10) {
    		// hamachi
    		String ip;
			try {
				ip = SaveSync.getHamachiIP();
			} catch (Exception e) {
				SaveSync.logger.error(e);
				ip = e.getMessage();
			}
    		if(ip == null) {
    			ip = "failed to get";
    		}
    		txtIpAddress.setText(ip);
    	} else if(button.id == 11) {
    		// external
    		String ip = SaveSync.getExternalIp();

    		txtIpAddress.setText(ip);
    		
    	} else {
    		super.actionPerformed(button);
    	}
    }
}
