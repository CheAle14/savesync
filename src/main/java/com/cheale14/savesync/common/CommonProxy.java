package com.cheale14.savesync.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.SSLHandshakeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.ProgressMonitor;

import com.cheale14.savesync.client.WSClient;
import com.cheale14.savesync.common.SaveSync.SaveConfig;
import com.cheale14.savesync.server.commands.SyncCommand;
import com.cheale14.savesync.server.commands.SyncDedicatedPublishCommand;
import com.cheale14.savesync.server.commands.SyncPublishCommand;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandPublishLocalServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {

	protected Logger logger = LogManager.getLogger("savesync-proxy");
	
	JsonObject getDefaultMLAPIdata() {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", "null");
		obj.addProperty("ip", "127.0.0.1");
		obj.addProperty("port", "25565");
		return obj;
	}
	
    public void serverStart(FMLServerStartingEvent event) {
		logger.info("Server starting, side: " + event.getSide());
    	event.registerServerCommand(new SyncCommand());
    }

	void warnStartups(ICommandSender sender, MinecraftServer server) {
    	if(!SaveSync.HasAPIKey()) {
    		sender.sendMessage(new TextComponentString("The server has not set their GitHub personal access token for save syncing.")
    				.setStyle(new Style().setColor(TextFormatting.RED)));
    	}
    	if(!SaveSync.loadedSync) {
    		sender.sendMessage(new TextComponentString("Error: savesync did not load properly - please check logs.")
    				.setStyle(new Style().setColor(TextFormatting.RED)));
    	} else {
	    	File root = server.getWorld(0).getSaveHandler().getWorldDirectory();
	    	if(root != null) {
				sender.sendMessage(new TextComponentString(root.getAbsolutePath()));
				File syncFile = new File(root, SaveSync.SYNCNAME);
				if(syncFile.exists()) {
		    		sender.sendMessage(new TextComponentString("World should be saved upon exit (or /sync now) to " + SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName)
	    				.setStyle(new Style().setColor(TextFormatting.GREEN)));
				} else {
		    		sender.sendMessage(new TextComponentString("Notice: This save will not be synced to github. To setup, use /sync now [branch]")
		    				.setStyle(new Style().setColor(TextFormatting.RED)));
				}
				
	    	} else {
	    		sender.sendMessage(new TextComponentString("[savesync] Could not find data directory?")
	    				.setStyle(new Style().setColor(TextFormatting.RED)));
	    	}
    	}
    }
	
	public void PublishFromJson(MinecraftServer server) {
		File mlapiData = new File(server.getDataDirectory(), "mlapi.json");
		logger.info("Looking for MLAPI data at: " + mlapiData.getAbsolutePath());
		
		JsonObject obj;
		Gson gson = new Gson();
		if(!mlapiData.exists()) {
			logger.warn("File does not exist, creating...");
			obj = getDefaultMLAPIdata();
			
			try {
				mlapiData.createNewFile();
    			try(FileWriter writer = new FileWriter(mlapiData)) {
    				writer.write(gson.toJson(obj));
    				writer.close();
    			}
	    	} catch (IOException e) {
				logger.error("Failed to save default MLAPI data ", e);
			}
		} else {
			try(FileReader reader = new FileReader(mlapiData)) {
				obj = gson.fromJson(reader, JsonObject.class);
			} catch (IOException e) {
				logger.error("Failed to read MLAPI data ", e);
				obj = getDefaultMLAPIdata();
			}
		}
		
		if(obj.get("name").getAsString().equals("null")) {
			logger.warn("MLAPI data remains at the default of null!");
			return;
		}
		
		try {
			this.PublishServer(obj, null);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			logger.error(e);
		}
	}
    
    public void serverStarted(FMLServerStartedEvent event) {
    	logger.info("Server started.");
    	MinecraftServer sender = FMLCommonHandler.instance().getMinecraftServerInstance();
    	warnStartups(sender, sender);

        CommandHandler ch = (CommandHandler) sender.getCommandManager();
    	if(sender.isDedicatedServer()) {
    		ch.registerCommand(new SyncDedicatedPublishCommand());
    		if(!SaveConfig.SyncServerConnect) {
    			return;
    		}
    			
    		PublishFromJson(sender);
    	} else {
	        ch.registerCommand(new SyncPublishCommand());
		}
    }

	public void serverStopped(FMLServerStoppedEvent event) {
    	logger.info("Server stopped, starting upload");
    	// this is on the dedicated server, so it should just be
    	// the world folder.
    	File file = GetSyncFolders().get(0);
    	try {
			SaveSync.SyncUpload(file, new SyncProgMonitor());
		} catch (GitAPIException | IOException | URISyntaxException e) {
			logger.error(e);
			logger.error("Sync may not have suceeded properly!");
		}
	}

	public void serverStopping(FMLServerStoppingEvent event) {
    	logger.info("Server stopping.");
	}
	
	
	
	private WSClient websocket;
	private String wsId;
	
	public void PublishServer(JsonObject serverData, IWebSocketHandler handler) throws KeyManagementException, NoSuchAlgorithmException {
		if(wsId != null) {
			serverData.addProperty("id", wsId);
		}
		logger.info("UpsertServer: " + serverData.toString());
		URI uri = URI.create(SaveSync.WS_URI(false));
		

		WSPacket packet = new WSPacket();
		packet.Id("UpsertServer");
		packet.Content(serverData);
		Gson gson = new Gson();
		String sending = gson.toJson(packet);
		
		if(websocket == null) {
			websocket = new WSClient(uri, new IWebSocketHandler() {
				@Override
				public void OnPacket(WSPacket packet) {
					if(packet.Id().equals("UpsertServer")) {
						if(packet.Content() instanceof JsonObject) {
	
							wsId = ((JsonObject)packet.Content()).get("id").getAsString();
						}
					}
					if(handler != null)
						handler.OnPacket(packet);
				}
				@Override
				public void OnOpen() {
					websocket.send(sending);
					
					if(handler != null)
						handler.OnOpen();
				}
				@Override
				public void OnClose(int errorCode, String reason) {
					if(handler != null)
						handler.OnClose(errorCode, reason);
				}
				@Override
				public void OnError(Exception error) {
					if(handler != null)
						handler.OnError(error);
				}
				
			});
			websocket.connect();
		} else {
			websocket.send(sending);
		}
	}
	
	@SubscribeEvent
	public void commandExecuted(CommandEvent event) {
		ICommand cmd = event.getCommand();
		logger.info("Executing '/" + cmd.getName() + "' - " + cmd.getClass().getName());
	}
	
	
	@SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if(event.player.world.isRemote)
			return;
    	Side side = FMLCommonHandler.instance().getEffectiveSide();
    	logger.info(side + " effective");
    	logger.info(FMLCommonHandler.instance().getSide() + " actual");
    	if(side != Side.SERVER)
    		return;
    	EntityPlayer player = event.player;
    	warnStartups(player, player.getServer());
    	/*if(!SaveSync.loadedSync) {
    		return; // don't bother with hamachi if we're not syncing
    	}
    	if(!SaveConfig.SyncServerConnect) {
    		return;
    	}
    	try {
    		SaveSync.hamachiIP = SaveSync.getHamachiIP();
    		SaveSync.hamachiRunning = SaveSync.hamachiIP != null;
		} catch (IOException | InterruptedException e) {
			logger.error(e);
			return;
		}
    	if(!SaveSync.hamachiRunning) {
    		player.sendMessage(new TextComponentString("Hamachi is not running, perhaps you should turn it on?"));
    		return;
    	}
    	MinecraftServer server = player.getServer();
    	if(SaveSync.lanPort == null) {
    		try {
    			SaveSync.hamachiIP = SaveSync.getHamachiIP();
    			if(SaveSync.hamachiIP == null) {
    	    		player.sendMessage(new TextComponentString("Could not determine hamachi IP automatcally. You'll need to do this part yourself"));
    	    		return;
    			}
    	    	if(SaveSync.hamachiIP != null) {
    	    		SaveSync.hamachiRunning = true;
    	    		SaveSync.lanPort = server.shareToLAN(GameType.SURVIVAL, true);
	    			player.sendMessage(new TextComponentString(
	    					"Opened to lan: " + SaveSync.hamachiIP + ":" + SaveSync.lanPort
	    					));
	    			this.PublishServer(null);
    	    		try {
        				String s = SaveSync.PutServer();
	    				if(s != null) {
	    		    		player.sendMessage(new TextComponentString(s)
	    		    				.setStyle(new Style().setColor(TextFormatting.RED)));
	    		    		return;
	    				}
    	    		} catch(SSLHandshakeException e) {
    	    			logger.error(e);
    	    			player.sendMessage(new TextComponentString(
						"Failed to set IP on the cloud, you may need to apply security fixes to cacert"
    	    					).setStyle(new Style().setColor(TextFormatting.RED)));
    	    			
    	    		}
    	    	}
    	    	player.sendMessage(new TextComponentString("This server should automatically be found on the server list via " 
    	    	+ SaveSync.hamachiIP + ":" + SaveSync.lanPort));
        	}
    		catch (IOException | InterruptedException e1) {
    			// TODO Auto-generated catch block
    			logger.error(e1);
	    		player.sendMessage(new TextComponentString("Failed to automatically publish server connection info, check log for error")
	    				.setStyle(new Style().setColor(TextFormatting.RED)));
	    		return;
    		}
    	}*/
    }


	public void backupDone() {
		File worldFolder = new File(FMLCommonHandler.instance().getSavesDirectory(), "world");
		if(!SaveSync.IsSyncFolder(worldFolder)) {
			SaveSync.logger.info("Not sync folder, not acting.");
			return;
		}
		SaveSync.logger.info("Automatically syncing as backup has been done.");
		try {
			SaveSync.SyncUpload(worldFolder, new SyncProgMonitor());
		} catch (GitAPIException | IOException | URISyntaxException e) {
			SaveSync.logger.error(e);
		}
	}
    
    public List<File> GetSyncFolders() {
    	List<File> ls = new LinkedList<File>();
    	File srv = FMLCommonHandler.instance().getSavesDirectory();
    	File worldFolder = new File(srv, "world");
    	logger.info("Server world folder: " + worldFolder.getAbsolutePath());
    	if(worldFolder.exists()) {
    		ls.add(worldFolder);
    	}
    	return ls;
    }
    
    public File GetSaveFolder() {
    	return FMLCommonHandler.instance().getSavesDirectory();
    }
    
    public File GetDefaultBranchFolder() {
    	return new File(GetSaveFolder(), "world");
    }
    
    
}
