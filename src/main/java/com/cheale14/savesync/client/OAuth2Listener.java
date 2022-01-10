package com.cheale14.savesync.client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

import com.cheale14.savesync.SaveSync;
import com.cheale14.savesync.client.gui.SyncLoginGui;


public class OAuth2Listener implements Runnable {


	ServerSocket serverSocket;
	public int Port;
	SyncLoginGui parent;
	
	public OAuth2Listener(SyncLoginGui gui, int port) throws IOException {
		parent = gui;
		serverSocket = new ServerSocket(port);
		Port = serverSocket.getLocalPort();
	}
	
	Thread thread;
	public void Start() {
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public void Stop() {
		try {
			serverSocket.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		SaveSync.logger.info("Listening on " + serverSocket.getInetAddress().toString() + ":" + serverSocket.getLocalPort());
		
		try {
			Socket connect = serverSocket.accept();
			SaveSync.logger.info("Got new connection");
			// we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			//dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			// get first line of the request from the client
			String input = in.readLine();
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			// we get file requested
			fileRequested = parse.nextToken().toLowerCase();
			
			SaveSync.logger.info("HTTP request: " + method + " " + fileRequested);
			String query = fileRequested.substring(fileRequested.indexOf("?") + 1);
			
			SaveSync.logger.info("Query string: " + query);
			String[] params = query.split("&");
			String code = "";
			String state = "";
			for(String paramString : params) {
				String[] keypair = paramString.split("=");
				SaveSync.logger.info("Key: " + keypair[0] + ", value: " + keypair[1]);
				if(keypair[0].equals("code")) {
					code = keypair[1];
				} else if(keypair[0].equals("state")) {
					state = keypair[1];
				}
			}
			
			int responseCode;
			try {
				responseCode = parent.GotLoginData(code, state);
			} catch(Exception e) {
				responseCode = 0;
				e.printStackTrace();
			}
			if(responseCode != 200) {
				parent.scheduleUI();
			}
			
			String responseText;
			out.print("HTTP/1.1 " + responseCode);
			if(responseCode == 200) {
				out.print("OK");
				responseText = "Thank you, you have successfully logged in and may close this window.";
			}
			else if (responseCode == 400)  {
				out.print("Bad Request");
				responseText = "State parameter did not match. Please try process again.";
			} else if(responseCode == 500) {
				out.print("Internal Error");
				responseText = "An error occured when trying to communicate with GitHub";
			} else {
				out.print("Bad code");
				responseText = "An unknown internal error occured.";
			}
			
			out.print("\n");
			
			out.println("Server: Java HTTP Server from SSaurel : 1.1");
			out.println("Date: " + new Date());
			out.println("Content-type: text/html");
			out.println("Content-length: " + responseText.length());
			out.println();
			out.print(responseText);
			out.flush();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
