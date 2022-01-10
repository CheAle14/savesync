package com.cheale14.savesync.interop;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.cheale14.savesync.SaveSync;
import com.cheale14.savesync.common.SyncProgMonitor;
import com.feed_the_beast.mods.ftbbackups.BackupEvent;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ActiveFTBBackups extends DummyFTBBackups {
	@SubscribeEvent
	public void backupDone(BackupEvent.Post event) {
		if(event.getError() != null) {
			SaveSync.logger.info("Error occured during backup, so not autopushing.");
			return;
		}
		SaveSync.proxy.backupDone();
	}
}
