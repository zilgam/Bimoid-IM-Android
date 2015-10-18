package ru.ivansuper.bimoidproto;

import java.io.DataInputStream;
import java.util.Vector;

import ru.ivansuper.socket.ByteBuffer;

public class sTLDList {
	private int count = 0;
	private Vector<sTLD> list = new Vector<sTLD>();
	public sTLDList(ByteBuffer data, int block_size){
		int readed = 0;
		while(readed < block_size){
			sTLD tld = new sTLD(data);
			list.add(tld);
			readed += 4 + tld.getLength();
			count++;
		}
	}
	public sTLDList(ByteBuffer data, int items_count, int a){
		int readed = 0;
		while(count < items_count){
			sTLD tld = new sTLD(data);
			list.add(tld);
			readed += 4 + tld.getLength();
			count++;
		}
	}
	public sTLDList(DataInputStream dis, int tlds_count){
		for(int i=0; i<tlds_count; i++){
			try {
				sTLD tld = new sTLD(dis);
				list.add(tld);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public sTLD getTLD(int type){
		for(int i=0; i<list.size(); i++){
			sTLD tld = list.get(i);
			if(tld.getType() == type){
				return tld;
			}
		}
		return null;
	}
	public Vector<sTLD> getTLDs(){
		return list;
	}
}
