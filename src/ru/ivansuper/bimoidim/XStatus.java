package ru.ivansuper.bimoidim;

import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class XStatus {
	public static Vector<Bitmap> list = new Vector<Bitmap>();
	private static boolean initialized;
	public static void init(){
		if(initialized) return;
		final BitmapDrawable source_drw = (BitmapDrawable)resources.res.getDrawable(R.drawable.xstatuses);
		final Bitmap source = source_drw.getBitmap();
		final int sample_size = source.getHeight();
		for(int i=0; i<56; i++){
			Bitmap sts = Bitmap.createBitmap(source, i*sample_size, 0, sample_size, sample_size);
			sts.setDensity(0);
			list.add(sts);
		}
		initialized = true;
	}
	public static Drawable getIcon(int index){
		return new BitmapDrawable(list.get(index));
	}
}
