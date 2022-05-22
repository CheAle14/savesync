package cheale14.savesync.client.gui;

import java.util.function.Predicate;

import cheale14.savesync.SaveSync;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

public class SyncReplaceButton extends Button {

	public SyncReplaceButton(Button replacing,
			ITextComponent p_i232255_5_, Predicate<Button> func) {
		super(replacing.x, replacing.y, replacing.getWidth(), replacing.getHeight(), p_i232255_5_, new Button.IPressable() {
			@Override
			public void onPress(Button p_onPress_1_) {
				boolean v = func.test(replacing);
				SaveSync.LOGGER.info("SyncReplace " + p_i232255_5_.getContents() + " == " + v);
				if(!v) {
					replacing.onPress();
				}
			}
		});
	}
	public SyncReplaceButton(Button replacing, ITextComponent message) {
		this(replacing, message, (x) -> false);
	}
	
	
	
}
