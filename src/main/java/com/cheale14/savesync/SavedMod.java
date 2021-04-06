package com.cheale14.savesync;

import net.minecraftforge.fml.common.ModContainer;

public class SavedMod {
	public String Id;
	public String Name;
	public String Version;
	
	public SavedMod(ModContainer mod) {
		Id = mod.getModId();
		Name = mod.getName();
		Version = mod.getVersion();
	}
	
	public SavedMod(String line) {
		String[] arr = line.split(":");
		Id = arr[0];
		Name = arr[1];
		Version = arr[2];
	}
	
	@Override
	public String toString() {
		return Id + ":" + Name + ":" + Version;
	}
	
}
