package ru.ivansuper.bimoidproto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import android.app.Service;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import ru.ivansuper.bimoidim.ChatActivity;
import ru.ivansuper.bimoidim.ContactsAdapter;
import ru.ivansuper.bimoidim.Media;
import ru.ivansuper.bimoidim.PreferenceTable;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidim.utilities;
import ru.ivansuper.bimoidproto.filetransfer.FileReceiver;
import ru.ivansuper.bimoidproto.filetransfer.FileSender;
import ru.ivansuper.bimoidproto.filetransfer.FileTransfer;
import ru.ivansuper.bimoidproto.transports.Transport;
import ru.ivansuper.bimoidproto.transports.TransportAccount;
import ru.ivansuper.bimoidproto.transports.TransportParams;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.socket.ByteBuffer;
import ru.ivansuper.socket.ByteCache;
import ru.ivansuper.socket.ClientSocketConnection;

public class BimoidProfile {
	public static final byte CONN_STUDY_1 = 0;
	public static final byte CONN_STUDY_2 = 1;
	public static final byte CONN_STUDY_3 = 2;
	public static final byte CONN_STUDY_4 = 3;
	public static final byte CONN_STUDY_5 = 4;
	public static final byte CONN_STUDY_6 = 5;
	public static final byte CONN_STUDY_7 = 6;
	public String ID = "";
	public String nickname = "";
	public String password = "";
	public byte connection_status = 0;
	public BimoidService svc;
	private ClientSocketConnection socket;
	private int sequence = 0;
	public boolean connected;
	public boolean connecting;
	public Vector<RosterItem> contacts = new Vector<RosterItem>();
	private int status = -1;
	private int extended_status = 0;
	private String sts_desc = "";
	private String ext_sts_desc = "";
	private Vector<HistoryItem> messages_for_confirming = new Vector<HistoryItem>();
	private File roster_database;
	public File profile_dir;
	public boolean expanded_in_contact_list = true;
	public String server = "bimoid.net";
	public TransferParams ft_params = new TransferParams();
	public Vector<FileTransfer> transfers = new Vector<FileTransfer>();
	private Vector<RosterOperation> operations = new Vector<RosterOperation>();
	public Vector<TransportParams> transport_params = new Vector<TransportParams>();
	private reconnector Reconnector = new reconnector();
	public int max_transports;
	public BanList banlist = new BanList();
	private ping_thread PING_THREAD;
	public BimoidProfile(final BimoidService svc, String id, String password){
		this.svc = svc;
		String[] params = id.split("@");
		ID = params[0];
		server = params[1];
		nickname = ID;
		this.password = password;
		extended_status = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getInt(ID+"_ext_idx", 0);
		ext_sts_desc = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getString(ID+"_ext_desc_"+extended_status, "");		
		socket = new ClientSocketConnection(svc){
			@Override
			public void onRawData(ByteBuffer data) {
				handleRawData(data);
			}
			@Override
			public void onConnect() {
				handleConnectedToServer();
			}
			@Override
			public void onConnecting() {
			}
			@Override
			public void onDisconnect() {
				handleDisconnected();
			}
			@Override
			public void onLostConnection() {
				handleConnectionLosted();
			}
			@Override
			public void onError(int errorCode) {
				switch(errorCode){
				case 1:
					svc.showDialogInContactList(ID, Locale.getString("s_socket_error_1"));
					break;
				case 2:
					svc.showDialogInContactList(ID, Locale.getString("s_socket_error_2"));
					break;
				case 3:
					//svc.showDialogInContactList(ID+": Сеть", "Связь потеряна");
					break;
				case 4:
					svc.showDialogInContactList(Locale.getString("s_information"), utilities.match(Locale.getString("s_app_error"), new String[]{ID}));
					break;
				}
			}
			@Override
			public void onSocketCreated() {
			}
		};
		profile_dir = new File(resources.DATA_PATH+ID+"/");
		if(!profile_dir.exists())
			try {
				profile_dir.mkdir();
			} catch (Exception e) {
				Log.e("ROSTER", "Can't create profile directory!");
				//e.printStackTrace();
			}
		roster_database = new File(resources.DATA_PATH+ID+"/roster.bin");
		if(roster_database.exists())
			try {
				roster_database.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		TransportParams.load(this);
		try {
			loadRoster();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("ROSTER", "Can't read roster local copy!");
		}
		sortContactList();
	}
	private void updateConnStatus(byte status){
		this.connection_status = status;
		svc.handleProfileStatusChanged();
	}
	private void sortContactList(){
		synchronized(ContactsAdapter.LOCKER){
			//Vector<RosterItem> notes_ = new Vector<RosterItem>();
			Vector<RosterItem> contacts_ = new Vector<RosterItem>();
			Vector<RosterItem> groups_ = new Vector<RosterItem>();
			//Log.i("ROSTER", String.valueOf(contacts.size()));
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				item.level = 0;
				if(item.type == RosterItem.OBIMP_CONTACT){
					contacts_.add(contacts.get(i));
					//Log.i("ROSTER", "Contact filtered");
				}
				if(item.type == RosterItem.TRANSPORT_ITEM){
					contacts_.add(contacts.get(i));
					//Log.i("ROSTER", "Contact filtered");
				}
				if(item.type == RosterItem.CL_ITEM_TYPE_NOTE){
					contacts_.add(contacts.get(i));
					//Log.e("ROSTER1", "Note filtered");
				}
				if(item.type == RosterItem.OBIMP_GROUP){
					groups_.add(contacts.get(i));
					//Log.i("ROSTER", "Group filtered");
				}
			}
			Collections.sort(groups_);
			Collections.sort(contacts_);
			contacts.clear();
			if(groups_.size() == 0){
				//Log.i("ROSTER", "There is no groups");
				contacts.addAll(contacts_);
				//Log.e("ROSTER", "Notes count: "+notes_.size());
				//contacts.addAll(notes_);
				return;
			}
			sortGroups(groups_, contacts_);
			contacts.addAll(groups_);
			contacts.addAll(contacts_);
			//Log.e("ROSTER", "Notes count: "+notes_.size());
			//contacts.addAll(notes_);
			groups_ = null;
			contacts_ = null;
			//notes_ = null;
			//for(int i=0; i<contacts.size(); i++){
			//	Log.e("Sorted", contacts.get(i).name);
			//}
		}
	}
	private void sortGroups(Vector<RosterItem> groups_, Vector<RosterItem> contacts_){
		Vector<RosterItem> groups = new Vector<RosterItem>();
		for(int i=0; i<groups_.size(); i++){
			Group item = (Group)groups_.get(i);
			if(item.getGroupId() == 0){
				groups.add(item);
				groups_.removeElementAt(i);
				i--;
			}
		}
		for(int i=0; i<groups.size(); i++){
			RosterItem itm = groups.get(i);
			if(itm.type != RosterItem.OBIMP_GROUP) continue;
			Group item = (Group)itm;
			Vector<RosterItem> gps = getChildGroups(item.getItemId(), groups_, item.level);
			Vector<RosterItem> cnts = getChildContacts(((Group)item).getItemId(), contacts_, item.level);
			groups.addAll(i+1, gps);
			groups.addAll(i+1+gps.size(), cnts);
		}
		groups_.clear();
		groups_.addAll(groups);
	}
	private Vector<RosterItem> getChildGroups(int id, Vector<RosterItem> groups, int parent_level){
		Vector<RosterItem> list = new Vector<RosterItem>();
		for(int i=0; i<groups.size(); i++){
			RosterItem item = groups.get(i);
			if(item.type == RosterItem.OBIMP_GROUP){
				if(((Group)item).getGroupId() == id){
					item.level = parent_level+1;
					list.add((Group)item);
				}
			}
		}
		//Collections.sort(list);
		return list;
	}
	private Vector<RosterItem> getChildContacts(int id, Vector<RosterItem> contacts_, int parent_level){
		final Vector<RosterItem> list = new Vector<RosterItem>();
		for(int i=0; i<contacts_.size(); i++){
			RosterItem item = contacts_.get(i);
			if(item.type == RosterItem.OBIMP_CONTACT
					|| item.type == RosterItem.TRANSPORT_ITEM
					|| item.type == RosterItem.CL_ITEM_TYPE_NOTE){
				if(item.getGroupId() == id){
					item.level = parent_level+1;
					list.add(item);
					contacts_.removeElementAt(i);
					i--;
				}
			}
		}
		//Collections.sort(list);
		return list;
	}
	public Vector<RosterItem> getContacts(){
		final Vector<RosterItem> list = new Vector<RosterItem>();
		for(int i=0; i<contacts.size(); i++){
			RosterItem item = contacts.get(i);
			if(item.type == RosterItem.OBIMP_CONTACT){
				list.add(item);
			}
			if(item.type == RosterItem.TRANSPORT_ITEM){
				list.add(item);
			}
			if(item.type == RosterItem.CL_ITEM_TYPE_NOTE){
				list.add(item);
			}
		}
		Collections.sort(list);
		return list;
	}
	private void handleRawData(ByteBuffer data){
		if(PING_THREAD != null && connected) PING_THREAD.resetTimer();
		BEX bex = new BEX(data);
		handleBEX(bex);
	}
	private void handleBEX(BEX bex){
		//Log.d("Profile BEX", "Type: "+String.valueOf(bex.getType())+"   SubType: "+String.valueOf(bex.getSubType()));
		switch(bex.getType()){
		case 0x1:
			switch(bex.getSubType()){
			case 0x2:
				handleServerHelloReply(bex);
				updateConnStatus(CONN_STUDY_3);
				break;
			case 0x4:
				handleServerLoginReply(bex);
				updateConnStatus(CONN_STUDY_5);
				break;
			case 0x5:
				handleServerBye(bex);
				break;
			case 0x6:
				handleServerPING();
				break;
			case 0x7:
				//Log.e("PONG", "*");
				break;
			}
			break;
		case 0x2:
			switch(bex.getSubType()){
			case 0x2:
				handleCL_PARAMS(bex);
				break;
			case 0x4:
				updateConnStatus(CONN_STUDY_7);
				handleServerRoster(bex);
				break;
			case 0x11:
				send(BEX.createEmptyBex(sequence, 2, 0x12, 0));//DEL_OFFAUTH
				break;
			case 0x13:
				handleServerItemOperation(bex);
				break;
			case 0xD:
				handleAuthRequest(bex);
				break;
			case 0xE:
				handleAuthReply(bex);
				break;
			case 0xF:
				handleAuthRevoke(bex);
				break;
			case 0x8:
				handleRosterOperationResult(bex);
				break;
			case 0xA:
				handleRosterOperationResult(bex);
				break;
			case 0xC:
				handleRosterOperationResult(bex);
				break;
			case 0x14:
				handleServerBeginUpdate();
				break;
			case 0x15:
				handleServerEndUpdate();
				break;
			}
			break;
		case 0x3:
			switch(bex.getSubType()){
			case 0x6:
				handleServerUserOnline(bex);
				break;
			case 0x7:
				handleServerUserOffline(bex);
				break;
			}
			break;
		case 0x4:
			switch(bex.getSubType()){
			case 0x2:
				handleIM_PARAMS(bex);
				break;
			case 0x4:
				handleServerDoneOffline();
				break;
			case 0x7:
				handleServerMessage(bex);
				break;
			case 0x8:
				handleMessageReport(bex);
				break;
			case 0x9:
				handleTypingNotify(bex);
				break;
			}
			break;
		case 0x5:
			switch(bex.getSubType()){
			case 0x4:
				if(bex.getID() == 1){
					handleProfileDetails(bex);
				}else if(bex.getID() == 2){
					handleUserDetails(bex);
				}
				break;
			case 0x8:
				handleSearchResult(bex);
				break;
			}
			break;
		case 0x7:
			switch(bex.getSubType()){
			case 0x2:
				handleFT_PARAMS(bex);
				break;
			case 0x3:
				handleServerIncomingFileAsk(bex);
				break;
			case 0x4:
				handleFileReply(bex);
				break;
			case 0x5:
				handleFTControl(bex);
				break;
			}
			break;
		case 0x8:
			switch(bex.getSubType()){
			case 0x2:
				handleTPParams(bex);
				break;
			case 0x3:
				handleTransportReady(bex);
				break;
			case 0x5:
				handleTransportSettingsUpdateReply(bex);
				break;
			case 0x7:
				handleTransportInfo(bex);
				break;
			case 0x8:
				handleTransportPopup(bex);
				break;
			}
		}
	}
	private void handleServerPING(){
		send(BEX.createEmptyBex(sequence, 1, 7, 0));
	}
	private void handleConnectedToServer(){
		updateConnStatus(CONN_STUDY_2);
		try {
			send(BimoidProtocol.createClientHello(sequence, ID));
		} catch (Exception e) {
			socket.disconnect();
			e.printStackTrace();
		}
	}
	private void handleServerBye(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD error = list.getTLD(0x1);
		if(error != null){
			int reason = error.getData().readWord();
			switch(reason){
			case 0x1:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_1"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- SRV_SHUTDOWN");
				break;
			case 0x2:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_2"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- CLI_NEW_LOGIN");
				break;
			case 0x3:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_3"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- ACCOUNT_KICKED");
				break;
			case 0x4:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_4"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- INCORRECT_SEQ");
				break;
			case 0x5:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_5"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- INCORRECT_BEX_TYPE");
				break;
			case 0x6:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_6"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- INCORRECT_BEX_SUB");
				break;
			case 0x7:
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- INCORRECT_BEX_STEP");
				break;
			case 0x8:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_7"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- TIMEOUT");
				break;
			case 0x9:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_8"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- INCORRECT_WTLD");
				break;
			case 0xA:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_9"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- NOT_ALLOWED");
				break;
			case 0xB:
				svc.showDialogInContactList(ID, Locale.getString("s_server_bye_reason_10"));
				Log.e("BimoidProfile", ID+": Disconnected by server. Reason -- FLOODING");
				break;
			}
		}
		svc.doVibrate(300);
		socket.disconnect();
	}
	private void handleServerHelloReply(BEX bex) {
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD error = list.getTLD(0x1);
		if(error != null){
			int code = error.getData().readWord();
			switch(code){
			case 0x1:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_1"));
				Log.e("BimoidProfile", ID+": Login error -- ACCOUNT_INVALID");
				break;
			case 0x2:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_2"));
				Log.e("BimoidProfile", ID+": Login error -- SERVICE_TEMP_UNAVAILABLE");
				break;
			case 0x3:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_3"));
				Log.e("BimoidProfile", ID+": Login error -- ACCOUNT_BANNED");
				break;
			case 0x4:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_4"));
				Log.e("BimoidProfile", ID+": Login error -- WRONG_COOKIE");
				break;
			case 0x5:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_5"));
				Log.e("BimoidProfile", ID+": Login error -- TOO_MANY_CLIENTS");
				break;
			case 0x6:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_6"));
				Log.e("BimoidProfile", ID+": Login error -- INVALID_LOGIN");
				break;
			}
			return;
		}
		wTLD redirect = list.getTLD(0x3);
		if(redirect != null){
			socket.disconnect();
			String server = redirect.getData().readStringUTF8(redirect.getLength());
			wTLD portt = list.getTLD(0x4);
			int port = portt.getData().readDWord();
			//Log.e("BimoidProfile", ID+": Jumping to another server -- "+server+":"+String.valueOf(port));
			socket.connect(server, port);
			return;
		}
		wTLD login_code = list.getTLD(0x2);
		if(login_code != null){
			String code = login_code.getData().readStringUTF8(login_code.getLength());
			proceedMD5Login(code);
			return;
		}
		wTLD plain_text_login = list.getTLD(0x3);
		if(plain_text_login != null){
			proceedPlainTextLogin();
			return;
		}
		Log.e("BimoidProfile", ID+": Invalid server reply");
		socket.disconnect();
	}
	private void proceedMD5Login(String key){
		try {
			send(BimoidProtocol.createMD5Login(sequence, ID, password, key));
		} catch (Exception e) {
			Log.e("BimoidProfile", ID+": Login error -- Can't create MD5 packet for login");
			socket.disconnect();
			//e.printStackTrace();
		}
	}
	private void proceedPlainTextLogin(){
		try {
			send(BimoidProtocol.createPlainTextLogin(sequence, ID, password));
		} catch (Exception e) {
			Log.e("BimoidProfile", ID+": Login error -- Can't create plain-text password packet for login");
			socket.disconnect();
			//e.printStackTrace();
		}
	}
	private void handleServerLoginReply(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD error = list.getTLD(0x1);
		if(error != null){
			int code = error.getData().readWord();
			switch(code){
			case 0x1:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_1"));
				Log.e("BimoidProfile", ID+": Login error in server reply -- ACCOUNT_INVALID");
				break;
			case 0x2:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_2"));
				Log.e("BimoidProfile", ID+": Login error in server reply -- SERVICE_TEMP_UNAVAILABLE");
				break;
			case 0x3:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_3"));
				Log.e("BimoidProfile", ID+": Login error in server reply -- ACCOUNT_BANNED");
				break;
			case 0x4:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_7"));
				Log.e("BimoidProfile", ID+": Login error in server reply -- WRONG_PASSWORD");
				break;
			case 0x5:
				svc.showDialogInContactList(ID, Locale.getString("s_hello_reason_6"));
				Log.e("BimoidProfile", ID+": Login error in server reply -- INVALID_LOGIN");
				break;
			}
			socket.disconnect();
			return;
		}
		//Log.i("BimoidProfile", ID+": Login successful!");
		doRequestParams();
		getRoster();
		send(BimoidProtocol.createSetCaps(sequence));
	}
	private void doRequestParams(){
		updateConnStatus(CONN_STUDY_4);
		send(BEX.createEmptyBex(sequence, 2, 1, 0));//CL_PARAMS
		send(BEX.createEmptyBex(sequence, 4, 1, 0));//IM_PARAMS
		send(BEX.createEmptyBex(sequence, 7, 1, 0));//FT_PARAMS
		send(BEX.createEmptyBex(sequence, 8, 1, 0));//TP_PARAMS
	}
	public TransportParams getTransportParamsByUUID(String UUID){
		for(int i=0; i<transport_params.size(); i++){
			final TransportParams params = transport_params.get(i);
			if(params.UUID.equals(UUID)) return params;
		}
		return null;
	}
	public Transport getTransportByID(int ID){
		for(RosterItem item: contacts){
			if(item.type != RosterItem.TRANSPORT_ITEM) continue;
			Transport transport = (Transport)item;
			if(transport.getItemId() == ID) return transport;
		}
		return null;
	}
	public int countTransports(){
		int count = 0;
		for(RosterItem item: contacts){
			if(item.type != RosterItem.TRANSPORT_ITEM) continue;
			count++;
		}
		return count;
	}
	public Transport removeTransportByID(int ID){
		synchronized(ContactsAdapter.LOCKER){
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				if(item.type != RosterItem.TRANSPORT_ITEM) continue;
				Transport transport = (Transport)item;
				if(transport.getItemId() == ID){
					contacts.remove(i);
					return transport;
				}
			}
			return null;
		}
	}
	public void setTransportStatus(Transport t, int status, int code, int xstatus){
		t.setStatus(status);
		userSend(BimoidProtocol.createTP_CLI_MANAGE(sequence, t.item_id, code, status, xstatus, ""));
	}
	private void handleTransportReady(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		final int transport_id = tld.getData().readDWord();
		Log.e("BimoidProfile", "Transport "+transport_id+" ready");
		Transport t = getTransportByID(transport_id);
		if(t == null) return;
		t.ready = true;
		TransportAccount a = getTransportAccount(t.account_name);
		if(a == null){
			//t.account_name = "";
			t.account_pass = "";
			t.account_server = t.params.default_host;
			t.account_port = t.params.default_port;
			saveTransportAccount(t);
			svc.handleContactListNeedRebuild();
			return;
		}else{
			t.account_pass = a.pass;
			t.account_server = a.server;
			t.account_port = a.port;
		}
		userSend(BimoidProtocol.createTP_CLI_SETTINGS(sequence, transport_id, a.pass, t.params.default_host, t.params.default_port, 1));
		svc.handleContactListNeedRebuild();
	}
	public void updateTransportParams(Transport t){
		userSend(BimoidProtocol.createTP_CLI_SETTINGS(sequence, t.item_id, t.account_pass, t.account_server, t.account_port, 0));
		svc.handleContactListNeedRebuild();
	}
	private void handleTransportPopup(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		int tid = tld.getData().readDWord();
		Transport t = getTransportByID(tid);
		if(t == null) return;
		tld = list.getTLD(0x2);
		boolean autoclose = tld.getData().readBoolean();
		tld = list.getTLD(0x3);
		boolean warning = tld.getData().readBoolean();
		tld = list.getTLD(0x2);
		String header = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x3);
		String message = tld.getData().readStringUTF8(tld.getLength());
		svc.showDialogInContactList(warning? "[!] "+header: header, message);
	}
	private void handleTransportInfo(BEX bex){
		final int TP_STATE_LOGGEDON = 0x0000;
		final int TP_STATE_LOGGEDOFF = 0x0001;
		final int TP_STATE_STATUS_CHANGED = 0x0002;
		final int TP_STATE_CON_FAILED = 0x0003;
		final int TP_STATE_ACCOUNT_INVALID = 0x0004;
		final int TP_STATE_SERVICE_TEMP_UNAVAILABLE = 0x0005;
		final int TP_STATE_WRONG_PASSWORD = 0x0006;
		final int TP_STATE_INVALID_LOGIN = 0x0007;
		final int TP_STATE_OTHER_PLACE_LOGIN = 0x0008;
		final int TP_STATE_CANT_LOGIN_TRY_LATER = 0x0009;
		final int TP_STATE_SRV_PAUSED = 0x000A;
		final int TP_STATE_SRV_RESUMED = 0x000B;
		final int TP_STATE_SRV_MIGRATED = 0x000C;
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		int tid = tld.getData().readDWord();
		tld = list.getTLD(0x2);
		int code = tld.getData().readWord();
		Transport t = getTransportByID(tid);
		if(t == null) return;
		switch(code){
		case TP_STATE_LOGGEDON:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_1"), Toast.LENGTH_SHORT);
			t.connected = true;
			break;
		case TP_STATE_LOGGEDOFF:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_2"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_STATUS_CHANGED:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_3"), Toast.LENGTH_SHORT);
			break;
		case TP_STATE_CON_FAILED:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_4"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_ACCOUNT_INVALID:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_5"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_SERVICE_TEMP_UNAVAILABLE:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_6"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_WRONG_PASSWORD:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_7"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_INVALID_LOGIN:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_8"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_OTHER_PLACE_LOGIN:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_9"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_CANT_LOGIN_TRY_LATER:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_10"), Toast.LENGTH_SHORT);
			t.setStatus(-1);
			t.connected = false;
			setAllTransportContactsOffline(tid);
			break;
		case TP_STATE_SRV_PAUSED:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_11"), Toast.LENGTH_SHORT);
			break;
		case TP_STATE_SRV_RESUMED:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_12"), Toast.LENGTH_SHORT);
			break;
		case TP_STATE_SRV_MIGRATED:
			svc.showToast(t.params.name_of_account_ids+" "+t.account_name+": "+Locale.getString("s_transport_state_13"), Toast.LENGTH_SHORT);
			break;
		}
		svc.handleContactListNeedRebuild();
	}
	private void handleTransportSettingsUpdateReply(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x2);
		final int result = tld.getData().readWord();
		if(result == 0){
			//Log.e("BimoidProfile", "Transport updated successful");
			tld = list.getTLD(0x1);
			if(bex.getID() == 0) svc.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_transport_updated"));
			final int transport_id = tld.getData().readDWord();
			final Transport transport = getTransportByID(transport_id);
			if(transport != null){
				if(transport.ready && transport.getRememberedStatus() != -1){
					setTransportStatus(transport, transport.getRememberedStatus(), 1, transport.extended_status);
				}
			}
			//Log.e("BimoidProfile", "Trying to connect transport #"+transport_id);
		}else{
			if(bex.getID() == 0) svc.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_transport_update_error"));
		}
	}
	private void handleTPParams(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		max_transports = tld.getData().readDWord();
		tld = list.getTLD(0x2);
		final ByteBuffer transports = tld.getData();
		final int count = transports.readDWord();
		boolean exist = true;
		boolean dont_add_params = false;
		for(int i=0; i<count; i++){
			final String UUID = utilities.convertToHex(transports.readBytes(16));
			TransportParams params = getTransportParamsByUUID(UUID);
			if(params == null){
				params = new TransportParams();
				exist = false;
			}
			params.UUID = UUID;
			final int length = transports.readDWord();
			sTLDList values = new sTLDList(transports, length);
			sTLD stld = values.getTLD(0x1);
			params.full_name = stld.getData().readStringUTF8(stld.getLength());
			stld = values.getTLD(0x2);
			params.short_name = stld.getData().readStringUTF8(stld.getLength());
			stld = values.getTLD(0x3);
			params.name_of_account_ids = stld.getData().readStringUTF8(stld.getLength());
			stld = values.getTLD(0x4);
			params.default_host = stld.getData().readStringUTF8(stld.getLength());
			stld = values.getTLD(0x5);
			params.default_port = stld.getData().readDWord();
			stld = values.getTLD(0x6);
			final int[] statuses = new int[stld.getLength()/4];
			final ByteBuffer statuses_list = stld.getData();
			for(int j=0; j<statuses.length; j++)
				statuses[j] = statuses_list.readDWord();
			params.status_wrapper = statuses;
			stld = values.getTLD(0x7);
			params.additional_status_pic = stld.getData().readBoolean();
			stld = values.getTLD(0x8);
			params.additional_status_pic_count = stld.getData().readByte();
			stld = values.getTLD(0x9);
			params.add_contacts = stld.getData().readBoolean();
			stld = values.getTLD(0xA);
			params.update_contacts = stld.getData().readBoolean();
			stld = values.getTLD(0xB);
			params.delete_contacts = stld.getData().readBoolean();
			stld = values.getTLD(0xC);
			params.visible_list = stld.getData().readBoolean();
			stld = values.getTLD(0xD);
			params.invisible_list = stld.getData().readBoolean();
			stld = values.getTLD(0xE);
			params.ignore_list = stld.getData().readBoolean();
			stld = values.getTLD(0xF);
			params.move_to_ignore = stld.getData().readBoolean();
			stld = values.getTLD(0x10);
			params.auth_supported = stld.getData().readBoolean();
			stld = values.getTLD(0x11);
			params.auth_revoke = stld.getData().readBoolean();
			stld = values.getTLD(0x12);
			params.message_ack = stld.getData().readBoolean();
			stld = values.getTLD(0x13);
			params.notification_messages = stld.getData().readBoolean();
			stld = values.getTLD(0x14);
			params.detail_req = stld.getData().readBoolean();
			stld = values.getTLD(0x15);
			params.update_avatar = stld.getData().readBoolean();
			stld = values.getTLD(0x16);
			params.search = stld.getData().readBoolean();
			stld = values.getTLD(0x17);
			params.avatars = stld.getData().readBoolean();
			stld = values.getTLD(0x18);
			params.update_avatar = stld.getData().readBoolean();
			stld = values.getTLD(0x19);
			params.offline_messages = stld.getData().readBoolean();
			stld = values.getTLD(0x1A);
			params.presence_info_req = stld.getData().readBoolean();
			if(params.main_status_list == null || params.main_status_list.getHeight() != TransportParams.getPreferedSize()){
				stld = values.getTLD(0x81);
				params.main_status_list = TransportParams.getBitmap(stld.getData().readStringUTF8(stld.getLength()));
			}
			if(params.main_status_list == null) dont_add_params = true;
			if(params.additional_status_list == null || params.main_status_list.getHeight() != TransportParams.getPreferedSize()){
				stld = values.getTLD(0x82);
				params.additional_status_list = TransportParams.getBitmap(stld.getData().readStringUTF8(stld.getLength()));
			}
			if(params.additional_status_list == null) dont_add_params = true;
			if(!exist) if(!dont_add_params){
				transport_params.add(params);
				params.save(this);
			}
			//Log.e("BimoidProfile", UUID+": params parsed successful (added: "+!dont_add_params+")");
		}
	}
	private void getRoster(){
		send(BEX.createEmptyBex(sequence, 0x2, 0x3, 0x0));
	}
	private void handleServerBeginUpdate(){
		svc.showProgressInContactList(Locale.getString("s_please_wait_for_operation_end"));
	}
	private void handleServerEndUpdate(){
		svc.hideProgressInContactList();
	}
	private void handleServerRoster(BEX bex){
		Reconnector.stop();
		//Log.i("ROSTER", "Roster received");
		Vector<RosterItem> temporary_contacts = getTemporaryContacts();
		wTLDList list_ = new wTLDList(bex.getData(), bex.getLength());
		wTLD roster = list_.getTLD(0x1);
		synchronized(ContactsAdapter.LOCKER){
			label:
			if(roster.getType() == 0x1){
					contacts.clear();
					ByteBuffer tld = roster.getData();
					int items_count = tld.readDWord();
					if(items_count == 0) break label;
					//Log.i("RosterParser", "Items count: "+String.valueOf(items_count));
					for(int i=0; i<items_count; i++){
						int type = tld.readWord();
						int id = tld.readDWord();
						int group_id = tld.readDWord();
						int tlds_length = tld.readDWord();
						//Log.i("RosterParser", "Type: "+String.valueOf(type)+" "+
						//		"ID: "+String.valueOf(id)+" "+
						//		"Group_ID: "+String.valueOf(group_id)+" "+
						//		"Length: "+String.valueOf(tlds_length)+" ");
						sTLDList list = new sTLDList(tld, tlds_length);
						sTLD stld;
						ByteBuffer sdata;
						switch(type){
						case RosterItem.OBIMP_CONTACT:
							stld = list.getTLD(0x2);
							sdata = stld.getData();
							String account = sdata.readStringUTF8(stld.getLength());
							stld = list.getTLD(0x3);
							sdata = stld.getData();
							String name = sdata.readStringUTF8(stld.getLength());
							stld = list.getTLD(0x4);
							sdata = stld.getData();
							int privacy = sdata.readByte();
							stld = list.getTLD(0x1001);
							int transport_id = -1;
							if(stld != null){
								sdata = stld.getData();
								transport_id = sdata.readDWord();
							}
							Contact contact = new Contact(account, name, group_id, id, privacy,
									(list.getTLD(0x5) != null), (list.getTLD(0x6) != null), this);
							//Log.i("RosterParser", "Contact: "+account+"/"+name);
							contact.setTransportId(transport_id);
							contacts.add(contact);
							break;
						case RosterItem.OBIMP_GROUP:
							stld = list.getTLD(0x1);
							sdata = stld.getData();
							String group_name = sdata.readStringUTF8(stld.getLength());
							Group group = new Group(group_name, id, group_id, this);
							//Log.i("RosterParser", "Group: "+group_name);
							contacts.add(group);
							break;
						case RosterItem.TRANSPORT_ITEM:
							stld = list.getTLD(0x1002);
							sdata = stld.getData();
							String UUID = utilities.convertToHex(sdata.readBytes(stld.getLength()));
							stld = list.getTLD(0x1003);
							sdata = stld.getData();
							String account_name = sdata.readStringUTF8(stld.getLength());
							stld = list.getTLD(0x1004);
							sdata = stld.getData();
							String friendly_name = sdata.readStringUTF8(stld.getLength());
							Transport transport = new Transport(this, account_name, UUID);
							transport.account_name = account_name;
							transport.name = friendly_name;
							transport.UUID = UUID;
							transport.item_id = id;
							transport.group_id = group_id;
							transport.profile = this;
							transport.params = getTransportParamsByUUID(UUID);
							//Log.e("ParsingRoster", "Transport found. Name: "+friendly_name);
							contacts.add(transport);
							break;
						case RosterItem.CL_ITEM_TYPE_NOTE:
							stld = list.getTLD(0x2001);
							String NAME = stld.getData().readStringUTF8(stld.getLength());
							stld = list.getTLD(0x2002);
							byte TYPE = stld.getData().readByte();
							stld = list.getTLD(0x2003);
							String TEXT = "";
							if(stld != null) TEXT = stld.getData().readStringUTF8(stld.getLength());
							stld = list.getTLD(0x2004);
							long TIMESTAMP = 0;
							if(stld != null) TIMESTAMP = stld.getData().readUNIXEpochTimeStampNoCorrection();
							byte[] HASH = null;
							stld = list.getTLD(0x2005);
							if(stld != null){
								HASH = stld.getData().readBytes(stld.getLength());
							}
							NoteItem note_item = new NoteItem();
							note_item.profile = this;
							note_item.setItemId(id);
							note_item.setRosterId(group_id);
							note_item.name = NAME;
							note_item.TYPE = TYPE;
							note_item.TEXT = TEXT;
							note_item.TIMESTAMP = TIMESTAMP;
							note_item.HASH = HASH;
							//Log.e("BimoidProfile", "Parsing note item: "+note_item.name);
							contacts.add(note_item);
							break;
						}
					}
			}
			//Log.i("ROSTER", "Roster parsed");
			svc.clearOpened();
			contacts.addAll(temporary_contacts);
			sortContactList();
		}
		try {
			saveRoster(roster_database);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("ROSTER", "Can't save roster!");
		}
		PING_THREAD = new ping_thread();
		PING_THREAD.setName(ID+":ping_thread");
		PING_THREAD.start();
		connecting = false;
		connected = true;
		svc.handleProfileStatusChanged();
		svc.handleContactListNeedRebuild();
		send(BimoidProtocol.createSetStatus(sequence, status, null, extended_status, ext_sts_desc));
		send(BimoidProtocol.createDetailsRequest(sequence, ID, 0x1));
		send(BEX.createEmptyBex(sequence, 3, 5, 0));//CLI_ACTIVATE
	}
	private void handleCL_PARAMS(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x9);
		if(tld != null){
			int offline_messages_count = tld.getData().readDWord();
			if(offline_messages_count > 0){
				send(BEX.createEmptyBex(sequence, 2, 0x10, 0));//OFFAUTH_REQ
			}
		}
	}
	private void handleAuthRequest(BEX bex){
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x1);
		String account = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1001);
		int tid = -1;
		if(tld != null) tid = tld.getData().readDWord();
		Contact contact = getContactById(account, tid);
		if(contact == null){
			if(AntispamBot.enabled()){
				sendRawMessage(account, tid, AntispamBot.getQuestion());
				return;
			}else{
				contact = createTemporaryContact(account, tid);
			}
		}
		tld = list.getTLD(0x2);
		String reason = "";
		if(tld == null){
			reason = Locale.getString("s_contact_ask_for_auth");
		}else{
			reason = tld.getData().readStringUTF8(tld.getLength());
		}
		HistoryItem hst = new HistoryItem();
		hst.contact = contact;
		hst.direction = HistoryItem.DIRECTION_INCOMING;
		hst.unique_id = (int)System.currentTimeMillis();
		hst.isAuthMessage = Contact.AUTH_REQ;
		hst.message = reason;
		contact.getHistoryObject().preloadCache();
		contact.getHistoryObject().putMessage(hst, false);
		contact.setHasAuth(1);
		contact.increaseUnreadMessages();
		if(!(utilities.contactEquals(svc.currentChatContact, contact) && ChatActivity.isAnyChatOpened)){
			svc.createAuthNotify(utilities.getHash(contact), R.drawable.auth_req, contact.getID(), reason);
		}
		svc.handleContactListNeedRebuild();
		svc.putIntoOpened(contact);
		svc.refreshChat();
		svc.doVibrate(150);
		svc.media.playEvent(Media.AUTH_REQUEST);
	}
	private void handleAuthReply(BEX bex){
		final int AUTH_REPLY_GRANTED = 0x0001;
		final int AUTH_REPLY_DENIED = 0x0002;
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		String account = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1001);
		int tid = -1;
		if(tld != null) tid = tld.getData().readDWord();
		Contact contact = getContactById(account, tid);
		if(contact == null){
			if(AntispamBot.enabled()){
				sendRawMessage(account, tid, AntispamBot.getQuestion());
				return;
			}else{
				contact = createTemporaryContact(account, tid);
			}
		}
		contact.getHistoryObject().preloadCache();
		tld = list.getTLD(0x2);
		int auth_code = tld.getData().readWord();
		switch(auth_code){
		case AUTH_REPLY_GRANTED:
			HistoryItem hst = new HistoryItem();
			hst.contact = contact;
			hst.direction = HistoryItem.DIRECTION_INCOMING;
			hst.unique_id = (int)System.currentTimeMillis();
			hst.isAuthMessage = Contact.AUTH_ACCEPTED;
			hst.message = Locale.getString("s_contact_accept_auth");
			contact.getHistoryObject().putMessage(hst, false);
			svc.media.playEvent(Media.AUTH_ACCEPTED);
			if(!(utilities.contactEquals(svc.currentChatContact, contact) && ChatActivity.isAnyChatOpened)){
				contact.auth_flag = false;
				contact.setHasAuth(Contact.AUTH_ACCEPTED);
				svc.createAuthNotify(utilities.getHash(contact), R.drawable.auth_acc, contact.getID(), hst.message);
			}
			try {
				saveRoster(roster_database);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case AUTH_REPLY_DENIED:
			hst = new HistoryItem();
			hst.contact = contact;
			hst.direction = HistoryItem.DIRECTION_INCOMING;
			hst.unique_id = (int)System.currentTimeMillis();
			hst.isAuthMessage = Contact.AUTH_REJECTED;
			hst.message = Locale.getString("s_contact_decline_auth");
			contact.getHistoryObject().putMessage(hst, false);
			svc.media.playEvent(Media.AUTH_DENIED);
			if(!(utilities.contactEquals(svc.currentChatContact, contact) && ChatActivity.isAnyChatOpened)){
				//contact.auth_flag = false;
				contact.setHasAuth(Contact.AUTH_REJECTED);
				svc.createAuthNotify(utilities.getHash(contact), R.drawable.auth_rej, contact.getID(), hst.message);
			}
			break;
		}
		if(svc.currentChatContact == null){
			contact.increaseUnreadMessages();
			svc.handleContactListNeedRebuild();
			return;
		}else{
			if(!(svc.currentChatContact.equals(contact) && ChatActivity.isAnyChatOpened)){
				contact.increaseUnreadMessages();
				svc.handleContactListNeedRebuild();
			}else{
				svc.cancelAuthNotify(utilities.getHash(contact));
				contact.setHasNoAuth();
				svc.handleContactListNeedRebuild();
			}
		}
		svc.putIntoOpened(contact);
		svc.refreshChat();
		svc.doVibrate(150);
		svc.handleContactListNeedRebuild();
	}
	private void handleAuthRevoke(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		String account = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1001);
		int tid = -1;
		if(tld != null) tid = tld.getData().readDWord();
		Contact contact = getContactById(account, tid);
		if(contact == null){
			if(AntispamBot.enabled()){
				sendRawMessage(account, tid, AntispamBot.getQuestion());
				return;
			}else{
				contact = createTemporaryContact(account, tid);
			}
		}
		contact.getHistoryObject().preloadCache();
		tld = list.getTLD(0x2);
		String reason = "";
		if(tld == null){
			reason = " - ";
		}else{
			reason = tld.getData().readStringUTF8(tld.getLength());
		}
		HistoryItem hst = new HistoryItem();
		hst.contact = contact;
		hst.direction = HistoryItem.DIRECTION_INCOMING;
		hst.unique_id = (int)System.currentTimeMillis();
		hst.isAuthMessage = Contact.AUTH_REJECTED;
		hst.message = utilities.match(Locale.getString("s_contact_revoke_auth"), new String[]{reason});
		contact.getHistoryObject().putMessage(hst, false);
		contact.increaseUnreadMessages();
		contact.setHasAuth(Contact.AUTH_REJECTED);
		contact.auth_flag = true;
		svc.createAuthNotify(contact.hashCode()-0xffff, R.drawable.auth_rej, contact.getID(), reason);
		svc.media.playEvent(Media.AUTH_DENIED);
		if(svc.currentChatContact == null){
			contact.increaseUnreadMessages();
			svc.handleContactListNeedRebuild();
			return;
		}else{
			if(!(svc.currentChatContact.equals(contact) && ChatActivity.isAnyChatOpened)){
				contact.increaseUnreadMessages();
				svc.handleContactListNeedRebuild();
			}else{
				svc.cancelAuthNotify(utilities.getHash(contact));
				contact.setHasNoAuth();
				svc.handleContactListNeedRebuild();
			}
		}
		try {
			saveRoster(roster_database);
		} catch (Exception e) {
			e.printStackTrace();
		}
		svc.refreshChat();
		svc.doVibrate(150);
		svc.handleContactListNeedRebuild();
	}
	private void handleServerUserOnline(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		String account = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1001);
		int tid = -1;
		if(tld != null) tid = tld.getData().readDWord();
		tld = list.getTLD(0x2);
		int status = tld.getData().readDWord();
		tld = list.getTLD(0x8);
		String client = null;
		if(tld != null){
			client = tld.getData().readStringUTF8(tld.getLength());
		}
		tld = list.getTLD(0x9);
		String client_version = null;
		if(tld != null){
			ByteBuffer ver = tld.getData();
			client_version = "";
			client_version += String.valueOf(ver.readWord())+".";
			client_version += String.valueOf(ver.readWord())+".";
			client_version += String.valueOf(ver.readWord())+".";
			client_version += String.valueOf(ver.readWord());
		}
		tld = list.getTLD(0x4);
		int additional_status = 0;
		if(tld != null)
			additional_status = tld.getData().readDWord();
		tld = list.getTLD(0x5);
		String additional_status_desc = "";
		if(tld != null)
			additional_status_desc = tld.getData().readStringUTF8(tld.getLength());
		Contact contact = getContactById(account, tid);
		if(contact == null) return;
		int old_status = contact.getStatus();
		contact.setStatus(status);
		//Log.e("Additional status", String.valueOf(additional_status));
		//Log.e("Additional status desc", additional_status_desc);
		contact.setExtendedStatus(additional_status);
		contact.setExtendedDescription(additional_status_desc);
		contact.setClient(client);
		contact.setClientVersionString(client_version);
		//Log.i("CONTACT_ONLINE", "Account: "+account+"   Status: "+String.valueOf(status));
		notifyContactStatusChanged(contact, old_status, status);
	}
	private void handleServerUserOffline(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		String account = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1001);
		int tid = -1;
		if(tld != null) tid = tld.getData().readDWord();
		Contact contact = getContactById(account, tid);
		if(contact == null) return;
		int old_status = contact.getStatus();
		contact.status = -1;
		//Log.i("CONTACT_OFFLINE", "Account: "+account);
		notifyContactStatusChanged(contact, old_status, -1);
	}
	private void notifyContactStatusChanged(Contact contact, int old_status, int status){
		if((old_status != -1) && (status != -1)){
			svc.handleContactListNeedRefresh();
		}else{
			svc.handleContactListNeedRebuild();
		}
		if((old_status < 0) && (status >= 0)){
			svc.media.playEvent(Media.CONTACT_IN);
		}else if((old_status >= 0) && (status < 0)){
			svc.media.playEvent(Media.CONTACT_OUT);
		}
		if(svc.currentChatContact == null) return;
		//if(svc.currentChatContact.equals(contact))
		svc.refreshChatUserInfo();
	}
	private void handleIM_PARAMS(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x3);
		if(tld == null) return;
		int offline_count = tld.getData().readDWord();
		if(offline_count > 0){
			send(BEX.createEmptyBex(sequence, 4, 3, 0));//Requesting offline msgs
		}
	}
	private void handleServerDoneOffline(){
		send(BEX.createEmptyBex(sequence, 4, 5, 0));//Deleting offline msgs
	}
	private void handleUserDetails(BEX bex) {
		final int DETAILS_RES_SUCCESS = 0x0000;
		final int DETAILS_RES_NOT_FOUND = 0x0001;
		final int DETAILS_RES_TOO_MANY_REQUESTS = 0x0002;
		final int DETAILS_RES_SERVICE_TEMP_UNAVAILABLE = 0x0003;
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		if(tld == null){
			//Log.e("handleUserDetails", "tld1 is null");
			return;
		}
		int result_code = tld.getData().readWord();
		if(result_code != DETAILS_RES_SUCCESS){
			switch(result_code){
			case DETAILS_RES_NOT_FOUND:
				svc.showDialogInContactList(Locale.getString("s_error_message_header"), Locale.getString("s_show_info_error_1"));
				break;
			case DETAILS_RES_TOO_MANY_REQUESTS:
				svc.showDialogInContactList(Locale.getString("s_error_message_header"), Locale.getString("s_show_info_error_2"));
				break;
			case DETAILS_RES_SERVICE_TEMP_UNAVAILABLE:
				svc.showDialogInContactList(Locale.getString("s_error_message_header"), Locale.getString("s_show_info_error_3"));
				break;
			}
			return;
		}
		AccountInfoContainer info = new AccountInfoContainer();
		tld = list.getTLD(0x4);
		if(tld != null) info.nick_name = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x5);
		if(tld != null) info.first_name = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x6);
		if(tld != null) info.last_name = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x7);
		if(tld != null) info.country = AccountInfoContainer.countries[tld.getData().readWord()];
		tld = list.getTLD(0x8);
		if(tld != null) info.region = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x9);
		if(tld != null) info.city = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0xA);
		if(tld != null) info.zipcode = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0xB);
		if(tld != null) info.address = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0xC);
		if(tld != null) info.language = AccountInfoContainer.languages[tld.getData().readWord()];
		tld = list.getTLD(0xD);
		if(tld != null) info.additional_language = AccountInfoContainer.languages[tld.getData().readWord()];
		tld = list.getTLD(0xE);
		if(tld != null) info.gender = AccountInfoContainer.genders[tld.getData().readByte()];
		tld = list.getTLD(0xF);
		if(tld != null) info.birthday = tld.getData().readUNIXEpochTimeStampNoCorrection();
		tld = list.getTLD(0x10);
		if(tld != null) info.homepage = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x11);
		if(tld != null) info.about = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x12);
		if(tld != null) info.interests = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x13);
		if(tld != null) info.email = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x14);
		if(tld != null) info.additional_email = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x15);
		if(tld != null) info.home_phone = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x16);
		if(tld != null) info.work_phone = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x17);
		if(tld != null) info.cellular_phone = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x18);
		if(tld != null) info.fax_number = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1A);
		if(tld != null) info.company = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1B);
		if(tld != null) info.departament = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1C);
		if(tld != null) info.position = tld.getData().readStringUTF8(tld.getLength());
		svc.showAccountInfoDialogInContactList(info);
	}
	private void handleSearchResult(BEX bex){
		final int SEARCH_RES_SUCCESS = 0x0000;
		final int SEARCH_RES_NOT_FOUND = 0x0001;
		final int SEARCH_RES_BAD_REQUEST = 0x0002;
		final int SEARCH_RES_TOO_MANY_REQUESTS = 0x0003;
		final int SEARCH_RES_SERVICE_TEMP_UNAVAILABLE = 0x0004;
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		int result_code = tld.getData().readWord();
		switch(result_code){
		case SEARCH_RES_SUCCESS:
			AccountInfoContainer result = new AccountInfoContainer();
			tld = list.getTLD(0x2);
			if(tld != null){
				result.account_ = tld.getData().readStringUTF8(tld.getLength());
			}
			tld = list.getTLD(0x3);
			if(tld != null){
				result.nick_name = tld.getData().readStringUTF8(tld.getLength());
			}
			tld = list.getTLD(0x4);
			if(tld != null){
				result.first_name = tld.getData().readStringUTF8(tld.getLength());
			}
			tld = list.getTLD(0x5);
			if(tld != null){
				result.last_name = tld.getData().readStringUTF8(tld.getLength());
			}
			tld = list.getTLD(0x6);
			if(tld != null){
				result.gender = AccountInfoContainer.genders[tld.getData().readByte()];
			}
			tld = list.getTLD(0x7);
			if(tld != null){
				result.age_ = tld.getData().readByte();
			}
			tld = list.getTLD(0x8);
			if(tld != null){
				result.online_ = true;
			}
			svc.handleSearchResult(result);
			tld = list.getTLD(0x9);
			if(tld != null){
				svc.handleSearchEnd();
			}
			break;
		case SEARCH_RES_NOT_FOUND:
			svc.showDialogInSearch(Locale.getString("s_search_notify_header"), Locale.getString("s_search_error_1"));
			break;
		case SEARCH_RES_BAD_REQUEST:
			svc.showDialogInSearch(Locale.getString("s_search_notify_header"), Locale.getString("s_search_error_2"));
			break;
		case SEARCH_RES_TOO_MANY_REQUESTS:
			svc.showDialogInSearch(Locale.getString("s_search_notify_header"), Locale.getString("s_search_error_3"));
			break;
		case SEARCH_RES_SERVICE_TEMP_UNAVAILABLE:
			svc.showDialogInSearch(Locale.getString("s_search_notify_header"), Locale.getString("s_search_error_4"));
			break;
		}
	}
	private void handleProfileDetails(BEX bex) {
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		if(tld == null) return;
		int result_code = tld.getData().readWord();
		if(result_code != 0) return;
		tld = list.getTLD(0x4);
		String nick = tld.getData().readStringUTF8(tld.getLength());
		if(nick.length() == 0) return;
		if(nickname.equals(nick)) return;
		nickname = nick;
		try {
			svc.profiles.saveProfiles();
		} catch (Exception e) {
			e.printStackTrace();
		}
		svc.handleProfileStatusChanged();
	}
	private void handleTypingNotify(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD account = list.getTLD(0x1);
		if(account == null) return;
		String account_s = account.getData().readStringUTF8(account.getLength());
		wTLD tld = list.getTLD(0x1001);
		int tid = -1;
		if(tld != null) tid = tld.getData().readDWord();
		Contact contact = getContactById(account_s, tid);
		if(contact == null) return;
		wTLD notify_type = list.getTLD(0x2);
		int notify = notify_type.getData().readDWord();
		switch(notify){
		case 0x1:
			wTLD value_tld = list.getTLD(0x3);
			if(value_tld == null) return;
			int value = value_tld.getData().readDWord();
			if(value == 0x1){
				contact.setTyping(true);
			}else{
				contact.setTyping(false);
			}
			break;
		}
	}
	private Contact createTemporaryContact(String ID, int tid){
		synchronized(ContactsAdapter.LOCKER){
			Contact contact = new Contact(ID, ID, ID.hashCode(), 0, 0, false, false, this);
			contact.setTemporary(true);
			contact.setTransportId(tid);
			contacts.add(contact);
			sortContactList();
			svc.handleContactListNeedRebuild();
			try {
				saveRoster(roster_database);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return contact;
		}
	}
	private void handleServerMessage(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		String account = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1001);
		int tid = -1;
		if(tld != null) tid = tld.getData().readDWord();
		tld = list.getTLD(0x2);
		int unique_id = tld.getData().readDWord();
		tld = list.getTLD(0x3);
		int msg_type = tld.getData().readDWord();
		if(msg_type != 0x1) return;
		tld = list.getTLD(0x4);
		String message = tld.getData().readStringUTF8(tld.getLength());
		Contact contact = getContactById(account, tid);
		tld = list.getTLD(0x7);
		if(contact == null){
			if(AntispamBot.enabled()){
				int res = AntispamBot.checkQuestion(account, message, this);
				switch(res){
				case AntispamBot.ACCEPTED:
					if(tld == null){
						sendRawMessage(account, tid, AntispamBot.getAccepted());
						message = Locale.getString("s_contact_passed_antispam");
						contact = createTemporaryContact(account, tid);
					}else{
						return;
					}
					break;
				case AntispamBot.NEED_QUEST:
					if(tld == null) sendRawMessage(account, tid, AntispamBot.getQuestion());
					return;
				case AntispamBot.BANNED:
					return;
				}
			}else{
				contact = createTemporaryContact(account, tid);
			}
		}
		contact.getHistoryObject().preloadCache();
		HistoryItem hst = null;
		if(tld != null){
			tld = list.getTLD(0x8);
			long timestamp = utilities.correctTimestamp(tld.getData().readUNIXEpochTimeStamp());
			hst = new HistoryItem(timestamp);
		}else{
			hst = new HistoryItem();
		}
		hst.contact = contact;
		hst.direction = HistoryItem.DIRECTION_INCOMING;
		hst.unique_id = unique_id;
		hst.message = message;
		contact.getHistoryObject().putMessage(hst, true);
		svc.putIntoOpened(contact);
		svc.refreshChat();
		svc.doVibrate(150);
		svc.media.playEvent(Media.INC_MSG);
		if(list.getTLD(0x5) != null)
			userSend(BimoidProtocol.createMessageReport(sequence, account, unique_id));
		svc.contactForOpenFromNotify = contact;
		if(!((utilities.contactEquals(svc.currentChatContact, contact)) && ChatActivity.isAnyChatOpened)){
			contact.increaseUnreadMessages();
			svc.createPersonalMessageNotify(utilities.getHash(contact), R.drawable.inc_msg_animated, contact.getID()+"/"+contact.getName(), hst.message, contact, -1);
			svc.handleContactListNeedRebuild();
			svc.checkUnreaded();
		}
		if(!ChatActivity.isAnyChatOpened)
			svc.currentChatContact = contact;
	}
	private void handleMessageReport(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x2);
		if(tld == null) return;
		int unique_id = tld.getData().readDWord();
		setMessageConfirmed(unique_id);
		svc.refreshChat();
	}
	private void setMessageConfirmed(int unique_id){
		for(int i=0; i<messages_for_confirming.size(); i++){
			HistoryItem hst = messages_for_confirming.get(i);
			if(hst.unique_id == unique_id){
				hst.setConfirmed();
				messages_for_confirming.removeElementAt(i);
				return;
			}
		}
	}
	private void handleServerItemOperation(BEX bex){
		final int OPER_ADD_ITEM = 0x0001;
		final int OPER_DEL_ITEM = 0x0002;
		final int OPER_UPD_ITEM = 0x0003;
		Log.i("BimoidProfile:handleServerItemOperation", "received");
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		if(tld == null){
			//svc.showDialogInContactList(ID, "Сервер произвел операцию над удаленной копией" +
			//		" контакт-листа. Уведомление об изменении" +
			//		" локальной копии обработано не верно." +
			//		" Рекомендуется переподключить аккаунт.");
			return;
		}
		int operation = tld.getData().readWord();
		  tld = list.getTLD(0x2);
		int item_type = tld.getData().readWord();
		  tld = list.getTLD(0x3);
		int item_id = tld.getData().readDWord();
		  tld = list.getTLD(0x4);
		int group_id = tld.getData().readDWord();
		  tld = list.getTLD(0x5);
		sTLDList slist;
		sTLD stld;
		ByteBuffer sdata;
		synchronized(ContactsAdapter.LOCKER){
			switch(item_type){
			case RosterItem.OBIMP_CONTACT:
				switch(operation){
				case OPER_ADD_ITEM:
					if(getContactById(item_id) != null){
						//svc.showDialogInContactList(ID, "Сервер попытался создать объект в контакт-листе. Такой объект уже существует в Вашем контакт-листе.");
						return;
					}
					slist = new sTLDList(tld.getData(), tld.getLength());
					stld = slist.getTLD(0x2);
					sdata = stld.getData();
					  String account = sdata.readStringUTF8(stld.getLength());
					stld = slist.getTLD(0x3);
					sdata = stld.getData();
					  String name = sdata.readStringUTF8(stld.getLength());
					stld = slist.getTLD(0x4);
					sdata = stld.getData();
					  int privacy = sdata.readByte();
					int transport_id = -1;
					stld = slist.getTLD(0x1001);
					if(stld != null) transport_id = stld.getData().readDWord();
					Contact contact = new Contact(account, name, group_id, item_id, privacy,
							(slist.getTLD(0x5) != null), (slist.getTLD(0x6) != null), this);
					contact.setTransportId(transport_id);
					contacts.add(contact);
					sortContactList();
					svc.handleContactListNeedRebuild();
					break;
				case OPER_DEL_ITEM:
					removeRosterItemById(item_id);
					sortContactList();
					svc.handleContactListNeedRebuild();
					break;
				case OPER_UPD_ITEM:
					contact = getContactById(item_id);
					//Log.i("ITEM_OPER", "ID: "+item_id);
					if(contact == null){
						svc.showDialogInContactList(ID, Locale.getString("s_roster_operations_err1"));
						return;
					}
					slist = new sTLDList(tld.getData(), tld.getLength());
					stld = slist.getTLD(0x2);
					sdata = stld.getData();
					  account = sdata.readStringUTF8(stld.getLength());
					stld = slist.getTLD(0x3);
					sdata = stld.getData();
					  name = sdata.readStringUTF8(stld.getLength());
					stld = slist.getTLD(0x4);
					sdata = stld.getData();
					  privacy = sdata.readByte();
					transport_id = -1;
					stld = slist.getTLD(0x1001);
					if(stld != null) transport_id = stld.getData().readDWord();
					  contact.setName(name);
					  contact.setTransportId(transport_id);
					  contact.setRosterId(group_id);
					  contact.setPrivacy(privacy);
					  contact.auth_flag = slist.getTLD(0x5) != null;
					  contact.general_flag = slist.getTLD(0x6) != null;
					sortContactList();
					svc.handleContactListNeedRebuild();
					break;
				}
				break;
			case RosterItem.OBIMP_GROUP:
				switch(operation){
				case OPER_ADD_ITEM:
					if(getGroupById(item_id) != null){
						svc.showDialogInContactList(ID, Locale.getString("s_roster_operations_err2"));
						return;
					}
					slist = new sTLDList(tld.getData(), tld.getLength());
					stld = slist.getTLD(0x1);
					sdata = stld.getData();
					String group_name = sdata.readStringUTF8(stld.getLength());
					Group group = new Group(group_name, item_id, group_id, this);
					contacts.add(group);
					break;
				case OPER_DEL_ITEM:
					removeRosterItemById(item_id);
					sortContactList();
					svc.handleContactListNeedRebuild();
					break;
				case OPER_UPD_ITEM:
					group = getGroupById(item_id);
					if(group == null){
						svc.showDialogInContactList(ID, Locale.getString("s_roster_operations_err1"));
						return;
					}
					slist = new sTLDList(tld.getData(), tld.getLength());
					stld = slist.getTLD(0x1);
					sdata = stld.getData();
					group_name = sdata.readStringUTF8(stld.getLength());
					group.setName(group_name);
					group.setRosterId(group_id);
					break;
				}
				break;
			}
		}
	}
	private void handleRosterOperationResult(BEX bex){
		/*
		ADD_RES_SUCCESS = 0x0000
		ADD_RES_ERROR_WRONG_ITEM_TYPE = 0x0001
		ADD_RES_ERROR_WRONG_PARENT_GROUP = 0x0002
		ADD_RES_ERROR_NAME_LEN_LIMIT = 0x0003
		ADD_RES_ERROR_WRONG_NAME = 0x0004
		ADD_RES_ERROR_ITEM_ALREADY_EXISTS = 0x0005
		ADD_RES_ERROR_ITEM_LIMIT_REACHED = 0x0006
		ADD_RES_ERROR_BAD_REQUEST = 0x0007
		ADD_RES_ERROR_BAD_ITEM_STLD = 0x0008
		ADD_RES_ERROR_NOT_ALLOWED = 0x0009
		
		DEL_RES_SUCCESS = 0x0000
		DEL_RES_ERROR_NOT_FOUND = 0x0001
		DEL_RES_ERROR_NOT_ALLOWED = 0x0002
		DEL_RES_ERROR_GROUP_NOT_EMPTY = 0x0003
		
		UPD_RES_SUCCESS = 0x0000
		UPD_RES_ERROR_NOT_FOUND = 0x0001
		UPD_RES_ERROR_WRONG_PARENT_GROUP = 0x0002
		UPD_RES_ERROR_NAME_LEN_LIMIT = 0x0003
		UPD_RES_ERROR_WRONG_NAME = 0x0004
		UPD_RES_ERROR_ITEM_ALREADY_EXISTS = 0x0005
		UPD_RES_ERROR_BAD_REQUEST = 0x0006
		UPD_RES_ERROR_BAD_ITEM_STLD = 0x0007
		UPD_RES_ERROR_NOT_ALLOWED = 0x0008
		*/
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		int result = tld.getData().readWord();
		synchronized(ContactsAdapter.LOCKER){
			switch(result){
			case 0x0:
				RosterOperation operation = getOperation(bex.getID());
				if(operation == null){
					//svc.showDialogInContactList(ID+": Результат операции", "Запроса на выполнение операции модификации контакт-листа не было. Переподключитесь");
					return;
				}
				switch(operation.operation){
				case RosterOperation.NOTE_ADD:
					tld = list.getTLD(0x2);
					NoteItem note = new NoteItem();
					note.profile = this;
					note.item_id = tld.getData().readDWord();
					note.group_id = operation.parent_id;
					note.name = operation.NOTE_NAME;
					note.TEXT = operation.NOTE_DESC;
					note.TYPE = operation.NOTE_TYPE;
					note.TIMESTAMP = operation.NOTE_TIMESTAMP;
					note.HASH = operation.NOTE_HASH;
					contacts.add(note);
					break;
				case RosterOperation.NOTE_MODIFY:
					note = getNoteById(operation.id);
					note.group_id = operation.parent_id;
					note.name = operation.NOTE_NAME;
					note.TEXT = operation.NOTE_DESC;
					note.TYPE = operation.NOTE_TYPE;
					note.TIMESTAMP = operation.NOTE_TIMESTAMP;
					note.HASH = operation.NOTE_HASH;
					break;
				case RosterOperation.NOTE_REMOVE:
					removeNoteById(operation.id);
					break;
				case RosterOperation.CONTACT_ADD:
					tld = list.getTLD(0x2);
					Contact contact;
					contact = getContactById(operation.account, operation.tid);
					if(contact != null)
						if(contact.isTemporary()){
							removeContactById(contact.getID(), contact.getTransportId());
						}
					contact = new Contact(operation.account,
							operation.name,
							operation.parent_id,
							tld.getData().readDWord(),
							operation.privacy,
							operation.auth_flag,
							operation.general_flag, this);
					contact.setTransportId(operation.tid);
					contacts.add(contact);
					sortContactList();
					try {
						saveRoster(roster_database);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					svc.handleContactListNeedRebuild();
					break;
				case RosterOperation.CONTACT_MODIFY:
					contact = getContactById(operation.id);
					if(contact == null) return;
					contact.setRosterId(operation.parent_id);
					contact.setPrivacy(operation.privacy);
					contact.setName(operation.name);
					contact.auth_flag = operation.auth_flag;
					contact.general_flag = operation.general_flag;
					break;
				case RosterOperation.CONTACT_REMOVE:
					removeContactById(operation.account, operation.tid);
					break;
				case RosterOperation.GROUP_ADD:
					tld = list.getTLD(0x2);
					Group group = new Group(operation.name,
							tld.getData().readDWord(),
							operation.parent_id,
							this);
					contacts.add(group);
					sortContactList();
					try {
						saveRoster(roster_database);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					svc.handleContactListNeedRebuild();
					break;
				case RosterOperation.GROUP_MODIFY:
					group = getGroupById(operation.id);
					if(group == null) return;
					group.setRosterId(operation.parent_id);
					group.setName(operation.name);
					break;
				case RosterOperation.GROUP_REMOVE:
					removeRosterItemById(operation.id);
					break;
				case RosterOperation.TRANSPORT_ADD:
					tld = list.getTLD(0x2);
					Transport t = new Transport(this, operation.transport_account, operation.transport_UUID);
					TransportParams p = getTransportParamsByUUID(operation.transport_UUID);
					t.UUID = operation.transport_UUID;
					t.params = operation.transport_params;
					t.account_name = operation.transport_account;
					t.account_pass = operation.transport_pass;
					t.name = operation.transport_friendly_name;
					t.account_server = p.default_host;
					t.account_port = p.default_port;
					t.group_id = 0;
					t.item_id = tld.getData().readDWord();
					t.profile = this;
					contacts.add(t);
					saveTransportAccount(t);
					break;
				case RosterOperation.TRANSPORT_REMOVE:
					removeTransportByID(operation.id);
					break;
				}
				sortContactList();
				svc.handleContactListNeedRebuild();
				try {
					saveRoster(roster_database);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			default:
				switch(bex.getSubType()){
				case 0x8:
					switch(result){
					case 0x1:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_1"));
						break;
					case 0x2:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_2"));
						break;
					case 0x3:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_3"));
						break;
					case 0x4:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_4"));
						break;
					case 0x5:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_5"));
						break;
					case 0x6:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_6"));
						break;
					case 0x7:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_7"));
						break;
					case 0x8:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_8"));
						break;
					case 0x9:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_9"));
						break;
					}
					break;
				case 0xA:
					switch(result){
					case 0x1:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_10"));
						break;
					case 0x2:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_12"));
						break;
					case 0x3:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_12"));
						break;
					}
					break;
				case 0xC:
					switch(result){
					case 0x1:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_13"));
						break;
					case 0x2:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_14"));
						break;
					case 0x3:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_3"));
						break;
					case 0x4:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_4"));
						break;
					case 0x5:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_5"));
						break;
					case 0x6:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_7"));
						break;
					case 0x7:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_8"));
						break;
					case 0x8:
						svc.showDialogInContactList(ID+": "+Locale.getString("s_operation_result"), Locale.getString("s_operation_result_9"));
						break;
					}
					break;
				}
				break;
			}
		}
		getOperation(bex.getID());
	}
	//=-=-=-=-=-=-= FILE TRANSFER CODE =-=-=-=-=-=-=
	private void handleFT_PARAMS(BEX bex){
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x5);
		boolean enabled = tld.getData().readBoolean();
		ft_params.enabled = enabled;
		if(!enabled) return;
		tld = list.getTLD(0x6);
		boolean proxy_enabled = tld.getData().readBoolean();
		ft_params.proxy_enabled = proxy_enabled;
		tld = list.getTLD(0x7);
		String proxy_host = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x8);
		int proxy_port = tld.getData().readDWord();
		ft_params.proxy_host = proxy_host;
		ft_params.proxy_port = proxy_port;
	}
	private void handleServerIncomingFileAsk(BEX bex){
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x1);
		String account = tld.getData().readStringUTF8(tld.getLength());
		tld = list.getTLD(0x1001);
		int tid = -1;
		Contact contact = getContactById(account, tid);
		if(contact == null){
			if(AntispamBot.enabled()){
				sendRawMessage(account, tid, AntispamBot.getQuestion());
				return;
			}else{
				contact = createTemporaryContact(account, tid);
			}
		}
		tld = list.getTLD(0x2);
		byte[] unique_id = tld.getData().readBytes(8);
		tld = list.getTLD(0x3);
		int files_count = tld.getData().readDWord();
		tld = list.getTLD(0x4);
		long total_size = tld.getData().readLong();
		tld = list.getTLD(0x5);
		String first_file_name = tld.getData().readStringUTF8(tld.getLength());
		String remote_host = "";
		int remote_port = 0;
		String proxy_host = "";
		int proxy_port = 0;
		boolean sender_supports_proxy = false;
		int mode = -1;
		tld = list.getTLD(0x6);
		if(tld != null){
			mode = FileReceiver.MODE_NORMAL;
			remote_host = tld.getData().readStringUTF8(tld.getLength());
			remote_port = list.getTLD(0x7).getData().readDWord();
		}
		tld = list.getTLD(0x8);
		if(tld != null){
			if(mode == -1) mode = FileReceiver.MODE_PROXY;
			proxy_host = tld.getData().readStringUTF8(tld.getLength());
			proxy_port = list.getTLD(0x9).getData().readDWord();
			sender_supports_proxy = true;
		}
		Log.i("FileReceiver", "UniqueID: "+String.valueOf(unique_id));
		Log.i("FileReceiver", "File name: "+first_file_name);
		Log.i("FileReceiver", "Files count: "+String.valueOf(files_count));
		Log.i("FileReceiver", "Total size: "+String.valueOf(total_size));
		FileReceiver receiver = new FileReceiver(unique_id, contact, first_file_name, files_count, total_size, sender_supports_proxy, mode);
		receiver.setRemoteAddress(remote_host, remote_port);
		receiver.setProxyAddress(proxy_host, proxy_port);
		transfers.add(receiver);
		//=-=-=-=-=-= NOTIFYING USER =-=-=-=-=-=
		HistoryItem hst = new HistoryItem();
		hst.contact = contact;
		hst.direction = HistoryItem.DIRECTION_INCOMING;
		hst.unique_id = (int)System.currentTimeMillis();
		if(files_count == 1){
			hst.message = utilities.match(Locale.getString("s_transfer_inc_file"), new String[]{first_file_name, String.valueOf(total_size)});
		}else{
			hst.message = utilities.match(Locale.getString("s_transfer_inc_files"), new String[]{first_file_name, String.valueOf(files_count), String.valueOf(total_size)});
		}
		hst.attachTransfer(receiver);
		contact.getHistoryObject().preloadCache();
		contact.getHistoryObject().putMessage(hst, false);
		svc.doVibrate(150);
		svc.media.playEvent(Media.INC_MSG);
		svc.putIntoOpened(contact);
		svc.refreshChat();
		if(!((utilities.contactEquals(svc.currentChatContact, contact)) && ChatActivity.isAnyChatOpened)){
			//Log.i("BimoidProfile", "Notifying user about incoming file 1");
			//Log.i("BimoidProfile", "Notifying user about incoming file 2");
			contact.increaseUnreadMessages();
			contact.setHasFile();
			svc.createPersonalFileNotify(utilities.getHash(contact), R.drawable.file, contact.getID()+"/"+contact.getName(), Locale.getString("s_file_transfer"), contact, files_count);
			svc.handleContactListNeedRebuild();
		}
	}
	private void handleFileReply(BEX bex){
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x2);
		if(tld == null) return;
		byte[] unique_id = tld.getData().readBytes(8);
		FileTransfer transfer = getTransferById(unique_id);
		tld = list.getTLD(0x3);
		if(tld == null) return;
		int code = tld.getData().readWord();
		Log.i("FileReply", String.valueOf(code));
		switch(code){
		case FileTransfer.FT_REPLY_CODE_ACCEPT:
			tld = list.getTLD(0x4);
			if(tld != null){
				String remote_ip = tld.getData().readStringUTF8(tld.getLength());
				int remote_port = list.getTLD(0x5).getData().readDWord();
				if(transfer.type == FileTransfer.SENDER)
					((FileSender)transfer).setRemoteAddress(remote_ip, remote_port);
			}
			transfer.runTransfer();
			break;
		case FileTransfer.FT_REPLY_CODE_DECLINE:
			transfer.cancel();
			break;
		case FileTransfer.FT_REPLY_CODE_DISABLED:
			transfer.cancel();
			break;
		case FileTransfer.FT_REPLY_CODE_NOT_ALLOWED:
			transfer.cancel();
			break;
		}
	}
	private void handleFTControl(BEX bex){
		wTLDList list = new wTLDList(bex.getData(), bex.getLength());
		wTLD tld = list.getTLD(0x2);
		byte[] unique_id = tld.getData().readBytes(8);
		tld = list.getTLD(0x3);
		int code = tld.getData().readWord();
		FileTransfer transfer = getTransferById(unique_id);
		if(transfer == null) return;
		Log.i("FTControl", String.valueOf(code));
		switch(code){
		case FileTransfer.FT_CONTROL_CODE_CANCEL:
			transfer.cancel();
			break;
		case FileTransfer.FT_CONTROL_CODE_READY:
			transfer.runTransfer();
			break;
		case FileTransfer.FT_CONTROL_CODE_DIRECT_FAILED:
			transfer.cancel();
			break;
		case FileTransfer.FT_CONTROL_CODE_DIRECT_FAILED_TRY_PROXY:
			transfer.runProxyTransfer();
			break;
		case FileTransfer.FT_CONTROL_CODE_DIRECT_FAILED_TRY_REVERSE:
			transfer.reverseConnection();
			break;
		case FileTransfer.FT_CONTROL_CODE_PROXY_FAILED:
			transfer.cancel();
			break;
		}
	}
	public void createFileSender(Contact contact, String[] files){
		FileSender sender = new FileSender(contact, files, ft_params.proxy_enabled);
		sender.setProxyAddress(ft_params.proxy_host, ft_params.proxy_port);
		transfers.add(sender);
		HistoryItem hst = new HistoryItem();
		hst.contact = contact;
		hst.direction = HistoryItem.DIRECTION_INCOMING;
		hst.unique_id = (int)System.currentTimeMillis();
		hst.message = Locale.getString("s_file_send_initializing");
		hst.attachTransfer(sender);
		contact.getHistoryObject().preloadCache();
		contact.getHistoryObject().putMessage(hst, false);
		//svc.refreshChat();
	}
	public void acceptFile(byte[] unique_id, String account){
		FileTransfer transfer = getTransferById(unique_id);
		if(transfer == null) return;
		sendFileTransferAnswer(unique_id, account, FileTransfer.FT_REPLY_CODE_ACCEPT, transfer.getLocalPort());
	}
	public void sendFileTransferAnswer(byte[] unique_id, String ID, int code, int port){
		userSend(BimoidProtocol.createFileTransferAnswer(sequence, ID, unique_id, code, socket.getIp(), port));
	}
	public void sendFileTransferAsk(byte[] unique_id, String ID, int files_count, long total_size, String file_name, int port){
		userSend(BimoidProtocol.createFileTransferAsk(sequence, ID, unique_id, files_count, total_size, file_name, socket.getIp(), port, ft_params.proxy_host, ft_params.proxy_port));
	}
	public void sendFTControl(byte[] unique_id, String ID, int code){
		userSend(BimoidProtocol.createFT_CONTROL(sequence, ID, unique_id, code));
	}
	public FileTransfer getTransferById(byte[] unique_id){
		synchronized(transfers){
			for(FileTransfer transfer: transfers){
				if(utilities.arrayEquals(transfer.getUniqueId(), unique_id))
					return transfer;
			}
		}
		return null;
	}
	public void removeTransferById(byte[] unique_id){
		synchronized(transfers){
			for(FileTransfer transfer: transfers){
				if(utilities.arrayEquals(transfer.getUniqueId(), unique_id))
					transfers.remove(transfer);
			}
		}
	}
	//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	public void sendRawMessage(String ID, int tid, String message){
		userSend(BimoidProtocol.createMessage(sequence, (int)System.currentTimeMillis(), ID, message, false, tid));
	}
	public void sendMessage(Contact contact, HistoryItem hst){
		svc.media.playEvent(Media.OUT_MSG);
		messages_for_confirming.add(hst);
		contact.getHistoryObject().putMessage(hst, true);
		userSend(BimoidProtocol.createMessage(sequence, hst.unique_id, contact.getID(), hst.message, true, contact.getTransportId()));
	}
	public void sendTypingNotify(int value, String id, int tid){
		userSend(BimoidProtocol.createTypingNotify(sequence, id, value, tid));
	}
	public void sendSearchRequest(AccountInfoContainer criteries){
		userSend(BimoidProtocol.createSearchRequest(sequence, criteries));
	}
	public void doRequestAccountInfo(Contact contact){
		send(BimoidProtocol.createDetailsRequest(sequence, contact, 0x2));
	}
	public void sendAuthReply(String account, int code, int tid){
		userSend(BimoidProtocol.createAuthReply(sequence, account, code, tid));
	}
	public void sendAuthReq(String account, String reason, int tid){
		userSend(BimoidProtocol.createAuthReq(sequence, account, reason, tid));
	}
	public void sendAuthRev(String account, String reason, int tid){
		userSend(BimoidProtocol.createAuthRevoke(sequence, account, reason, tid));
	}
	public void setStatus(int status, String desc){
		this.status = status;
		sts_desc = desc;
		ByteBuffer data = BimoidProtocol.createSetStatus(sequence, status, "", extended_status, ext_sts_desc);
		userSend(data);
		svc.handleProfileStatusChanged();
	}
	public void setExtStatus(int status, String desc){
		extended_status = status;
		ext_sts_desc = desc;
		ByteBuffer data = BimoidProtocol.createSetStatus(sequence, this.status, sts_desc, extended_status, ext_sts_desc);
		userSend(data);
		svc.handleProfileStatusChanged();
	}
	public int getStatus(){
		return status;
	}
	public int getExtStatus(){
		return extended_status;
	}
	public String getExtStatusDescription(){
		return ext_sts_desc;
	}
	private void setAllTransportContactsOffline(int tid){
		synchronized(ContactsAdapter.LOCKER){
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)contacts.get(i);
					if(contact.getTransportId() == tid){
						contact.setStatus(-1);
					}
					break;
				}
			}
			if(ChatActivity.isAnyChatOpened)
				svc.refreshChatUserInfo();
		}
	}
	private void setAllContactsOffline(){
		synchronized(ContactsAdapter.LOCKER){
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)contacts.get(i);
					contact.setStatus(-1);
					break;
				case RosterItem.TRANSPORT_ITEM:
					Transport transport = (Transport)contacts.get(i);
					transport.setStatus(-1);
					transport.connected = false;
					transport.ready = false;
					break;
				}
			}
			if(ChatActivity.isAnyChatOpened)
				svc.refreshChatUserInfo();
		}
	}
	//========
	public NoteItem getNoteById(int id){
		for(RosterItem item: contacts){
			if(item.type != RosterItem.CL_ITEM_TYPE_NOTE) continue;
			NoteItem note = (NoteItem)item;
			if(note.item_id == id){
				return note;
			}
		}
		return null;
	}
	public Contact getContactById(String id, int tid){
		for(RosterItem item: contacts){
			if(item.type != RosterItem.OBIMP_CONTACT) continue;
			Contact contact = (Contact)item;
			if(contact.getID().equals(id) && contact.getTransportId() == tid){
				return contact;
			}
		}
		return null;
	}
	public Contact removeContactById(String id, int tid){
		synchronized(ContactsAdapter.LOCKER){
			for(RosterItem item: contacts){
				if(item.type != RosterItem.OBIMP_CONTACT) continue;
				Contact contact = (Contact)item;
				if(contact.getID().equals(id) && contact.getTransportId() == tid){
					svc.removeFromOpened(contact);
					contact.getHistoryObject().deleteHistory();
					contacts.removeElement(item);
					sortContactList();
					try {
						saveRoster(roster_database);
					} catch (Exception e) {
						e.printStackTrace();
					}
					svc.handleIgnoreListNeedRebuild();
					svc.handleContactListNeedRebuild();
					return contact;
				}
			}
			return null;
		}
	}
	public NoteItem removeNoteById(int id){
		synchronized(ContactsAdapter.LOCKER){
			for(RosterItem item: contacts){
				if(item.type != RosterItem.CL_ITEM_TYPE_NOTE) continue;
				NoteItem note = (NoteItem)item;
				if(note.item_id == id){
					contacts.removeElement(item);
					sortContactList();
					try {
						saveRoster(roster_database);
					} catch (Exception e) {
						e.printStackTrace();
					}
					svc.handleContactListNeedRebuild();
					return note;
				}
			}
			return null;
		}
	}
	public Contact getContactById(int id){
		for(RosterItem item: contacts){
			if(item.type != RosterItem.OBIMP_CONTACT) continue;
			Contact contact = (Contact)item;
			if(contact.getItemId() == id){
				return contact;
			}
		}
		return null;
	}
	public Group getGroupById(int id){
		for(RosterItem item: contacts){
			if(item.type != RosterItem.OBIMP_GROUP) continue;
			Group group = (Group)item;
			if(group.getItemId() == id){
				return group;
			}
		}
		return null;
	}
	public RosterItem getItemByRosterId(int id){
		synchronized(ContactsAdapter.LOCKER){
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)contacts.get(i);
					if(contact.getGroupId() == id){
						return contact;
					}
					break;
				case RosterItem.OBIMP_GROUP:
					Group group = (Group)contacts.get(i);
					if(group.getItemId() == id){
						return group;
					}
					break;
				}
			}
			return null;
		}
	}
	public void removeRosterItemById(int id){
		synchronized(ContactsAdapter.LOCKER){
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)contacts.get(i);
					if(contact.getItemId() == id){
						contacts.removeElementAt(i);
						return;
					}
					break;
				case RosterItem.OBIMP_GROUP:
					Group group = (Group)contacts.get(i);
					if(group.getItemId() == id){
						contacts.removeElementAt(i);
						return;
					}
					break;
				}
			}
		}
	}
	public void getUnreaded(MessagesDump dump){
		synchronized(ContactsAdapter.LOCKER){
			if(dump == null) return;
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)contacts.get(i);
					if(contact.hasUnreadMessages()){
						dump.contacts++;
						dump.messages += contact.getUnreadCount();
						if(contact.haveAuth() || contact.haveIncFile())
							dump.messages--;
					}
					break;
				}
			}
		}
	}
	public void getPresenceDump(PresenceDump dump){
		synchronized(ContactsAdapter.LOCKER){
			if(dump == null) return;
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)contacts.get(i);
					if(contact.getStatus() >= 0){
						dump.online++;
					}
					dump.total++;
					break;
				}
			}
		}
	}
	public void addTransport(String login, String pass, TransportParams params){
		if(!connected){
			svc.showDialogInContactList(this.ID+": "+Locale.getString("s_information"), Locale.getString("s_profile_must_be_connected_notify"));
			return;
		}
		RosterOperation operation = new RosterOperation(RosterOperation.TRANSPORT_ADD);
		operation.transport_UUID = params.UUID;
		operation.transport_params = params;
		operation.transport_account = login;
		operation.parent_id = 0;
		operation.transport_friendly_name = params.full_name+" ("+login+")";
		operation.transport_pass = pass;
		operations.add(operation);
		userSend(operation.buildOperationPacket(sequence));
	}
	public void addNote(String NAME, String TEXT, byte TYPE, int parent_id){
		if(!connected){
			svc.showDialogInContactList(this.ID+": "+Locale.getString("s_information"), Locale.getString("s_profile_must_be_connected_notify"));
			return;
		}
		RosterOperation operation = new RosterOperation(RosterOperation.NOTE_ADD);
		operations.add(operation);
		operation.NOTE_NAME = NAME;
		operation.NOTE_DESC = TEXT;
		operation.parent_id = parent_id;
		operation.NOTE_TYPE = TYPE;
		operation.NOTE_TIMESTAMP = 0;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void addContact(String ID, String nickname, int parent_id){
		if(!connected){
			svc.showDialogInContactList(this.ID+": "+Locale.getString("s_information"), Locale.getString("s_profile_must_be_connected_notify"));
			return;
		}
		RosterOperation operation = new RosterOperation(RosterOperation.CONTACT_ADD);
		operations.add(operation);
		operation.name = nickname;
		operation.account = ID;
		operation.parent_id = parent_id;
		operation.privacy = Contact.CL_PRIV_TYPE_NONE;
		operation.auth_flag = true;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void addTransportContact(String ID, String nickname, int parent_id, Transport t){
		if(!t.connected){
			svc.showDialogInContactList(this.ID+": "+Locale.getString("s_information"), Locale.getString("s_profile_must_be_connected_notify"));
			return;
		}
		RosterOperation operation = new RosterOperation(RosterOperation.CONTACT_ADD);
		operations.add(operation);
		operation.name = nickname;
		operation.account = ID;
		operation.parent_id = parent_id;
		operation.privacy = Contact.CL_PRIV_TYPE_NONE;
		operation.auth_flag = true;
		operation.tid = t.item_id;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void renameContact(int ID, String new_nickname){
		Contact contact = getContactById(ID);
		if(contact == null) return;
		RosterOperation operation = new RosterOperation(contact, RosterOperation.CONTACT_MODIFY);
		operations.add(operation);
		operation.name = new_nickname;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void moveContact(int ID, int new_parent_id){
		Contact contact = getContactById(ID);
		if(contact == null) return;
		RosterOperation operation = new RosterOperation(contact, RosterOperation.CONTACT_MODIFY);
		operations.add(operation);
		operation.parent_id = new_parent_id;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void deleteContact(String ID, int tid){
		Contact contact = getContactById(ID, tid);
		if(contact == null) return;
		RosterOperation operation = new RosterOperation(contact, RosterOperation.CONTACT_REMOVE);
		operations.add(operation);
		userSend(operation.buildOperationPacket(sequence));
	}
	public void modifyNote(int ID, int parent_id, String NAME, String TEXT, byte TYPE, long TIMESTAMP){
		NoteItem note = getNoteById(ID);
		if(note == null) return;
		RosterOperation operation = new RosterOperation(note, RosterOperation.NOTE_MODIFY);
		operations.add(operation);
		operation.parent_id = parent_id;
		operation.NOTE_NAME = NAME;
		operation.NOTE_DESC = TEXT;
		operation.NOTE_TYPE = TYPE;
		operation.NOTE_TIMESTAMP = TIMESTAMP;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void moveNote(int ID, int new_parent_id){
		NoteItem note = getNoteById(ID);
		if(note == null) return;
		RosterOperation operation = new RosterOperation(note, RosterOperation.NOTE_MODIFY);
		operations.add(operation);
		operation.parent_id = new_parent_id;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void deleteNote(int ID){
		NoteItem note = getNoteById(ID);
		if(note == null) return;
		RosterOperation operation = new RosterOperation(note, RosterOperation.NOTE_REMOVE);
		operations.add(operation);
		userSend(operation.buildOperationPacket(sequence));
	}
	public void deleteTransport(int tid){
		Transport transport = getTransportByID(tid);
		if(transport == null) return;
		RosterOperation operation = new RosterOperation(RosterOperation.TRANSPORT_REMOVE);
		operation.id = transport.getItemId();
		operations.add(operation);
		userSend(operation.buildOperationPacket(sequence));
	}
	public void addGroup(String name, int parent_id){
		if(!connected){
			svc.showDialogInContactList(this.ID+": "+Locale.getString("s_information"), Locale.getString("s_profile_must_be_connected_notify"));
			return;
		}
		RosterOperation operation = new RosterOperation(RosterOperation.GROUP_ADD);
		operations.add(operation);
		operation.name = name;
		operation.parent_id = parent_id;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void renameGroup(int ID, String new_name){
		Group group = getGroupById(ID);
		if(group == null) return;
		RosterOperation operation = new RosterOperation(group, RosterOperation.GROUP_MODIFY);
		operations.add(operation);
		operation.name = new_name;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void moveGroup(int ID, int new_parent_id){
		Group group = getGroupById(ID);
		if(group == null) return;
		RosterOperation operation = new RosterOperation(group, RosterOperation.GROUP_MODIFY);
		operations.add(operation);
		operation.parent_id = new_parent_id;
		userSend(operation.buildOperationPacket(sequence));
	}
	public void deleteGroup(int ID){
		Group group = getGroupById(ID);
		if(group == null) return;
		RosterOperation operation = new RosterOperation(group, RosterOperation.GROUP_REMOVE);
		operations.add(operation);
		userSend(operation.buildOperationPacket(sequence));
	}
	private RosterOperation getOperation(int operation_id){
		for(int i=0; i<operations.size(); i++){
			if(operations.get(i).operation_id == operation_id){
				return operations.remove(i);
			}
		}
		return null;
	}
	public Vector<Group> getGroups(){
		synchronized(ContactsAdapter.LOCKER){
			Vector<Group> list = new Vector<Group>();
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				if(item.type != RosterItem.OBIMP_GROUP) continue;
				list.add((Group)contacts.get(i));
			}
			return list;
		}
	}
	public void changePrivacy(String ID, int tid, int privacy){
		Contact contact = getContactById(ID, tid);
		if(contact == null) return;
		RosterOperation operation = new RosterOperation(contact, RosterOperation.CONTACT_MODIFY);
		operations.add(operation);
		if(privacy == Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST)
			operation.parent_id = 0;
		operation.privacy = privacy;
		userSend(operation.buildOperationPacket(sequence));
	}
	private Vector<RosterItem> getTemporaryContacts(){
		synchronized(ContactsAdapter.LOCKER){
			Vector<RosterItem> list = new Vector<RosterItem>();
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)contacts.get(i);
					if(contact.isTemporary()) list.add(contact);
					break;
				}
			}
			return list;
		}
	}
	//========
	private void userSend(ByteBuffer data){
		if(connected){
			send(data);
		}
	}
	private synchronized void send(ByteBuffer data){
		socket.write(data);
		//Log.e("Sequence", String.valueOf(sequence));
		sequence += 1;
		//if(sequence > 0xffffffff) sequence = 0x0;
	}
	public void connect(){
		if(!connected){
			sequence = 0;
			connecting = true;
			updateConnStatus(CONN_STUDY_1);
			socket.connect(server, 7023);
			svc.handleProfileStatusChanged();
		}
	}
	public void disconnectA(){
		Reconnector.stop();
		disconnect();
	}
	public void disconnect(){
		if(connected || connecting)
			socket.disconnect();
	}
	private void handleConnectionLosted(){
		Reconnector.start(status);
		handleDisconnected();
	}
	private void handleDisconnected(){
		connected = false;
		connecting = false;
		status = -1;
		banlist.clear();
		setAllContactsOffline();
		svc.refreshChatUserInfo();
		svc.handleProfileStatusChanged();
		svc.handleContactListNeedRebuild();
		svc.hideProgressInContactList();
	}
	public void saveTransportAccount(Transport t){
		final File dir = new File(resources.DATA_PATH+ID+"/TransportAccounts/");
		if(!(dir.isDirectory() && dir.exists())){
			try{
				dir.mkdirs();
			}catch(Exception e){}
		}
		final File file = new File(resources.DATA_PATH+ID+"/TransportAccounts/"+t.UUID+"_"+t.account_name);
		DataOutputStream dos = null;
		try{
			dos = new DataOutputStream(new FileOutputStream(file));
			dos.writeUTF(t.account_name);
			dos.writeUTF(t.account_pass);
			dos.writeUTF(t.account_server);
			dos.writeInt(t.account_port);
		}catch(Exception e){}
		try{
			if(dos != null) dos.close();
		}catch(Exception e){}
	}
	private TransportAccount getTransportAccount(String ID){
		final File file = new File(resources.DATA_PATH+this.ID+"/TransportAccounts/");
		//Log.e("BimoidProfile", "getTransportAccount(): dir exist: "+file.exists());
		final File[] accounts = file.listFiles();
		//Log.e("BimoidProfile", "getTransportAccount(): accounts: "+(accounts == null));
		if(accounts == null) return null;
		for(int i=0; i<accounts.length; i++){
			if(accounts[i].isDirectory()) continue;
			//Log.e("BimoidProfile", "getTransportAccount(): "+accounts[i].getName());
			if(accounts[i].getName().endsWith(ID)){
				DataInputStream dis = null;
				TransportAccount account = null;
				try{
					dis = new DataInputStream(new FileInputStream(accounts[i]));
					account = new TransportAccount();
					account.account = dis.readUTF();
					account.pass = dis.readUTF();
					account.server = dis.readUTF();
					account.port = dis.readInt();
				}catch(Exception e){}
				try{
					if(dis != null) dis.close();
				}catch(Exception e){}
				return account;
			}
		}
		return null;
	}
	//=======================================
	private void saveRoster(File database) throws Exception{
		synchronized(ContactsAdapter.LOCKER){
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(database));
			ByteBuffer roster = new ByteBuffer(1024);
			dos.writeShort(contacts.size());
			//Log.e("BimoidProfile", "Saving "+contacts.size()+" items");
			for(int i=0; i<contacts.size(); i++){
				RosterItem item = contacts.get(i);
				switch(item.type){
				case RosterItem.OBIMP_CONTACT:
					Contact contact = (Contact)item;
					roster.reset(1024);
					roster.writeByte((byte)RosterItem.OBIMP_CONTACT);
					roster.writeByte((byte)contact.getPrivacy());
					if(contact.auth_flag){
						roster.writeByte((byte)0x1);
					}else{
						roster.writeByte((byte)0x0);
					}
					if(contact.general_flag){
						roster.writeByte((byte)0x1);
					}else{
						roster.writeByte((byte)0x0);
					}
					if(contact.isTemporary()){
						roster.writeByte((byte)0x1);
					}else{
						roster.writeByte((byte)0x0);
					}
					roster.writeDWord(contact.getGroupId());
					roster.writeDWord(contact.getItemId());
					roster.writeDWord(contact.getTransportId());
					ByteBuffer tld = new ByteBuffer(512);
					tld.writeStringUTF8(contact.getName());
					roster.writeSTLD(tld, 0x0);
					tld = new ByteBuffer(512);
					tld.writeStringUTF8(contact.getID());
					roster.writeSTLD(tld, 0x1);
					dos.write(roster.getBytes());
					break;
				case RosterItem.OBIMP_GROUP:
					Group group = (Group)item;
					roster.reset(1024);
					roster.writeByte((byte)RosterItem.OBIMP_GROUP);
					roster.writeDWord(group.getItemId());
					roster.writeDWord(group.getGroupId());
					tld = new ByteBuffer(512);
					tld.writeStringUTF8(group.getName());
					roster.writeSTLD(tld, 0x0);
					dos.write(roster.getBytes());
					break;
				case RosterItem.TRANSPORT_ITEM:
					Transport transport = (Transport)item;
					roster.reset(1024);
					roster.writeByte((byte)RosterItem.TRANSPORT_ITEM);
					roster.writeDWord(transport.getItemId());
					roster.writeDWord(transport.getGroupId());
					tld = new ByteBuffer(512);
					tld.writeStringUTF8(transport.getName());
					roster.writeSTLD(tld, 0x0);
					tld = new ByteBuffer(512);
					tld.writeStringUTF8(transport.account_name);
					roster.writeSTLD(tld, 0x0);
					tld = new ByteBuffer(512);
					tld.writeStringUTF8(transport.UUID);
					roster.writeSTLD(tld, 0x0);
					dos.write(roster.getBytes());
					//Log.e("BimoidProfile", "Saving transport: "+transport.getName());
					break;
				case RosterItem.CL_ITEM_TYPE_NOTE:
					NoteItem note_item = (NoteItem)item;
					roster.reset(2048);
					roster.writeByte((byte)RosterItem.CL_ITEM_TYPE_NOTE);
					roster.writeDWord(note_item.getItemId());
					roster.writeDWord(note_item.getGroupId());
					tld = new ByteBuffer(2048);
					tld.writeStringUTF8(note_item.name);
					roster.writeSTLD(tld, 0x0);
					roster.writeByte(note_item.TYPE);
					tld = new ByteBuffer(2048);
					tld.writeStringUTF8((note_item.TEXT == null)? "": note_item.TEXT);
					roster.writeSTLD(tld, 0x0);
					roster.writeLong(note_item.TIMESTAMP);
					if(note_item.HASH == null){
						roster.writeByte((byte)0);
					}else{
						roster.writeByte((byte)note_item.HASH.length);
						roster.write(note_item.HASH);
					}
					dos.write(roster.getBytes());
					//Log.e("BimoidProfile", "Saving note: "+note_item.name);
					break;
				}
			}
		}
	}
	private void loadRoster() throws Exception{
		synchronized(ContactsAdapter.LOCKER){
			if(roster_database.length() < 16) return;
			contacts.clear();
			DataInputStream dis = new DataInputStream(new FileInputStream(roster_database));
			int items_count = dis.readShort();
			//Log.e("BimoidProfile", "Loading "+items_count+" items");
			for(int i=0; i<items_count; i++){
				byte type = dis.readByte();
				switch(type){
				case RosterItem.OBIMP_CONTACT:
					int privacy = dis.readByte();
					int auth = dis.readByte();
					int general = dis.readByte();
					int temporary = dis.readByte();
					int group_id = dis.readInt();
					int item_id = dis.readInt();
					int transport_id = dis.readInt();
					sTLD tld = new sTLD(dis);
					String nickname = tld.getData().readStringUTF8(tld.getLength());
					tld = new sTLD(dis);
					String ID = tld.getData().readStringUTF8(tld.getLength());
					Contact contact = new Contact(ID, nickname, group_id, item_id, privacy, (auth == 1), (general == 1), this);
					contact.setTemporary(temporary == 1);
					contact.setTransportId(transport_id);
					contacts.add(contact);
					break;
				case RosterItem.OBIMP_GROUP:
					item_id = dis.readInt();
					group_id = dis.readInt();
					tld = new sTLD(dis);
					String name = tld.getData().readStringUTF8(tld.getLength());
					Group group = new Group(name, item_id, group_id, this);
					contacts.add(group);
					break;
				case RosterItem.TRANSPORT_ITEM:
					item_id = dis.readInt();
					group_id = dis.readInt();
					tld = new sTLD(dis);
					String t_name = tld.getData().readStringUTF8(tld.getLength());
					tld = new sTLD(dis);
					String t_account_name = tld.getData().readStringUTF8(tld.getLength());
					tld = new sTLD(dis);
					String t_UUID = tld.getData().readStringUTF8(tld.getLength());
					Transport transport = new Transport(this, t_account_name, t_UUID);
					transport.item_id = item_id;
					transport.group_id = group_id;
					transport.account_name = t_account_name;
					transport.name = t_name;
					transport.UUID = t_UUID;
					transport.profile = this;
					transport.params = getTransportParamsByUUID(t_UUID);
					TransportAccount a = getTransportAccount(t_account_name);
					if(a != null){
						transport.account_pass = a.pass;
						transport.account_server = a.server;
						transport.account_port = a.port;
						//Log.e("BimoidProfile", "Loading transport with local params: "+transport.getName());
					}else{
						transport.account_server = transport.params.default_host;
						transport.account_port = transport.params.default_port;
						//Log.e("BimoidProfile", "Loading transport with default params: "+transport.getName());
					}
					contacts.add(transport);
					break;
				case RosterItem.CL_ITEM_TYPE_NOTE:
					item_id = dis.readInt();
					group_id = dis.readInt();
					tld = new sTLD(dis);
					String NAME = tld.getData().readStringUTF8(tld.getLength());
					byte TYPE = dis.readByte();
					tld = new sTLD(dis);
					String TEXT = tld.getData().readStringUTF8(tld.getLength());
					long TIMESTAMP = dis.readLong();
					byte len_ = dis.readByte();
					byte[] HASH = new byte[len_];
					if(len_ != 0){
						dis.read(HASH, 0, len_);
					}
					NoteItem note_item = new NoteItem();
					note_item.profile = this;
					note_item.setItemId(item_id);
					note_item.setRosterId(group_id);
					note_item.name = NAME;
					note_item.TYPE = TYPE;
					note_item.TEXT = TEXT;
					note_item.TIMESTAMP = TIMESTAMP;
					note_item.HASH = HASH;
					//Log.e("BimoidProfile", "Loading note: "+note_item.name);
					contacts.add(note_item);
					break;
				}
			}
		}
		svc.handleContactListNeedRebuild();
	}
	//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	private class TransferParams{
		public int proxy_port;
		public String proxy_host;
		public boolean proxy_enabled;
		public boolean enabled;
	}
	//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	private class reconnector{
		private reconnect_timer rt = new reconnect_timer();
		public boolean is_active = false; 
		public boolean enabled = false;
		private int last_status = 0;
		public reconnector(){
			rt.setDaemon(true);
			rt.start();
		}
		public void start(int last_status){
			if(!is_active){
				this.last_status = last_status;
				enabled = true;
				is_active = true;
				if(!svc.reconnect_mode) svc.addWakeLock(ID+password);
				Log.v("RECONNECTOR", "============STARTING RECONNECTOR=========");
			}
		}
		public void stop(){
			if(is_active){
				enabled = false;
				is_active = false;
				if(svc.reconnect_mode) svc.removeWakeLock(ID+password);
				Log.v("RECONNECTOR", "============RECONNECTOR STOPPED==========");
			}
		}
		private class reconnect_timer extends Thread{
			@Override
			public void run(){
				is_active = true;
				int i = 0;
				while(true){
					if(!enabled){
						try {
							sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					i++;
					if(i < 15) continue;
					i = 0;
					if(!enabled) continue;
					Log.v("RECONNECTOR", "TRYING TO CONNECT ...");
					status = last_status;
					connect();
				}
			}
		}
	}
	private void sendPingPacket(){
		send(BEX.createEmptyBex(sequence, 0x1, 0x6, 0x0));
	}
	private final class ping_thread extends Thread {
		private int counter = 0;
		private boolean ping_answer_received = true;
		public void resetTimer(){
			counter = 0;
			ping_answer_received = true;
		}
		@Override
		public void run(){
			setPriority(1);
			//counter = -30;
			int period = PreferenceTable.ping_freq;
			setName(ID+" ping thread");
			if(!PreferenceTable.use_ping) return;
			while(connected){
				try{
					if(counter > period){
						if(ping_answer_received){
							//Log.i("PING", "Answer received, continuing ...");
							ping_answer_received = false;
							if(connected){
								sendPingPacket();
							}
							//toastHandler.sendEmptyMessage(0x3);
						}else{
							//Log.i("PING", "Answer don't received, registering connection lost");
							handleConnectionLosted();
							return;
						}
						counter = 0;
					}
					sleep(1000);
					counter++;
					//svc.put_log("ICQ: Counter: "+counter);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	class BanList {
		public static final int TRYES_LIMIT = 5;
		final Vector<Item> list = new Vector<Item>();
		public synchronized void clear(){
			list.clear();
		}
		public synchronized int increase(String id){
			Item item = get(id);
			if(item == null){
				item = new Item();
				item.identifier = id;
				list.add(item);
			}else{
				item.tryes++;
			}
			return item.tryes;
		}
		public synchronized void remove(String id){
			for(int i=0; i<list.size(); i++){
				Item item = list.get(i);
				if(item.identifier.equals(id)){
					list.remove(i);
					return;
				}
			}
		}
		public synchronized Item get(String id){
			for(int i=0; i<list.size(); i++){
				Item item = list.get(i);
				if(item.identifier.equals(id)) return item;
			}
			return null;
		}
		public class Item {
			String identifier;
			int tryes = 0;
		}
	}
}
