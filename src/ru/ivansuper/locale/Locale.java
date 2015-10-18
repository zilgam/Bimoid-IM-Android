package ru.ivansuper.locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidim.utilities;

import android.preference.PreferenceManager;
import android.util.Log;

public class Locale {
	public static final int DEFAULT = 0;
	public static final int ENGLISH = 1;
	private static HashMap<String, String> strings = new HashMap<String, String>();
	public static void prepareLocale(BufferedReader reader){
		try {
			strings.clear();
			while(reader.ready()){
				String line = reader.readLine();
				//Log.e("Locale", line);
				if(line.startsWith("s")){
					int idx = line.indexOf(":=", 0);
					if(idx <= 3) continue;
					String key = line.substring(0, idx).trim();
					String value = line.substring(idx+2, line.length()).trim();
					strings.put(key, value);
					//Log.e("Locale", key+" := "+value);
				}
			}
		} catch (IOException e) {
			strings.clear();
			e.printStackTrace();
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void prepare(){
		int current = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(resources.ctx).getString("ms_select_language", "-1"));
		if(current == -1){
			final String code = resources.ctx.getResources().getConfiguration().locale.getLanguage();
			Log.e("Device lang code", code);
			if(code.equalsIgnoreCase("ru")){
				prepareInternalRU();
			}else{
				prepareInternalEN();
			}
		}else{
		ArrayList<Language> list = getAvailable();
		if(current > list.size()) current = DEFAULT;
			switch(current){
			case DEFAULT:
				prepareInternalRU();
				break;
			case ENGLISH:
				prepareInternalEN();
				break;
			}
		}
	}
	public static void prepareInternalRU(){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(resources.am.open("locale/RU.txt")));
			prepareLocale(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void prepareInternalEN(){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(resources.am.open("locale/EN.txt")));
			prepareLocale(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static ArrayList<Language> getAvailable(){
		final ArrayList<Language> list = new ArrayList<Language>();
		Language language = new Language("Русская локализация", "Русский", "Ivansuper", "locale/RU.txt", true);
		list.add(language);
		language = new Language("English localization", "English", "Ivansuper", "locale/EN.txt", true);
		list.add(language);
		return list;
	}
	public static String getString(String key){
		String result = strings.get(key);
		if(result == null) result = "null";
		result = utilities.replace(result, "[NL]", "\n");
		return result;
	}
	
}
