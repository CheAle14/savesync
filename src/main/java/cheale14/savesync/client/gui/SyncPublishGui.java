package cheale14.savesync.client.gui;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import com.google.gson.JsonObject;

import cheale14.savesync.SaveSync;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ShareToLanScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
	
	public Object getProperty(String name) {
		try {
			Field f = ShareToLanScreen.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.get(this);
		} catch (Exception e) {
			SaveSync.LOGGER.error(e);
			return null;
		}
	}
	
	
	@Override
	public void init() {

		txtServerName = new TextFieldWidget(font, this.width / 2 - 155, 125, 300, 20, new StringTextComponent("Name"));
		txtServerName.setMaxLength(32)
		txtIpAddress = new TextFieldWidget(font, this.width / 2 - 155, 150, 150, 20, new StringTextComponent("127.0.0.1"));
		txtGameKind = new TextFieldWidget(font, this.width / 2 - 155, 175, 150, 20, new StringTextComponent("modded/omnifactory"));
		txtGameKind.setValue("modded/x");
		txtGameKind.active = true;
		
		this.addWidget(txtServerName);
		this.addWidget(txtIpAddress);
		this.addWidget(txtGameKind);
		
		
		super.init();
		this.addButton(new Button(this.width / 2 + 5, 150, 70, 20, new StringTextComponent("Hamachi"), new Button.IPressable() {
			@Override
			public void onPress(Button p_onPress_1_) {
				
			}
		}));
		this.addButton(new Button(this.width / 2 + 75, 150, 70, 20, new StringTextComponent("External"), new Button.IPressable() {
			@Override
			public void onPress(Button p_onPress_1_) {
				// TODO Auto-generated method stub
				
			}
		}));
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

            	try {
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
				} catch (KeyManagementException | NoSuchAlgorithmException | InterruptedException e) {
					SaveSync.logger.error(e);
				}
            	this.mc.ingameGUI.getChatGUI().printChatMessage(
            			new TextComponentString("Attempting to publish server details on https://ml-api.uk.ms/masterlist"));
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
