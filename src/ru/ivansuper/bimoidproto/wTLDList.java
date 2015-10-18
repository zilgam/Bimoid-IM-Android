package ru.ivansuper.bimoidproto;

import java.util.Vector;

import android.util.Log;

import ru.ivansuper.socket.ByteBuffer;

public class wTLDList {
	private int count = 0;
	private Vector<wTLD> list = new Vector<wTLD>();
	public wTLDList(ByteBuffer data, int block_size){
		int readed = 0;
		while(readed < block_size){
			wTLD tld = new wTLD(data);
			list.add(tld);
			readed += 8 + tld.getLength();
			count++;
		}
	}
	public wTLD getTLD(int type){
		for(int i=0; i<list.size(); i++){
			wTLD tld = list.get(i);
			if(tld.getType() == type){
				return tld;
			}
		}
		return null;
	}
	public void recycle() {
		for(int i=0; i<list.size(); i++){
			list.get(i).recycle();
		}
	}
}
