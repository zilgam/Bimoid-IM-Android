package ru.ivansuper.bimoidim;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ivansuper.bimoidproto.Contact;
import ru.ivansuper.locale.Locale;
import ru.ivansuper.socket.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;
public class utilities {
	public static String chars = "0123456789AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz"+
	"јаЅб¬в√гƒд≈е®Є∆ж«з»и…й кЋлћмЌнќоѕп–р—с“т”у‘ф’х÷ц„чЎшўщЏъџы№ьЁэёюя€";
    public static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) 
                    buf.append((char) ('0' + halfbyte));
                else 
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        return buf.toString();
    } 
    public static byte[] getHashArray(String key, String ID, String password) throws Exception{
        byte[] id = getStringUTF8(ID.toLowerCase());
        byte[] md5buf = new byte[512];
        int writePos = 0;
        System.arraycopy(id, 0, md5buf, 0, id.length);
        writePos += id.length;
        
        byte[] obimp = getStringUTF8("OBIMPSALT");
        System.arraycopy(obimp, 0, md5buf, writePos, obimp.length);
        writePos += obimp.length;
        
        byte[] pass = getStringUTF8(password);
        System.arraycopy(pass, 0, md5buf, writePos, pass.length);
        writePos += pass.length;
        
        byte[] first_hash = MD5.calculateMD5(ByteBuffer.normalizeBytes(md5buf, writePos));

        byte[] key_ = getStringUTF8(key);
        
        byte[] second_md5buf = new byte[first_hash.length+key_.length];
        
        System.arraycopy(first_hash, 0, second_md5buf, 0, first_hash.length);
        System.arraycopy(key_, 0, second_md5buf, first_hash.length, key_.length);
        
        byte[] final_hash = MD5.calculateMD5(second_md5buf);
        return final_hash;
    }
    public static byte[] getStringUTF8(String source){
    	ByteBuffer buf = new ByteBuffer(source.length()*2);
    	byte[] res = null;
		try {
	    	buf.writeStringUTF8(source);
		} catch (Exception e) {
			e.printStackTrace();
		}
		res = ByteBuffer.normalizeBytes(buf.bytes, buf.writePos);
    	return res;
    }
    public static byte[] hexStringToBytesArray(String paramString){
      int i = paramString.length();
      if (i % 2 != 0)
        throw new IllegalArgumentException("Input string must contain an even number of characters");
      byte[] arrayOfByte = new byte[paramString.length() / 2];
      int j = 0;
      while (true){
        if (j >= i)
          return arrayOfByte;
        int k = j / 2;
        int m = j + 2;
        byte n = (byte)Integer.parseInt(paramString.substring(j, m), 16);
        arrayOfByte[k] = n;
        j += 2;
      }
    }
    public static boolean arrayEquals(byte[] what, byte[] with){
    	int len = what.length;
    	for(int i=0; i<len; i++){
    		if(what[i] != with[i]){
    			return false;
    		}
    	}
    	return true;
    }
    public static boolean isUnicode(byte a, byte b, byte c){
    	if((a <= 0xA) && (b > 0xA) && (c <= 0xA)){
    		return true;
    	}
    	return false;
    }
    public static String longitudeToString(long seconds) {
        int days = (int)(seconds / 86400);
        seconds %= 86400;
        int hours = (int)(seconds / 3600);
        seconds %= 3600;
        int minutes = (int)(seconds / 60);
        seconds %= 60;
        StringBuffer buf = new StringBuffer();
        buf.append(days);
        buf.append(" дней ");
        buf.append(hours);
        buf.append(" часов ");
        buf.append(minutes);
        buf.append(" минут ");
        buf.append(seconds);
        buf.append(" секунд");
        return buf.toString();
    }
    public static boolean isEmptyForDisplay(String source){
    	String b = source.replaceAll(" ", "").replaceAll("\n", "");
    	return b.length() == 0;
    }
	public static String formatBirthdayTimestamp(long timestamp){
		Date dt = new Date(timestamp);
		String format = "dd/MM/yyyy";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(dt);
	}
	public static String filter(String source){
		if(source.trim().length() == 0){
			return " - ";
		}else{
			return source;
		}
	}
	public static String unfilter(String source){
		if(source.trim().equals(" - ")){
			return "";
		}else{
			return source;
		}
	}
	public static int getHash(Contact contact){
		if(contact == null) return 0;
		return (contact.getID()+contact.getTransportId()+contact.getProfile().ID).hashCode()-0xffff;
	}
	public static boolean contactEquals(Contact contact, Contact contact_){
		if(contact == null) return false;
		if(contact_ == null) return false;
		return contact.equals(contact_);
	}
	public static byte[] generateUniqueID(){
		final byte[] unique_id = new byte[8];
		final long timestamp = System.currentTimeMillis();
		final String key = "BIMOID_ANDROID_SALT_"+Long.toHexString(timestamp)+"_ABCDEF_"+String.valueOf(timestamp-(timestamp%0xffff));
		final byte[] md5 = MD5.calculateMD5(key.getBytes());
		System.arraycopy(md5, 0, unique_id, 0, 8);
		return unique_id;
	}
	public static final Bitmap downloadImage(String address){
		try{
			address = address.substring(address.indexOf("http://"));
	        URL url = new URL(address);
			HttpURLConnection c = (HttpURLConnection)url.openConnection();
			Log.e("HTTPResponse", ""+c.getResponseCode());
			if(c.getResponseCode() == HttpURLConnection.HTTP_OK){
				final Bitmap bitmap = BitmapFactory.decodeStream(c.getInputStream());
				bitmap.setDensity(0);
				return bitmap;
			}else{
				return null;
			}
		}catch(Exception e){
			e.printStackTrace();
			Log.e("HTTPResponse", "Error occured!"+e.getMessage());
		}
		return null;
	}
	public static final Bitmap downloadImage(String address, float scale_factor){
		try{
			address = address.substring(address.indexOf("http://"));
	        URL url = new URL(address);
			HttpURLConnection c = (HttpURLConnection)url.openConnection();
			Log.e("HTTPResponse", ""+c.getResponseCode());
			if(c.getResponseCode() == HttpURLConnection.HTTP_OK){
				Bitmap bitmap = BitmapFactory.decodeStream(c.getInputStream());
				bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()*scale_factor), (int)(bitmap.getHeight()*scale_factor), true);
				bitmap.setDensity(0);
				return bitmap;
			}else{
				return null;
			}
		}catch(Exception e){
			e.printStackTrace();
			Log.e("HTTPResponse", "Error occured!"+e.getMessage());
		}
		return null;
	}
	public static final long correctTimestamp(long source){
    	Calendar c = Calendar.getInstance();
    	source += c.get(Calendar.ZONE_OFFSET)+c.get(Calendar.DST_OFFSET);
    	return source;
	}
	public static final String compute(final String manufact, final String model){
		final String manufact_ = manufact.trim().toLowerCase();
		final String model_ = model.trim().toLowerCase();
		String result = "";
		int total = manufact_.length();
		if(model_.length() < total) total = model_.length();
		int matches = 0;
		for(int i=0; i<total; i++){
			if(manufact_.charAt(i) == model_.charAt(i)) matches++;
		}
		final int percentage = matches*100/total;
		if(percentage > 20){
			result = model;
		}else{
			result = manufact+" "+model;
		}
		return result;
	}
	public static final String match(String source, String[] parts){
		String result = source;
		for(int i=0; i< parts.length; i++){
			//Log.e("Matcher", "Source: %%%"+String.valueOf(i+1)+"  Dest: "+parts[i]);
			result = replace(result, "%%%"+String.valueOf(i+1), parts[i]);
		}
		return result;
	}
	public static final String replace(String source, String part, String new_part){
		String result = "";
		Pattern pattern = Pattern.compile(part, Pattern.LITERAL);
		Matcher matcher = pattern.matcher(source);
		result = matcher.replaceAll(new_part);
		return result;
	}
	public static final String[] split(String source, String determiner){
		if(source == null) return new String[]{};
		if(determiner == null) return new String[]{};
		Pattern p = Pattern.compile(determiner, Pattern.LITERAL);
		return p.split(source);
	}
	public static final Drawable setDrawableBounds(Drawable source){
		source.setBounds(0, 0, source.getIntrinsicWidth(), source.getIntrinsicHeight());
		return source;
	}
	public static final TextView setLabel(TextView view, String text){
		view.setText(Locale.getString(text));
		return view;
	}
	public static final String getStackTraceString(Throwable tr) {
		if (tr == null) return ""; 
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		tr.printStackTrace(pw);
		String result = sw.toString();
		return result.trim(); 
	}
}
