package cheale14.savesync.common.commands;

import java.io.File;
import java.io.IOException;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import cheale14.savesync.SaveSync;
import cheale14.savesync.common.ServerEnvironment;
import cheale14.savesync.common.SyncSave;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.util.text.StringTextComponent;

public class SyncCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
		Commands.literal("sync")
		.requires((cs) -> {return cs.hasPermission(1);})
		
		.then(Commands.literal("register")
			.then(Commands.argument("branch", StringArgumentType.word())
				.then(Commands.argument("repository", StringArgumentType.word())
						.executes((ctx) -> {
							return syncRegister(ctx.getSource(), StringArgumentType.getString(ctx, "branch"), StringArgumentType.getString(ctx, "repository"));
						})
					)
				.executes((ctx) -> {
					return syncRegister(ctx.getSource(), StringArgumentType.getString(ctx, "branch"), SaveSync.CONFIG.DefaultRepository.get());
				})
			)
			.executes((ctx) -> {return syncRegister(ctx.getSource(), "main", SaveSync.CONFIG.DefaultRepository.get());})
			)
		.then(Commands.literal("publish").executes((ctx) -> {
			return syncPublish(ctx.getSource());
			}))
		);

	}
	
	private static int syncRegister(CommandSource src, String branch, String repository) throws CommandSyntaxException {
		File file;
		try {
			file = SaveSync.getWorldFolder(src.getServer().overworld());
			SyncSave save = new SyncSave(repository, branch, file);
			save.WriteTo(file);
			src.sendSuccess(new StringTextComponent("Now syncing " + file.getAbsolutePath()), true);
		} catch (NoSuchFieldException | IllegalAccessException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			src.sendFailure(new StringTextComponent("Failed to write sync file."));
		}
		return 0;
	}
	
	private static int syncPublish(CommandSource src) {
		ServerEnvironment en = (ServerEnvironment)SaveSync.PROXY;
		boolean v = en.PublishFromJson(src.getServer());
		if(v)
			src.sendSuccess(new StringTextComponent("Published to MLAPI"), true);
		else
			src.sendFailure(new StringTextComponent("Failed to publish to MLAPI, see console for errors."));
		return v ? 0 : 1;
	}
}
