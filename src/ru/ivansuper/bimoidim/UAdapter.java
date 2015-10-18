package ru.ivansuper.bimoidim;

import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UAdapter extends BaseAdapter {
	public static final int SHOW_ALL = 0;
	public static final int FORCE_HIDE_LABEL = 1;
	public static final int FORCE_HIDE_ICON = 2;
	public static final int FORCE_HIDE_LABEL_AND_ICON = 3;
	private Vector<Drawable> icons = new Vector<Drawable>();
	private Vector<String> labels = new Vector<String>();
	private Vector<Integer> ids = new Vector<Integer>();
	private Vector<Integer> gravs = new Vector<Integer>();
	private Vector<Integer> select = new Vector<Integer>();
	private Vector<Object> separators = new Vector<Object>();
	private Vector<filtered_item> filtered = new Vector<filtered_item>();
	private int mode = SHOW_ALL;
	private int padding = 0;
	private int text_color = 0xffffffff;
	private int text_size = 20;
	private String filter = "";
	private boolean select_as_bold = false;
	public UAdapter(){
    	if(ColorScheme.initialized) text_color = ColorScheme.getColor(12);
	}
	@Override
	public int getCount() {
		if(filter.length() == 0){
			return labels.size();
		}else{
			return filtered.size();
		}
	}
	@Override
	public String getItem(int arg0) {
		return labels.get(arg0);
	}
	@Override
	public long getItemId(int arg0) {
		if(filter.length() == 0){
			return ids.get(arg0).intValue();
		}else{
			return filtered.get(arg0).id;
		}
	}
	@Override
	public boolean areAllItemsEnabled(){
		return false;
	}
	@Override
	public boolean isEnabled(int idx){
		if(separators.get(idx) != null) return false;
		return true;
	}
	public void put(String label, int id){
		icons.addElement(null);
		labels.addElement(label);
		ids.addElement(new Integer(id));
		gravs.addElement(null);
		select.addElement(null);
		separators.addElement(null);
	}
	public void put(Drawable icon, String label, int id){
		icons.addElement(icon);
		labels.addElement(label);
		ids.addElement(new Integer(id));
		gravs.addElement(null);
		select.addElement(null);
		separators.addElement(null);
	}
	public void put(String label, int id, int gravity){
		icons.addElement(null);
		labels.addElement(label);
		ids.addElement(new Integer(id));
		gravs.addElement(new Integer(gravity));
		select.addElement(null);
		separators.addElement(null);
	}
	public void put(Drawable icon, String label, int id, int gravity){
		icons.addElement(icon);
		labels.addElement(label);
		ids.addElement(new Integer(id));
		gravs.addElement(new Integer(gravity));
		select.addElement(null);
		separators.addElement(null);
	}
	public void put_separator(){
		icons.addElement(null);
		labels.addElement("---");
		ids.addElement(null);
		gravs.addElement(null);
		select.addElement(null);
		separators.addElement(new Object());
	}
	public void put(String[] array){
		for(int i=0; i<array.length; i++){
			icons.addElement(null);
			labels.addElement(array[i]);
			ids.addElement(new Integer(i));
			gravs.addElement(null);
			select.addElement(null);
			separators.addElement(null);
		}
	}
	public void put(Vector<Bitmap> array){
		for(int i=0; i<array.size(); i++){
			icons.addElement(new BitmapDrawable(array.get(i)));
			labels.addElement(null);
			ids.addElement(new Integer(i));
			gravs.addElement(Gravity.CENTER);
			select.addElement(null);
			separators.addElement(null);
		}
	}
	public void setMode(int mode){
		this.mode = mode;
	}
	public void toggleSelection(int pos){
		if(select.get(pos) != null){
			select.set(pos, null);
		}else{
			select.set(pos, new Integer(1));
		}
		notifyDataSetChanged();
	}
	public void setSelectionAsBold(boolean value){
		select_as_bold = value;
		notifyDataSetChanged();
	}
	public void setSelected(int pos){
		unselectAll();
		select.set(pos, new Integer(1));
		notifyDataSetChanged();
	}
	public void setUnselected(int pos){
		unselectAll();
		select.set(pos, null);
		notifyDataSetChanged();
	}
	public void unselectAll(){
		for(int i=0; i<select.size(); i++){
			select.set(i, null);
		}
	}
	public int getSelectedIdx(){
		for(int i=0; i<select.size(); i++){
			if(select.get(i) != null) return i;
		}
		return -1;
	}
	public void setPadding(int padding){
		this.padding = padding;
		notifyDataSetChanged();
	}
	public void setTextColor(int color){
		notifyDataSetChanged();
	}
	public void setTextColorA(int color){
		text_color = color;
		notifyDataSetChanged();
	}
	public void setTextSize(int size){
		text_size = size;
		notifyDataSetChanged();
	}
	public void clear(){
		icons.clear();
		labels.clear();
		ids.clear();
		gravs.clear();
		select.clear();
		separators.clear();
		notifyDataSetChanged();
	}
	public void setFilter(String expression){
		filter = expression;
		doFilter();
		notifyDataSetChanged();
	}
	private void doFilter(){
		filtered.clear();
		for(int i=0; i<labels.size(); i++){
			String label = labels.get(i);
			if(label.toLowerCase().startsWith(filter.toLowerCase())){
				filtered_item item = new filtered_item();
				item.label = label;
				item.id = ids.get(i).intValue();
				filtered.add(item);
			}
		}
	}
	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		LinearLayout lay = null;
		if(arg1 == null){
			lay = (LinearLayout)View.inflate(resources.ctx, R.layout.list_item, null);
		}else{
			lay = (LinearLayout)arg1;
		}
		ImageView icon = (ImageView)lay.findViewById(R.id.list_item_icon);
		TextView label = (TextView)lay.findViewById(R.id.list_item_label);
		label.setTypeface(Typeface.DEFAULT);
		icon.setVisibility(View.VISIBLE);
		label.setVisibility(View.VISIBLE);
		LinearLayout separator = (LinearLayout)lay.findViewById(R.id.list_item_separator);
		separator.setVisibility(View.GONE);
		lay.setBackgroundDrawable(null);
		if(!isEnabled(arg0)){
			lay.setPadding(padding, padding, padding, padding);
			label.setPadding(padding, 0, 0, 0);
			separator.setVisibility(View.VISIBLE);
			separator.setBackgroundColor(ColorScheme.getColor(4));
			icon.setVisibility(View.GONE);
			label.setVisibility(View.GONE);
			return lay;
		}
		if(select.get(arg0) != null){
			if(select_as_bold){
				label.setTypeface(Typeface.DEFAULT_BOLD);
			}else{
				lay.setBackgroundDrawable(resources.res.getDrawable(R.drawable.item_selected));
			}
		}
		lay.setPadding(padding, padding, padding, padding);
		label.setPadding(padding, 0, 0, 0);
		if(filter.length() == 0){
			if(gravs.get(arg0) == null){
				lay.setGravity(Gravity.LEFT+Gravity.CENTER_VERTICAL);
			}else{
				lay.setGravity(gravs.get(arg0).intValue());
			}
			Drawable icn = icons.get(arg0);
			if(icn != null){
				icon.setImageDrawable(icn);
			}
			label.setTextSize(text_size);
			label.setTextColor(text_color);
			label.setText(labels.get(arg0));
			if(mode != SHOW_ALL){
				if(mode == FORCE_HIDE_ICON){
					icon.setVisibility(View.GONE);
				}
				if(mode == FORCE_HIDE_LABEL_AND_ICON){
					icon.setVisibility(View.GONE);
					label.setVisibility(View.GONE);
				}
				if(mode == FORCE_HIDE_LABEL){
					label.setVisibility(View.GONE);
				}
			}
		}else{
			label.setTextSize(text_size);
			label.setTextColor(text_color);
			label.setShadowLayer(3f, 0, 0, 0xffb3e15b);
			label.setText(filtered.get(arg0).label);
		}
		return lay;
	}
	public class filtered_item{
		public String label = "";
		public int id = 0;
	}
}
