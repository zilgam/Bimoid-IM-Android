package ru.ivansuper.bimoidim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.URLConnection;
import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.AccountInfoContainer;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.BimoidProtocol;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.Group;
import ru.ivansuper.bimoidproto.Int;
import ru.ivansuper.bimoidproto.NoteItem;
import ru.ivansuper.bimoidproto.RosterItem;
import ru.ivansuper.bimoidproto.RosterOperation;
import ru.ivansuper.bimoidproto.SuperGroup;
import ru.ivansuper.bimoidproto.filetransfer.FileBrowserActivity;
import ru.ivansuper.bimoidproto.transports.Transport;
import ru.ivansuper.bimoidproto.transports.TransportParams;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.popup.PopupBuilder;
import ru.ivansuper.popup.QuickAction;
import ru.ivansuper.ui.ConfigListenerView;
import ru.ivansuper.ui.ExFragment;
import ru.ivansuper.ui.ExFragmentManager;
import ru.ivansuper.ui.ExFragmentManager.ExRunnable;
import ru.ivansuper.ui.JFragmentActivity;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LayoutAnimationController;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContactListActivity extends JFragmentActivity implements Callback, OnSharedPreferenceChangeListener {
    private BimoidService service;
    private ListView contacts;
    private ContactsAdapter contacts_adapter;
    private LinearLayout bottom_panel;
    private BimoidProfile context_profile;
    private NoteItem context_note;
    private Contact context_contact;
    private Transport context_transport;
    private Group context_group;
    private Handler hdl = new Handler(this);
    public static final int REBUILD_LIST = 0;
    public static final int REFRESH_LIST = 1;
    public static final int BUILD_BOTTOM_PANEL = 2;
    public static final int SHOW_INFO_DIALOG = 3;
    public static final int MENU_PRESSED = 4;
    public static final int SHOW_ACCOUNT_INFO_DIALOG = 5;
    public static final int REINIT_INTERFACE = 6;
    public static final int SHOW_PROGRESS_DIALOG = 7;
    public static final int HIDE_PROGRESS_DIALOG = 8;
    public static final int SHOW_ERROR_DIALOG = 9;
	public static final int RETURN_TO_CONTACTS = 270;
    public static boolean VISIBLE = true;
    public static Vector<BufferedDialog> dialogs = new Vector<BufferedDialog>();
    private AccountInfoContainer account_info_for_display;
    private BufferedDialog dialog_for_display;
    private Dialog last_shown_dialog;
    private int roster_operation_confirm_helper;
	private Dialog last_shown_error_dialog;
	private Dialog last_shown_notify_dialog;
	public boolean adding_temporary;
	public int status_selection_mode;
	private int selected_xstatus;
	private SharedPreferences sp;
	private Dialog progress_dialog;
	private EditText FILE_NOTE_SELECT_FILE_HELPER;
	public boolean exiting;
	protected QuickAction last_quick_action;
	private boolean CURRENT_IS_CONTACTS = true;
	private boolean ANY_CHAT_ACTIVE = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if(resources.IT_IS_TABLET) getWindow().addFlags(0x1000000);//Hardware acceleration
        setContentView(R.layout.contact_list);
        setVolumeControlStream(0x3);
        initViews();
       	service = resources.service;
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
		handleServiceConnected();
		checkLogs();
		Intent i = getIntent();
		proceedIntent(i);
    }
    @Override
    public void onNewIntent(Intent i){
    	proceedIntent(i);
    }
    private final void proceedIntent(Intent i){
    	String action = i.getAction();
    	if(action != null){
    		if(action.startsWith("CHAT"))
    			startFragmentChat(action);
    	}
    }
    private final void checkLogs(){
        final File marker = new File(resources.DATA_PATH+"ForceClosed.marker");
        if(marker.exists()){
        	if(!resources.sd_mounted()) return;
        	marker.delete();
        	copyDumpsToSD();
        	showDialog(-1);
        }
    }
	private void copyDumpsToSD(){
		byte[] buffer = new byte[0x4000];
        File data_dir = new File(resources.DATA_PATH);
        FilenameFilter filter = new FilenameFilter(){
			@Override
			public boolean accept(File dir, String filename) {
				if(filename.endsWith(".st")) return true;
				return false;
			}
        };
        File[] dumps = data_dir.listFiles(filter);
        for(File dump: dumps){
        	//Log.e("Processing file", dump.getName());
        	try{
        		File out = new File(resources.SD_PATH+"/Bimoid/"+dump.getName());
        		FileOutputStream fos = new FileOutputStream(out);
        		FileInputStream fis = new FileInputStream(dump);
        		while(fis.available() > 0){
        			int readed = fis.read(buffer, 0, 0x4000);
        			fos.write(buffer, 0, readed);
        		}
        		fos.close();
        		fis.close();
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        	dump.delete();
        }
	}
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	//if(svcc != null) unbindService(svcc);
		if(exiting){
			service.profiles.disconnectAll();
			service.performDestroying();
			Intent i = new Intent(resources.ctx, BimoidService.class);
			stopService(i);
			android.os.Process.sendSignal(android.os.Process.myPid(), android.os.Process.SIGNAL_KILL);
		}
    }
    @Override
    public void onResume(){
    	VISIBLE = true;
    	super.onResume();
    	
    	updateUI();
    }
    private final void localOnResume(){
    	checkForBufferedDialogs();
    }
    private void checkForBufferedDialogs(){
    	if(dialogs.size() > 0){
    		dialog_for_display = dialogs.remove(0);
    		if(dialog_for_display.is_error){
        		showDialog(-2);
    		}else{
        		showDialog(3);
    		}
    	}
    }
    @Override
    public void onPause(){
    	VISIBLE = false;
    	super.onPause();
    }
    @Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    	ExFragmentManager.executeEvent(new ExRunnable(){
			@Override
			public void run() {
				fragment.onActivityResult(requestCode, resultCode, data);
			}
    	});
    	if(requestCode == 0){
    		if(resultCode == RESULT_OK){
    			FILE_NOTE_SELECT_FILE_HELPER.setText(data.getAction());
    		}
    	}
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
    	//super.onKeyDown(keyCode, event)
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(keyCode){
    		case KeyEvent.KEYCODE_BACK:
				if(CURRENT_IS_CONTACTS){
					if(ANY_CHAT_ACTIVE){
						service.handleContactlistReturnToContacts();
					}else{
						finish();
					}
				}else{
					CURRENT_IS_CONTACTS = true;
					updateUI();
				}
    			break;
    		case KeyEvent.KEYCODE_MENU:
        		removeDialog(2);
            	showDialog(2);
    			break;
    		}
    	}
    	return false;
    }
    private void initViews(){
    	ConfigListenerView clv = (ConfigListenerView)findViewById(R.id.config_listener);
    	clv.listener = new ConfigListenerView.OnLayoutListener() {
			@Override
			public void onNewLayout(int w, int h, int oldw, int oldh) {
				if(last_quick_action != null && last_quick_action.window.isShowing()){
					last_quick_action.dismiss();
				}
			}
		};
    	contacts = (ListView)findViewById(R.id.contact_list_list);
    	bottom_panel = (LinearLayout)findViewById(R.id.contact_list_bottom_panel);
    	attachInterface();
    	final ImageView fast_settings = (ImageView)findViewById(R.id.contact_list_fast_settings);
    	fast_settings.setBackgroundDrawable(Interface.getSelector());
    	fast_settings.setPadding(10, 10, 10, 10);
    	fast_settings.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				final UAdapter adapter = new UAdapter();
				adapter.setTextSize(15);
				adapter.setPadding(7);
				final Resources res = getResources();
				if(service.prefs.getBoolean("ms_sounds", true)){
					adapter.put(res.getDrawable(R.drawable.toggle_sound_on), Locale.getString("s_sound_on"), 0);
				}else{
					adapter.put(res.getDrawable(R.drawable.toggle_sound_off), Locale.getString("s_sound_off"), 0);
				}
				if(service.prefs.getBoolean("ms_vibro", true)){
					adapter.put(res.getDrawable(R.drawable.toggle_vibro_on), Locale.getString("s_vibro_on"), 1);
				}else{
					adapter.put(res.getDrawable(R.drawable.toggle_vibro_off), Locale.getString("s_vibro_off"), 1);
				}
				if(service.prefs.getBoolean("ms_offline", true)){
					adapter.put(res.getDrawable(R.drawable.toggle_offline_on), Locale.getString("s_hide_offline_off"), 2);
				}else{
					adapter.put(res.getDrawable(R.drawable.toggle_offline_off), Locale.getString("s_hide_offline_on"), 2);
				}
				if(service.prefs.getBoolean("ms_groups", true)){
					adapter.put(res.getDrawable(R.drawable.show_groups), Locale.getString("s_show_groups"), 3);
				}else{
					adapter.put(res.getDrawable(R.drawable.hide_groups), Locale.getString("s_hide_groups"), 3);
				}
				last_quick_action = PopupBuilder.buildList(adapter, v, null, 230*resources.dm.density, LayoutParams.WRAP_CONTENT, new OnItemClickListener(){
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						last_quick_action.dismiss();
						switch((int)arg0.getAdapter().getItemId(arg2)){
						case 0:
							service.prefs.edit().putBoolean("ms_sounds", !service.prefs.getBoolean("ms_sounds", true)).commit();
							break;
						case 1:
							service.prefs.edit().putBoolean("ms_vibro", !service.prefs.getBoolean("ms_vibro", true)).commit();
							break;
						case 2:
							service.prefs.edit().putBoolean("ms_offline", !service.prefs.getBoolean("ms_offline", true)).commit();
							break;
						case 3:
							service.prefs.edit().putBoolean("ms_groups", !service.prefs.getBoolean("ms_groups", true)).commit();
							break;
						}
					}
				});
				last_quick_action.show();
			}
    	});
    }
    private void attachInterface(){
    	Interface.attachSelector(contacts);
    	if(ColorScheme.initialized && !ColorScheme.internal) contacts.setBackgroundColor(ColorScheme.getColor(0));
    	Interface.attachBackground(contacts, Interface.contact_list_items_back);
    	LinearLayout bottom = (LinearLayout)findViewById(R.id.contact_list_bottom);
    	if(ColorScheme.initialized && !ColorScheme.internal) bottom.setBackgroundColor(ColorScheme.getColor(1));
    	Interface.attachBackground(bottom, Interface.contact_list_bottom_back);
    }
    private void handleServiceConnected(){
        Intent i = getIntent();
        boolean no_profiles = i.getBooleanExtra("no_profiles", false);
        if(no_profiles){
        	showDialog(1);
        }
    	service.clHdl = hdl;
    	contacts_adapter = new ContactsAdapter(service);
    	contacts.setAdapter(contacts_adapter);
    	contacts.setOnItemClickListener(new roster_click_listener());
    	contacts.setOnItemLongClickListener(new roster_long_click_listener());
    	service.handleContactListNeedRebuild();
    	otherInit();
    }
    private void otherInit(){
    	buildBottomPanel();
    }
    private class context_menu_listener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			last_shown_dialog.dismiss();
			UAdapter adapter = (UAdapter)arg0.getAdapter();
			adapter.notifyDataSetChanged();
			int id = (int)(adapter).getItemId(arg2);
			switch(id){
			case 0:
				showDialog(0);
				break;
			case 1:
				exiting = true;
				finish();
				//service.profiles.disconnectAll();
				//service.performDestroying();
				//unbindService(svcc);
				//Intent i = new Intent(resources.ctx, BimoidService.class);
				//stopService(i);
				//System.runFinalization();
				//try {
				//	Thread.sleep(1000);
				//} catch (InterruptedException e) {
				//	e.printStackTrace();
				//}
				//System.exit(0);
				break;
			case 2:
				removeDialog(8);
				showDialog(8);
				break;
			case 3:
				removeDialog(2);
				Intent i = new Intent(resources.ctx, SettingsActivity.class);
				startActivity(i);
				break;
			case 4://Rename contact
				if(context_contact == null) return;
				roster_operation_confirm_helper = RosterOperation.CONTACT_MODIFY;
				removeDialog(7);
				showDialog(7);
				break;
			case 5://Delete contact
				if(context_contact == null) return;
				roster_operation_confirm_helper = RosterOperation.CONTACT_REMOVE;
				removeDialog(0x5);
				showDialog(0x5);
				break;
			case 6://Delete contact
				if(context_contact == null) return;
				removeDialog(0xB);
				showDialog(0xB);
				break;
			case 7://Contact visibility
				if(context_contact == null) return;
				removeDialog(0xD);
				showDialog(0xD);
				break;
			case 8://Rename group
				if(context_group == null) return;
				roster_operation_confirm_helper = RosterOperation.GROUP_MODIFY;
				removeDialog(7);
				showDialog(7);
				break;
			case 9://Delete group
				if(context_group == null) return;
				roster_operation_confirm_helper = RosterOperation.GROUP_REMOVE;
				removeDialog(0x5);
				showDialog(0x5);
				break;
			case 10://Add contact
				removeDialog(9);
				showDialog(9);
				break;
			case 11://Add group
				removeDialog(10);
				showDialog(10);
				break;
			case 12:
				removeDialog(0xC);
				showDialog(0xC);
				break;
			case 13:
				context_contact.getProfile().changePrivacy(context_contact.getID(), context_contact.getTransportId(), Contact.CL_PRIV_TYPE_VISIBLE_LIST);
				break;
			case 14:
				context_contact.getProfile().changePrivacy(context_contact.getID(), context_contact.getTransportId(), Contact.CL_PRIV_TYPE_INVISIBLE_LIST);
				break;
			case 15:
				context_contact.getProfile().changePrivacy(context_contact.getID(), context_contact.getTransportId(), Contact.CL_PRIV_TYPE_IGNORE_LIST);
				break;
			case 16:
				context_contact.getProfile().changePrivacy(context_contact.getID(), context_contact.getTransportId(), Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST);
				break;
			case 17:
			case 18:
			case 19:
				context_contact.getProfile().changePrivacy(context_contact.getID(), context_contact.getTransportId(), Contact.CL_PRIV_TYPE_NONE);
				break;
			case 20:
				//if(context_contact.auth_flag){
				//	service.showDialogInContactList("Ошибка", "Нельзя посмотреть информацию у не авторизованного контакта");
				//}else{
					context_contact.getProfile().doRequestAccountInfo(context_contact);
				//}
				break;
			case 21:
				removeDialog(0xF);
				showDialog(0xF);
				break;
			case 22://ID Search
				break;
			case 23://E-Mail Search
				break;
			case 24://Detail Search
				removeDialog(0x10);
				showDialog(0x10);
				break;
			case 25://Add temporary contact
				adding_temporary = true;
				if(context_contact.itIsTransport()){
					removeDialog(0x18);
					showDialog(0x18);
				}else{
					removeDialog(9);
					showDialog(9);
				}
				break;
			case 26:
				removeDialog(0x12);
				showDialog(0x12);
				break;
			case 27:
				removeDialog(0x13);
				showDialog(0x13);
				break;
			case 28:
				removeDialog(0x14);
				showDialog(0x14);
				break;
			case 29:
				roster_operation_confirm_helper = RosterOperation.NOTE_ADD;
				removeDialog(0x21);
				showDialog(0x21);
				break;
			}
		}
    }
    protected Dialog onCreateDialog(final int type){
    	Dialog dialog = null;
    	switch(type){
    	case -0x2://Error dialog
    		if(dialog_for_display == null) return null;
    		final String stack = dialog_for_display.field1;
        	dialog = DialogBuilder.createYesNo(ContactListActivity.this, Gravity.TOP,
        			dialog_for_display.header, dialog_for_display.text+"\n----------\n"+stack, Locale.getString("s_close"), Locale.getString("s_do_copy"),
        			 new OnClickListener(){
    					@Override
    					public void onClick(View v) {
    						removeDialog(type);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View v) {
    						removeDialog(type);
    						ClipboardManager cm = (ClipboardManager)getSystemService(Service.CLIPBOARD_SERVICE);
    						cm.setText(stack);
    						Toast.makeText(ContactListActivity.this, Locale.getString("s_copied"), Toast.LENGTH_SHORT);
    					}
    				});
        	last_shown_notify_dialog = dialog;
    		break;
    	case -0x1://Crush
        	dialog = DialogBuilder.createOk(ContactListActivity.this,
        			Locale.getString("s_information"), Locale.getString("s_force_close_info"),
        			Locale.getString("s_close"), Gravity.TOP, new OnClickListener(){
    					@Override
    					public void onClick(View v) {
    						removeDialog(type);
    					}
    		});
    		break;
    	case 0x0://About
        	dialog = DialogBuilder.createOk(ContactListActivity.this,
        			Locale.getString("s_about_header"), utilities.match(Locale.getString("s_about_text"), new String[]{resources.VERSION}),
        			Locale.getString("s_close"), Gravity.TOP, new OnClickListener(){
    					@Override
    					public void onClick(View v) {
    						removeDialog(type);
    					}
    		});
    		break;
    	case 0x1://No profiles notify
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				Gravity.TOP,
    				Locale.getString("s_accounts"),
    				Locale.getString("s_no_accounts_notify"),
            		Locale.getString("s_yes"), Locale.getString("s_no"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(1);
    						Intent i = new Intent(resources.ctx, ProfilesActivity.class);
    						i.putExtra("force_add_profile", true);
    						startActivity(i);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(1);
    					}
    				});
    		break;
    	case 0x2://Main menu
        	UAdapter adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_contact_list"), 2);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_users_search"), 21);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_settings"), 3);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_about_header"), 0);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_exit"), 1);
        	dialog = DialogBuilder.create(ContactListActivity.this,
        			Locale.getString("s_main_menu"),
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0xF://Search variants
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	//adapter.put(resources.context_menu_icon, "По ID", 22);
        	//adapter.put(resources.context_menu_icon, "По E-Mail", 23);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_search_type_detail"), 24);
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0x10://Profiles for search
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	for(int i=0; i<service.profiles.list.size(); i++){
            	adapter.put(resources.context_menu_icon, service.profiles.list.get(i).nickname, i);
        	}
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							removeDialog(0x10);
							BimoidProfile profile = service.profiles.list.get(arg2);
							if(profile == null) return;
							if(!profile.connected){
								service.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_profile_must_be_connected_notify"));
								return;
							}
							Intent activity = new Intent(ContactListActivity.this, DetailSearchActivity.class);
							activity.putExtra("PID", profile.ID);
							startActivity(activity);
						}
        			});
    		break;
    	case 0x13://Profiles for ignore list
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	for(int i=0; i<service.profiles.list.size(); i++){
            	adapter.put(resources.context_menu_icon, service.profiles.list.get(i).nickname, i);
        	}
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							removeDialog(0x13);
							BimoidProfile profile = service.profiles.list.get(arg2);
							if(profile == null) return;
							Intent activity = new Intent(ContactListActivity.this, IgnoreActivity.class);
							activity.setAction(profile.ID);
							startActivity(activity);
						}
        			});
    		break;
    	case 0x3://Notify dialog
    		if(dialog_for_display == null) return null;
    		if(dialog_for_display.is_error){
    			dialog = DialogBuilder.createYesNo(ContactListActivity.this, 0,
    					dialog_for_display.header, dialog_for_display.text,
    					Locale.getString("s_ok"), Locale.getString("s_do_copy"),
    					new OnClickListener(){
							@Override
							public void onClick(View v){
								removeDialog(type);
							}
    					},
    					new OnClickListener(){
    						@Override
    						public void onClick(View v){
								removeDialog(type);
    						}
    					});
    		}else{
	        	dialog = DialogBuilder.createOk(ContactListActivity.this,
	        			dialog_for_display.header, dialog_for_display.text, Locale.getString("s_close"),
	        			Gravity.TOP, new OnClickListener(){
	    					@Override
	    					public void onClick(View v) {
	    						removeDialog(type);
	    					}
	    		});
    		}
        	last_shown_notify_dialog = dialog;
   		break;
    	case 0x20://Note context
        	adapter = new UAdapter();
        	adapter.setPadding(5);
            adapter.put(resources.context_menu_icon, Locale.getString("s_copy_note_name"), 3);
            if(context_note.TEXT != null)
            	if(context_note.TEXT.length() > 0)
            		adapter.put(resources.context_menu_icon, Locale.getString("s_copy_note_text"), 4);
			if(context_note.getProfile().connected){
	            adapter.put(resources.context_menu_icon, Locale.getString("s_do_change"), 2);
	        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_move"), 1);
	            adapter.put(resources.context_menu_icon, Locale.getString("s_do_delete"), 0);
			}
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							last_shown_dialog.dismiss();
							switch((int)arg0.getAdapter().getItemId(arg2)){
							case 0:
								roster_operation_confirm_helper = RosterOperation.NOTE_REMOVE;
								removeDialog(0x5);
								showDialog(0x5);
								break;
							case 1:
								removeDialog(0xC);
								showDialog(0xC);
								break;
							case 2:
								roster_operation_confirm_helper = RosterOperation.NOTE_MODIFY;
								removeDialog(0x21);
								showDialog(0x21);
								break;
							case 3:
								ClipboardManager cm = (ClipboardManager)getSystemService(Service.CLIPBOARD_SERVICE);
								cm.setText(context_note.name);
								Toast.makeText(ContactListActivity.this, Locale.getString("s_copied"), Toast.LENGTH_SHORT).show();
								break;
							case 4:
								cm = (ClipboardManager)getSystemService(Service.CLIPBOARD_SERVICE);
								cm.setText(context_note.TEXT);
								Toast.makeText(ContactListActivity.this, Locale.getString("s_copied"), Toast.LENGTH_SHORT).show();								break;
							}
						}
        	});
    		break;
    	case 0x4://Contact context
        	adapter = new UAdapter();
        	adapter.setPadding(5);
        	if(context_contact.isTemporary())
            	adapter.put(resources.context_menu_icon, Locale.getString("s_add_to_contact_list"), 25);
        	
        	if(context_contact.itIsTransport()){
        		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.update_contacts)
	        		adapter.put(resources.context_menu_icon, Locale.getString("s_do_rename"), 4);
        	}else{
	        	if(!context_contact.isTemporary())
	        	if(context_contact.getPrivacy() != Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST)
	        		adapter.put(resources.context_menu_icon, Locale.getString("s_do_rename"), 4);
        	}
        	if(context_contact.itIsTransport()){
        		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.delete_contacts)
                	adapter.put(resources.context_menu_icon, Locale.getString("s_do_delete"), 5);
        	}else{
            	adapter.put(resources.context_menu_icon, Locale.getString("s_do_delete"), 5);
        	}
        	
        	if(!context_contact.isTemporary())
        	if(context_contact.getPrivacy() != Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST)
        		adapter.put(resources.context_menu_icon, Locale.getString("s_do_move"), 12);
        	
        	if(context_contact.itIsTransport()){
        		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.auth_supported)
                	if(context_contact.auth_flag)
                		adapter.put(resources.context_menu_icon, Locale.getString("s_ask_suth"), 26);
        	}else{
            	if(context_contact.auth_flag)
            		adapter.put(resources.context_menu_icon, Locale.getString("s_ask_suth"), 26);
        	}
        	
        	if(context_contact.itIsTransport()){
        		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.auth_revoke)
            		adapter.put(resources.context_menu_icon, Locale.getString("s_revoke_auth"), 6);
        	}else{
            	if(!context_contact.isTemporary())
                	if(context_contact.getPrivacy() != Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST)
                		adapter.put(resources.context_menu_icon, Locale.getString("s_revoke_auth"), 6);
        	}
        	
        	if(!context_contact.isTemporary())
        	if(context_contact.getPrivacy() != Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST)
        		adapter.put(resources.context_menu_icon, Locale.getString("s_visibility"), 7);
        	
        	if(context_contact.itIsTransport()){
        		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.detail_req)
            		adapter.put(resources.context_menu_icon, Locale.getString("s_contact_info"), 20);
        	}else{
        		adapter.put(resources.context_menu_icon, Locale.getString("s_contact_info"), 20);
        	}
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0xD://Contact visibility list
        	adapter = new UAdapter();
        	adapter.setPadding(5);
        	switch(context_contact.getPrivacy()){
        	case Contact.CL_PRIV_TYPE_NONE:
            	if(context_contact.itIsTransport()){
            		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.visible_list)
    	            	adapter.put(resources.context_menu_icon, Locale.getString("s_add_to_vis"), 13);
            		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.invisible_list)
    	            	adapter.put(resources.context_menu_icon, Locale.getString("s_add_to_invis"), 14);
            		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.ignore_list)
    	            	adapter.put(resources.context_menu_icon, Locale.getString("s_add_to_ignore"), 15);
            		if(context_contact.profile.getTransportByID(context_contact.getTransportId()).params.move_to_ignore)
    	            	adapter.put(resources.context_menu_icon, Locale.getString("s_move_to_ignore"), 16);
            	}else{
	            	adapter.put(resources.context_menu_icon, Locale.getString("s_add_to_vis"), 13);
	            	adapter.put(resources.context_menu_icon, Locale.getString("s_add_to_invis"), 14);
	            	adapter.put(resources.context_menu_icon, Locale.getString("s_add_to_ignore"), 15);
	            	adapter.put(resources.context_menu_icon, Locale.getString("s_move_to_ignore"), 16);
            	}
        		break;
        	case Contact.CL_PRIV_TYPE_VISIBLE_LIST:
            	adapter.put(resources.context_menu_icon, Locale.getString("s_del_from_vis"), 17);
        		break;
        	case Contact.CL_PRIV_TYPE_INVISIBLE_LIST:
            	adapter.put(resources.context_menu_icon, Locale.getString("s_del_from_invis"), 18);
        		break;
        	case Contact.CL_PRIV_TYPE_IGNORE_LIST:
            	adapter.put(resources.context_menu_icon, Locale.getString("s_del_from_ignore"), 19);
        		break;
        	}
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0xC://Moving to list
    		final UAdapter adapter_ = new UAdapter();
    		adapter_.setPadding(5);
    		Vector<Group> list = null;
			switch(roster_operation_confirm_helper){
			case RosterOperation.CONTACT_MODIFY:
				list = context_contact.getProfile().getGroups();
				break;
			case RosterOperation.GROUP_MODIFY:
				list = context_group.getProfile().getGroups();
				break;
			case RosterOperation.NOTE_MODIFY:
				list = context_note.getProfile().getGroups();
				break;
			}
			adapter_.put("["+Locale.getString("s_without_group")+"]", 0);
    		for(int i=0; i<list.size(); i++)
    			adapter_.put(list.get(i).getName(), list.get(i).getItemId());
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter_,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int arg2, long arg3) {
							switch(roster_operation_confirm_helper){
							case RosterOperation.CONTACT_MODIFY:
								if(context_contact.getGroupId() == (int)adapter_.getItemId(arg2)){
									service.showDialogInContactList(Locale.getString("s_moving"), utilities.match(Locale.getString("s_contact_moving_error_1"), new String[]{context_contact.getName()}));
									return;
								}
								context_contact.getProfile().moveContact(context_contact.getItemId(), (int)adapter_.getItemId(arg2));
								break;
							case RosterOperation.GROUP_MODIFY:
								if(context_group.getGroupId() == (int)adapter_.getItemId(arg2)){
									service.showDialogInContactList(Locale.getString("s_moving"), utilities.match(Locale.getString("s_group_moving_error_1"), new String[]{context_group.getName()}));
									return;
								}
								if(context_group.getItemId() == (int)adapter_.getItemId(arg2)){
									service.showDialogInContactList(Locale.getString("s_moving"), Locale.getString("s_group_moving_error_2"));
									return;
								}
								context_group.getProfile().moveGroup(context_group.getItemId(), (int)adapter_.getItemId(arg2));
								break;
							case RosterOperation.NOTE_MODIFY:
								if(context_note.getGroupId() == (int)adapter_.getItemId(arg2)){
									service.showDialogInContactList(Locale.getString("s_moving"), utilities.match(Locale.getString("s_contact_moving_error_1"), new String[]{context_note.getName()}));
									return;
								}
								context_note.getProfile().moveNote(context_note.getItemId(), (int)adapter_.getItemId(arg2));
								break;
							}
							removeDialog(0xC);
						}
        			});
    		break;
    	case 0x6://Group context
        	adapter = new UAdapter();
        	adapter.setPadding(5);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_rename"), 8);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_delete"), 9);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_move"), 12);
        	dialog = DialogBuilder.createWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0x5://Confirmer
    		String text = "";
    		switch(roster_operation_confirm_helper){
    		case RosterOperation.CONTACT_REMOVE:
    			text = utilities.match(Locale.getString("s_delete_contact_confirm"), new String[]{context_contact.getName()});
    			break;
    		case RosterOperation.GROUP_REMOVE:
    			text = utilities.match(Locale.getString("s_delete_group_confirm"), new String[]{context_group.getName()});
    			break;
    		case RosterOperation.NOTE_REMOVE:
    			text = utilities.match(Locale.getString("s_delete_note_confirm"), new String[]{context_note.getName()});
    			break;
    		}
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				Gravity.TOP,
    				Locale.getString("s_operation_confirming"),
    				text,
            		Locale.getString("s_yes"), Locale.getString("s_no"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(5);
    			    		switch(roster_operation_confirm_helper){
    			    		case RosterOperation.CONTACT_REMOVE:
    			    			if(context_contact.isTemporary()){
        							context_contact.getProfile().removeContactById(context_contact.getID(), context_contact.getTransportId());
    			    			}else{
        							context_contact.getProfile().deleteContact(context_contact.getID(), context_contact.getTransportId());
    			    			}
    			    			break;
    			    		case RosterOperation.GROUP_REMOVE:
    							context_group.getProfile().deleteGroup(context_group.getItemId());
    			    			break;
    			    		case RosterOperation.NOTE_REMOVE:
    			    			context_note.getProfile().deleteNote(context_note.getItemId());
    			    			break;
    			    		}
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(5);
    					}
    				});
    		break;
    	case 0x7://Rename dialog
    		LinearLayout lay = (LinearLayout)View.inflate(this, R.layout.rename_dialog, null);
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_type_in_new_name").setTextColor(ColorScheme.getColor(12));
    		final EditText name = (EditText)lay.findViewById(R.id.rename_dialog_name);
    		if(ColorScheme.initialized) name.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(name);
			switch(roster_operation_confirm_helper){
			case RosterOperation.CONTACT_MODIFY:
				if(context_contact == null) return null;
				name.setText(context_contact.getName());
				break;
			case RosterOperation.GROUP_MODIFY:
				if(context_group == null) return null;
				name.setText(context_group.getName());
				break;
			}
			name.setSelection(0, name.getText().toString().length());
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_renaming"),
            		Locale.getString("s_ok"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						String name_ = name.getText().toString();
    						if(name_.trim().length() == 0) return;
    						switch(roster_operation_confirm_helper){
    						case RosterOperation.CONTACT_MODIFY:
    							context_contact.getProfile().renameContact(context_contact.getItemId(), name_);
    							break;
    						case RosterOperation.GROUP_MODIFY:
    							context_group.getProfile().renameGroup(context_group.getItemId(), name_);
    							break;
    						}
    						removeDialog(7);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(7);
    					}
    				});
    		break;
    	case 0x8://Contact-list menu
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_add_contact"), 10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_add_group"), 11);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_add_transport"), 28);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_add_note"), 29);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_moved_to_ignore"), 27);
        	dialog = DialogBuilder.create(ContactListActivity.this,
        			Locale.getString("s_contact_list"),
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0x9://Add contact dialog
    		if(service.profiles.list.size() == 0) return null;
    		lay = (LinearLayout)View.inflate(this, R.layout.add_contact_dialog, null);
    		if(ColorScheme.initialized){
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_select_profile").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_contact_account").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l3)), "s_contact_name").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l4)), "s_select_group").setTextColor(ColorScheme.getColor(12));
    		}
    		final ListView profiles = (ListView)lay.findViewById(R.id.add_contact_dialog_profiles);
    		Interface.attachBackground(profiles, Interface.list_view_back);
    		Interface.attachSelector(profiles);
    		final UAdapter adapter_a = new UAdapter();
    		BimoidProfile temp_profile = null;
    		if(adding_temporary){
    			temp_profile = context_contact.profile;
    			adapter_a.put(context_contact.getProfile().nickname, 0);
    		}else{
    			for(int i=0; i<service.profiles.list.size(); i++){
    				adapter_a.put(service.profiles.list.get(i).nickname, i);
    			}
    		}
    		//adapter_a.setSelected(0);
    		profiles.setAdapter(adapter_a);
    		final EditText account = (EditText)lay.findViewById(R.id.add_contact_dialog_account);
    		if(ColorScheme.initialized) account.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(account);
    		if(adding_temporary){
    			account.setText(context_contact.getID());
    		}
    		final EditText nickname = (EditText)lay.findViewById(R.id.add_contact_dialog_name);
    		if(ColorScheme.initialized) nickname.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(nickname);
    		final ListView groups = (ListView)lay.findViewById(R.id.add_contact_dialog_groups);
    		Interface.attachBackground(groups, Interface.list_view_back);
    		Interface.attachSelector(groups);
    		final UAdapter adapter_b = new UAdapter();
    		groups.setAdapter(adapter_b);

    		
			adapter_a.setSelected(0);
			BimoidProfile profile = service.profiles.list.get(0);
			Vector<Group> list2 = profile.getGroups();
			adapter_b.clear();
			adapter_b.put("["+Locale.getString("s_without_group")+"]", 0);
    		for(int i=0; i<list2.size(); i++)
    			adapter_b.put(list2.get(i).getName(), list2.get(i).getItemId());

			adapter_b.setSelected(0);
    		
    		profiles.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					adapter_a.setSelected(arg2);
					BimoidProfile profile = service.profiles.list.get(arg2);
					Vector<Group> list = profile.getGroups();
					adapter_b.clear();
	    			adapter_b.put("["+Locale.getString("s_without_group")+"]", 0);
		    		for(int i=0; i<list.size(); i++)
		    			adapter_b.put(list.get(i).getName(), list.get(i).getItemId());
					adapter_b.setSelected(0);
				}
    		});
    		adding_temporary = false;
    		groups.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					adapter_b.setSelected(arg2);
				}
    		});
    		final BimoidProfile temp_profile1 = temp_profile;
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_add_contact"),
            		Locale.getString("s_do_add"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						if(adapter_a.getSelectedIdx() < 0) return;
    						if(adapter_b.getSelectedIdx() < 0) return;
    						String account_ = account.getText().toString().toLowerCase().trim();
    						if(account_.length() == 0) return;
    						String nickname_ = nickname.getText().toString().trim();
    						if(nickname_.length() == 0){
    							nickname_ = account_;
    						}
    						BimoidProfile profile = (temp_profile1 == null)? service.profiles.list.get(adapter_a.getSelectedIdx()): temp_profile1;
    						int parent_id = (int)adapter_b.getItemId(adapter_b.getSelectedIdx());
    						if(profile == null) return;
    						profile.addContact(account_, nickname_, parent_id);
    						removeDialog(9);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(9);
    					}
    				});
    		break;
    	case 0x21://Add/Modify note dialog
			final boolean modify = roster_operation_confirm_helper == RosterOperation.NOTE_MODIFY;

    		if(service.profiles.list.size() == 0) return null;
    		final Int selected_type = new Int();
    		selected_type.VALUE = NoteItem.CL_NOTE_TYPE_TEXT;
    		lay = (LinearLayout)View.inflate(this, R.layout.add_note_dialog, null);
    		final TextView l1 = (TextView)lay.findViewById(R.id.l1);
    		final TextView l2 = (TextView)lay.findViewById(R.id.l2);
    		final TextView l3 = (TextView)lay.findViewById(R.id.l3);
    		final TextView l4 = (TextView)lay.findViewById(R.id.l4);
    		if(ColorScheme.initialized){
    			l1.setTextColor(ColorScheme.getColor(12));
        		l2.setTextColor(ColorScheme.getColor(12));
        		l3.setTextColor(ColorScheme.getColor(12));
        		l4.setTextColor(ColorScheme.getColor(12));
    		}
    		l1.setText(Locale.getString("s_select_profile"));
    		l4.setText(Locale.getString("s_select_group"));
    		l2.setText(Locale.getString("s_note_name"));
    		l3.setText(Locale.getString("s_note_text"));
    		final LinearLayout type_lay = (LinearLayout)lay.findViewById(R.id.add_note_type_dialog_type_list_layout);
    		type_lay.setOnTouchListener(new OnTouchListener(){
				@Override
				public boolean onTouch(View arg0, MotionEvent arg1) {
					if(arg1.getAction() == MotionEvent.ACTION_DOWN){
						type_lay.setVisibility(View.GONE);
						return true;
					}
					return false;
				}
    		});
    		type_lay.setVisibility(View.GONE);
    		final ListView type_list = (ListView)lay.findViewById(R.id.add_note_dialog_type_list);
    		Interface.attachSelector(type_list);
    		final UAdapter type_adapter = new UAdapter();
    		type_adapter.setPadding(5);
    		type_adapter.setTextColor(ColorScheme.getColor(12));
    		type_adapter.put(getResources().getDrawable(R.drawable.note), Locale.getString("s_note_type_text"), NoteItem.CL_NOTE_TYPE_TEXT);
    		type_adapter.put(getResources().getDrawable(R.drawable.note_prog), Locale.getString("s_note_type_file"), NoteItem.CL_NOTE_TYPE_COMMAND);
    		type_adapter.put(getResources().getDrawable(R.drawable.note_web), Locale.getString("s_note_type_link"), NoteItem.CL_NOTE_TYPE_LINK);
    		type_adapter.put(getResources().getDrawable(R.drawable.note_mail), Locale.getString("s_note_type_mail"), NoteItem.CL_NOTE_TYPE_EMAIL);
    		type_adapter.put(getResources().getDrawable(R.drawable.note_phone), Locale.getString("s_note_type_phone"), NoteItem.CL_NOTE_TYPE_PHONE);
    		type_list.setAdapter(type_adapter);
    		final Button note_type = (Button)lay.findViewById(R.id.add_note_dialog_type_btn);
    		final Button select_file = (Button)lay.findViewById(R.id.add_note_dialog_select_file_btn);
    		if(ColorScheme.initialized){
    			note_type.setTextColor(ColorScheme.getColor(24));
    			select_file.setTextColor(ColorScheme.getColor(24));
    		}
    		Interface.attachButtonStyle(note_type);
    		Interface.attachButtonStyle(select_file);
    		select_file.setVisibility(View.GONE);
    		type_list.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		    		type_lay.setVisibility(View.GONE);
		    		select_file.setVisibility(View.GONE);
					selected_type.VALUE = (int)arg0.getAdapter().getItemId(arg2);
					switch(selected_type.VALUE){
					case NoteItem.CL_NOTE_TYPE_TEXT:
			    		l2.setText(Locale.getString("s_note_name"));
			    		l3.setText(Locale.getString("s_note_text"));
						note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note)), null, null, null);
						break;
					case NoteItem.CL_NOTE_TYPE_COMMAND:
			    		l2.setText(Locale.getString("s_prog_note_name"));
			    		l3.setText(Locale.getString("s_prog_note_text"));
						note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_prog)), null, null, null);
			    		select_file.setVisibility(View.VISIBLE);
						break;
					case NoteItem.CL_NOTE_TYPE_LINK:
			    		l2.setText(Locale.getString("s_link_note_name"));
			    		l3.setText(Locale.getString("s_link_note_text"));
						note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_web)), null, null, null);
						break;
					case NoteItem.CL_NOTE_TYPE_EMAIL:
			    		l2.setText(Locale.getString("s_mail_note_name"));
			    		l3.setText(Locale.getString("s_mail_note_text"));
						note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_mail)), null, null, null);
						break;
					case NoteItem.CL_NOTE_TYPE_PHONE:
			    		l2.setText(Locale.getString("s_phone_note_name"));
			    		l3.setText(Locale.getString("s_phone_note_text"));
						note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_phone)), null, null, null);
						break;
					}
				}
    		});
    		if(modify){
        		selected_type.VALUE = context_note.TYPE;
				switch(context_note.TYPE){
				case NoteItem.CL_NOTE_TYPE_TEXT:
		    		l2.setText(Locale.getString("s_note_name"));
		    		l3.setText(Locale.getString("s_note_text"));
					note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note)), null, null, null);
					break;
				case NoteItem.CL_NOTE_TYPE_COMMAND:
		    		l2.setText(Locale.getString("s_prog_note_name"));
		    		l3.setText(Locale.getString("s_prog_note_text"));
					note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_prog)), null, null, null);
		    		select_file.setVisibility(View.VISIBLE);
					break;
				case NoteItem.CL_NOTE_TYPE_LINK:
		    		l2.setText(Locale.getString("s_link_note_name"));
		    		l3.setText(Locale.getString("s_link_note_text"));
					note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_web)), null, null, null);
					break;
				case NoteItem.CL_NOTE_TYPE_EMAIL:
		    		l2.setText(Locale.getString("s_mail_note_name"));
		    		l3.setText(Locale.getString("s_mail_note_text"));
					note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_mail)), null, null, null);
					break;
				case NoteItem.CL_NOTE_TYPE_PHONE:
		    		l2.setText(Locale.getString("s_phone_note_name"));
		    		l3.setText(Locale.getString("s_phone_note_text"));
					note_type.setCompoundDrawables(utilities.setDrawableBounds(getResources().getDrawable(R.drawable.note_phone)), null, null, null);
					break;
				}
    		}
    		note_type.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
		    		type_lay.setVisibility(View.VISIBLE);
				}
    		});
    		final ListView profiles2 = (ListView)lay.findViewById(R.id.add_note_dialog_profiles);
    		if(modify){
    			l1.setVisibility(View.GONE);
    			profiles2.setVisibility(View.GONE);
    		}
    		Interface.attachBackground(profiles2, Interface.list_view_back);
    		Interface.attachSelector(profiles2);
    		final UAdapter adapter_a1 = new UAdapter();
    		//adapter_a.setSelected(0);
    		profiles2.setAdapter(adapter_a1);
    		final EditText note_name = (EditText)lay.findViewById(R.id.add_note_dialog_name);
    		if(ColorScheme.initialized) note_name.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(note_name);
    		final EditText note_text = (EditText)lay.findViewById(R.id.add_note_dialog_text);
    		if(ColorScheme.initialized) note_text.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(note_text);
        	if(modify){
        		note_name.setText(context_note.name);
        		note_text.setText(context_note.TEXT);
        	}
    		select_file.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					FILE_NOTE_SELECT_FILE_HELPER = note_text;
					if(context_note == null){
						Intent exec = new Intent("get_file:");
						exec.setClass(getApplicationContext(), FileBrowserActivity.class);
						startActivityForResult(exec, 0);
					}else{
						Intent exec = new Intent("get_file:"+context_note.TEXT);
						exec.setClass(getApplicationContext(), FileBrowserActivity.class);
						startActivityForResult(exec, 0);
					}
				}
    		});
    		final ListView groups1 = (ListView)lay.findViewById(R.id.add_note_dialog_groups);
    		Interface.attachBackground(groups1, Interface.list_view_back);
    		Interface.attachSelector(groups1);
    		final UAdapter adapter_b1 = new UAdapter();
    		groups1.setAdapter(adapter_b1);
    		
			for(int i=0; i<service.profiles.list.size(); i++)
				adapter_a1.put(service.profiles.list.get(i).nickname, i);

			adapter_a1.setSelected(0);
			profile = modify? context_note.profile: service.profiles.list.get(0);
			list2 = profile.getGroups();
			adapter_b1.clear();
			adapter_b1.put("["+Locale.getString("s_without_group")+"]", 0);
			int selected = 0;
    		for(int i=0; i<list2.size(); i++){
    			final int id = list2.get(i).getItemId();
    			adapter_b1.put(list2.get(i).getName(), id);
    			if(modify)
    				if(context_note.group_id == id)
    					selected = i+1;
    		}
    		//Log.e("Selected", ""+selected);
			adapter_b1.setSelected(selected);
    		
    		profiles2.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					adapter_a1.setSelected(arg2);
					BimoidProfile profile = service.profiles.list.get(arg2);
					Vector<Group> list = profile.getGroups();
					adapter_b1.clear();
	    			adapter_b1.put("["+Locale.getString("s_without_group")+"]", 0);
		    		for(int i=0; i<list.size(); i++)
		    			adapter_b1.put(list.get(i).getName(), list.get(i).getItemId());
					adapter_b1.setSelected(0);
				}
    		});
    		groups1.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					adapter_b1.setSelected(arg2);
				}
    		});
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				(modify? Locale.getString("s_modify_note"): Locale.getString("s_add_note")),
            		modify? Locale.getString("s_do_change"): Locale.getString("s_do_add"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						if(adapter_a1.getSelectedIdx() < 0) return;
    						if(adapter_b1.getSelectedIdx() < 0) return;
    						String name_ = note_name.getText().toString().trim();
    						if(name_.length() == 0) return;
    						String text_ = note_text.getText().toString().trim();
    						//if(text_.length() == 0){
    						//	text_ = name_;
    						//}
    						BimoidProfile profile = service.profiles.list.get(adapter_a1.getSelectedIdx());
    						int parent_id = (int)adapter_b1.getItemId(adapter_b1.getSelectedIdx());
    						if(profile == null) return;
    						if(profile.noteNameExist(name_)){
    							Toast t = Toast.makeText(ContactListActivity.this, Locale.getString("s_note_elready_exist"), Toast.LENGTH_LONG);
    							t.setGravity(Gravity.TOP, 0, 0);
    							t.show();
    							return;
    						}
    						if(modify){
    							context_note.profile.modifyNote(context_note.item_id, parent_id, name_, text_, (byte)selected_type.VALUE, context_note.TIMESTAMP);
    						}else{
        						profile.addNote(name_, text_, (byte)selected_type.VALUE, parent_id);
    						}
    						removeDialog(0x21);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(0x21);
    					}
    				});
    		break;
    	case 0x14://Add transport dialog
    		if(service.profiles.list.size() == 0) return null;
    		lay = (LinearLayout)View.inflate(this, R.layout.add_transport_dialog, null);
    		if(ColorScheme.initialized){
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_select_profile").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_login").setTextColor(ColorScheme.getColor(12)); 
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l3)), "s_password").setTextColor(ColorScheme.getColor(12)); 
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l4)), "s_transport").setTextColor(ColorScheme.getColor(12)); 
    		}
     		final ListView profiles1 = (ListView)lay.findViewById(R.id.add_transport_dialog_profiles);
     		final ListView transports = (ListView)lay.findViewById(R.id.add_transport_transports);
    		Interface.attachBackground(profiles1, Interface.list_view_back);
    		Interface.attachSelector(profiles1);
    		Interface.attachBackground(transports, Interface.list_view_back);
    		Interface.attachSelector(transports);
    		final UAdapter p_adapter = new UAdapter();
    		p_adapter.setMode(UAdapter.FORCE_HIDE_ICON);
    		for(int i=0; i<service.profiles.list.size(); i++)
    			p_adapter.put(service.profiles.list.get(i).nickname, i);
    		profiles1.setAdapter(p_adapter);
        	p_adapter.setSelected(0);
        	
    		final UAdapter t_adapter = new UAdapter();
			profile = service.profiles.list.get(0);
			Vector<TransportParams> t = profile.transport_params;
			t_adapter.clear();
    		for(int i=0; i<t.size(); i++)
    			t_adapter.put(t.get(i).getLogo(), t.get(i).full_name, i);
    		if(t_adapter.getCount() > 0) t_adapter.setSelected(0);
    		transports.setAdapter(t_adapter);

			profiles1.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					//Log.e("ProfilesAdapter", "Selected idx: "+arg2);
					p_adapter.setSelected(arg2);
					final BimoidProfile profile = service.profiles.list.get(arg2);
					final Vector<TransportParams> list = profile.transport_params;
					t_adapter.clear();
		    		for(int i=0; i<list.size(); i++)
		    			t_adapter.put(list.get(i).getLogo(), list.get(i).full_name, i);
		    		if(t_adapter.getCount() > 0){
		    			t_adapter.setSelected(0);
		    		}
				}
    		});
			
    		final EditText login = (EditText)lay.findViewById(R.id.add_transport_dialog_account);
    		if(ColorScheme.initialized) login.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(login);
    		final EditText pass = (EditText)lay.findViewById(R.id.add_transport_dialog_pass);
    		if(ColorScheme.initialized) pass.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(pass);
    		
        	transports.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
		    		t_adapter.setSelected(arg2);
				}
    		});

			dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_add_transport"),
            		Locale.getString("s_do_add"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						if(p_adapter.getSelectedIdx() < 0) return;
    						if(t_adapter.getSelectedIdx() < 0) return;
    						String login_ = login.getText().toString().toLowerCase().trim();
    						if(login_.length() == 0) return;
    						String pass_ = pass.getText().toString().trim();
       						if(pass_.length() == 0) return;
    						BimoidProfile profile = service.profiles.list.get(p_adapter.getSelectedIdx());
    						if(profile == null) return;
    						if(profile.countTransports() > profile.max_transports){
    							service.showToast(utilities.match(Locale.getString("s_add_transport_limit_notify"), new String[]{String.valueOf(profile.max_transports)}), Toast.LENGTH_LONG);
    							return;
    						}
    						TransportParams params = profile.transport_params.get(t_adapter.getSelectedIdx());
    						profile.addTransport(login_, pass_, params);
    						removeDialog(0x14);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(0x14);
    					}
    				});
    		break;
    	case 0x18://Add transport contact dialog
    		if(service.profiles.list.size() == 0) return null;
    		if(adding_temporary){
    			context_transport = context_contact.profile.getTransportByID(context_contact.getTransportId());
    		}
    		lay = (LinearLayout)View.inflate(this, R.layout.add_tcontact_dialog, null);
    		final BimoidProfile tprofile = context_transport.profile;
    		final EditText taccount = (EditText)lay.findViewById(R.id.add_contact_dialog_account);
        	Interface.attachEditTextStyle(taccount);
    		final EditText tnickname = (EditText)lay.findViewById(R.id.add_contact_dialog_name);
        	Interface.attachEditTextStyle(tnickname);
        	if(ColorScheme.initialized){
        		taccount.setTextColor(ColorScheme.getColor(13));
        		tnickname.setTextColor(ColorScheme.getColor(13));
        		utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_contact_account").setTextColor(ColorScheme.getColor(12));
        		utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_contact_name").setTextColor(ColorScheme.getColor(12));
        		utilities.setLabel(((TextView)lay.findViewById(R.id.l3)), "s_select_group").setTextColor(ColorScheme.getColor(12));
        	}
    		final ListView tgroups = (ListView)lay.findViewById(R.id.add_contact_dialog_groups);
    		Interface.attachBackground(tgroups, Interface.list_view_back);
    		Interface.attachSelector(tgroups);
    		final UAdapter tg_adapter = new UAdapter();
    		tgroups.setAdapter(tg_adapter);
    		
    		if(adding_temporary) taccount.setText(context_contact.getID());
    		
			Vector<Group> tlist = tprofile.getGroups();
			tg_adapter.clear();
			tg_adapter.put("["+Locale.getString("s_without_group")+"]", 0);
    		for(int i=0; i<tlist.size(); i++)
    			tg_adapter.put(tlist.get(i).getName(), tlist.get(i).getItemId());

    		tg_adapter.setSelected(0);
    		
    		tgroups.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					tg_adapter.setSelected(arg2);
				}
    		});
    		adding_temporary = false;
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_add_contact"),
            		Locale.getString("s_do_add"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						if(tg_adapter.getSelectedIdx() < 0) return;
    						String account_ = taccount.getText().toString().toLowerCase().trim();
    						if(account_.length() == 0) return;
    						String nickname_ = tnickname.getText().toString().trim();
    						if(nickname_.length() == 0){
    							nickname_ = account_;
    						}
    						BimoidProfile profile = context_transport.profile;
    						int parent_id = (int)tg_adapter.getItemId(tg_adapter.getSelectedIdx());
    						if(profile == null) return;
    						profile.addTransportContact(account_, nickname_, parent_id, context_transport);
    						removeDialog(0x18);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(0x18);
    					}
    				});
    		break;
    	case 0xA://Add group dialog
    		lay = (LinearLayout)View.inflate(this, R.layout.add_group_dialog, null);
    		if(ColorScheme.initialized){
        		utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_select_profile").setTextColor(ColorScheme.getColor(12));
        		utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_group_name").setTextColor(ColorScheme.getColor(12));
        		utilities.setLabel(((TextView)lay.findViewById(R.id.l3)), "s_parent_group").setTextColor(ColorScheme.getColor(12));
    		}
    		final ListView profiles_ = (ListView)lay.findViewById(R.id.add_group_dialog_profiles);
    		Interface.attachBackground(profiles_, Interface.list_view_back);
    		Interface.attachSelector(profiles_);
    		final UAdapter adapter_a_ = new UAdapter();
    		for(int i=0; i<service.profiles.list.size(); i++){
    			adapter_a_.put(service.profiles.list.get(i).nickname, i);
    		}
    		profiles_.setAdapter(adapter_a_);
    		final EditText name_a = (EditText)lay.findViewById(R.id.add_group_dialog_name);
    		if(ColorScheme.initialized) name_a.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(name_a);
    		final ListView groups_ = (ListView)lay.findViewById(R.id.add_group_dialog_groups);
    		Interface.attachBackground(groups_, Interface.list_view_back);
    		Interface.attachSelector(groups_);
    		final UAdapter adapter_b_ = new UAdapter();
    		groups_.setAdapter(adapter_b_);

			adapter_a_.setSelected(0);
			BimoidProfile profile1 = service.profiles.list.get(0);
			Vector<Group> list1 = profile1.getGroups();
			adapter_b_.clear();
			adapter_b_.put("["+Locale.getString("s_without_group")+"]", 0);
    		for(int i=0; i<list1.size(); i++)
    			adapter_b_.put(list1.get(i).getName(), list1.get(i).getItemId());

			adapter_b_.setSelected(0);

    		profiles_.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					adapter_a_.setSelected(arg2);
					BimoidProfile profile = service.profiles.list.get(arg2);
					Vector<Group> list = profile.getGroups();
					adapter_b_.clear();
					adapter_b_.put("["+Locale.getString("s_without_group")+"]", 0);
		    		for(int i=0; i<list.size(); i++)
		    			adapter_b_.put(list.get(i).getName(), list.get(i).getItemId());
					adapter_b_.setSelected(0);
				}
    		});
    		groups_.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					adapter_b_.setSelected(arg2);
				}
    		});
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_add_group"),
            		Locale.getString("s_do_add"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						if(adapter_a_.getSelectedIdx() < 0) return;
    						if(adapter_b_.getSelectedIdx() < 0) return;
    						String name_ = name_a.getText().toString().trim();
    						if(name_.length() == 0) return;
    						BimoidProfile profile = service.profiles.list.get(adapter_a_.getSelectedIdx());
    						int parent_id = (int)adapter_b_.getItemId(adapter_b_.getSelectedIdx());
    						if(profile == null) return;
    						profile.addGroup(name_, parent_id);
    						removeDialog(10);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(10);
    					}
    				});
    		break;
    	case 0xB://Auth revoke confirm
    		lay = (LinearLayout)View.inflate(this, R.layout.auth_revoke_reason_input, null);
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_type_in_auth_revoke_text").setTextColor(ColorScheme.getColor(12));
    		final EditText reason = (EditText)lay.findViewById(R.id.auth_revoke_reason_input);
    		if(ColorScheme.initialized) reason.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(reason);
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_revoke_auth"),
            		Locale.getString("s_do_revoke"), "Отмена",
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						String reason_ = reason.getText().toString().trim();
    						if(reason_.length() == 0) return;
    						if(context_contact == null) return;
    						context_contact.getProfile().sendAuthRev(context_contact.getID(), reason_, context_contact.getTransportId());
    						removeDialog(11);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(11);
    					}
    				});
    		break;
    	case 0xE://Account info
    		if(account_info_for_display == null) return null;
    		String information = "";
    		lay = (LinearLayout)View.inflate(this, R.layout.account_info_dialog, null);
    		if(ColorScheme.initialized){
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_vcard_name").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_vcard_surname").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l3)), "s_vcard_country").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l4)), "s_vcard_region").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l5)), "s_vcard_city").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l6)), "s_vcard_zip").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l7)), "s_vcard_address").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l8)), "s_vcard_languages").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l9)), "s_vcard_gender_birthday").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l10)), "s_vcard_home_page").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l11)), "s_vcard_about").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l12)), "s_vcard_interests").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l13)), "s_vcard_email").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l14)), "s_vcard_tels").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l15)), "s_vcard_company").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l16)), "s_vcard_dept").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l17)), "s_vcard_position").setTextColor(ColorScheme.getColor(12));
    		}
    		
    		TextView nick_name = (TextView)lay.findViewById(R.id.info_dialog_nickname);
    		if(ColorScheme.initialized) nick_name.setTextColor(ColorScheme.getColor(12));
    		 nick_name.setText(utilities.filter(account_info_for_display.nick_name));
     		TextView first_name = (TextView)lay.findViewById(R.id.info_dialog_firstname);
    		if(ColorScheme.initialized) first_name.setTextColor(ColorScheme.getColor(12));
     		 first_name.setText(utilities.filter(account_info_for_display.first_name));
    		TextView about = (TextView)lay.findViewById(R.id.info_dialog_about);
    		if(ColorScheme.initialized) about.setTextColor(ColorScheme.getColor(12));
    		 about.setText(utilities.filter(account_info_for_display.about));
    		TextView address = (TextView)lay.findViewById(R.id.info_dialog_address);
    		if(ColorScheme.initialized) address.setTextColor(ColorScheme.getColor(12));
    		 address.setText(utilities.filter(account_info_for_display.address));
    		TextView city = (TextView)lay.findViewById(R.id.info_dialog_city);
    		if(ColorScheme.initialized) city.setTextColor(ColorScheme.getColor(12));
    		 city.setText(utilities.filter(account_info_for_display.city));
    		TextView company = (TextView)lay.findViewById(R.id.info_dialog_company);
    		if(ColorScheme.initialized) company.setTextColor(ColorScheme.getColor(12));
    		 company.setText(utilities.filter(account_info_for_display.company));
    		TextView country = (TextView)lay.findViewById(R.id.info_dialog_country);
    		if(ColorScheme.initialized) country.setTextColor(ColorScheme.getColor(12));
    		 country.setText(utilities.filter(account_info_for_display.country));
    		TextView dept = (TextView)lay.findViewById(R.id.info_dialog_dept);
    		if(ColorScheme.initialized) dept.setTextColor(ColorScheme.getColor(12));
    		 dept.setText(utilities.filter(account_info_for_display.departament));
    		TextView emails = (TextView)lay.findViewById(R.id.info_dialog_emails);
    		if(ColorScheme.initialized) emails.setTextColor(ColorScheme.getColor(12));
    		 emails.setText("1: "+utilities.filter(account_info_for_display.email)
    				 +"\n2: "+utilities.filter(account_info_for_display.additional_email));
    		TextView gender_birthday = (TextView)lay.findViewById(R.id.info_dialog_gender_birthday);
    		if(ColorScheme.initialized) gender_birthday.setTextColor(ColorScheme.getColor(12));
    		if(account_info_for_display.birthday != 0){
    		 gender_birthday.setText(Locale.getString("s_birthday")+": "+utilities.formatBirthdayTimestamp(account_info_for_display.birthday)+
    				 "\n"+Locale.getString("s_gender")+": "+account_info_for_display.gender);
    		}else{
       		 gender_birthday.setText(Locale.getString("s_birthday")+": - \n"+Locale.getString("s_gender")+": "+account_info_for_display.gender);
    		}
    		TextView homepage = (TextView)lay.findViewById(R.id.info_dialog_homepage);
    		if(ColorScheme.initialized) homepage.setTextColor(ColorScheme.getColor(12));
    		 homepage.setText(utilities.filter(account_info_for_display.homepage));
    		TextView interests = (TextView)lay.findViewById(R.id.info_dialog_interests);
    		if(ColorScheme.initialized) interests.setTextColor(ColorScheme.getColor(12));
    		 interests.setText(utilities.filter(account_info_for_display.interests));
    		TextView languages = (TextView)lay.findViewById(R.id.info_dialog_languages);
    		if(ColorScheme.initialized) languages.setTextColor(ColorScheme.getColor(12));
    		 languages.setText("1: "+utilities.filter(account_info_for_display.language)+
    				 "\n2: "+utilities.filter(account_info_for_display.additional_language));
    		TextView lastname = (TextView)lay.findViewById(R.id.info_dialog_lastname);
    		if(ColorScheme.initialized) lastname.setTextColor(ColorScheme.getColor(12));
    		 lastname.setText(utilities.filter(account_info_for_display.last_name));
    		TextView phones = (TextView)lay.findViewById(R.id.info_dialog_phones);
    		if(ColorScheme.initialized) phones.setTextColor(ColorScheme.getColor(12));
    		 phones.setText(Locale.getString("s_home_phone")+": "+utilities.filter(account_info_for_display.home_phone)+"\n"+
    				 Locale.getString("s_work_phone")+": "+utilities.filter(account_info_for_display.work_phone)+"\n"+
    				 Locale.getString("s_cellular_phone")+": "+utilities.filter(account_info_for_display.cellular_phone)+"\n"+
    				 Locale.getString("s_fax")+": "+utilities.filter(account_info_for_display.fax_number));
    		TextView position = (TextView)lay.findViewById(R.id.info_dialog_position);
    		if(ColorScheme.initialized) position.setTextColor(ColorScheme.getColor(12));
    		 position.setText(utilities.filter(account_info_for_display.position));
    		TextView region = (TextView)lay.findViewById(R.id.info_dialog_region);
    		if(ColorScheme.initialized) region.setTextColor(ColorScheme.getColor(12));
    		 region.setText(utilities.filter(account_info_for_display.region));
    		TextView zipcode = (TextView)lay.findViewById(R.id.info_dialog_zipcode);
    		if(ColorScheme.initialized) zipcode.setTextColor(ColorScheme.getColor(12));
    		 zipcode.setText(utilities.filter(account_info_for_display.zipcode));
    		 information+=Locale.getString("s_nick")+": "+nick_name.getText()+"\n";
    		 information+=Locale.getString("s_name")+": "+first_name.getText()+"\n";
    		 information+=Locale.getString("s_surname")+": "+lastname.getText()+"\n";
    		 information+=Locale.getString("s_country")+": "+country.getText()+"\n";
    		 information+=Locale.getString("s_region")+": "+region.getText()+"\n";
    		 information+=Locale.getString("s_city")+": "+city.getText()+"\n";
    		 information+=Locale.getString("s_zipcode")+": "+zipcode.getText()+"\n";
    		 information+=Locale.getString("s_address")+": "+address.getText()+"\n";
    		 information+=Locale.getString("s_languages")+":\n"+languages.getText()+"\n";
    		 information+=Locale.getString("s_gender")+"/"+Locale.getString("s_birthday")+":\n"+gender_birthday.getText()+"\n";
    		 information+=Locale.getString("s_homepage")+": "+homepage.getText()+"\n";
    		 information+=Locale.getString("s_about_user")+": "+about.getText()+"\n";
    		 information+=Locale.getString("s_interests")+": "+interests.getText()+"\n";
    		 information+=Locale.getString("s_email")+":\n"+emails.getText()+"\n";
    		 information+=Locale.getString("s_phones")+":\n"+phones.getText()+"\n";
    		 information+=Locale.getString("s_company")+": "+company.getText()+"\n";
    		 information+=Locale.getString("s_dept")+": "+dept.getText()+"\n";
    		 information+=Locale.getString("s_position")+": "+position.getText()+"\n";
    		 final String completed_information = information;
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_information"),
            		Locale.getString("s_do_copy"), Locale.getString("s_close"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
    						cm.setText(completed_information);
    						removeDialog(0xE);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(0xE);
    					}
    				});
    		break;
    	case 0x11://Additional status description input dialog
    		lay = (LinearLayout)View.inflate(this, R.layout.additional_status_desc_input, null);
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_type_in_status_text").setTextColor(ColorScheme.getColor(12));
    		final EditText description_ = (EditText)lay.findViewById(R.id.additional_status_desc_input_text);
    		if(ColorScheme.initialized) description_.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(description_);
    		description_.setText(service.prefs.getString(context_profile.ID+"_ext_desc_"+selected_xstatus, ""));
    		dialog = DialogBuilder.createYesNo(ContactListActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_extended_status"),
            		Locale.getString("s_ok"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						String description = description_.getText().toString().trim();
    						if(context_profile == null) return;
    						context_profile.setExtStatus(selected_xstatus, description);
    						service.prefs.edit().putString(context_profile.ID+"_ext_desc_"+selected_xstatus, description).
    						putInt(context_profile.ID+"_ext_idx", selected_xstatus).commit();
    						removeDialog(0x11);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(0x11);
    					}
    				});
    		break;
    	case 0x12://Auth reason input
        	LinearLayout lay1 = (LinearLayout)View.inflate(this, R.layout.auth_reason_input, null);
        	final EditText reason_input = (EditText)lay1.findViewById(R.id.auth_reason_input);
        	reason_input.setText(Locale.getString("s_type_in_auth_req_template"));
    		if(ColorScheme.initialized) reason_input.setTextColor(ColorScheme.getColor(13));
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay1.findViewById(R.id.l1)), "s_type_in_auth_req_text").setTextColor(ColorScheme.getColor(12));
        	Interface.attachEditTextStyle(reason_input);
        	dialog = DialogBuilder.createYesNo(ContactListActivity.this, lay1,
        			Gravity.CENTER,
        			Locale.getString("s_authorization"),
        			Locale.getString("s_do_send"),
        			Locale.getString("s_cancel"),
    				new OnClickListener(){
						@Override
						public void onClick(View arg0) {
				        	String reason = reason_input.getText().toString();
							if(reason.trim().length() > 0){
								context_contact.getProfile().sendAuthReq(context_contact.getID(), reason, context_contact.getTransportId());
								removeDialog(0x12);
							}else{
								Toast.makeText(ContactListActivity.this, Locale.getString("s_error_message_header")+": "+Locale.getString("s_auth_request_error"), Toast.LENGTH_SHORT).show();
							}
						}
        			},
        			new OnClickListener(){
						@Override
						public void onClick(View v) {
							removeDialog(0x12);
						}
        			});
    		break;
    	case 0x15://Transport menu
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(context_transport.params.getStatus(context_transport.status), Locale.getString("s_status"), 0);
        	if(context_transport.params.additional_status_pic) adapter.put(context_transport.params.getAddStatus(context_transport.extended_status), Locale.getString("s_extended_status"), 1);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_add_contact"), 4);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_settings"), 2);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_delete"), 3);
        	dialog = DialogBuilder.create(ContactListActivity.this,
        			context_transport.params.name_of_account_ids+" "+context_transport.account_name,
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
								removeDialog(0x15);
								switch((int)(arg0.getAdapter().getItemId(arg2))){
								case 0:
									removeDialog(0x16);
									showDialog(0x16);
									break;
								case 1:
									removeDialog(0x17);
									showDialog(0x17);
									break;
								case 2:
									
									TransportSettingsActivity.launch(ContactListActivity.this, context_transport);
									
									//removeDialog(0x19);
									//showDialog(0x19);
									break;
								case 3:
									context_transport.profile.deleteTransport(context_transport.getItemId());
									break;
								case 4:
									if(!context_transport.connected){
										service.showToast(Locale.getString("s_add_transport_contact_error"), Toast.LENGTH_SHORT);
										return;
									}
									removeDialog(0x18);
									showDialog(0x18);
									break;
								}
							}
        				});
    		break;
    	case 0x17://Transport xstatus
        	adapter = new UAdapter();
        	adapter.setMode(UAdapter.FORCE_HIDE_LABEL);
        	adapter.setPadding(10);
        	//adapter.put(XStatus.getIcon(0), "", 0);
        	for(int i=0; i<context_transport.params.additional_status_pic_count+1; i++){
            	adapter.put(context_transport.params.getAddStatus(i), "", i, Gravity.CENTER);
        	}
        	dialog = DialogBuilder.createGridWithNoHeader(ContactListActivity.this,
        			adapter,
    				Gravity.CENTER,
    				5,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
								removeDialog(0x17);
								final int status = (int)arg0.getAdapter().getItemId(arg2);
								context_transport.setExtendedStatus(status);
								if(context_transport.connected){
									context_transport.getProfile().setTransportStatus(context_transport, context_transport.status, 2, status);
									return;
								}
							}
        				});
    		break;
    	case 0x16://Transport status
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(context_transport.params.getOnline(), Locale.getString("s_status_online"), 0);
        	for(int i=0; i<context_transport.params.status_wrapper.length; i++){
        		if(context_transport.params.status_wrapper[i] != 0)
        			adapter.put((context_transport.params.getStatus(context_transport.params.status_wrapper[i])), TransportParams.translateStatus(context_transport.params.status_wrapper[i]), context_transport.params.status_wrapper[i]);
        	}
        	adapter.put(context_transport.params.getOffline(), Locale.getString("s_status_offline"), -1);
        	dialog = DialogBuilder.create(ContactListActivity.this,
        			Locale.getString("s_status"),
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
								removeDialog(0x16);
								if(context_transport.account_name.length() == 0 || context_transport.account_pass.length() == 0){
									service.showToast(Locale.getString("s_transport_params_need_update"), Toast.LENGTH_SHORT);
						    		
						    		TransportSettingsActivity.launch(ContactListActivity.this, context_transport);
						    		
									return;
								}
								final int status = (int)arg0.getAdapter().getItemId(arg2);
								context_transport.setStatusA(status);
								if(status == -1 && context_transport.getStatus() >= 0){
									context_transport.getProfile().setTransportStatus(context_transport, status, 3, context_transport.extended_status);
									return;
								}
								if(status >= 0 && context_transport.getStatus() >= 0){
									context_transport.getProfile().setTransportStatus(context_transport, status, 2, context_transport.extended_status);
									return;
								}
								if(status >= 0 && context_transport.getStatus() == -1){
									context_transport.getProfile().setTransportStatus(context_transport, status, 1, context_transport.extended_status);
									return;
								}
							}
        				});
    		break;
     	}
    	last_shown_dialog = dialog;
    	dialog.setOnDismissListener(new OnDismissListener(){
			@Override
			public void onDismiss(DialogInterface arg0) {
		    	checkForBufferedDialogs();
			}
    	});
    	return dialog;
    }
    private void buildBottomPanel(){
    	ProfilesManager profiles = service.profiles;
    	if(profiles == null){
    		//Log.i("Bottom Panel", "PM is null!");
    		return;
    	}
    	bottom_panel.removeAllViews();
    	for(int i=0; i<profiles.list.size(); i++){
    		final BimoidProfile profile = profiles.list.get(i);
    		//=-=-=-=-=-=-= MAIN STATUS ICON =-=-=-=-=-=-=
    		ImageView sts = new ImageView(this);
    		sts.setDrawingCacheEnabled(false);
    		if(profile.connecting){
    			switch(profile.connection_status){
    			case BimoidProfile.CONN_STUDY_1:
        			sts.setImageResource(R.drawable.sts_connecting_0);
    				break;
    			case BimoidProfile.CONN_STUDY_2:
        			sts.setImageResource(R.drawable.sts_connecting_1);
    				break;
    			case BimoidProfile.CONN_STUDY_3:
        			sts.setImageResource(R.drawable.sts_connecting_2);
    				break;
    			case BimoidProfile.CONN_STUDY_4:
        			sts.setImageResource(R.drawable.sts_connecting_3);
    				break;
    			case BimoidProfile.CONN_STUDY_5:
        			sts.setImageResource(R.drawable.sts_connecting_4);
    				break;
    			case BimoidProfile.CONN_STUDY_6:
        			sts.setImageResource(R.drawable.sts_connecting_5);
    				break;
    			case BimoidProfile.CONN_STUDY_7:
        			sts.setImageResource(R.drawable.sts_connecting_6);
    				break;
    			}
    		}else{
    			sts.setImageDrawable(resources.getMainStatusIcon(profile.getStatus()));
    		}
    		sts.setBackgroundDrawable(Interface.getSelector());
    		sts.setPadding(10, 10, 10, 10);
    		sts.setOnClickListener(new OnClickListener(){
    			@Override
    			public void onClick(final View v){
    				context_profile = profile;
    				final UAdapter adapter = new UAdapter();
    				adapter.setSelectionAsBold(true);
    				adapter.setTextSize(15);
    				Resources res = resources.ctx.getResources();
    				adapter.put(res.getDrawable(R.drawable.sts_chat), Locale.getString("s_status_ready_for_chat"), 0);
    				adapter.put(res.getDrawable(R.drawable.sts_home), Locale.getString("s_status_home"), 1);
    				adapter.put(res.getDrawable(R.drawable.sts_work), Locale.getString("s_status_work"), 2);
    				adapter.put_separator();
    				adapter.put(res.getDrawable(R.drawable.sts_lunch), Locale.getString("s_status_lunch"), 3);
    				adapter.put(res.getDrawable(R.drawable.sts_away), Locale.getString("s_status_away"), 4);
    				adapter.put(res.getDrawable(R.drawable.sts_na), Locale.getString("s_status_na"), 5);
    				adapter.put(res.getDrawable(R.drawable.sts_oc), Locale.getString("s_status_oc"), 6);
    				adapter.put(res.getDrawable(R.drawable.sts_dnd), Locale.getString("s_status_dnd"), 7);
    				adapter.put_separator();
    				adapter.put(res.getDrawable(R.drawable.sts_online), Locale.getString("s_status_online"), 8);
    				adapter.put(res.getDrawable(R.drawable.sts_invis), Locale.getString("s_status_invisible"), 9);
    				adapter.put(res.getDrawable(R.drawable.sts_invis_all), Locale.getString("s_status_invisible_for_all"), 10);
    				adapter.put_separator();
    				adapter.put(res.getDrawable(R.drawable.sts_offline), Locale.getString("s_status_offline"), 11);
    				switch(profile.getStatus()){
    				case BimoidProtocol.PRES_STATUS_FREE_FOR_CHAT:
    					adapter.toggleSelection(0);
    					break;
    				case BimoidProtocol.PRES_STATUS_AT_HOME:
    					adapter.toggleSelection(1);
    					break;
    				case BimoidProtocol.PRES_STATUS_AT_WORK:
    					adapter.toggleSelection(2);
    					break;
    				case BimoidProtocol.PRES_STATUS_LUNCH:
    					adapter.toggleSelection(4);
    					break;
    				case BimoidProtocol.PRES_STATUS_AWAY:
    					adapter.toggleSelection(5);
    					break;
    				case BimoidProtocol.PRES_STATUS_NOT_AVAILABLE:
    					adapter.toggleSelection(6);
    					break;
    				case BimoidProtocol.PRES_STATUS_OCCUPIED:
    					adapter.toggleSelection(7);
    					break;
    				case BimoidProtocol.PRES_STATUS_DO_NOT_DISTURB:
    					adapter.toggleSelection(8);
    					break;
    				case BimoidProtocol.PRES_STATUS_ONLINE:
    					adapter.toggleSelection(10);
    					break;
    				case BimoidProtocol.PRES_STATUS_INVISIBLE:
    					adapter.toggleSelection(11);
    					break;
    				case BimoidProtocol.PRES_STATUS_INVISIBLE_FOR_ALL:
    					adapter.toggleSelection(12);
    					break;
    				case BimoidProtocol.PRES_STATUS_OFFLINE:
    					adapter.toggleSelection(14);
    					break;
    				}
    				last_quick_action = PopupBuilder.buildList(adapter, v, profile.nickname, 220*resources.dm.density, LayoutParams.WRAP_CONTENT, new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							last_quick_action.dismiss();
							switch((int)arg0.getAdapter().getItemId(arg2)){
							case 0:
								context_profile.setStatus(3, "");
								break;
							case 1:
								context_profile.setStatus(4, "");
								break;
							case 2:
								context_profile.setStatus(5, "");
								break;
							case 3:
								context_profile.setStatus(6, "");
								break;
							case 4:
								context_profile.setStatus(7, "");
								break;
							case 5:
								context_profile.setStatus(8, "");
								break;
							case 6:
								context_profile.setStatus(9, "");
								break;
							case 7:
								context_profile.setStatus(10, "");
								break;
							case 8:
								context_profile.setStatus(0, "");
								break;
							case 9:
								context_profile.setStatus(1, "");
								break;
							case 10:
								context_profile.setStatus(2, "");
								break;
							case 11:
								context_profile.disconnectA();
								return;
							}
							if(!(context_profile.connected && context_profile.connecting)){
								context_profile.connect();
							}
						}
    				});
    				last_quick_action.show();
    			}
    	    });
    		bottom_panel.addView(sts);
    		//=-=-=-=-=-=-= ADDITIONAL STATUS ICON =-=-=-=-=-=-=
    		sts = new ImageView(this);
    		sts.setImageDrawable(XStatus.getIcon(profile.getExtStatus()));
    		sts.setBackgroundDrawable(Interface.getSelector());
    		sts.setPadding(10, 10, 10, 10);
    		sts.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					context_profile = profile;
			    	UAdapter ua = new UAdapter();
			    	ua.setMode(UAdapter.FORCE_HIDE_LABEL);
			    	ua.setPadding(7);
			    	ua.put(XStatus.list);
			    	ua.toggleSelection(context_profile.getExtStatus());
			    	last_quick_action = PopupBuilder.buildGrid(ua, v, profile.nickname, 6, 260*resources.dm.density, LayoutParams.WRAP_CONTENT, new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							last_quick_action.dismiss();
							if(arg2 == 0){
								context_profile.setExtStatus(0, "");
								service.prefs.edit().putInt(context_profile.ID+"_ext_idx", selected_xstatus).commit();
								return;
							}
							selected_xstatus = arg2;
							removeDialog(0x11);
							showDialog(0x11);
						}
			    	});
			    	last_quick_action.show();
				}
    		});
    		bottom_panel.addView(sts);
    	}
    }
    private void updateUI(){
    	if(resources.IT_IS_TABLET){
    		findViewById(R.id.contacts_fragment).setVisibility(View.VISIBLE);
    		if(ANY_CHAT_ACTIVE){
    			findViewById(R.id.chat_fragment).setVisibility(View.VISIBLE);
    			//findViewById(R.id.contactlist_list_chat_separator).setVisibility(View.VISIBLE);
    		}else{
    			findViewById(R.id.chat_fragment).setVisibility(View.GONE);
    			//findViewById(R.id.contactlist_list_chat_separator).setVisibility(View.GONE);
				findViewById(R.id.chat_fragment).setVisibility(View.GONE);
    		}
			localOnResume();
    	}else{
	    	if(CURRENT_IS_CONTACTS){
	    		checkAndRemoveChat();
	    		findViewById(R.id.contacts_fragment).setVisibility(View.VISIBLE);
	    		findViewById(R.id.chat_fragment).setVisibility(View.GONE);
				localOnResume();
	    	}else{
	    		findViewById(R.id.contacts_fragment).setVisibility(View.GONE);
	    		findViewById(R.id.chat_fragment).setVisibility(View.VISIBLE);
	    	}
    	}
    }
	private void checkAndRemoveChat(){
		removeFragment(R.id.chat_fragment);
		ANY_CHAT_ACTIVE = false;
	}
    private void startChatFragment(ExFragment chat){
		checkAndRemoveChat();
		ANY_CHAT_ACTIVE = true;
		if(!resources.IT_IS_TABLET) CURRENT_IS_CONTACTS = false;
		attachFragment(R.id.chat_fragment, chat);
    }
    private void startFragmentChat(Contact contact){
    	ChatActivity chat = ChatActivity.getInstance(contact, new ChatInitCallback(){
			@Override
			public void chatInitialized() {
				updateUI();
			}
    	});
    	startChatFragment(chat);
    }
    private void startFragmentChat(String scheme){
    	ChatActivity chat = ChatActivity.getInstance(scheme, new ChatInitCallback(){
			@Override
			public void chatInitialized() {
				updateUI();
			}
    	});
    	startChatFragment(chat);
    }
    private class roster_click_listener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			if(!CURRENT_IS_CONTACTS) return;
			RosterItem item = contacts_adapter.getItem(arg2);
			switch(item.type){
			case RosterItem.OBIMP_CONTACT:
				startFragmentChat((Contact)item);
				//service.putIntoOpened((Contact)item);
				break;
			case RosterItem.OBIMP_GROUP:
				Group group = (Group)item;
				group.opened = !group.opened;
				service.handleContactListNeedRebuild();
				break;
			case RosterItem.PROFILE_GROUP:
				SuperGroup sgroup = (SuperGroup)item;
				sgroup.profile.expanded_in_contact_list = !sgroup.profile.expanded_in_contact_list;
				service.handleContactListNeedRebuild();
				break;
			case RosterItem.TRANSPORT_ITEM:
				context_transport = (Transport)item;
				if(!context_transport.ready){
					service.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_transport_is_not_ready_error"));
					break;
				}
				removeDialog(0x15);
				showDialog(0x15);
				break;
			case RosterItem.CL_ITEM_TYPE_NOTE:
				context_note = (NoteItem)item;
				try{
					switch(context_note.TYPE){
					case NoteItem.CL_NOTE_TYPE_COMMAND:
				    	File file = new File(context_note.TEXT);
				    	if(!file.exists()){
				    		service.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_file_note_err1"));
				    		break;
				    	}
				    	Uri uri = Uri.fromFile(file);
				    	String mime_type = URLConnection.guessContentTypeFromName(uri.toString());
				    	if(mime_type == null){
				    		mime_type = "*/*";
				    	}
				    	try{
					    	Intent exec = new Intent(Intent.ACTION_VIEW);
					    	exec.setDataAndType(uri, mime_type);
					    	startActivity(exec);
				    	}catch(Exception e){
				    		service.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_file_note_err2"));
				    	}
					break;
					case NoteItem.CL_NOTE_TYPE_EMAIL:
						Intent exec = new Intent(Intent.ACTION_VIEW);
						Uri data = Uri.parse("mailto:?to="+context_note.TEXT);
						exec.setData(data);
						startActivity(exec);
						break;
					case NoteItem.CL_NOTE_TYPE_LINK:
						exec = new Intent("android.intent.action.VIEW");
						String url = context_note.TEXT.trim();
						if(url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")){
							exec.setData(Uri.parse(context_note.TEXT));
						}else{
							exec.setData(Uri.parse("http://"+context_note.TEXT));
						}
						//Log.e("DATA", exec.getData().toString());
						exec.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
						startActivity(exec);
					break;
					case NoteItem.CL_NOTE_TYPE_PHONE:
						exec = new Intent("android.intent.action.VIEW");
						exec.setData(Uri.parse("tel:"+context_note.TEXT));
						startActivity(exec);
					break;
					case NoteItem.CL_NOTE_TYPE_TEXT:
						Toast.makeText(ContactListActivity.this, context_note.name+"\n-----------\n"+context_note.TEXT, Toast.LENGTH_LONG).show();
					break;
					}
				}catch(Exception e){}
				break;
			}
		}
	}
	private class roster_long_click_listener implements OnItemLongClickListener{
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			contacts_adapter.notifyDataSetChanged();
			RosterItem item = contacts_adapter.getItem(arg2);
			switch(item.type){
			case RosterItem.OBIMP_CONTACT:
				context_contact = (Contact)item;
				//Log.e("ContextContact", "Is transport contact: "+(context_contact.getTransportId() != -1)+" (item_id: "+context_contact.getItemId()+")");
				if(!context_contact.getProfile().connected){
					service.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_you_must_be_connected_for_roster_control"));
					return false;
				}
				roster_operation_confirm_helper = RosterOperation.CONTACT_MODIFY;
				removeDialog(4);
				showDialog(4);
				break;
			case RosterItem.OBIMP_GROUP:
				context_group = (Group)item;
				if(!context_group.getProfile().connected){
					service.showDialogInContactList(Locale.getString("s_information"), Locale.getString("s_you_must_be_connected_for_roster_control"));
					return false;
				}
				roster_operation_confirm_helper = RosterOperation.GROUP_MODIFY;
				removeDialog(6);
				showDialog(6);
				break;
			case RosterItem.CL_ITEM_TYPE_NOTE:
				context_note = (NoteItem)item;
				roster_operation_confirm_helper = RosterOperation.NOTE_MODIFY;
				removeDialog(0x20);
				showDialog(0x20);
				break;
			}
			return false;
		}
    }
	@Override
	public boolean handleMessage(Message msg) {
		switch(msg.what){
		case REBUILD_LIST:
			contacts_adapter.build();
			break;
		case REFRESH_LIST:
			contacts_adapter.notifyDataSetChanged();
			break;
		case BUILD_BOTTOM_PANEL:
			buildBottomPanel();
			break;
		case SHOW_ERROR_DIALOG:
			BufferedDialog bd = (BufferedDialog)msg.obj;
			if(VISIBLE){
				if(last_shown_error_dialog != null){
					if(last_shown_error_dialog.isShowing()){
						dialogs.add(bd);
					}else{
						dialog_for_display = bd;
						removeDialog(-2);
						showDialog(-2);
					}
				}else{
					dialog_for_display = bd;
					removeDialog(-2);
					showDialog(-2);
				}
			}else{
				dialogs.add(bd);
			}
			break;
		case SHOW_INFO_DIALOG:
			bd = (BufferedDialog)msg.obj;
			if(VISIBLE){
				if(last_shown_notify_dialog != null){
					if(last_shown_notify_dialog.isShowing()){
						dialogs.add(bd);
					}else{
						dialog_for_display = bd;
						removeDialog(3);
						showDialog(3);
					}
				}else{
					dialog_for_display = bd;
					removeDialog(3);
					showDialog(3);
				}
			}else{
				dialogs.add(bd);
			}
			break;
		case MENU_PRESSED:
			if(!VISIBLE) break;
			KeyEvent e = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU);
			onKeyDown(KeyEvent.KEYCODE_MENU, e);
			break;
		case SHOW_ACCOUNT_INFO_DIALOG:
			if(VISIBLE){
				account_info_for_display = (AccountInfoContainer)msg.obj;
				removeDialog(0xE);
				showDialog(0xE);
			}
			break;
		case REINIT_INTERFACE:
			attachInterface();
			break;
		case SHOW_PROGRESS_DIALOG:
			if(progress_dialog != null) if(progress_dialog.isShowing()) progress_dialog.dismiss();
			progress_dialog = DialogBuilder.createProgress(this, (String)msg.obj, false);
			progress_dialog.show();
			break;
		case HIDE_PROGRESS_DIALOG:
			if(progress_dialog != null) progress_dialog.dismiss();
			break;
		case RETURN_TO_CONTACTS:
			if(!CURRENT_IS_CONTACTS){
				CURRENT_IS_CONTACTS = true;
				updateUI();
			}else{
				checkAndRemoveChat();
				updateUI();
			}
			break;
		}
		return false;
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	}
}
