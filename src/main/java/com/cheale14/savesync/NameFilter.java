package com.cheale14.savesync;

import java.io.File;
import java.io.FilenameFilter;

public class NameFilter implements FilenameFilter {
	
	private final String _filter;
	public NameFilter(String name) {
		_filter = name;
	}

	@Override
	public boolean accept(File dir, String name) {
		return _filter.equalsIgnoreCase(name);
	}
	
}