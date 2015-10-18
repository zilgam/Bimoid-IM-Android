package ru.ivansuper.bimoidproto;

import android.util.Log;
import ru.ivansuper.bimoidim.utilities;
import ru.ivansuper.bimoidproto.transports.Transport;
import ru.ivansuper.bimoidproto.transports.TransportParams;
import ru.ivansuper.socket.ByteBuffer;

public class RosterOperation {
	public static final int CONTACT_ADD = 0;
	public static final int CONTACT_MODIFY = 1;
	public static final int CONTACT_REMOVE = 2;
	public static final int GROUP_ADD = 3;
	public static final int GROUP_MODIFY = 4;
	public static final int GROUP_REMOVE = 5;
	public static final int TRANSPORT_ADD = 6;
	public static final int TRANSPORT_REMOVE = 7;
	public static final int NOTE_ADD = 8;
	public static final int NOTE_MODIFY = 9;
	public static final int NOTE_REMOVE = 10;
	public RosterItem item;
	public int id;
	public int parent_id;
	public int tid = -1;
	public String account;
	public String name;
	public int privacy;
	public boolean auth_flag;
	public boolean general_flag;
	public int operation;
	public int operation_id;
	public String transport_UUID;
	public String transport_account;
	public String transport_pass;
	public String transport_friendly_name;
	public TransportParams transport_params;
	public String NOTE_NAME;
	public String NOTE_DESC;
	public byte NOTE_TYPE;
	public long NOTE_TIMESTAMP;
	public byte[] NOTE_HASH;
	public RosterOperation(int operation){
		this.operation = operation;
		operation_id = (int)(System.currentTimeMillis()/1000);
	}
	public RosterOperation(Contact contact, int operation){
		this.operation = operation;
		id = contact.getItemId();
		parent_id = contact.getGroupId();
		tid = contact.getTransportId();
		account = contact.getID();
		name = contact.getName();
		privacy = contact.getPrivacy();
		auth_flag = contact.auth_flag;
		general_flag = contact.general_flag;
		item = contact;
		operation_id = (int)(System.currentTimeMillis()/1000);
	}
	public RosterOperation(Group group, int operation){
		this.operation = operation;
		id = group.getItemId();
		parent_id = group.getGroupId();
		name = group.getName();
		item = group;
		operation_id = (int)(System.currentTimeMillis()/1000);
	}
	public RosterOperation(Transport transport, int operation){
		this.operation = operation;
		id = transport.getItemId();
		parent_id = transport.getGroupId();
		name = transport.getName();
		item = transport;
		operation_id = (int)(System.currentTimeMillis()/1000);
	}
	public RosterOperation(NoteItem note, int operation){
		this.operation = operation;
		id = note.getItemId();
		parent_id = note.getGroupId();
		NOTE_NAME = note.name;
		NOTE_DESC = note.TEXT;
		NOTE_TYPE = note.TYPE;
		NOTE_TIMESTAMP = note.TIMESTAMP;
		NOTE_HASH = note.HASH;
		item = note;
		operation_id = (int)(System.currentTimeMillis()/1000);
	}
	public ByteBuffer buildOperationPacket(int sequence){
		ByteBuffer data = new ByteBuffer();
			switch(operation){
			case NOTE_ADD:
				ByteBuffer tld = new ByteBuffer(2);
				tld.writeWord(RosterItem.CL_ITEM_TYPE_NOTE);
				data.writeWTLD(tld, 0x1);
				
				tld = new ByteBuffer(4);
				tld.writeDWord(parent_id);
				data.writeWTLD(tld, 0x2);
				
				ByteBuffer stlds = new ByteBuffer();
				tld = new ByteBuffer();
				tld.writeStringUTF8(NOTE_NAME);
				stlds.writeSTLD(tld, 0x2001);
				tld = new ByteBuffer();
				tld.writeByte(NOTE_TYPE);
				stlds.writeSTLD(tld, 0x2002);
				if(NOTE_DESC != null){
					if(NOTE_DESC.length() > 0){
						tld = new ByteBuffer();
						tld.writeStringUTF8(NOTE_DESC);
						stlds.writeSTLD(tld, 0x2003);
					}
				}
				if(NOTE_TIMESTAMP != 0){
					tld = new ByteBuffer();
					tld.writeLong(NOTE_TIMESTAMP/1000);
					stlds.writeSTLD(tld, 0x2004);
				}
				data.writeWTLD(stlds, 0x3);
				return BEX.createBex(sequence, 0x2, 0x7, operation_id, data);
			case NOTE_MODIFY:
				tld = new ByteBuffer(4);
				tld.writeDWord(id);
				data.writeWTLD(tld, 0x1);
				
				tld = new ByteBuffer(4);
				tld.writeDWord(parent_id);
				data.writeWTLD(tld, 0x2);
				
				stlds = new ByteBuffer();
				tld = new ByteBuffer();
				tld.writeStringUTF8(NOTE_NAME);
				stlds.writeSTLD(tld, 0x2001);
				tld = new ByteBuffer();
				tld.writeByte(NOTE_TYPE);
				stlds.writeSTLD(tld, 0x2002);
				if(NOTE_DESC != null)
					if(NOTE_DESC.length() > 0){
						tld = new ByteBuffer();
						tld.writeStringUTF8(NOTE_DESC);
						stlds.writeSTLD(tld, 0x2003);
					}
				if(NOTE_TIMESTAMP != 0){
					tld = new ByteBuffer();
					tld.writeLong(NOTE_TIMESTAMP/1000);
					stlds.writeSTLD(tld, 0x2004);
				}
				if(NOTE_HASH != null)
					if(NOTE_HASH.length > 0){
						tld = new ByteBuffer();
						tld.write(NOTE_HASH);
						stlds.writeSTLD(tld, 0x2005);
					}
				data.writeWTLD(stlds, 0x3);
				return BEX.createBex(sequence, 0x2, 0xB, operation_id, data);
			case NOTE_REMOVE:
				tld = new ByteBuffer(4);
				tld.writeDWord(id);
				data.writeWTLD(tld, 0x1);
				return BEX.createBex(sequence, 0x2, 0x9, operation_id, data);
			case TRANSPORT_ADD:
				tld = new ByteBuffer(2);
				tld.writeWord(RosterItem.TRANSPORT_ITEM);
				data.writeWTLD(tld, 0x1);
				
				tld = new ByteBuffer(4);
				tld.writeDWord(parent_id);
				data.writeWTLD(tld, 0x2);
				
				stlds = new ByteBuffer();
				tld = new ByteBuffer();
				tld.write(utilities.hexStringToBytesArray(transport_UUID));
				stlds.writeSTLD(tld, 0x1002);
				tld = new ByteBuffer();
				tld.writeStringUTF8(transport_account);
				stlds.writeSTLD(tld, 0x1003);
				tld = new ByteBuffer();
				tld.writeStringUTF8(transport_friendly_name);
				stlds.writeSTLD(tld, 0x1004);
				
				data.writeWTLD(stlds, 0x3);
				return BEX.createBex(sequence, 0x2, 0x7, operation_id, data);
			case TRANSPORT_REMOVE:
				tld = new ByteBuffer(4);
				tld.writeDWord(id);
				data.writeWTLD(tld, 0x1);
				return BEX.createBex(sequence, 0x2, 0x9, operation_id, data);
			case CONTACT_ADD:
				tld = new ByteBuffer(2);
				tld.writeWord(RosterItem.OBIMP_CONTACT);
				data.writeWTLD(tld, 0x1);
				tld = new ByteBuffer(4);
				tld.writeDWord(parent_id);
				data.writeWTLD(tld, 0x2);
				
				stlds = new ByteBuffer();
				tld = new ByteBuffer();
				tld.writeStringUTF8(account);
				stlds.writeSTLD(tld, 0x2);
				tld = new ByteBuffer();
				tld.writeStringUTF8(name);
				stlds.writeSTLD(tld, 0x3);
				tld = new ByteBuffer(1);
				tld.writeByte((byte)privacy);
				stlds.writeSTLD(tld, 0x4);
				tld = new ByteBuffer(0);
				stlds.writeSTLD(tld, 0x5);
				if(tid != -1){
					  tld = new ByteBuffer(4);
					  tld.writeDWord(tid);
					  stlds.writeSTLD(tld, 0x1001);
				}
				data.writeWTLD(stlds, 0x3);
				Log.e("ByteBuffer", utilities.convertToHex(data.getBytes()));
				return BEX.createBex(sequence, 0x2, 0x7, operation_id, data);
			case CONTACT_MODIFY:
				tld = new ByteBuffer(4);
				tld.writeDWord(id);
				data.writeWTLD(tld, 0x1);
				tld = new ByteBuffer(4);
				tld.writeDWord(parent_id);
				data.writeWTLD(tld, 0x2);
				stlds = new ByteBuffer();
				  tld = new ByteBuffer();
				  tld.writeStringUTF8(account);
				  stlds.writeSTLD(tld, 0x2);
				  tld = new ByteBuffer();
				  tld.writeStringUTF8(name);
				  stlds.writeSTLD(tld, 0x3);
				  tld = new ByteBuffer(1);
				  tld.writeByte((byte)privacy);
				  stlds.writeSTLD(tld, 0x4);
				if(auth_flag){
					tld = new ByteBuffer(0);
					stlds.writeSTLD(tld, 0x5);
				}
				if(general_flag){
					tld = new ByteBuffer(0);
					stlds.writeSTLD(tld, 0x6);
				}
				if(tid != -1){
					  tld = new ByteBuffer(4);
					  tld.writeDWord(tid);
					  stlds.writeSTLD(tld, 0x1001);
				}
				data.writeWTLD(stlds, 0x3);
				Log.e("ByteBuffer", utilities.convertToHex(data.getBytes()));
				return BEX.createBex(sequence, 0x2, 0xB, operation_id, data);
			case CONTACT_REMOVE:
				tld = new ByteBuffer(4);
				tld.writeDWord(id);
				data.writeWTLD(tld, 0x1);
				Log.e("ByteBuffer", utilities.convertToHex(data.getBytes()));
				return BEX.createBex(sequence, 0x2, 0x9, operation_id, data);
			case GROUP_ADD:
				tld = new ByteBuffer(2);
				tld.writeWord(RosterItem.OBIMP_GROUP);
				data.writeWTLD(tld, 0x1);
				tld = new ByteBuffer(4);
				tld.writeDWord(parent_id);
				data.writeWTLD(tld, 0x2);
				stlds = new ByteBuffer();
				tld = new ByteBuffer();
				tld.writeStringUTF8(name);
				stlds.writeSTLD(tld, 0x1);
				data.writeWTLD(stlds, 0x3);
				return BEX.createBex(sequence, 0x2, 0x7, operation_id, data);
			case GROUP_MODIFY:
				tld = new ByteBuffer(4);
				tld.writeDWord(id);
				data.writeWTLD(tld, 0x1);
				tld = new ByteBuffer(4);
				tld.writeDWord(parent_id);
				data.writeWTLD(tld, 0x2);
				stlds = new ByteBuffer();
				  tld = new ByteBuffer();
				  tld.writeStringUTF8(name);
				  stlds.writeSTLD(tld, 0x1);
				data.writeWTLD(stlds, 0x3);
				return BEX.createBex(sequence, 0x2, 0xB, operation_id, data);
			case GROUP_REMOVE:
				tld = new ByteBuffer(4);
				tld.writeDWord(id);
				data.writeWTLD(tld, 0x1);
				return BEX.createBex(sequence, 0x2, 0x9, operation_id, data);
			}
		return null;
	}
}
