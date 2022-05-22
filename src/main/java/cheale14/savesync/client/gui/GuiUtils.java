package cheale14.savesync.client.gui;

import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class GuiUtils {
	public static String getWidgetText(Widget widget) {
		ITextComponent component = widget.getMessage();
		if(component instanceof TranslationTextComponent) {
			TranslationTextComponent translate = (TranslationTextComponent)component;
			return translate.getKey();
		}
		return "";
	}
}
