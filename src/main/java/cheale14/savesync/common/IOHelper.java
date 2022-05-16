package cheale14.savesync.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IOHelper {
	public static List<String> ReadAllLines(File file) throws FileNotFoundException {
		Scanner reader = new Scanner(file);
		ArrayList<String> list = new ArrayList<String>();
		while(reader.hasNextLine() ) {
			list.add(reader.nextLine());	
		}
		return list;
	}
	public static String ReadAllText(File file) throws FileNotFoundException {
		return String.join("\n", IOHelper.ReadAllLines(file));
	}
	public static void WriteAllText(File file, String text) throws IOException {
		try(FileWriter writer = new FileWriter(file)) {
			writer.write(text);
			writer.close();
    	}
	}
	public static int Move(File from, File to) throws IOException {
		int files = 0;
    	if(from.isDirectory()) {
    		to.mkdir();
    		for(File file : from.listFiles()) {
    			files += Move(file, new File(to, file.getName()));
    		}
    		from.delete();
    	} else {
    		Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    		files = 1;
    	}
    	return files;
    }
}
