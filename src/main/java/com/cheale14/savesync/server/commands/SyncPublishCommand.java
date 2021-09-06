package com.cheale14.savesync.server.commands;

import com.cheale14.savesync.client.gui.SyncPublishGui;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandPublishLocalServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

public class SyncPublishCommand extends CommandPublishLocalServer {
	
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {


    	if(sender.getEntityWorld().isRemote) {
    		return;
    	}
    		

    	Minecraft mc = Minecraft.getMinecraft();
    	mc.displayGuiScreen(new SyncPublishGui(mc.currentScreen));
    }

}
