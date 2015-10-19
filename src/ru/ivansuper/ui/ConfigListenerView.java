package ru.ivansuper.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ConfigListenerView extends View {
	public OnLayoutListener listener;
	public ConfigListenerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public void onMeasure(int a, int b){
		setMeasuredDimension(MeasureSpec.getSize(a), MeasureSpec.getSize(b));
	}
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		if(listener != null) listener.onNewLayout(w, h, oldw, oldh);
	}
	public static interface OnLayoutListener {
		public abstract void onNewLayout(int w, int h, int oldw, int oldh);
	}
}
