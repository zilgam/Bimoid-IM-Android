package ru.ivansuper.ui;

import java.util.Vector;

import android.app.Activity;
import android.view.ViewGroup;

public class JFragmentActivity extends Activity {
	private Vector<JFragment> mStack = new Vector<JFragment>();
	public final JFragment findFragment(int id){
		for(JFragment f: mStack){
			if(f.getId() == id)
				return f;
		}
		return null;
	}
	public final void attachFragment(int container, JFragment fragment){
		fragment.setId(container);
		fragment.onAttach(this);
		fragment.onCreate();
		fragment.createContent();
		ViewGroup vg = (ViewGroup)findViewById(container);
		vg.addView(fragment.getView());
		fragment.onStart();
		fragment.onResume();
		mStack.add(fragment);
	}
	public final void removeFragment(int id){
		for(JFragment f: mStack){
			if(f.getId() == id){
				f.onPause();
				f.onDestroy();
				ViewGroup vg = (ViewGroup)findViewById(f.getId());
				vg.removeView(f.getView());
				mStack.remove(f);
				return;
			}
		}
	}
	public void onResume(){
		super.onResume();
		for(JFragment f: mStack)
			if(f.isPaused())
				f.onResume();
	}
	public void onPause(){
		super.onPause();
		for(JFragment f: mStack)
			if(!f.isPaused())
				f.onPause();
	}
	public void onDestroy(){
		super.onDestroy();
		for(JFragment f: mStack){
			if(f.isPaused())
				f.onPause();
			f.onDestroy();
		}
		mStack.clear();
	}
}
