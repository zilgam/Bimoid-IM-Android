package ru.ivansuper.bservice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import ru.ivansuper.bimoidim.BReceiver;
import ru.ivansuper.bimoidim.BufferedDialog;
import ru.ivansuper.bimoidim.ChatActivity;
import ru.ivansuper.bimoidim.ContactListActivity;
import ru.ivansuper.bimoidim.DetailSearchActivity;
import ru.ivansuper.bimoidim.IgnoreActivity;
import ru.ivansuper.bimoidim.Media;
import ru.ivansuper.bimoidim.PreferenceTable;
import ru.ivansuper.bimoidim.ProfilesManager;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.main;
import ru.ivansuper.bimoidim.proto_utils;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidim.utilities;
import ru.ivansuper.bimoidproto.AccountInfoContainer;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.MessagesDump;
import ru.ivansuper.locale.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class BimoidService extends Service implements OnSharedPreferenceChangeListener, Callback {
	private static final Class<?>[] mSetForegroundSignature = new Class[] {
	    boolean.class};
	private static final Class<?>[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {
	    boolean.class};
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private WakeLock wake_lock;
	private WifiLock wifi_lock;
	private BReceiver receiver;
	public Handler svcHdl;
	private Vibrator vbr;
	private NotificationManager nm;
	public boolean firstStart = true;
	public ProfilesManager profiles;
	public Handler clHdl;
	public Handler chatHdl;
	public Handler regHdl;
	public Handler searchHdl;
	public Handler ignoreHdl;
	public SharedPreferences prefs;
	public Media media = new Media();
	public boolean reconnect_mode;
	private WakeLock temp_wake_lock;
	private Vector<Contact> opened_contacts = new Vector<Contact>();
	public synchronized boolean itInOpened(Contact contact){
		return opened_contacts.contains(contact);
	}
	public synchronized void putIntoOpened(Contact contact){
		if(!opened_contacts.contains(contact)){
			opened_contacts.addElement(contact);
		}
	}
	public synchronized void removeFromOpened(Contact contact){
		if(opened_contacts.contains(contact)){
			opened_contacts.removeElement(contact);
		}
	}
	public Contact getOpened(int idx){
		Log.i("Service:opened_contacts", "Requested at: "+idx);
		return opened_contacts.get(idx);
	}
	public Contact getOpened(String ID){
		for(int i=0; i<opened_contacts.size(); i++){
			Contact contact = opened_contacts.elementAt(i);
			if(contact.getID().equals(ID)){
				return contact;
			}
		}
		return null;
	}
	public int getOpenedCount(){
		return opened_contacts.size();
	}
	public synchronized void clearOpened(){
		opened_contacts.clear();
	}
	@Override
	public IBinder onBind(Intent intent) {
		return new binder();
	}
	public class binder extends Binder{
		public BimoidService getService(){
			return BimoidService.this;
		}
	}
	//--------FOREGROUND COMPAT--------
	void invokeMethod(Method method, Object[] args) {
	    try {
	        mStartForeground.invoke(this, mStartForegroundArgs);
	    } catch (InvocationTargetException e) {
	        Log.w("ApiDemos", "Unable to invoke method", e);
	    } catch (IllegalAccessException e) {
	        Log.w("ApiDemos", "Unable to invoke method", e);
	    }
	}
	void startForegroundCompat(int id, Notification notification) {
	    if (mStartForeground != null) {
	        mStartForegroundArgs[0] = Integer.valueOf(id);
	        mStartForegroundArgs[1] = notification;
	        invokeMethod(mStartForeground, mStartForegroundArgs);
	        return;
	    }
	    mSetForegroundArgs[0] = Boolean.TRUE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
	    nm.notify(id, notification);
	}
	void stopForegroundCompat(int id) {
	    if (mStopForeground != null) {
	        mStopForegroundArgs[0] = Boolean.TRUE;
	        try {
	            mStopForeground.invoke(this, mStopForegroundArgs);
	        } catch (InvocationTargetException e) {
	            Log.w("ApiDemos", "Unable to invoke stopForeground", e);
	        } catch (IllegalAccessException e) {
	            Log.w("ApiDemos", "Unable to invoke stopForeground", e);
	        }
	        return;
	    }
	    nm.cancel(id);
	    mSetForegroundArgs[0] = Boolean.FALSE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
	}
	private void startForegroundCompat(){
        try{
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
            } catch (NoSuchMethodException e) {
            	// Running on an older platform.
            	mStartForeground = mStopForeground = null;
            	return;
            }
            try {
            	mSetForeground = getClass().getMethod("setForeground",
            			mSetForegroundSignature);
            } catch (NoSuchMethodException e) {
            	throw new IllegalStateException(
            	"OS doesn't have Service.startForeground OR Service.setForeground!");
            }
            startForegroundCompat(0xff33, getNotification());
	}
    private Notification getNotification() {
        CharSequence text = "BimoidIM";
        Notification notification = new Notification();
        notification.when = 0;
        if(profiles != null){
	        if(profiles.getConnectedProfilesCount() > 0){
	            notification.icon = R.drawable.icon;
	        }else{
	            notification.icon = R.drawable.icon_not_connected;
	        }
        }else{
            notification.icon = R.drawable.icon_not_connected;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, main.class), 0);
        if(profiles != null){
	        if(profiles.list.size() > 1){
	            notification.setLatestEventInfo(resources.ctx, text, utilities.match(Locale.getString("s_main_notify"), new String[]{String.valueOf(profiles.getConnectedProfilesCount()), String.valueOf(profiles.list.size())}), contentIntent);
	        }else{
	            notification.setLatestEventInfo(resources.ctx, text, "", contentIntent);
	        }
        }else{
            notification.setLatestEventInfo(resources.ctx, text, "", contentIntent);
        }
		return notification;
    }
	//-------End Of Foreground compat-------------
    @Override
    public void onCreate() {
		if(resources.ctx == null) android.os.Process.killProcess(android.os.Process.myPid());
    	svcHdl = new Handler(this);
		if(profiles == null) profiles = new ProfilesManager(this);
    	/*SensorManager sm = (SensorManager)getSystemService(Service.SENSOR_SERVICE);
    	Sensor s = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    	if((sm != null) && (s != null)){
    	sm.registerListener(new SensorEventListener(){
    		private int check = -1;
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				
			}
			@Override
			public void onSensorChanged(SensorEvent event) {
				if((check != 0) && (event.values[0] == 0)){
					sendMenuPressed();
				}
				check = (int)event.values[0];
					//Log.i("PROXIMITY",
							//String.valueOf(event.values[0]));
			}
    	}, s, SensorManager.SENSOR_DELAY_UI);
    	}*/
    	prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	prefs.registerOnSharedPreferenceChangeListener(this);
    	//mp = new Media(this);
    	//Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    	startForegroundCompat();
    	vbr = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        initSettings();
        IntentFilter i = new IntentFilter();
        i.addAction("android.intent.action.PHONE_STATE");
        i.addAction("android.media.RINGER_MODE_CHANGED");
        i.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        i.addAction("android.intent.action.SCREEN_OFF");
        i.addAction("android.intent.action.SCREEN_ON");
        receiver = new BReceiver(this);
        registerReceiver(receiver, i);
        if(prefs.getBoolean("ms_wake_lock", false)){
        	PowerManager pMan = (PowerManager)getSystemService(POWER_SERVICE);
        	wake_lock = pMan.newWakeLock(0x1, "ru.ivansuper.jasmin_wake");
        	wake_lock.acquire();
    		Log.v("POWER", "WAKE_LOCK ENABLED");
        }
        if(prefs.getBoolean("ms_wifi_lock", true)){
        	WifiManager pMan = (WifiManager)getSystemService(WIFI_SERVICE);
        	wifi_lock = pMan.createWifiLock(1, BimoidService.class.getName());
        	wifi_lock.acquire();
    		Log.v("POWER", "WIFI_LOCK ENABLED");
        }
        //createLogFloatingWindow();
    }
    @Override
    public void onDestroy() {
	    prefs.unregisterOnSharedPreferenceChangeListener(this);
        //stopForegroundCompat(0xffff);
    	super.onDestroy();
    }
    public void performDestroying(){
    	if(wake_lock != null)
    		wake_lock.release();
    	if(wifi_lock != null)
    		wifi_lock.release();
    }
    private void initSettings(){
    	PreferenceTable.show_groups = prefs.getBoolean("ms_groups", true);
    	PreferenceTable.show_offline = prefs.getBoolean("ms_offline", true);
    	Media.silent_mode = !prefs.getBoolean("ms_sounds", true);
    	PreferenceTable.personal_notify = prefs.getBoolean("ms_personal_notify", false);
    	PreferenceTable.addit_desc_under_nick = prefs.getBoolean("ms_show_addit_desc", false);
    	PreferenceTable.vibro = prefs.getBoolean("ms_vibro", true);
    	PreferenceTable.store_history = prefs.getBoolean("ms_store_history", true);
    	PreferenceTable.hide_empty_groups = prefs.getBoolean("ms_hide_empty_groups", false);
    }
    @Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		initSettings();
		handleContactListNeedRebuild();
	}
	public void handleScreenTurnedOff() {
		
	}
	public void handleScreenTurnedOn() {
		
	}
	public void updateStatusBarIcon(){
	    nm.notify(0xff33, getNotification());
	}
	public void checkUnreaded(){
		if(PreferenceTable.personal_notify) return;
		MessagesDump dump = new MessagesDump();
		profiles.getUnreadedDump(dump);
		if(dump.messages > 0){
			nm.notify(0xffff, getUnreadedNotification(dump));
		}else{
			nm.cancel(0xffff);
		}
	}
	public synchronized void putMessageNotify(final Contact contact, final String nick, final String text){
		runOnUi(new Runnable(){
			@Override
			public void run() {
				MNotification mn = new MNotification();
			    Intent intent = new Intent(BimoidService.this, main.class);
			    final String schema = proto_utils.getSchema(contact);
				intent.setAction(schema);
		    	mn.intent = intent;
		    	mn.nick = nick;
		    	mn.text = text;
		    	mn.schema = schema;
		    	NotifyManager.put(mn);
		    	checkUnreaded();
			}
		});
	}
	public synchronized void removeMessageNotify(Contact contact){
		NotifyManager.remove(proto_utils.getSchema(contact));
    	checkUnreaded();
	}
    private Notification getUnreadedNotification(MessagesDump dump) {
        CharSequence text = Locale.getString("s_has_unreaded_messages");
        Notification notification = new Notification();
        notification.icon = R.drawable.inc_msg_animated;
        Intent i = new Intent(this, main.class);
        i.setAction("OPEN_CHAT");
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        notification.setLatestEventInfo(this, text, utilities.match(Locale.getString("s_x_messages_from_y_contacts"), new String[]{String.valueOf(dump.messages), String.valueOf(dump.contacts)}), contentIntent);
	    int flags = 0;
	    //if(led){
	    	flags |= Notification.FLAG_SHOW_LIGHTS;
	    	notification.ledARGB = 0xff00ff00;
	    	notification.ledOffMS = 1000;
	    	notification.ledOnMS = 1000;
	    //}
	    notification.flags |= flags;
		return notification;
    }
    public void createAuthNotify(int id, int icon, String header, String text){
        Notification notification = new Notification(icon, null, 0);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, main.class), 0);
        notification.setLatestEventInfo(this, header, text, contentIntent);
	    int flags = 0;
	    //if(led){
	    	flags |= Notification.FLAG_SHOW_LIGHTS;
	    	notification.ledARGB = 0xff00ff00;
	    	notification.ledOffMS = 1000;
	    	notification.ledOnMS = 1000;
	    //}
	    notification.flags |= flags;
		nm.notify(id, notification);
    }
    public void cancelAuthNotify(int id){
		nm.cancel(id);
    }
    public void createPersonalMessageNotify(int id, int icon, String header, String text, Contact contact, int custom_index){
    	if(!PreferenceTable.personal_notify) return;
        Notification notification = new Notification(icon, null, 0);
        Intent intent = new Intent(this, main.class);
        intent.setAction("%MSG%"+contact.getID()+";;;"+contact.getProfile().ID+";;;"+String.valueOf(contact.getTransportId()));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification.setLatestEventInfo(this, header, text, contentIntent);
        if(custom_index == -1){
        	notification.number = contact.getUnreadCount();
        }else{
        	notification.number = custom_index;
        }
	    int flags = 0;
	    //if(led){
	    	flags |= Notification.FLAG_SHOW_LIGHTS;
	    	notification.ledARGB = 0xff00ff00;
	    	notification.ledOffMS = 1000;
	    	notification.ledOnMS = 1000;
	    //}
	    notification.flags |= flags;
	    Log.i("NOTIFY_ID", String.valueOf(id));
		nm.notify(id, notification);
    }
    public void createPersonalFileNotify(int id, int icon, String header, String text, Contact contact, int custom_index){
        Notification notification = new Notification(icon, null, 0);
        Intent intent = new Intent(this, main.class);
        intent.setAction("%MSG%"+contact.getID()+";;;"+contact.getProfile().ID+";;;"+String.valueOf(contact.getTransportId()));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification.setLatestEventInfo(this, header, text, contentIntent);
        if(custom_index == -1){
        	notification.number = contact.getUnreadCount();
        }else{
        	notification.number = custom_index;
        }
	    int flags = 0;
	    //if(led){
	    	flags |= Notification.FLAG_SHOW_LIGHTS;
	    	notification.ledARGB = 0xff00ff00;
	    	notification.ledOffMS = 1000;
	    	notification.ledOnMS = 1000;
	    //}
	    notification.flags |= flags;
	    Log.i("NOTIFY_ID", String.valueOf(id));
		nm.notify(id, notification);
    }
    public void cancelPersonalMessageNotify(int id){
	    Log.i("NOTIFY_ID(CANCEL)", String.valueOf(id));
		nm.cancel(id);
    }
	public void showDialogInSearch(String header, String text){
		if(searchHdl != null){
			Message msg = Message.obtain(searchHdl,
					DetailSearchActivity.HANDLE_NOTIFICATION, new BufferedDialog(3, header, text));
			searchHdl.sendMessage(msg);
		}
	}
	public void handleSearchResult(AccountInfoContainer result){
		if(searchHdl != null){
			Message msg = Message.obtain(searchHdl,
					DetailSearchActivity.HANDLE_SEARCH_RESULT, result);
			searchHdl.sendMessage(msg);
		}
	}
	public void handleSearchEnd(){
		if(searchHdl != null)
			searchHdl.sendEmptyMessage(DetailSearchActivity.HANDLE_SEARCH_END);
	}
	public void showProgressInContactList(String text){
		if(clHdl != null){
			Message msg = Message.obtain(clHdl,
					ContactListActivity.SHOW_PROGRESS_DIALOG, text);
			clHdl.sendMessage(msg);
		}
	}
	public void hideProgressInContactList(){
		if(clHdl != null){
			Message msg = Message.obtain(clHdl,
					ContactListActivity.HIDE_PROGRESS_DIALOG);
			clHdl.sendMessage(msg);
		}
	}
	public void showErrorDialogInContactList(String header, String text, String error){
		if(clHdl != null){
			Message msg = Message.obtain(clHdl,
					ContactListActivity.SHOW_ERROR_DIALOG, new BufferedDialog(3, header, text, error));
			clHdl.sendMessage(msg);
		}
	}
	public void showDialogInContactList(String header, String text){
		if(clHdl != null){
			Message msg = Message.obtain(clHdl,
					ContactListActivity.SHOW_INFO_DIALOG, new BufferedDialog(3, header, text));
			clHdl.sendMessage(msg);
		}
	}
	public void showAccountInfoDialogInContactList(AccountInfoContainer info){
		if(clHdl != null){
			Message msg = Message.obtain(clHdl,
					ContactListActivity.SHOW_ACCOUNT_INFO_DIALOG, info);
			clHdl.sendMessage(msg);
		}
	}
	public void refreshRegistrationState(){
		if(regHdl != null)
			regHdl.sendEmptyMessage(ContactListActivity.REBUILD_LIST);
	}
	public void handleContactlistReturnToContacts(){
		if(clHdl != null)
			clHdl.sendEmptyMessage(ContactListActivity.RETURN_TO_CONTACTS);
	}
	public void handleIgnoreListNeedRebuild(){
		if(ignoreHdl != null)
			ignoreHdl.sendEmptyMessage(IgnoreActivity.UPDATE_LIST);
	}
	public void handleContactListNeedRebuild(){
		if(clHdl != null)
			clHdl.sendEmptyMessage(ContactListActivity.REBUILD_LIST);
	}
	public void handleContactListNeedRefresh(){
		if(clHdl != null)
			clHdl.sendEmptyMessage(ContactListActivity.REFRESH_LIST);
	}
	public void handleProfileStatusChanged(){
		updateStatusBarIcon();
		if(clHdl != null)
			clHdl.sendEmptyMessage(ContactListActivity.BUILD_BOTTOM_PANEL);
	}
	public void refreshContactListInterface(){
		if(clHdl != null)
			clHdl.sendEmptyMessage(ContactListActivity.REINIT_INTERFACE);
	}
	public void refreshChatUserInfo(){
		if(chatHdl != null)
			chatHdl.sendEmptyMessage(ChatActivity.UPDATE_USER_INFO);
	}
	public void rebuildChat(){
		if(chatHdl != null)
			chatHdl.sendEmptyMessage(ChatActivity.REBUILD_CHAT);
	}
	public void refreshChat(){
		if(chatHdl != null)
			chatHdl.sendEmptyMessage(ChatActivity.REFRESH_CHAT);
	}
	public void doVibrate(long how_long){
		if(vbr != null)
			if(PreferenceTable.vibro)
				vbr.vibrate(how_long);
	}
	@Override
	public boolean handleMessage(Message arg0) {
		return false;
	}
	public Vector<String> profiles_in_reconnection = new Vector<String>();
	private void updateWake(){
		if(wake_lock != null) return;
		if(profiles_in_reconnection.size() > 0){
			if(temp_wake_lock != null)
				if(temp_wake_lock.isHeld()) return;
	    	PowerManager pMan = (PowerManager)getSystemService(POWER_SERVICE);
	    	temp_wake_lock = pMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP, "ru.ivansuper.bimoid_reconnect_wake");
	    	temp_wake_lock.acquire();
		}else{
			if(temp_wake_lock != null){
				if(temp_wake_lock.isHeld())
					temp_wake_lock.release();
				//temp_wake_lock = null;
			}
		}
	}
	public synchronized void addWakeLock(String tag){
		if(!profiles_in_reconnection.contains(tag)){
			profiles_in_reconnection.addElement(tag);
			updateWake();
		}
	}
	public synchronized void removeWakeLock(String tag){
		if(profiles_in_reconnection.contains(tag)){
			profiles_in_reconnection.removeElement(tag);
			updateWake();
		}
	}
	public void showToast(final String text, final int length){
		this.svcHdl.post(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(BimoidService.this, text, length).show();
			}
		});
	}
	public final void runOnUi(Runnable task){
		svcHdl.post(task);
	}
	public final void runOnUi(Runnable task, long delay){
		svcHdl.postDelayed(task, delay);
	}
}
