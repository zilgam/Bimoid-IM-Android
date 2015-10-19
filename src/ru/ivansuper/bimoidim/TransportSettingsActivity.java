package ru.ivansuper.bimoidim;

import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.transports.Transport;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.ui.TabsContentHolder;
import ru.ivansuper.ui.TabsContentHolder.TabContent;
import ru.ivansuper.ui.TabsHeaders;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class TransportSettingsActivity extends Activity {
	
	private static BimoidProfile mProfile;
	private static Transport mTransport;
	
	public static final void launch(Activity parent, Transport transport){
		
		mProfile = transport.getProfile();
		mTransport = transport;
		
		final Intent intent = new Intent(parent, TransportSettingsActivity.class);
		parent.startActivity(intent);
		
	}
	
	private TabsContentHolder mTabsContentHolder;
	
	@Override
	public void onCreate(Bundle state){
		super.onCreate(state);
		
		setContentView(R.layout.transport_settings_layout);
		
		findViews();
		
	}
	
	private final void findViews(){
		
		((TextView)findViewById(R.id.l1)).setText(mTransport.account_name);
		
		final Button ok_button = (Button)findViewById(R.id.transport_settings_ok_btn);
		ok_button.setText(Locale.getString("s_ok"));
		ok_button.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				
				mTransport.updateValuesFromForm(mTabsContentHolder);
				mProfile.saveTransportAccount(mTransport);
				mProfile.updateTransportParams(mTransport);
				
				finish();
				
			}
		});
		
		final Button cancel_button = (Button)findViewById(R.id.transport_settings_cancel_btn);
		cancel_button.setText(Locale.getString("s_cancel"));
		cancel_button.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				
				finish();
				
			}
		});
		
		mTabsContentHolder = (TabsContentHolder)findViewById(R.id.transport_settings_tabs_content);
		
		final TabsHeaders tabs_headers = (TabsHeaders)findViewById(R.id.transport_settings_tabs_headers);
		
		mTabsContentHolder.attachTabsHeaders(tabs_headers);
		
		buildTabs();
		
	}
	
	private final void buildTabs(){
		
		mTransport.buildSettingsGUI(mTabsContentHolder);
		
	}
	
}
