package ru.ivansuper.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ru.ivansuper.bimoidproto.BimoidProfile;
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
	public abstract void onError(int errorCode, Throwable t);
	private int delay;
	private BimoidProfile profile;
	public ClientSocketConnection(BimoidProfile profile){
		this.profile = profile;
	}
	public ClientSocketConnection(){
	}
	private void errorOccured(Throwable t){
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
		onError(lastErrorCode, t);
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
		onError(lastErrorCode, null);
		onDisconnect();
	}
	public boolean write(ByteBuffer source){
		if(connected)
			return writeA(source);
		return false;
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
	public boolean writeA(ByteBuffer buffer){
		if(connected){
			try {
				socketOut.write(ByteBuffer.normalizeBytes(buffer.bytes, buffer.writePos));
				buffer = null;
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				lastErrorCode = 5;
				errorOccured(null);
			}
		}
		return false;
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
				if(profile != null && profile.use_ssl) lastPort = 7033;
				InetSocketAddress addr = new InetSocketAddress(lastServer, lastPort);
				socket = new Socket();
				onSocketCreated();
				socket.connect(addr, 7000);
				if(profile != null && profile.use_ssl) jumpToSSL();
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
	private final void jumpToSSL(){
		try {
			TrustManager[] tm = new TrustManager[]{new NaiveTrustManager()};
			SSLContext context = SSLContext.getInstance("SSL");
			context.init(new KeyManager[0], tm, new SecureRandom());
			System.setProperty("javax.net.ssl.trustStore", "ru.ivansuper.bimoidim.NaiveTrustManager");
			SSLSocketFactory factory = context.getSocketFactory();
			socket = (SSLSocket)factory.createSocket(socket, lastServer, lastPort, true);
		} catch (Exception e) {
			e.printStackTrace();
			lastErrorCode = 0;
			onError(0, e);
			return;
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
						errorOccured(null);
						break;
					}
					try{
						int realyReaded = 0;
						while(realyReaded < header.length){
							readed = socketIn.read(header, realyReaded, header.length-realyReaded);
							if(readed == -0x1){
								if(connected){
									lastErrorCode = 3;
									errorOccured(null);
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
									errorOccured(null);
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
						onRawData(bex);
					}catch(IOException e) {
						if(connected){
							e.printStackTrace();
							lastErrorCode = 3;
							errorOccured(e);
							enabled = false;
						}
						continue;
					}catch(Exception e) {
						if(connected){
							e.printStackTrace();
							lastErrorCode = 4;
							errorOccured(e);
							enabled = false;
						}
						continue;
					}
				}
		}
	}
	public final class NaiveTrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
