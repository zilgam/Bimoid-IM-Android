package ru.ivansuper.ui;

import java.util.Vector;

import android.util.Log;

public class ExFragmentManager {
	private static final Vector<ExFragment> mList = new Vector<ExFragment>();
	public static synchronized void addFragment(ExFragment fragment){
		if(!mList.contains(fragment)) mList.add(fragment);
	}
	public static synchronized void removeFragment(ExFragment fragment){
		if(mList.contains(fragment)) mList.remove(fragment);
	}
	public static synchronized void executeEvent(ExRunnable event){
		event.setExFragment(null);
		for(ExFragment f: mList){
			event.setExFragment(f);
			event.run();
		}
		event.setExFragment(null);
	}
	public static abstract class ExRunnable implements Runnable {
		protected ExFragment fragment;
		public void setExFragment(ExFragment fragment){
			this.fragment = fragment;
		}
	}
}
