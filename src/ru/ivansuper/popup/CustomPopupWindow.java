package ru.ivansuper.popup;

import android.content.Context;

import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
 
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
 
import android.widget.PopupWindow;
 
public class CustomPopupWindow {
  protected final View anchor;
  public final PopupWindow window;
  private View root;
  private Drawable background = null;
  protected final WindowManager windowManager;
  public CustomPopupWindow(View anchor) {
    this.anchor = anchor;
    this.window = new PopupWindow(anchor.getContext());
    window.setTouchInterceptor(new OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
          CustomPopupWindow.this.window.dismiss();
          return true;
        }
        return false;
      }
    });
    windowManager = (WindowManager) anchor.getContext().getSystemService(Context.WINDOW_SERVICE);
    onCreate();
  }
 
  protected void onCreate() {}
 
 
  protected void onShow() {}
 
  protected void preShow() {
    if (root == null) {
      throw new IllegalStateException("error");
    }
    
    onShow();
 
    if (background == null) {
      window.setBackgroundDrawable(new ColorDrawable(0x00000000));
    } else {
      window.setBackgroundDrawable(background);
    }
    
    window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
    window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
    window.setTouchable(true);
    window.setFocusable(true);
    window.setOutsideTouchable(true);
 
    window.setContentView(root);
  }
 
  public void setBackgroundDrawable(Drawable background) {
    this.background = background;
  }
 
  public void setContentView(View root) {
    this.root = root;
    
    window.setContentView(root);
  }
 
  public void setContentView(int layoutResID) {
    LayoutInflater inflator =
        (LayoutInflater) anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    
    setContentView(inflator.inflate(layoutResID, null));
  }
 
 
  public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
    window.setOnDismissListener(listener);
  }
 
 
  public void showDropDown() {
    showDropDown(0, 0);
  }
 
  public void showDropDown(int xOffset, int yOffset) {
    preShow();
    window.setAnimationStyle(android.R.style.Animation_Toast);
    //window.setAnimationStyle(R.style.Animations_PopDownMenu);
 
    window.showAsDropDown(anchor, xOffset, yOffset);
  }
 
  public void showLikeQuickAction() {
    showLikeQuickAction(0, 0);
  }
 
  public void showLikeQuickAction(int xOffset, int yOffset) {
    preShow();
    window.setAnimationStyle(android.R.style.Animation_Toast);
    //window.setAnimationStyle(R.style.Animations_PopUpMenu_Center);
 
    int[] location = new int[2];
    anchor.getLocationOnScreen(location);
 
    Rect anchorRect =
        new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1]
          + anchor.getHeight());
 
    root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    
    int rootWidth     = root.getMeasuredWidth();
    int rootHeight     = root.getMeasuredHeight();
 
    int screenWidth   = windowManager.getDefaultDisplay().getWidth();
 
    int xPos       = ((screenWidth - rootWidth) / 2) + xOffset;
    int yPos       = anchorRect.top - rootHeight + yOffset;
 
 
    if (rootHeight > anchorRect.top) {
      yPos = anchorRect.bottom + yOffset;
      window.setAnimationStyle(android.R.style.Animation_Toast);
      //window.setAnimationStyle(R.style.Animations_PopDownMenu_Center);
    }
 
    window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
  }
  
  public void dismiss() {
    window.dismiss();
  }
}