package ru.ivansuper.bimoidim;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class ProfilesActivity extends Activity {
    private BimoidService service;
    private ServiceConnection svcc;
    private BimoidProfile context_profile;
    private ProfilesManager pm;
    private UAdapter profiles_adapter;
    private ListView profiles_list;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profiles);
		initViews();
        setVolumeControlStream(0x3);
       	service = resources.service;
		handleServiceConnected();
        Intent i = getIntent();
        boolean force_add_profile = i.getBooleanExtra("force_add_profile", false);
        if(force_add_profile){
        	doAddProfile();
        }
	}
	@Override
	public void onResume(){
		super.onResume();
	}
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	if(svcc != null) unbindService(svcc);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(keyCode){
    		case KeyEvent.KEYCODE_BACK:
    			onBackDown();
    			break;
    		case KeyEvent.KEYCODE_MENU:
            	showDialog(2);
    			break;
    		}
    	}
    	return false;
    }
    private void onBackDown(){
    	finish();
    }
    protected Dialog onCreateDialog(int type){
    	Dialog dialog = null;
    	switch(type){
    	case 0x0://Add profile dialog
    		LayoutInflater li = LayoutInflater.from(resources.ctx);
    		LinearLayout lay = (LinearLayout)li.inflate(R.layout.profile_dataview, null);
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_login").setTextColor(ColorScheme.getColor(12));
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_password").setTextColor(ColorScheme.getColor(12));
    		final EditText ID = (EditText)lay.findViewById(R.id.profile_dataview_id);
    		ID.setHint(Locale.getString("s_login_example"));
    		if(ColorScheme.initialized) ID.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(ID);
    		final EditText PASS = (EditText)lay.findViewById(R.id.profile_dataview_pass);
    		if(ColorScheme.initialized) PASS.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(PASS);
        	final CheckBox AUTO = (CheckBox)lay.findViewById(R.id.profile_dataview_autoconnect);
        	AUTO.setText(Locale.getString("s_autoconnect"));
        	if(ColorScheme.initialized) AUTO.setTextColor(ColorScheme.getColor(12));
        	Interface.attachCheckBoxStyle(AUTO);
        	final CheckBox SSL = (CheckBox)lay.findViewById(R.id.profile_dataview_ssl);
        	if(ColorScheme.initialized) SSL.setTextColor(ColorScheme.getColor(12));
        	Interface.attachCheckBoxStyle(SSL);
    		dialog = DialogBuilder.createYesNo(this, lay, Gravity.TOP, Locale.getString("s_adding"), Locale.getString("s_do_add"), Locale.getString("s_cancel"), new OnClickListener(){
    			@Override
    			public void onClick(View arg0) {
    				String id_s = ID.getText().toString().toLowerCase();
    				String[] params = id_s.split("@");
    				if(params.length != 2){
    					Toast.makeText(ProfilesActivity.this, Locale.getString("s_add_profile_error_1"), Toast.LENGTH_LONG).show();
    					return;
    				}
    				if(params[1].trim().length() < 5){
    					Toast.makeText(ProfilesActivity.this, Locale.getString("s_add_profile_error_2"), Toast.LENGTH_LONG).show();
    					return;
    				}
    				String pass_s = PASS.getText().toString();
    				ID.setText(id_s);
    				if((params[0].trim().length() < 3) || (pass_s.length() == 0)){
    					Toast.makeText(ProfilesActivity.this, Locale.getString("s_add_profile_error_3"), Toast.LENGTH_LONG).show();
    					return;
    				}
    				if(pm.isExist(id_s)){
    					Toast.makeText(ProfilesActivity.this, Locale.getString("s_add_profile_error_4"), Toast.LENGTH_LONG).show();
    					return;
    				}
    				BimoidProfile profile = new BimoidProfile(service, id_s, pass_s, AUTO.isChecked(), SSL.isChecked());
    				pm.addProfile(profile);
    				fillFromPM();
    				try {
    					pm.saveProfiles();
    				} catch (Exception e) {
    					e.printStackTrace();
    					Log.e("ProfilesActivity", "Can't save profiles to data path");
    				}
    				removeDialog(0);
    			}
    		}, new OnClickListener(){
    			@Override
    			public void onClick(View arg0) {
    				removeDialog(0);
    			}
    		});
    		break;
    	case 0x1://Context menu
        	UAdapter adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_change"), 2);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_delete"), 3);
        	dialog = DialogBuilder.create(ProfilesActivity.this,
        			Locale.getString("s_menu"),
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0x2://Menu
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_add_profile"), 0);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_reg_and_add_profile"), 1);
        	dialog = DialogBuilder.create(ProfilesActivity.this,
        			Locale.getString("s_menu"),
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0x3://Edit profile dialog
    		LinearLayout lay1 = (LinearLayout)View.inflate(resources.ctx, R.layout.profile_dataview, null);
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay1.findViewById(R.id.l1)), "s_login").setTextColor(ColorScheme.getColor(12));
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay1.findViewById(R.id.l2)), "s_password").setTextColor(ColorScheme.getColor(12));
    		final EditText ID1 = (EditText)lay1.findViewById(R.id.profile_dataview_id);
    		ID1.setHint(Locale.getString("s_login_example"));
    		if(ColorScheme.initialized) ID1.setTextColor(ColorScheme.getColor(13));
    		ID1.setEnabled(false);
        	Interface.attachEditTextStyle(ID1);
    		ID1.setText(context_profile.ID+"@"+context_profile.server);
    		final EditText PASS1 = (EditText)lay1.findViewById(R.id.profile_dataview_pass);
    		if(ColorScheme.initialized) PASS1.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(PASS1);
    		PASS1.setText(context_profile.password);
    		PASS1.requestFocus();
        	final CheckBox AUTO1 = (CheckBox)lay1.findViewById(R.id.profile_dataview_autoconnect);
        	AUTO1.setText(Locale.getString("s_autoconnect"));
        	if(ColorScheme.initialized) AUTO1.setTextColor(ColorScheme.getColor(12));
        	Interface.attachCheckBoxStyle(AUTO1);
        	AUTO1.setChecked(context_profile.autoconnect);
        	final CheckBox SSL1 = (CheckBox)lay1.findViewById(R.id.profile_dataview_ssl);
        	if(ColorScheme.initialized) SSL1.setTextColor(ColorScheme.getColor(12));
        	Interface.attachCheckBoxStyle(SSL1);
        	SSL1.setChecked(context_profile.use_ssl);
        	dialog = DialogBuilder.createYesNo(this, lay1, Gravity.TOP, Locale.getString("s_profile_correction"), Locale.getString("s_apply"), Locale.getString("s_cancel"), new OnClickListener(){
    			@Override
    			public void onClick(View arg0) {
    				String id_s = ID1.getText().toString().toLowerCase();
    				String[] params = id_s.split("@");
    				if(params.length != 2){
    					Toast.makeText(ProfilesActivity.this, Locale.getString("s_add_profile_error_1"), Toast.LENGTH_LONG).show();
    					return;
    				}
    				if(params[1].trim().length() < 5){
    					Toast.makeText(ProfilesActivity.this, Locale.getString("s_add_profile_error_2"), Toast.LENGTH_LONG).show();
    					return;
    				}
    				String pass_s = PASS1.getText().toString();
    				ID1.setText(id_s);
    				if((params[0].trim().length() < 3) || (pass_s.length() == 0)){
    					Toast.makeText(ProfilesActivity.this, Locale.getString("s_add_profile_error_3"), Toast.LENGTH_LONG).show();
    					return;
    				}
    				context_profile.ID = params[0];
    				context_profile.server = params[1];
    				context_profile.password = pass_s;
    				context_profile.autoconnect = AUTO1.isChecked();
    				context_profile.use_ssl = SSL1.isChecked();
    				fillFromPM();
    				try {
    					pm.saveProfiles();
    				} catch (Exception e) {
    					e.printStackTrace();
    					Log.e("ProfilesActivity", "Can't save profiles to data path");
    				}
    				service.handleContactListNeedRebuild();
    				removeDialog(3);
    			}
    		}, new OnClickListener(){
    			@Override
    			public void onClick(View arg0) {
    				removeDialog(3);
    			}
    		});
    		break;
    	}
    	return dialog;
    }
	private void initViews(){
	    if(ColorScheme.initialized) ((LinearLayout)findViewById(R.id.profiles_back)).setBackgroundColor(ColorScheme.getColor(30));
        profiles_list = (ListView)findViewById(R.id.profiles_list);
        Interface.attachBackground(profiles_list, Interface.profiles_list_back);
		Interface.attachSelector(profiles_list);
        if(ColorScheme.initialized) utilities.setLabel(((TextView)findViewById(R.id.l1)), "s_available_accounts").setTextColor(ColorScheme.getColor(3));
        if(ColorScheme.initialized) ((LinearLayout)findViewById(R.id.divider)).setBackgroundColor(ColorScheme.getColor(4));
        Interface.attachBackground((LinearLayout)findViewById(R.id.header), Interface.profiles_top_panel);
        otherInit();
	}
	private void otherInit(){
		profiles_adapter = new UAdapter();
		profiles_list.setAdapter(profiles_adapter);
		profiles_list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				profiles_adapter.notifyDataSetChanged();
			}
		});
		profiles_list.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				context_profile = pm.list.get(arg2);
				showDialog(1);
				return false;
			}
		});
	}
	private void handleServiceConnected(){
		pm = service.profiles;
		fillFromPM();
	}
    private class context_menu_listener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			UAdapter adapter = (UAdapter)arg0.getAdapter();
			adapter.notifyDataSetChanged();
			int id = (int)(adapter).getItemId(arg2);
			switch(id){
			case 0://Add profile
				doAddProfile();
				removeDialog(2);
				break;
			case 1://Register profile
				removeDialog(2);
				Intent i = new Intent(resources.ctx, RegistrationActivity.class);
				startActivity(i);
				finish();
				break;
			case 2://Change profile
				removeDialog(1);
				removeDialog(3);
				showDialog(3);
				break;
			case 3://Delete profile
				doDeleteProfile();
				removeDialog(1);
				break;
			}
		}
    }
    private void doAddProfile(){
		showDialog(0);
    }
    private void doDeleteProfile(){
    	pm.removeProfileByID(context_profile.ID);
    	try {
			pm.saveProfiles();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	fillFromPM();
    }
    private void fillFromPM(){
    	profiles_adapter.clear();
    	for(int i=0; i<pm.list.size(); i++){
    		BimoidProfile profile = pm.list.get(i);
    		profiles_adapter.put(profile.ID, i);
    	}
    }
}
