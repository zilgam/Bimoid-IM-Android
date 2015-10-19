package ru.ivansuper.popup;

import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;

public class PopupBuilder {
	public static final QuickAction build(View content, View on_who, String header){
		final QuickAction qa = new QuickAction(on_who, header);
		qa.setCustomView(content);
		return qa;
	}
	public static final QuickAction buildList(BaseAdapter adapter, View on_who, String header, float width, float height, OnItemClickListener listener){
		final ListView list = new ListView(resources.ctx);
		list.setAdapter(adapter);
		list.setDivider(null);
		list.setDividerHeight(0);
		list.setAlwaysDrawnWithCacheEnabled(false);
		list.setBackgroundColor(0x00000000);
		list.setCacheColorHint(0x00000000);
		list.setDrawingCacheEnabled(false);
		list.setWillNotCacheDrawing(true);
		list.setOnItemClickListener(listener);
		list.setLayoutParams(new LinearLayout.LayoutParams((int)width, (int)height));
		Interface.attachSelector(list);
		final QuickAction qa = new QuickAction(on_who, header);
		qa.setCustomView(list);
		return qa;
	}
	public static final QuickAction buildGrid(BaseAdapter adapter, View on_who, String header, int columns_num, float width, float height, OnItemClickListener listener){
		final GridView grid = new GridView(resources.ctx);
		grid.setNumColumns(columns_num);
		grid.setAdapter(adapter);
		grid.setBackgroundColor(0x00000000);
		grid.setCacheColorHint(0x00000000);
		grid.setDrawingCacheEnabled(false);
		grid.setWillNotCacheDrawing(true);
		grid.setVerticalSpacing(0);
		grid.setHorizontalSpacing(0);
		grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		grid.setOnItemClickListener(listener);
		grid.setLayoutParams(new LinearLayout.LayoutParams((int)width, (int)height));
		Interface.attachSelector(grid);
		final QuickAction qa = new QuickAction(on_who, header);
		qa.setCustomView(grid);
		return qa;
	}
}
