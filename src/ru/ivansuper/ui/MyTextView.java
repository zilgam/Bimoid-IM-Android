package ru.ivansuper.ui;

import java.util.Vector;

import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.UAdapter;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.locale.Locale;


import android.app.Dialog;
import android.app.Service;
import android.text.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Handler.Callback;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.MeasureSpec;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MyTextView extends View {
	private int parentSpace;
	private Layout layout;
	private CharSequence text;
	private TextPaint paint;
	private int max_lines = 0;
	private Dialog mContextMenu;
	public MyTextView(Context context) {
		super(context);
		init();
	}
	public MyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	private void init(){
		paint = new TextPaint();
		paint.setTextSize(14);
		paint.setColor(0xffffffff);
		paint.setAntiAlias(true);
		text = "";
	}
	public void setText(CharSequence text, boolean detect_links){
		this.text = text;
		if(detect_links){
			Spannable s = null;
			if(this.text instanceof Spannable){
				s = (Spannable)this.text;
			}else{
				s = Factory.getInstance().newSpannable(this.text);
			}
			Linkify.addLinks(s, Linkify.WEB_URLS);
			this.text = s;
		}
		//relayout();
		invalidate();
	}
	public static final synchronized SpannableStringBuilder detectLinks(String source){
		SpannableStringBuilder s = null;
		s = new SpannableStringBuilder(source);
		Linkify.addLinks(s, Linkify.WEB_URLS);
		return s;
	}
	public void setText(CharSequence text){
		setText(text, true);
	}
	public void setTextSize(float size){
		paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, resources.dm));
		//relayout();
		invalidate();
	}
	public void setTextColor(int color){
		paint.setColor(color);
		//relayout();
		invalidate();
	}
	public int getTextColor(){
		return paint.getColor();
	}
	public void setLinkTextColor(int color){
		paint.linkColor = color;
		//relayout();
		invalidate();
	}
	public void setMaxLines(int lines){
		max_lines = lines;
	}
	public void relayout(){
		if(layout != null)
			//if(getMeasuredWidth() != layout.getWidth()){
				makeNewLayout(layout.getWidth());
			//}
		requestLayout();
	}
	private void makeNewLayout(int width){
		CharSequence cs = text;
		if(max_lines != 0) cs = TextUtils.ellipsize(text, paint, width*max_lines, TextUtils.TruncateAt.END);
		layout = new StaticLayout(cs, paint, width, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
	}
	@Override
	public void onMeasure(int a, int b){
		int w = MeasureSpec.getSize(a);
		parentSpace = w;
		//int h = MeasureSpec.getSize(b);
		int dh = 0;
		if(layout == null){
			makeNewLayout(w);
		}else{
			if(w != layout.getWidth())
				makeNewLayout(w);
		}
		dh = layout.getLineTop(layout.getLineCount());
		setMeasuredDimension(w, dh);
	}
	private final void attachHightlight(ClickableSpan span){
		try{
			//detectStringBoundaries(span);
			SpannableStringBuilder ssb = (SpannableStringBuilder)text;
			highlight_span = new BackgroundColorSpan(0x770000ff);
			ssb.setSpan(highlight_span, ssb.getSpanStart(span), ssb.getSpanEnd(span), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			setText(ssb, false);
		}catch(Exception e){}
	}
	private final void removeHighlight(){
		if(highlight_span == null) return;
		try{
			SpannableStringBuilder ssb = (SpannableStringBuilder)text;
			ssb.removeSpan(highlight_span);
			setText(ssb, false);
			highlight_span = null;
		}catch(Exception e){}
	}
	private boolean mClickHandled = false;
	private boolean mLongClickHandled = false;
	private URLSpan mContextURL;
	private BackgroundColorSpan highlight_span;
	@Override
	public boolean dispatchTouchEvent(MotionEvent event){
		try{
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				mClickHandled = false;
				mLongClickHandled = false;
				int idx = getCharIndexFromCoordinate(event.getX(), event.getY());
				ClickableSpan span = getSpanAt(idx);
				if(span != null){
					attachHightlight(span);
					if(span instanceof URLSpan){
						this.postDelayed(new Runnable(){
							@Override
							public void run() {
								if(!mClickHandled){
									mLongClickHandled = true;
									showMyContextMenu();
								}
							}
						}, 600);
						mContextURL = (URLSpan)span;
					}
					return true;
				}
				//if(idx >= 0)
				//	Log.e("Touch detector", "["+idx+"]Symbol pressed: "+text.charAt(idx));
			}
			switch(event.getAction()){
			case MotionEvent.ACTION_CANCEL:
				mClickHandled = true;
				removeHighlight();
				break;
			case MotionEvent.ACTION_OUTSIDE:
				mClickHandled = true;
				removeHighlight();
				break;
			case MotionEvent.ACTION_UP:
				removeHighlight();
				if(mLongClickHandled) break;
				mClickHandled = true;
				int idx = getCharIndexFromCoordinate(event.getX(), event.getY());
				ClickableSpan span = getSpanAt(idx);
				if(span != null){
					span.onClick(this);
					return true;
				}
				break;
			}
		}catch(Exception e){}
		return false;//super.dispatchTouchEvent(event);
	}
	private final void showMyContextMenu(){
		final UAdapter adp = new UAdapter();
		adp.setMode(UAdapter.FORCE_HIDE_ICON);
		adp.setTextSize(16);
		adp.setPadding(16);
		adp.put(Locale.getString("s_do_open"), 0);
		adp.put(Locale.getString("s_do_copy"), 1);
		mContextMenu = new Dialog(resources.service, R.style.DialogTheme);
		Window wnd = mContextMenu.getWindow();
		LayoutParams lp = wnd.getAttributes();
		lp.type = LayoutParams.TYPE_SYSTEM_ALERT;
		lp.gravity = Gravity.CENTER;
		lp.width = (int)(getWidth()*0.8);
		lp.windowAnimations = 0;
		lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;
		wnd.setAttributes(lp);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		ListView list = new ListView(getContext());
		list.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		list.setDividerHeight(0);
		list.setSelector(Interface.getSelector());
		list.setAdapter(adp);
		list.setOnItemClickListener(new OnItemClickListener(){
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						mContextMenu.dismiss();
						int id = (int)adp.getItemId(arg2);
						if(id < 2){
							switch(id){
							case 0:
								mContextURL.onClick(MyTextView.this);
								break;
							case 1:
								ClipboardManager cm = (ClipboardManager)getContext().getSystemService(Service.CLIPBOARD_SERVICE);
								cm.setText(mContextURL.getURL());
								Toast.makeText(getContext(), Locale.getString("s_copied"), Toast.LENGTH_SHORT).show();
								break;
							}
						}else{
						}
					}
				});
		lay.addView(list);
		wnd.setContentView(lay);
		mContextMenu.show();
	}
	private ClickableSpan getSpanAt(int idx){
		ClickableSpan span = null;
		Spanned spn = null;
		if(idx < 0) return span;
		try{
			spn = (Spannable)this.text;
		}catch(Exception e){
			return span;
		}
		ClickableSpan[] spans = spn.getSpans(0, this.text.length(), ClickableSpan.class);
		for(int i=0; i<spans.length; i++){
			final ClickableSpan s = spans[i];
			final int start = spn.getSpanStart(s);
			final int end = spn.getSpanEnd(s);
			//Log.e("getSpanAt", "[idx: "+idx+"|s: "+start+"|e: "+end+"]Span: "+text.subSequence(start, end));
			if((start <= idx) && (idx <= end)){
				span = s;
				break;
			}
		}
		return span;
	}
	public int getCharIndexFromCoordinate(float x, float y) {
		int line = layout.getLineForVertical((int)y);
		final int index = layout.getOffsetForHorizontal(line, x);
		return x > layout.getLineWidth(line)? -1: index;
	}
	public final int getParentAvailableSpace(){
		return parentSpace;
	}
	@Override
	public void onDraw(Canvas canvas){
		if(layout != null)
			layout.draw(canvas);
	}
	public void selectMatches(String pattern){
		if(pattern == null || pattern.length() == 0) return;
		final int pattern_length = pattern.length();
		String text = this.text.toString();
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
		Log.e("MyTextView", "Class 0x"+Integer.toHexString(hashCode())+" finalized");
	}
}
