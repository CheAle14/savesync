package cheale14.savesync.common;

import java.util.function.Consumer;

import org.eclipse.jgit.lib.ProgressMonitor;

class Monitor implements ProgressMonitor {
	
	public Monitor(Consumer<String> _log) {
		log = _log;
	}
	
	private Consumer<String> log;
	private int total;
	private int done;
	private boolean cancelled;
	@Override
	public void start(int totalTasks) {
		log.accept("Starting " + this.toString() + "; tasks: " + totalTasks);
	}

	@Override
	public void beginTask(String title, int totalWork) {
		total = totalWork;
		log.accept("Task: " + title + "; work: " + totalWork);
		done = 0;
	}

	@Override
	public void update(int completed) {
		done += completed;
		log.accept("  " + done + "/" + total);
	}

	@Override
	public void endTask() {
		// TODO Auto-generated method stub
		log.accept("  Done.");
		done = 0;
		total = 0;
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}
	public void Cancel() {
		this.cancelled = true;
	}
}