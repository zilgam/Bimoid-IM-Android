package ru.ivansuper.bimoidim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import android.preference.PreferenceManager;
import android.util.Log;

public class debug {
	public static boolean initialized;
	public static void init(){
		exception_hdl hdl = new exception_hdl();
		Thread.setDefaultUncaughtExceptionHandler(hdl);
	}
	public static class exception_hdl implements UncaughtExceptionHandler{
		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			try{
				Log.e("BimoidIM:stack_dump", "Exception handled! Saving stack trace ...");
				if(!resources.sd_mounted()) return;
				String unique_id = String.valueOf(System.currentTimeMillis());
				unique_id = unique_id.substring(unique_id.length()-7, unique_id.length());
				File dump = new File(resources.DATA_PATH+"stack_trace_"+unique_id+".st");
				if(!dump.exists())
					dump.createNewFile();
				PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(dump)));
				out.print("=== Bimoid Stack Dump File ==="+"\n");
				out.print("BOARD: "+android.os.Build.BOARD+"\n");
				out.print("BRAND: "+android.os.Build.BRAND+"\n");
				out.print("FINGERPRINT: "+android.os.Build.FINGERPRINT+"\n");
				out.print("ID: "+android.os.Build.ID+"\n");
				out.print("MANUFACTURER: "+android.os.Build.MANUFACTURER+"\n");
				out.print("MODEL: "+android.os.Build.MODEL+"\n");
				out.print("PRODUCT: "+android.os.Build.PRODUCT+"\n");
				out.print("TAGS: "+android.os.Build.TAGS+"\n");
				out.print("TYPE: "+android.os.Build.TYPE+"\n");
				out.print("USER: "+android.os.Build.USER+"\n\n");
				out.print("OS Version: SDK:"+android.os.Build.VERSION.SDK_INT+"/RELEASE:"+android.os.Build.VERSION.RELEASE+"\n\n");
				out.print("Bimoid IM Version: "+resources.VERSION+"\n\n");
				ex.printStackTrace(out);
				Log.e("BimoidIM", "Error occured", ex);
				out.close();
				File marker = new File(resources.DATA_PATH+"ForceClosed.marker");
				marker.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
			}
			Runtime.getRuntime().exit(0);
		}
	}
}
