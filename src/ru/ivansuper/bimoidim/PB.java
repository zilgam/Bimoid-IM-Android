package ru.ivansuper.bimoidim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PB extends View {
	private long max = 100;
	private long value = 0;
	private int view_width = 0;
	private int view_height = 16;
	private int shadow_border = 3;
	private int color = 0xffffffff;
	public PB(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public PB(Context context, AttributeSet attrs) {
		super(context, attrs);
	}	public PB(Context context) {
		super(context);
	}
	protected void onMeasure(int a, int b){
		a = View.MeasureSpec.getSize(a);
		view_width = a-shadow_border*2-1;
		setMeasuredDimension(view_width+1, view_height);
		///Log.v("PB", "Width = "+String.valueOf(a)+" Height = "+String.valueOf(b));
	}
	@Override
	public void draw(Canvas canvas){
		//canvas.drawColor(0xffff0000);
		Paint p = new Paint();
		p.setColor(0x20000000);
		//p.setShadowLayer(1f, 0f, 0f, 0x33000000);
		p.setStyle(Style.FILL);
		canvas.drawRect(shadow_border, shadow_border, view_width, view_height-shadow_border, p);
		p = new Paint();
		p.setColor(color);
		//p.setShadowLayer(1f, 0f, 0f, 0x33000000);
		p.setStyle(Style.STROKE);
		canvas.drawRect(shadow_border+1, shadow_border+1, view_width-2, view_height-shadow_border-2, p);
		p = new Paint();
		p.setStyle(Style.FILL);
		p.setColor(color);
		long width = value*(view_width-4)/max;
		if(width <= 4) return;
		canvas.drawRect(shadow_border+3, shadow_border+3, width-4, view_height-shadow_border-3, p);
	}
	public void setMax(long maximum){
		max = maximum;
		if(max <= 0) max = 1;
		if(value > max) value = max;
		//invalidate();
	}
	public void setProgress(long progress){
		if(max > progress){
			value = progress;
			//last = value;
		}
		invalidate();
	}
	public final void setColor(int color){
		this.color = color;
	}
}
