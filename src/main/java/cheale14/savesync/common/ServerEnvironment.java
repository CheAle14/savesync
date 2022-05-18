package cheale14.savesync.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import com.google.gson.JsonObject;

import cheale14.savesync.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ServerEnvironment implements Environment {

	@Override
	public SyncSave GetDefaultSave() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		File file = server.getServerDirectory();
		try {
			return SyncSave.Load(new File(file, "world"));
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	@Override
	public void OnServerStopped(MinecraftServer server) throws NoSuchFieldException, IllegalAccessException, GitAPIException, URISyntaxException, IOException {
		SyncSave s = this.GetDefaultSave();
		if(s != null) {
			s.Upload(new Monitor((msg) -> {
				logger.info("[upload] " + msg);
			}));
		}
	}
	
}
