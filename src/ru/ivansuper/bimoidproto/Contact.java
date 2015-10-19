package ru.ivansuper.bimoidproto;

import java.util.Vector;

public class Contact extends RosterItem {
	public static final int AUTH_REQ = 1;
	public static final int AUTH_ACCEPTED = 2;
	public static final int AUTH_REJECTED = 3;
	public static final int CL_PRIV_TYPE_NONE = 0x00;
	public static final int CL_PRIV_TYPE_VISIBLE_LIST = 0x01;
	public static final int CL_PRIV_TYPE_INVISIBLE_LIST = 0x02;
	public static final int CL_PRIV_TYPE_IGNORE_LIST = 0x03;
	public static final int CL_PRIV_TYPE_IGNORE_NOT_IN_LIST = 0x04;
	private String ID = "";
	private int transport_id = -1;
	private int privacy = 0;
	public boolean auth_flag;
	public boolean general_flag;
	private String description = "";
	private String extended_description = "";
	private int unread_count = 0;
	private boolean typing;
	private int have_auth = -1;
	private int have_file = -1;
	private String client = null;
	private String client_version = null;
	private ContactHistory history;
	private boolean temporary;
	public boolean antispam_accepted = true;
	public Contact(String ID, String nickname, int id, int item_id, int privacy, boolean a, boolean b, BimoidProfile profile){
		this.ID = ID;
		this.name = nickname;
		this.group_id = id;
		this.item_id = item_id;
		this.privacy = privacy;
		auth_flag = a;
		general_flag = b;
		this.profile = profile;
		history = new ContactHistory(this);
		type = RosterItem.OBIMP_CONTACT;
	}
	public void update(RosterItem item){
		try{
			Contact c = (Contact)item;
			this.ID = c.ID;
			this.group_id = c.group_id;
			this.privacy = c.privacy;
			this.auth_flag = c.auth_flag;
			this.general_flag = c.general_flag;
		}catch(Exception e){}
	}
	public String getID(){
		return ID;
	}
	public int getPrivacy(){
		return privacy;
	}
	public void setPrivacy(int privacy){
		this.privacy = privacy;
	}
	public void setStatus(int status){
		this.status = status;
		if(status == 0) setTyping(false);
	}
	public void setDescription(String desc){
		description = desc;
	}
	public void setExtendedStatus(int status){
		extended_status = status;
	}
	public void setExtendedDescription(String desc){
		extended_description = desc;
	}
	public void setTransportId(int id){
		transport_id = id;
	}
	public int getStatus(){
		return status;
	}
	public int getExtendedStatus(){
		return extended_status;
	}
	public String getDescription(){
		return description;
	}
	public String getExtendedDescription(){
		return extended_description;
	}
	public int getTransportId(){
		return transport_id;
	}
	public boolean itIsTransport(){
		return transport_id != -1;
	}
	public boolean haveExtendedStatusDescription(){
		return extended_description.trim().length() > 0;
	}
	public Vector<HistoryItem> getChat(){
		return history.getMessageList();
	}
	public ContactHistory getHistoryObject(){
		return history;
	}
	public void increaseUnreadMessages(){
		hasUnreadMessages = true;
		unread_count++;
	}
	public boolean hasUnreadMessages(){
		return hasUnreadMessages;
	}
	public void clearUnreadMessages(){
		hasUnreadMessages = false;
		unread_count = 0;
	}
	public int getUnreadCount(){
		return unread_count;
	}
	public void setTyping(boolean typing){
		this.typing = typing;
		profile.svc.handleContactListNeedRefresh();
		profile.svc.refreshChatUserInfo();
	}
	public boolean getTyping(){
		return typing;
	}
	public void setClient(String client){
		this.client = client;
	}
	public String getClient(){
		return client;
	}
	public void setClientVersionString(String version){
		client_version = version;
	}
	public String getClientVersionString(){
		return client_version;
	}
	public void setHasAuth(int type){
		have_auth = type;
	}
	public void setHasNoAuth(){
		have_auth = -1;
	}
	public boolean haveAuth(){
		return have_auth != -1;
	}
	public int getAuth(){
		return have_auth;
	}
	public boolean haveAuthReq(){
		return have_auth == 1;
	}
	public boolean haveAuthAccepted(){
		return have_auth == 2;
	}
	public boolean haveAuthRejected(){
		return have_auth == 3;
	}
	public void setHasFile(){
		have_file = 1;
	}
	public void setHasNoFile(){
		have_file = -1;
	}
	public boolean haveIncFile(){
		return have_file == 1;
	}
	public void setTemporary(boolean value){
		temporary = value;
	}
	public boolean isTemporary(){
		return temporary;
	}
}
