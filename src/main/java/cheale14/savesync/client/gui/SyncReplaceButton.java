package cheale14.savesync.client.gui;

import java.util.function.Predicate;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

public class SyncReplaceButton extends Button {

	public SyncReplaceButton(Button replacing,
			ITextComponent p_i232255_5_, Predicate<Button> func) {
		super(replacing.x, replacing.y, replacing.getWidth(), replacing.getHeight(), p_i232255_5_, new Button.IPressable() {
			@Override
			public void onPress(Button p_onPress_1_) {
				if(!func.test(p_onPress_1_)) {
					p_onPress_1_.onPress();
				}
			}
		});
	}
	
	
	
}
