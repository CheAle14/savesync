package com.cheale14.savesync.client.discord;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.client.GuiScrollingList;

public class GuiDiscordList extends GuiScrollingList {
	public GuiDiscordList(GuiDiscord parent, int top, int bottom,
			int screenWidth, int screenHeight) {
		super(parent.mc, parent.width - 10, parent.height - 50, top, bottom, 5, 36, parent.width, parent.height);
		// TODO Auto-generated constructor stub
		owner = parent;
	}
	private List<DiscordGuild> servers = new ArrayList<DiscordGuild>();
	private GuiDiscord owner;
	
	public void updateServers(Iterable<DiscordGuild> guilds) {
		servers.clear();
		for(DiscordGuild g : guilds) {
			servers.add(g);
		}
	}

	@Override
	protected int getSize() {
		// TODO Auto-generated method stub
		return servers.size();
	}
	
	@Override
	protected void elementClicked(int index, boolean doubleClick) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean isSelected(int index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void drawBackground() {
		this.owner.drawDefaultBackground();
	}

	@Override
	protected void drawSlot(int slotIdx, int entryRight, int slotTop, int height, Tessellator tess) {
		DiscordGuild server = servers.get(slotIdx);
		FontRenderer font = this.owner.mc.fontRenderer;
		
		Color clr;
		if(isSelected(slotIdx)) {
			clr = Color.green;
		} else {
			clr = Color.red;
		}
		font.drawString(server.name, this.left + 3, slotTop + 1, clr.getRGB());
		if(server.icon_url != null) {
			font.drawString(server.getIconUrl(32), this.left + 3, slotTop + 1 + font.FONT_HEIGHT, clr.getRGB());
		}
	}

}
