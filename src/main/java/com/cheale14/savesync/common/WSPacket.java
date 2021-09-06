package com.cheale14.savesync.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class WSPacket {
	public WSPacket() {
	}
	private String id;
	private JsonElement content;
	
	
	public String Id() {
		return id;
	}
	
	public void Id(String value) {
		this.id = value;
	}
	
	public JsonElement Content() {
		return content;
	}
	
	public void Content(String value) {
		this.content = new JsonPrimitive(value);
	}
	public void Content(Integer value) {
		this.content = new JsonPrimitive(value);
	}
	public void Content(JsonElement value) {
		this.content = value;
	}
	

}
