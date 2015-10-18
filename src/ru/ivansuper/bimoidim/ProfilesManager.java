package ru.ivansuper.bimoidim;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Vector;

import android.util.Log;

import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.MessagesDump;
import ru.ivansuper.bimoidproto.sTLD;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.socket.ByteBuffer;

public class ProfilesManager {
	public Vector<BimoidProfile> list = new Vector<BimoidProfile>();
	private BimoidService service;
	private File profiles_database;
	public ProfilesManager(BimoidService svc){
		service = svc;
		profiles_database = new File(resources.DATA_PATH+"profiles.bin");
		if(!profiles_database.exists()){
			try{
				profiles_database.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try {
			loadProfiles();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("ProfilesManager", "Can't load profiles from database!");
		}
	}
	public void addProfile(BimoidProfile profile){
		list.add(profile);
		service.handleContactListNeedRebuild();
		service.handleProfileStatusChanged();
	}
	public void disconnectAll(){
		for(int i=0; i<list.size(); i++){
			BimoidProfile profile = list.get(i);
			if(profile.connected || profile.connecting) profile.disconnect();
		}
	}
	public synchronized void saveProfiles() throws Exception{
		if(list.size() < 1){
			try{
				profiles_database.delete();
				profiles_database.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
			}
			return;
		}
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(profiles_database));
		dos.writeShort(list.size());
		for(int i=0; i<list.size(); i++){
			BimoidProfile profile = list.get(i);
			ByteBuffer buf = new ByteBuffer(512);
			ByteBuffer tld = new ByteBuffer(512);
			tld.write(proceedISEM_A(profile.ID+"@"+profile.server));
			buf.writeSTLD(tld, 0x0);
			dos.write(buf.getBytes());
			tld = new ByteBuffer(512);
			tld.write(proceedISEM_A(profile.password));
			buf.reset(512);
			buf.writeSTLD(tld, 0x0);
			dos.write(buf.getBytes());
			tld = new ByteBuffer(512);
			tld.write(proceedISEM_A(profile.nickname));
			buf.reset(512);
			buf.writeSTLD(tld, 0x0);
			dos.write(buf.getBytes());
		}
		dos.close();
	}
	public synchronized void loadProfiles() throws Exception{
		if(profiles_database.length() < 5) return;
		DataInputStream dis = new DataInputStream(new FileInputStream(profiles_database));
		int profiles_count = dis.readShort();
		for(int i=0; i<profiles_count; i++){
			sTLD tld = new sTLD(dis);
			String id = proceedISEM_B(tld.getData().readBytes(tld.getLength()));
			tld = new sTLD(dis);
			String pass = proceedISEM_B(tld.getData().readBytes(tld.getLength()));
			BimoidProfile profile = new BimoidProfile(service, id, pass);
			tld = new sTLD(dis);
			String nick = proceedISEM_B(tld.getData().readBytes(tld.getLength()));
			profile.nickname = nick;
			list.add(profile);
		}
		dis.close();
	}
	public boolean isExist(String ID){
		for(int i=0; i<list.size(); i++){
			if(list.get(i).ID.equals(ID)) return true;
		}
		return false;
	}
	public BimoidProfile getProfileByID(String ID){
		for(int i=0; i<list.size(); i++){
			BimoidProfile profile = list.get(i);
			if(profile.ID.equals(ID)) return profile;
		}
		return null;
	}
	public void removeProfileByID(String ID){
		for(int i=0; i<list.size(); i++){
			BimoidProfile profile = list.get(i);
			if(profile.ID.equals(ID)){
				profile.disconnect();
				list.removeElementAt(i);
			}
		}
    	service.handleContactListNeedRebuild();
    	service.handleProfileStatusChanged();
	}
	public void getUnreadedDump(MessagesDump dump){
		for(int i=0; i<list.size(); i++){
			list.get(i).getUnreaded(dump);
		}
	}
	public int getConnectedProfilesCount(){
		int connected_count = 0;
		for(int i=0; i<list.size(); i++){
			if(list.get(i).connected) connected_count++;
		}
		return connected_count;
	}
	public BimoidProfile getFirstConnectedProfile(){
		for(int i=0; i<list.size(); i++){
			BimoidProfile profile = list.get(i);
			if(profile.connected) return profile;
		}
		return null;
	}
	public static byte[] proceedISEM_A(String source) throws Exception{
		byte[] stepA = source.getBytes("windows1251");
		String stepB = utilities.convertToHex(stepA);
		byte[] stepC = stepB.getBytes("windows1251");
		byte j = 0x2;
		for(int i=0; i<stepC.length; i++){
			stepC[i] = (byte) (stepC[i] - j);
			j += 2;
		}
		return stepC;
	}
	public static String proceedISEM_B(byte[] source) throws Exception{
		byte j = 0x2;
		for(int i=0; i<source.length; i++){
			source[i] = (byte) (source[i] + j);
			j += 2;
		}
		String stepA = new String(source, "windows1251");
		byte[] stepB = utilities.hexStringToBytesArray(stepA);
		String stepC = new String(stepB, "windows1251");
		return stepC;
	}
}
