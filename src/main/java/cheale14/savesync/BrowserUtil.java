package cheale14.savesync;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class BrowserUtil {
	public static void Open(URI url) throws IOException {
		if(Desktop.isDesktopSupported()) {
			Desktop d = Desktop.getDesktop();
			if(d.isSupported(Action.BROWSE)) {
				d.browse(url);
				return;
			}
		} 
		
		Runtime rt = Runtime.getRuntime();
		rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
	}
	public static void Open(String url) throws IOException {
		Open(URI.create(url));
	}
}
