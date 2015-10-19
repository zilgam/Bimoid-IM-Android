package ru.ivansuper.bimoidim;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.Registrator;
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
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class RegistrationActivity extends Activity implements Callback{
    private BimoidService service;
    private EditText id;
    private EditText pass;
    private EditText pass_dbl;
    private EditText email;
    private Button do_reg;
    private Button cancel_reg;
    private Button back;
    private LinearLayout data_field;
    private LinearLayout success_field;
    private String dialog_header;
    private String dialog_message;
    private Handler handler = new Handler(this);
    public static final int REFRESH_STATE = 0x0;
    public static final int REGISTRATION_SUCCESS = 0x1;
    public static final int REGISTRATION_FAILED = 0x2;
    private Registrator registrator;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.registration);
		initViews();
        setVolumeControlStream(0x3);
       	service = resources.service;
		handleServiceConnected();
	}
	@Override
	public void onResume(){
		super.onResume();
	}
    @Override
    public void onDestroy(){
    	super.onDestroy();
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
    	case 0x0://Message dialog
    		dialog = DialogBuilder.createOk(this, dialog_header,
    				dialog_message,
    				Locale.getString("s_close"),
    				Gravity.TOP, new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							removeDialog(0);
						}
    				});
    		break;
    	case 0x1:
    		dialog = DialogBuilder.createProgress(this, Locale.getString("s_profile_registration_in_progress"), true);
    		break;
    	}
    	return dialog;
    }
    private void showNotify(String header, String message){
    	dialog_header = header;
    	dialog_message = message;
    	removeDialog(0);
    	showDialog(0);
    }
	private void initViews(){
	    if(ColorScheme.initialized){
	    	((LinearLayout)findViewById(R.id.registration_back)).setBackgroundColor(ColorScheme.getColor(31));
			((LinearLayout)findViewById(R.id.divider)).setBackgroundColor(ColorScheme.getColor(4));
			utilities.setLabel(((TextView)findViewById(R.id.l1)), "s_reg_header").setTextColor(ColorScheme.getColor(3));
			utilities.setLabel(((TextView)findViewById(R.id.l2)), "s_reg_account").setTextColor(ColorScheme.getColor(12));
			utilities.setLabel(((TextView)findViewById(R.id.l3)), "s_reg_pass_1").setTextColor(ColorScheme.getColor(12));
			utilities.setLabel(((TextView)findViewById(R.id.l4)), "s_reg_pass_2").setTextColor(ColorScheme.getColor(12));
			utilities.setLabel(((TextView)findViewById(R.id.l5)), "s_reg_email").setTextColor(ColorScheme.getColor(12));
			utilities.setLabel(((TextView)findViewById(R.id.l6)), "s_reg_success_label_1").setTextColor(ColorScheme.getColor(12));
			utilities.setLabel(((TextView)findViewById(R.id.l7)), "s_reg_success_label_2").setTextColor(ColorScheme.getColor(12));
	    }
		Interface.attachBackground((LinearLayout)findViewById(R.id.header), Interface.registration_top_panel);
		Interface.attachBackground((ScrollView)findViewById(R.id.reg_field), Interface.registration_list_back);
	    id = (EditText)findViewById(R.id.reg_id);
		if(ColorScheme.initialized) id.setTextColor(ColorScheme.getColor(13));
    	Interface.attachEditTextStyle(id);
	    pass = (EditText)findViewById(R.id.reg_pass);
		if(ColorScheme.initialized) pass.setTextColor(ColorScheme.getColor(13));
    	Interface.attachEditTextStyle(pass);
	    pass_dbl = (EditText)findViewById(R.id.reg_pass_dbl);
		if(ColorScheme.initialized) pass_dbl.setTextColor(ColorScheme.getColor(13));
    	Interface.attachEditTextStyle(pass_dbl);
	    email = (EditText)findViewById(R.id.reg_email);
		if(ColorScheme.initialized) email.setTextColor(ColorScheme.getColor(13));
    	Interface.attachEditTextStyle(email);
	    do_reg = (Button)findViewById(R.id.reg_do_register);
	    if(ColorScheme.initialized) do_reg.setTextColor(ColorScheme.getColor(24));
	    do_reg.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String id_ = id.getText().toString();
				String pass_ = pass.getText().toString();
				String pass_dbl_ = pass_dbl.getText().toString();
				String email_ = email.getText().toString();
				if(id_.length() < 3){
					service.media.playEvent(Media.INFO_MSG);
					showNotify(Locale.getString("s_information"), Locale.getString("s_profile_registration_error_1"));
					return;
				}
				if(service.profiles.isExist(id_)){
					service.media.playEvent(Media.INFO_MSG);
					service.media.playEvent(Media.SVC_MSG);
					showNotify(Locale.getString("s_information"), Locale.getString("s_profile_registration_error_2"));
					return;
				}
				if(!pass_.equals(pass_dbl_)){
					service.media.playEvent(Media.INFO_MSG);
					showNotify(Locale.getString("s_information"), Locale.getString("s_profile_registration_error_3"));
					return;
				}
				if(pass_.length() < 3){
					service.media.playEvent(Media.INFO_MSG);
					showNotify(Locale.getString("s_information"), Locale.getString("s_profile_registration_error_4"));
					return;
				}
				if(email_.length() < 10){
					service.media.playEvent(Media.INFO_MSG);
					showNotify(Locale.getString("s_information"), Locale.getString("s_profile_registration_error_5"));
					return;
				}
				registrator = new Registrator(service);
				registrator.doRegister(id_, pass_, email_);
				showDialog(1);
			}
	    });
	    do_reg.setText(Locale.getString("s_reg_do_register"));
	    cancel_reg = (Button)findViewById(R.id.reg_cancel);
	    if(ColorScheme.initialized) cancel_reg.setTextColor(ColorScheme.getColor(24));
	    cancel_reg.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
	    });
	    cancel_reg.setText(Locale.getString("s_reg_cancel_register"));
	    back = (Button)findViewById(R.id.reg_back);
	    if(ColorScheme.initialized) back.setTextColor(ColorScheme.getColor(24));
	    back.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
	    });
	    back.setText(Locale.getString("s_reg_back"));
	    data_field = (LinearLayout)findViewById(R.id.reg_datafield);
	    success_field = (LinearLayout)findViewById(R.id.reg_success_field);
        otherInit();
	}
	private void otherInit(){
		
	}
	private void handleServiceConnected(){
		service.regHdl = handler;
	}
	@Override
	public boolean handleMessage(Message arg0) {
		switch(arg0.what){
		case REFRESH_STATE:
			if(registrator.STATUS == Registrator.SUCCESS){
				handleRegSuccess();
			}else if(registrator.STATUS == Registrator.ERROR){
				handleRegError();
			}
			break;
		case REGISTRATION_SUCCESS:
			handleRegSuccess();
			break;
		case REGISTRATION_FAILED:
			handleRegError();
			break;
		}
		return false;
	}
	private void handleRegSuccess(){
		removeDialog(1);
		BimoidProfile profile = new BimoidProfile(service,
				registrator.ID+"@bimoid.net",
				registrator.pass, false, false);
		service.profiles.list.add(profile);
		try {
			service.profiles.saveProfiles();
		} catch (Exception e) {
			Log.e("Registration", "Can't save profiles");
			e.printStackTrace();
		}
		service.handleContactListNeedRebuild();
		service.handleProfileStatusChanged();
		data_field.setVisibility(View.GONE);
		success_field.setVisibility(View.VISIBLE);
	}
	private void handleRegError(){
		removeDialog(1);
		service.media.playEvent(Media.SVC_MSG);
		showNotify(Locale.getString("s_error_message_header"), registrator.RESULT_MSG);
	}
}
