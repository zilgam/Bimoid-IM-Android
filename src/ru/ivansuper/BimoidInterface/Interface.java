package ru.ivansuper.BimoidInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.StateListDrawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.CheckBox;

public class Interface {
	public static final String contact_list_items_back="contact_list_items_back";
	public static final String contact_list_bottom_back="contact_list_bottom_back";
	public static final String status_selector_back="status_selector_back";
	public static final String dialogs_back="dialogs_back";
	public static final String list_view_back="list_view_back";
	public static final String list_selector="list_selector";
	public static final String chat_top_panel="chat_top_panel";
	public static final String chat_messages_back="chat_messages_back";
	public static final String chat_bottom_panel="chat_bottom_panel";
	public static final String smileys_back="smileys_back";
	public static final String history_top_panel="history_top_panel";
	public static final String history_messages_back="history_messages_back";
	public static final String ignored_top_panel="ignored_top_panel";
	public static final String ignored_list_back="ignored_list_back";
	public static final String search_top_panel="search_top_panel";
	public static final String search_list_back="search_list_back";
	public static final String profiles_top_panel="profiles_top_panel";
	public static final String profiles_list_back="profiles_list_back";
	public static final String registration_top_panel="registration_top_panel";
	public static final String registration_list_back="registration_list_back";
	public static final String btn_normal="btn_normal";
	public static final String btn_focused="btn_focused";
	public static final String btn_pressed="btn_pressed";
	public static final String input_normal="input_normal";
	public static final String input_focused="input_focused";
	public static final String input_pressed="input_pressed";
	public static final String chk_normal="chk_normal";
	public static final String chk_checked="chk_checked";
	public static final String img_mail_box_count_back="img_mail_box_count_back";
	//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	private static HashMap<String, DrawableContainer> components = new HashMap<String, DrawableContainer>();
	public static boolean initialized;
	public static String current_skin;
	public static void init(){
		if(initialized) return;
		forceLoad();
		initialized = true;
	}
	public static void forceLoad(){
		current_skin = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getString("current_skin", "#$%INTERNAL#$%");
		ColorScheme.initialize("ColorScheme/ColorScheme.bcs", true);
		DrawableContainer container;
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.contactlist_items_back, container.rect);
		components.put("contact_list_items_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.contactlist_bottom_panel, container.rect);
		components.put("contact_list_bottom_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.popup_back, container.rect);
		components.put("status_selector_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.blue_glow, container.rect);
		components.put("dialogs_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.list_views_back, container.rect);
		components.put("list_view_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.item_selected, container.rect);
		components.put("list_selector", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_top_panel, container.rect);
		components.put("chat_top_panel", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_messages_back, container.rect);
		components.put("chat_messages_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_bottom_panel, container.rect);
		components.put("chat_bottom_panel", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.smileys_selector_back, container.rect);
		components.put("smileys_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_top_panel, container.rect);
		components.put("history_top_panel", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.profiles_and_ignore_back, container.rect);
		components.put("history_messages_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_top_panel, container.rect);
		components.put("ignored_top_panel", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.profiles_and_ignore_back, container.rect);
		components.put("ignored_list_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_top_panel, container.rect);
		components.put("search_top_panel", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.profiles_and_ignore_back, container.rect);
		components.put("search_list_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_top_panel, container.rect);
		components.put("profiles_top_panel", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.profiles_and_ignore_back, container.rect);
		components.put("profiles_list_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.chat_top_panel, container.rect);
		components.put("registration_top_panel", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.profiles_and_ignore_back, container.rect);
		components.put("registration_list_back", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.button_normal, container.rect);
		components.put("btn_normal", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.button_selected, container.rect);
		components.put("btn_focused", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.button_pressed, container.rect);
		components.put("btn_pressed", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.textfield_normal, container.rect);
		components.put("input_normal", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.textfield_selected, container.rect);
		components.put("input_focused", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.textfield_pressed, container.rect);
		components.put("input_pressed", container);

		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.checkbox_off, container.rect);
		components.put("chk_normal", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.checkbox_on, container.rect);
		components.put("chk_checked", container);
		
		container = new DrawableContainer();
		container.bitmap = decodeBitmap(R.drawable.img_mail_box_count_back, container.rect);
		components.put(img_mail_box_count_back, container);
		
		if(!current_skin.equals("#$%INTERNAL#$%")){
			Log.e("Interface", "External skin used!");
			File config = new File(resources.SKINS_PATH+current_skin+"/SkinConfig.bsf");
			if(config.length() < 256) return;
			try {
				BufferedReader reader = new BufferedReader(new FileReader(config));
				String line = null;
				while(reader.ready()){
					line = reader.readLine();
					if(line.startsWith("//")) continue;
					String[] parts = line.split("=");
					if(parts.length != 2) continue;
					String param = parts[0].trim();
					String value = parts[1].trim();
					if(param.equals("color_scheme")){
						Log.e("Interface", "External color scheme used!");
						ColorScheme.forceInit(resources.SKINS_PATH+current_skin+"/"+value, false);
						continue;
					}
					Rect rect = new Rect();
					Bitmap bmp = decodeBitmap(value, rect);
					if(bmp == null){
						Log.e("External Skin Loader", param+"="+value+"; (not found)");
						continue;
					}
					container = new DrawableContainer();
					container.bitmap = bmp;
					container.rect = rect;
					components.put(param, container);
					Log.i("External Skin Loader", param+"="+value+";");
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
	private static Bitmap decodeBitmap(String file, Rect rect){
		//BitmapFactory.Options opts = new BitmapFactory.Options();
		File raw = new File(resources.SKINS_PATH+current_skin+"/"+file);
		if(!raw.exists()) return null;
		FileInputStream is = null;
		try {
			is = new FileInputStream(raw);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}
		Bitmap bitmap = BitmapFactory.decodeStream(is, rect, null);
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	private static Bitmap decodeBitmap(int resId, Rect rect){
		//BitmapFactory.Options opts = new BitmapFactory.Options();
		InputStream is = resources.res.openRawResource(resId);
		Bitmap bitmap = BitmapFactory.decodeStream(is, rect, null);
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	public static Drawable getDrawable(String component, Rect rect){
		DrawableContainer container = components.get(component);
		container.bitmap.setDensity(0);
		if(container.rect == null){
			//Log.i("Interface:getDrawable()", "Rect of component "+component+" is null");
			container.rect = new Rect(0, 0, 0, 0);
		}
		byte[] nine_patch_chunk = container.bitmap.getNinePatchChunk();
		Drawable drawable = null;
		if(nine_patch_chunk != null){
			Rect new_rect = new Rect(container.rect.left,
										container.rect.top,
										container.rect.right,
										container.rect.bottom);
			if(rect != null){
				new_rect.set(new_rect.left+rect.left,
						new_rect.top+rect.top,
						new_rect.right+rect.right,
						new_rect.bottom+rect.bottom);
			}
			drawable = new NinePatchDrawable(resources.res, container.bitmap, container.bitmap.getNinePatchChunk(), container.rect, component);
		}else{
			drawable = new BitmapDrawable(container.bitmap);
		}
		if(component.equals("list_selector")){
			final StateListDrawable list_selector_ = new StateListDrawable();
			int[] state = new int[]{-android.R.attr.state_window_focused};
			list_selector_.addState(state, new ColorDrawable(0x0));
			state = new int[]{android.R.attr.state_focused, -android.R.attr.state_enabled, android.R.attr.state_pressed};
			list_selector_.addState(state, new ColorDrawable(0x0));
			state = new int[]{android.R.attr.state_focused, -android.R.attr.state_enabled};
			list_selector_.addState(state, new ColorDrawable(0x0));
			state = new int[]{android.R.attr.state_focused, android.R.attr.state_pressed};
			list_selector_.addState(state, drawable);
			state = new int[]{-android.R.attr.state_focused, android.R.attr.state_pressed};
			list_selector_.addState(state, drawable);
			state = new int[]{-android.R.attr.state_focused};
			list_selector_.addState(state, new ColorDrawable(0x0));
			drawable = list_selector_;
		}
		return drawable;
	}
	public static void attachWindowBackground(Window window, String component){
		final Drawable drawable = getDrawable(component, null);
		window.setBackgroundDrawable(drawable);
	}
	public static void attachBackground(View view, String component){
		final Drawable drawable = getDrawable(component, null);
		view.setBackgroundDrawable(drawable);
	}
	public static void attachSelector(AbsListView list){
		final Drawable drawable = getDrawable(Interface.list_selector, null);
		list.setSelector(drawable);
	}
	public static Drawable getSelector(){
		return getDrawable(Interface.list_selector, null);
	}
	public static void attachButtonStyle(View view){
		final Drawable btn_normal = getDrawable(Interface.btn_normal, new Rect(32, 16, 32, 16));
		final Drawable btn_pressed = getDrawable(Interface.btn_pressed, new Rect(32, 16, 32, 16));
		final Drawable btn_focused = getDrawable(Interface.btn_focused, new Rect(32, 16, 32, 16));
		if(btn_normal == null) return;
		if(btn_pressed == null) return;
		if(btn_focused == null) return;
		StateListDrawable style = new StateListDrawable();
		int[] state = new int[]{android.R.attr.state_pressed};
		style.addState(state, btn_pressed);
		state = new int[]{android.R.attr.state_focused};
		style.addState(state, btn_focused);
		state = new int[]{android.R.attr.state_enabled};
		style.addState(state, btn_normal);
		view.setBackgroundDrawable(style);
	}
	public static void attachCheckBoxStyle(CheckBox check_box){
		final Drawable chk_normal = getDrawable(Interface.chk_normal, null);
		final Drawable chk_checked = getDrawable(Interface.chk_checked, null);
		if(chk_normal == null) return;
		if(chk_checked == null) return;
		StateListDrawable style = new StateListDrawable();
		int[] state = new int[]{android.R.attr.state_checked};
		style.addState(state, chk_checked);
		state = new int[]{android.R.attr.state_enabled};
		style.addState(state, chk_normal);
		check_box.setButtonDrawable(style);
	}
	public static void attachEditTextStyle(View view){
		final Drawable input_normal = getDrawable(Interface.input_normal, null);
		final Drawable input_pressed = getDrawable(Interface.input_pressed, null);
		final Drawable input_focused = getDrawable(Interface.input_focused, null);
		if(input_normal == null) return;
		if(input_pressed == null) return;
		if(input_focused == null) return;
		StateListDrawable style = new StateListDrawable();
		int[] state = new int[]{android.R.attr.state_pressed};
		style.addState(state, input_pressed);
		state = new int[]{android.R.attr.state_focused};
		style.addState(state, input_focused);
		state = new int[]{android.R.attr.state_enabled};
		style.addState(state, input_normal);
		view.setBackgroundDrawable(style);
	}
	@Override
	public void finalize() throws Throwable {
		throw new RuntimeException("Holding resources");
	}
}
