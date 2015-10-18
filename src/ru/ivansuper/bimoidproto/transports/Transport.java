package ru.ivansuper.bimoidproto.transports;

import android.preference.PreferenceManager;
import android.util.Log;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.RosterItem;

public class Transport extends RosterItem {
	public String UUID = "";
	public String account_name = "";
	public String account_pass = "";
	public String account_server = "";
	public int account_port = 0;
	public boolean ready;
	public TransportParams params;
	public boolean connected;
	private int remembered_status = -1;
	public Transport(BimoidProfile profile, String account, String UUID){
		type = RosterItem.TRANSPORT_ITEM;
		//Log.e("Loading", profile.ID+UUID+account_name);
		remembered_status = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getInt(profile.ID+UUID+account+"sts", -1);
		extended_status = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getInt(profile.ID+UUID+account+"asts", 0);
		//Log.e("RememberedSts", ""+remembered_status);
		//Log.e("RememberedASts", ""+extended_status);
	}
	public void setStatusA(int status){
		remembered_status = status;
		PreferenceManager.getDefaultSharedPreferences(resources.ctx).edit().putInt(profile.ID+UUID+account_name+"sts", status).commit();
	}
	public void setStatus(int status){
		this.status = status;
		profile.svc.handleContactListNeedRefresh();
	}
	public int getRememberedStatus(){
		return remembered_status;
	}
	public void setExtendedStatus(int status){
		extended_status = status;
		profile.svc.handleContactListNeedRefresh();
		PreferenceManager.getDefaultSharedPreferences(resources.ctx).edit().putInt(profile.ID+UUID+account_name+"asts", status).commit();
		//Log.e("Saving", profile.ID+UUID+account_name);
	}
	public int getStatus(){
		return status;
	}
	public int getExtendedStatus(){
		return extended_status;
	}
}
