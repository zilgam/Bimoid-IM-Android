package ru.ivansuper.bimoidim;

import java.util.ArrayList;
import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.HistoryItemA;
import ru.ivansuper.bimoidproto.filetransfer.FileReceiver;
import ru.ivansuper.bimoidproto.filetransfer.FileReceiver.view_container;
import ru.ivansuper.bimoidproto.filetransfer.FileSender;
import ru.ivansuper.bimoidproto.filetransfer.FileTransfer;
import ru.ivansuper.ui.MyTextView;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryAdapter extends BaseAdapter {
	private ArrayList<HistoryItemA> history;
	public ArrayList<HistoryItemA> filtered_list = new ArrayList<HistoryItemA>();
	private boolean filtered;
	private String pattern;
	public HistoryAdapter(){
		this.history = new ArrayList<HistoryItemA>();
		notifyAdapter();
	}
	public void forceSetSource(ArrayList<HistoryItemA> source){
		history = source;
		notifyAdapter();
	}
	public void setFilter(String pattern){
		this.pattern = pattern;
		if(pattern == null){
			filtered = false;
			notifyDataSetChanged();
			return;
		}
		if(pattern.length() == 0){
			filtered = false;
			notifyDataSetChanged();
			return;
		}
		this.pattern = this.pattern.toLowerCase();
		filtered = true;
		doFilter();
		notifyDataSetChanged();
	}
	private void doFilter(){
		filtered_list.clear();
		for(int i=0; i<history.size(); i++){
			HistoryItemA item = history.get(i);
			String a = "";
			if(item.message != null) a = item.message.toLowerCase();
			if(a.contains(pattern)) filtered_list.add(item);
		}
	}
	@Override
	public HistoryItemA getItem(int arg0) {
		if(filtered){
			return filtered_list.get(arg0);
		}else{
			return history.get(arg0);
		}
	}	public void notifyAdapter(){
		notifyDataSetChanged();
	}
	public void put(HistoryItemA hst){
		history.add(hst);
	}
	@Override
	public int getCount() {
		if(filtered){
			return filtered_list.size();
		}else{
			return history.size();
		}
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout item;
		HistoryItemA hst = getItem(position);
		if(convertView == null){
			item = (LinearLayout)LayoutInflater.from(resources.ctx).inflate(R.layout.chat_item, null);
		}else{
			item = (LinearLayout)convertView;
		}
		ImageView icon = (ImageView)item.findViewById(R.id.chat_item_icon);
		TextView nick = (TextView)item.findViewById(R.id.chat_item_nick);
		TextView date = (TextView)item.findViewById(R.id.chat_item_date);
		MyTextView text = (MyTextView)item.findViewById(R.id.chat_item_text);
		text.setTextSize(14);
		LinearLayout transfer_buttons = (LinearLayout)item.findViewById(R.id.chat_item_transfer);
		transfer_buttons.setVisibility(View.GONE);
		date.setText(hst.formattedDate);
		if(ColorScheme.initialized) date.setTextColor(ColorScheme.getColor(20));
		text.setText(hst.message);
		text.selectMatches(pattern);
		if(ColorScheme.initialized) text.setLinkTextColor(ColorScheme.getColor(21));
		switch(hst.direction){
		case HistoryItemA.DIRECTION_INCOMING:
			nick.setText(hst.contact);
			icon.setImageDrawable(resources.msg_in);
			text.setTextColor(0xffb5ffb5);
			if(ColorScheme.initialized) text.setTextColor(ColorScheme.getColor(18));
			if(ColorScheme.initialized) nick.setTextColor(ColorScheme.getColor(16));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(32));
			break;
		case HistoryItemA.DIRECTION_OUTGOING:
			nick.setText(hst.profile);
			if(hst.confirmed()){
				icon.setImageDrawable(resources.msg_out_c);
			}else{
				icon.setImageDrawable(resources.msg_out);
			}
			text.setTextColor(0xffffffff);
			if(ColorScheme.initialized) text.setTextColor(ColorScheme.getColor(19));
			if(ColorScheme.initialized) nick.setTextColor(ColorScheme.getColor(17));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(33));
			break;
		}
		switch(hst.isAuthMessage){
		case Contact.AUTH_ACCEPTED:
			icon.setImageDrawable(resources.res.getDrawable(R.drawable.auth_acc));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(34));
			break;
		case Contact.AUTH_REJECTED:
			icon.setImageDrawable(resources.res.getDrawable(R.drawable.auth_rej));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(35));
			break;
		case Contact.AUTH_REQ:
			icon.setImageDrawable(resources.res.getDrawable(R.drawable.auth_req));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(36));
			break;
		}
		text.relayout();
		item.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		return item;
	}
}
