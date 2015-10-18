package ru.ivansuper.bimoidproto;

import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidproto.BimoidProfile.BanList.Item;
import android.preference.PreferenceManager;
import android.util.Log;

public class AntispamBot {
	public static final int BANNED = 0;
	public static final int NEED_QUEST = 1;
	public static final int ACCEPTED = 2;
	public static final synchronized int checkQuestion(String id, String message, BimoidProfile profile){
		Item item = profile.banlist.get(id);
		if(item != null){
			Log.e("AntispamBot", "User rating: "+item.tryes);
			if(item.tryes >= 5){
				Log.e("AntispamBot", "User: "+id+" | banned");
				return BANNED;
			}
			profile.banlist.increase(id);
		}else{
			profile.banlist.increase(id);
			Log.e("AntispamBot", "User: "+id+" | increasing tryes");
		}
		String answer = PreferenceManager.getDefaultSharedPreferences(resources.ctx).getString("antispam_answer", "Russia, Россия");
		String[] variants = answer.split(",");
		for(String variant: variants){
			if(message.trim().equalsIgnoreCase(variant.trim())){
				Log.e("AntispamBot", "User: "+id+" | accepted");
				profile.banlist.remove(id);
				return ACCEPTED;
			}
		}
		Log.e("AntispamBot", "User: "+id+" | need question");
		return NEED_QUEST;
	}
	public static final String getQuestion(){
		return PreferenceManager.getDefaultSharedPreferences(resources.ctx).getString("antispam_question", "[EN] Antispam: What is the biggest country in the world?_[RU] Антиспам: Самая большая по площади страна в мире?").replace("_", "\n");
	}
	public static final String getAccepted(){
		return PreferenceManager.getDefaultSharedPreferences(resources.ctx).getString("antispam_accepted_msg", "[EN] Thank you. Now you can send messages directly to my contact list._[RU] Спасибо. Теперь вы можете писать прямо мне.").replace("_", "\n");
	}
	public static final boolean enabled(){
		return PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("antispam_enabled", false);
	}
}
