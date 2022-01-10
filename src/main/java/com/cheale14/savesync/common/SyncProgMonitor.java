package com.cheale14.savesync.common;

import java.awt.Component;

import org.eclipse.jgit.lib.ProgressMonitor;

import com.cheale14.savesync.SaveSync;


public class SyncProgMonitor implements ProgressMonitor {
	private String currentTask;
	private int taskCount;
	private int thusFar;
	@Override
	public void beginTask(String arg0, int arg1) {
		taskCount = arg1;
		currentTask = arg0;
		thusFar = 0;
		SaveSync.logger.info(currentTask + " 0/" + taskCount);
	}

	@Override
	public void endTask() {
		thusFar = 0;
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
		thusFar += arg0;
		SaveSync.logger.info(pad + thusFar + "/" + taskCount);
	}

}
