package cheale14.savesync.common;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import cheale14.savesync.SaveSync;
import cheale14.savesync.client.gui.SyncProgressGui;
import cheale14.savesync.client.gui.SyncProgressGui.SyncType;

public class SyncThread extends Thread {
	
	
	private SyncProgressGui Gui;
	private SyncType Type;
	private SyncSave save;
	private boolean didError = false;
	
	private boolean doCancel = false;
	public SyncThread(SyncProgressGui gui, SyncSave _save) {
		Gui = gui;
		Type = gui.Type;
		save = _save;
	}
	
	void log(String message) {
		Gui.Append(save.getRepository() + ": " + message);
	}
	
	public boolean hasError() {
		return didError;
	}
	
	
	public void Cancel() {
		doCancel = true;
		SaveSync.LOGGER.info("Stopping " + this.toString());
	}
	
	@Override
	public void run() {
		SaveSync.LOGGER.info("Starting " + this.toString());
		
		try {
			if(Type.Pull) {
				save.Download(new Monitor());
			} else {
				save.Upload(new  Monitor());
			}
		} catch(Exception e) {
			log(e.toString());
			log("ERROR");
			didError = true;
			e.printStackTrace();
		}

		
		Gui.SetButtonDone();
		log("Thread Exited");
		SaveSync.LOGGER.info("Stopped! " + this.toString());
	}
	
	@Override
	public String toString() {
		return (Type.Pull ? "Pull" : "Push")
				+ " of " 
				+ (Type.All ? " all " : " only ") 
				+ (save.getRootDirectory().getAbsolutePath());
	}
	
	private class Monitor implements ProgressMonitor {
		private int total;
		private int done;
		@Override
		public void start(int totalTasks) {
			log("Starting " + this.toString() + "; tasks: " + totalTasks);
		}

		@Override
		public void beginTask(String title, int totalWork) {
			total = totalWork;
			log("Task: " + title + "; work: " + totalWork);
			done = 0;
		}

		@Override
		public void update(int completed) {
			done += completed;
			log("  " + done + "/" + total);
		}

		@Override
		public void endTask() {
			// TODO Auto-generated method stub
			log("  Done.");
			done = 0;
			total = 0;
		}

		@Override
		public boolean isCancelled() {
			return doCancel;
		}
	}
}
