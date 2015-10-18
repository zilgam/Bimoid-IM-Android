package ru.ivansuper.BimoidInterface;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Vector;

import android.util.Log;

import ru.ivansuper.bimoidim.resources;

public class ColorScheme {
	public static Vector<Color> colors = new Vector<Color>();
	public static boolean initialized;
	public static boolean internal;
	public static void initialize(String file, boolean internal){
		if(initialized) return;
		forceInit(file, internal);
	}
	public static void forceInit(String file, boolean internal){
		try{
			BufferedReader reader = null;
			label:{
				if(internal){
					reader = new BufferedReader(new InputStreamReader(resources.am.open("ColorScheme/ColorScheme.bcs")));
					internal = true;
				}else{
					File color_scheme = new File(file);
					if(!color_scheme.exists()){
						return;
					}
					reader = new BufferedReader(new FileReader(color_scheme));
				}
			}
			colors.clear();
			while(reader.ready()){
				String line = reader.readLine();
				if(line.startsWith("//")) continue;
				String[] params = line.split("=");
				Log.i("BimoidIM:ColorScheme", params[0].trim()+": "+params[1].trim());
				String[] value = params[1].split("x");
				Color color = new Color((int)Long.parseLong(value[1], 16));
				colors.addElement(color);
			}
			initialized = true;
		}catch(Exception e){
			colors.clear();
			e.printStackTrace();
		}
	}
	public static int getColor(int index){
		return colors.get(index).value;
	}
	@Override
	public void finalize() throws Throwable {
		throw new RuntimeException("Holding colors");
	}
}
