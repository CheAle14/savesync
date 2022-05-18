package cheale14.savesync.common;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import cheale14.savesync.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ServerEnvironment implements Environment {

	private Logger logger = LogManager.getLogger("savesync-cproxy");

	@Override
	public SyncSave GetDefaultSave() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		File file = server.getServerDirectory();
		try {
			return SyncSave.Load(new File(file, "world"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
}
