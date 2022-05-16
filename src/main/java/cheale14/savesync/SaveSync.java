package cheale14.savesync;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import cheale14.savesync.client.ClientEnvironment;
import cheale14.savesync.client.GithubUser;
import cheale14.savesync.common.ServerEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SaveSync.MODID)
public class SaveSync
{
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

	public static final String MODID = "savesync";
	public static final String NAME = "SaveSync";

    public SaveSync() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC); 
        

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        PROXY = DistExecutor.safeRunForDist(() -> ClientEnvironment::new, () -> ServerEnvironment::new);
        MinecraftForge.EVENT_BUS.register(PROXY);
    }
    
    public static Environment PROXY;
    
	public static void FixNBT(File worldFolder) throws IOException {
    	File nbtFile = new File(worldFolder, "level.dat");
    	if(!nbtFile.exists()) {
    		LOGGER.warn("NBT file does not exist for " + worldFolder.getName() + ", this is weird?");
    		return;
    	}
    	
    	NamedTag outer = NBTUtil.read(nbtFile);
    	CompoundTag levelData = (CompoundTag) outer.getTag();
    	CompoundTag Data = (CompoundTag) levelData.get("Data");
    	Data.remove("Player");
    	levelData.put("Data", Data);
    	outer.setTag(levelData);
    	File file = new File(worldFolder, "level_new.dat");
    	LOGGER.info("Writing new NBT data to " + file.getPath());
    	NBTUtil.write(outer, file);
    	Files.copy(file.toPath(), nbtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
    	
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().options);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    
    
    public static GithubUser GetCurrentUser(String accessToken) throws IOException {
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/vnd.github.v3+json");
		headers.put("Authorization", "token " + accessToken);
		
		String json;
		try {
			json = HttpUtil.GET("https://api.github.com/user", headers);
		} catch (HttpError e) {
			return null;
		}
		
		return new Gson().fromJson(json, GithubUser.class);
    }
    
    public static void PurgeCachedUser() {
    	savedUser = null;
    }
    
    static GithubUser savedUser = null;
    public static GithubUser GetCurrentUser() throws IOException {
    	if(HasApiKey()) {
    		if(savedUser == null) {
    			savedUser = GetCurrentUser(SaveSync.CONFIG.ApiKey.get());
    		}
    		return savedUser;
    	} else {
    		return null;
    	}
    }
    
    public static boolean HasApiKey() {
    	String g = SaveSync.CONFIG.ApiKey.get();
    	return g != null && g != "none";
    }
    

    public static class Config {
    	private static final String defaultApiKey = "none";
    	public final ConfigValue<String> ApiKey;
    	
    	private static final Boolean defaultCloseUIOnSuccess = true;
    	public final ConfigValue<Boolean> CloseUIOnSuccess;
    	
    	public Config(ForgeConfigSpec.Builder builder) {
    		builder.push("SaveSync");
    		
    		this.ApiKey = builder.comment("Gitub Personal Access Token")
    				.define("apiKey", defaultApiKey);
    		
    		this.CloseUIOnSuccess = builder.comment("Close Progress UI On Success")
    				.define("closeOnSuccess", defaultCloseUIOnSuccess);
    		
    		builder.pop();
    	}
    }
    
    public static final Config CONFIG;
	public static final ForgeConfigSpec CONFIG_SPEC;

	static //constructor
	{
		Pair<Config, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Config::new);
		CONFIG = commonSpecPair.getLeft();
		CONFIG_SPEC = commonSpecPair.getRight();
	}
}
