package ru.ivansuper.bimoidim;

import android.content.Context;
import android.graphics.Rect;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

public class AutoRefreshTextView extends TextView {
	private int parentSpace;
	public AutoRefreshTextView(Context context) {
		super(context);
		init();
	}
	public AutoRefreshTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	public AutoRefreshTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	private final void init(){
	}
	@Override
	public final void onMeasure(int a, int b){
		parentSpace = MeasureSpec.getSize(a);
		super.onMeasure(a, b);
	}
	public final int getParentAvailableSpace(){
		return parentSpace;
	}
	@Override
	public boolean dispatchTouchEvent(MotionEvent event){
		try{
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				int idx = getCharIndexFromCoordinate(event.getX(), event.getY());
				ClickableSpan span = getSpanAt(idx);
				if(span != null)
					return true;
			}
			if(event.getAction() == MotionEvent.ACTION_UP){
				int idx = getCharIndexFromCoordinate(event.getX(), event.getY());
				ClickableSpan span = getSpanAt(idx);
				if(span != null)
					span.onClick(this);
			}
		}catch(ClassCastException e){}
		return false;
	}
	private URLSpan getSpanAt(int idx){
		URLSpan span = null;
		Spanned spn = null;
		if(idx < 0) return span;
		try{
			spn = (Spannable)getText();
		}catch(Exception e){
			return span;
		}
		URLSpan[] spans = spn.getSpans(0, getText().length(), URLSpan.class);
		for(int i=0; i<spans.length; i++){
			final URLSpan s = spans[i];
			final int start = spn.getSpanStart(s);
			final int end = spn.getSpanEnd(s);
			if((start <= idx) && (idx <= end)){
				span = s;
				break;
			}
		}
		return span;
	}
	public int getCharIndexFromCoordinate(float x, float y) {
	    // Offset the top padding
	    int height = getPaddingTop();
	    boolean need_break = false;
	    final CharSequence text = getText();
	    final Layout layout = getLayout();
	    for (int i = 0; i < layout.getLineCount(); i++) {
	    	if(need_break) break;
	    	final float size = getTextSize();
	        Rect bounds = new Rect();
	        layout.getLineBounds(i, bounds);
	        //Log.e("Bounds", i+": "+(bounds.right-bounds.left));
	        height += bounds.height();
	        if (height >= y) {
	        	need_break = true;
	            int lineStart = layout.getLineStart(i);
	            int lineEnd = layout.getLineEnd(i);
	            //Log.e("LineLength", ""+(lineEnd - lineStart));
	            Spanned span = (Spanned)text;
	            MetricAffectingSpan[] sizeSpans = span.getSpans(lineStart, lineEnd, MetricAffectingSpan.class);
	            float scaleFactor = 1;
            	/*final TextPaint p = new TextPaint();
	            if ( sizeSpans != null ) {
	                for (int j = 0; j < sizeSpans.length; j++) {
	                    sizeSpans[j].updateMeasureState(p);
	                    scaleFactor = p.getTextSize()/size;
	                }
	            }*/
	            span = (Spanned)text.subSequence(lineStart, lineEnd);
	            String lineSpan = text.subSequence(lineStart, lineEnd).toString();
	            //Log.e("Line1", lineSpan);
	            float[] widths = new float[lineSpan.length()];
	            TextPaint paint = getPaint();
	            paint.getTextWidths(lineSpan, widths);
	            float width = 0;
	            for (int j = 0; j < lineSpan.length(); j++) {
	            	final float sz = getCharSize(sizeSpans, j, size, span);
	            	//Log.e("MyTextView", "Size: "+size+"     CharSpannedSize: "+sz);
	            	scaleFactor = sz/size;
	                width += widths[j] * scaleFactor;
	                if (width >= x) {
	                    return lineStart + j;
	                }
	            }
	        }
	    }
	    return -1;
	}
	private static final float getCharSize(MetricAffectingSpan[] sizeSpans, int char_idx, float size, Spanned source){
		float res = 0;
    	final TextPaint p = new TextPaint();
    	if(sizeSpans == null) return size;
    	boolean found = false;
    	for(MetricAffectingSpan span: sizeSpans){
    		final int s = source.getSpanStart(span);
    		final int e = source.getSpanEnd(span);
    		if((s <= char_idx) && (char_idx <= e)){
    			span.updateMeasureState(p);
        		found = true;
    		}
    	}
    	if(!found) return size;
    	res = p.getTextSize();
		return res;
	}
	public void selectMatches(String pattern){
		if(pattern == null || pattern.length() == 0) return;
		final int pattern_length = pattern.length();
		String text = getText().toString();
		SpannableStringBuilder ssb = new SpannableStringBuilder(text);
		text = text.toLowerCase();
		int idx = 0;
		while(true){
			idx = text.indexOf(pattern, idx);
			if(idx == -1) break;
			ssb.setSpan(new BackgroundColorSpan(0x7700ff00), idx, idx+pattern_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			idx += pattern_length;
		}
		setText(ssb);
	}
	@Override
	protected void finalize(){
		Log.e("AutoRefreshTextView", "Class 0x"+Integer.toHexString(hashCode())+" finalized");
	}
}
