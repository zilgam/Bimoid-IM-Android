package ru.ivansuper.bimoidproto;

import java.io.DataInputStream;

import ru.ivansuper.socket.ByteBuffer;

public class sTLD {
	private int type = 0;
	private int length = 0;
	private byte[] data;
	public sTLD(ByteBuffer data){
		type = data.readWord();
		length = data.readWord();
		this.data = data.readBytes(length);
	}
	public sTLD(DataInputStream dis) throws Exception{
		type = dis.readShort();
		length = dis.readShort();
		data = new byte[length];
		dis.read(data, 0, length);
	}
	public int getType(){
		return type;
	}
	public int getLength(){
		return length;
	}
	public ByteBuffer getData(){
		return new ByteBuffer(data);
	}
}
