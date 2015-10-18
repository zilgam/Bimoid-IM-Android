package ru.ivansuper.bimoidproto;

public class Group extends RosterItem {
	public boolean opened = true;
	private BimoidProfile profile;
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
}
