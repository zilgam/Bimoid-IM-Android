package ru.ivansuper.bimoidim;

import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.RosterItem;
import ru.ivansuper.bservice.BimoidService;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IgnoreAdapter extends BaseAdapter {
	private BimoidService service;
	private Vector<RosterItem> builded = new Vector<RosterItem>();
	private Vector<RosterItem> display = new Vector<RosterItem>();
	public IgnoreAdapter(BimoidService svc){
		service = svc;
	}
	public void fill(String ID){
		ProfilesManager profiles = service.profiles;
		if(profiles == null) return;
		builded.clear();
		for(int i=0; i<profiles.list.size(); i++){
			BimoidProfile profile = profiles.list.get(i);
			if(profile == null) continue;
			if(!profile.ID.equals(ID)) continue;
			for(int j=0; j<profile.contacts.size(); j++){
				RosterItem item = profile.contacts.get(j);
				if(item == null) continue;
				if(item.type == RosterItem.OBIMP_CONTACT){
					if(((Contact)item).getPrivacy() == Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST)
						builded.addElement(profile.contacts.get(j));
				}
			}
		}
		display.clear();
		display.addAll(builded);
		notifyDataSetChanged();
	}
	@Override
	public int getCount() {
		return display.size();
	}
	@Override
	public RosterItem getItem(int pos) {
		return display.get(pos);
	}
	@Override
	public long getItemId(int pos) {
		return pos;
	}
	@Override
	public View getView(int pos, View view, ViewGroup v_group) {
		LinearLayout item;
		if(view == null){
			item = (LinearLayout)LayoutInflater.from(resources.ctx).inflate(R.layout.contactlist_item, null);
		}else{
			item = (LinearLayout)view;
		}
		item.setPadding(10, 25, 10, 25);
		ImageView tsts = (ImageView)item.findViewById(R.id.contactlist_item_tsts);
		tsts.setVisibility(View.GONE);
		ImageView tasts = (ImageView)item.findViewById(R.id.contactlist_item_tasts);
		tasts.setVisibility(View.GONE);
		ImageView msg = (ImageView)item.findViewById(R.id.contactlist_item_msg);
		msg.setVisibility(View.GONE);
		msg.clearAnimation();
		ImageView sts = (ImageView)item.findViewById(R.id.contactlist_item_sts);
		sts.setPadding(3, 0, 3, 0);
		//sts.setVisibility(View.GONE);
		ImageView ests = (ImageView)item.findViewById(R.id.contactlist_item_ests);
		ests.setVisibility(View.GONE);
		TextView label = (TextView)item.findViewById(R.id.contactlist_item_label);
		label.setGravity(Gravity.LEFT);
		TextView additional_status = (TextView)item.findViewById(R.id.contactlist_item_addit_desc);
		additional_status.setVisibility(View.GONE);
		LinearLayout group_line_1 = (LinearLayout)item.findViewById(R.id.contact_list_item_group_line_1);
		LinearLayout group_line_2 = (LinearLayout)item.findViewById(R.id.contact_list_item_group_line_2);
		group_line_1.setVisibility(View.GONE);
		group_line_2.setVisibility(View.GONE);
    	if(ColorScheme.initialized){
    		group_line_1.setBackgroundColor(ColorScheme.getColor(6));
    		group_line_2.setBackgroundColor(ColorScheme.getColor(6));
    	}
		LinearLayout flags = (LinearLayout)item.findViewById(R.id.contactlist_item_flags);
		flags.removeAllViews();
		RosterItem i = getItem(pos);
		switch(i.type){
		case RosterItem.OBIMP_CONTACT:
			Contact contact = (Contact)i;
			label.setText(contact.getName());
			label.setShadowLayer(3f, 1f, 1f, 0xff000000);
	    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(12));
			sts.setImageDrawable(resources.getMainStatusIcon(contact.getStatus()));
			ImageView vis = new ImageView(resources.ctx);
			vis.setImageDrawable(resources.res.getDrawable(R.drawable.contact_ignore));
			flags.addView(vis);
			sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_in_ignore));
			break;
		}
		return item;
	}
}
