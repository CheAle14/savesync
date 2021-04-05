package com.cheale14.savesync;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
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
			String branch = SaveSync.saveRepo.getDefaultBranch();
			if(args.length == 2) {
				branch = args[1];
			} else {
				SaveSync.logger.warn("No branch provided, assuming default of " + branch);
			}
			File saveFolder = DimensionManager.getCurrentSaveRootDirectory();
			SaveSync.logger.info(saveFolder.getAbsolutePath());
			try {
				File syncFile = new File(saveFolder, "SYNC.txt");
				boolean created = syncFile.createNewFile();
				if(created) {
					SaveSync.logger.info("Sync file created, writing branch name");
					SaveSync.WriteSyncBranch(saveFolder, branch);
					sender.sendMessage(new TextComponentString("Successfully registered this world to sync with " + branch + " branch"));
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
				SaveSync.SyncUpload(DimensionManager.getCurrentSaveRootDirectory());
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
			File syncFile = new File(saveFolder, "SYNC.txt");
			syncFile.delete();
			sender.sendMessage(new TextComponentString("This world will no longer be automatically synced"));
		} else {
			sender.sendMessage(new TextComponentString("Option '" + args[0] + "' is invalid")
					.setStyle(new Style().setColor(TextFormatting.RED)));
		}
				
	}

}
