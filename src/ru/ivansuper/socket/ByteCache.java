
package ru.ivansuper.socket;

import java.util.Collections;
import java.util.Vector;

import android.util.Log;

public class ByteCache {
	private static Vector<byte[]> buffer = new Vector<byte[]>();
	static{
		for(int i=0; i<150; i++){
			byte[] small_array = new byte[128];
			buffer.add(small_array);
		}
		for(int i=0; i<100; i++){
			byte[] medium_array = new byte[256];
			buffer.add(medium_array);
		}
		for(int i=0; i<50; i++){
			byte[] normal_array = new byte[512];
			buffer.add(normal_array);
		}
		for(int i=0; i<20; i++){
			byte[] big_array = new byte[1024];
			buffer.add(big_array);
		}
		for(int i=0; i<32; i++){
			byte[] very_big_array = new byte[0x4000];
			buffer.add(very_big_array);
		}
		for(int i=0; i<5; i++){
			byte[] very_big_array2 = new byte[0x8000];
			buffer.add(very_big_array2);
		}
	}
	public static void recycle(byte[] array){
		if(array == null) return;
		synchronized(buffer){
			//for(int i=0; i<array.length; i++) array[i] = 0;
			buffer.addElement(array);
			//Log.i("ByteRecycler", "Recycled size: "+array.length+" / Total count: "+buffer.size());
		}
	}
	public static byte[] getByteArray(int desired_length){
		synchronized(buffer){
			for(byte[] array: buffer){
				if(array.length >= desired_length){
					buffer.removeElement(array);
					//Log.i("ByteCache", "Array used from cache");
					return array;
				}
			}
			//Log.e("ByteCache", "Used new array [length: "+desired_length+"]");
			return new byte[desired_length];
		}
	}
}
