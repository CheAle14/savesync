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

import com.cheale14.savesync.common.SaveSync;


public class OAuth2Listener implements Runnable {


	ServerSocket serverSocket;
	public int Port;
	
	public OAuth2Listener(int port) throws IOException {
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
			
			String response = "Thank you for your redirect!";
			
			out.println("HTTP/1.1 200 OK");
			out.println("Server: Java HTTP Server from SSaurel : 1.1");
			out.println("Date: " + new Date());
			out.println("Content-type: text/html");
			out.println("Content-length: " + response.length());
			out.println();
			out.print(response);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
