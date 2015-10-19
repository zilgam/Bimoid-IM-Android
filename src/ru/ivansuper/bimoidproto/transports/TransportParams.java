package ru.ivansuper.bimoidproto.transports;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.XStatus;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidim.utilities;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.locale.Locale;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

public class TransportParams {
	public String UUID = "";
	public String full_name = "";
	public String short_name = "";
	public String name_of_account_ids = "";
	public String default_host = "";
	public int default_port = 0;
	
	public TransportSettings mTransportSettings;
	
	public int[] status_wrapper;
	public boolean additional_status_pic;
	public int additional_status_pic_count;
	public boolean add_contacts;
	public boolean update_contacts;
	public boolean delete_contacts;
	public boolean visible_list;
	public boolean invisible_list;
	public boolean ignore_list;
	public boolean move_to_ignore;
	public boolean auth_supported;
	public boolean auth_revoke;
	public boolean message_ack;
	public boolean notification_messages;
	public boolean detail_req;
	public boolean update_details;
	public boolean search;
	public boolean avatars;
	public boolean update_avatar;
	public boolean offline_messages;
	public boolean presence_info_req;
	public Bitmap main_status_list;
	public Bitmap additional_status_list;
	public static final int[] wrapA = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
	public static final int[] wrapB = {5, 6, 14, 12, 13, 11, 7, 8, 9, 10};
	public static final String translateStatus(int status){
		switch(status){
		case 1:
			return Locale.getString("s_status_invisible");
		case 2:
			return Locale.getString("s_status_invisible_for_all");
		case 3:
			return Locale.getString("s_status_ready_for_chat");
		case 4:
			return Locale.getString("s_status_home");
		case 5:
			return Locale.getString("s_status_work");
		case 6:
			return Locale.getString("s_status_lunch");
		case 7:
			return Locale.getString("s_status_away");
		case 8:
			return Locale.getString("s_status_na");
		case 9:
			return Locale.getString("s_status_oc");
		case 10:
			return Locale.getString("s_status_dnd");
		}
		return "";
	}
	public final Drawable getLogo(){
		final Bitmap res = Bitmap.createBitmap(main_status_list, 0, 0, main_status_list.getHeight(), main_status_list.getHeight());
		res.setDensity(0);
		final BitmapDrawable drawable = new BitmapDrawable(res);
		return drawable;
	}
	public final Drawable getOnline(){
		final Bitmap res = Bitmap.createBitmap(main_status_list, main_status_list.getHeight(), 0, main_status_list.getHeight(), main_status_list.getHeight());
		res.setDensity(0);
		return new BitmapDrawable(res);
	}
	public final Drawable getOffline(){
		final Bitmap res = Bitmap.createBitmap(main_status_list, 2*main_status_list.getHeight(), 0, main_status_list.getHeight(), main_status_list.getHeight());
		res.setDensity(0);
		return new BitmapDrawable(res);
	}
	public final Drawable getConnecting(){
		final Bitmap res = Bitmap.createBitmap(main_status_list, 3*main_status_list.getHeight(), 0, main_status_list.getHeight(), main_status_list.getHeight());
		res.setDensity(0);
		return new BitmapDrawable(res);
	}
	public final Drawable getUndetermined(){
		final Bitmap res = Bitmap.createBitmap(main_status_list, 4*main_status_list.getHeight(), 0, main_status_list.getHeight(), main_status_list.getHeight());
		res.setDensity(0);
		return new BitmapDrawable(res);
	}
	public final Drawable getStatus(int code){
		if(code == -1) return getOffline();
		int pos = 1;
		for(int i=0; i<wrapA.length; i++)
			if(wrapA[i] == code){
				pos = wrapB[i];
				break;
			}
		final Bitmap res = Bitmap.createBitmap(main_status_list, pos*main_status_list.getHeight(), 0, main_status_list.getHeight(), main_status_list.getHeight());
		res.setDensity(0);
		return new BitmapDrawable(res);
	}
	public final Drawable getAddStatus(int code){
		if(code == 0) return XStatus.getIcon(0);
		int pos = code - 1;
		final Bitmap res = Bitmap.createBitmap(additional_status_list, pos*additional_status_list.getHeight(), 0, additional_status_list.getHeight(), additional_status_list.getHeight());
		res.setDensity(0);
		return new BitmapDrawable(res);
	}
	public final void save(BimoidProfile profile){
		synchronized(resources.ctx){
			DataOutputStream dos = null;
			try{
				final File dir = new File(resources.DATA_PATH+profile.ID+"/TransportParams/"+UUID+"/");
				if(!(dir.isDirectory() && dir.exists()))
					dir.mkdirs();
				final File config = new File(resources.DATA_PATH+profile.ID+"/TransportParams/"+UUID+"/config.cfg");
				dos = new DataOutputStream(new FileOutputStream(config));
				dos.writeUTF(full_name);
				dos.writeUTF(short_name);
				dos.writeUTF(name_of_account_ids);
				dos.writeUTF(default_host);
				dos.writeInt(default_port);
				dos.writeByte(status_wrapper.length);
				for(int i=0; i<status_wrapper.length; i++)
					dos.writeInt(status_wrapper[i]);
				dos.writeBoolean(additional_status_pic);
				dos.writeInt(additional_status_pic_count);
				dos.writeBoolean(add_contacts);
				dos.writeBoolean(update_contacts);
				dos.writeBoolean(delete_contacts);
				dos.writeBoolean(visible_list);
				dos.writeBoolean(invisible_list);
				dos.writeBoolean(ignore_list);
				dos.writeBoolean(move_to_ignore);
				dos.writeBoolean(auth_supported);
				dos.writeBoolean(auth_revoke);
				dos.writeBoolean(message_ack);
				dos.writeBoolean(notification_messages);
				dos.writeBoolean(detail_req);
				dos.writeBoolean(update_details);
				dos.writeBoolean(search);
				dos.writeBoolean(avatars);
				dos.writeBoolean(update_avatar);
				dos.writeBoolean(offline_messages);
				dos.writeBoolean(presence_info_req);
				
				final File res_dir = new File(resources.DATA_PATH+"TransportsRes/"+UUID);
				if(!(res_dir.isDirectory() && res_dir.exists()))
					try{
						res_dir.mkdirs();
					}catch(Exception e){}
				final File main_sts = new File(resources.DATA_PATH+"TransportsRes/"+UUID+"/main_sts.bin");
				main_status_list.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(main_sts));
				
				if(additional_status_pic){
					final File add_sts = new File(resources.DATA_PATH+"TransportsRes/"+UUID+"/add_sts.bin");
					additional_status_list.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(add_sts));
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				if(dos != null) dos.close();
			}catch(Exception e){}
			
		}
	}
	public static final void load(BimoidProfile profile){
		final File dir = new File(resources.DATA_PATH+profile.ID+"/TransportParams/");
		if(!(dir.isDirectory() && dir.exists())){
			dir.mkdirs();
			return;
		}
		profile.transport_params.clear();
		final File[] entries = dir.listFiles();
		for(int i=0; i<entries.length; i++){
			if(!entries[i].isDirectory()) continue;
			final TransportParams params = new TransportParams();
			DataInputStream dis = null;
			try{
				dis = new DataInputStream(new FileInputStream(new File(resources.DATA_PATH+profile.ID+"/TransportParams/"+entries[i].getName()+"/config.cfg")));
				params.UUID = entries[i].getName();
				params.full_name = dis.readUTF();
				params.short_name = dis.readUTF();
				params.name_of_account_ids = dis.readUTF();
				params.default_host = dis.readUTF();
				params.default_port = dis.readInt();
				final int count = dis.readByte();
				final int[] statuses = new int[count];
				for(int j=0; j<count; j++)
					statuses[j] = dis.readInt();
				params.status_wrapper = statuses;
				params.additional_status_pic = dis.readBoolean();
				params.additional_status_pic_count = dis.readInt();
				params.add_contacts = dis.readBoolean();
				params.update_contacts = dis.readBoolean();
				params.delete_contacts = dis.readBoolean();
				params.visible_list = dis.readBoolean();
				params.invisible_list = dis.readBoolean();
				params.ignore_list = dis.readBoolean();
				params.move_to_ignore = dis.readBoolean();
				params.auth_supported = dis.readBoolean();
				params.auth_revoke = dis.readBoolean();
				params.message_ack = dis.readBoolean();
				params.notification_messages = dis.readBoolean();
				params.detail_req = dis.readBoolean();
				params.update_details = dis.readBoolean();
				params.search = dis.readBoolean();
				params.avatars = dis.readBoolean();
				params.offline_messages = dis.readBoolean();
				params.presence_info_req = dis.readBoolean();
				final File main_sts = new File(resources.DATA_PATH+"TransportsRes/"+params.UUID+"/main_sts.bin");
				params.main_status_list = BitmapFactory.decodeStream(new FileInputStream(main_sts));
				if(params.main_status_list == null) throw new Exception("Error occured while loading main statuses for "+params.UUID+" transport");
				
				if(params.additional_status_pic){
					final File add_sts = new File(resources.DATA_PATH+"TransportsRes/"+params.UUID+"/add_sts.bin");
					params.additional_status_list = BitmapFactory.decodeStream(new FileInputStream(add_sts));
				}
				//if(params.additional_status_list == null) throw new Exception("Error occured while loading additional statuses for "+params.UUID+" transport");
				profile.transport_params.add(params);
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				if(dis != null) dis.close();
			}catch(Exception e){}
		}
	}
	
	public final void saveTransportSettings(BimoidProfile profile, Transport t){
		
		DataOutputStream dis = null;
		try{
			final String transport_id = "id_"+t.item_id;
			
			(new File(resources.DATA_PATH+profile.ID+"/TransportSettings/"+transport_id+"/")).mkdirs();
			
			dis = new DataOutputStream(new FileOutputStream(new File(resources.DATA_PATH+profile.ID+"/TransportSettings/"+transport_id+"/settings_table.bin")));
			
			final byte[] block = mTransportSettings.serialize(false);
			
			dis.writeInt(block.length);
			dis.write(block, 0, block.length);
			
		}catch(Throwable t1){ t1.printStackTrace(); }
		
		try{ dis.close(); }catch(Throwable t1){}
		
	}
	
	public final void readTransportSettings(BimoidProfile profile, Transport t){
		
		DataInputStream dis = null;
		try{
			final String transport_id = "id_"+t.item_id;
			
			dis = new DataInputStream(new FileInputStream(new File(resources.DATA_PATH+profile.ID+"/TransportSettings/"+transport_id+"/settings_table.bin")));
			
			final int block_length = dis.readInt();
			
			final byte[] block = new byte[block_length];
			
			dis.read(block, 0, block_length);
			
			mTransportSettings.updateValues(block);
			
		}catch(Throwable t1){ }
		
		try{ dis.close(); }catch(Throwable t1){}
		
	}
	
	public static int getPreferedSize(){
		int prefered_size = 0;
		switch(resources.dm.densityDpi){
		case DisplayMetrics.DENSITY_LOW:
			prefered_size = 16;
			break;
		case DisplayMetrics.DENSITY_MEDIUM:
			prefered_size = 24;
			break;
		case DisplayMetrics.DENSITY_HIGH:
			prefered_size = 36;
			break;
		case DisplayMetrics.DENSITY_XHIGH:
			prefered_size = 48;
			break;
		default:
			prefered_size = 16;
			break;
		}
		return prefered_size;
	}
	public static Bitmap getBitmap(String links){
		int prefered_size = getPreferedSize();
		Log.e("BitmapDownloader", "Prefered size: "+prefered_size);
		String[] lines = links.split("\n");
		if(lines.length < 1) return null;
		if(lines.length == 1){
			String[] params = lines[0].split(",");
			//int size = Integer.parseInt(params[0]);
			return utilities.downloadImage(params[1]);
		}
		Log.e("BitmapDownloader", "Links: "+lines.length);
		int max_available = 0;
		int min_available = 100;
		int max_available2 = 0;
		String max_link = "";
		String max_link2 = "";
		String min_link = "";
		for(int i=lines.length-1; i>=0; i--){
			String[] params = lines[i].split(",");
			int size = Integer.parseInt(params[0]);
			Log.e("BitmapDownloader", "Checking size: "+size);
			if(size < min_available){
				min_available = size;
				min_link = params[1];
				Log.e("BitmapDownloader", "Recorded as min");
			}
			if(max_available2 < size){
				max_available2 = size;
				max_link2 = params[1];
			}
			if(size > prefered_size){
				Log.e("BitmapDownloader", "Size too big");
				continue;
			}
			if(max_available < size){
				max_available = size;
				max_link = params[1];
				Log.e("BitmapDownloader", "Recorded as max");
			}
		}
		Log.e("BitmapDownloader", "Max available: "+max_available);
		Log.e("BitmapDownloader", "Global max available: "+max_available2);
		if(max_available2 > prefered_size){
			return utilities.downloadImage(max_link2, (float)prefered_size/max_available2);
		}else{
			if(max_available == 0){
				return utilities.downloadImage(min_link);
			}else{
				return utilities.downloadImage(max_link);
			}
		}
	}
}
