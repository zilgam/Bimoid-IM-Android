package ru.ivansuper.ui;

import java.util.Vector;

import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class ExFragment extends JFragment {
	public Activity ACTIVITY;
	private final DialogManager mDialogManager = new DialogManager();
	public void onAttach(Activity activity){
		ACTIVITY = activity;
		super.onAttach(activity);
	}
	public void setTheme(int theme){
		ACTIVITY.setTheme(theme);
	}
	public SharedPreferences getDefaultSharedPreferences(){
		return PreferenceManager.getDefaultSharedPreferences(ACTIVITY);
	}
	public void setVolumeControlStream(int stream){
		ACTIVITY.setVolumeControlStream(stream);
	}
	public Window getWindow(){
		return ACTIVITY.getWindow();
	}
	public Object getSystemService(String service){
		return ACTIVITY.getSystemService(service);
	}
	public void finish(){
		ACTIVITY.finish();
	}
	public View findViewById(int id){
		View v = getView();
		if(v == null) return null;
		return v.findViewById(id);
	}
	public Intent getIntent(){
		return ACTIVITY.getIntent();
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	}
	public void onConfigurationChanged(Configuration configuration, int diff){
	}
	public void showDialog(int id){
		Dialog d = onCreateDialog(id);
		mDialogManager.showDialog(id, d);
	}
	public Dialog onCreateDialog(int id){
		return null;
	}
	public void removeDialog(int id){
		mDialogManager.removeDialog(id);
	}
	//FRAGMENT CODE
	public void onCreate(){
		super.onCreate();
		ExFragmentManager.addFragment(this);
	}
	public void onDestroy(){
		super.onDestroy();
		ExFragmentManager.removeFragment(this);
	}
	private class DialogManager {
		private Vector<DialogItem> dialogs = new Vector<DialogItem>();
		private synchronized Dialog getById(int id){
			for(DialogItem item: dialogs)
				if(item.id == id) return item.dialog;
			return null;
		}
		public synchronized void showDialog(int id, Dialog d){
			Dialog d_ = getById(id);
			if(d_ == null){
				DialogItem item = new DialogItem();
				item.dialog = d;
				item.id = id;
				dialogs.add(item);
				d_ = d;
			}
			d_.show();
		}
		public synchronized void removeDialog(int id){
			Dialog d = getById(id);
			if(d != null)
				if(d.isShowing())
					d.dismiss();
			for(DialogItem item: dialogs)
				if(item.id == id) dialogs.remove(item);
		}
		private class DialogItem {
			public Dialog dialog;
			public int id;
		}
	}
}
