package ru.ivansuper.bimoidproto;

import java.util.Vector;

import ru.ivansuper.bimoidim.PreferenceTable;

public class Group extends RosterItem {
	public boolean opened = true;
	public Group(String name, int id, int parent_id, BimoidProfile profile){
		this.name = name;
		this.item_id = id;
		this.group_id = parent_id;
		this.profile = profile;
		type = RosterItem.OBIMP_GROUP;
	}
	public BimoidProfile getProfile(){
		return profile;
	}
	public void update(RosterItem item){
		try{
			Group g = (Group)item;
			this.name = g.name;
			this.group_id = g.group_id;
		}catch(Exception e){}
	}
	public final boolean isEmptyForDisplay(){
		final Vector<RosterItem> items = profile.getContactsByGroup(item_id);
		for(RosterItem it: items){
			if(it.type == RosterItem.OBIMP_CONTACT){
				if(((Contact)it).getStatus() != BimoidProtocol.PRES_STATUS_OFFLINE)
					return false;
			}else{
				return false;
			}
		}
		final Vector<Group> childs = profile.getChildGroups(item_id);
		if(PreferenceTable.show_offline){
			if(items.size() > 0){
				return false;
			}else{
				if(childs.size() == 0)
					return true;
			}
		}
		for(Group group: childs){
			boolean empty = group.isEmptyForDisplay();
			if(!empty) return false;
		}
		return true;
	}
}
