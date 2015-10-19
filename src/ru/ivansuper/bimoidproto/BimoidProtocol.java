package ru.ivansuper.bimoidproto;

import android.util.Log;
import ru.ivansuper.bimoidim.Base64Coder;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidim.utilities;
import ru.ivansuper.bimoidproto.filetransfer.FileTransfer;
import ru.ivansuper.bimoidproto.transports.TransportSettings;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.socket.ByteBuffer;

public class BimoidProtocol {
	public static final int PRES_STATUS_OFFLINE = -0x0001;
	public static final int PRES_STATUS_ONLINE = 0x0000;
	public static final int PRES_STATUS_INVISIBLE = 0x0001;
	public static final int PRES_STATUS_INVISIBLE_FOR_ALL = 0x0002;
	public static final int PRES_STATUS_FREE_FOR_CHAT = 0x0003;
	public static final int PRES_STATUS_AT_HOME = 0x0004;
	public static final int PRES_STATUS_AT_WORK = 0x0005;
	public static final int PRES_STATUS_LUNCH = 0x0006;
	public static final int PRES_STATUS_AWAY = 0x0007;
	public static final int PRES_STATUS_NOT_AVAILABLE = 0x0008;
	public static final int PRES_STATUS_OCCUPIED = 0x0009;
	public static final int PRES_STATUS_DO_NOT_DISTURB = 0x000A;
	public static ByteBuffer createClientHello(int seq, String ID) throws Exception{
		ByteBuffer tld = new ByteBuffer(256);
		tld.writeStringUTF8(ID);
		ByteBuffer data = new ByteBuffer();
		data.writeWTLD(tld, 0x1);
		return BEX.createBex(seq, 0x1, 0x1, 0x0, data);
	}
	public static ByteBuffer reg_createClientHello(int seq){
		ByteBuffer tld = new ByteBuffer(0);
		ByteBuffer data = new ByteBuffer();
		data.writeWTLD(tld, 0x3);
		//Log.i("reg_createRequest", utilities.convertToHex(data.getBytes()));
		return BEX.createBex(seq, 0x1, 0x1, 0x0, data);
	}
	public static ByteBuffer reg_createRequest(int seq, String ID, String pass, String email){
		ByteBuffer data = new ByteBuffer();
		
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(ID);
		data.writeWTLD(tld, 0x1);
		
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(pass);
		data.writeWTLD(tld, 0x2);
		
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(email);
		data.writeWTLD(tld, 0x3);
		
		//Log.i("reg_createRequest", utilities.convertToHex(data.getBytes()));
		return BEX.createBex(seq, 0x1, 0x8, 0x0, data);
	}
	public static ByteBuffer createMD5Login(int seq, String ID, String password, String key) throws Exception{
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(ID);
		data.writeWTLD(tld, 0x1);
		tld = new ByteBuffer(512);
		tld.write(utilities.getHashArray(key, ID, password));
		data.writeWTLD(tld, 0x2);
		return BEX.createBex(seq, 0x1, 0x3, 0x0, data);
	}
	public static ByteBuffer createPlainTextLogin(int seq, String ID, String password) throws Exception {
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(ID);
		data.writeWTLD(tld, 0x1);
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(password);
		data.writeWTLD(tld, 0x3);
		return BEX.createBex(seq, 0x1, 0x3, 0x0, data);
	}
	public static ByteBuffer createSetCaps(int seq){
		
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeWord(0x1);
		tld.writeWord(0x5);
		tld.writeWord(0x7);
		tld.writeWord(0x8);
		tld.writeWord(0xa);//Уведомления о почте
		data.writeWTLD(tld, 0x1);//CAPS
		tld = new ByteBuffer(512);
		tld.writeWord(0x1);
		data.writeWTLD(tld, 0x2);//Client type
		tld = new ByteBuffer(512);
		tld.writeStringUTF8("Bimoid [Android]");
		data.writeWTLD(tld, 0x3);//Client name
		tld = new ByteBuffer(512);
		String[] raw = resources.VERSION.split("\\.");
		tld.writeWord(Integer.parseInt(raw[0]));//
		tld.writeWord(Integer.parseInt(raw[1]));
		tld.writeWord(Integer.parseInt(raw[2]));
		tld.writeWord(Integer.parseInt(raw[3]));
		data.writeWTLD(tld, 0x4);//Client version
		tld = new ByteBuffer(2);
		tld.writeWord(Locale.getPreparedLanguageCode());
		data.writeWTLD(tld, 0x5);//Client language
		tld = new ByteBuffer(512);
		tld.writeStringUTF8("Android "+resources.OS_VERSION_STR+" ("+resources.SOFTWARE_STR+")["+resources.DEVICE_STR+"]");
		data.writeWTLD(tld, 0x6);//OS name
		return BEX.createBex(seq, 0x3, 0x3, 0x0, data);
	}
	public static ByteBuffer createSetStatus(int seq, int status, String desc, int extended_pic, String extended_desc){
		
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeDWord(status);
		data.writeWTLD(tld, 0x1);
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(desc);
		data.writeWTLD(tld, 0x2);
		tld = new ByteBuffer(4);
		tld.writeDWord(extended_pic);
		data.writeWTLD(tld, 0x3);
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(extended_desc);
		data.writeWTLD(tld, 0x4);
		return BEX.createBex(seq, 0x3, 0x4, 0x0, data);
	}
	public static ByteBuffer createMessage(int seq, int unique_id, String account, String message, boolean need_report, int transport_id){
		ByteBuffer data = new ByteBuffer(0x8000);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//To account
		tld = new ByteBuffer(512);
		tld.writeDWord(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		tld = new ByteBuffer(512);
		tld.writeDWord(0x1);
		data.writeWTLD(tld, 0x3);//Message type (UTF-8)
		tld = new ByteBuffer(0x6000);
		tld.writeStringUTF8(message);
		data.writeWTLD(tld, 0x4);//Message data
		if(need_report){
			tld = new ByteBuffer(1);
			data.writeWTLD(tld, 0x5);//Report flag
		}
		if(transport_id != -1){
			tld = new ByteBuffer(4);
			tld.writeDWord(transport_id);
			data.writeWTLD(tld, 0x1001);//Transport ID
		}
		return BEX.createBex(seq, 0x4, 0x6, 0x0, data);
	}
	public static ByteBuffer createDetailsRequest(int seq, String account, int reference){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		return BEX.createBex(seq, 0x5, 0x3, reference, data);
	}
	public static ByteBuffer createDetailsRequest(int seq, Contact contact, int reference){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(contact.getID());
		data.writeWTLD(tld, 0x1);//Account
		if(contact.itIsTransport()){
			tld = new ByteBuffer(4);
			tld.writeDWord(contact.getTransportId());
			data.writeWTLD(tld, 0x1001);//Transport ID
		}
		return BEX.createBex(seq, 0x5, 0x3, reference, data);
	}
	public static ByteBuffer createMessageReport(int seq, String account, int unique_id){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(512);
		tld.writeDWord(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		return BEX.createBex(seq, 0x4, 0x8, 0x0, data);
	}
	public static ByteBuffer createTypingNotify(int seq, String account, int value, int tid){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(512);
		tld.writeDWord(0x1);
		data.writeWTLD(tld, 0x2);//Notify type
		tld = new ByteBuffer(512);
		tld.writeDWord(value);
		data.writeWTLD(tld, 0x3);//Notify value
		if(tid != -1){
			tld = new ByteBuffer(4);
			tld.writeDWord(tid);
			data.writeWTLD(tld, 0x1001);//Transport ID
		}
		return BEX.createBex(seq, 0x4, 0x9, 0x0, data);
	}
	public static ByteBuffer createAuthReply(int seq, String account, int auth, int tid){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(512);
		tld.writeWord(auth);
		data.writeWTLD(tld, 0x2);//Auth code
		if(tid != -1){
			tld = new ByteBuffer(4);
			tld.writeDWord(tid);
			data.writeWTLD(tld, 0x1001);//Transport ID
		}
		return BEX.createBex(seq, 0x2, 0xE, 0x0, data);
	}
	public static ByteBuffer createAuthReq(int seq, String account, String reason, int tid){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(reason);
		data.writeWTLD(tld, 0x2);//Auth reason
		if(tid != -1){
			tld = new ByteBuffer(4);
			tld.writeDWord(tid);
			data.writeWTLD(tld, 0x1001);//Transport ID
		}
		return BEX.createBex(seq, 0x2, 0xD, 0x0, data);
	}
	public static ByteBuffer createAuthRevoke(int seq, String account, String reason, int tid){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(2048);
		tld.writeStringUTF8(reason);
		data.writeWTLD(tld, 0x2);//Reason
		if(tid != -1){
			tld = new ByteBuffer(4);
			tld.writeDWord(tid);
			data.writeWTLD(tld, 0x1001);//Transport ID
		}
		return BEX.createBex(seq, 0x2, 0xF, 0x0, data);
	}
	public static ByteBuffer createSearchRequest(int seq, AccountInfoContainer criteries){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		if(criteries.nick_name.length() > 0){
			tld.writeStringUTF8(criteries.nick_name);
			data.writeWTLD(tld, 0x3);
		}
		if(criteries.first_name.length() > 0){
			tld = new ByteBuffer(512);
			tld.writeStringUTF8(criteries.first_name);
			data.writeWTLD(tld, 0x4);
		}
		if(criteries.last_name.length() > 0){
			tld = new ByteBuffer(512);
			tld.writeStringUTF8(criteries.last_name);
			data.writeWTLD(tld, 0x5);
		}
		if(criteries.country_ > 0){
			tld = new ByteBuffer(2);
			tld.writeWord(criteries.country_);
			data.writeWTLD(tld, 0x6);
		}
		if(criteries.city.length() > 0){
			tld = new ByteBuffer(512);
			tld.writeStringUTF8(criteries.city);
			data.writeWTLD(tld, 0x7);
		}
		if(criteries.language_ > 0){
			tld = new ByteBuffer(2);
			tld.writeWord(criteries.language_);
			data.writeWTLD(tld, 0x8);
		}
		if(criteries.gender_ > 0){
			tld = new ByteBuffer(1);
			tld.writeByte((byte)criteries.gender_);
			data.writeWTLD(tld, 0x9);
		}
		if(criteries.age_min > 0){
			tld = new ByteBuffer(1);
			tld.writeByte((byte)criteries.age_min);
			data.writeWTLD(tld, 0xA);
		}
		if(criteries.age_max > 0){
			tld = new ByteBuffer(1);
			tld.writeByte((byte)criteries.age_max);
			data.writeWTLD(tld, 0xB);
		}
		if(criteries.zodiac_ > 0){
			tld = new ByteBuffer(1);
			tld.writeByte((byte)criteries.zodiac_);
			data.writeWTLD(tld, 0xC);
		}
		if(criteries.interests.length() > 0){
			tld = new ByteBuffer();
			tld.writeStringUTF8(criteries.interests);
			data.writeWTLD(tld, 0xD);
		}
		if(criteries.online_){
			data.writeDWord(0xE);
			data.writeDWord(0x0);
		}
		return BEX.createBex(seq, 0x5, 0x7, 0x0, data);
	}
	public static ByteBuffer createFileTransferAnswer(int seq, String account, byte[] unique_id, int code, String host, int port){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(8);
		tld.write(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		tld = new ByteBuffer(2);
		tld.writeWord(code);
		data.writeWTLD(tld, 0x3);//Reply code
		tld = new ByteBuffer(128);
		tld.writeStringUTF8(host);
		data.writeWTLD(tld, 0x4);//Local host
		tld = new ByteBuffer(4);
		tld.writeDWord(port);
		data.writeWTLD(tld, 0x5);//Local port
		return BEX.createBex(seq, 0x7, 0x4, 0x0, data);
	}
	public static ByteBuffer createDIR_PROX_HELLO(int seq, String account, byte[] unique_id){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(8);
		tld.write(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		return BEX.createBex(seq, 0x7, 0x102, 0x0, data);
	}
	public static ByteBuffer createDIR_PROX_FILE_REPLY(int seq, String account, byte[] unique_id){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(8);
		tld.write(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		tld = new ByteBuffer(8);
		tld.writeLong(0);
		data.writeWTLD(tld, 0x3);//Resume position
		return BEX.createBex(seq, 0x7, 0x104, 0x0, data);
	}
	public static ByteBuffer createDIR_PROX_FILE_HEADER(int seq, String account, byte[] unique_id, long file_size, String file_name){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(8);
		tld.write(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		tld = new ByteBuffer(8);
		tld.writeLong(file_size);
		data.writeWTLD(tld, 0x3);//File size
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(file_name);
		data.writeWTLD(tld, 0x4);//File name
		return BEX.createBex(seq, 0x7, 0x103, 0x0, data);
	}
	public static ByteBuffer createDIR_PROX_FILE_DATA(int seq, String account, byte[] unique_id, boolean is_last_file, boolean is_last_part_of_file, byte[] file_data, int data_length){
		ByteBuffer data = new ByteBuffer(0x1000, true);
		ByteBuffer tld = new ByteBuffer(512, true);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(8);
		tld.write(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		tld = new ByteBuffer(1);
		tld.writeBoolean(is_last_file);
		data.writeWTLD(tld, 0x3);//Is last file
		tld = new ByteBuffer(1);
		tld.writeBoolean(is_last_part_of_file);
		data.writeWTLD(tld, 0x4);//Is last part of the file
		tld = new ByteBuffer(data_length, true);
		tld.write(file_data, data_length);
		data.writeWTLD(tld, 0x5);//File data
		return BEX.createBex(seq, 0x7, 0x105, 0x0, data);
	}
	public static ByteBuffer createFT_CONTROL(int seq, String account, byte[] unique_id, int code){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(8);
		tld.write(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		tld = new ByteBuffer(2);
		tld.writeWord(code);
		data.writeWTLD(tld, 0x3);//Code
		return BEX.createBex(seq, 0x7, 0x5, 0x0, data);
	}
	public static ByteBuffer createFileTransferAsk(int seq, String account, byte[] unique_id, int files_count, long total_size,
												String file_name, String direct_ip, int direct_port, String proxy_ip, int proxy_port){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeStringUTF8(account);
		data.writeWTLD(tld, 0x1);//Account
		tld = new ByteBuffer(8);
		tld.write(unique_id);
		data.writeWTLD(tld, 0x2);//Unique ID
		tld = new ByteBuffer(4);
		tld.writeDWord(files_count);
		data.writeWTLD(tld, 0x3);//Files count
		tld = new ByteBuffer(8);
		tld.writeLong(total_size);
		data.writeWTLD(tld, 0x4);//Total size
		tld = new ByteBuffer(512);
		tld.writeStringUTF8(file_name);
		data.writeWTLD(tld, 0x5);//File name
		if(direct_ip != null){
			tld = new ByteBuffer(512);
			tld.writeStringUTF8(direct_ip);
			data.writeWTLD(tld, 0x6);//Local IP
			tld = new ByteBuffer(4);
			tld.writeDWord(direct_port);
			data.writeWTLD(tld, 0x7);//Local port
		}
		if(proxy_ip != null){
			tld = new ByteBuffer(512);
			tld.writeStringUTF8(proxy_ip);
			data.writeWTLD(tld, 0x8);//Proxy IP
			tld = new ByteBuffer(4);
			tld.writeDWord(proxy_port);
			data.writeWTLD(tld, 0x9);//Proxy port
		}
		return BEX.createBex(seq, 0x7, 0x3, 0x0, data);
	}
	public static ByteBuffer createTP_CLI_SETTINGS(int seq, int transport_id, String password, String host, int port, int id, TransportSettings settings){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeDWord(transport_id);
		data.writeWTLD(tld, 0x1);//Transport ID
		
		try{
			tld = new ByteBuffer(1024);
			tld.writeStringUTF8(new String(Base64Coder.encode(password.getBytes("utf8"))));
			data.writeWTLD(tld, 0x2);//Password
		}catch(Exception e){}
		
		tld = new ByteBuffer(1024);
		tld.writeStringUTF8(host);
		data.writeWTLD(tld, 0x3);//Host
		tld = new ByteBuffer(1024);
		tld.writeDWord(port);
		data.writeWTLD(tld, 0x4);//Password
		
		//Settings table
		tld = new ByteBuffer();
		tld.write(settings.serialize(true));
		data.writeWTLD(tld, 0x5);
		
		return BEX.createBex(seq, 0x8, 0x4, id, data);
	}
	public static ByteBuffer createTP_CLI_MANAGE(int seq, int transport_id, int code, int status, int additional_status, String additional_status_description){
		ByteBuffer data = new ByteBuffer(1024);
		ByteBuffer tld = new ByteBuffer(512);
		tld.writeDWord(transport_id);
		data.writeWTLD(tld, 0x1);//Transport ID
		tld = new ByteBuffer(1024);
		tld.writeWord(code);
		data.writeWTLD(tld, 0x2);//Managing code
		tld = new ByteBuffer(1024);
		tld.writeDWord(status);
		data.writeWTLD(tld, 0x3);//Status
		tld = new ByteBuffer(1024);
		tld.writeDWord(additional_status);
		data.writeWTLD(tld, 0x4);//Additional status
		tld = new ByteBuffer(1024);
		tld.writeStringUTF8(additional_status_description);
		data.writeWTLD(tld, 0x5);//Additional status description
		//Log.e("TP_CLI_MANAGE", utilities.convertToHex(data.getBytes()));
		return BEX.createBex(seq, 0x8, 0x6, 0x0, data);
	}
}
