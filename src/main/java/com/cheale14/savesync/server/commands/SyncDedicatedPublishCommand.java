package com.cheale14.savesync.server.commands;

import com.cheale14.savesync.SaveSync;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class SyncDedicatedPublishCommand extends CommandBase {

	@Override
	public String getName() {
		return "publish";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "commands.publish.usage";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		SaveSync.proxy.PublishFromJson(server);


		sender.sendMessage(new TextComponentString("We might be on MLAPI; please check server console for details."));
	}

}
