package ru.ivansuper.bimoidim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;

public class SmileysManager {
	public static boolean packLoaded = false;
	public static boolean loading = false;
	private static Context ctx;
	public static Vector<String> tags = new Vector<String>();
	public static Vector<Drawable> smileys = new Vector<Drawable>();
	public static Vector<String> selector_tags = new Vector<String>();
	public static Vector<Drawable> selector_smileys = new Vector<Drawable>();
	public static int max_height;
	public static void init(Context ctxParam){
		ctx = ctxParam;
		File SmileysDirectory = new File(resources.SD_PATH+"/Jasmine/Smileys");
		if(!SmileysDirectory.isDirectory() || !SmileysDirectory.exists()){
			SmileysDirectory.mkdirs();
		}
	}
	public static void preloadPack(){
		if(packLoaded) return;
		loadPack();
	}
	public static void loadPack(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		String pack_name = sp.getString("current_smileys_pack", "$*INTERNAL*$");
		if(pack_name.equals("$*INTERNAL*$")){
			loadFromAssets();
		}else{
			loadFromFile(new File(resources.SD_PATH+"/Bimoid/Smileys/"+pack_name));
		}
	}
	public static void forceChangeScale(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(resources.ctx);
    	double scale = Double.parseDouble(sp.getString("ms_smileys_scale", "100"))/100.00;
		for(int i=0; i<selector_smileys.size(); i++){
			Drawable drw = selector_smileys.get(i);
        	Bitmap tmp = ((BitmapDrawable)drw).getBitmap();
        	int w = new Double(tmp.getWidth()*scale).intValue();
        	int h = new Double(tmp.getHeight()*scale).intValue();
        	drw.setBounds(0, 0, w, h);
		}
		for(int i=0; i<smileys.size(); i++){
			Drawable drw = smileys.get(i);
        	Bitmap tmp = ((BitmapDrawable)drw).getBitmap();
        	int w = new Double(tmp.getWidth()*scale).intValue();
        	int h = new Double(tmp.getHeight()*scale).intValue();
        	drw.setBounds(0, 0, w, h);
		}
	}
	public static void loadFromAssets(){
		float multiplier = 1;
		switch(resources.dm.densityDpi){
		case 120:
			multiplier = 2f;
			break;
		case 160:
			multiplier = 1.5f;
			break;
		case 240:
			multiplier = 1f;
			break;
		case 320:
			multiplier = 0.7f;
			break;
		default:
			if(resources.dm.widthPixels < 400) multiplier = 1.5f;
			break;
		}
		if(resources.dm.widthPixels < 400) multiplier = 1.5f;
		tags.clear();
		smileys.clear();
		selector_tags.clear();
		selector_smileys.clear();
		loading = true;
		max_height = 0;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(resources.am.open("define_.ini")));
			int idx = 1;
			while(true){
				String line = in.readLine();
				if(line != null){
					String[] keys = line.split(",");
					Bitmap sml = BitmapFactory.decodeStream(resources.am.open(String.valueOf(idx)));
					sml.setDensity(0);
					sml = Bitmap.createScaledBitmap(sml, (int)((float)sml.getWidth()/multiplier), (int)((float)sml.getHeight()/multiplier), true);
					sml.setDensity(0);
					Drawable smiley = new BitmapDrawable(sml);
					int size = sml.getHeight();
					if(max_height < size) max_height = size;
					selector_tags.add(keys[0]);
					selector_smileys.add(smiley);
					for(int i=0; i<keys.length; i++){
						tags.add(keys[i]);
						BitmapDrawable smiley_ = new BitmapDrawable(sml);
						smileys.add(smiley_);
					}
					idx++;
				}else{
					break;
				}
			}
			forceChangeScale();
			packLoaded = true;
		} catch (Exception e) {
			tags.clear();
			smileys.clear();
			selector_tags.clear();
			selector_smileys.clear();
			packLoaded = false;
			Log.e("SmileysManager", "Smiley pack load error!");
			e.printStackTrace();
		}
		loading = false;
	}
	public static void loadFromFile(File directory){
		try{
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inSampleSize = 2;
		String path = directory.getAbsolutePath();
		File define = new File(path+"/define.ini");
		if(define.exists()){
			tags.clear();
			smileys.clear();
			selector_tags.clear();
			selector_smileys.clear();
	    	Log.v("loadFromFile", "DEFINE.INI FOUND!");
			int idx = 1;
			BufferedReader in = new BufferedReader(new FileReader(define));
			boolean item_loaded = false;
			while(true){
				String line = in.readLine();
				if(line != null){
					item_loaded = false;
					//Log.v("SMILEYS_MANAGER", line);
					String[] keys = line.split(",");
					opts.inSampleSize = 2;
					Bitmap sml = BitmapFactory.decodeStream(resources.am.open(String.valueOf(idx)), null, opts);
					Drawable smiley = new BitmapDrawable(sml);
					int size = sml.getHeight();
					if(max_height < size) max_height = size;
					for(int i=0; i<keys.length; i++){
						tags.add(keys[i]);
	                	Drawable drw = smiley;
						smileys.add(drw);
						if(!item_loaded){
							selector_tags.add(keys[0]);
							BitmapDrawable smiley_ = new BitmapDrawable(sml);
							//Drawable smileyA = Drawable.createFromPath(path+"/"+String.valueOf(idx));
		                	selector_smileys.add(smiley_);
						}
						item_loaded = true;
					}
					idx++;
				}else{
					break;
				}
			}
			forceChangeScale();
			packLoaded = true;
		}
		}catch(Exception e){
			tags.clear();
			smileys.clear();
			selector_tags.clear();
			selector_smileys.clear();
			packLoaded = false;
			Log.e("SmileysManager", "Smiley pack load error!");
			e.printStackTrace();
		}
	}
    public static Spannable getSmiledText(String text){
    	//Log.v("SPANNABLE_BUILDER", "PROCESSING STARTED");
    	String source = new String(text);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
    	int sz = tags.size();
        for (int j=0; j<sz; j++){
        	String entry = tags.get(j);
            int length = entry.length();
    		int idx = 0;
    		String plomb = getPlomb(length);
        	while(true){
        		idx = source.indexOf(entry, idx);
        		if(idx < 0){
        			break;
        		}else{
        			String a = source.substring(0, idx);
        			String b = source.substring(idx+length, source.length());
        			source = a + plomb + b;
        			builder.setSpan(new ImageSpan(smileys.get(j)), idx, idx + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        			idx += length-1;
       			}
        	}
        }
    	//Log.v("SPANNABLE_BUILDER", "PROCESSING COMPLETE: "+source);
        return builder;
    }
    public static Spannable getSmiledText(SpannableStringBuilder text){
    	//Log.v("SPANNABLE_BUILDER", "PROCESSING STARTED");
    	String source = text.toString();
        SpannableStringBuilder builder = text;
    	int sz = tags.size();
        for (int j=0; j<sz; j++){
        	String entry = tags.get(j);
            int length = entry.length();
    		int idx = 0;
    		String plomb = getPlomb(length);
        	while(true){
        		idx = source.indexOf(entry, idx);
        		if(idx < 0){
        			break;
        		}else{
        			String a = source.substring(0, idx);
        			String b = source.substring(idx+length, source.length());
        			source = a + plomb + b;
        			builder.setSpan(new ImageSpan(smileys.get(j)), idx, idx + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        			idx += length-1;
       			}
        	}
        }
    	//Log.v("SPANNABLE_BUILDER", "PROCESSING COMPLETE: "+source);
        return builder;
    }
    private static String getPlomb(int len){
    	String res = "";
    	for(int i=0; i<len; i++){
    		res += "#";
    	}
    	return res;
    }
	public static String getTag(String source){
		String res = null;
		String buffer = null;
		for(int i=0; i<tags.size(); i++){
			buffer = tags.get(i);
			if(source.startsWith(buffer)){
				res = buffer;
				break;
			}
		}
		return res;
	}
	/*public static GifMovie getMovieByTag(String tag){
		GifMovie res = null;
		for(int i=0; i<tags.size(); i++){
			if(tags.get(i).equals(tag)){
				res = smileys.get(i);
				break;
			}
		}
		return res;
	}*/
}
