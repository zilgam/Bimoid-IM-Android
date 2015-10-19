package ru.ivansuper.bimoidproto.transports;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Browser;
import android.text.TextUtils.TruncateAt;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.UAdapter;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.socket.ByteBuffer;
import ru.ivansuper.ui.TabsContentHolder;
import ru.ivansuper.ui.TabsContentHolder.TabContent;

public class TransportSettings {
	
	public static final int TP_SF_HIDE_SRV_PARAMS = 0x0001;
	
	private SettingsDir mHierarchicalPlacement;
	private final ArrayList<Setting> mEntries = new ArrayList<Setting>();
	private int mFlags = 0;
	
	public TransportSettings(ByteBuffer buffer){
		
		if(buffer == null) return;
		
		mFlags = buffer.readWord();
		
		final int entries_count = buffer.readWord();
		
		for(int i=0; i<entries_count; i++){
			final int block_length = buffer.readWord();
			
			mEntries.add(new Setting(new ByteBuffer(buffer.readBytes(block_length))));
		}
		
		mHierarchicalPlacement = new SettingsDir("ROOT", null, null, null);
		mHierarchicalPlacement.mRoot = true;
		
		sort();
		
		//mHierarchicalPlacement.trace();
		
	}
	
	private final void sort(){
		
		final int length = mEntries.size();
		
		for(int i=0; i<length; i++){
			
			final Setting setting = mEntries.get(i);
			
			final SettingsDir dir = getDir(setting.buildPath());
			
			dir.mSettings.add(setting);
			
		}
		
	}
	
	private final SettingsDir getDir(String path){
		
		return getDirRec(mHierarchicalPlacement, path);
		
	}
	private final SettingsDir getDirRec(SettingsDir parent, String path){
		
		final String parent_path = parent.buildPath();
		
		//Log.e(getClass().getSimpleName()+":getDirRec", "Dest: '"+path+"' in: '"+parent_path+"'");
		
		for(int i=0; i<parent.mDirs.size(); i++){
			
			final SettingsDir dir = parent.mDirs.get(i);
			
			final String builded_path = dir.buildPath();
			
			if(path.equals(builded_path)) return dir;
			if(path.startsWith(builded_path)) return getDirRec(dir, path);
			
		}
		
		final int builded_path_length = parent_path.length();
		
		int end_index = path.indexOf("\\", builded_path_length);
		if(end_index < 0) end_index = path.length();
		
		final String new_dir_name = path.substring(builded_path_length, end_index);
		
		//Log.e(getClass().getSimpleName(), "Adding dir: '"+new_dir_name+"'");
		
		final SettingsDir new_dir = new SettingsDir(new_dir_name, parent, null, null);
		
		if(new_dir.buildPath().equals(path)) return new_dir;
		
		return getDirRec(new_dir, path);
		
	}
	
	public final void updateValues(byte[] block){
		
		if(block == null) return;
		
		final ByteBuffer buffer = new ByteBuffer(block);
		
		buffer.skip(2);
		
		final int entries_count = buffer.readWord();
		
		for(int i=0; i<entries_count; i++){
			
			final int block_length = buffer.readWord();
			
			final ByteBuffer setting_bin = new ByteBuffer(buffer.readBytes(block_length));
			
			final int setting_id = setting_bin.readWord();
			
			final Setting setting = getSettingById(setting_id);
			
			if(setting == null) continue;
			
			setting.update(setting_bin);
			
		}
		
	}
	
	public final byte[] serialize(boolean for_send){
		
		final ByteBuffer buffer = new ByteBuffer();
		
		buffer.writeWord(mFlags);
		buffer.writeWord(mEntries.size());
		
		final int length = mEntries.size();
		
		for(int i=0; i<length; i++){
			final Setting setting = mEntries.get(i);
			
			//if(for_send && ((!setting.itIsCheckbox() && !setting.itIsComboBox() && !setting.itIsEditText()) || setting.itIsDisabled())) continue;
			
			final ByteBuffer content = new ByteBuffer();
			
			content.writeWord(setting.mOptionId);
			content.writeByte(setting.mValueType);
			content.writeDWord(setting.mValueFlags);
			
			content.writePreLenStringUTF8(setting.mName);
			content.writePreLenStringUTF8(setting.mSelectedValue);
			
			buffer.writeWord(content.writePos);
			buffer.writeByteBuffer(content);
			
		}
		
		return buffer.getBytes();
		
	}
	
