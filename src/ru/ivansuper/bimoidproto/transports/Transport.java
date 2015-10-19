package ru.ivansuper.bimoidproto.transports;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.NoteItem;
import ru.ivansuper.bimoidproto.RosterItem;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.ui.TabsContentHolder;
import ru.ivansuper.ui.TabsContentHolder.TabContent;

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
	
	private int mMailBoxUnreadedCount;
	
	public Transport(BimoidProfile profile, String account, String UUID){
		type = RosterItem.TRANSPORT_ITEM;
		//Log.e("Loading", profile.ID+UUID+account_name);
		remembered_status = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getInt(profile.ID+UUID+account+"sts", -1);
		extended_status = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getInt(profile.ID+UUID+account+"asts", 0);
		//Log.e("RememberedSts", ""+remembered_status);
		//Log.e("RememberedASts", ""+extended_status);
	}
	public void update(RosterItem item){
		try{
			Transport t = (Transport)item;
			this.UUID = t.UUID;
			this.account_name = t.account_name;
			this.account_pass = t.account_pass;
			this.account_server = t.account_server;
			this.account_port = t.account_port;
			this.params = t.params;
			this.remembered_status = t.remembered_status;
			this.extended_status = t.extended_status;
		}catch(Exception e){}
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
	
	public final void setMailBoxUnreadedCount(int count){
		mMailBoxUnreadedCount = count;
	}
	public final int getMailBoxUnreadedCount(){
		return mMailBoxUnreadedCount;
	}
	public final boolean isMailBoxHasUnreaded(){
		return mMailBoxUnreadedCount > 0;
	}
	
	public final void updateValuesFromForm(TabsContentHolder tabs_content){
		
		account_pass = ((TextView)tabs_content.findViewById(R.id.transport_settings_account_password)).getText().toString();
		
		if(params.mTransportSettings.isServerOptionsUsed()){
			
			account_server = ((TextView)tabs_content.findViewById(R.id.transport_settings_server_address)).getText().toString();
			account_port = Integer.valueOf(((TextView)tabs_content.findViewById(R.id.transport_settings_server_port)).getText().toString());
			
			if(account_port > 65535) account_port = 65535;
			
		}
		
		params.mTransportSettings.updateValuesFromForm(tabs_content);
		
	}
		
	public final void buildSettingsGUI(TabsContentHolder tabs_content){
		
		final View account_settings = View.inflate(tabs_content.getContext(), R.layout.transport_settings_account_layout, null);
		
		((TextView)account_settings.findViewById(R.id.l1)).setText(Locale.getString("s_transport_settings_account_tab_login"));
		((TextView)account_settings.findViewById(R.id.l2)).setText(Locale.getString("s_transport_settings_account_tab_password"));
		((TextView)account_settings.findViewById(R.id.transport_settings_account_login)).setText(account_name);
		((TextView)account_settings.findViewById(R.id.transport_settings_account_password)).setText(account_pass);
		
		TabContent tab_content = new TabContent(Locale.getString("s_transport_settings_account_tab"), account_settings);
		tabs_content.addTab(tab_content);
		
		if(params.mTransportSettings.isServerOptionsUsed()){
			
			final View server_settings = View.inflate(tabs_content.getContext(), R.layout.transport_settings_server_layout, null);
			
			((TextView)server_settings.findViewById(R.id.l1)).setText(Locale.getString("s_transport_settings_server_tab_server"));
			((TextView)server_settings.findViewById(R.id.l2)).setText(Locale.getString("s_transport_settings_server_tab_port"));
			((TextView)server_settings.findViewById(R.id.transport_settings_server_address)).setText(account_server);
			((TextView)server_settings.findViewById(R.id.transport_settings_server_port)).setText(String.valueOf(account_port));
			
			tab_content = new TabContent(Locale.getString("s_transport_settings_server_tab"), server_settings);
			tabs_content.addTab(tab_content);
			
		}
		
		params.mTransportSettings.buildGUI(tabs_content);
		
	}
	
	public final void saveSettings(TabsContentHolder tabs_content){
		
		
		
	}
	
}
