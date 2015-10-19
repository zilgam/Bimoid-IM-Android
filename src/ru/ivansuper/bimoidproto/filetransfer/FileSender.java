package ru.ivansuper.bimoidproto.filetransfer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.bimoidim.ChatActivity;
import ru.ivansuper.bimoidim.PB;
import ru.ivansuper.bimoidim.utilities;
import ru.ivansuper.bimoidproto.BEX;
import ru.ivansuper.bimoidproto.BimoidProtocol;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.wTLD;
import ru.ivansuper.bimoidproto.wTLDList;
import ru.ivansuper.bimoidproto.filetransfer.FileReceiver.view_container;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.socket.ByteBuffer;
import ru.ivansuper.socket.ByteCache;
import ru.ivansuper.socket.ClientSocketConnection;
import ru.ivansuper.socket.ServerSocketConnection;
import ru.ivansuper.ui.MyTextView;

public class FileSender extends FileTransfer {
	private Contact contact;
	private Vector<File> files_to_send = new Vector<File>();
	private long total_size;
	private long total_sended;
	private long current_file_size;
	private long current_file_sended;
	private int files_count;
	private int files_sended;
	private int current_file_index = 0;
	private String current_file_name;
	private int state;
	private int socket_mode;
	private FileInputStream fis;
	private String proxy_host = "";
	private int proxy_port;
	private boolean supports_proxy;
	private int server_port;
	public static final int STATE_WAITING = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_TRANSFERING = 2;
	public static final int STATE_SENDED = 3;
	public static final int STATE_ERROR = 4;
	public static final int MODE_NORMAL = 0;//Receiver connects to the srv_sck
	public static final int MODE_REVERSED = 1;//Connecting to the receiver
	public static final int MODE_PROXY = 2;//Sender & Receiver connects to the proxy server
	private ClientSocketConnection cli_sck;
	private ServerSocketConnection srv_sck;
	private ClientSocketConnection prx_sck;
	private int sequence = 0;
	private int percentage;
	private boolean canceled;
	private long resume_position;
	private long timestamp;//Used for cancel sending thread
	private String remote_host;
	private int remote_port;
	private view_container transfer_message = new view_container();
	{
		type = FileTransfer.SENDER;
	}
	public FileSender(Contact contact, String[] files, boolean supports_proxy){
		this.contact = contact;
		for(String file: files){
			File file_ = new File(file);
			if(file_.exists()){
				total_size += file_.length();
				files_to_send.add(new File(file));
				files_count++;
			}
		}
		socket_mode = MODE_NORMAL;
		this.supports_proxy = supports_proxy;
		unique_id = utilities.generateUniqueID();
		current_file_name = files_to_send.get(0).getName();
		server_port = 2000+((byte)(System.currentTimeMillis()%3000));
		state = STATE_WAITING;
		contact.getProfile().sendFileTransferAsk(unique_id, contact.getID(), files_count, total_size, current_file_name, server_port);
	}
	@Override
	public void runTransfer() {
		srv_sck = new ServerSocketConnection(){
			@Override
			public void onRawData(ByteBuffer data) {
				handleBEX(new BEX(data, true));
				ByteCache.recycle(data.bytes);
			}
			@Override
			public void onCreate() {
				handleServerCreated();
			}
			@Override
			public void onClientConnected() {
				handleNormalDirectConnected();
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
					Log.e("FileSender:error", "Client connection timeout. Trying reverse connection");
				}
			}
		};
		srv_sck.createServer(server_port);
	}
	private void handleServerCreated(){
		state = STATE_CONNECTING;
		contact.getProfile().sendFTControl(unique_id, contact.getID(), FileTransfer.FT_CONTROL_CODE_READY);
		updateChatA();
	}
	private void handleNormalDirectConnected(){
		Log.i("FileSender", "Client connected. Starting transfer session");
	}
	@Override
	public void cancel(){
		Log.i("FileReceiver", "Canceled");
		canceled = true;
		timestamp = System.currentTimeMillis();
		removeTransferFromProfile();
	}
	private void closeSockets(){
		timestamp = System.currentTimeMillis();
		if(srv_sck != null) srv_sck.disconnect();
		if(cli_sck != null) cli_sck.disconnect();
		if(prx_sck != null) prx_sck.disconnect();
		try{
			fis.close();
		}catch(Exception e){}
	}
	private void removeTransferFromProfile(){
		if(state == STATE_SENDED) return;
		closeSockets();
		state = STATE_ERROR;
		contact.getProfile().removeTransferById(unique_id);
		updateChatA();
	}
	private void updateChat(){
		if(ChatActivity.checkThisContactOpenedInChat(contact)){
			Message msg = Message.obtain(contact.getProfile().svc.svcHdl, new Runnable(){
				@Override
				public void run() {
					transfer_message.updateViews();
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
				}
			});
			contact.getProfile().svc.svcHdl.sendMessageDelayed(msg, 100);
		}
	}
	@Override
	public void reverseConnection(){
		closeSockets();
		socket_mode = MODE_REVERSED;
		cli_sck = new ClientSocketConnection(){
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
				handleConnectedToTheReceiver();
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
				if(errorCode == 255){
					if(supports_proxy){
						contact.getProfile().sendFTControl(unique_id, contact.getID(), FileTransfer.FT_CONTROL_CODE_DIRECT_FAILED_TRY_PROXY);
						runProxyTransfer();
					}else{
						removeTransferFromProfile();
					}
					updateChatA();
					return;
				}
			}
		};
		cli_sck.connect(remote_host, remote_port);
		updateChatA();
	}
	private void handleConnectedToTheReceiver(){
		Log.i("FileSender", "Connected to the receiver");
		send(BimoidProtocol.createDIR_PROX_HELLO(sequence, contact.getProfile().ID, unique_id));
		updateChatA();
	}
	@Override
	public void runProxyTransfer() {
		socket_mode = MODE_PROXY;
		if(!supports_proxy){
			contact.getProfile().sendFTControl(unique_id, contact.getID(), FileTransfer.FT_CONTROL_CODE_CANCEL);
			removeTransferFromProfile();
		}else{
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
					handleConnectedToProxy();
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
					removeTransferFromProfile();
					updateChatA();
					return;
				}
			};
			prx_sck.connect(proxy_host, proxy_port);
		}
		updateChatA();
	}
	private void handleConnectedToProxy(){
		Log.i("FileSender", "Connected to proxy");
		send(BimoidProtocol.createDIR_PROX_HELLO(sequence, contact.getProfile().ID, unique_id));
	}
	private void handleBEX(BEX bex){
		//Log.d("FileSender BEX", "Type: "+String.valueOf(bex.getType())+"   SubType: "+String.valueOf(bex.getSubType()));
		switch(bex.getSubType()){
		case 0x101:
			handleError(bex);
			break;
		case 0x102:
			handleHELLO(bex);
			break;
		case 0x104:
			handleFILE_REPLY(bex);
			break;
		}
	}
	private void handleError(BEX bex){
		Log.i("FileSender", "Transfer error");
		removeTransferFromProfile();
	}
	private void handleHELLO(BEX bex){
		if(socket_mode == MODE_NORMAL){
			send(BimoidProtocol.createDIR_PROX_HELLO(sequence, contact.getProfile().ID, unique_id));
		}
		switchAndPrepare();
	}
	private void switchAndPrepare(){
		if(current_file_index == files_count){
			closeSockets();
			state = STATE_SENDED;
			updateChatA();
			return;
		}
		File file = files_to_send.get(current_file_index);
		current_file_size = file.length();
		current_file_sended = 0;
		current_file_name = file.getName();
		current_file_index++;
		send(BimoidProtocol.createDIR_PROX_FILE_HEADER(sequence, contact.getProfile().ID, unique_id, current_file_size, current_file_name));
		state = STATE_TRANSFERING;
		updateChatA();
	}
	private final boolean isConnected(){
		if(cli_sck != null) if(cli_sck.connected) return true;
		if(srv_sck != null) if(srv_sck.connected) return true;
		if(prx_sck != null) if(prx_sck.connected) return true;
		return false;
	}
	private void handleFILE_REPLY(BEX bex){
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x3);
		resume_position = tld.getData().readLong();
		if(resume_position >= current_file_size){
			sendEmptyDataAndSwitchToNext();
			return;
		}
		try {
			final DataInputStream dis = new DataInputStream(new FileInputStream(files_to_send.get(current_file_index-1)));
			dis.skip(resume_position);
			Thread sender = new Thread(){
				private final long stamp = timestamp;
				private final byte[] buffer = new byte[0x800];
				@Override
				public void run(){
					setName("transfer sender");
					while(isConnected()){
						try {
							int readed = dis.read(buffer, 0, 0x800);
							if(readed == -1){
								files_sended++;
								switchAndPrepare();
								return;
							}
							current_file_sended += readed;
							total_sended += readed;
							if(stamp != timestamp) return;
							byte[] data = ByteBuffer.normalizeBytes(buffer, readed);
							if(stamp != timestamp) return;
							packAndSend(data, readed);
							if(stamp != timestamp) return;
						} catch (IOException e) {
							e.printStackTrace();
							//removeTransferFromProfile();
							break;
						}
					}
				}
			};
			sender.start();
		} catch (Exception e) {
			e.printStackTrace();
			removeTransferFromProfile();
			return;
		}
	}
	private void sendEmptyDataAndSwitchToNext(){
		byte[] empty_data = new byte[0];
		send(BimoidProtocol.createDIR_PROX_FILE_DATA(sequence, contact.getProfile().ID, unique_id, current_file_index >= files_count, true, empty_data, 0));
		switchAndPrepare();
	}
	private void packAndSend(byte[] data, int length){
		send(BimoidProtocol.createDIR_PROX_FILE_DATA(sequence, contact.getProfile().ID, unique_id, current_file_index >= files_count, (current_file_size-current_file_sended+0x800)<=0x800, data, length));
		updatePercentage();
	}
	private void send(ByteBuffer data){
		switch(socket_mode){
		case MODE_NORMAL:
			srv_sck.write(data);
			break;
		case MODE_REVERSED:
			cli_sck.write(data);
			break;
		case MODE_PROXY:
			prx_sck.write(data);
			break;
		}
		ByteCache.recycle(data.bytes);
		sequence++;
	}
	public void updatePercentage(){
		int old = percentage;
		if(current_file_size > 0){
			percentage = (int)(current_file_sended*100/current_file_size);
		}else{
			percentage = 0;
		}
		if(old != percentage)
			updateChat();
	}
	public int getMode(){
		return socket_mode;
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
	public boolean canceled(){
		return canceled;
	}
	public int getFilesCount(){
		return files_count;
	}
	public int getSendingFileIndex(){
		return files_sended;
	}
	public String getProcessingFileName(){
		return current_file_name;
	}
	public long getProcessingSize(){
		return current_file_size;
	}
	public long getTotalSize(){
		return total_size;
	}
	public long getTotalSended(){
		return total_sended;
	}
	public view_container getContainer(){
		return transfer_message;
	}
	public void setRemoteAddress(String host, int port){
		remote_host = host;
		remote_port = port;
	}
	public void setProxyAddress(String host, int port){
		proxy_host = host;
		proxy_port = port;
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
				progress.setVisibility(View.GONE);
				view.setText(Locale.getString("s_file_send_initializing"));
				view.relayout();
				break;
			case FileReceiver.STATE_CONNECTING:
				progress.setVisibility(View.GONE);
				switch(getMode()){
				case FileReceiver.MODE_NORMAL:
					view.setText(Locale.getString("s_file_transfer_label_1"));
					break;
				case FileReceiver.MODE_REVERSED:
					view.setText(Locale.getString("s_file_transfer_label_2.2"));
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
				progress.setProgress(getTotalSended());
				view.setText(utilities.match(Locale.getString("s_file_sending"), new String[]{String.valueOf(getSendingFileIndex()+1), String.valueOf(getFilesCount()), getProcessingFileName(), String.valueOf(getPercentage()), String.valueOf(getProcessingSize())}));
				view.relayout();
				break;
			case FileReceiver.STATE_RECEIVED:
				progress.setVisibility(View.GONE);
				transfer_buttons.setVisibility(View.GONE);
				if(getFilesCount() > 1){
					view.setText(Locale.getString("s_files_sended"));
				}else{
					view.setText(utilities.match(Locale.getString("s_file_sended"), new String[]{getProcessingFileName()}));
				}
				view.relayout();
				//hst.deattachTransfer();
				break;
			case FileReceiver.STATE_ERROR:
				progress.setVisibility(View.GONE);
				transfer_buttons.setVisibility(View.GONE);
				if(canceled()){
					view.setText(Locale.getString("s_file_sending_canceled"));
				}else{
					view.setText(Locale.getString("s_file_sending_error"));
				}
				view.relayout();
				//hst.deattachTransfer();
				break;
			}
			((LinearLayout)view.getParent()).invalidate();
		}
	}
}
