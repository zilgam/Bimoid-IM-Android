package ru.ivansuper.ui;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class TabsContentHolder extends FrameLayout {
	
	private final ArrayList<TabContent> mTabsContent = new ArrayList<TabContent>();
	private int mCurrentTab = 0;
	private int mTabsCount = 0;
	
	private TabsHeaders mTabsHeaders;
	
	public TabsContentHolder(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public final void addTab(TabContent tab_content){
		mTabsContent.add(tab_content);
		addView(tab_content.mContent);
		
		if(isHeadersAttached()) mTabsHeaders.addHeader(tab_content);
		
		mTabsCount++;
		
		if(mTabsCount == 1) switchToTab(0);
		
		computeTabs();
		
	}
	
	public final void removeTab(TabContent tab_content){
		if(mTabsContent.remove(tab_content)) mTabsCount --;
		removeView(tab_content.mContent);
		
		if(isHeadersAttached()) mTabsHeaders.removeHeader(tab_content);
		
		if(mCurrentTab >= mTabsCount) switchToTab(mTabsCount-1);
		
		computeTabs();
		
	}
	
	public final void switchToTabById(long id){
		
		final int length = mTabsContent.size();
		
		for(int i=0; i<length; i++){
			if(mTabsContent.get(i).mId == id){
				switchToTab(i);
				
				return;
			}
		}
		
	}
	
	public final void switchToTab(int index){
		if(index < 0) return;
		if(index >= mTabsCount) return;
		
		mCurrentTab = index;
		
		computeTabs();
		
	}
	
	private final void computeTabs(){
		
		final int count = getChildCount();
		
		for(int i=0; i<count; i++)
			getChildAt(i).setVisibility(i == mCurrentTab? View.VISIBLE: View.GONE);
		
		mTabsHeaders.setSelectedHeader(mTabsContent.get(mCurrentTab).mId);
		
	}
	
	public final int getCurrentTab(){
		return mCurrentTab;
	}
	
	public final void attachTabsHeaders(TabsHeaders tabs_headers){
		
		mTabsHeaders = tabs_headers;
		mTabsHeaders.mTabsContentHolder = this;
		
		mTabsHeaders.fillHeaders(mTabsContent);
		
		if(mTabsCount > 0)
			mTabsHeaders.setSelectedHeader(mTabsContent.get(mCurrentTab).mId);
		
	}
	
	private final boolean isHeadersAttached(){
		return mTabsHeaders != null;
	}
	
	public static final class TabContent {
		
		private String mTitle;
		private View mContent;
		
		private long mId;
		
		private static long mIdCounter;
		
		public TabContent(String title, View content){
			
			mTitle = title;
			mContent = content;
			
			mId = mIdCounter++;
			
		}
		
		public final String getTitle(){
			return mTitle;
		}
		
		public final long getId(){
			return mId;
		}
		
	}
	
}
