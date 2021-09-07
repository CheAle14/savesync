package com.cheale14.savesync.common;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
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
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.annotation.Nullable;
import javax.net.ssl.SSLHandshakeException;

import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;


@Mod(modid = SaveSync.MODID, name = SaveSync.NAME, version = SaveSync.VERSION, acceptableRemoteVersions = "*")
public class SaveSync
{
    public static final String MODID = "savesync";
    public static final String NAME = "Save Sync";
    public static final String VERSION = "0.9";
    
    public static final String SYNCNAME = "SYNC.txt";
    public static final String MODSNAME = "MODS.txt";
    
    public static final String MLAPI =  "http://localhost:8887"; // "https://ml-api.uk.ms"; //
    
    public static String WS_URI(boolean client) {
    	String url;
    	if(MLAPI.startsWith("https")) {
    		url = "wss://ml-api.uk.ms";
    	} else {
        	url = "ws://localhost:4650";
    	}
    	url += "/masterlist?";
    	if(client) {
    		url += "client=true&";
    	}
    	url += "game=minecraft&";
    	
    	url += "mode=" + SaveConfig.MLGameMode;
    	
    	return url;
    }
    

    @SidedProxy(modId=MODID, clientSide="com.cheale14.savesync.client.ClientProxy", serverSide="com.cheale14.savesync.common.CommonProxy")
    public static CommonProxy proxy;
    
    public static Logger logger;
    public static boolean loadedSync = false;
    public static boolean hamachiRunning = false;
    public static String hamachiIP = null;
    public static String lanPort = null;
    
    public static boolean hasRedirectedToConfig = false;
    
    public static boolean isUploading = false;
    
    public static boolean HasAPIKey() {
    	return !(SaveSync.SaveConfig.API_Key == null || SaveSync.SaveConfig.API_Key.equals("none"));
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    	if(proxy == null) {
    		logger.info("Proxy null.");
    	} else {
    		logger.info("Proxy: " + proxy.getClass().getName());
    		MinecraftForge.EVENT_BUS.register(proxy);
    	}
    }

