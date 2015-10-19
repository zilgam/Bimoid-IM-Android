package ru.ivansuper.bimoidproto;

import java.util.Vector;

public class Simple_sTLDList {
	private final Vector<sTLD> list = new Vector<sTLD>();
	public synchronized void put(sTLD stld){
		list.add(stld);
	}
	public synchronized sTLD get(int type){
		for(sTLD stld: list){
			if(stld.getType() == type)
				return stld;
		}
		return null;
	}
	public synchronized int getCount(){
		return list.size();
	}
}
