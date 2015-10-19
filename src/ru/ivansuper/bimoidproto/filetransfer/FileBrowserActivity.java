package ru.ivansuper.bimoidproto.filetransfer;

import java.io.File;
import java.io.IOException;

import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidim.ContactListActivity;
import ru.ivansuper.bimoidim.DialogBuilder;
import ru.ivansuper.bimoidim.ProfilesActivity;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.locale.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

public class FileBrowserActivity extends Activity {
	private GridView list;
	public static int LISTVIEW_STATE = 0;
	private files_adapter adp;
	private String selected_file = "";
	private boolean file_select_mode = false;
	private String start_path;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String action = getIntent().getAction();
        if(action != null){
        	if(action.startsWith("get_file")){
        		file_select_mode = true;
        		String[] params = action.split(":");
        		if(params.length > 1){
	        		final File recent = new File(params[1]);
	        		if(recent.exists()){
	        			final String path = recent.getAbsolutePath();
	        			Log.e("AbsolutePath", path);
	        			int idx = 0;
	        			while(true){
	        				int i = path.indexOf(File.separator, idx);
	            			Log.e("Searching at "+idx, "i: "+i);
	        				if(i != -1){
	        					idx = i+1;
	        				}else{
	        					break;
	        				}
	        			}
	        			start_path = path.substring(0, idx);
	        			Log.e("start_path", start_path);
	        		}
        		}
        	}
        }
        setVolumeControlStream(0x3);
        setContentView(R.layout.file_browser_activity);
        if(!file_select_mode){
        	((TextView)findViewById(R.id.l1)).setText(Locale.getString("s_file_browser_hint"));
        }
        initViews();
        adp = new files_adapter();
        File sd = new File((start_path == null)? resources.SD_PATH: start_path);
        adp.setData(sd.listFiles(), sd.getParentFile());
        list.setAdapter(adp);
        list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				files_adapter adp = (files_adapter)arg0.getAdapter();
				if(arg2 == 0){
					File path = adp.parent;
					if(path != null){
						adp.setData(path.listFiles(), path.getParentFile());
					}
				}else{
					File item = adp.getItem(arg2);
					if(item.isDirectory()){
						//adp.notifyDataSetInvalidated();
						adp.setData(item.listFiles(), item.getParentFile());
					}else{
						if(file_select_mode){
							selected_file = item.getAbsolutePath();
		            		showDialog(1);
						}else{
							adp.toggleSelection(arg2);
						}
						/*Intent result = new Intent();
						//result.putExtra("file_name", item.getAbsolutePath());
						result.setAction(item.getAbsolutePath());
						FileBrowserActivity.this.setResult(RESULT_OK, result);
						finish();*/
					}
				}
			}
        });
        /*list.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if((arg1.getAction() == MotionEvent.ACTION_UP) && (LISTVIEW_STATE == 2))
					adp.notifyDataSetChanged();
				return false;
			}
        });*/
        list.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
			}
			@Override
			public void onScrollStateChanged(AbsListView arg0, int arg1) {
				LISTVIEW_STATE = arg1;
				if(arg1 == 0){
					adp.notifyDataSetChanged();
				}else{
					adp.timestamp = System.currentTimeMillis();
				}
			}
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
    	//super.onKeyDown(keyCode, event)
    	if(event.getAction() == KeyEvent.ACTION_DOWN){
    		switch(keyCode){
    		case KeyEvent.KEYCODE_BACK:
				FileBrowserActivity.this.setResult(RESULT_CANCELED);
    			finish();
    			break;
    		case KeyEvent.KEYCODE_MENU:
    			if(file_select_mode) break;
            	if(adp.getSelected().length() == 0){
            		showDialog(0);
            	}else{
            		showDialog(1);
            	}
    			break;
    		}
    	}
    	return false;
    }
	private void initViews(){
		list = (GridView)findViewById(R.id.file_browser_list);
		list.setSelector(Interface.getSelector());
		if(resources.IT_IS_TABLET) list.setNumColumns(4);
	}
    protected Dialog onCreateDialog(final int type){
    	Dialog dialog = null;
    	switch(type){
    	case 0:
        	dialog = DialogBuilder.createOk(FileBrowserActivity.this,
        			Locale.getString("s_information"), Locale.getString("s_file_browser_notify_1"),
    				Locale.getString("s_close"), Gravity.TOP, new OnClickListener(){
    					@Override
    					public void onClick(View v) {
    						removeDialog(type);
    					}
    		});
    		break;
    	case 1:
    		dialog = DialogBuilder.createYesNo(FileBrowserActivity.this,
    				Gravity.TOP,
    				file_select_mode? Locale.getString("s_file_select_mode"): Locale.getString("s_file_transfer"),
    				file_select_mode? Locale.getString("s_file_select_confirm"): Locale.getString("s_send_files_question"),
            		Locale.getString("s_yes"), Locale.getString("s_no"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(1);
    						Intent i = new Intent();
    						i.setAction(file_select_mode? selected_file: adp.getSelected());
    						FileBrowserActivity.this.setResult(RESULT_OK, i);
    						finish();
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						selected_file = "";
    						removeDialog(1);
    					}
    				});
    		break;
    	}
    	return dialog;
    }
}
