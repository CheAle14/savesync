package com.cheale14.savesync;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;

@Mod(modid = SaveSync.MODID, name = SaveSync.NAME, version = SaveSync.VERSION)
public class SaveSync
{
    public static final String MODID = "savesync";
    public static final String NAME = "Save Sync";
    public static final String VERSION = "0.1";

    public static Logger logger;
    private static GitHub github;
    public static GHRepository saveRepo;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) throws NoWorkTreeException, InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException, SyncException
    {
        // some example code
    	logger.info("Repo: https://github.com/" + SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName);
		LoadGithub();
    }
    
    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
    	event.registerServerCommand(new SyncCommand());
    }
    
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
    	logger.info("Server is stopping");
    }
    
    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
    	logger.info("Server has stopped, syncing");
    	try {
			SyncUploadAll();
		} catch (GitAPIException | IOException | URISyntaxException e) {
			logger.error(e);
		}
    }
    
    
    public void LoadGithub() throws NoWorkTreeException, InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException, SyncException {
    	github = new GitHubBuilder().withOAuthToken(SaveConfig.API_Key).build();
    	try {
	    	GHMyself user = github.getMyself();
	    	logger.info("User: " + user.getName());
	    	saveRepo = github.getRepository(SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName);
    	} catch(HttpException ex) {
    		if(ex.getMessage().contains("Bad credentials")) {
    			Runtime rt = Runtime.getRuntime();
    			String url = "https://github.com/settings/tokens/new";
    			rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
    		}
    		github = null;
    		return;
    	}
		SyncDownload();
    }
    
    private static String readFile(File file) throws FileNotFoundException {
    	Scanner reader = new Scanner(file);
    	String text = "";
    	while(reader.hasNextLine()) {
    		text += reader.nextLine();
    	}
    	reader.close();
    	return text;
    }
    
    static UsernamePasswordCredentialsProvider auth() {
    	return new UsernamePasswordCredentialsProvider(SaveConfig.API_Key, "");
    }
    static String remoteUrl() {
		return "https://github.com/" + SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName + ".git";
    }
    
    public void SyncUploadAll() throws GitAPIException, IOException, URISyntaxException {
    	for(File folder : GetSyncFolders()) {
    		SyncUpload(folder);
    	}
    }
    
    public static boolean IsSyncFolder(File worldFolder) {
    	File syncFile = new File(worldFolder, "SYNC.txt");
    	return worldFolder.isDirectory() && syncFile.exists() && syncFile.isFile();
    }
    public static void WriteSyncBranch(File worldFolder, String branch) throws IOException {
    	File syncFile = new File(worldFolder, "SYNC.txt");
    	try(FileWriter writer = new FileWriter(syncFile)) {
			writer.write(branch);
			writer.close();
    	}
    }
    
    
    public static void SyncUpload(File world) throws GitAPIException, IOException, URISyntaxException {
    	String branchName = readFile(new File(world, "SYNC.txt"));
    	Git git = null;
    	if(world.listFiles(new NameFilter(".git")).length == 0) {
    		SaveSync.logger.info("No .git folder, so we'll need to init");
    		InitCommand init = Git.init();
    		init.setBare(false);
    		init.setDirectory(world);
    		init.setInitialBranch(branchName);
    		
    		String url = remoteUrl();
    		SaveSync.logger.info("Remote URL: " + url);
    		git = init.call();
    		git.remoteAdd()
	    		.setName("origin")
	    		.setUri(new URIish(url))
	    		.call();

    		
    	} else {
    		git = Git.open(world);
    	}
    	SaveSync.logger.info("Fetching from remote");
    	git.fetch().setCredentialsProvider(auth())
    		.call();
    	List<DiffEntry> diff = git.diff().call();
    	SaveSync.logger.info("Number of differences from remote: " + diff.size());
    	for(DiffEntry entry : diff) {
    		logger.info(entry.getChangeType().toString() + ": " + entry.getOldPath() + " -> " + entry.getNewPath());
    	}
    	git.add().addFilepattern(".").call();
    	git.commit().setSign(false).setMessage("Automatically syncing changes").call();
    	PushCommand push = git.push();
    	push.setCredentialsProvider(auth());
		push.setProgressMonitor(new SyncProgMonitor());
		push.call();
		git.close();
		SaveSync.logger.info("Successfully pushed?");
    }
    
    public List<File> GetSyncFolders() {
    	File saveFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves");
    	List<File> files = new LinkedList<File>();
    	for(File worldFolder : saveFolder.listFiles()) {
    		if(IsSyncFolder(worldFolder)) {
        		logger.info(worldFolder.getPath());
        		files.add(worldFolder);
    		}
    	}
    	return files;
    }
    
    public void SyncClone(File to, String branch) throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException {
    	if(!to.exists()) {
    		to.mkdir();
    	}
    	try(Git git = Git.init()
    			.setDirectory(to)
    			.setInitialBranch(branch)
    			.call()
    			) {
    		logger.info("Init repo, now pulling");
    		git.remoteAdd()
	    		.setName("origin")
	    		.setUri(new URIish(remoteUrl()))
	    		.call();
    		git.pull()
    			.setCredentialsProvider(auth())
    			.setRemote("origin")
    			.setProgressMonitor(new SyncProgMonitor())
    			.call();
    		logger.info("Successfully cloned " + branch + " into " + to.getAbsolutePath());
    		git.close();
    	}
    }
    
    void move(File from, File to) throws IOException {
    	logger.info(from + " -> " + to);
    	if(from.isDirectory()) {
    		to.mkdir();
    		for(File file : from.listFiles()) {
    			move(file, new File(to, file.getName()));
    		}
    		from.delete();
    	} else {
    		Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    	}
    }
    
    public void SyncDownload(File world) throws IOException, NoWorkTreeException, GitAPIException, SyncException, URISyntaxException {
    	String branchName = readFile(new File(world, "SYNC.txt"));
    	Git git = Git.open(world);
		/*git.fetch()
    		.setCredentialsProvider(auth())
    		.setProgressMonitor(new SyncProgMonitor())
    		.call();*/
		
		try {
    		git.pull()
    			.setCredentialsProvider(auth())
    			.setProgressMonitor(new SyncProgMonitor())
    			.call();
		} catch(CheckoutConflictException e) {
			git.close();
			logger.warn("Save " + world.getName() + " has conflict with upstream changes.");
			// Ideas:
			// 1) Commit existing changes to branch
			// 2) Create & checkout new branch, randomly generated
			// 3) Bring committ over to new branch
			// 4) Purge old folder, re-download it.
			Random rnd = new Random();
			String newName = "conflicted-" + rnd.nextInt(1000);
			String folderName = saveRepo.getName() + "-" + newName;
			File newWorld = new File(world.getParent() + "/" + folderName);
			logger.info("Attempting to rename " + world + " -> " + newWorld);
			move(world, newWorld);
			logger.info("Renamed");
			WriteSyncBranch(newWorld, newName);
			logger.info("Renamed folder over, moving to new branch...");
			git = Git.open(newWorld);
			Ref branch = git.branchCreate().setName(newName).call();
			git.checkout().setName(newName).call();
			git.close();
			SyncUpload(newWorld);
			logger.info("Pushed conflicts to their own branch.");
			logger.info("We can now attempt to make sure the old folder is deleted and pull it again");
			world.delete();
			logger.info("Folder deleted, cloning...");
			SyncClone(world, branchName);
			
		}
    }
    
    void CloneDefaultBranch() throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException {
		File saveFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves");
		File world = new File(saveFolder, saveRepo.getName() + "-" + saveRepo.getDefaultBranch());
		SyncClone(world, saveRepo.getDefaultBranch());
    }
    
    public void SyncDownload() throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, NoWorkTreeException, IOException, SyncException {
    	// Find save with 'SYNC.txt' in folder, 
    	// If none exists, freshly download master branch.
    	List<File> files = GetSyncFolders();
    	if(files.size() == 0) {
    		logger.warn("No sync folders exist, so cloning default branch...");
    		CloneDefaultBranch();
    		return;
    	}
    	boolean hasdefault = false;
    	for(File file : files) {
    		if(file.getName().endsWith(saveRepo.getDefaultBranch())) {
    			hasdefault = true;
    			break;
    		}
    	}
    	if(!hasdefault) {
    		CloneDefaultBranch();
    	}
    	for(File file : files) {
    		SyncDownload(file);
    	}
    }
    
    
    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID))
        {
        	try {
        		LoadGithub();
        		ConfigManager.sync(MODID, Type.INSTANCE);
        	} catch(IOException | NoWorkTreeException | GitAPIException | URISyntaxException | SyncException ex) {
        		logger.error(ex);
        		event.setResult(Result.DENY);
        	}
        }
    }
    
    
    @Config(modid = MODID, type = Type.INSTANCE, name = MODID)
    public static class SaveConfig
    {
    	@Name("Repository Owner")
    	@Comment("Github username of the repository's owner")
        public static String RepositoryOwner = "SneakyBoy10";
    	@Name("Repository Name")
    	@Comment("Name of the repository itself")
        public static String RepositoryName = "Ominfactory-Save";
    	
    	@Name("API Key")
    	@Comment("GitHub Personal Access Token with read/write access to the repository")
        public static String API_Key = "none";
    }


}


