package cheale14.savesync.common.commands;

import com.mojang.brigadier.CommandDispatcher;

import cheale14.savesync.SaveSync;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands.EnvironmentType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CommandSetup {

	@SubscribeEvent
	public static void onRegisterCommands(final RegisterCommandsEvent event) {
    	SaveSync.LOGGER.info("Registering commands!");
    	CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
    	EnvironmentType environment = event.getEnvironment();
		SyncCommand.register(dispatcher);
    }

}
