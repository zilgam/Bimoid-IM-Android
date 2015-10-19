package ru.ivansuper.bimoidim;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class DialogBuilder {
	private static int style = R.style.DialogTheme;
	public static void setStyle(int resource_id){
		style = resource_id;
	}
	private static LinearLayout prepareContainer(Context context, String caption){
		LinearLayout container = null;
		LinearLayout header = (LinearLayout)View.inflate(context, R.layout.dialog_header, null);
		TextView header_label = (TextView)header.findViewById(R.id.dialog_header);
		header_label.setText(caption);
    	if(ColorScheme.initialized) header_label.setTextColor(ColorScheme.getColor(3));
		LinearLayout divider = (LinearLayout)header.findViewById(R.id.dialog_header_divider);
    	if(ColorScheme.initialized) divider.setBackgroundColor(ColorScheme.getColor(4));
		container = (LinearLayout)header.findViewById(R.id.dialog_view);
		return container;
	}
	public static Dialog create(Context context, String hdr, String text, int gravity){
		return create(context, hdr, text, gravity, R.style.CenterDialogAnimation);
	}
	public static Dialog create(Context context, String hdr, String text, int gravity, int win_animation){
		LinearLayout container = prepareContainer(context, hdr);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
		TextView txt = new TextView(resources.ctx);
		txt.setTextSize(16);
		txt.setTextColor(0xffffffff);
    	if(ColorScheme.initialized) txt.setTextColor(ColorScheme.getColor(12));
		txt.setPadding(5, 5, 5, 5);
		txt.setText(text);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		txt.setLayoutParams(lay_p);
		lay.addView(txt);
		container.addView(lay);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.windowAnimations = win_animation;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView((View)container.getParent());
		return d;
	}
	public static Dialog create(Context context, String hdr, View content, int gravity){
		LinearLayout container = prepareContainer(context, hdr);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
		lay.addView(content);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		lay.setLayoutParams(lay_p);
		container.addView(lay);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView((View)container.getParent());
		return d;
	}
	public static Dialog create(Context context, String hdr, UAdapter adapter, int gravity, OnItemClickListener listener){
		return create(context, hdr, adapter, gravity, listener, R.style.CenterDialogAnimation);
	}
	public static Dialog create(Context context, String hdr, UAdapter adapter, int gravity, OnItemClickListener listener, int animation){
		LinearLayout container = prepareContainer(context, hdr);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
    	ListView list = new ListView(context);
    	list.setDividerHeight(0);
    	list.setCacheColorHint(0x00000000);
    	list.setAdapter(adapter);
    	list.setOnItemClickListener(listener);
    	Interface.attachSelector(list);
		lay.addView(list);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		lay.setLayoutParams(lay_p);
		container.addView(lay);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.windowAnimations = animation;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView((View)container.getParent());
		return d;
	}
	public static Dialog createWithNoHeader(Context context, View content, int gravity){
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		content.setLayoutParams(lay_p);
		lay.addView(content);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.windowAnimations = R.style.CenterDialogAnimation;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView(lay);
		return d;
	}
	public static Dialog createWithNoHeader(Context context, UAdapter adapter, int gravity, OnItemClickListener listener){
		return createWithNoHeader(context, adapter, gravity, listener, R.style.CenterDialogAnimation);
	}
	public static Dialog createWithNoHeader(Context context, UAdapter adapter, int gravity, OnItemClickListener listener, int animation){
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
    	ListView list = new ListView(context);
    	list.setDividerHeight(0);
    	list.setCacheColorHint(0x00000000);
    	Interface.attachSelector(list);
    	list.setAdapter(adapter);
    	list.setOnItemClickListener(listener);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		list.setLayoutParams(lay_p);
		lay.addView(list);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.windowAnimations = animation;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView(lay);
		return d;
	}
	public static Dialog createGridWithNoHeader(Context context, UAdapter adapter, int gravity, int columns, OnItemClickListener listener){
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
    	GridView list = new GridView(context);
    	list.setCacheColorHint(0x00000000);
    	Interface.attachSelector(list);
    	list.setAdapter(adapter);
    	list.setOnItemClickListener(listener);
    	list.setNumColumns(columns);
    	list.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		list.setLayoutParams(lay_p);
		lay.addView(list);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.windowAnimations = R.style.CenterDialogAnimation;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView(lay);
		return d;
	}
	public static Dialog createOk(Context context, String hdr, String text, String ok, int gravity, OnClickListener listener){
		LinearLayout container = prepareContainer(context, hdr);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
		TextView txt = new TextView(resources.ctx);
		txt.setTextSize(16);
		txt.setTextColor(0xffffffff);
		txt.setPadding(5, 5, 5, 5);
		txt.setText(text);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		txt.setLayoutParams(lay_p);
    	if(ColorScheme.initialized) txt.setTextColor(ColorScheme.getColor(12));
		lay.addView(txt);
		Button ok_btn = new Button(resources.ctx);
		Interface.attachButtonStyle(ok_btn);
	    if(ColorScheme.initialized) ok_btn.setTextColor(ColorScheme.getColor(24));
		//ok_btn.setBackgroundDrawable(resources.res.getDrawable(R.drawable.button));
		ok_btn.setText(ok);
		ok_btn.setOnClickListener(listener);
		lay.addView(ok_btn);
		container.addView(lay);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
	    lp.width = LayoutParams.FILL_PARENT;
	    lp.height = LayoutParams.FILL_PARENT;
	    lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView((View)container.getParent());
		return d;
	}
	public static Dialog createOk(Context context, View cnt, String hdr, String ok, int gravity, OnClickListener listener){
		LinearLayout container = prepareContainer(context, hdr);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		cnt.setLayoutParams(lay_p);
		lay.addView(cnt);
		Button ok_btn = new Button(resources.ctx);
		Interface.attachButtonStyle(ok_btn);
	    if(ColorScheme.initialized) ok_btn.setTextColor(ColorScheme.getColor(24));
		//ok_btn.setBackgroundDrawable(resources.res.getDrawable(R.drawable.button));
		ok_btn.setText(ok);
		ok_btn.setOnClickListener(listener);
		lay.addView(ok_btn);
		container.addView(lay);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView((View)container.getParent());
		return d;
	}
	public static Dialog createYesNo(Context context, int gravity, String hdr, String text, String yes, String no, OnClickListener yes_listener, OnClickListener no_listener){
		LinearLayout lay_ = new LinearLayout(resources.ctx);
		LinearLayout container = prepareContainer(context, hdr);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
		lay_.setOrientation(LinearLayout.HORIZONTAL);
		Button yes_btn = new Button(resources.ctx);
		Interface.attachButtonStyle(yes_btn);
	    if(ColorScheme.initialized) yes_btn.setTextColor(ColorScheme.getColor(24));
		//yes_btn.setBackgroundDrawable(resources.res.getDrawable(R.drawable.button));
		yes_btn.setText(yes);
		yes_btn.setOnClickListener(yes_listener);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		llp.weight = 1;
		yes_btn.setLayoutParams(llp);
		Button no_btn = new Button(resources.ctx);
		Interface.attachButtonStyle(no_btn);
	    if(ColorScheme.initialized) no_btn.setTextColor(ColorScheme.getColor(24));
		//no_btn.setBackgroundDrawable(resources.res.getDrawable(R.drawable.button));
		no_btn.setText(no);
		no_btn.setOnClickListener(no_listener);
		no_btn.setLayoutParams(llp);
		ScrollView sv = new ScrollView(resources.ctx);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		sv.setLayoutParams(lay_p);
		TextView txt = new TextView(resources.ctx);
		txt.setTextSize(18);
		txt.setTextColor(0xffffffff);
		txt.setPadding(5, 5, 5, 5);
		txt.setText(text);
		ScrollView.LayoutParams sv_p = new ScrollView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		txt.setLayoutParams(sv_p);
		sv.addView(txt);
    	if(ColorScheme.initialized) txt.setTextColor(ColorScheme.getColor(12));
		lay.addView(sv);
		lay_.addView(yes_btn);
		lay_.addView(no_btn);
		lay.addView(lay_);
		container.addView(lay);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
        lp.height = LayoutParams.FILL_PARENT;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView((View)container.getParent());
		return d;
	}
	public static Dialog createYesNo(Context context, View content, int gravity, String hdr, String yes, String no, OnClickListener yes_listener, OnClickListener no_listener){
		LinearLayout lay_ = new LinearLayout(resources.ctx);
		LinearLayout container = prepareContainer(context, hdr);
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.VERTICAL);
		lay_.setOrientation(LinearLayout.HORIZONTAL);
		Button yes_btn = new Button(resources.ctx);
		Interface.attachButtonStyle(yes_btn);
	    if(ColorScheme.initialized) yes_btn.setTextColor(ColorScheme.getColor(24));
		yes_btn.setText(yes);
		yes_btn.setOnClickListener(yes_listener);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		llp.weight = 1;
		yes_btn.setLayoutParams(llp);
		Button no_btn = new Button(resources.ctx);
		Interface.attachButtonStyle(no_btn);
	    if(ColorScheme.initialized) no_btn.setTextColor(ColorScheme.getColor(24));
		no_btn.setText(no);
		no_btn.setOnClickListener(no_listener);
		no_btn.setLayoutParams(llp);
		LinearLayout.LayoutParams lay_p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 5);
		content.setLayoutParams(lay_p);
		lay.addView(content);
		lay_.addView(yes_btn);
		lay_.addView(no_btn);
		lay.addView(lay_);
		container.addView(lay);
    	Dialog d = new Dialog(context, style);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.gravity = gravity;
    	wnd.setAttributes(lp);
    	wnd.setContentView((View)container.getParent());
		return d;
	}
	public static Dialog createProgress(Context context, String text, boolean cancelable){
		LinearLayout lay = new LinearLayout(resources.ctx);
		lay.setOrientation(LinearLayout.HORIZONTAL);
		lay.setGravity(Gravity.CENTER_VERTICAL);
		lay.setPadding(10, 10, 10, 10);
		ImageView arrows = new ImageView(context);
		arrows.setImageDrawable(resources.res.getDrawable(R.drawable.progress_arrows));
        RotateAnimation ra = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(500);
        ra.setInterpolator(context, android.R.anim.linear_interpolator);
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.RESTART);
        arrows.setAnimation(ra);
		TextView txt = new TextView(resources.ctx);
		txt.setTextSize(16);
    	if(ColorScheme.initialized) txt.setTextColor(ColorScheme.getColor(12));
		txt.setPadding(10, 5, 5, 5);
		txt.setText(text);
    	if(ColorScheme.initialized) txt.setTextColor(ColorScheme.getColor(12));
		lay.addView(arrows);
		lay.addView(txt);
    	Dialog d = new Dialog(context, style);
    	d.setCancelable(cancelable);
    	Window wnd = d.getWindow();
    	Interface.attachWindowBackground(wnd, Interface.dialogs_back);
    	WindowManager.LayoutParams lp = wnd.getAttributes();
    	lp.width = LayoutParams.FILL_PARENT;
    	lp.height = LayoutParams.FILL_PARENT;
    	lp.windowAnimations = R.style.CenterDialogAnimation;
    	lp.gravity = Gravity.CENTER;
    	wnd.setAttributes(lp);
    	wnd.setContentView(lay);
		return d;
	}
}
