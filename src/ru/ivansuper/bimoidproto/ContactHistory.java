package ru.ivansuper.bimoidproto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import android.util.Log;

import ru.ivansuper.bimoidim.PreferenceTable;
import ru.ivansuper.bimoidim.resources;

public class ContactHistory {
	private Contact contact;
	private File history_database;
	private File history_cache;
	private boolean historyPreLoaded;
	private Vector<HistoryItem> history = new Vector<HistoryItem>();
	public ContactHistory(Contact contact){
		this.contact = contact;
		history_database = new File(resources.DATA_PATH+contact.getProfile().ID+"/"+contact.getID());
		if(!history_database.exists()){
			try {
				history_database.createNewFile();
			} catch (IOException e) {
				Log.e("HISTORY", "Can't create contact history database!");
				//e.printStackTrace();
			}
		}
		history_cache = new File(resources.DATA_PATH+contact.getProfile().ID+"/"+contact.getID()+".cache");
		if(!history_cache.exists()){
			try {
				history_cache.createNewFile();
			} catch (IOException e) {
				Log.e("HISTORY", "Can't create contact history database cache!");
				//e.printStackTrace();
			}
		}
	}
	public void putMessage(HistoryItem message, boolean write_to_history){
		history.add(message);
		if(write_to_history && PreferenceTable.store_history){
			writeMessageToHistory(message);
		}
	}
	private void writeMessageToHistory(HistoryItem message){
		dumpCache();
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(history_database, true));
			dos.writeByte((byte)message.direction);
			dos.writeLong(message.date);
			byte[] msg = message.message.getBytes("windows1251");
			dos.writeInt(msg.length);
			dos.write(msg);
			dos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void dumpCache(){
		int i = 10;
		int sz = history.size();
		int start = 0;
		if(sz < i){
			i = sz;
		}else{
		    start = sz-i;
		}
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(history_cache));
			for(int j=0; j<i; j++){
				HistoryItem message = history.get(start);
				dos.writeByte((byte)message.direction);
				dos.writeLong(message.date);
				byte[] msg = message.message.getBytes("windows1251");
				dos.writeInt(msg.length);
				dos.write(msg);
				start++;
			}
			dos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void preloadCache(){
		Vector<HistoryItem> temp = new Vector<HistoryItem>();
		try {
		if(!historyPreLoaded){
			history.clear();
			if(history_cache.length() > 0){
				DataInputStream dis = new DataInputStream(new FileInputStream(history_cache));
				while(true){
					//Log.v("CACHE_MANAGER", "=========NEXT ITEM PROCESSING=========");
					if(dis.available() > 0){
						int direction = dis.readByte();
						long time = dis.readLong();
						int msgLen = dis.readInt();
						byte[] message = new byte[msgLen];
						dis.read(message, 0, msgLen);
						String msg = new String(message, "windows1251");
						HistoryItem item = new HistoryItem(time);
						item.direction = direction;
						item.message = msg;
						item.contact = contact;
						temp.add(item);
						if(temp.size() > 10){
							temp.remove(0);
						}
					}else{
						try {
							dis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					}
				}
				history = (Vector<HistoryItem>)temp.clone();
				contact.getProfile().svc.refreshChat();
			}
			historyPreLoaded = true;
		}
		} catch (Exception e) {
			//profile.makeShortToast("Ошибка при чтении истории");
			e.printStackTrace();
		}
		//}
		if(!historyPreLoaded){
			history.addAll(temp);
			historyPreLoaded = true;
		}
	}
	public Vector<HistoryItem> getMessageList(){
		return history;
	}
	public void deleteHistory(){
		try{
			history_database.delete();
			history_cache.delete();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
