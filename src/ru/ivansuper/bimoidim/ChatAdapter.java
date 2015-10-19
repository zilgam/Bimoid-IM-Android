package ru.ivansuper.bimoidim;

import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.HistoryItem;
import ru.ivansuper.bimoidproto.filetransfer.FileReceiver;
import ru.ivansuper.bimoidproto.filetransfer.FileReceiver.view_container;
import ru.ivansuper.bimoidproto.filetransfer.FileSender;
import ru.ivansuper.bimoidproto.filetransfer.FileTransfer;
import ru.ivansuper.ui.MyTextView;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatAdapter extends BaseAdapter {
	private Vector<HistoryItem> history;
	private View last_transfer_panel;
	private Context context;
	public ChatAdapter(Context context, Vector<HistoryItem> history){
		this.context = context;
		this.history = history;
		notifyAdapter();
	}
	public void forceSetSource(Vector<HistoryItem> source){
		history = source;
		notifyAdapter();
	}
	public void notifyAdapter(){
		notifyDataSetChanged();
	}
	public void put(HistoryItem hst){
		history.add(hst);
		notifyAdapter();
	}
	@Override
	public int getCount() {
		return history.size();
	}
	@Override
	public HistoryItem getItem(int position) {
		return history.get(position);
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout item;
		HistoryItem hst = getItem(position);
		if(convertView == null){
			item = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.chat_item, null);
		}else{
			if(hst.isReceiverAttached() || hst.isSenderAttached()){
				item = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.chat_item, null);
			}else{
				if(last_transfer_panel == null){
					item = (LinearLayout)convertView;
				}else{
					if(convertView.equals(last_transfer_panel)){
						item = (LinearLayout)LayoutInflater.from(resources.ctx).inflate(R.layout.chat_item, null);
					}else{
						item = (LinearLayout)convertView;
					}
				}
			}
		}
		ImageView icon = (ImageView)item.findViewById(R.id.chat_item_icon);
		TextView nick = (TextView)item.findViewById(R.id.chat_item_nick);
		TextView date = (TextView)item.findViewById(R.id.chat_item_date);
		MyTextView text = (MyTextView)item.findViewById(R.id.chat_item_text);
		text.setFocusable(false);
		if(ColorScheme.initialized) nick.setShadowLayer(1f, 1f, 1f, ColorScheme.getColor(37));
		if(ColorScheme.initialized) date.setShadowLayer(1f, 1f, 1f, ColorScheme.getColor(37));
		LinearLayout transfer_buttons = (LinearLayout)item.findViewById(R.id.chat_item_transfer);
		PB transfer_progress = (PB)item.findViewById(R.id.chat_item_transfer_progress);
		transfer_buttons.setVisibility(View.GONE);
		Button transfer_accept = (Button)item.findViewById(R.id.chat_item_accept_btn);
		Interface.attachButtonStyle(transfer_accept);
		//transfer_accept.setVisibility(View.GONE);
		Button transfer_decline = (Button)item.findViewById(R.id.chat_item_decline_btn);
		Interface.attachButtonStyle(transfer_decline);
		date.setText(hst.formattedDate);
		if(ColorScheme.initialized) date.setTextColor(ColorScheme.getColor(20));
		if(hst.span_message == null){
			hst.span_message = MyTextView.detectLinks(hst.message);
			hst.span_message = SmileysManager.getSmiledText((SpannableStringBuilder)hst.span_message);
		}
		text.setText(hst.span_message);
		if(ColorScheme.initialized) text.setLinkTextColor(ColorScheme.getColor(21));
		text.setTextSize(14);
		switch(hst.direction){
		case HistoryItem.DIRECTION_INCOMING:
			nick.setText(hst.contact.getName());
			icon.setImageDrawable(resources.msg_in);
			text.setTextColor(0xffb5ffb5);
			if(hst.isReceiverAttached()){
				transfer_buttons.setVisibility(View.VISIBLE);
				icon.setImageDrawable(resources.res.getDrawable(R.drawable.file));
				final FileReceiver receiver = (FileReceiver)hst.getAttachedTransfer();
				view_container container = receiver.getContainer();
					container.setText(text);
					container.setProgress(transfer_progress);
					container.setButtons(transfer_buttons);
					container.setAccept(transfer_accept);
					container.setDecline(transfer_decline);
					transfer_accept.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							receiver.getContact().getProfile().acceptFile(receiver.getUniqueId(), receiver.getContact().getID());
						}
					});
					transfer_decline.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							receiver.cancel();
							receiver.getContact().getProfile().sendFTControl(receiver.getUniqueId(), receiver.getContact().getID(), FileTransfer.FT_CONTROL_CODE_CANCEL);
						}
					});
				container.updateViews();
				last_transfer_panel = item;
			}
			if(hst.isSenderAttached()){
				transfer_buttons.setVisibility(View.VISIBLE);
				transfer_accept.setVisibility(View.GONE);
				icon.setImageDrawable(resources.res.getDrawable(R.drawable.file));
				final FileSender sender = (FileSender)hst.getAttachedTransfer();
				transfer_decline.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						sender.cancel();
						sender.getContact().getProfile().sendFTControl(sender.getUniqueId(), sender.getContact().getID(), FileTransfer.FT_CONTROL_CODE_CANCEL);
					}
				});
				ru.ivansuper.bimoidproto.filetransfer.FileSender.view_container container = sender.getContainer();
				container.setText(text);
				container.setProgress(transfer_progress);
				container.setButtons(transfer_buttons);
				container.setAccept(transfer_accept);
				container.setDecline(transfer_decline);
				transfer_decline.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						sender.cancel();
						sender.getContact().getProfile().sendFTControl(sender.getUniqueId(), sender.getContact().getID(), FileTransfer.FT_CONTROL_CODE_CANCEL);
					}
				});
				container.updateViews();
				last_transfer_panel = item;
			}
			if(ColorScheme.initialized) text.setTextColor(ColorScheme.getColor(18));
			if(ColorScheme.initialized) nick.setTextColor(ColorScheme.getColor(16));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(32));
			break;
		case HistoryItem.DIRECTION_OUTGOING:
			nick.setText(hst.contact.getProfile().nickname);
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
			//item.setBackgroundDrawable(resources.res.getDrawable(R.drawable.auth_acc_item_normal));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(34));
			break;
		case Contact.AUTH_REJECTED:
			icon.setImageDrawable(resources.res.getDrawable(R.drawable.auth_rej));
			//item.setBackgroundDrawable(resources.res.getDrawable(R.drawable.auth_rej_item_normal));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(35));
			break;
		case Contact.AUTH_REQ:
			icon.setImageDrawable(resources.res.getDrawable(R.drawable.auth_req));
			//item.setBackgroundDrawable(resources.res.getDrawable(R.drawable.auth_req_item_normal));
			if(ColorScheme.initialized) item.setBackgroundColor(ColorScheme.getColor(36));
			break;
		default:
			//item.setBackgroundDrawable(resources.res.getDrawable(R.drawable.item_normal));
			break;
		}
		text.relayout();
		item.setPadding(0, 0, 0, 0);
		item.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		return item;
	}
}
