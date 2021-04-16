package com.cheale14.savesync;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class Version implements Comparable<Version> {
	
	public Integer[] IntegerParts;
	public String[] SuffixParts;

    static final Pattern NON_INT_PATTERN = Pattern.compile("[^0-9]+");
	
	public int tryParseInt(String value, int defaultVal) {
	    try {
	        return Integer.parseInt(value);
	    } catch (NumberFormatException e) {
	        return defaultVal;
	    }
	}
	public boolean isInt(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	public Version(String str) {
		String[] intParts = str.split("[\\. -]");
		List<Integer> intLs = new LinkedList<Integer>();
		List<String> strLs = new LinkedList<String>();
		for(String part : intParts) {
			if(!isInt(part)) {
				// has non-int values, so assume we're doing suffix things
				// shouldn't matter if we're *not*, as long as we are incorrect
				// for both versions
				strLs.add(part);
			} else {
				intLs.add(Integer.parseInt(part));
			}
		}
		IntegerParts = intLs.toArray(new Integer[0]);
		SuffixParts = strLs.toArray(new String[0]);
	}
	
	public static Version parse(String s) {
		return new Version(s);
	}

	
	
	@Override
	public int compareTo(Version that) {
		int max = Math.max(IntegerParts.length, that.IntegerParts.length);
		for(Integer i = 0; i < max; i++ ) {
			int thisPart = i < this.IntegerParts.length ?
                this.IntegerParts[i] : 0;
            int thatPart = i < that.IntegerParts.length ?
                that.IntegerParts[i]  : 0;
    		SaveSync.logger.info(this.toString() + " vs " + that.toString() + ": " + thisPart + " vs " + thatPart);
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
		}
		SaveSync.logger.info(this.toString() + " vs " + that.toString() + ": ints are identical, comparing str");
		max = Math.max(SuffixParts.length, that.SuffixParts.length);
		for(Integer i = 0; i < max; i++ ) {
			String thisPart = i < this.SuffixParts.length ?
                this.SuffixParts[i] : "";
            String thatPart = i < that.SuffixParts.length ?
                that.SuffixParts[i]  : "";
            int compare = compareStr(thisPart, thatPart);
    		SaveSync.logger.info(this.toString() + " vs " + that.toString() + ": " + thisPart + " vs " + thatPart + " = " + compare);
            if(compare != 0)
            	return compare;
		}
		return 0;
	}
	
	int compareStr(String left, String right) {
		if("".equals(left) || left == null)
			return -1;
		if("".equals(right) || right == null)
			return 1;
		int compare = left.compareTo(right);
		if(compare > 0)
			return 1;
		if(compare < 0)
			return -1;
		return 0;
	}
	
	@Override
	public String toString() {
		String version = "";
		if(IntegerParts.length > 0)
			version = StringUtils.join(IntegerParts, ".");
		if(SuffixParts.length > 0)
			version = version + "-" + String.join("-", SuffixParts);
		if(version.length() == 0)
			version = "0.0.0";
		return version;
	}
}