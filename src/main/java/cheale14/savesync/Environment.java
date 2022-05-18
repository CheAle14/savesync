package cheale14.savesync;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cheale14.savesync.common.IWebSocketHandler;
import cheale14.savesync.common.WSClient;
import cheale14.savesync.common.WSPacket;

public interface Environment {
	public Logger logger = LogManager.getLogger("savesync-proxy");

}
