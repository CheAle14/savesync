package com.cheale14.savesync;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.CompoundTag;

import java.awt.SplashScreen;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

@Mod(modid = SaveSync.MODID, name = SaveSync.NAME, version = SaveSync.VERSION)
public class SaveSync
{
    public static final String MODID = "savesync";
    public static final String NAME = "Save Sync";
    public static final String VERSION = "0.4";
    
    public static final String SYNCNAME = "SYNC.txt";
    public static final String MODSNAME = "MODS.txt";

    public static Logger logger;
    public static boolean loadedSync = false;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) throws Exception
    {
    	logger.info("Side: " + event.getSide());
    	logger.info("Repo: https://github.com/" + SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName);
    	LoadGithub();
    	CheckMods();
    	loadedSync = true;
    }
    
    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
    	event.registerServerCommand(new SyncCommand());
    	
    }
    
    void warnStartups(ICommandSender sender, MinecraftServer server) {
    	if(SaveConfig.API_Key == null || "none".equalsIgnoreCase(SaveConfig.API_Key)) {
    		sender.sendMessage(new TextComponentString("The server has not set their GitHub personal access token for save syncing.")
    				.setStyle(new Style().setColor(TextFormatting.RED)));
    	}
    	if(!loadedSync) {
    		sender.sendMessage(new TextComponentString("Error: savesync did not load properly - please check logs.")
    				.setStyle(new Style().setColor(TextFormatting.RED)));
    	} else {
	    	File root = server.getWorld(0).getSaveHandler().getWorldDirectory();
	    	if(root != null) {
				sender.sendMessage(new TextComponentString(root.getAbsolutePath()));
				File syncFile = new File(root, SYNCNAME);
				if(syncFile.exists()) {
		    		sender.sendMessage(new TextComponentString("World should be saved upon exit (or /sync now) to " + SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName)
	    				.setStyle(new Style().setColor(TextFormatting.GREEN)));
				} else {
		    		sender.sendMessage(new TextComponentString("Notice: This save will not be synced to github. To setup, use /sync now [branch]")
		    				.setStyle(new Style().setColor(TextFormatting.RED)));
				}
				
	    	} else {
	    		sender.sendMessage(new TextComponentString("[savesync] Could not find data directory?")
	    				.setStyle(new Style().setColor(TextFormatting.RED)));
	    	}
    	}
    }
    
    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
    	MinecraftServer sender = FMLCommonHandler.instance().getMinecraftServerInstance();
    	warnStartups(sender, sender);
    }
    
    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
    	warnStartups(event.player, event.player.getServer());
    }
    
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
    	logger.info("Server is stopping");
    }
    
    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
    	logger.info("Server has stopped, syncing");
    	File root = DimensionManager.getCurrentSaveRootDirectory();
    	if(root == null) {
    		logger.info("Attempting to save only " + root.getAbsolutePath());
    		try {
				SyncUpload(root, new SyncProgMonitor());
			} catch (GitAPIException | IOException | URISyntaxException e) {
				// TODO Auto-generated catch block
				logger.error(e);
			}
    	} else {
        	try {
    			SyncUploadAll(new SyncProgMonitor());
    		} catch (GitAPIException | IOException | URISyntaxException e) {
    			logger.error(e);
    		}
    	}
    }
    
    public void AddServer() {
    	MinecraftServer s;
    }
    
    public void CheckMods() throws Exception {
    	List<SavedMod> mods = new LinkedList<SavedMod>();
    	for(ModContainer mod : Loader.instance().getModList()) {
    		mods.add(new SavedMod(mod));
    	}
    	for(File world : GetSyncFolders()) {
    		CheckMods(world, mods);
    	}
    }
    
    private SavedMod getMod(List<SavedMod> mods, String modId ) {
    	for(SavedMod mod : mods) {
    		if(modId.equalsIgnoreCase(mod.Id))
    			return mod;
    	}
    	return null;
    }
    
    // left > right
    boolean versionIsAhead(String left, String right) {
    	Version lV = Version.parse(left);
    	Version rV = Version.parse(right);
    	int compare = lV.compareTo(rV);
    	logger.info(left + " vs " + right + " = " + compare);
    	return compare > 0;
    }
    
    public void CheckMods(File worldFolder, List<SavedMod> mods) throws Exception {
    	File modsFile = new File(worldFolder, MODSNAME);
    	if(!modsFile.exists())
    		return;
    	List<SavedMod> storedMods = new LinkedList<SavedMod>();
    	try (Scanner scanner = new Scanner(modsFile)) {
    		while(scanner.hasNextLine()) {
        		storedMods.add(new SavedMod(scanner.nextLine()));
    		}
    	}
    	for(SavedMod hasMod : mods) {
    		SavedMod neededMod = getMod(storedMods, hasMod.Id);
    		if(neededMod == null) {
    			throw new Exception("We are missing mod " + hasMod.toString());
    		}
    		if(!neededMod.Version.equalsIgnoreCase(hasMod.Version)) {
    			if(!versionIsAhead(hasMod.Version, neededMod.Version)) {
    				throw new Exception(hasMod.Name + " requires version " + neededMod.Version + ", but we have " + hasMod.Version);
    			}
    		}
    	}
    }
    
    public static void WriteMods(File worldFolder) throws IOException {
    	File modsFile = new File(worldFolder, MODSNAME);
    	if(!modsFile.exists())
    		modsFile.createNewFile();
    	try(FileWriter writer = new FileWriter(modsFile)) {
	    	for(ModContainer mod : Loader.instance().getModList()) {
	    		SavedMod sv = new SavedMod(mod);
	    		writer.write(sv.toString() + "\r\n");
	    	}
			writer.close();
    	}
    }
    
    
    
    public void LoadGithub() throws NoWorkTreeException, InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException, SyncException {
    	if(SaveConfig.API_Key == null || "none".equalsIgnoreCase(SaveConfig.API_Key)) {
			logger.warn("No API key confiured, not attempting sync things..");
		} else {
	    	SyncDownload();
		}
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
    
    public void SyncUploadAll(ProgressMonitor monitor) throws GitAPIException, IOException, URISyntaxException {
    	for(File folder : GetSyncFolders()) {
    		SyncUpload(folder, monitor);
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
    
    
    public static void SyncUpload(File world, ProgressMonitor monitor) throws GitAPIException, IOException, URISyntaxException {
    	String branchName = readFile(new File(world, "SYNC.txt"));
    	Git git = null;
    	if(world.listFiles(new NameFilter(".git")).length == 0) {
    		monitor.beginTask("Initializing git folder", 2);
    		InitCommand init = Git.init();
    		init.setBare(false);
    		init.setDirectory(world);
    		init.setInitialBranch(branchName);
    		monitor.update(1);
    		
    		String url = remoteUrl();
    		SaveSync.logger.info("Remote URL: " + url);
    		git = init.call();
    		git.remoteAdd()
	    		.setName("origin")
	    		.setUri(new URIish(url))
	    		.call();
    		monitor.update(1);
    		monitor.endTask();
    		
    	} else {
    		git = Git.open(world);
    	}
    	WriteMods(world);
    	monitor.beginTask("Fetching from remote", 1);
    	git.fetch().setCredentialsProvider(auth())
    		.call();
    	monitor.endTask();
    	monitor.beginTask("Calculating changes", 1);
    	List<DiffEntry> diff = git.diff().call();
    	SaveSync.logger.info("Number of differences from remote: " + diff.size());
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
    
    public static void FixNBT(File worldFolder) throws IOException {
    	File nbtFile = new File(worldFolder, "level.dat");
    	if(!nbtFile.exists()) {
    		logger.warn("NBT file does not exist for " + worldFolder.getName() + ", this is weird?");
    		return;
    	}
    	NamedTag outer = NBTUtil.read(nbtFile);
    	CompoundTag levelData = (CompoundTag) outer.getTag();
    	CompoundTag Data = (CompoundTag) levelData.get("Data");
    	Data.remove("Player");
    	levelData.put("Data", Data);
    	outer.setTag(levelData);
    	File file = new File(worldFolder, "level_new.dat");
    	logger.info("Writing new NBT data to " + file.getPath());
    	NBTUtil.write(outer, file);
    	Files.copy(file.toPath(), nbtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    public void SyncClone(File to, String branch) throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException {
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
    		FixNBT(to);
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
    		FixNBT(world);
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
			String folderName = SaveConfig.RepositoryName + "-" + newName;
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
			SyncUpload(newWorld, new SyncProgMonitor());
			logger.info("Pushed conflicts to their own branch.");
			logger.info("We can now attempt to make sure the old folder is deleted and pull it again");
			world.delete();
			logger.info("Folder deleted, cloning...");
			SyncClone(world, branchName);
			
		}
    }
    
    void CloneDefaultBranch() throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException {
		File saveFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves");
		File world = new File(saveFolder, SaveConfig.RepositoryName + "-main");
		SyncClone(world, "main");
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
    		if(file.getName().endsWith("main")) {
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


