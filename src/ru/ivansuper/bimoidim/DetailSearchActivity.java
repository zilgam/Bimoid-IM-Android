package ru.ivansuper.bimoidim;

import java.util.Vector;

import ru.ivansuper.BimoidInterface.ColorScheme;
import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidproto.AccountInfoContainer;
import ru.ivansuper.bimoidproto.BimoidProfile;
import ru.ivansuper.bimoidproto.Group;
import ru.ivansuper.bservice.BimoidService;
import ru.ivansuper.locale.Locale;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class DetailSearchActivity extends Activity implements Callback{
    private BimoidService service;
    private ListView search_results;
    private Button set_criteries;
    private Button begin_search;
    private AccountInfoContainer search_criteries = new AccountInfoContainer();
    private static final int GENDER = 0;
    private static final int AGE = 1;
    private static final int ZODIAC = 2;
    private static final int COUNTRY = 3;
    private static final int LANGUAGE = 4;
    private int select_info_type;
    public static boolean VISIBLE;
    private Dialog progress_dialog;
    private SearchResultsAdapter adapter = new SearchResultsAdapter();
    private Handler searchHdl = new Handler(this);
    private BufferedDialog dialog_for_display;
    public static final int HANDLE_SEARCH_RESULT = 0;
    public static final int HANDLE_SEARCH_END = 1;
    public static final int HANDLE_NOTIFICATION = 2;
	private TextView gender_preview;
	private TextView age_preview;
	private TextView zodiac_preview;
	private TextView country_preview;
	private TextView language_preview;
	private AccountInfoContainer context_item;
	private BimoidProfile profile;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_search);
        setVolumeControlStream(0x3);
        initViews();
       	service = resources.service;
		handleServiceConnected();
    }
    @Override
    public void onResume(){
    	VISIBLE = true;
    	super.onResume();
    }
    @Override
    public void onPause(){
    	VISIBLE = false;
    	super.onPause();
    }
    private void initViews(){
	    if(ColorScheme.initialized) ((LinearLayout)findViewById(R.id.search_back)).setBackgroundColor(ColorScheme.getColor(29));
    	if(ColorScheme.initialized) utilities.setLabel(((TextView)findViewById(R.id.detail_search_header)), "s_detail_search_header").setTextColor(ColorScheme.getColor(3));
    	if(ColorScheme.initialized) ((LinearLayout)findViewById(R.id.detail_search_divider)).setBackgroundColor(ColorScheme.getColor(4));
    	search_results = (ListView)findViewById(R.id.detail_search_result_list);
		Interface.attachSelector(search_results);
    	search_results.setAdapter(adapter);
    	search_results.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				context_item = adapter.getItem(arg2);
				removeDialog(3);
				showDialog(3);
				return false;
			}
    	});
    	set_criteries = (Button)findViewById(R.id.detail_search_set_criteries);
	    if(ColorScheme.initialized) set_criteries.setTextColor(ColorScheme.getColor(24));
	    Interface.attachButtonStyle(set_criteries);
    	set_criteries.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				removeDialog(0);
				showDialog(0);
			}
    	});
    	set_criteries.setText(Locale.getString("s_set_detail_search_params"));
    	begin_search = (Button)findViewById(R.id.detail_search_begin_search);
	    if(ColorScheme.initialized) begin_search.setTextColor(ColorScheme.getColor(24));
	    Interface.attachButtonStyle(begin_search);
	    begin_search.setText(Locale.getString("s_do_search"));
	    Interface.attachBackground((LinearLayout)findViewById(R.id.detail_search_header_field), Interface.search_top_panel);
	    Interface.attachBackground((LinearLayout)findViewById(R.id.detail_search_results_field), Interface.search_list_back);
    }
    private void handleServiceConnected(){
		Intent i = getIntent();
    	profile = service.profiles.getProfileByID(i.getStringExtra("PID"));
    	service.searchHdl = searchHdl;
    	begin_search.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(service.profiles.getConnectedProfilesCount() == 0) return;
				progress_dialog = DialogBuilder.createProgress(DetailSearchActivity.this, Locale.getString("s_search_in_progress"), true);
				progress_dialog.show();
				adapter.clear();
				if(profile == null){
					progress_dialog.dismiss();
					return;
				}
				profile.sendSearchRequest(search_criteries);
			}
    	});
    }
    protected Dialog onCreateDialog(final int type){
    	Dialog dialog = null;
    	switch(type){
    	case 0:
    		LinearLayout lay = (LinearLayout)View.inflate(this, R.layout.detail_criteries_dialog, null);
    		if(ColorScheme.initialized){
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l1)), "s_search_params_1").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_search_params_2").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l3)), "s_search_params_3").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l4)), "s_search_params_4").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l5)), "s_search_params_5").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l6)), "s_search_params_6").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l7)), "s_search_params_7").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l8)), "s_search_params_8").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l9)), "s_search_params_9").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l10)), "s_search_params_10").setTextColor(ColorScheme.getColor(12));
    		}
    		final EditText nickname = (EditText)lay.findViewById(R.id.details_dialog_nickname);
    		if(ColorScheme.initialized) nickname.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(nickname);
    		nickname.setText(search_criteries.nick_name);
    		final EditText firstname = (EditText)lay.findViewById(R.id.details_dialog_firstname);
    		if(ColorScheme.initialized) firstname.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(firstname);
    		firstname.setText(search_criteries.first_name);
    		final EditText lastname = (EditText)lay.findViewById(R.id.details_dialog_lastname);
    		if(ColorScheme.initialized) lastname.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(lastname);
    		lastname.setText(search_criteries.last_name);
    		final EditText city = (EditText)lay.findViewById(R.id.details_dialog_city);
    		if(ColorScheme.initialized) city.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(city);
    		city.setText(search_criteries.city);
    		final EditText interests = (EditText)lay.findViewById(R.id.details_dialog_interests);
    		if(ColorScheme.initialized) interests.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(interests);
    		interests.setText(search_criteries.interests);
    		final Button gender = (Button)lay.findViewById(R.id.details_dialog_gender);
    	    if(ColorScheme.initialized) gender.setTextColor(ColorScheme.getColor(24));
    	    Interface.attachButtonStyle(gender);
    		gender.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					select_info_type = GENDER;
					removeDialog(2);
					showDialog(2);
				}
    		});
    		gender.setText(Locale.getString("s_do_select"));
    		final Button age = (Button)lay.findViewById(R.id.details_dialog_age);
    	    if(ColorScheme.initialized) age.setTextColor(ColorScheme.getColor(24));
    	    Interface.attachButtonStyle(age);
    		age.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					select_info_type = AGE;
					removeDialog(2);
					showDialog(2);
				}
    		});
    		age.setText(Locale.getString("s_do_select"));
    		final Button zodiac = (Button)lay.findViewById(R.id.details_dialog_zodiac);
    	    if(ColorScheme.initialized) zodiac.setTextColor(ColorScheme.getColor(24));
    	    Interface.attachButtonStyle(zodiac);
    		zodiac.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					select_info_type = ZODIAC;
					removeDialog(2);
					showDialog(2);
				}
    		});
    		zodiac.setText(Locale.getString("s_do_select"));
    		final Button country = (Button)lay.findViewById(R.id.details_dialog_country);
    	    if(ColorScheme.initialized) country.setTextColor(ColorScheme.getColor(24));
    	    Interface.attachButtonStyle(country);
    		country.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					select_info_type = COUNTRY;
					removeDialog(2);
					showDialog(2);
				}
    		});
    		country.setText(Locale.getString("s_do_select"));
    		final Button language = (Button)lay.findViewById(R.id.details_dialog_language);
    	    if(ColorScheme.initialized) language.setTextColor(ColorScheme.getColor(24));
    	    Interface.attachButtonStyle(language);
    		language.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					select_info_type = LANGUAGE;
					removeDialog(2);
					showDialog(2);
				}
    		});
    		language.setText(Locale.getString("s_do_select"));
    		gender_preview = (TextView)lay.findViewById(R.id.details_dialog_gender_preview);
    		if(ColorScheme.initialized) gender_preview.setTextColor(ColorScheme.getColor(12));
    		gender_preview.setText(AccountInfoContainer.genders[search_criteries.gender_]);
    		age_preview = (TextView)lay.findViewById(R.id.details_dialog_age_preview);
    		if(ColorScheme.initialized) age_preview.setTextColor(ColorScheme.getColor(12));
    		age_preview.setText(AccountInfoContainer.ages[search_criteries.age_]);
    		zodiac_preview = (TextView)lay.findViewById(R.id.details_dialog_zodiac_preview);
    		if(ColorScheme.initialized) zodiac_preview.setTextColor(ColorScheme.getColor(12));
    		zodiac_preview.setText(AccountInfoContainer.zodiacs[search_criteries.zodiac_]);
    		country_preview = (TextView)lay.findViewById(R.id.details_dialog_country_preview);
    		if(ColorScheme.initialized) country_preview.setTextColor(ColorScheme.getColor(12));
    		country_preview.setText(AccountInfoContainer.countries[search_criteries.country_]);
    		language_preview = (TextView)lay.findViewById(R.id.details_dialog_language_preview);
    		if(ColorScheme.initialized) language_preview.setTextColor(ColorScheme.getColor(12));
    		language_preview.setText(AccountInfoContainer.languages[search_criteries.language_]);
    		final CheckBox only_online = (CheckBox)lay.findViewById(R.id.details_dialog_only_online);
    		if(ColorScheme.initialized) only_online.setTextColor(ColorScheme.getColor(12));
    		Interface.attachCheckBoxStyle(only_online);
    		only_online.setChecked(search_criteries.online_);
    		only_online.setText(Locale.getString("s_search_params_11"));
    		dialog = DialogBuilder.createYesNo(DetailSearchActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_search_params"),
            		Locale.getString("s_apply"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						search_criteries.nick_name = nickname.getText().toString().trim();
    						search_criteries.first_name = firstname.getText().toString().trim();
    						search_criteries.last_name = lastname.getText().toString().trim();
    						search_criteries.city = city.getText().toString().trim();
    						search_criteries.interests = interests.getText().toString().trim();
    						search_criteries.online_ = only_online.isChecked();
    						removeDialog(0);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(0);
    					}
    				});
    		break;
    	case 0x1://Notify dialog
    		if(dialog_for_display == null) return null;
        	dialog = DialogBuilder.createOk(DetailSearchActivity.this,
        			dialog_for_display.header, dialog_for_display.text, Locale.getString("s_close"),
        			Gravity.TOP, new OnClickListener(){
    					@Override
    					public void onClick(View v) {
    						removeDialog(type);
    					}
    		});
        	break;
    	case 0x2://Parameters selector
    		lay = (LinearLayout)View.inflate(resources.ctx, R.layout.selector_dialog, null);
    		final EditText filter = (EditText)lay.findViewById(R.id.selector_dialog_search);
    		if(ColorScheme.initialized) filter.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(filter);
    		final ListView list = (ListView)lay.findViewById(R.id.selector_dialog_list);
    		list.setDividerHeight(0);
    		Interface.attachSelector(list);
    		final UAdapter adapter_ = new UAdapter();
    		adapter_.setPadding(5);
    		switch(select_info_type){
    		case GENDER:
    			filter.setVisibility(View.GONE);
        		adapter_.put(AccountInfoContainer.genders);
    			break;
    		case AGE:
    			filter.setVisibility(View.GONE);
        		adapter_.put(AccountInfoContainer.ages);
    			break;
    		case ZODIAC:
    			filter.setVisibility(View.GONE);
        		adapter_.put(AccountInfoContainer.zodiacs);
    			break;
    		case COUNTRY:
        		adapter_.put(AccountInfoContainer.countries);
    			break;
    		case LANGUAGE:
        		adapter_.put(AccountInfoContainer.languages);
    			break;
    		}
    		filter.addTextChangedListener(new TextWatcher(){
				@Override
				public void afterTextChanged(Editable s) {
				}
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					adapter_.setFilter(filter.getText().toString());
				}
    		});
        	list.setCacheColorHint(0x00000000);
    		list.setAdapter(adapter_);
    		list.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		    		switch(select_info_type){
		    		case GENDER:
		    			search_criteries.gender_ = arg2;
		    			gender_preview.setText(AccountInfoContainer.genders[search_criteries.gender_]);
		    			break;
		    		case AGE:
		    			search_criteries.age_min = AccountInfoContainer.ages_mins[arg2];
		    			search_criteries.age_max = AccountInfoContainer.ages_maxs[arg2];
		    			search_criteries.age_ = arg2;
		    			age_preview.setText(AccountInfoContainer.ages[search_criteries.age_]);
		    			break;
		    		case ZODIAC:
		    			search_criteries.zodiac_ = arg2;
		    			zodiac_preview.setText(AccountInfoContainer.zodiacs[search_criteries.zodiac_]);
		    			break;
		    		case COUNTRY:
		    			search_criteries.country_ = (int)adapter_.getItemId(arg2);
		    			country_preview.setText(AccountInfoContainer.countries[search_criteries.country_]);
		    			break;
		    		case LANGUAGE:
		    			search_criteries.language_ = (int)adapter_.getItemId(arg2);
		    			language_preview.setText(AccountInfoContainer.languages[search_criteries.language_]);
		    			break;
		    		}
					removeDialog(2);
				}
    		});
        	dialog = DialogBuilder.createWithNoHeader(DetailSearchActivity.this,
        			lay,
    				Gravity.CENTER);
    		break;
    	case 0x3://Search results context
        	UAdapter adapter = new UAdapter();
        	adapter.setPadding(10);
        	adapter.put(resources.context_menu_icon, Locale.getString("s_add_contact"), 0);
        	dialog = DialogBuilder.create(DetailSearchActivity.this,
        			Locale.getString("s_menu"),
        			adapter,
    				Gravity.CENTER,
    				new OnItemClickListener(){
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int arg2, long arg3) {
							removeDialog(3);
							removeDialog(4);
							showDialog(4);
						}
        			});
    		break;
    	case 0x4://Add contact dialog
    		if(context_item == null) break;
    		lay = (LinearLayout)View.inflate(this, R.layout.add_contact_dialog, null);
    		if(ColorScheme.initialized){
    			((TextView)lay.findViewById(R.id.l1)).setVisibility(View.GONE);
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l2)), "s_contact_account").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l3)), "s_contact_name").setTextColor(ColorScheme.getColor(12));
    			utilities.setLabel(((TextView)lay.findViewById(R.id.l4)), "s_select_group").setTextColor(ColorScheme.getColor(12));
    		}
    		final ListView profiles = (ListView)lay.findViewById(R.id.add_contact_dialog_profiles);
    		profiles.setVisibility(View.GONE);
    		Interface.attachBackground(profiles, Interface.list_view_back);
    		Interface.attachSelector(profiles);
    		final UAdapter adapter_a = new UAdapter();
			Intent i = getIntent();
			adapter_a.put(i.getStringExtra("PID"), 0);
    		profiles.setAdapter(adapter_a);
    		final EditText account = (EditText)lay.findViewById(R.id.add_contact_dialog_account);
    		if(ColorScheme.initialized) account.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(account);
    		account.setText(context_item.account_);
    		account.setSelection(0, context_item.account_.length());
    		final EditText nickname_ = (EditText)lay.findViewById(R.id.add_contact_dialog_name);
    		if(ColorScheme.initialized) nickname_.setTextColor(ColorScheme.getColor(13));
        	Interface.attachEditTextStyle(nickname_);
    		String nick = utilities.unfilter(context_item.nick_name);
    		nickname_.setText(nick);
    		nickname_.setSelection(0, nick.length());
    		final ListView groups = (ListView)lay.findViewById(R.id.add_contact_dialog_groups);
    		Interface.attachBackground(groups, Interface.list_view_back);
    		Interface.attachSelector(groups);
    		final UAdapter adapter_b = new UAdapter();
    		groups.setAdapter(adapter_b);
    		
    		
			adapter_a.setSelected(0);
			Vector<Group> list1 = profile.getGroups();
			adapter_b.clear();
			adapter_b.put("["+Locale.getString("s_without_group")+"]", 0);
    		for(int j=0; j<list1.size(); j++)
    			adapter_b.put(list1.get(j).getName(), list1.get(j).getItemId());
    		
			adapter_b.setSelected(0);
    		
    		groups.setOnItemClickListener(new OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					adapter_b.setSelected(arg2);
				}
    		});
    		dialog = DialogBuilder.createYesNo(DetailSearchActivity.this,
    				lay,
    				Gravity.TOP,
    				Locale.getString("s_add_contact"),
            		Locale.getString("s_do_add"), Locale.getString("s_cancel"),
            		new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						if(adapter_a.getSelectedIdx() < 0) return;
    						if(adapter_b.getSelectedIdx() < 0) return;
    						String account_ = account.getText().toString().toLowerCase().trim();
    						if(account_.length() == 0) return;
    						String nickname__ = nickname_.getText().toString().trim();
    						if(nickname__.length() == 0) nickname__ = account_;
    						int parent_id = (int)adapter_b.getItemId(adapter_b.getSelectedIdx());
    						if(profile == null) return;
    						profile.addContact(account_, nickname__, parent_id);
    						removeDialog(4);
    					}
    				},
    				new OnClickListener(){
    					@Override
    					public void onClick(View arg0) {
    						removeDialog(4);
    					}
    				});
    		break;
    	}
    	return dialog;
    }
	@Override
	public boolean handleMessage(Message arg0) {
		switch(arg0.what){
		case HANDLE_SEARCH_RESULT:
			AccountInfoContainer result = (AccountInfoContainer)arg0.obj;
			adapter.put(result);
			break;
		case HANDLE_SEARCH_END:
			progress_dialog.dismiss();
			break;
		case HANDLE_NOTIFICATION:
			progress_dialog.dismiss();
			dialog_for_display = (BufferedDialog)arg0.obj;
			if(VISIBLE){
				removeDialog(1);
				showDialog(1);
			}
			break;
		}
		return false;
	}
}