    @EventHandler
    public void init(FMLInitializationEvent event) throws Exception
    {
    	logger.info("Repo: https://github.com/" + SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName);
    	if(event.getSide() == Side.SERVER) {
    		logger.info("On dedicated server");
    		if(!HasAPIKey()) {
    			logger.error("No API key configured.");
    			File saves = FMLCommonHandler.instance().getSavesDirectory();
    			File configFolder = new File(saves, "config");
    			File saveConfig = new File(configFolder, "savesync.cfg");
    			throw new Exception("You must set an API key at " + saveConfig.getAbsolutePath());
    		}
    		SyncDownload(new SyncProgMonitor());
    	}
    	//LoadGithub();
    	CheckMods();
    	loadedSync = true;
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
    	proxy.serverStart(event);
    }
    
    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
    	proxy.serverStarted(event);
    }

    @EventHandler
	public void serverStopped(FMLServerStoppedEvent event) {
    	proxy.serverStopped(event);
	}

    @EventHandler
	public void serverStopping(FMLServerStoppingEvent event) {
		proxy.serverStopping(event);
	}
    
    File lastLoaded = null;
    
    
    // http://stackoverflow.com/a/19005828/3764804
    private static boolean isProcessRunning(String processName) throws IOException, InterruptedException
    {
        ProcessBuilder processBuilder = new ProcessBuilder("tasklist.exe");
        Process process = processBuilder.start();
        String tasksList = toString(process.getInputStream());

        return tasksList.contains(processName);
    }

    // http://stackoverflow.com/a/5445161/3764804
    private static String toString(InputStream inputStream)
    {
    	try(Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A")) {
            String string = scanner.hasNext() ? scanner.next() : "";
            return string;
    	}
    }
    
    public static String PutServer() throws IOException, InterruptedException {
    	if(!SaveConfig.SyncServerConnect) {
    		return "Sync disabled";
    	}
		URL url = new URL(MLAPI + "/mc/sethamIp?ip=" + hamachiIP + "&port=" + lanPort);
		logger.info("GETing to " + url.toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		
		int code = con.getResponseCode();
		if(code < 200 || code > 299) {
			Reader streamReader = new InputStreamReader(con.getErrorStream());
			StringBuffer content = new StringBuffer();
			try(BufferedReader bf = new BufferedReader(streamReader)) {
				String line;
				while((line = bf.readLine()) != null)
					content.append(line);
			}
			logger.error("Failed to PUT: " + code + ": " + content);
			return "Failed with " + code;
		}
		logger.info("Successfully PUTted");
		return null;
    }
    
    private static String http_get(URL url)
    {
        try
        {
            HttpURLConnection httpurlconnection = (HttpURLConnection)url.openConnection();
            httpurlconnection.setRequestMethod("GET");
            httpurlconnection.setUseCaches(false);
            httpurlconnection.setDoOutput(true);
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(httpurlconnection.getInputStream()));
            StringBuffer stringbuffer = new StringBuffer();
            String s;

            while ((s = bufferedreader.readLine()) != null)
            {
                stringbuffer.append(s);
                stringbuffer.append('\r');
            }

            bufferedreader.close();
            return stringbuffer.toString();
        }
        catch (Exception exception)
        {
            logger.error(exception);
            return "";
        }
    }
    
    public static String getHamachiIP() throws IOException, InterruptedException {
    	if(!isProcessRunning("hamachi-2-ui.exe") ) {
    		logger.info("Hamachi is not running, no IP");
    		return null;
    	}
    	Enumeration<NetworkInterface> ints = NetworkInterface.getNetworkInterfaces();
    	while(ints.hasMoreElements()) {
    		NetworkInterface netInt = ints.nextElement();
    		if(!netInt.getDisplayName().contains("Hamachi"))
    			continue;
    		Enumeration<InetAddress> addrs = netInt.getInetAddresses();
    		while(addrs.hasMoreElements()) {
    			InetAddress addr = addrs.nextElement();
    			logger.info(netInt.getDisplayName() + ": " + addr.toString());
    			if(addr.getHostAddress().startsWith("25.")) {
					return addr.getHostAddress();
    			}
    		}
    	}
    	return null;
    }
    
    public static String getExternalIp() throws MalformedURLException {
    	String ip = http_get(new URL("https://api.ipify.org/"));
    	return ip.trim();
    }
    
    
    /*public void LoadGithub() throws NoWorkTreeException, InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException, SyncException {
    	if(SaveConfig.API_Key == null || "none".equalsIgnoreCase(SaveConfig.API_Key)) {
			logger.warn("No API key confiured, not attempting sync things..");
		} else {
	    	SyncDownload(new SyncProgMonitor());
		}
    }*/
    
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
    
    public static void WriteMods(File worldFolder) throws IOException {
    	File modsFile = new File(worldFolder, SaveSync.MODSNAME);
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
    
    private static SavedMod getMod(List<SavedMod> mods, String modId ) {
    	for(SavedMod mod : mods) {
    		if(modId.equalsIgnoreCase(mod.Id))
    			return mod;
    	}
    	return null;
    }
    
    // left > right
    static boolean versionIsAhead(String left, String right) {
    	logger.debug("Comparing " + left + " vs " + right);
    	Version lV = Version.parse(left);
    	Version rV = Version.parse(right);
    	int compare = lV.compareTo(rV);
    	logger.info(left + " vs " + right + " = " + compare);
    	return compare > 0;
    }
    
    public static void CheckMods(File worldFolder, List<SavedMod> mods) throws Exception {
    	File modsFile = new File(worldFolder, SaveSync.MODSNAME);
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
    
    public static void CheckMods() throws Exception {
    	if(!SaveConfig.CheckMods) {
    		return;
    	}
    	List<SavedMod> mods = new LinkedList<SavedMod>();
    	for(ModContainer mod : Loader.instance().getModList()) {
    		mods.add(new SavedMod(mod));
    	}
    	for(File world : proxy.GetSyncFolders()) {
    		CheckMods(world, mods);
    	}
    }
    
    public static void SyncUploadAll(ProgressMonitor monitor) throws GitAPIException, IOException, URISyntaxException {
    	for(File folder : proxy.GetSyncFolders()) {
    		SyncUpload(folder, monitor);
    	}
    }
    
    public static void SyncUpload(File world, ProgressMonitor monitor) throws GitAPIException, IOException, URISyntaxException {
    	String branchName = readFile(new File(world, SYNCNAME));
    	Git git = null;
    	if(world.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return ".git".equals(name);
			}
    	}).length == 0) {
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
    	monitor.beginTask("Writing modlist to " + SaveSync.MODSNAME, 1);
    	WriteMods(world);
    	monitor.endTask();
    	
    	monitor.beginTask("Fixing NBT to neutral perpsective", 1);
    	FixNBT(world);
    	monitor.endTask();
    	
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
		isUploading = false;
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
    
    public static void SyncClone(File to, String branch, ProgressMonitor monitor) throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException {
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
    			.setProgressMonitor(monitor)
    			.call();
    		logger.info("Successfully cloned " + branch + " into " + to.getAbsolutePath());
    		git.close();
    		FixNBT(to);
    	}
    }
    
    static void move(File from, File to) throws IOException {
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
    
    public static boolean IsSyncFolder(File worldFolder) {
    	File syncFile = new File(worldFolder, SYNCNAME);
    	return worldFolder.isDirectory() && syncFile.exists() && syncFile.isFile();
    }
    public static void WriteSyncBranch(File worldFolder, String branch) throws IOException {
    	File syncFile = new File(worldFolder, SYNCNAME);
    	try(FileWriter writer = new FileWriter(syncFile)) {
			writer.write(branch);
			writer.close();
    	}
    }
    
    public static void SyncDownload(File world, ProgressMonitor monitor) throws IOException, NoWorkTreeException, GitAPIException, SyncException, URISyntaxException {
    	String branchName = readFile(new File(world, SYNCNAME));
    	Git git = Git.open(world);
		/*git.fetch()
    		.setCredentialsProvider(auth())
    		.setProgressMonitor(new SyncProgMonitor())
    		.call();*/
		
		try {
    		git.pull()
    			.setCredentialsProvider(auth())
    			.setProgressMonitor(monitor)
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
			SyncClone(world, branchName, monitor);
			
		}
    }
    
    static void CloneDefaultBranch(ProgressMonitor monitor) throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, IOException {
		File world = proxy.GetDefaultBranchFolder();
		SyncClone(world, "main", monitor);
    }
    
    public static void SyncDownload(ProgressMonitor monitor) throws InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, NoWorkTreeException, IOException, SyncException {
    	// Find save with 'SYNC.txt' in folder, 
    	// If none exists, freshly download master branch.
    	List<File> files = proxy.GetSyncFolders();
    	if(files.size() == 0) {
    		logger.warn("No sync folders exist, so cloning default branch...");
    		CloneDefaultBranch(monitor);
    		return;
    	}
    	boolean hasdefault = false;
    	for(File file : files) {
    		if(file.getName().endsWith("main") || file.getName().equals("world")) {
    			hasdefault = true;
    			break;
    		}
    	}
    	if(!hasdefault) {
    		CloneDefaultBranch(monitor);
    	}
    	for(File file : files) {
    		SyncDownload(file, monitor);
    	}
    }
    

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID))
        {
        	ConfigManager.sync(MODID, Type.INSTANCE);
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
    	
    	@Name("Server Sync")
    	@Comment("Sends server connection info to MLAPI and sets server list accordingly")
    	public static boolean SyncServerConnect = true;
    	
    	@Name("Close UI")
    	@Comment("Automatically close sync UI if no errors occur")
    	public static boolean CloseUIOnSuccess = true;
    	
    	@Name("Check Mod Differences")
    	@Comment("Check whether the synced save has different mods to what we have")
    	public static boolean CheckMods = false;
    	
    	@Name("ML Game Mode")
    	@Comment("The game mode used to categorise the server on the masterlist")
    	public static String MLGameMode = "modded";
    	
    	@Name("MLAPI Data")
    	@Comment("For dedicated servers only! Controls information sent to MLAPI")
    	public static DedicatedServerData MLAPIData = new DedicatedServerData();
    	
    	static class DedicatedServerData {
    		@Name("Name")
    		public static String Name = "name";
    		
    		@Name("IP Address")
    		public static String IPAddress = "195.168.1.1";
    		
    		@Name("Port")
    		@RangeInt(min = 1, max = 65565)
    		public static Integer Port = 25565;
    		
    		
    	}
    }
    
    public static class NameFilter implements FilenameFilter {
    	
    	private final String _filter;
    	public NameFilter(String name) {
    		_filter = name;
    	}

    	@Override
    	public boolean accept(File dir, String name) {
    		return _filter.equalsIgnoreCase(name);
    	}
    	
    }
}


