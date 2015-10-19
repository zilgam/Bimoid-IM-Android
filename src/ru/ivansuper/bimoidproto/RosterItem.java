package ru.ivansuper.bimoidproto;

import android.util.Log;
import ru.ivansuper.bimoidim.utilities;

public class RosterItem implements Comparable<RosterItem> {
	public static final int OBIMP_GROUP = 0x0001;
	public static final int OBIMP_CONTACT = 0x0002;
	public static final int TRANSPORT_ITEM = 0x0003;
	public static final int CL_ITEM_TYPE_NOTE = 0x0004;
	//=-=-=-=-SUPERGROUPS-=-=-=-=
	public static final int PROFILE_GROUP = 7;
	public static final int OPENED_CHATS_GROUP = 5;
	public static final int NOT_IN_LIST_GROUP = 6;
	public int type = 1;
	public int level = 0;
	public int item_id = 0;
	public int group_id = 0;
	public String name;
	public boolean chating;
	public boolean hasUnreadMessages;
	public int status = -1;
	public int extended_status = 0;
	public BimoidProfile profile;
	public int getGroupId(){
		return group_id;
	}
	public int getItemId(){
		return item_id;
	}
	public void setRosterId(int id){
		this.group_id = id;
	}
	public void setItemId(int id){
		this.item_id = id;
	}
	public BimoidProfile getProfile(){
		return profile;
	}
	public void setProfile(BimoidProfile profile){
		this.profile = profile;
	}
	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name = name;
	}
	public int getHash(){
		return (""+item_id+type).hashCode();
	}
	public void update(RosterItem item){
	}
	@Override
	public int compareTo(RosterItem contact) {
		/*if(hasUnreadMessages && !contact.hasUnreadMessages){
			return -1;
		}else if(!hasUnreadMessages && contact.hasUnreadMessages){
			return 1;
		}
		if(chating && !contact.chating) return -1;
		if(!chating && contact.chating) return 1;
		if((status == -0x1) && (contact.status != -0x1)){
			return 1;
		}else if((status != -0x1) && (contact.status == -0x1)){
			return -1;
		}*/
		try {
			System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
			String nameA = this.name;
			String nameB = contact.name;
			//Log.w("COMPARATOR", "Comparing: "+nameA+"  "+nameB);
			int minLen = nameA.length();
			if(nameB.length() < minLen){
				minLen = nameB.length();
			}
			int lvl = 0;
			while(true){
				int a = nameA.charAt(lvl);
				a = 256+utilities.chars.indexOf(a);
				if(a == 255){
					a = nameA.charAt(lvl);
				}
				int b = nameB.charAt(lvl);
				b = 256+utilities.chars.indexOf(b);
				if(b == 255){
					b = nameB.charAt(lvl);
				}
				//Log.v("COMPARATOR", "Comparing: "+String.valueOf(a)+"  "+String.valueOf(b));
				if(a == b){
					lvl++;
					if(lvl >= minLen){
						return 0;
					}
				}else if(a < b){
					return -1;
				}else if(a > b){
					return 1;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
