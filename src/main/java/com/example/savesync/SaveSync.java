package com.example.savesync;

import net.minecraft.init.Blocks;
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
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.time.Clock;

import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHBranch;
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

    private static Logger logger;
    private static GitHub github;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // some example code
        
    	logger.info("Repo: https://github.com/" + SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName);
    	
    	try {
    		
    		LoadGithub();
    	} catch(IOException ex) {
    		logger.error(ex);
    		
    	}
    }
    
    public void LoadGithub() throws IOException {
    	github = new GitHubBuilder().withOAuthToken(SaveConfig.API_Key).build();
    	try {
	    	GHMyself user = github.getMyself();
	    	logger.info("User: " + user.getName());
    	} catch(HttpException ex) {
    		if(ex.getMessage().contains("Bad credentials")) {
    			Runtime rt = Runtime.getRuntime();
    			String url = "https://github.com/settings/tokens/new";
    			rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
    		}
    		github = null;
    		throw ex;
    	}
    	Git git = Git.
    	
    }
    
    
    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID))
        {
        	try {
        		LoadGithub();
        		ConfigManager.sync(MODID, Type.INSTANCE);
        	} catch(IOException ex) {
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


