package cheale14.savesync.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cheale14.savesync.Environment;
import cheale14.savesync.SaveSync;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ServerEnvironment implements Environment {

	@Override
	public SaveInfo GetDefaultSave() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		File file = server.getServerDirectory();
		File world = new File(file, "world");
		try {
			return SaveInfo.Load(world);
		} catch (FileNotFoundException e) {
			return new SaveInfo(SaveSync.CONFIG.DefaultRepository.get(), "main", world);
		}
	}

	@Override
	public void OnServerStopped(MinecraftServer server) throws NoSuchFieldException, IllegalAccessException, GitAPIException, URISyntaxException, IOException {
		SaveInfo s = this.GetDefaultSave();
		if(s != null) {
			long time = server.overworld().getDayTime();
			long numDays = time % 24000;
			s.Upload(new Monitor((msg) -> {
				logger.info("[upload] " + msg);
			}), " on day " + numDays);
		}
	}
	
	JsonObject getDefaultMLAPIdata() {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", "null");
		obj.addProperty("ip", "127.0.0.1");
		obj.addProperty("port", "25565");
		return obj;
	}
	
	public boolean PublishFromJson(MinecraftServer server) {
		File mlapiData = new File(server.getServerDirectory(), "mlapi.json");
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
			return false;
		}
		
		try {
			SaveSync.PublishServer(obj, null);
			return true;
		} catch (KeyManagementException | NoSuchAlgorithmException | InterruptedException  e) {
			logger.error(e);
			e.printStackTrace();
			return false;
		}
		
	}
	
	@SubscribeEvent
	public void OnServerStarted(FMLServerStartedEvent event) {
		this.PublishFromJson(event.getServer());
	}
	
}
