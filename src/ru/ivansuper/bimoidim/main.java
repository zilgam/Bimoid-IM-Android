package ru.ivansuper.bimoidim;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class main extends Activity implements Callback {
    private ServiceConnection svcc;
    private Handler hdl = new Handler(this);
    private ImageView arrows;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ((TextView)findViewById(R.id.l1)).setText(Locale.getString("s_loading")+" ...");
        arrows = (ImageView)findViewById(R.id.launch_progress);
        RotateAnimation ra = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(250);
        ra.setInterpolator(this, android.R.anim.linear_interpolator);
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.RESTART);
        arrows.setAnimation(ra);
    	if(resources.service == null){
	      	svcc = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName arg0, IBinder arg1) {
					BimoidService service = ((BimoidService.binder)arg1).getService();
					if(resources.service == null) resources.service = service;
					splashThread t = new splashThread();
					t.start();
				}
				@Override
				public void onServiceDisconnected(ComponentName arg0) {
				}
	       	};
	       	Intent svc = new Intent();
	       	svc.setClass(this, BimoidService.class);
	        startService(svc);
	        bindService(svc, svcc, 0);
    	}else{
    		intentNext();
    	}
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
    			return true;
    		}
    	}
    	return false;
    }
    private class splashThread extends Thread{
    	@Override
    	public void run(){
        	if(resources.service.firstStart){
        		resources.service.firstStart = false;
        		try {
    				Thread.sleep(1200);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
        	}
    		hdl.sendEmptyMessage(0x0);
    	}
    }
    private void intentNext(){
    	if(resources.service == null) finish();
    	if(resources.service.profiles.list.size() == 0){
        	intentContactList(true);
    	}else{
        	intentContactList(false);
    	}
    	finish();
    }
    private void intentContactList(boolean profiles_request){
		Intent i = new Intent();
		i.setClass(main.this, ContactListActivity.class);
		if(profiles_request){
			i.putExtra("no_profiles", true);
		}
		Intent ii = getIntent();
		final String action = ii.getAction();
		if(action != null)
			i.setAction(action);
		startActivity(i);
		
		/*if(ii.getAction().startsWith("%MSG%")){
			String raw = ii.getAction().substring(5, ii.getAction().length());
			String[] params = raw.split(";;;");
			if(params.length != 3) return;
			BimoidProfile profile = resources.service.profiles.getProfileByID(params[1]);
			if(profile == null) return;
			Contact contact = profile.getContactById(params[0], Integer.parseInt(params[2]));
			if(contact == null) return;
			resources.service.currentChatContact = contact;
			Intent chat = new Intent(this, ChatActivity.class);
			startActivity(chat);
		}
		if(ii.getAction().startsWith("OPEN_CHAT")){
			resources.service.currentChatContact = resources.service.contactForOpenFromNotify;
			Intent chat = new Intent(this, ChatActivity.class);
			startActivity(chat);
		}*/
    }
	@Override
	public boolean handleMessage(Message msg) {
		switch(msg.what){
		case 0x0:
			intentNext();
			break;
		case 0x1:
	    	Window wnd = getWindow();
	    	wnd.setFlags(4,4);
			break;
		}
		return false;
	}
}