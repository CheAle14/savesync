package com.cheale14.savesync.http;

public class HttpError extends Exception {
	private int statusCode;
	private String errorBody;
	
	public HttpError(int code, String body) {
		statusCode = code;
		errorBody = body;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	public String getErrorBody() {
		return errorBody;
	}
}
