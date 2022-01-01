package com.cheale14.savesync.client.discord;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import net.minecraft.client.Minecraft;

public class IPCClient extends Thread {
	public static Logger logger = LogManager.getLogger("savesync-IPC");
	private RandomAccessFile socket;
	public Gson _gson = new Gson();
	private List<IPCHandler> _handlers = new ArrayList<IPCHandler>();
	private IPCState _state = IPCState.DISCONNECTED;
	
	public DiscordUser user;
	
	public boolean HasStarted() {
		return _state != IPCState.DISCONNECTED;
	}
	public IPCState state() {
		return _state;
	}
	public void state(IPCState st) {
		_state = st;
		emit((IPCHandler h) -> {
			h.OnState(st);
		});
	}
	
	public IPCClient() {
	}
	
	public void addHandler(IPCHandler handler) {
		_handlers.add(handler);
	}
	public void removeHandler(IPCHandler handler) {
		_handlers.remove(handler);
	}
	
	private RandomAccessFile _connect(int id) throws FileNotFoundException {
		String path = "\\\\?\\pipe\\discord-ipc-" + id;
		RandomAccessFile pipe = new RandomAccessFile(path, "rw");
		
		return pipe;
	}
	
	private ByteBuffer encode(int op, JsonElement jdata) {
		String data = _gson.toJson(jdata);
		byte[] bytes = data.getBytes();
		ByteBuffer encoded = ByteBuffer.allocate(bytes.length + 8);
		encoded.order(ByteOrder.LITTLE_ENDIAN);
		encoded.putInt(op);
		encoded.putInt(bytes.length);
		for(byte b : bytes) {
			encoded.put(b);
		}
		return encoded;
	}
	
	public static int readIntLittleEndian(RandomAccessFile file)
            throws IOException {
        int a = file.readByte() & 0xFF;
        int b = file.readByte() & 0xFF;
        int c = file.readByte() & 0xFF;
        int d = file.readByte() & 0xFF;
        int res = (d << 24) | (c << 16) | (b << 8) | a;
        return res;
    }
	
	
	private IPCPacket read() throws IOException {
		while(socket.length() == 0) {
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		int op = 0;
		while(op == 0) {
			op = readIntLittleEndian(socket);
		}
		int len = 0;
		while(len == 0) {
			len = readIntLittleEndian(socket);
		}
		logger.info("[<+] op=" + op + ", len=" + len);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int count = 0;
		while(count < len) {
			byte next = socket.readByte();
			baos.write(next);
			count++;
		}
		
		String s = baos.toString();
		logger.info("[<<] " + op + ": " + s);
		JsonElement j = new JsonParser().parse(s);
		IPCOpCode code = IPCOpCode.values()[op];
		IPCPacket packet;
		if(code == IPCOpCode.FRAME) {
			IPCFramePacket frame = new IPCFramePacket();
			frame.op = code;
			frame.data = j;
			frame.payload = _gson.fromJson(j, IPCPayload.class);
			packet = frame;
		} else {
			packet = new IPCPacket();
			packet.op = code;
			packet.data = j;
		}
		
		return packet;
	}
	
	private void connect() throws IOException {
		state(IPCState.CONNECTING);
		socket = null;
		int id = 0;
		while(socket == null) {
			try {
				socket = _connect(id);
			} catch(FileNotFoundException e) {
				if(id < 10) {
					logger.warn(e.toString());
					id++;
				} else {
					throw e;
				}
			}
		}
		logger.info("Connected to IPC pipe, sending handshake..");
		state(IPCState.CONNECTED);
		this.Send(new IPCHandshakePacket());
	}
	
	public void Send(JsonElement data, IPCOpCode op) throws IOException {
		logger.info("[>>] " + op.name()  + ": " + this._gson.toJson(data));
		ByteBuffer buf = this.encode(op.ordinal(), data);
		socket.write(buf.array());
		logger.info("[>/] Sent");
	}
	public void Send(JsonElement data) throws IOException {
		Send(data, IPCOpCode.FRAME);
	}
	public void Send(IPCPacket packet) throws IOException {
		Send(packet.data, packet.op);
	}
	public void Send(IPCPayload payload) throws IOException {
		Send(_gson.toJsonTree(payload));
	}
	public IPCFramePacket SendWaitResponse(IPCPayload payload) throws IOException {
		if(payload.nonce == null) {
			payload.nonce = UUID.randomUUID().toString();
		}
		Send(payload);
		return wait_for(payload.nonce);
	}
	
	public void Pong() throws JsonSyntaxException, IOException {
		Send(new JsonPrimitive(123456), IPCOpCode.PONG);
	}
	public void Start() throws IOException {
		connect();
		start();
	}
	public void Stop() throws IOException {
		Send(null, IPCOpCode.CLOSE);
		socket.close();
		socket = null;
	}
	
	private Map<String, AutoResetEvent> waitings = new HashMap<String, AutoResetEvent>();
	
	public IPCFramePacket wait_for(String nonce) {
		AutoResetEvent v = new AutoResetEvent(false);
		waitings.put(nonce, v);
		logger.info("Waiting for " + nonce);
		try {
			v.waitOne();
		} catch (InterruptedException e) {
			logger.info(e);
		}
		return v.packet;
	}
	
	Thread emit(Consumer<IPCHandler> cns) {
		Thread emitThread = new Thread(() -> {
			try {
				for(IPCHandler handle : _handlers) {
					cns.accept(handle);
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		});
		emitThread.start();
		return emitThread;
	}
	
	@Override
	public void run() {
		int errors = 0;
		while(socket != null) {
			try {
				logger.info("Waiting for next packet...");
				IPCPacket next = read();
				emit((IPCHandler h) -> {
					h.OnPacketPre(next);
				});
				if(next.op == IPCOpCode.FRAME) {
					IPCFramePacket p = (IPCFramePacket)next;
					if(waitings.containsKey(p.payload.nonce)) {
						logger.info("Triggering reset event for " + p.payload.nonce);
						AutoResetEvent are = waitings.get(p.payload.nonce);
						are.set(p);
						waitings.remove(p.payload.nonce);
						return;
					}
				}
				emit((IPCHandler h) -> {
					try {
						h.OnPacket(next);
					} catch (IOException e) {
						e.printStackTrace();
						logger.catching(e);
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
				logger.error(e);
				errors++;
				if(errors > 2) {
					try {
						this.Stop();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					break;
				}
			}
		}
		logger.info("[--] Listen thread exit");
	}
}
