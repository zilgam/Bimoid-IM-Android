package ru.ivansuper.Preferences;

import java.util.ArrayList;

import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.SettingsActivity;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.locale.Language;
import ru.ivansuper.locale.Locale;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class LanguagePicker extends DialogPreference {
	private int current = 0;
	private SharedPreferences manager;
	private View lay;
	public LanguagePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		manager = PreferenceManager.getDefaultSharedPreferences(getContext());
	}
	@Override
	protected View onCreateDialogView(){
		lay = (LinearLayout)View.inflate(resources.ctx, R.layout.columns_picker, null);
		return lay;
	}
	@Override
	public void showDialog(Bundle state){
		super.showDialog(state);
	}
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		current = Integer.parseInt(manager.getString(super.getKey(), String.valueOf(Locale.DEFAULT)));
		final TextView title = (TextView)view.findViewById(R.id.l2);
		title.setText(this.getTitle());
		final LinearLayout lay = (LinearLayout)view;
		RadioGroup rg = (RadioGroup)lay.findViewById(R.id.rg1);
		rg.removeAllViews();
		ArrayList<Language> list = Locale.getAvailable();
		int i = 0;
		for(Language language: list){
			RadioButton r = new RadioButton(this.getContext());
			r.setText(language.NAME+"\n"+Locale.getString("s_ms_select_language_language")+" "+language.LANGUAGE+"\n");
			final int ii = i;
			r.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					current = ii;
				}
			});
			rg.addView(r);
			if(current == i) r.setChecked(true);
			i++;
		}
	}
	protected void onDialogClosed(boolean positiveResult){
		if(positiveResult){
			//Log.e("LanguagePicker", "Saving: "+(current));
			manager.edit().putString(getKey(), String.valueOf(current)).commit();
			Toast toast = Toast.makeText(getContext(), Locale.getString("s_app_needs_to_restart"), Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			//Locale.prepare();
			//SettingsActivity.update();
		}
	}
}
