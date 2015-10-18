package ru.ivansuper.bimoidim;

import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.bimoidproto.AccountInfoContainer;
import ru.ivansuper.locale.Locale;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SearchResultsAdapter extends BaseAdapter {
	private Vector<AccountInfoContainer> list = new Vector<AccountInfoContainer>();
	public void put(AccountInfoContainer info){
		list.add(info);
		notifyDataSetChanged();
	}
	public void clear(){
		list.clear();
		notifyDataSetChanged();
	}
	@Override
	public int getCount() {
		return list.size();
	}
	@Override
	public AccountInfoContainer getItem(int arg0) {
		return list.get(arg0);
	}
	@Override
	public long getItemId(int arg0) {
		return arg0;
	}
	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		LinearLayout item = null;
		if(arg1 == null){
			item = (LinearLayout)View.inflate(resources.ctx, R.layout.search_result_item, null);
		}else{
			item = (LinearLayout)arg1;
		}
		ImageView sts = (ImageView)item.findViewById(R.id.search_item_status);
		TextView account = (TextView)item.findViewById(R.id.search_item_account);
		if(ColorScheme.initialized) account.setTextColor(ColorScheme.getColor(22));
		TextView firstname = (TextView)item.findViewById(R.id.search_item_firstname);
		if(ColorScheme.initialized) firstname.setTextColor(ColorScheme.getColor(23));
		TextView lastname = (TextView)item.findViewById(R.id.search_item_lastname);
		if(ColorScheme.initialized) lastname.setTextColor(ColorScheme.getColor(23));
		TextView gender = (TextView)item.findViewById(R.id.search_item_gender);
		if(ColorScheme.initialized) gender.setTextColor(ColorScheme.getColor(23));
		TextView age = (TextView)item.findViewById(R.id.search_item_age);
		if(ColorScheme.initialized) age.setTextColor(ColorScheme.getColor(23));
		AccountInfoContainer info = getItem(arg0);
		if(info.online_){
			sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_online));
		}else{
			sts.setImageDrawable(resources.res.getDrawable(R.drawable.sts_offline));
		}
		account.setText(Locale.getString("s_id")+": "+utilities.filter(info.account_)+"\n"+Locale.getString("s_nick")+": "+utilities.filter(info.nick_name));
		firstname.setText(Locale.getString("s_name")+": "+utilities.filter(info.first_name));
		lastname.setText(Locale.getString("s_surname")+": "+utilities.filter(info.last_name));
		gender.setText(Locale.getString("s_gender")+": "+utilities.filter(info.gender));
		age.setText(Locale.getString("s_age")+": "+String.valueOf(info.age_));
		return item;
	}
}
