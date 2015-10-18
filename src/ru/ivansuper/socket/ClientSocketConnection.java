package ru.ivansuper.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import ru.ivansuper.bservice.BimoidService;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public abstract class ClientSocketConnection {
	public boolean connected = false;
	public boolean connecting = false;
	private connectedThread connectedThrd;
	private InputStream socketIn;
	private OutputStream socketOut;
	public int lastErrorCode = -1;
	public String lastServer = "none";
	public int lastPort = 0;
	private Socket socket;
	public abstract void onRawData(ByteBuffer data);
	public abstract void onSocketCreated();
	public abstract void onConnect();
	public abstract void onConnecting();
	public abstract void onDisconnect();
	public abstract void onLostConnection();
	public abstract void onError(int errorCode);
	private BimoidService svc;
	public Handler bexHandler;
	private int delay;
	public ClientSocketConnection(BimoidService param){
		svc = param;
	}
	public ClientSocketConnection(BimoidService param, Handler hdl){
		svc = param;
		bexHandler = hdl;
	}
	public ClientSocketConnection() {
	}
	private void errorOccured(){
		if((socket != null) && connected){
			try {
				socket.close();
			} catch (Exception e) {}
		}
		connecting = false;
		connected = false;
		try {
			socketIn.close();
			socketOut.close();
		} catch (Exception e) {}
		onError(lastErrorCode);
		onLostConnection();
	}
	private void errorOccuredA(){
		if((socket != null) && connected){
			try {
				socket.close();
			} catch (Exception e) {}
		}
		connecting = false;
		connected = false;
		try {
			socketIn.close();
		} catch (Exception e) {}
		try {
			socketOut.close();
		} catch (Exception e) {}
		onError(lastErrorCode);
		onDisconnect();
	}
	public void write(ByteBuffer source){
		if(connected){
			writeA(source);
		}
	}
	public void connect(String server, int port, int delay){
		if(!connected){
			this.delay = delay;
			lastServer = server;
			lastPort = port;
			Log.v("SOCKET", "Connecting called");
			connectThread cnt = new connectThread();
			cnt.start();
		}
	}
	public void connect(String server, int port){
		if(!connected){
			delay = 0;
			lastServer = server;
			lastPort = port;
			Log.v("SOCKET", "Connecting called");
			connectThread cnt = new connectThread();
			cnt.start();
		}
	}
	public void connect(String fullServerName){
		if(!connected){
			delay = 0;
			String server[] = fullServerName.split(":");
			lastServer = server[0];
			lastPort = Integer.parseInt(server[1]);
			Log.v("SOCKET", "Connecting called");
			connectThread cnt = new connectThread();
			cnt.start();
		}
	}
	public void disconnect(){
		if(connected){
			connected = false;
			connectedThrd.cancel();
			try {
				socketIn.close();
				socketOut.close();
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			connecting = false;
			onDisconnect();
		}
	}
	public void writeA(ByteBuffer buffer){
		if(connected){
			try {
				socketOut.write(ByteBuffer.normalizeBytes(buffer.bytes, buffer.writePos));
				buffer = null;
			} catch (Exception e) {
				e.printStackTrace();
				lastErrorCode = 5;
				errorOccured();
			}
		}
	}
	public String getIp(){
		InetAddress addr = socket.getLocalAddress();
		return addr.getHostAddress();
	}
	public class connectThread extends Thread{
		@Override
		public void run(){
			Log.v("SOCKET", "Connect thread started!");
			connecting = true;
			try {
				onConnecting();
				sleep(delay);
				InetSocketAddress addr = new InetSocketAddress(lastServer, lastPort);
				socket = new Socket();
				onSocketCreated();
				socket.connect(addr, 7000);
				socketIn = socket.getInputStream();
				socketOut = socket.getOutputStream();
				connecting = false;
				connected = true;
				connectedThrd = new connectedThread();
				connectedThrd.setName("SocketReader");
				connectedThrd.start();
				onConnect();
			} catch (Exception e) {
				e.printStackTrace();
				lastErrorCode = 255;
				errorOccuredA();
			}
		}
	}
	private class connectedThread extends Thread{
		final ByteBuffer bex = new ByteBuffer(17);
		final byte[] header = new byte[17];
		private boolean enabled = true;
		public void cancel(){
			enabled = false;
		}
		@Override
		public void run(){
			setName("ClientSocket thread");
			Thread.currentThread().setPriority(2);
			int readed = 0;
			Log.v("SOCKET", "Connected thread started!!!");
			while(enabled){
					if(!enabled){
						break;
					}
					if(!socket.isConnected()){
						lastErrorCode = 7;
						errorOccured();
						break;
					}
					try{
						int realyReaded = 0;
						while(realyReaded < header.length){
							readed = socketIn.read(header, realyReaded, header.length-realyReaded);
							if(readed == -0x1){
								if(connected){
									lastErrorCode = 3;
									errorOccured();
									enabled = false;
								}
							}
							if(readed != -0x1) realyReaded += readed;
						}
					    int data_length = header[16] & 0xFF | ((header[15] & 0xFF) << 8) | ((header[14] & 0xFF) << 16) | ((header[13] & 0xFF) << 24);
					    byte[] raw = ByteCache.getByteArray(data_length+64);
					    System.arraycopy(header, 0, raw, 0, header.length);
						realyReaded = 0;
						while(realyReaded < data_length){
							readed = socketIn.read(raw, realyReaded+header.length, data_length-realyReaded);
							if(readed == -0x1){
								if(connected){
									lastErrorCode = 3;
									errorOccured();
									enabled = false;
								}
							}
							if(readed != -0x1) realyReaded += readed;
						}
					    bex.bytes = raw;
						bex.readPos = 0;
						bex.writePos = data_length+header.length;
						if(bex.previewByte(0) != 0x23){
							Log.e("Socket", "Invalid BEX!");
							throw new IOException();
						}
						if(bexHandler == null){
							onRawData(bex);
						}else{
							bexHandler.sendMessage(Message.obtain(bexHandler, 2, bex));
						}
					}catch(IOException e) {
						if(connected){
							e.printStackTrace();
							lastErrorCode = 3;
							errorOccured();
							enabled = false;
						}
						continue;
					}catch(Exception e) {
						if(connected){
							e.printStackTrace();
							lastErrorCode = 4;
							errorOccured();
							enabled = false;
						}
						continue;
					}
				}
		}
	}
}
