package ru.ivansuper.bimoidproto.filetransfer;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Vector;

import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class files_adapter extends BaseAdapter {
	private Vector<File> dirs = new Vector<File>();
	private Vector<File> files = new Vector<File>();
	private Vector<item> list = new Vector<item>();
	private Vector<File> selected = new Vector<File>();
	private Object locker = new Object();
	private ThreadGroup tg = new ThreadGroup("ImageLoaders");
	public File parent;
	public long timestamp = 0;
	public void setData(File[] list, File parent){
		this.parent = parent;
		timestamp = System.currentTimeMillis();
		final Vector<File> temp_list = new Vector<File>();
		this.list.clear();
		try{
			for(File file: list)
				temp_list.add(file);
			this.list.add(new item(parent));
			for(int i=0; i<list.length; i++){
				if(list[i].isDirectory()){
					dirs.add(list[i]);
				}else{
					files.add(list[i]);
				}
			}
			Collections.sort(dirs);
			Collections.sort(files);
			for(File file: dirs)
				this.list.add(new item(file));
			for(File file: files)
				this.list.add(new item(file));
			dirs.clear();
			files.clear();
		}catch(Exception e){
			dirs.clear();
			files.clear();
			this.list.clear();
			this.list.add(new item(parent));
		}
		notifyDataSetChanged();
	}
	public void toggleSelection(int position){
		item it = list.get(position);
		it.selected = !it.selected;
		if(it.selected){
			selected.addElement(it.file);
		}else{
			selected.removeElement(it.file);
		}
		notifyDataSetChanged();
	}
	public String getSelected(){
		String res = "";
		for(File file: selected){
			res += file.getAbsolutePath()+"////";
		}
		if(res.length() != 0)
			res = res.substring(0, res.length()-4);
		return res;
	}
	@Override
	public int getCount() {
		return list.size();
	}
	@Override
	public File getItem(int position) {
		return list.get(position).file;
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LinearLayout lay = null;
		if(convertView == null){
			lay = (LinearLayout)View.inflate(resources.ctx, R.layout.file_browser_item, null);
		}else{
			lay = (LinearLayout)convertView;
		}
		final TextView label = (TextView)lay.findViewById(R.id.file_item_label);
		final TextView size = (TextView)lay.findViewById(R.id.file_item_size);
		final ImageView selected = (ImageView)lay.findViewById(R.id.file_item_check);
		ImageView icon_ = (ImageView)lay.findViewById(R.id.file_item_icon);
		size.setVisibility(View.INVISIBLE);
		selected.setVisibility(View.GONE);
		final File item = getItem(position);
		list.get(position).selected = this.selected.contains(item);
		if(position == 0){
			icon_.setImageDrawable(resources.res.getDrawable(R.drawable.browser_back));
			label.setText("..");
		}else{
			if(item.isDirectory()){
				icon_.setImageDrawable(resources.res.getDrawable(R.drawable.directory));
				label.setText(item.getName());
			}else{
				if(list.get(position).selected){
					selected.setVisibility(View.VISIBLE);
				}else{
					selected.setVisibility(View.GONE);
				}
				if(itIsImage(item)){
					Drawable icon = list.get(position).icon;
					if(icon == null){
						icon_.setImageDrawable(resources.res.getDrawable(R.drawable.image_file));
					}else{
						icon_.setImageDrawable(list.get(position).icon);
					}
					if((icon == null) && (FileBrowserActivity.LISTVIEW_STATE == OnScrollListener.SCROLL_STATE_IDLE)){
						//icons.set(position, resources.res.getDrawable(R.drawable.file_browsing));
						Runnable rr = new Runnable(){
							final long stamp = timestamp;
							@Override
							public void run(){
								label:{
								Thread.currentThread().setPriority(7);
								synchronized(locker){
									if(stamp != timestamp) break label;
									BitmapFactory.Options opts = new BitmapFactory.Options();
									opts.inDither = true;
									//opts.inPreferQualityOverSpeed = false;
									opts.inPreferredConfig = Bitmap.Config.RGB_565;
									//opts.inTempStorage = decode_buffer;
									opts.inJustDecodeBounds = true;
									Bitmap bmpA = BitmapFactory.decodeFile(item.getAbsolutePath(), opts);
									if(opts.outWidth == -1) return;
									opts.inSampleSize = Math.max(opts.outWidth, opts.outHeight)/128;
									opts.inJustDecodeBounds = false;
									bmpA = BitmapFactory.decodeFile(item.getAbsolutePath(), opts);
									if(bmpA == null) return;
									//Bitmap bmpA = Bitmap.createScaledBitmap(bmp, 48, 48, true);
									//bmp.recycle();
									//bmp = null;
									if(stamp != timestamp) break label;
									Drawable preview = new BitmapDrawable(bmpA);
									//preview.setBounds(0, 0, bmpA.getWidth(), bmpA.getHeight());
									if(stamp != timestamp) break label;
									list.get(position).icon = preview;
									if(stamp != timestamp) break label;
									Runnable r = new Runnable(){
										@Override
										public void run() {
											notifyDataSetChanged();
										}
									};
									label.post(r);
									//locker.notify();
								}
								}
							}
						};
						Thread t = new Thread(tg, rr);
						t.setName("ImageLoader");
						t.start();
					}
				}else{
					icon_.setImageDrawable(resources.res.getDrawable(R.drawable.file_browsing));
				}
				label.setText(item.getName());
				size.setVisibility(View.VISIBLE);
				size.setText(getSizeLabel(item.length()));
			}
		}
		return lay;
	}
	private String getSizeLabel(long size){
		String res = "[]";
		double sz = size;
		if(sz < 1024){
			res = "["+String.valueOf(sz)+" b]";
		}else if((size >= 1024) && (size < 1048576)){
			sz = sz/1024;
			sz = new BigDecimal(sz).setScale(2, RoundingMode.UP).doubleValue();
			res = "["+String.valueOf(sz)+" KB]";
		}else if(size >= 1048576){
			sz = sz/1024/1024;
			sz = new BigDecimal(sz).setScale(2, RoundingMode.UP).doubleValue();
			res = "["+String.valueOf(sz)+" MB]";
		}
		return res;
	}
	private boolean itIsImage(File file){
		String file_name = file.getAbsolutePath();
		if(file_name.toLowerCase().endsWith(".gif")){
			return true;
		}else if(file_name.toLowerCase().endsWith(".jpg")){
			return true;
		}else if(file_name.toLowerCase().endsWith(".jpeg")){
			return true;
		}else if(file_name.toLowerCase().endsWith(".bmp")){
			return true;
		}else if(file_name.toLowerCase().endsWith(".png")){
			return true;
		}
		return false;
	}
	private class item implements Comparable<File> {
		public File file;
		public boolean selected;
		public Drawable icon;
		public item(File file){
			this.file = file;
		}
		@Override
		public int compareTo(File arg0) {
			return file.compareTo(arg0);
		}
	}
}
