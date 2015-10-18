package ru.ivansuper.bimoidim;

import ru.ivansuper.BimoidInterface.Interface;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class SmileysSelector extends Activity {
	private GridView smileys;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        setVolumeControlStream(0x3);
        setContentView(R.layout.smileys_selector);
        smileys = (GridView)findViewById(R.id.smileys_selector_field);
        Interface.attachBackground(smileys, Interface.smileys_back);
		Interface.attachSelector(smileys);
        smileys.setNumColumns(PreferenceTable.smileys_selector_columns);
		final smileys_adapter adapter = new smileys_adapter();
		smileys.setAdapter(adapter);
		smileys.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String tag = adapter.getTag(arg2);
				Intent i = new Intent();
				i.setAction(" "+tag+" ");
				Log.i("SmileysSelector", i.getAction());
				SmileysSelector.this.setResult(RESULT_OK, i);
				finish();
			}
		});
	}
}
