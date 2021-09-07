package com.cheale14.savesync.client;

import java.net.URI;
import java.security.KeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.cheale14.savesync.common.IWebSocketHandler;
import com.cheale14.savesync.common.SaveSync;
import com.cheale14.savesync.common.WSPacket;
import com.google.gson.Gson;

public class WSClient extends WebSocketClient {

	public WSClient(URI uri, IWebSocketHandler handle) throws KeyManagementException, NoSuchAlgorithmException {
		super(uri);
		
		TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };


        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { tm }, new java.security.SecureRandom());
        
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        this.setSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        
        
		logger = SaveSync.logger;
		logger.info("[WS] URI: " + uri.toString());
		handler = handle;
		gson = new Gson();
	}
	
	@Override
	protected void onSetSSLParameters(SSLParameters params) {
		params.setEndpointIdentificationAlgorithm(null);
	}
	
	Logger logger;
	IWebSocketHandler handler;
	Gson gson;

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
		logger.warn("[WS] connection closed: " + arg0 + " " + arg1);
		handler.OnClose(arg0, arg1);
	}

	@Override
	public void onError(Exception arg0) {
		// TODO Auto-generated method stub

		arg0.printStackTrace();
		logger.error("[WS] Error: ", arg0);
		handler.OnError(arg0);
	}

	@Override
	public void onMessage(String arg0) {
		// TODO Auto-generated method stub
		
		logger.info("[WS] << " + arg0);
		
		WSPacket packet = gson.fromJson(arg0, WSPacket.class);
		logger.info("Parsed packet of ID " + packet.Id());
		handler.OnPacket(packet);

	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		// TODO Auto-generated method stub

		logger.info("[WS] Connected");
		handler.OnOpen();
	}

}
