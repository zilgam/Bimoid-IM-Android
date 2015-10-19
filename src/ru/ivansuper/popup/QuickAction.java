package ru.ivansuper.popup;

import android.content.Context;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
 
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
 
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
 
import java.util.ArrayList;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;
 
public class QuickAction extends CustomPopupWindow {
  private final View root;
  //private final ImageView mArrowUp;
  private final TextView mHeaderLabel;
  private final LayoutInflater inflater;
  private final Context context;
  
  protected static final int ANIM_GROW_FROM_LEFT = 1;
  protected static final int ANIM_GROW_FROM_RIGHT = 2;
  protected static final int ANIM_GROW_FROM_CENTER = 3;
  protected static final int ANIM_REFLECT = 4;
  protected static final int ANIM_AUTO = 5;
  
  private int animStyle;
  private ViewGroup mTrack;
  
  public QuickAction(View anchor, String header) {
    super(anchor);
    
    context    = anchor.getContext();
    inflater   = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    
    root    = (ViewGroup) inflater.inflate(R.layout.popup, null); 
    
    mHeaderLabel = (TextView) root.findViewById(R.id.popup_wnd_header);
    LinearLayout mHeaderDivider = (LinearLayout) root.findViewById(R.id.popup_header_divider);
    if(header != null){
    	mHeaderDivider.setBackgroundColor(ColorScheme.getColor(4));
    	mHeaderLabel.setTextColor(ColorScheme.getColor(2));
    	mHeaderLabel.setText(header);
    }else{
    	mHeaderDivider.setVisibility(View.GONE);
    	mHeaderLabel.setVisibility(View.GONE);
    }
    LinearLayout mContainer = (LinearLayout) root.findViewById(R.id.popup_container);
    Interface.attachBackground(mContainer, Interface.status_selector_back);
    
    setContentView(root);
    
    mTrack       = (ViewGroup) root.findViewById(R.id.tracks);
    animStyle    = ANIM_AUTO;
  }
 
  public void show () {
    preShow();
    
    int xPos, yPos;
    
    int[] location     = new int[2];
  
    anchor.getLocationOnScreen(location);
 
    Rect anchorRect   = new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] 
              + anchor.getHeight());
 
    //createActionList();
    root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
  
    int rootHeight     = root.getMeasuredHeight();
    int rootWidth    = root.getMeasuredWidth();
    if(rootWidth > 1000) rootWidth = root.getWidth();
    
    int screenWidth   = windowManager.getDefaultDisplay().getWidth();
    int screenHeight  = windowManager.getDefaultDisplay().getHeight();
    
  if ((anchorRect.left + rootWidth) > screenWidth) {
      xPos = anchorRect.left - (rootWidth-anchor.getWidth());
    } else {
      if (anchor.getWidth() > rootWidth) {
        xPos = anchorRect.centerX() - (rootWidth/2);
      } else {
        xPos = anchorRect.left;
      }
    }
    
    int dyTop      = anchorRect.top;
    int dyBottom    = screenHeight - anchorRect.bottom;
 
    //boolean onTop    = (dyTop > dyBottom) ? true : false;
 
    //if (onTop) {
      if (rootHeight > dyTop) {
    	    //Log.e("INFO", "rootHeight > dyTop");
        yPos       = 0;
        LayoutParams l   = mTrack.getLayoutParams();
        l.height    = dyTop;// - anchor.getHeight();
      } else {
  	    //Log.e("INFO", "rootHeight <= dyTop");
        yPos = anchorRect.top - rootHeight;
      }
    /*} else {
      yPos = anchorRect.bottom;
      
      if (rootHeight > dyBottom) { 
        LayoutParams l   = mTrack.getLayoutParams();
        l.height    = dyBottom;
      }
    }*/
    if(xPos < 0) xPos = 0;
    //int my_x = anchorRect.left-xPos+anchor.getWidth()/2;
    //Log.i("INFO", String.valueOf(xPos));
    //Log.i("INFO", String.valueOf(anchorRect.left));
    //showArrow(R.id.arrow_down, my_x);
    
    //setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);
    //Log.e("INFO", String.valueOf(yPos));
    window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
  }
  
  
  public void setCustomView(View view){
	  mTrack.addView(view);
  }
  
 
}