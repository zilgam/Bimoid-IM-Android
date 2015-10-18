package ru.ivansuper.bimoidim;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.text.ClipboardManager;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class IgnoreActivity extends Activity implements Callback {
    private BimoidService service;
    private ListView contacts;
    private IgnoreAdapter adapter;
	private String ID;
	protected Contact context_contact;
	private Handler ignoreHdl = new Handler(this);
	public static final int UPDATE_LIST = 0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ignore);
        setVolumeControlStream(0x3);
        ID = getIntent().getAction();
        initViews();
       	service = resources.service;
		handleServiceConnected();
    }
    private void initViews(){
    	contacts = (ListView)findViewById(R.id.ignore_list);
    	Interface.attachBackground(contacts, Interface.ignored_list_back);
		Interface.attachSelector(contacts);
    	Interface.attachBackground((LinearLayout)findViewById(R.id.header), Interface.ignored_top_panel);
    	contacts.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				context_contact = (Contact)adapter.getItem(pos);
				showDialog(0);
			}
    	});
    	TextView label = (TextView)findViewById(R.id.l1);
    	label.setText(ID+":\n"+Locale.getString("s_moved_to_ignore"));
        if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(3));
        if(ColorScheme.initialized) ((LinearLayout)findViewById(R.id.divider)).setBackgroundColor(ColorScheme.getColor(4));
    }
    private void handleServiceConnected(){
    	service.ignoreHdl = ignoreHdl;
    	adapter = new IgnoreAdapter(service);
    	contacts.setAdapter(adapter);
    	adapter.fill(ID);
    }
    protected Dialog onCreateDialog(final int type){
    	Dialog dialog = null;
    	switch(type){
    	case 0x0://Menu
        	UAdapter adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_delete"), 0);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_copy_id"), 1);
        	dialog = DialogBuilder.create(IgnoreActivity.this,
        			Locale.getString("s_menu"),
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
							if(context_contact == null) return;
							removeDialog(0);
							switch(pos){
							case 0:
								if(context_contact.getProfile().connected){
									context_contact.getProfile().deleteContact(context_contact.getID(), context_contact.getTransportId());
								}else{
									Toast.makeText(IgnoreActivity.this, Locale.getString("s_you_must_be_connected_for_this_operation"), Toast.LENGTH_SHORT).show();
								}
								break;
							case 1:
								ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
								cm.setText(context_contact.getID());
								Toast.makeText(IgnoreActivity.this, Locale.getString("s_copied"), Toast.LENGTH_SHORT).show();
								break;
							}
						}
        			});
    		break;
    	}
    	return dialog;
    }
	@Override
	public boolean handleMessage(Message msg) {
		switch(msg.what){
		case UPDATE_LIST:
			adapter.fill(ID);
			break;
		}
		return false;
	}
}
