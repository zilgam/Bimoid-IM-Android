package ru.ivansuper.base;

import ru.ivansuper.BimoidInterface.Interface;
import ru.ivansuper.bimoidim.SmileysManager;
import ru.ivansuper.bimoidim.XStatus;
import ru.ivansuper.bimoidim.debug;
import ru.ivansuper.bimoidim.resources;
import ru.ivansuper.bimoidproto.AccountInfoContainer;
import android.app.Application;

public class loader extends Application {
	public void onCreate(){
		super.onCreate();
    	label:{
    		if(resources.service != null) break label;
    		if(resources.ctx != null) break label;
    		resources.init(this);
	    	ru.ivansuper.locale.Locale.prepare();
	    	AccountInfoContainer.preloadLocale();
	    	Interface.init();
	    	SmileysManager.loadFromAssets();
	    	XStatus.init();
	        if(!debug.initialized){
	        	debug.init();
	        	debug.initialized = true;
	        }
    	}
	}
}
