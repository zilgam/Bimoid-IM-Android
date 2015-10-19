package ru.ivansuper.bimoidim;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.HistoryItemA;
import ru.ivansuper.locale.Locale;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends Activity {
	private TextView label;
	private ListView list;
	private HistoryAdapter adapter;
	private File history_database;
	private String contact;
	private String profile;
	private Dialog progress;
	private int context_item;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if(resources.IT_IS_TABLET) getWindow().addFlags(0x1000000);//Hardware acceleration
        setContentView(R.layout.history);
        setVolumeControlStream(0x3);
        initViews();
    }
    @Override
    public void onDestroy(){
    	super.onDestroy();
    }
    private void initViews(){
    	EditText search_input = (EditText)findViewById(R.id.history_search_input);
		if(ColorScheme.initialized) search_input.setTextColor(ColorScheme.getColor(13));
		Interface.attachEditTextStyle(search_input);
		search_input.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				adapter.setFilter(arg0.toString());
			}
		});
	    if(ColorScheme.initialized) ((LinearLayout)findViewById(R.id.history_back)).setBackgroundColor(ColorScheme.getColor(28));
    	list = (ListView)findViewById(R.id.history_message_list);
		Interface.attachSelector(list);
    	adapter = new HistoryAdapter();
    	list.setAdapter(adapter);
    	list.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				context_item = arg2;
				removeDialog(0x0);
				showDialog(0x0);
				return false;
			}
    	});
    	label = (TextView)findViewById(R.id.l1);
    	LinearLayout divider = (LinearLayout)findViewById(R.id.divider);
    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(3));
    	if(ColorScheme.initialized) divider.setBackgroundColor(ColorScheme.getColor(4));
    	TextView status = (TextView)findViewById(R.id.history_status);
    	if(ColorScheme.initialized) status.setTextColor(ColorScheme.getColor(12));
    	status.setText(Locale.getString("s_no_messages_history"));
    	Interface.attachBackground((LinearLayout)findViewById(R.id.header), Interface.history_top_panel);
    	Interface.attachBackground((FrameLayout)findViewById(R.id.history_list_field), Interface.history_messages_back);
    	Intent i = getIntent();
    	String[] raw = i.getAction().split(";;;%;;;");
    	contact = raw[0];
    	profile = raw[1];
		label.setText(raw[2]+"\n"+Locale.getString("s_messages_history"));
		history_database = new File(resources.DATA_PATH+raw[1]+"/"+raw[0]);
		if(history_database.exists()){
			if(history_database.length() > 0){
				status.setVisibility(View.GONE);
				startLoadingHistory();
			}
		}
    }
    protected Dialog onCreateDialog(final int type){
    	Dialog dialog = null;
    	switch(type){
    	case 0x0://Main menu
    		UAdapter adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_do_copy"), 0);
        	dialog = DialogBuilder.create(HistoryActivity.this,
        			Locale.getString("s_menu"),
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							removeDialog(0x0);
							HistoryItemA item = (HistoryItemA)HistoryActivity.this.adapter.getItem(context_item);
							ClipboardManager cm = (ClipboardManager)HistoryActivity.this.getSystemService(Service.CLIPBOARD_SERVICE);
							switch((int)arg0.getAdapter().getItemId(arg2)){
							case 0:
								cm.setText(item.message);
								Toast.makeText(HistoryActivity.this, Locale.getString("s_copied"), Toast.LENGTH_SHORT).show();
								break;
							}
						}
        	});
        	break;
    	}
    	return dialog;
    }
    private void startLoadingHistory(){
    	progress = DialogBuilder.createProgress(this, Locale.getString("s_please_wait"), false);
    	progress.show();
    	Thread load_thread = new Thread(){
    		@Override
    		public void run(){
    			try {
        			sleep(300);
    				DataInputStream dis = new DataInputStream(new FileInputStream(history_database));
    				while(true){
    					if(dis.available() > 0){
    						int direction = dis.readByte();
    						long time = dis.readLong();
    						int msgLen = dis.readInt();
    						byte[] message = new byte[msgLen];
    						dis.read(message, 0, msgLen);
    						String msg = new String(message, "windows1251");
    						HistoryItemA item = new HistoryItemA(time);
    						item.direction = direction;
    						item.message = msg;
    						item.contact = contact;
    						item.profile = profile;
    						adapter.put(item);
    					}else{
    						try {
    							dis.close();
    						} catch (IOException e) {
    							e.printStackTrace();
    						}
    						break;
    					}
    				}
    			}catch(Exception e){
    				e.printStackTrace();
    			}
				label.post(new Runnable(){
					@Override
					public void run() {
						adapter.notifyDataSetChanged();
						if(progress != null) progress.dismiss();
					}
				});
    		}
    	};
    	load_thread.start();
    }
}
