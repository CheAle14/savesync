package com.cheale14.savesync;

import java.awt.Component;

import org.eclipse.jgit.lib.ProgressMonitor;


public class SyncProgMonitor implements ProgressMonitor {
	private String currentTask;
	private int taskCount;
	@Override
	public void beginTask(String arg0, int arg1) {
		taskCount = arg1;
		currentTask = arg0;
		SaveSync.logger.info(currentTask + " 0/" + taskCount);
	}

	@Override
	public void endTask() {
		SaveSync.logger.info(currentTask = " - Done");
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void start(int arg0) {
		SaveSync.logger.info("Start " + arg0);
	}

	@Override
	public void update(int arg0) {
		String pad = new String(new char[currentTask.length() + 1]).replace("\0", " ");
		SaveSync.logger.info(pad + arg0 + "/" + taskCount);
	}

}
