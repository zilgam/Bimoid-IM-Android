package ru.ivansuper.bimoidproto;

import android.util.Log;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.socket.ByteBuffer;
import ru.ivansuper.socket.ClientSocketConnection;

public class Registrator {
	private BimoidService service;
	private ClientSocketConnection socket;
	public int STATUS;
	private int sequence = 0;
	public static final int IN_PROGRESS = 0;
	public static final int SUCCESS = 1;
	public static final int ERROR = 2;
	public String RESULT_MSG = "";
	private boolean disconnected_by_server = true;
	public String ID;
	public String pass;
	public String email;
	public Registrator(BimoidService service){
		this.service = service;
		socket = new ClientSocketConnection(){
			@Override
			public void onRawData(ByteBuffer data) {
				BEX bex = new BEX(data);
				handleBEX(bex);
			}
			@Override
			public void onConnect() {
				handleConnected();
			}
			@Override
			public void onConnecting() {
				
			}
			@Override
			public void onDisconnect() {
				Log.i("Registrator", "Disconnected");
				handleDisconnected();
			}
			@Override
			public void onLostConnection() {
				Log.i("Registrator", "onLostConnection");
				handleDisconnected();
			}
			@Override
			public void onError(int errorCode, Throwable t) {
				Log.i("Registrator", "Error code: "+String.valueOf(errorCode));
			}
			@Override
			public void onSocketCreated() {
			}
		};
	}
	public void doRegister(String ID, String pass, String email){
		this.ID = ID;
		this.pass = pass;
		this.email = email;
		STATUS = IN_PROGRESS;
		service.refreshRegistrationState();
		socket.connect("bimoid.net", 7023);
	}
	private void handleConnected(){
		send(BimoidProtocol.reg_createClientHello(sequence));
	}
	private void handleBEX(BEX bex){
		//Log.i("BEX", "Type: "+String.valueOf(bex.getType())+"   SubType: "+String.valueOf(bex.getSubType()));
		switch(bex.getType()){
		case 0x1:
			switch(bex.getSubType()){
			case 0x2:
				handleServerHelloReply(bex);
				break;
			case 0x5:
				handleServerBye(bex);
				break;
			case 0x9:
				handleServerRegistrationReply(bex);
				break;
			}
			break;
		}
	}
	private void handleServerBye(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD error = list.getTLD(0x1);
		if(error != null){
			int reason = error.getData().readWord();
			parseErrorCode(reason);
		}
		service.doVibrate(300);
		socket.disconnect();
	}
	private void parseErrorCode(int reason){
		switch(reason){
		case 0x1:
			RESULT_MSG = "Disconnected by server. Reason -- SRV_SHUTDOWN";
			break;
		case 0x2:
			RESULT_MSG = "Disconnected by server. Reason -- CLI_NEW_LOGIN";
			break;
		case 0x3:
			RESULT_MSG = "Disconnected by server. Reason -- ACCOUNT_KICKED";
			break;
		case 0x4:
			RESULT_MSG = "Disconnected by server. Reason -- INCORRECT_SEQ";
			break;
		case 0x5:
			RESULT_MSG = "Disconnected by server. Reason -- INCORRECT_BEX_TYPE";
			break;
		case 0x6:
			RESULT_MSG = "Disconnected by server. Reason -- INCORRECT_BEX_SUB";
			break;
		case 0x7:
			RESULT_MSG = "Disconnected by server. Reason -- INCORRECT_BEX_STEP";
			break;
		case 0x8:
			RESULT_MSG = "Disconnected by server. Reason -- TIMEOUT";
			break;
		case 0x9:
			RESULT_MSG = "Disconnected by server. Reason -- INCORRECT_WTLD";
			break;
		case 0xA:
			RESULT_MSG = "Disconnected by server. Reason -- NOT_ALLOWED";
			break;
		case 0xB:
			RESULT_MSG = "Disconnected by server. Reason -- FLOODING";
			break;
		}
	}
	private void handleDisconnected(){
		if(disconnected_by_server){
			STATUS = ERROR;
		}
		service.refreshRegistrationState();
	}
	private void handleServerHelloReply(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD error = list.getTLD(0x1);
		if(error != null){
			int reason = error.getData().readWord();
			parseErrorCode(reason);
			socket.disconnect();
			return;
		}
		wTLD tld = list.getTLD(0x5);
		if(tld == null){
			RESULT_MSG = Locale.getString("s_incorrect_server_answer");
			socket.disconnect();
			return;
		}
		boolean registration_enabled = tld.getData().readBoolean();
		if(!registration_enabled){
			RESULT_MSG = Locale.getString("s_registration_not_enabled");
			socket.disconnect();
			return;
		}
		doSendRegData();
	}
	private void doSendRegData(){
		send(BimoidProtocol.reg_createRequest(sequence, ID, pass, email));
	}
	private void handleServerRegistrationReply(BEX bex){
		ByteBuffer data = bex.getData();
		wTLDList list = new wTLDList(data, bex.getLength());
		wTLD tld = list.getTLD(0x1);
		if(tld == null){
			RESULT_MSG = Locale.getString("s_incorrect_server_answer");
			socket.disconnect();
			return;
		}
		int result = tld.getData().readWord();
		switch(result){
		/*
		REG_RES_SUCCESS = 0x0000
		REG_RES_DISABLED = 0x0001
		REG_RES_ACCOUNT_EXISTS = 0x0002
		REG_RES_BAD_ACCOUNT_NAME = 0x0003
		REG_RES_BAD_REQUEST = 0x0004
		REG_RES_BAD_SERVER_KEY = 0x0005
		REG_RES_SERVICE_TEMP_UNAVAILABLE = 0x0006
		 */
		case 0x0:
			RESULT_MSG = Locale.getString("s_registration_success");
			disconnected_by_server = false;
			STATUS = SUCCESS;
			break;
		case 0x1:
			RESULT_MSG = Locale.getString("s_reg_err1");
			break;
		case 0x2:
			RESULT_MSG = Locale.getString("s_reg_err2");
			break;
		case 0x3:
			RESULT_MSG = Locale.getString("s_reg_err3");
			break;
		case 0x4:
			RESULT_MSG = Locale.getString("s_reg_err4");
			break;
		case 0x5:
			RESULT_MSG = Locale.getString("s_reg_err5");
			break;
		case 0x6:
			RESULT_MSG = Locale.getString("s_reg_err6");
			break;
		}
		socket.disconnect();
	}
	private void send(ByteBuffer data){
		socket.write(data);
		sequence++;
	}
}
