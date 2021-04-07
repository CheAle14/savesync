package com.cheale14.savesync;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.lib.ProgressMonitor;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class MCCommandMonitor implements ProgressMonitor {

	private final ICommandSender sender;
	private int totalWork;
	private int currentWork;
	public MCCommandMonitor(ICommandSender _sender) {
		sender = _sender;
	}
	
	ITextComponent suffixed(TextComponentString arg1) {
		return new TextComponentString("")
				.appendSibling(new TextComponentString("[savesync] ")
				.setStyle(new Style().setColor(TextFormatting.BLUE)))
				.appendSibling(arg1);
	}
	
	@Override
	public void beginTask(String arg0, int arg1) {
		// TODO Auto-generated method stub
		totalWork = arg1;
		currentWork = 0;
		sender.sendMessage(suffixed(
				new TextComponentString("Begun: " + arg0 + " " + arg1)));
	}

	@Override
	public void endTask() {
		// TODO Auto-generated method stub
		sender.sendMessage(suffixed(
				new TextComponentString("    -> Task done")));
	}

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void start(int arg0) {
		// TODO Auto-generated method stub
		sender.sendMessage(suffixed(
				new TextComponentString("Started, " + arg0 + " tasks to perform...")));
	}

	@Override
	public void update(int arg0) {
		// TODO Auto-generated method stub
		currentWork += arg0;
	}

}
