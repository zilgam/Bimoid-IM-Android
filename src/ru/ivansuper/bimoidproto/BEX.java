package ru.ivansuper.bimoidproto;

import ru.ivansuper.socket.ByteBuffer;
import ru.ivansuper.socket.ByteCache;

public class BEX {
	private int type = 0;
	private int subtype = 0;
	private int id = 0;
	private int length = 0;
	private byte[] data;
	
	public BEX(ByteBuffer data){
		data.skip(5);
		type = data.readWord();
		subtype = data.readWord();
		id = data.readDWord();
		length = data.readDWord();
		if(length > 0) this.data = data.readBytes(length);
	}
	public BEX(ByteBuffer data, boolean plomb){
		data.skip(5);
		type = data.readWord();
		subtype = data.readWord();
		id = data.readDWord();
		length = data.readDWord();
		if(length > 0) this.data = data.readBytesA(length);
	}
	public int getType(){
		return type;
	}
	public int getSubType(){
		return subtype;
	}
	public int getID(){
		return id;
	}
	public int getLength(){
		return length;
	}
	public ByteBuffer getData(){
		return new ByteBuffer(data);
	}
	public ByteBuffer getDataByRef(){
		return new ByteBuffer(data, true, length);
	}
	public byte[] getRawData(){
		return data;
	}
	public static ByteBuffer createBex(int seq, int type, int subtype, int id, ByteBuffer data){
		ByteBuffer bex = new ByteBuffer();
		bex.writeByte((byte)0x23);
		bex.writeDWord(seq);
		bex.writeWord(type);
		bex.writeWord(subtype);
		bex.writeDWord(id);
		bex.writeDWord(data.writePos);
		bex.writeByteBuffer(data);
		return bex;
	}
	public static ByteBuffer createEmptyBex(int seq, int type, int subtype, int id){
		ByteBuffer bex = new ByteBuffer(17);
		bex.writeByte((byte)0x23);
		bex.writeDWord(seq);
		bex.writeWord(type);
		bex.writeWord(subtype);
		bex.writeDWord(id);
		bex.writeDWord(0x0);
		return bex;
	}
	public void recycle(){
		ByteCache.recycle(data);
	}
}
