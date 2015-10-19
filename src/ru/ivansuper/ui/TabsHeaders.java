package ru.ivansuper.ui;

import java.util.ArrayList;

import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.ui.TabsContentHolder.TabContent;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TabsHeaders extends HorizontalScrollView {
	
	private ViewGroup mHeadersHolder;
	public TabsContentHolder mTabsContentHolder;
	private long mSelectedHeaderId;
	
	public TabsHeaders(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init();
	}
	
	private final void init(){
		
		this.setHorizontalScrollBarEnabled(false);
		
		final LinearLayout headers_holder = new LinearLayout(getContext());
		headers_holder.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		mHeadersHolder = headers_holder;
		
		addView(mHeadersHolder);
		
	}
	
	public final void fillHeaders(ArrayList<TabContent> tabs_content){
		
		mHeadersHolder.removeAllViews();
		
		for(int i=0; i<tabs_content.size(); i++){
			final TabContent tab_content = tabs_content.get(i);
			
			mHeadersHolder.addView(new TabHeader(tab_content.getTitle(), tab_content.getId(), getContext()));
		}
		
	}
	
	public final void addHeader(TabContent tab_content){
		
		mHeadersHolder.addView(new TabHeader(tab_content.getTitle(), tab_content.getId(), getContext()));
		
	}
	
	public final void removeHeader(TabContent tab_content){
		
		final int count = mHeadersHolder.getChildCount();
		
		for(int i=0; i<count; i++){
			final TabHeader tab_header = (TabHeader)mHeadersHolder.getChildAt(i);
			
			if(tab_header.mId == tab_content.getId()){
				mHeadersHolder.removeView(tab_header);
				
				break;
			}
		}
		
	}
	
	public final void setSelectedHeader(long id){
		
		final int count = mHeadersHolder.getChildCount();
		
		for(int i=0; i<count; i++){
			
			final TabHeader tab_header = (TabHeader)mHeadersHolder.getChildAt(i);
			
			if(tab_header.mId == id){
				mSelectedHeaderId = id;
				
				tab_header.setBackgroundResource(R.drawable.img_tabs_selected_header_back);
				tab_header.setTextColor(0xff000044);
				
				final int tab_header_left = tab_header.getLeft();
				final int tab_header_right = tab_header.getRight();
				final int header_scroll = getScrollX();
				
				if(tab_header_left < header_scroll)
					this.scrollTo(tab_header.getLeft() - 32, 0);
					
				if((tab_header_left + tab_header.getWidth()) > (header_scroll + getWidth()))
					this.scrollTo(header_scroll + getWidth() - tab_header.getWidth() - 32, 0);
				
			}else{
				tab_header.setBackgroundResource(R.drawable.img_tabs_normal_header_back);
				tab_header.setTextColor(0xaa000044);
			}
			
		}
		
	}
	
	@Override
	public void onLayout(boolean changed, int l, int t, int r, int b){
		super.onLayout(changed, l, t, r, b);
		
		if(changed) setSelectedHeader(mSelectedHeaderId);
		
	}
	
	private class TabHeader extends TextView {
		
		private long mId;
		
		public TabHeader(String title, long id, Context context){
			super(context);
			
			mId = id;
			
			setTextColor(0xff000044);
			setTextSize(16);
			
			final int padding = (int)resources.dm.density*6;
			setPadding(padding, padding, padding, padding);
			
			setText(title);
			
			setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					mTabsContentHolder.switchToTabById(mId);
				}
			});
			
		}
		
	}
	
}
