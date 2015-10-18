package ru.ivansuper.bimoidim;

import java.io.File;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.locale.Locale;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SkinsActivity extends Activity {
	private ListView skins;
	private UAdapter skins_adapter;
	private Button apply;
	private int last_selected;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skins);
		initViews();
        setVolumeControlStream(0x3);
	}
	private void initViews(){
		utilities.setLabel((TextView)findViewById(R.id.l1), "s_available_skins");
		skins = (ListView)findViewById(R.id.skins_list);
		skins_adapter = new UAdapter();
		skins_adapter.setMode(UAdapter.FORCE_HIDE_ICON);
		skins_adapter.setPadding(16);
		skins_adapter.setTextColorA(0xffffffff);
		skins.setAdapter(skins_adapter);
		skins.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				last_selected = arg2;
				skins_adapter.setSelected(arg2);
				skins_adapter.notifyDataSetChanged();
			}
		});
		apply = (Button)findViewById(R.id.skins_apply);
		apply.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(last_selected >= skins_adapter.getCount()) return;
				final String selected;
				if(last_selected == 0){
					selected = "#$%INTERNAL#$%";
				}else{
					selected = skins_adapter.getItem(last_selected);
				}
				PreferenceManager.getDefaultSharedPreferences(resources.ctx).edit().putString("current_skin", selected).commit();
				fill();
				Interface.forceLoad();
				Toast.makeText(resources.ctx, Locale.getString("s_selected_skin_saved"), Toast.LENGTH_SHORT).show();
				if(resources.service != null) resources.service.refreshContactListInterface();
			}
		});
		apply.setText(Locale.getString("s_apply_skin"));
        fill();
	}
	private void fill(){
		skins_adapter.clear();
		skins_adapter.put(Locale.getString("s_standard_skin"), 0);
		String current = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getString("current_skin", "#$%INTERNAL#$%");
		if(current.equals("#$%INTERNAL#$%")){
			skins_adapter.setSelected(0);
		}
		File skins_dir = new File(resources.SKINS_PATH);
		if(!skins_dir.exists()) return;
		File[] dirs = skins_dir.listFiles();
		for(File file: dirs)
			if(file.isDirectory())
				if(isSkin(file)){
					final String name = file.getName();
					skins_adapter.put(name, 1);
					if(current.equals(name)) skins_adapter.setSelected(skins_adapter.getCount()-1);
				}
	}
	private boolean isSkin(File file){
		File[] files = file.listFiles();
		for(File file_: files)
			if(file_.getName().equals("SkinConfig.bsf"))
				return true;
		return false;
	}
}
