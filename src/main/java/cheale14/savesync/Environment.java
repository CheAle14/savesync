package cheale14.savesync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;

import cheale14.savesync.common.IWebSocketHandler;
import cheale14.savesync.common.SyncSave;
import cheale14.savesync.common.WSClient;
import cheale14.savesync.common.WSPacket;
import cheale14.savesync.common.commands.SyncCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands.EnvironmentType;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


public interface Environment {
	public Logger logger = LogManager.getLogger("savesync-proxy");
	
	public SyncSave GetDefaultSave();
	
	public void OnServerStopped(MinecraftServer server) throws NoSuchFieldException, IllegalAccessException, GitAPIException, URISyntaxException, IOException;

}
