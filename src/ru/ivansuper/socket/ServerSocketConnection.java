package ru.ivansuper.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import ru.ivansuper.bimoidproto.filetransfer.FileTransfer;
import ru.ivansuper.bservice.BimoidService;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public abstract class ServerSocketConnection {
	public boolean connected = false;
	public boolean connecting = false;
	private connectedThread connectedThrd;
	private InputStream socketIn;
	private OutputStream socketOut;
	public int lastErrorCode = -1;
	public String lastServer = "none";
	public int lastPort = 0;
	private ServerSocket socket;
	public abstract void onRawData(ByteBuffer data);
	public abstract void onCreate();
	public abstract void onClientConnected();
	public abstract void onDisconnect();
	public abstract void onLostConnection();
	public abstract void onError(int errorCode);
	private BimoidService svc;
	public Handler flapHandler;
	public int listening_port;
	public ServerSocketConnection(BimoidService param){
		svc = param;
	}
	public ServerSocketConnection(BimoidService param, Handler hdl){
		svc = param;
		flapHandler = hdl;
	}
	public ServerSocketConnection() {
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
	public void createServer(int port){
		listening_port = port;
		createThread t = new createThread();
		t.start();
	}
	public void disconnect(){
		if(connected){
			Log.v("SERVER_SOCKET", "Disconnecting called");
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
				//Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
				lastErrorCode = 5;
				errorOccured();
			}
		}
	}
	public String getIP(){
		InetAddress addr = socket.getInetAddress();
		return addr.getHostAddress();
	}
	public int getPort(){
		return 23780;
	}
	public class createThread extends Thread{
		@Override
		public void run(){
			Log.v("SOCKET", "Connect thread started!");
			connecting = true;
			try {
				socket = new ServerSocket(listening_port);
				socket.setSoTimeout(3000);
				onCreate();
				Socket sck = socket.accept();
				socketIn = sck.getInputStream();
				socketOut = sck.getOutputStream();
				connecting = false;
				connected = true;
				connectedThrd = new connectedThread();
				connectedThrd.start();
				onClientConnected();
			} catch (Exception e) {
				e.printStackTrace();
				lastErrorCode = 255;
				errorOccuredA();
			}
		}
	}
	private class connectedThread extends Thread{
		final ByteBuffer flap = new ByteBuffer(17);
		final byte[] header = new byte[17];
		private boolean enabled = true;
		public void cancel(){
			enabled = false;
		}
		@Override
		public void run(){
			setName("ServerSocket thread");
			Thread.currentThread().setPriority(2);
			int readed = 0;
			Log.v("SOCKET", "Connected thread started!!!");
			while(enabled){
					if(!enabled){
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
									return;
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
									return;
								}
							}
							if(readed != -0x1) realyReaded += readed;
						}
					    flap.bytes = raw;
						flap.readPos = 0;
						flap.writePos = data_length+header.length;
						if(flap.previewByte(0) != 0x23){
							Log.e("Socket", "Invalid BEX!");
							throw new IOException();
						}
						if(flapHandler == null){
							onRawData(flap);
						}else{
							flapHandler.sendMessage(Message.obtain(flapHandler, 2, flap));
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
