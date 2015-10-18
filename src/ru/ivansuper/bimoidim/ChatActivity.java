package ru.ivansuper.bimoidim;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.HistoryItem;
import ru.ivansuper.bimoidproto.filetransfer.FileBrowserActivity;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChatActivity extends Activity implements Callback {
    private BimoidService service;
    private ListView messages_list;
    private EditText input;
    private Button send_btn;
    private ImageView sts;
    private ImageView ests;
    private TextView nickname;
    private TextView client;
    private Contact contact;
    private ChatAdapter chat_adapter;
    private Handler hdl = new Handler(this);
    private BufferedDialog dialog_for_display = new BufferedDialog(0, null, null);
    public static boolean isAnyChatOpened;
    public static final int REBUILD_CHAT = 0;
    public static final int REFRESH_CHAT = 1;
    public static final int UPDATE_USER_INFO = 2;
    private HistoryItem context_message;
    private TypingTimeoutThread typing_timeout_thread = new TypingTimeoutThread();
	private boolean typing = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        setVolumeControlStream(0x3);
        initViews();
        if(resources.service == null){
        	finish();
        	return;
        }
       	service = resources.service;
		handleServiceConnected();
        typing_timeout_thread.start();
    }
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	typing_timeout_thread.destroy();
    	//if(svcc != null) unbindService(svcc);
    }
    @Override
    public void onResume(){
    	isAnyChatOpened = true;
    	if(contact != null){
    		if(contact.hasUnreadMessages()){
	    		service.cancelPersonalMessageNotify(utilities.getHash(contact));
	        	contact.clearUnreadMessages();
	        	contact.setHasNoFile();
	    		if(!contact.haveAuthReq()){
			    	if(contact.haveAuth()){
			    		contact.setHasNoAuth();
			    	    service.cancelAuthNotify(contact.hashCode()-0xffff);
			    	}
	    		}
	    		service.handleContactListNeedRebuild();
	    		service.checkUnreaded();
    		}
    	}
    	if(chat_adapter != null){
    		chat_adapter.notifyAdapter();
    	}
    	super.onResume();
    }
    @Override
    public void onPause(){
    	
    	super.onPause();
    	isAnyChatOpened = false;
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
    	//super.onKeyDown(keyCode, event)
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(keyCode){
    		case KeyEvent.KEYCODE_BACK:
    			onBackDown();
    			break;
    		case KeyEvent.KEYCODE_MENU:
        		removeDialog(0);
            	showDialog(0);
    			break;
    		}
    	}
    	return false;
    }
    private void onBackDown(){
    	finish();
    }
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == 0){
    		if(resultCode == RESULT_OK){
    			String[] files = data.getAction().split("////");
    			sendFiles(files);
    		}
    	}else if(requestCode == 1){
    		if(resultCode == RESULT_OK){
    			String smile = data.getAction();
    			if(input == null) return;
    			input.append(smile);
    			input.setSelection(input.length());
    		}
    	}
    }
    private void sendFiles(String[] files){
    	contact.getProfile().createFileSender(contact, files);
    }
    protected Dialog onCreateDialog(final int type){
    	Dialog dialog = null;
    	switch(type){
    	case 0x0://Main menu
        	UAdapter adapter = new UAdapter();
        	adapter.setPadding(10);
        	if(contact.itIsTransport()){
        		if(contact.profile.getTransportByID(contact.getTransportId()).params.auth_supported){
	            	if(contact.auth_flag)
	            		adapter.put(resources.context_menu_icon, Locale.getString("s_ask_suth"), 0);
	            	if(contact.haveAuthReq()){
		            		adapter.put(resources.context_menu_icon, Locale.getString("s_accept_auth"), 1);
		            		adapter.put(resources.context_menu_icon, Locale.getString("s_decline_auth"), 2);
	            	}
        		}
            	if(contact.getStatus() != -1){
            		adapter.put(resources.context_menu_icon, Locale.getString("s_show_status_message"), 7);
            		//adapter.put(resources.context_menu_icon, "Передать файл", 6);
            	}
        	}else{
            	if(contact.auth_flag)
            		adapter.put(resources.context_menu_icon, Locale.getString("s_ask_suth"), 0);
            	if(contact.haveAuthReq()){
            		adapter.put(resources.context_menu_icon, Locale.getString("s_accept_auth"), 1);
            		adapter.put(resources.context_menu_icon, Locale.getString("s_decline_auth"), 2);
            	}
            	if(contact.getStatus() != -1){
            		adapter.put(resources.context_menu_icon, Locale.getString("s_show_status_message"), 7);
            		adapter.put(resources.context_menu_icon, Locale.getString("s_send_file"), 6);
            	}
        	}
    		adapter.put(resources.context_menu_icon, Locale.getString("s_messages_history"), 8);
        	dialog = DialogBuilder.create(ChatActivity.this,
        			Locale.getString("s_chat"),
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	case 0x1://Auth reason input
        	LinearLayout lay = (LinearLayout)View.inflate(this, R.layout.auth_reason_input, null);
        	final EditText reason_input = (EditText)lay.findViewById(R.id.auth_reason_input);
        	reason_input.setText(Locale.getString("s_type_in_auth_req_template"));
    		if(ColorScheme.initialized) reason_input.setTextColor(ColorScheme.getColor(13));
    		if(ColorScheme.initialized) utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_type_in_auth_req_text").setTextColor(ColorScheme.getColor(12));
        	Interface.attachEditTextStyle(reason_input);
        	dialog = DialogBuilder.createYesNo(ChatActivity.this, lay,
        			Gravity.CENTER,
        			Locale.getString("s_authorization"),
        			Locale.getString("s_do_send"),
        			Locale.getString("s_cancel"),
    				new OnClickListener(){
						@Override
						public void onClick(View arg0) {
				        	String reason = reason_input.getText().toString();
							if(reason.trim().length() > 0){
								contact.getProfile().sendAuthReq(contact.getID(), reason, contact.getTransportId());
								removeDialog(1);
							}else{
								showMessage(Locale.getString("s_error_message_header"), Locale.getString("s_auth_request_error"));
							}
						}
        			},
        			new OnClickListener(){
						@Override
						public void onClick(View v) {
							removeDialog(1);
						}
        			});
    		break;
    	case 0x2://Notify dialog
			service.media.playEvent(Media.SVC_MSG);
    		if(dialog_for_display == null) return null;
        	dialog = DialogBuilder.createOk(ChatActivity.this,
        			dialog_for_display.header, dialog_for_display.text, Locale.getString("s_close"),
        			Gravity.TOP, new OnClickListener(){
    					@Override
    					public void onClick(View v) {
    						removeDialog(type);
    					}
    		});
    		break;
    	case 0x3://Message context
        	adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_quote"), 3);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_copy"), 4);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_copy_only_text"), 5);
        	dialog = DialogBuilder.create(ChatActivity.this,
        			Locale.getString("s_chat"),
        			adapter,
    				Gravity.CENTER,
    				new context_menu_listener());
    		break;
    	}
    	/*Window wnd = dialog.getWindow();
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	int flags = lp.flags;
    	flags += WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
    	lp.flags = flags;
    	wnd.setAttributes(lp);*/
    	return dialog;
    }
    private class context_menu_listener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			removeDialog(0);
			UAdapter adapter = (UAdapter)arg0.getAdapter();
			adapter.notifyDataSetChanged();
			int id = (int)(adapter).getItemId(arg2);
			switch(id){
			case 0:
				removeDialog(1);
				showDialog(1);
				break;
			case 1:
				service.cancelAuthNotify(contact.hashCode()-0xffff);
				contact.clearUnreadMessages();
				contact.setHasNoAuth();
				service.handleContactListNeedRebuild();
				contact.getProfile().sendAuthReply(contact.getID(), 0x1, contact.getTransportId());//Auth accepted
				break;
			case 2:
				service.cancelAuthNotify(contact.hashCode()-0xffff);
				contact.clearUnreadMessages();
				contact.setHasNoAuth();
				service.handleContactListNeedRebuild();
				contact.getProfile().sendAuthReply(contact.getID(), 0x2, contact.getTransportId());//Auth rejected
				break;
			case 3://Quote
				if(context_message == null) return;
				String buffer = "";
				if(context_message.direction == HistoryItem.DIRECTION_INCOMING){
					buffer += context_message.contact.getName();
				}else{
					buffer += context_message.contact.getProfile().nickname;
				}
				buffer += " "+context_message.formattedDate+"\n";
				buffer += context_message.message+"\n";
				input.setText(buffer);
				input.setSelection(buffer.length(), buffer.length());
				removeDialog(3);
				break;
			case 4://Full copy
				if(context_message == null) return;
				ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
				buffer = "";
				if(context_message.direction == HistoryItem.DIRECTION_INCOMING){
					buffer += context_message.contact.getName();
				}else{
					buffer += context_message.contact.getProfile().nickname;
				}
				buffer += " "+context_message.formattedDate+"\n";
				buffer += context_message.message+"\n";
				cm.setText(buffer);
				removeDialog(3);
				break;
			case 5://Copy only text
				if(context_message == null) return;
				cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
				buffer = context_message.message+"\n";
				cm.setText(buffer);
				removeDialog(3);
				break;
			case 6:
				Intent i = new Intent(ChatActivity.this, FileBrowserActivity.class);
				startActivityForResult(i, 0);
				break;
			case 7:
				if(contact.haveExtendedStatusDescription()){
					HistoryItem hst = new HistoryItem();
					hst.contact = contact;
					hst.direction = HistoryItem.DIRECTION_INCOMING;
					hst.unique_id = (int)(System.currentTimeMillis()/1000);
					hst.message = contact.getExtendedDescription();
					hst.resetConfirmation();
					chat_adapter.put(hst);
					//chat_adapter.notifyAdapter();
				}else{
					dialog_for_display.header = Locale.getString("s_information");
					dialog_for_display.text = Locale.getString("s_contact_didnt_have_status_message");
					removeDialog(2);
					showDialog(2);
				}
				break;
			case 8:
				Intent history = new Intent(ChatActivity.this, HistoryActivity.class);
				history.setAction(contact.getID()+";;;%;;;"+contact.getProfile().ID+";;;%;;;"+contact.getName());
				startActivity(history);
				break;
			}
		}
    }
    private void showMessage(String header, String text){
		dialog_for_display = new BufferedDialog(0, header, text);
		removeDialog(2);
		showDialog(2);
    }
    private void initViews(){
    	messages_list = (ListView)findViewById(R.id.messagesList);
    	Interface.attachSelector(messages_list);
    	messages_list.setOnItemClickListener(new messages_click_listener());
    	messages_list.setOnItemLongClickListener(new messages_long_click_listener());
    	input = (EditText)findViewById(R.id.chat_input);
		if(ColorScheme.initialized) input.setTextColor(ColorScheme.getColor(13));
		Interface.attachEditTextStyle(input);
    	input.addTextChangedListener(new el());
    	send_btn = (Button)findViewById(R.id.chat_send_button);
    	Interface.attachButtonStyle(send_btn);
    	send_btn.setPadding(24, 16, 24, 16);
    	send_btn.setOnClickListener(new send_btn_listener());
    	final Button smiley_select_btn = (Button)findViewById(R.id.chat_smileys_select);
    	Interface.attachButtonStyle(smiley_select_btn);
    	smiley_select_btn.setPadding(24, 16, 24, 16);
    	smiley_select_btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(ChatActivity.this, SmileysSelector.class), 0x1);
			}
    	});
    	sts = (ImageView)findViewById(R.id.chat_user_sts);
    	ests = (ImageView)findViewById(R.id.chat_user_ests);
    	//ests.setVisibility(View.GONE);
    	nickname = (TextView)findViewById(R.id.chat_user_nickname);
		if(ColorScheme.initialized) nickname.setTextColor(ColorScheme.getColor(14));
    	client = (TextView)findViewById(R.id.chat_user_client);
		if(ColorScheme.initialized) client.setTextColor(ColorScheme.getColor(15));
		LinearLayout chat_top_panel = (LinearLayout)findViewById(R.id.chat_top_panel);
		LinearLayout chat_bottom_panel = (LinearLayout)findViewById(R.id.chat_bottom_panel);
	    if(ColorScheme.initialized && !ColorScheme.internal) chat_top_panel.setBackgroundColor(ColorScheme.getColor(26));
	    if(ColorScheme.initialized && !ColorScheme.internal) chat_bottom_panel.setBackgroundColor(ColorScheme.getColor(25));
	    if(ColorScheme.initialized && !ColorScheme.internal) messages_list.setBackgroundColor(ColorScheme.getColor(27));
	    Interface.attachBackground(chat_top_panel, Interface.chat_top_panel);
	    Interface.attachBackground(chat_bottom_panel, Interface.chat_bottom_panel);
	    Interface.attachBackground(messages_list, Interface.chat_messages_back);
    }
	private class el implements TextWatcher{
		private String prev_state = "";
		@Override
		public void afterTextChanged(Editable arg0) {
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if(!s.toString().equals(prev_state)){
				prev_state = s.toString();
				if(!typing){
					//if(PreferenceTable.send_typing_notify)
					contact.getProfile().sendTypingNotify(0x1, contact.getID(), contact.getTransportId());
					typing = true;
				}
				typing_timeout_thread.resetCounter();
			}
		}
	}
    private void handleServiceConnected(){
    	contact = service.currentChatContact;
    	contact.getHistoryObject().preloadCache();
    	chat_adapter = new ChatAdapter(this, contact.getHistoryObject().getMessageList());
    	messages_list.setAdapter(chat_adapter);
    	service.chatHdl = hdl;
    	drawChat();
    }
    private void drawChat(){
		contact.getHistoryObject().preloadCache();
    	if(contact.hasUnreadMessages()){
    		service.cancelPersonalMessageNotify(utilities.getHash(contact));
        	contact.setHasNoFile();
    		if(!contact.haveAuthReq()){
    			contact.clearUnreadMessages();
				service.handleContactListNeedRebuild();
		    	if(contact.haveAuth()){
		    		contact.setHasNoAuth();
		    	    service.cancelAuthNotify(contact.hashCode()-0xffff);
		    	}
    		}
    	}
		service.checkUnreaded();
    	//playMessagesAnimation();
    	drawUserInfo();
    }
    private void drawUserInfo(){
    	if(contact.getTyping()){
    		sts.setImageDrawable(resources.res.getDrawable(R.drawable.contact_typing));
    	}else{
    		if(contact.getTransportId() != -1){
				sts.setImageDrawable(contact.getProfile().getTransportByID(contact.getTransportId()).params.getStatus(contact.getStatus()));
    		}else{
    			sts.setImageDrawable(resources.getMainStatusIcon(contact.getStatus()));
    		}
    	}
    	if(contact.getExtendedStatus() > 0){
    		if(contact.getTransportId() != -1){
				ests.setImageDrawable(contact.getProfile().getTransportByID(contact.getTransportId()).params.getAddStatus(contact.getExtendedStatus()));
    		}else{
    			ests.setImageDrawable(XStatus.getIcon(contact.getExtendedStatus()));
    		}
    	}else{
    		ests.setImageDrawable(null);
    	}
    	nickname.setText(contact.getName());
    	String client_ = "";
    	final String cli = contact.getClient();
    	final String cli_ver = contact.getClientVersionString();
    	if(cli != null){
    		client_ += cli;
    		if(cli_ver != null){
    			client_ += " "+cli_ver;
    		}
    		client.setText(client_);
    	}else{
    		client.setText("");
    	}
    }
    private class send_btn_listener implements OnClickListener{
		@Override
		public void onClick(View v) {
			String text = input.getText().toString();
			if(text.length() == 0) return;
			if(!contact.getProfile().connected) return;
			HistoryItem hst = new HistoryItem();
			hst.contact = contact;
			hst.direction = HistoryItem.DIRECTION_OUTGOING;
			hst.unique_id = (int)(System.currentTimeMillis()/1000);
			hst.message = text;
			hst.resetConfirmation();
			//chat_adapter.put(hst);
			contact.getProfile().sendMessage(contact, hst);
			chat_adapter.notifyAdapter();
			input.setText("");
		}
    }
	@Override
	public boolean handleMessage(Message msg) {
		switch(msg.what){
		case REBUILD_CHAT:
			if(chat_adapter != null) chat_adapter.notifyAdapter();
			break;
		case REFRESH_CHAT:
			if(chat_adapter != null) chat_adapter.notifyAdapter();
			break;
		case UPDATE_USER_INFO:
			if(contact != null) drawUserInfo();
			break;
		}
		return false;
	}
	private class messages_click_listener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			ChatAdapter adapter = (ChatAdapter)arg0.getAdapter();
			adapter.notifyDataSetChanged();
		}
	}
	private class messages_long_click_listener implements OnItemLongClickListener{
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			ChatAdapter adapter = (ChatAdapter)arg0.getAdapter();
			context_message = adapter.getItem(arg2);
			removeDialog(3);
			showDialog(3);
			return false;
		}
	}
	private class TypingTimeoutThread extends Thread {
		private int counter = 0;
		private boolean enabled = true;
		public void resetCounter(){
			counter = 3;
		}
		public void destroy(){
			enabled = false;
			sendTypingEnd();
		}
		@Override
		public void run(){
			while(enabled){
				try{
					sleep(1000);
					if(counter <= 0){
						sendTypingEnd();
					}else{
						counter--;
					}
				}catch(Exception e){}
			}
		}
		private void sendTypingEnd(){
			if(typing){
				contact.getProfile().sendTypingNotify(0x2, contact.getID(), contact.getTransportId());
				typing = false;
			}
		}
	}
}
