package ru.ivansuper.bimoidim;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;

public class Media {
	public static final int INC_MSG = 0x0;
	public static final int AUTH_ACCEPTED = 0x1;
	public static final int AUTH_DENIED = 0x2;
	public static final int AUTH_REQUEST = 0x3;
	public static final int AUTH_REVOKED = 0x9;
	public static final int CONTACT_IN = 0x4;
	public static final int CONTACT_OUT = 0x5;
	public static final int OUT_MSG = 0x6;
	public static final int INFO_MSG = 0x7;
	public static final int SVC_MSG = 0x8;
	private MediaPlayer mp = new MediaPlayer();
	public static int ring_mode = 0x0;
	public static boolean silent_mode;
	public void playEvent(int event){
		if(ring_mode != 0x0) return;
		if(silent_mode == true) return;
		try{
			switch(event){
			case INC_MSG:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("inc_msg_s", true)) break;
				mp.reset();
				AssetFileDescriptor ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_inc_msg);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case AUTH_ACCEPTED:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("auth_a_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_auth_grant);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case AUTH_DENIED:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("auth_d_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_auth_deny);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case AUTH_REQUEST:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("auth_r_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_auth_req);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case AUTH_REVOKED:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("auth_re_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_auth_rev);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case CONTACT_IN:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("in_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_online);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case CONTACT_OUT:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("out_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_offline);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case OUT_MSG:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("sent_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_msg_sent);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case INFO_MSG:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("info_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_info_msg);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			case SVC_MSG:
				if(!PreferenceManager.getDefaultSharedPreferences(resources.ctx).getBoolean("svc_s", true)) break;
				mp.reset();
				ad = resources.ctx.getResources().openRawResourceFd(R.raw.snd_svc_msg);
				mp.setDataSource(ad.getFileDescriptor(), ad.getStartOffset(), ad.getLength());
				ad.close();
				mp.prepare();
				mp.setLooping(false);
				mp.start();
				break;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
