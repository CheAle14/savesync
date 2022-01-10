package com.cheale14.savesync.client.gui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Mouse;

import com.cheale14.savesync.SaveSync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;

public class SyncGuiLog extends SyncScrollingList {
	
	private List<String> messages = new LinkedList<String>();
	private GuiScreen parent;
	private long startTime;
	private Object _lock = new Object();
	private boolean skipEnd = true;
	
	public SyncGuiLog(GuiScreen src, int listWidth, int slotHeight) {
		super(src.mc, listWidth, src.height, 30, src.height - 15, 15, slotHeight, src.width, src.height);
		// TODO Auto-generated constructor stub
		parent = src;
		startTime = Minecraft.getSystemTime();
	}
	
	public void Add(String message) {
		synchronized(_lock) {
			long diff = Minecraft.getSystemTime() - startTime;
			int seconds = (int)(diff / 1000);
			String padded = StringUtils.leftPad("" + seconds, 3, '0');
			messages.add("[" + padded + "] " + message);
			SaveSync.logger.info(message);
			this.selectedIndex = messages.size() - 1;
			if(skipEnd) {
				int scroll = this.slotHeight * (this.messages.size() - 15); // display last x msgs
				this.scrollDistance = Math.max(scroll, 0);
			}
		}
	}
	
	public void Clear() {
		messages.clear();
	}

	@Override
	protected int getSize() {
		return messages.size();
	}

	@Override
	protected void elementClicked(int index, boolean doubleClick) {
		// Given they're just strings, we have nothing to do here.
	}

	@Override
	protected boolean isSelected(int index) {
		return false;
	}

	@Override
	protected void drawBackground() {
		this.parent.drawDefaultBackground();
	}
	
	@Override
    protected int getContentHeight()
    {
        return (this.getSize()) * 20 + 1;
    }

	@Override
	protected void drawSlot(int idx, int right, int top, int height, Tessellator tess) {
		synchronized(_lock) {
			String       msg      = messages.get(idx);
	        FontRenderer font     = this.parent.mc.fontRenderer;
	        font.drawString(font.trimStringToWidth(msg, this.parent.width), this.left + 3, top + 2, 0xFFFFFF);
		}
	}
	
	@Override
	public void handleMouseInput(int mouseX, int mouseY) throws IOException {
		super.handleMouseInput(mouseX, mouseY);
        int scroll = Mouse.getEventDWheel();
        if(scroll != 0) {
        	skipEnd = this.scrollDistance >= this.listHeight;
        }
	}

}
