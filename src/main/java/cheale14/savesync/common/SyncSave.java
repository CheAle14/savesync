package cheale14.savesync.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import cheale14.savesync.SaveSync;

public class SyncSave {
	public static final String SYNCFILE_NAME = "SYNC.txt";
	
	private String _repository;
	private String _branch;
	private File _rootDir;
	
	public SyncSave(String repository, String branch, File rootDir) {
		_repository = repository;
		_branch = branch;
		_rootDir = rootDir;
	}
	
	public String getRepository() {
		return _repository;
	}
	public String getBranch() {
		return _branch;
	}
	public File getRootDirectory() {
		return _rootDir;
	}
	public String getURL() {
		return "https://github.com/" + _repository + ".git";
	}
	
	private UsernamePasswordCredentialsProvider auth() {
    	return new UsernamePasswordCredentialsProvider(SaveSync.CONFIG.ApiKey.get(), "");
    }
	
	
	
	void Clone(ProgressMonitor monitor) throws IOException, InvalidRefNameException, IllegalStateException, GitAPIException, URISyntaxException {
		Logger logger = LogManager.getLogger("SyncClone");
		try(Git git = Git.init()
    			.setDirectory(_rootDir)
    			.setInitialBranch(_branch)
    			.call()
    			) {
    		String url = this.getURL();
    		logger.info("Init repo, now pulling from " + url);
    		git.remoteAdd()
	    		.setName("origin")
	    		.setUri(new URIish(url))
	    		.call();
    		git.pull()
    			.setCredentialsProvider(auth())
    			.setRemote("origin")
    			.setProgressMonitor(monitor)
    			.call();
    		logger.info("Successfully cloned " + _branch + " into " + _rootDir.getAbsolutePath());
    		SaveSync.FixNBT(_rootDir);
    	}
    	WriteTo(_rootDir);
	}
	
	public void Download(ProgressMonitor monitor) throws WrongRepositoryStateException, InvalidConfigurationException, InvalidRemoteException, CanceledException, RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException, GitAPIException, IllegalStateException, IOException, URISyntaxException {
		Logger logger = LogManager.getLogger("SaveSync");
		
		File gitDir = new File(_rootDir, ".git");
		if(!_rootDir.exists()) {
			logger.info("Directory does not exist, creating " + _rootDir);
			_rootDir.mkdir();
		}
		if(!gitDir.exists()) {
			logger.info("Git directory does not exist, cloning " + gitDir);
			Clone(monitor);
			return;
		}
		
    	Git git = Git.open(_rootDir);

		/*git.fetch()
    		.setCredentialsProvider(auth())
    		.setProgressMonitor(new SyncProgMonitor())
    		.call();*/
		try {
    		git.pull()
    			.setCredentialsProvider(auth())
    			.setProgressMonitor(monitor)
    			.call();
    		SaveSync.FixNBT(_rootDir);
		} catch(CheckoutConflictException e) {
			git.close();
			logger.warn("Save " + _rootDir.getName() + " has conflict with upstream changes.");
			// Ideas:
			// 1) Commit existing changes to branch
			// 2) Create & checkout new branch, randomly generated
			// 3) Bring committ over to new branch
			// 4) Purge old folder, re-download it.
			Random rnd = new Random();
			int conInd = 0;
			File newWorld;
			do {
				conInd++;
				newWorld = new File(_rootDir, "-c_" + conInd);
			} while(newWorld.exists());
			String newBranch = "conflict-" + conInd;
			logger.info("Selected " + newBranch + " as new conflict branch");
			
			logger.info("Attempting to rename " + _rootDir + " -> " + newWorld);
			int moved = IOHelper.Move(_rootDir, newWorld);
			logger.info("Renamed, " + moved + " files moved");
			SyncSave newSave = SyncSave.Load(newWorld);
			newSave.WriteTo(newWorld);
			
			// Checkout new branch
			logger.info("Renamed folder over, moving to new branch...");
			git = Git.open(newWorld);
			Ref branch = git.branchCreate().setName(newBranch).call();
			git.checkout().setName(newBranch).call();
			git.close();
			
			newSave.Upload(monitor);
			logger.info("Pushed conflicts to their own branch.");
			logger.info("We can now attempt to make sure the old folder is deleted and pull it again");
			_rootDir.delete();
			logger.info("Folder deleted, cloning...");
			Clone(monitor);
		}
	}
	
	public void Upload(ProgressMonitor monitor) throws GitAPIException, URISyntaxException, IOException {
		Logger logger = LogManager.getLogger("SyncUpload");
    	Git git = null;
    	if(_rootDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return ".git".equals(name);
			}
    	}).length == 0) {
    		monitor.beginTask("Initializing git folder", 2);
    		InitCommand init = Git.init();
    		init.setBare(false);
    		init.setDirectory(_rootDir);
    		init.setInitialBranch(_branch);
    		monitor.update(1);
    		
    		String url = this.getURL();
    		logger.info("Remote URL: " + url);
    		git = init.call();
    		git.remoteAdd()
	    		.setName("origin")
	    		.setUri(new URIish(url))
	    		.call();
    		monitor.update(1);
    		monitor.endTask();
    		
    	} else {
    		git = Git.open(_rootDir);
    	}
    	monitor.beginTask("Fixing NBT to neutral perpsective", 1);
    	SaveSync.FixNBT(_rootDir);
    	monitor.endTask();
    	
    	monitor.beginTask("Fetching from remote", 1);
    	git.fetch().setCredentialsProvider(auth())
    		.call();
    	monitor.endTask();
    	monitor.beginTask("Calculating changes", 1);
    	List<DiffEntry> diff = git.diff().call();
    	logger.info("Number of differences from remote: " + diff.size());
    	for(DiffEntry entry : diff) {
    		logger.info(entry.getChangeType().toString() + ": " + entry.getOldPath() + " -> " + entry.getNewPath());
    	}
    	monitor.endTask();
    	git.add().addFilepattern(".").call();
    	git.commit().setSign(false).setMessage("Automatically syncing changes").call();
    	PushCommand push = git.push();
    	push.setCredentialsProvider(auth());
		push.setProgressMonitor(monitor);
		push.call();
		git.close();
		logger.info("Successfully pushed?");
	}
	
	public static boolean IsSyncedDirectory(File directory) {
		if(!directory.isDirectory()) return false;
		return new File(directory, SYNCFILE_NAME).exists();
	}
	
	public static SyncSave Load(File syncFile) throws FileNotFoundException {
		if(syncFile.isDirectory()) {
			syncFile = new File(syncFile, SYNCFILE_NAME);
		}
		List<String> lines = IOHelper.ReadAllLines(syncFile);
		return new SyncSave(lines.get(1), lines.get(0), syncFile.getParentFile());
	}
	
	public static List<SyncSave> LoadAll(File saveDirectory) throws FileNotFoundException {
		List<SyncSave> list = new ArrayList<SyncSave>();
		for(File dir : saveDirectory.listFiles()) {
			if(dir.isDirectory()) {
				File syncFile = new File(dir, SYNCFILE_NAME);
				if(syncFile.exists()) {
					list.add(SyncSave.Load(syncFile));
				}
			}
		}
		return list;
	}
	public void WriteTo(File syncFile) throws IOException {
		if(syncFile.isDirectory()) {
			syncFile = new File(syncFile, SYNCFILE_NAME);
		}
		IOHelper.WriteAllText(syncFile, _branch + "\r\n" + _repository + "\r\n");
		
	}
}
