package ru.ivansuper.bimoidproto;

import ru.ivansuper.socket.ByteBuffer;
import ru.ivansuper.socket.ByteCache;

public class wTLD {
	private int type = 0;
	private int length = 0;
	private byte[] data;
	public wTLD(ByteBuffer data){
		type = data.readDWord();
		length = data.readDWord();
		this.data = data.readBytesA(length);
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
	public void recycle() {
		ByteCache.recycle(data);
	}
}