	public final Setting getSettingById(int id){
		
		final int length = mEntries.size();
		
		for(int i=0; i<length; i++){
			final Setting setting = mEntries.get(i);
			
			if(setting.mOptionId == id) return setting;
		}
		
		return null;
		
	}
	
	public final boolean isServerOptionsUsed(){
		
		return (mFlags & TP_SF_HIDE_SRV_PARAMS) != TP_SF_HIDE_SRV_PARAMS;
		
	}
	
	public final void updateValuesFromForm(TabsContentHolder tabs_content){
		
		final int length = mEntries.size();
		
		for(int i=0; i<length; i++){
			
			final Setting setting = mEntries.get(i);
			
			final View setting_view = tabs_content.findViewWithTag(setting.mOptionId);
			
			if(setting_view instanceof CheckBox){
				
				final CheckBox check_box = (CheckBox)setting_view;
				
				setting.mSelectedValue = check_box.isChecked()? "1": "0";
				
			}
			
			if(setting_view instanceof EditText){
				
				final EditText edit_text = (EditText)setting_view;
				
				setting.mSelectedValue = edit_text.getText().toString();
				
			}
			
			if(setting_view instanceof Spinner){
				
				final Spinner spinner = (Spinner)setting_view;
				
				setting.mSelectedValue = String.valueOf(spinner.getSelectedItemPosition());
				
			}
			
		}
		
	}
	
	public final void buildGUI(TabsContentHolder tabs_content){
		
		final int length = mHierarchicalPlacement.mDirs.size();
		
		for(int i=0; i<length; i++){
			final SettingsDir settings_dir = mHierarchicalPlacement.mDirs.get(i);
			
			final View tab_layout = buildCategoryRec(tabs_content.getContext(), settings_dir, false);
			
			final TabContent tab_content = new TabContent(settings_dir.mName, tab_layout);
			
			tabs_content.addTab(tab_content);
			
		}
		
	}
	
	private final View buildCategoryRec(final Context context, SettingsDir dir, boolean insert_name){
		
		final ScrollView scroll_view = new ScrollView(context);
		scroll_view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		final LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		
		if(insert_name){
			final TextView category_name = new TextView(context);
			category_name.setSingleLine();
			category_name.setTextColor(0xff3a5a82);
			category_name.setTextSize(18);
			category_name.setTypeface(Typeface.DEFAULT_BOLD);
			category_name.setEllipsize(TruncateAt.END);
			
			category_name.setText(dir.mName);
			
			layout.addView(category_name);
		}
		
		{
			final int length = dir.mDirs.size();
			
			for(int i=0; i<length; i++){
				
				final SettingsDir settings_dir = dir.mDirs.get(i);
				
				layout.addView(buildCategoryRec(context, settings_dir, true));
				
			}
		}
		
		final LinearLayout settings_layout = new LinearLayout(context);
		settings_layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		settings_layout.setOrientation(LinearLayout.VERTICAL);
		
		final int padding = (int)resources.dm.density*10;
		
		settings_layout.setPadding(padding, padding, padding, padding);
		
		{
			final int length = dir.mSettings.size();
			
			for(int i=0; i<length; i++){
				
				final Setting setting = dir.mSettings.get(i);
				
				if(setting.itIsDisabled()) continue;
				
				if(setting.itIsCheckbox()){
					
					final CheckBox check_box = new CheckBox(context);
					check_box.setTextColor(0xff3a5a82);
					check_box.setTextSize(16);
					Interface.attachCheckBoxStyle(check_box);
					
					check_box.setText(setting.mName);
					check_box.setChecked(setting.mSelectedValue.equals("1")? true: false);
					
					check_box.setTag(setting.mOptionId);
					
					settings_layout.addView(check_box);
					
					continue;
				}
				
				if(setting.itIsEditText()){
					
					final TextView label = new TextView(context);
					label.setTextColor(0xff3a5a82);
					label.setTextSize(16);
					label.setPadding(padding/2, padding/2, padding/2, padding/2);
					
					label.setText(setting.mName+":");
					
					settings_layout.addView(label);
					
					final EditText edit_text = new EditText(context);
					Interface.attachEditTextStyle(edit_text);
					
					edit_text.setText(setting.mSelectedValue);
					edit_text.setTag(setting.mOptionId);
					
					settings_layout.addView(edit_text);
					
					continue;
				}
				
				if(setting.itIsLink()){
					
					final TextView link = new TextView(context);
					link.setTextColor(0xff3a5a82);
					link.setTextSize(16);
					link.setPadding(padding/2, padding/2, padding/2, padding/2);
					
					link.setText(setting.mName);
					
					link.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							final Intent exec = new Intent("android.intent.action.VIEW");
							exec.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							exec.setData(Uri.parse(setting.mSelectedValue));
							context.startActivity(exec);
						}
					});
					
					settings_layout.addView(link);
					
					continue;
				}
				
