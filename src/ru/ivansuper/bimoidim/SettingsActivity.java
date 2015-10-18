package ru.ivansuper.bimoidim;

import ru.ivansuper.locale.Locale;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
	public static SettingsActivity static_instance;
	public static PreferenceScreen static_instance_preference_screen;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        setVolumeControlStream(0x3);
		addPreferencesFromResource(R.xml.prefs);
	}
	@Override
	public void onResume(){
		super.onResume();
		static_instance = this;
	}
	@Override
	public void onPause(){
		super.onPause();
	}
	@Override
	public void setPreferenceScreen(PreferenceScreen preferenceScreen){
		super.setPreferenceScreen(preferenceScreen);
		static_instance_preference_screen = preferenceScreen;
		for(int i=0; i<preferenceScreen.getPreferenceCount(); i++){
			Preference p = preferenceScreen.getPreference(i);
			proceedPreference(p);
			try{
				PreferenceScreen s = (PreferenceScreen)p;
				for(int j=0; j<s.getPreferenceCount(); j++){
					Preference pp = s.getPreference(j);
					proceedPreference(pp);
				}
			}catch(Exception e){}
		}
	}
	private void proceedPreference(Preference preference){
		String p = preference.getKey();
		String title = Locale.getString("s_"+p);
		if(!(preference instanceof PreferenceCategory))
			preference.setLayoutResource(R.layout.preference_item);
		try{
			DialogPreference dp = (DialogPreference)preference;
			String msg = Locale.getString("s_"+p+"_msg");
			if(!msg.equals("null"))
				dp.setDialogMessage(msg);
			dp.setTitle(title);
			dp.setPositiveButtonText(Locale.getString("s_ok"));
			dp.setNegativeButtonText(Locale.getString("s_cancel"));
		}catch(ClassCastException e){}
		try{
			ListPreference lp = (ListPreference)preference;
			lp.setDialogTitle(title);
			lp.setPositiveButtonText(Locale.getString("s_ok"));
			lp.setNegativeButtonText(Locale.getString("s_cancel"));
		}catch(ClassCastException e){}
		if(title.equals("null")) return;
		preference.setTitle(title);
		String desc = Locale.getString("s_"+p+"_desc");
		if(desc.equals("null")) return;
		preference.setSummary(desc);
	}
	public static void update(){
		Log.e("Preferences", "static_instance_preference_screen is null: "+(static_instance_preference_screen == null));
		Log.e("Preferences", "static_instance is null: "+(static_instance == null));
		if(static_instance_preference_screen != null) static_instance.setPreferenceScreen(static_instance_preference_screen);
		if(static_instance != null) static_instance.onContentChanged();
	}
}
