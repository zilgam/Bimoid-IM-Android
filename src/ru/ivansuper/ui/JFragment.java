package ru.ivansuper.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class JFragment {
	private int mContainer;
	private View mContent;
	private int mContentId;
	private boolean mPaused;
	private Activity mActivity;
	public void onAttach(Activity activity){
		mActivity = activity;
	}
	public void setContentView(int res_id){
		mContentId = res_id;
	}
	public void createContent(){
		final View view = View.inflate(mActivity, mContentId, null);
		mContent = view;
	}
	public final View getView(){
		return mContent;
	}
	public void onCreate(){
	}
	public void onStart(){
	}
	public void onPause(){
		mPaused = true;
	}
	public void onResume(){
		mPaused = false;
	}
	public void onDestroy(){
	}
	public final int getId(){
		return mContainer;
	}
	public final void setId(int container){
		mContainer = container;
	}
	public final boolean isPaused(){
		return mPaused;
	}
	public final void startActivity(Intent intent){
		mActivity.startActivity(intent);
	}
}
