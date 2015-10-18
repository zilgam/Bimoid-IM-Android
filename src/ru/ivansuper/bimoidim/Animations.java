package ru.ivansuper.bimoidim;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

public class Animations {
	public static AnimationSet sts_grid_show;
	public static AnimationSet sts_grid_hide;
	public static AnimationSet DIALOG_SHOW;
	public static AnimationSet DIALOG_HIDE;
	public static AnimationSet DIALOG_SHOW_2;
	public static AnimationSet DIALOG_HIDE_2;
	static{
		sts_grid_show = new AnimationSet(true);
		AlphaAnimation aa = new AlphaAnimation(0f, 1f);
		TranslateAnimation ta = new TranslateAnimation(1, 0, 1, 0, 1, 1f, 1, 0);
		sts_grid_show.addAnimation(aa);
		sts_grid_show.addAnimation(ta);
		sts_grid_show.setDuration(400);
		sts_grid_show.setInterpolator(resources.ctx, android.R.anim.bounce_interpolator);
		//-----------------------------
		sts_grid_hide = new AnimationSet(true);
		aa = new AlphaAnimation(1f, 0f);
		ta = new TranslateAnimation(1, 0, 1, 0, 1, 0f, 1, 1);
		sts_grid_hide.addAnimation(aa);
		sts_grid_hide.addAnimation(ta);
		sts_grid_hide.setDuration(250);
		sts_grid_hide.setInterpolator(resources.ctx, android.R.anim.accelerate_decelerate_interpolator);
		//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
		DIALOG_HIDE = new AnimationSet(true);
		aa = new AlphaAnimation(1f, 0f);
		ScaleAnimation sa = new ScaleAnimation(1f, 0.9f, 1f, 0.9f, 1, 0.5f, 1, 0.5f);
		DIALOG_HIDE.addAnimation(aa);
		DIALOG_HIDE.addAnimation(sa);
		DIALOG_HIDE.setDuration(250);
		DIALOG_HIDE.setInterpolator(resources.ctx, android.R.anim.decelerate_interpolator);
		//-----------------------------
		DIALOG_SHOW = new AnimationSet(true);
		aa = new AlphaAnimation(0f, 1f);
		sa = new ScaleAnimation(0.9f, 1f, 0.9f, 1f, 1, 0.5f, 1, 0.5f);
		DIALOG_SHOW.addAnimation(aa);
		DIALOG_SHOW.addAnimation(sa);
		DIALOG_SHOW.setDuration(250);
		//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
		DIALOG_SHOW_2 = new AnimationSet(true);
		//aa = new AlphaAnimation(0f, 1f);
		ta = new TranslateAnimation(1, 0, 1, 0, 1, -1f, 1, 0);
		//DIALOG_SHOW_2.addAnimation(aa);
		DIALOG_SHOW_2.addAnimation(ta);
		DIALOG_SHOW_2.setDuration(500);
		DIALOG_SHOW_2.setInterpolator(resources.ctx, android.R.anim.decelerate_interpolator);
		//-----------------------------
		DIALOG_HIDE_2 = new AnimationSet(true);
		//aa = new AlphaAnimation(1f, 0f);
		ta = new TranslateAnimation(1, 0, 1, 0, 1, 0f, 1, -1f);
		//DIALOG_HIDE_2.addAnimation(aa);
		DIALOG_HIDE_2.addAnimation(ta);
		DIALOG_HIDE_2.setDuration(500);
		DIALOG_HIDE_2.setInterpolator(resources.ctx, android.R.anim.accelerate_interpolator);
	}
	public static void animate(final LinearLayout view, Animation ani, final boolean is_visible){
		ani.setAnimationListener(new AnimationListener(){
			@Override
			public void onAnimationEnd(Animation animation) {
				if(is_visible){
					view.setVisibility(View.GONE);
				}else{
					view.setVisibility(View.VISIBLE);
				}
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
		});
		view.startAnimation(ani);
	}
}
