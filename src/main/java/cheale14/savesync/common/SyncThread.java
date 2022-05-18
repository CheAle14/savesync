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
	private File folder;
	private boolean didError = false;
	
	private boolean doCancel = false;
	public SyncThread(SyncProgressGui gui, File _folder) {
		Gui = gui;
		Type = gui.Type;
		folder = _folder;
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
				if(Type.All) {
					for(SyncSave save : SyncSave.LoadAll(folder)) {
						save.Download(new Monitor());	
					}
				} else {
					SyncSave.Load(folder).Download(new Monitor());
				}
			} else {
				if(Type.All) {
					for(SyncSave save : SyncSave.LoadAll(folder)) {
						save.Download(new Monitor());	
					}
				} else {
					SyncSave.Load(folder).Download(new Monitor());
				}
			}
		} catch(Exception e) {
			Gui.Append(e.toString());
			Gui.Append("ERROR");
			didError = true;
			e.printStackTrace();
		}

		
		Gui.SetButtonDone();
		Gui.Append("Thread Exited");
		SaveSync.LOGGER.info("Stopped! " + this.toString());
	}
	
	@Override
	public String toString() {
		return (Type.Pull ? "Pull" : "Push")
				+ " of " 
				+ (Type.All ? " all " : " only ") 
				+ (folder.getAbsolutePath());
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