				if(setting.itIsComboBox()){
					
					final TextView label = new TextView(context);
					label.setTextColor(0xff3a5a82);
					label.setTextSize(16);
					label.setPadding(padding/2, padding/2, padding/2, padding/2);
					
					label.setText(setting.mName+":");
					
					settings_layout.addView(label);
					
					final Spinner spinner = new Spinner(context);
					spinner.setBackgroundResource(R.drawable.combo);
					spinner.setPadding(0, 0, 0, 0);
					spinner.setTag(setting.mOptionId);
					
					final UAdapter values_list = new UAdapter();
					values_list.setMode(UAdapter.FORCE_HIDE_ICON);
					values_list.setPadding(padding/2);
					
					final int values_count = setting.mValues.size();
					
					for(int j=0; j<values_count; j++)
						values_list.put(setting.mValues.get(j), j);
					
					spinner.setAdapter(values_list);
					spinner.setSelection(Integer.parseInt(setting.mSelectedValue));
					
					settings_layout.addView(spinner);
					
					continue;
				}
				
			}
		}
		
		layout.addView(settings_layout);
		
		scroll_view.addView(layout);
		
		return scroll_view;
		
	}
	
	private static class SettingsDir {
		
		private SettingsDir mParent;
		private final String mName;
		private final ArrayList<SettingsDir> mDirs;
		private final ArrayList<Setting> mSettings;
		
		private boolean mRoot;
		
		public SettingsDir(String name, SettingsDir parent, SettingsDir[] dirs, Setting[] settings){
			
			mParent = parent;
			mName = name;
			mDirs = new ArrayList<SettingsDir>();
			mSettings = new ArrayList<Setting>();
			
			if(mParent != null){
				mParent.mDirs.add(this);
				
				//Log.e(getClass().getSimpleName(), "'"+mName+"' added to '"+mParent.mName+"'");
			}
			
			if(dirs != null) for(int i=0; i<dirs.length; i++) mDirs.add(dirs[i]);
			if(settings != null) for(int i=0; i<settings.length; i++) mSettings.add(settings[i]);
			
		}
		
		private final SettingsDir getDir(String name){
			
			for(int i=0; i<mDirs.size(); i++){
				final SettingsDir dir = mDirs.get(i);
				
				if(dir.mName.equals(name)) return dir;
			}
			
			return null;
			
		}
		
		private final String buildPath(){
			
			String path = "";
			
			if(mParent != null && !mParent.mRoot) path += mParent.buildPath();
			
			if(mName != null && !mRoot) path += mName+"\\";
			
			return path;
			
		}
		
		private final void trace(){
			
			Log.e(getClass().getSimpleName()+":category", mName);
			
			for(int i=0; i<mDirs.size(); i++) mDirs.get(i).trace();
			
			for(int i=0; i<mSettings.size(); i++){
				final Setting setting = mSettings.get(i);
				
				Log.e(getClass().getSimpleName()+":setting", "*"+setting.mName);
			}
			
		}
		
	}
	
	private static class Setting {
		
		//Option types
		public static final byte TP_OT_BOOL = 1;
		public static final byte TP_OT_BYTE = 2;
		public static final byte TP_OT_WORD = 3;
		public static final byte TP_OT_LONGWORD = 4;
		public static final byte TP_OT_QUADWORD = 5;
		public static final byte TP_OT_UTF8 = 6;
		
		//Option flags
		public static final int TP_OF_CHECK = 0x00000001;
		public static final int TP_OF_EDIT = 0x00000002;
		public static final int TP_OF_COMBO = 0x00000004;
		public static final int TP_OF_LINK = 0x00000008;
		public static final int TP_OF_CHANGE =  0x00010000; //(replaces saved value of option)
		public static final int TP_OF_READ_ONLY = 0x00020000;
		public static final int TP_OF_DISABLED = 0x00040000;
		
		private final ArrayList<String> mPath = new ArrayList<String>();
		private final ArrayList<String> mValues = new ArrayList<String>();
		private String mSelectedValue;
		private int mSelectedIndexInArray;
		private byte mValueType;
		private int mValueFlags;
		private int mOptionId;
		private String mName;
		private boolean mAsArray;
		
		private Setting(ByteBuffer buffer){
			
			mOptionId = buffer.readWord();
			mValueType = buffer.readByte();
			mValueFlags = buffer.readDWord();
			
			mAsArray = (mValueFlags & TP_OF_COMBO) == TP_OF_COMBO;
			
			final int name_length = buffer.readWord();
			mName = buffer.readStringUTF8(name_length);
			
			if(mName.indexOf("\\") > 0){// Path present
				
				int last_slash = 0;
				
				while(true){
					
					int index = mName.indexOf("\\", last_slash);
					
					if(index < 0) break;
					
					mPath.add(mName.substring(last_slash, index));
					
					last_slash = index+1;
					
				}
				
				if(last_slash > 0){
					mName = mName.substring(last_slash);
				}
				
			}
			
			final int value_length = buffer.readWord();
			final String value = buffer.readStringUTF8(value_length);
			
			if(mAsArray){
				final String[] values = mName.split(Pattern.quote("|"));
				
				mName = values[0];
				
				for(int i=1; i<values.length; i++) mValues.add(values[i]);
				
				mSelectedValue = value;
			}else{
				mValues.add(value);
				
				mSelectedValue = value;
			}
			
			String path = "";
			
			for(int i=0; i<mPath.size(); i++) path += mPath.get(i)+",";
			
			//Log.e(getClass().getSimpleName(), "Parsed: Path: '"+path+"' '"+mName+"' = '"+mSelectedValue+"'");
			
		}
		
		private final String buildPath(){
			
			final StringBuilder buf = new StringBuilder();
			
			for(int i=0; i<mPath.size(); i++) buf.append(mPath.get(i)).append("\\");
			
			return buf.toString();
			
		}
		
		private final void update(ByteBuffer buffer){
			
			buffer.skip(5);
			buffer.skip(buffer.readWord());
			
			final int value_length = buffer.readWord();
			mSelectedValue = buffer.readStringUTF8(value_length);
			
			//Log.e(getClass().getSimpleName(), "Updated: name='"+mName+"'     value='"+mSelectedValue+"'");
			
		}
		
		private final boolean itIsCheckbox(){
			return (mValueFlags & TP_OF_CHECK) == TP_OF_CHECK;
		}
		
		private final boolean itIsEditText(){
			return (mValueFlags & TP_OF_EDIT) == TP_OF_EDIT;
		}
		
		private final boolean itIsComboBox(){
			return (mValueFlags & TP_OF_COMBO) == TP_OF_COMBO;
		}
		
		private final boolean itIsLink(){
			return (mValueFlags & TP_OF_LINK) == TP_OF_LINK;
		}
		
		private final boolean itIsDisabled(){
			return (mValueFlags & TP_OF_DISABLED) == TP_OF_DISABLED;
		}
		
	}
	
}
