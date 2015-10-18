package ru.ivansuper.bimoidproto;

public class SuperGroup extends RosterItem {
	public boolean opened = true;
	public BimoidProfile profile;
	public SuperGroup(String name, int type){
		this.name = name;
		this.type = type;
	}
	public void setName(String name){
		this.name = name;
	}
	public String getName(){
		return name;
	}
}
