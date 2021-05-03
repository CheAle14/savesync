package com.cheale14.savesync.common;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import com.cheale14.savesync.client.gui.SyncProgressGui;
import com.cheale14.savesync.client.gui.SyncProgressGui.SyncType;

public class SyncThread extends Thread {
	
	
	private SyncProgressGui Gui;
	private SyncType Type;
	private File Folder;
	private boolean doCancel = false;
	public SyncThread(SyncProgressGui gui, File worldFolder) {
		Gui = gui;
		Type = gui.Type;
		Folder = worldFolder;
	}
	
	public void Cancel() {
		doCancel = true;
		SaveSync.logger.info("Stopping " + this.toString());
	}
	
	@Override
	public void run() {
		SaveSync.logger.info("Starting " + this.toString());
		
		try {
			if(Type.Pull) {
				if(Type.All) {
					SaveSync.SyncDownload(new Monitor());
				} else {
					SaveSync.SyncDownload(Folder, new Monitor());
				}
			} else {
				if(Type.All) {
					SaveSync.SyncUploadAll(new Monitor());
				} else {
					SaveSync.SyncUpload(Folder, new Monitor());
				}
			}
		} catch(Exception e) {
			Gui.Append(e.toString());
			Gui.Append("ERROR");
		}

		
		Gui.SetButtonDone();
		Gui.Append("Thread Exited");
		SaveSync.logger.info("Stopped! " + this.toString());
	}
	
	@Override
	public String toString() {
		return (Type.Pull ? "Pull" : "Push")
				+ " of " 
				+ (Type.All ? " all " : " only ") 
				+ (Folder == null ? "" : Folder.getAbsolutePath());
	}
	
	private class Monitor implements ProgressMonitor {
		private int total;
		private int done;
		@Override
		public void start(int totalTasks) {
			Gui.Append("Starting " + this.toString() + "; tasks: " + totalTasks);
		}

		@Override
		public void beginTask(String title, int totalWork) {
			total = totalWork;
			Gui.Append("Task: " + title + "; work: " + totalWork);
			done = 0;
		}

		@Override
		public void update(int completed) {
			done += completed;
			Gui.Append("  " + done + "/" + total);
		}

		@Override
		public void endTask() {
			// TODO Auto-generated method stub
			Gui.Append("  Done.");
			done = 0;
			total = 0;
		}

		@Override
		public boolean isCancelled() {
			return doCancel;
		}
	}
}
