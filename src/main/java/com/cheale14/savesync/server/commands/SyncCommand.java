package com.cheale14.savesync.server.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.cheale14.savesync.common.MCCommandMonitor;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.SyncFileInfo;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;

public class SyncCommand extends CommandBase {

	@Override
	public String getName() {
		return "sync";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "command.sync.usage";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(args.length == 0) {
			sender.sendMessage(new TextComponentString("Error: You did not use this command correctly")
					.setStyle(new Style().setColor(TextFormatting.RED)));
			return;
		}
		if("register".equalsIgnoreCase(args[0])) {
			String branch = null;
			String repoSlug = null;
			if(args.length == 2) {
				String[] split = args[1].split("#");
				if(split.length == 1) {
					repoSlug = split[0];
					branch = "main";
				} else if(split.length == 2) {
					repoSlug = split[0];
					if(repoSlug.split("/").length != 2) {
						repoSlug = null;
					}
				}
			}
			if(StringUtils.isNullOrEmpty(repoSlug) || StringUtils.isNullOrEmpty(branch)) {
				sender.sendMessage(new TextComponentString("Usage: /sync register RepoOwner/RepoName#Branch | e.g. CheAle14/some-repo#main")
						.setStyle(new Style().setColor(TextFormatting.RED)));
				return;
			}
			File saveFolder = DimensionManager.getCurrentSaveRootDirectory();
			SaveSync.logger.info(saveFolder.getAbsolutePath());
			try {
				File syncFile = new File(saveFolder, "SYNC.txt");
				boolean created = syncFile.createNewFile();
				if(created) {
					SaveSync.logger.info("Sync file created, writing branch name");
					SyncFileInfo syncInfo = new SyncFileInfo(branch, repoSlug);
					syncInfo.ToFile(new File(saveFolder, SaveSync.SYNCNAME));
					sender.sendMessage(new TextComponentString("Successfully registered this world to sync with " + syncInfo.toString()));
				} else {
					sender.sendMessage(new TextComponentString("Error: Failed to create sync file")
							.setStyle(new Style().setColor(TextFormatting.RED)));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				SaveSync.logger.error(e);
			}
		} else if("now".equalsIgnoreCase(args[0])) {
			try {
				SaveSync.SyncUpload(DimensionManager.getCurrentSaveRootDirectory(), new MCCommandMonitor(sender));
				sender.sendMessage(new TextComponentString("Synced")
						.setStyle(new Style().setColor(TextFormatting.GREEN)));
			} catch (GitAPIException | IOException | URISyntaxException e) {
				// TODO Auto-generated catch block
				SaveSync.logger.error(e);
				sender.sendMessage(new TextComponentString("An " + e.getClass().getSimpleName() + " occured when attempting to sync")
						.setStyle(new Style().setColor(TextFormatting.RED)));
			}
		} else if("stop".equalsIgnoreCase(args[0])) {
			File saveFolder = DimensionManager.getCurrentSaveRootDirectory();
			File syncFile = new File(saveFolder, SaveSync.SYNCNAME);
			syncFile.delete();
			sender.sendMessage(new TextComponentString("This world will no longer be automatically synced"));
		} else {
			sender.sendMessage(new TextComponentString("Option '" + args[0] + "' is invalid")
					.setStyle(new Style().setColor(TextFormatting.RED)));
		}
				
	}

}
