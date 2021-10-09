package com.cheale14.savesync.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import com.cheale14.savesync.common.SaveSync.SaveConfig;

import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

public class SyncFileInfo {
	
	public String Branch;
	public String RepoOwner;
	public String RepoName;
	
	public SyncFileInfo(String branch, String repositoryFull) {
		Branch = branch;
		String[] s = repositoryFull.split("/");
		RepoOwner = s[0];
		RepoName = s[1];
	}
	
	public static SyncFileInfo FromFile(File file) throws FileNotFoundException {
		Scanner reader = new Scanner(file);
		String branch = reader.nextLine();
		String repos = null;
		if(reader.hasNextLine() ) {
			repos = reader.nextLine();	
			if(StringUtils.isNullOrEmpty(repos)) {
				SaveSync.logger.warn("Using default repository save information from config");
				repos = SaveConfig.RepositoryOwner + "/" + SaveConfig.RepositoryName;
			}
		}
    	reader.close();
    	
    	return new SyncFileInfo(branch, repos);
	}
	
	public void ToFile(File file) throws IOException {
		try(FileWriter writer = new FileWriter(file)) {
    		writer.write(Branch + "\r\n");
    		writer.write(RepoOwner + "/" + RepoName + "\r\n");
			writer.close();
    	}
	}
	
	@Override
	public String toString() {
		return RepoOwner + "/" + RepoName + "#" + Branch;
	}
}
