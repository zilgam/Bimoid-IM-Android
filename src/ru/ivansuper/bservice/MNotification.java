package ru.ivansuper.bservice;

import android.content.Intent;

public class MNotification {
	public String nick;
	public String text;
	//public ContactlistItem item;
	public Intent intent;
	public String schema;
	public MNotification(){
		
	}
	public MNotification(String nick, String text, Intent intent, String scheme){
		this.nick = nick;
		this.text = text;
		//this.item = item;
		this.intent = intent;
		this.schema = scheme;
	}
}
