package ru.ivansuper.bimoidim;

import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class smileys_adapter extends BaseAdapter {
	private Vector<String> tags = new Vector<String>();
	private Vector<Drawable> smileys = new Vector<Drawable>();
	public smileys_adapter(){
		tags = SmileysManager.selector_tags;
		smileys = SmileysManager.selector_smileys;
		notifyDataSetInvalidated();
	}
	@Override
	public int getCount() {
		return smileys.size();
	}
	@Override
	public Drawable getItem(int position) {
		return smileys.get(position);
	}
	public String getTag(int position) {
		return tags.get(position);
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout lay;
		if(convertView == null){
			lay = (LinearLayout)View.inflate(resources.ctx, R.layout.smile_item, null);
		}else{
			lay = (LinearLayout)convertView;
		}
		lay.setMinimumHeight(SmileysManager.max_height+4);
		TextView txt = (TextView)lay.findViewById(R.id.smile_item);
		SpannableStringBuilder ssb = new SpannableStringBuilder(" ");
		ssb.setSpan(new ImageSpan(getItem(position)), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		txt.setText(ssb);
		txt.setSingleLine(true);
		return lay;
	}
}
