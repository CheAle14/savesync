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
import cheale14.savesync.SaveSync;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ServerEnvironment implements Environment {

	@Override
	public SyncSave GetDefaultSave() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		File file = server.getServerDirectory();
		File world = new File(file, "world");
		try {
			return SyncSave.Load(world);
		} catch (FileNotFoundException e) {
			return new SyncSave(SaveSync.CONFIG.DefaultRepository.get(), "main", world);
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
