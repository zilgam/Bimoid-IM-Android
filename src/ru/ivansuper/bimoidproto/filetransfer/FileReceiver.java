package ru.ivansuper.bimoidproto.filetransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.bimoidim.ChatActivity;
import ru.ivansuper.bimoidim.PB;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidim.utilities;
import ru.ivansuper.bimoidproto.BEX;
import ru.ivansuper.bimoidproto.BimoidProtocol;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.wTLD;
import ru.ivansuper.bimoidproto.wTLDList;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.socket.ByteBuffer;
import ru.ivansuper.socket.ByteCache;
import ru.ivansuper.socket.ClientSocketConnection;
import ru.ivansuper.socket.ServerSocketConnection;
import ru.ivansuper.ui.MyTextView;

public class FileReceiver extends FileTransfer {
	private String remote_host = "";
	private int remote_port = 0;
	private String proxy_host = "";
	private int proxy_port = 0;
	private int files_count = 0;
	private long total_size = 0;
	private String current_file_name = "";
	private long current_file_size = 0;
	private long current_file_received = 0;
	private long total_received = 0;
	private int files_received = 0;
	private int state;
	private int socket_mode = -1;
	private FileOutputStream fos;
	private boolean sender_supports_proxy;
	public static final int STATE_WAITING = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_TRANSFERING = 2;
	public static final int STATE_RECEIVED = 3;
	public static final int STATE_ERROR = 4;
	public static final int MODE_NORMAL = 0;//CLI_SCK connects to file sender
	public static final int MODE_REVERSED = 1;//Creating server for sender connection
	public static final int MODE_PROXY = 2;//Sender & Receiver connects to proxy server
	private ClientSocketConnection cli_sck;
	private ServerSocketConnection srv_sck;
	private ClientSocketConnection prx_sck;
	private int sequence = 0;
	private Contact contact;
	private int percentage;
	private boolean canceled;
	private view_container transfer_message = new view_container();
	{
		type = FileTransfer.RECEIVER;
	}
	public FileReceiver(byte[] unique_id, Contact contact, String first_file_name, int files_count, long total_size, boolean sender_supports_proxy, int mode){
		this.unique_id = unique_id;
		this.contact = contact;
		current_file_name = first_file_name;
		this.files_count = files_count;
		this.total_size = total_size;
		this.sender_supports_proxy = sender_supports_proxy;
		state = STATE_WAITING;
		socket_mode = mode;
		server_port = 1000+((int)System.currentTimeMillis())%3000;
		cli_sck = new ClientSocketConnection(){
			@Override
				public void onRawData(ByteBuffer data) {
					handleBEX(new BEX(data, true));
					ByteCache.recycle(data.bytes);
				}
				@Override
				public void onConnect() {
					handleNormalDirectConnected();
				}
				@Override
				public void onConnecting() {
					
				}
				@Override
				public void onDisconnect() {
					
				}
				@Override
				public void onLostConnection() {
					
				}
				@Override
				public void onError(int errorCode, Throwable t) {
					if(errorCode == 255)
						handleNormalDirectError();
				}
				@Override
				public void onSocketCreated() {
					
				}
			};
	}
	@Override
	public void runTransfer(){
		Log.i("FileReceiver", "Host: "+remote_host);
		Log.i("FileReceiver", "Port: "+String.valueOf(remote_port));
		state = STATE_CONNECTING;
		updateChatA();
		cli_sck.connect(remote_host, remote_port);
	}
	@Override
	public void runProxyTransfer(){
		if(!sender_supports_proxy) return;
		socket_mode = MODE_PROXY;
		Log.i("FileReceiver", "Host: "+proxy_host);
		Log.i("FileReceiver", "Port: "+String.valueOf(proxy_port));
		state = STATE_CONNECTING;
		updateChatA();
		prx_sck = new ClientSocketConnection(){
			@Override
			public void onRawData(ByteBuffer data) {
				handleBEX(new BEX(data, true));
				ByteCache.recycle(data.bytes);
			}
			@Override
			public void onSocketCreated() {
				
			}
			@Override
			public void onConnect() {
				handleProxyConnected();
			}
			@Override
			public void onConnecting() {
				
			}
			@Override
			public void onDisconnect() {
				
			}
			@Override
			public void onLostConnection() {
				
			}
			@Override
			public void onError(int errorCode, Throwable t) {
				if(errorCode == 255)
					Log.i("FileReceiver", "Proxy error");
					removeTransferFromProfile();
			}
		};
		prx_sck.connect(proxy_host, proxy_port);
	}
	private void handleProxyConnected(){
		Log.i("FileReceiver", "Connected to proxy. Sending hello");
		send(BimoidProtocol.createDIR_PROX_HELLO(sequence, contact.getProfile().ID, unique_id));
	}
	@Override
	public void cancel(){
		Log.i("FileReceiver", "Canceled");
		canceled = true;
		removeTransferFromProfile();
	}
	private void removeTransferFromProfile(){
		if(state == STATE_RECEIVED) return;
		closeSockets();
		state = STATE_ERROR;
		contact.getProfile().removeTransferById(unique_id);
		updateChatA();
	}
	private void closeSockets(){
		switch(socket_mode){
		case MODE_NORMAL:
			cli_sck.disconnect();
			break;
		case MODE_REVERSED:
			srv_sck.disconnect();
			break;
		case MODE_PROXY:
			prx_sck.disconnect();
			break;
		}
		try{
			fos.close();
		}catch(Exception e){}
	}
	public void force_cancel(){
		closeSockets();
		state = STATE_ERROR;
		contact.getProfile().removeTransferById(unique_id);
		contact.getProfile().sendFTControl(unique_id, contact.getID(), FileTransfer.FT_CONTROL_CODE_CANCEL);
		updateChatA();
	}
	private void handleBEX(BEX bex){
		switch(bex.getSubType()){
		case 0x101:
			handleError(bex);
			break;
		case 0x102:
			handleHELLO(bex);
			break;
		case 0x103:
			handleFileHeader(bex);
			break;
		case 0x105:
			handleFileData(bex);
			break;
		}
		bex.recycle();
	}
	private void handleError(BEX bex){
		Log.i("FileReceiver", "Transfer error");
		removeTransferFromProfile();
	}
	private void handleHELLO(BEX bex){
		if(socket_mode == MODE_REVERSED){
			send(BimoidProtocol.createDIR_PROX_HELLO(sequence, contact.getProfile().ID, unique_id));
		}
	}
	private void handleFileHeader(BEX bex){
		state = STATE_TRANSFERING;
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x3);
		current_file_size = (int)tld.getData().readLong();
		tld = list.getTLD(0x4);
		current_file_name = tld.getData().readStringUTF8(tld.getLength());
		current_file_received = 0;
		File dir = new File(resources.INCOMING_FILES_PATH+contact.getProfile().ID+"/"+contact.getID());
		if(!dir.exists())
			try{
				dir.mkdirs();
			}catch(Exception e){
				e.printStackTrace();
				force_cancel();
				return;
			}
		openFile();
		updateChatA();
		send(BimoidProtocol.createDIR_PROX_FILE_REPLY(sequence, contact.getProfile().ID, unique_id));
	}
	private void openFile(){
		int index = 1;
		File incoming_file = new File(resources.INCOMING_FILES_PATH+contact.getProfile().ID+"/"+contact.getID()+"/"+current_file_name);
		if(incoming_file.exists()){
			while(true){
				index++;
				incoming_file = new File(resources.INCOMING_FILES_PATH+contact.getProfile().ID+"/"+contact.getID()+"/("+String.valueOf(index)+") "+current_file_name);
				if(!incoming_file.exists())
					break;
			}
		}
		try {
			fos = new FileOutputStream(incoming_file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			force_cancel();
			return;
		}
	}
	private void handleFileData(BEX bex){
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x5);
		byte[] raw_file_data = tld.getData().readBytesA(tld.getLength());
		try {
			fos.write(raw_file_data, 0, tld.getLength());
			ByteCache.recycle(raw_file_data);
		} catch (Exception e) {
			e.printStackTrace();
			force_cancel();
			return;
		}
		current_file_received += tld.getLength();
		total_received += tld.getLength();
		tld = list.getTLD(0x3);
		boolean is_last_file = tld.getData().readBoolean();
		if(current_file_received >= current_file_size){
			if(is_last_file) state = STATE_RECEIVED;
			Log.i("FileReceiver", "File '"+current_file_name+"' received successful!");
			if(is_last_file){
				closeSockets();
			}
			updateChat();
		}
		updatePercentage();
		list.recycle();
	}
	private void handleNormalDirectError(){
		Log.i("FileReceiver", "Can't connect to the sender via direct connection. Trying redirect. Notifying sender ...");
		socket_mode = MODE_REVERSED;
		updateChatA();
		srv_sck = new ServerSocketConnection(){
			@Override
			public void onRawData(ByteBuffer data) {
				handleBEX(new BEX(data, true));
				ByteCache.recycle(data.bytes);
			}
			@Override
			public void onCreate() {
				Log.i("FileReceiver", "Server created at port 4331. Waiting for connection");
			}
			@Override
			public void onClientConnected() {
				handleClientConnected();
			}
			@Override
			public void onDisconnect() {
				
			}
			@Override
			public void onLostConnection() {
				
			}
			@Override
			public void onError(int errorCode) {
				if(errorCode == 255){
					Log.i("FileReceiver", "Server create error");
					socket_mode = MODE_PROXY;
					if(!sender_supports_proxy) contact.getProfile().sendFTControl(unique_id, contact.getID(), FileTransfer.FT_CONTROL_CODE_DIRECT_FAILED);
					if(!sender_supports_proxy) removeTransferFromProfile();
				}
			}
		};
		srv_sck.createServer(server_port);
		contact.getProfile().sendFTControl(unique_id, contact.getID(), FileTransfer.FT_CONTROL_CODE_DIRECT_FAILED_TRY_REVERSE);
	}
	private void handleClientConnected(){
		Log.i("FileReceiver", "Client connected. Starting transfer");
	}
	private void handleNormalDirectConnected(){
		updateChatA();
		Log.i("FileReceiver", "Connected to sender with normal direct mode. Sending hello");
		send(BimoidProtocol.createDIR_PROX_HELLO(sequence, contact.getProfile().ID, unique_id));
	}
	private void send(ByteBuffer data){
		switch(socket_mode){
		case MODE_NORMAL:
			cli_sck.write(data);
			break;
		case MODE_REVERSED:
			srv_sck.write(data);
			break;
		case MODE_PROXY:
			prx_sck.write(data);
			break;
		}
		data.bytes = null;
		data = null;
		sequence++;
	}
	private void updateChat(){
		if(ChatActivity.checkThisContactOpenedInChat(contact)){
			Message msg = Message.obtain(contact.getProfile().svc.svcHdl, new Runnable(){
				@Override
				public void run() {
					transfer_message.updateViews();
					//Log.i("UpdateChat", "Updating chat ...");
				}
			});
			contact.getProfile().svc.svcHdl.sendMessageDelayed(msg, 100);
		}
	}
	private void updateChatA(){
		if(ChatActivity.checkThisContactOpenedInChat(contact)){
			Message msg = Message.obtain(contact.getProfile().svc.svcHdl, new Runnable(){
				@Override
				public void run() {
					contact.getProfile().svc.refreshChat();
					//Log.i("UpdateChat", "Updating chat ...");
				}
			});
			contact.getProfile().svc.svcHdl.sendMessageDelayed(msg, 100);
		}
	}
	public void updatePercentage(){
		int old = percentage;
		if(current_file_size > 0){
			percentage = (int)(current_file_received*100/current_file_size);
		}else{
			percentage = 0;
		}
		if(old != percentage)
			updateChat();
	}
	public int getMode(){
		return socket_mode;
	}
	public String getProcessingFileName(){
		return current_file_name;
	}
	public long getTotalReceived(){
		return total_received;
	}
	public long getProcessingReceived(){
		return current_file_received;
	}
	public int getFilesCount(){
		return files_count;
	}
	public int getReceivedFilesCount(){
		return files_received;
	}
	public long getTotalSize(){
		return total_size;
	}
	public long getProcessingSize(){
		return current_file_size;
	}
	public int getPercentage(){
		return percentage;
	}
	public int getState(){
		return state;
	}
	public Contact getContact(){
		return contact;
	}
	public String getLocalHost(){
		return srv_sck.getIP();
	}
	public view_container getContainer(){
		return transfer_message;
	}
	public boolean canceled(){
		return canceled;
	}
	public void setRemoteAddress(String host, int port){
		remote_host = host;
		remote_port = port;
	}
	public void setProxyAddress(String host, int port){
		proxy_host = host;
		proxy_port = port;
	}
	@Override
	public void reverseConnection(){
		
	}
	public class view_container {
		private MyTextView view;
		private PB progress;
		private Button transfer_accept;
		private Button transfer_decline;
		private LinearLayout transfer_buttons;
		public void setText(MyTextView view){
			this.view = view;
		}
		public void setProgress(PB progress){
			this.progress = progress;
		}
		public void setButtons(LinearLayout view){
			this.transfer_buttons = view;
		}
		public void setAccept(Button accept){
			this.transfer_accept = accept;
		}
		public void setDecline(Button decline){
			this.transfer_decline = decline;
		}
		public MyTextView getText(){
			return view;
		}
		public LinearLayout getButtons(){
			return transfer_buttons;
		}
		public Button getAccept(){
			return transfer_accept;
		}
		public Button getDecline(){
			return transfer_decline;
		}
		public void detachViews(){
			view = null;
			progress = null;
			transfer_buttons = null;
			transfer_accept = null;
			transfer_decline = null;
		}
		public void updateViews(){
			if(view == null) return;
			if(progress == null) return;
			if(transfer_buttons == null) return;
			if(transfer_accept == null) return;
			if(transfer_decline == null) return;
			//Log.i("Container", "Updating views");
			switch(getState()){
			case FileReceiver.STATE_WAITING:
				transfer_accept.setVisibility(View.VISIBLE);
				progress.setVisibility(View.GONE);
				break;
			case FileReceiver.STATE_CONNECTING:
				transfer_accept.setVisibility(View.GONE);
				switch(getMode()){
				case FileReceiver.MODE_NORMAL:
					view.setText(Locale.getString("s_file_transfer_label_1"));
					break;
				case FileReceiver.MODE_REVERSED:
					view.setText(Locale.getString("s_file_transfer_label_2"));
					break;
				case FileReceiver.MODE_PROXY:
					view.setText(Locale.getString("s_file_transfer_label_3"));
					break;
				}
				view.relayout();
				break;
			case FileReceiver.STATE_TRANSFERING:
				progress.setVisibility(View.VISIBLE);
				progress.setColor(ColorScheme.getColor(18));
				progress.setMax(getTotalSize());
				progress.setProgress(getTotalReceived());
				transfer_accept.setVisibility(View.GONE);
				view.setText(utilities.match(Locale.getString("s_file_receiving"), new String[]{String.valueOf(getReceivedFilesCount()+1), String.valueOf(getFilesCount()), getProcessingFileName(), String.valueOf(getPercentage()), String.valueOf(getProcessingSize())}));
				view.relayout();
				break;
			case FileReceiver.STATE_RECEIVED:
				progress.setVisibility(View.GONE);
				transfer_buttons.setVisibility(View.GONE);
				if(getFilesCount() > 1){
					view.setText(Locale.getString("s_files_received"));
				}else{
					view.setText(utilities.match(Locale.getString("s_file_received"), new String[]{getProcessingFileName()}));
				}
				view.relayout();
				break;
			case FileReceiver.STATE_ERROR:
				progress.setVisibility(View.GONE);
				transfer_buttons.setVisibility(View.GONE);
				if(canceled()){
					view.setText(Locale.getString("s_file_receiving_canceled"));
				}else{
					view.setText(Locale.getString("s_file_receiving_error"));
				}
				view.relayout();
				break;
			}
			((LinearLayout)view.getParent()).invalidate();
		}
	}
}
