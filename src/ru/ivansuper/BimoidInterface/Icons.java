package ru.ivansuper.BimoidInterface;

import java.io.File;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class Icons {
	private static HashMap<String, Drawable> icons = new HashMap<String, Drawable>();
	public static void init(File config){
		icons.clear();
		if(!config.exists()) return;
		
	}
}
