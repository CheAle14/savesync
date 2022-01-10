package com.cheale14.savesync.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.logging.log4j.Logger;

import com.cheale14.savesync.SaveSync;

public class HttpUtil {
	
	static Logger logger = SaveSync.logger;
	
	private static String process(HttpURLConnection con) throws IOException, HttpError {
		StringBuilder content;
		
		logger.debug("[HTTP-" + con.getRequestMethod() + "] >>" + con.getURL().toString());

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()))) {

            String line;
            content = new StringBuilder();

            while ((line = in.readLine()) != null) {

                content.append(line);
                content.append(System.lineSeparator());
            }
        } finally {
        	con.disconnect();
        }
        
        String s = content.toString();
        
		logger.info("[HTTP-" + con.getRequestMethod() + "] << " + con.getURL().toString() 
			+ ": " + con.getResponseCode() + ": " + s);
		
		if(con.getResponseCode() < 200 || con.getResponseCode() > 299) {
			throw new HttpError(con.getResponseCode(), s);
		}
		
		return s;
        
	}
	
	public static String GET(String url, Map<String, String> headers) throws IOException, HttpError {

		URL uri = new URL(url);
        HttpURLConnection con = (HttpURLConnection) uri.openConnection();
        con.setRequestMethod("GET");
        
        for(Map.Entry<String,String> entry : headers.entrySet()) {
        	con.addRequestProperty(entry.getKey(), entry.getValue());
        }
        
        return process(con);
	}
	
	public static String GET(String url) throws IOException, HttpError {
		return GET(url, new HashMap<String, String>());
	}
	
	public static String POST(String url, String contentType, byte[] body, Map<String, String> headers) throws IOException, HttpError {
		URL uri = new URL(url);
        HttpURLConnection con = (HttpURLConnection) uri.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        

        for(Map.Entry<String,String> entry : headers.entrySet()) {
        	con.addRequestProperty(entry.getKey(), entry.getValue());
        }
        
        con.setFixedLengthStreamingMode(body.length);
        con.setRequestProperty("Content-Type", contentType);
        con.connect();
        try(OutputStream os = con.getOutputStream()) {
            os.write(body);
        }
        
        return process(con);
	}
	
	public static String POST(String url, String json) throws IOException, HttpError {
		return POST(url, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8),
				new HashMap<>());
	}
	
	public static String POST(String url, Map<String, String> formEncoded, Map<String, String> headers) throws IOException, HttpError {
		StringJoiner sj = new StringJoiner("&");
		for(Map.Entry<String,String> entry : formEncoded.entrySet())
		    sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" 
		         + URLEncoder.encode(entry.getValue(), "UTF-8"));
		byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
		return POST(url, "application/x-www-form-urlencoded; charset=UTF-8", out, 
				headers);
	}
	
	public static String POST(String url, Map<String, String> form) throws IOException, HttpError {
		return POST(url, form, new HashMap<>());
	}
}
