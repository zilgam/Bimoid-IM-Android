package ru.ivansuper.bimoidim;

import java.io.File;
import java.io.IOException;

import ru.ivansuper.bservice.BimoidService;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

public class resources {
	public static BimoidService service;
	public static String DATA_PATH = "";
	public static String SD_PATH = "/mnt/sdcard/";
	public static String INCOMING_FILES_PATH = "";
	public static String SKINS_PATH = "";
	public static String VERSION = "";
	public static Context ctx;
	public static Resources res;
	public static Drawable sts_online;
	public static Drawable sts_chat;
	public static Drawable sts_home;
	public static Drawable sts_lunch;
	public static Drawable sts_dnd;
	public static Drawable sts_oc;
	public static Drawable sts_na;
	public static Drawable sts_away;
	public static Drawable sts_work;
	public static Drawable sts_invis;
	public static Drawable sts_invis_all;
	public static Drawable sts_offline;
	public static Drawable sts_connecting;
	public static Drawable msg_in;
	public static Drawable msg_out;
	public static Drawable msg_out_c;
	//===============================
	public static Drawable context_menu_icon;
	public static Drawable button;
	public static AssetManager am;
	public static DisplayMetrics dm;
	public static String OS_VERSION_STR;
	public static String DEVICE_STR;
	public static String SOFTWARE_STR;
	public static boolean IT_IS_TABLET;
	public static void init(Context context){
		if(ctx != null) return;
		ctx = context;
		am = ctx.getAssets();
		dm = ctx.getResources().getDisplayMetrics();
		try {
			DATA_PATH = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), 0).dataDir+"/";
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		SD_PATH = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
		File profiles = new File(DATA_PATH+"profiles.bin");
		if(!profiles.exists())
			try {
				profiles.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		INCOMING_FILES_PATH = SD_PATH+"/Bimoid/RcvdFiles/";
		File ifp = new File(INCOMING_FILES_PATH);
		if(!ifp.exists())
			try{
				ifp.mkdirs();
			}catch(Exception e){}
			SKINS_PATH = SD_PATH+"/Bimoid/Skin/";
			File skin = new File(SKINS_PATH);
			if(!skin.exists())
				try{
					skin.mkdirs();
				}catch(Exception e){}
		try {
			VERSION = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		OS_VERSION_STR = android.os.Build.VERSION.RELEASE;
		DEVICE_STR = utilities.compute(android.os.Build.BRAND, android.os.Build.MODEL);
		SOFTWARE_STR = android.os.Build.ID;
		IT_IS_TABLET = isTablet();
		res = ctx.getResources();
		sts_online = res.getDrawable(R.drawable.sts_online);
		sts_chat = res.getDrawable(R.drawable.sts_chat);
		sts_home = res.getDrawable(R.drawable.sts_home);
		sts_lunch = res.getDrawable(R.drawable.sts_lunch);
		sts_dnd = res.getDrawable(R.drawable.sts_dnd);
		sts_oc = res.getDrawable(R.drawable.sts_oc);
		sts_na = res.getDrawable(R.drawable.sts_na);
		sts_away = res.getDrawable(R.drawable.sts_away);
		sts_work = res.getDrawable(R.drawable.sts_work);
		sts_invis = res.getDrawable(R.drawable.sts_invis);
		sts_invis_all = res.getDrawable(R.drawable.sts_invis_all);
		sts_offline = res.getDrawable(R.drawable.sts_offline);
		sts_connecting = res.getDrawable(R.drawable.sts_connecting);
		msg_in = res.getDrawable(R.drawable.msg_in_0);
		msg_out = res.getDrawable(R.drawable.msg_out);
		msg_out_c = res.getDrawable(R.drawable.msg_out_c);
		context_menu_icon = res.getDrawable(R.drawable.context_menu_icon);
		button = res.getDrawable(R.drawable.button);
	}
	/*
	PRES_STATUS_ONLINE = 0x0000
	PRES_STATUS_INVISIBLE = 0x0001
	PRES_STATUS_INVISIBLE_FOR_ALL = 0x0002
	PRES_STATUS_FREE_FOR_CHAT = 0x0003
	PRES_STATUS_AT_HOME = 0x0004
	PRES_STATUS_AT_WORK = 0x0005
	PRES_STATUS_LUNCH = 0x0006
	PRES_STATUS_AWAY = 0x0007
	PRES_STATUS_NOT_AVAILABLE = 0x0008
	PRES_STATUS_OCCUPIED = 0x0009
	PRES_STATUS_DO_NOT_DISTURB = 0x000A
	 */
	public static Drawable getMainStatusIcon(int status_code){
		switch(status_code){
		case -0x0001:
			return sts_offline;
		case 0x0000:
			return sts_online;
		case 0x0001:
			return sts_invis;
		case 0x0002:
			return sts_invis_all;
		case 0x0003:
			return sts_chat;
		case 0x0004:
			return sts_home;
		case 0x0005:
			return sts_work;
		case 0x0006:
			return sts_lunch;
		case 0x0007:
			return sts_away;
		case 0x0008:
			return sts_na;
		case 0x0009:
			return sts_oc;
		case 0x000A:
			return sts_dnd;
		}
		return sts_online;
	}
	public static boolean sd_mounted(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			return true;
		}else{
			return false;
		}
	}
	public static boolean isTablet() { 
	    try { 
	        // Compute screen size 
	        DisplayMetrics dm = ctx.getResources().getDisplayMetrics(); 
	        float screenWidth  = dm.widthPixels / dm.densityDpi;
	        float screenHeight = dm.heightPixels / dm.densityDpi; 
	        double size = Math.sqrt(Math.pow(screenWidth, 2) + 
	                                Math.pow(screenHeight, 2)); 
	        Log.e("ScreenSize", ""+size);
	        return size >= 6; 
	    } catch(Throwable t) { 
	        Log.e("resources", "Failed to compute screen size", t); 
	        return false; 
	    } 
	} 
}
