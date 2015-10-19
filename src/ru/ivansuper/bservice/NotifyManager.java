package ru.ivansuper.bservice;

import java.util.Vector;

public class NotifyManager {
	private static final Vector<MNotification> mList = new Vector<MNotification>();
	public static final synchronized void put(MNotification n){
		remove(n.schema);
		mList.insertElementAt(n, 0);
	}
	public static final synchronized MNotification get(){
		return mList.get(0);
	}
	public static final synchronized void remove(String scheme){
		for(int i=0; i<mList.size(); i++){
			final MNotification n = mList.get(i);
			if(n.schema.equals(scheme)){
				mList.remove(i);
				i--;
			}
		}
	}
	public static final synchronized void clear(){
		mList.clear();
	}
	public static final synchronized int count(){
		return mList.size();
	}
}

