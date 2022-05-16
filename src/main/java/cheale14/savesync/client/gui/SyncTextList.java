package cheale14.savesync.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;

import java.io.IOException;
import java.util.List;

public class SyncTextList extends ExtendedList<SyncTextList.TextEntry> 
{
	public SyncTextList(SyncProgressGui gui, int listWidth, int top, int bottom)  {
        super(gui.mc, listWidth, gui.height, top, bottom, gui.getFont().lineHeight * 2 + 8);
		parent = gui;
	}
	private SyncProgressGui parent;
	
	public void add(String text) {
		this.addEntry(new TextEntry(text, parent));
	}
	

	public class TextEntry extends ExtendedList.AbstractListEntry<TextEntry> {
		private String text;
		private SyncProgressGui parent;
		public TextEntry(String _text, SyncProgressGui _parent) {
			text = _text;
			parent = _parent;
		}
		@Override
		public void render(MatrixStack mStack, int entryIdx, int top, int left, int entryWidth, 
				int entryHeight, int mouseX, int mouseY, boolean p_194999_5_, float partialTicks) {
			ITextComponent t = new StringTextComponent(net.minecraft.util.StringUtils.stripColor(text));
			FontRenderer font = this.parent.getFont();
            font.draw(mStack, LanguageMap.getInstance().getVisualOrder(ITextProperties.composite(font.substrByWidth(t,    this.parent.width))), left + 3, top + 2, 0xFFFFFF);
		}
		
	}


	public void Clear() {
		this.clearEntries();
	}
}