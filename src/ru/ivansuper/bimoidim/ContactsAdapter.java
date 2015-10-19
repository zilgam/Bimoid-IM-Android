package ru.ivansuper.bimoidim;

import java.util.Collections;
import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.bimoidproto.Group;
import ru.ivansuper.bimoidproto.GroupInfo;
import ru.ivansuper.bimoidproto.NoteItem;
import ru.ivansuper.bimoidproto.PresenceDump;
import ru.ivansuper.bimoidproto.RosterItem;
import ru.ivansuper.bimoidproto.SuperGroup;
import ru.ivansuper.bimoidproto.transports.Transport;
import ru.ivansuper.bservice.BimoidService;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContactsAdapter extends BaseAdapter {
	private BimoidService service;
	private Vector<RosterItem> builded = new Vector<RosterItem>();
	private Vector<RosterItem> display = new Vector<RosterItem>();
	public static final Object LOCKER = new Object();
	public ContactsAdapter(BimoidService svc){
		service = svc;
	}
	public void build(){
		synchronized(LOCKER){
			if(PreferenceTable.show_groups){
				fill_with_groups();
			}else{
				fill_without_groups();
			}
		}
	}
	public void fill_without_groups(){
		ProfilesManager profiles = service.profiles;
		if(profiles == null) return;
		builded.clear();
		for(int i=0; i<profiles.list.size(); i++){
			BimoidProfile profile = profiles.list.get(i);
			if(profile == null) continue;
			SuperGroup sgroup = new SuperGroup(profile.nickname, RosterItem.PROFILE_GROUP);
			sgroup.profile = profile;
			sgroup.opened = profile.expanded_in_contact_list;
			builded.add(sgroup);
			int group_idx = builded.size();
			int online_idx = builded.size();
			int offline_idx = builded.size();
			int notes_idx = builded.size();
			int transport_idx = builded.size();
			if(!sgroup.opened) continue;
			Vector<RosterItem> contacts = profile.getContacts();
			for(int j=0; j<contacts.size(); j++){
				RosterItem item = contacts.get(j);
				if(item == null) continue;
				
				if(item.type == RosterItem.OBIMP_CONTACT){
					if(((Contact)item).getPrivacy() == Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST) continue;
					
					if(((Contact)item).hasUnreadMessages()){
						builded.insertElementAt(item, group_idx);
						group_idx++;
						online_idx++;
						offline_idx++;
						transport_idx++;
						notes_idx++;
					}else{
						if(((Contact)item).getStatus() < 0){
							if(PreferenceTable.show_offline){
								builded.insertElementAt(contacts.get(j), offline_idx);
								offline_idx++;
								transport_idx++;
								notes_idx++;
							}
						}else{
							builded.insertElementAt(item, online_idx);
							online_idx++;
							offline_idx++;
							transport_idx++;
							notes_idx++;
						}
					}
				}
				if(item.type == RosterItem.TRANSPORT_ITEM){
					builded.insertElementAt(item, transport_idx);
					transport_idx++;
					notes_idx++;
				}
				if(item.type == RosterItem.CL_ITEM_TYPE_NOTE){
					builded.insertElementAt(item, notes_idx);
					notes_idx++;
				}
			}
		}
		display.clear();
		display.addAll(builded);
		notifyDataSetChanged();
	}
	public void fill_with_groups(){
		int level = 0;
		int current_level = -1;
		int group_idx = 0;
		int online_idx = 0;
		ProfilesManager profiles = service.profiles;
		if(profiles == null) return;
		builded.clear();
		for(int i=0; i<profiles.list.size(); i++){
			boolean skip = false;
			BimoidProfile profile = profiles.list.get(i);
			if(profile == null) continue;
			SuperGroup sgroup = new SuperGroup(profile.nickname, RosterItem.PROFILE_GROUP);
			sgroup.profile = profile;
			sgroup.opened = profile.expanded_in_contact_list;
			builded.add(sgroup);
			online_idx = builded.size();
			group_idx = builded.size();
			if(!sgroup.opened) continue;
			final Vector<RosterItem> contacts = profile.contacts;
			for(int j=0; j<contacts.size(); j++){
				RosterItem item = contacts.get(j);
				if(item == null) continue;
				if(!skip)
				if(current_level != item.level){
					group_idx = builded.size();
					online_idx = builded.size();
				}
				current_level = item.level;
				if((item.level <= level) && skip){
					skip = false;
				}
				GroupInfo info = null;
				if(item.type == RosterItem.OBIMP_GROUP){
					info = new GroupInfo();
					item.profile.getGroupPresenceInfo(item, info);
				}
				if(!skip){
					if(item.type == RosterItem.OBIMP_GROUP){
						if(((Group)item).isEmptyForDisplay() && PreferenceTable.hide_empty_groups)
							continue;
						builded.add(contacts.get(j));
						group_idx++;
						online_idx++;
					}else if(item.type == RosterItem.OBIMP_CONTACT){
						if(((Contact)item).getPrivacy() == Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST) continue;
						if(((Contact)item).hasUnreadMessages()){
							builded.insertElementAt(contacts.get(j), group_idx);
							group_idx++;
							online_idx++;
						}else{
							if(((Contact)item).getStatus() < 0){
								if(PreferenceTable.show_offline)
									builded.add(contacts.get(j));
							}else{
								builded.insertElementAt(contacts.get(j), online_idx);
								online_idx++;
							}
						}
					}else if(item.type == RosterItem.TRANSPORT_ITEM){
						builded.insertElementAt(contacts.get(j), online_idx);
						online_idx++;
					}else if(item.type == RosterItem.CL_ITEM_TYPE_NOTE){
						builded.insertElementAt(contacts.get(j), online_idx);
						online_idx++;
					}
				}
				if(item.type == RosterItem.OBIMP_GROUP){
					Group grp = (Group)item;
					if(!grp.opened && !skip){
						level = grp.level;
						skip = true;
					}
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
	private AlphaAnimation getAnimation(){
		AlphaAnimation aa = new AlphaAnimation(0f, 1f);
		aa.setDuration(750);
		aa.setRepeatCount(-1);
		return aa;
	}
	@Override
	public View getView(int pos, View view, ViewGroup v_group) {
			LinearLayout item;
			if(view == null){
				item = (LinearLayout)LayoutInflater.from(resources.ctx).inflate(R.layout.contactlist_item, null);
			}else{
				item = (LinearLayout)view;
			}
			ImageView msg = (ImageView)item.findViewById(R.id.contactlist_item_msg);
			msg.setVisibility(View.GONE);
			msg.clearAnimation();
			ImageView sts = (ImageView)item.findViewById(R.id.contactlist_item_sts);
			sts.setPadding(3, 0, 3, 0);
			ImageView ests = (ImageView)item.findViewById(R.id.contactlist_item_ests);
			ests.setVisibility(View.GONE);
			ImageView tsts = (ImageView)item.findViewById(R.id.contactlist_item_tsts);
			tsts.setVisibility(View.GONE);
			ImageView tests = (ImageView)item.findViewById(R.id.contactlist_item_tasts);
			tests.setVisibility(View.GONE);
			TextView label = (TextView)item.findViewById(R.id.contactlist_item_label);
			label.setGravity(Gravity.LEFT);
			TextView additional_status = (TextView)item.findViewById(R.id.contactlist_item_addit_desc);
			additional_status.setVisibility(View.GONE);
			
			final TextView mail_box_unreaded_label = (TextView)item.findViewById(R.id.contact_item_mail_box_unreaded);
			mail_box_unreaded_label.setVisibility(View.GONE);
			
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
				if(ColorScheme.initialized) label.setShadowLayer(1f, 1f, 1f, ColorScheme.getColor(37));
				if(contact.hasUnreadMessages()){
					if(contact.haveAuth()){
						switch(contact.getAuth()){
						case Contact.AUTH_ACCEPTED:
							msg.setImageDrawable(resources.res.getDrawable(R.drawable.auth_acc));
							break;
						case Contact.AUTH_REJECTED:
							msg.setImageDrawable(resources.res.getDrawable(R.drawable.auth_rej));
							break;
						case Contact.AUTH_REQ:
							msg.setImageDrawable(resources.res.getDrawable(R.drawable.auth_req));
							break;
						}
						msg.setVisibility(View.VISIBLE);
						msg.setAnimation(getAnimation());
					}else{
						if(contact.haveIncFile()){
							msg.setImageDrawable(resources.res.getDrawable(R.drawable.file));
							msg.setVisibility(View.VISIBLE);
							msg.setAnimation(getAnimation());
						}else{
							msg.setImageDrawable(resources.res.getDrawable(R.drawable.msg_in_0));
							msg.setVisibility(View.VISIBLE);
							msg.setAnimation(getAnimation());
						}
					}
				}
				if(contact.getTyping()){
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.contact_typing));
					label.setTextColor(0xffeeeeee);
			    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(10));
				}else{
					if(contact.isTemporary()){
						label.setTextColor(0xffaaaaaa);
						if(contact.getTransportId() != -1){
							try{
								sts.setImageDrawable(contact.getProfile().getTransportByID(contact.getTransportId()).params.getUndetermined());
							}catch(Exception e){
								sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_not_in_list));
							}
						}else{
							sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_not_in_list));
						}
					}else{
						if(contact.getTransportId() == -1){
							sts.setImageDrawable(resources.getMainStatusIcon(contact.getStatus()));
						}
						if(contact.getStatus() != -1){
							label.setTextColor(0xff99ee99);
					    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(8));
							if(contact.getTransportId() != -1){
								try{
									sts.setImageDrawable(contact.getProfile().getTransportByID(contact.getTransportId()).params.getStatus(contact.getStatus()));
								}catch(Exception e){
									sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_in_ignore));
								}
							}
						}else{
							label.setTextColor(0xffaa0000);
					    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(9));
							if(contact.getTransportId() != -1){
								try{
									sts.setImageDrawable(contact.getProfile().getTransportByID(contact.getTransportId()).params.getOffline());
								}catch(Exception e){
									sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_in_ignore));
								}
							}
						}
					}
				}
				if(contact.getExtendedStatus() > 0 && contact.getStatus() != -1){
					ests.setVisibility(View.VISIBLE);
					if(contact.getTransportId() != -1){
						try{
							ests.setImageDrawable(contact.getProfile().getTransportByID(contact.getTransportId()).params.getAddStatus(contact.getExtendedStatus()));
						}catch(Exception e){
							ests.setImageDrawable(XStatus.getIcon(0));
						}
					}else{
						ests.setImageDrawable(XStatus.getIcon(contact.getExtendedStatus()));
					}
				}
				if(contact.haveExtendedStatusDescription() && PreferenceTable.addit_desc_under_nick && contact.getStatus() != -1){
					additional_status.setVisibility(View.VISIBLE);
					additional_status.setText(contact.getExtendedDescription());
			    	if(ColorScheme.initialized) additional_status.setTextColor(ColorScheme.getColor(11));
				}
				switch(contact.getPrivacy()){
				case Contact.CL_PRIV_TYPE_VISIBLE_LIST:
					ImageView vis = new ImageView(resources.ctx);
					vis.setImageDrawable(resources.res.getDrawable(R.drawable.contact_vis));
					flags.addView(vis);
					break;
				case Contact.CL_PRIV_TYPE_INVISIBLE_LIST:
					vis = new ImageView(resources.ctx);
					vis.setImageDrawable(resources.res.getDrawable(R.drawable.contact_invis));
					flags.addView(vis);
					break;
				case Contact.CL_PRIV_TYPE_IGNORE_LIST:
					vis = new ImageView(resources.ctx);
					vis.setImageDrawable(resources.res.getDrawable(R.drawable.contact_ignore));
					flags.addView(vis);
					break;
				case Contact.CL_PRIV_TYPE_IGNORE_NOT_IN_LIST:
					vis = new ImageView(resources.ctx);
					vis.setImageDrawable(resources.res.getDrawable(R.drawable.contact_ignore));
					flags.addView(vis);
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_in_ignore));
					break;
				}
				if(contact.auth_flag){
					ImageView auth = new ImageView(resources.ctx);
					auth.setImageDrawable(resources.res.getDrawable(R.drawable.auth_flag));
					flags.addView(auth);
				}
				break;
			case RosterItem.TRANSPORT_ITEM:
				Transport transport = (Transport)i;
				label.setText(transport.getName());
				if(ColorScheme.initialized) label.setShadowLayer(1f, 1f, 1f, ColorScheme.getColor(37));
				label.setTextColor(0xff99ee99);
		    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(8));
		    	if(transport.ready){
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.transport_item));
		    	}else{
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.transport_item_not_ready));
		    	}
		    	tsts.setVisibility(View.VISIBLE);
		    	tests.setVisibility(View.VISIBLE);
		    	mail_box_unreaded_label.setVisibility(transport.isMailBoxHasUnreaded()? View.VISIBLE: View.GONE);
		    	
		    	tsts.setImageDrawable(transport.params.getStatus(transport.status));
		    	tests.setImageDrawable(transport.params.additional_status_pic? transport.params.getAddStatus(transport.extended_status): null);
		    	
		    	mail_box_unreaded_label.setText(String.valueOf(transport.getMailBoxUnreadedCount()));
				break;
			case RosterItem.OBIMP_GROUP:
				Group group = (Group)i;
				sts.setPadding(10, 0, 3, 0);
				if(group.opened){
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.group_opened));
				}else{
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.group_closed));
				}
				label.setText(group.getName());
				if(ColorScheme.initialized) label.setShadowLayer(1f, 1f, 1f, ColorScheme.getColor(37));
				label.setTextColor(0xffffffff);
		    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(7));
				break;
			case RosterItem.PROFILE_GROUP:
				SuperGroup sgroup = (SuperGroup)i;
				sts.setPadding(10, 0, 3, 0);
				if(sgroup.opened){
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.group_opened));
				}else{
					sts.setImageDrawable(resources.res.getDrawable(R.drawable.group_closed));
				}
				PresenceDump dump = new PresenceDump();
				sgroup.profile.getPresenceDump(dump);
				label.setText(sgroup.getName()+" ("+String.valueOf(dump.online)+"/"+String.valueOf(dump.total)+")");
				if(ColorScheme.initialized) label.setShadowLayer(1f, 1f, 1f, ColorScheme.getColor(37));
				label.setTextColor(0xffffffff);
		    	if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(5));
				label.setGravity(Gravity.CENTER);
				group_line_1.setVisibility(View.VISIBLE);
				group_line_2.setVisibility(View.VISIBLE);
				break;
			case RosterItem.CL_ITEM_TYPE_NOTE:
				NoteItem note_item = (NoteItem)i;
				sts.setImageDrawable(note_item.getIcon());
				label.setText(note_item.name);
				label.setTextColor(0xffffffff);
				if(ColorScheme.initialized) label.setTextColor(ColorScheme.getColor(38));
				if(note_item.TEXT.trim().length()>0 && note_item.TYPE != NoteItem.CL_NOTE_TYPE_COMMAND && PreferenceTable.addit_desc_under_nick){
					additional_status.setVisibility(View.VISIBLE);
					additional_status.setText(TextUtils.ellipsize(note_item.TEXT, additional_status.getPaint(), resources.dm.widthPixels*0.8f, TruncateAt.END));
			    	if(ColorScheme.initialized) additional_status.setTextColor(ColorScheme.getColor(39));
				}
				break;
			}
			if(PreferenceTable.show_groups){
				item.setPadding(i.level*(int)(16*resources.dm.density), 8, 0, 8);
			}else{
				item.setPadding(8, 8, 0, 8);
			}
			return item;
	}
}
