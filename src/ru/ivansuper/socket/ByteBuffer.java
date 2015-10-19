package ru.ivansuper.socket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.System;
import java.sql.Date;

import ru.ivansuper.bimoidim.utilities;

import android.util.Log;

public class ByteBuffer {
	public byte[] bytes;
	public int writePos;
	public int readPos;
	public ByteBuffer(){
		bytes = new byte[0x4000];
		writePos = 0;
		readPos = 0;
	}
	public ByteBuffer(int bufferLength){
		bytes = new byte[bufferLength];
		writePos = 0;
		readPos = 0;
	}
	public ByteBuffer(int bufferLength, boolean plomb){
		bytes = ByteCache.getByteArray(bufferLength);
		writePos = 0;
		readPos = 0;
	}
	public ByteBuffer(byte[] source){
		bytes = source;
		writePos = source.length;
		readPos = 0;
	}
	public ByteBuffer(byte[] source, boolean not_clone, int length){
		bytes = source;
		writePos = length;
		readPos = 0;
	}
	public ByteBuffer(boolean flag){
		bytes = null;
		writePos = 0;
		readPos = 0;
	}
	public void double_increase(){
		byte[] array = new byte[bytes.length*2];
		System.arraycopy(bytes, 0, array, 0, bytes.length);
		bytes = array;
	}
	public final int getBytesCountAvailableToRead(){
		return writePos - readPos;
	}
	public final byte[] readBytes(int len){
		final byte[] res = new byte[len];
		System.arraycopy(bytes, readPos, res, 0, len);
		readPos += len;
		return res;
	}
	public final byte[] readBytesA(int len){
		final byte[] res = ByteCache.getByteArray(len);
		System.arraycopy(bytes, readPos, res, 0, len);
		readPos += len;
		return res;
	}
	public final byte readByte(){
		byte res = bytes[readPos];
		readPos += 1;
		return res;
	}
	public boolean readBoolean() {
		int i = readByte();
		return i == 1;
	}
	public void writeBoolean(boolean value) {
		if(value){
			writeByte((byte)1);
		}else{
			writeByte((byte)0);
		}
	}
	public long readUNIXEpochTimeStamp() {
		skip(4);
		long timestamp = readDWord();
		if(timestamp == 0x7C52DE80) return 0;
		Date dt = new Date(System.currentTimeMillis());
		timestamp += dt.getTimezoneOffset()*60;
		timestamp *= 1000;
		return timestamp;
	}
	public long readUNIXEpochTimeStampNoCorrection() {
		skip(4);
		long timestamp = readDWord();
		if(timestamp == 0x7C52DE80) return 0;
		timestamp *= 1000;
		return timestamp;
	}
	public final byte previewByte(int offset){
		byte res = bytes[readPos+offset];
		return res;
	}
	public final int readWord(){
		int j = readPos;
	    readPos += 2;
	    int i1 = (bytes[j] & 0xFF) << 8;
	    int i3 = bytes[j + 1] & 0xFF | i1;
	    return i3;
	}
	public final int previewWord(int offset){
		int j = readPos + offset;
	    int i1 = (bytes[j] & 0xFF) << 8;
	    int i3 = bytes[j + 1] & 0xFF | i1;
	    return i3;
	}
	public final int readDWord(){
		int j = readPos;
	    readPos += 4;
	    int i1 = (bytes[j] & 0xFF) << 24;
	    int i2 = j + 1;
	    int i3 = (bytes[i2] & 0xFF) << 16;
	    int i4 = i1 | i3;
	    int i5 = i2 + 1;
	    int i6 = (bytes[i5] & 0xFF) << 8;
	    int i7 = i4 | i6;
	    int i8 = i5 + 1;
	    int i = bytes[i8];
	    int i9 = i & 0xFF | i7;
	    return i9;
	}
	public final int previewDWord(int offset){
		int j = readPos + offset;
	    int i1 = (bytes[j] & 0xFF) << 24;
	    int i2 = j + 1;
	    int i3 = (bytes[i2] & 0xFF) << 16;
	    int i4 = i1 | i3;
	    int i5 = i2 + 1;
	    int i6 = (bytes[i5] & 0xFF) << 8;
	    int i7 = i4 | i6;
	    int i8 = i5 + 1;
	    int i = bytes[i8];
	    int i9 = i & 0xFF | i7;
	    return i9;
	}
	public final long readLong(){
		int j = readPos;
	    readPos += 8;
	    long c = (bytes[j] & 0xFF) << 56 | (bytes[j+1] & 0xFF) << 48 | (bytes[j+2] & 0xFF) << 40 | (bytes[j+3] & 0xFF) << 32 | (bytes[j+4] & 0xFF) << 24 | (bytes[j+5] & 0xFF) << 16 | (bytes[j+6] & 0xFF) << 8 | bytes[j+7] & 0xFF;
	    return c;
	}
	public final String readStringUTF8(int paramInt) {
		String str2 = "null";
		try{
			int rp = readPos;
			skip(paramInt);
			int i = paramInt + 2;
    		byte[] arrayOfByte = new byte[i];
    		System.arraycopy(bytes, rp, arrayOfByte, 2, paramInt);
    		byte j = (byte)(paramInt >> 8 & 0xFF);
    		arrayOfByte[0] = j;
    		byte k = (byte)(paramInt & 0xFF);
    		arrayOfByte[1] = k;
    		ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(arrayOfByte);
    		String str1 = new DataInputStream(localByteArrayInputStream).readUTF();
    		str2 = str1;
		}catch(Exception e){
			e.printStackTrace();
		}
    	return str2;
	}
   	public final String readIP(){
		StringBuilder ip = new StringBuilder();
		int val = readDWord();
		ip.append(val>>24 & 0xff);
		ip.append(".");
		ip.append(val>>16 & 0xff);
		ip.append(".");
		ip.append(val>>8 & 0xff);
		ip.append(".");
		ip.append(val & 0xff);
		return ip.toString();
	}
	public void write(byte[] source){
		System.arraycopy(source, 0, bytes, writePos, source.length);
		writePos += source.length;
	}
	public void write(byte[] source, int length){
		System.arraycopy(source, 0, bytes, writePos, length);
		writePos += source.length;
	}
	public final void writeByte(byte source){
		bytes[writePos] = source;
		writePos += 1;
	}
	public final void writeWord(int source){
	    int i = writePos;
	    int j = i + 1;
	    byte k = (byte)(source >> 8);
	    bytes[i] = k;
	    byte m = (byte)source;
	    bytes[j] = m;
	    int n = writePos + 2;
	    writePos = n;
	}
	public final void writeDWord(int source){
	    int i = writePos;
	    int j = i + 1;
	    byte k = (byte)(source >> 24);
	    bytes[i] = k;
	    int m = j + 1;
	    byte n = (byte)(source >> 16);
	    bytes[j] = n;
	    int i1 = m + 1;
	    byte i2 = (byte)(source >> 8);
	    bytes[m] = i2;
	    byte i3 = (byte)source;
	    bytes[i1] = i3;
	    writePos = writePos + 4;
	}
	public final void writeLong(long source){
		try{
			ByteArrayOutputStream btOut = new ByteArrayOutputStream(8);
			DataOutputStream out = new DataOutputStream(btOut);
			out.writeLong(source);
			System.arraycopy(btOut.toByteArray(), 0, bytes, writePos, 8);
			writePos += 8;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public final void writeStringUTF8(String source){
		try{
			ByteArrayOutputStream btOut = new ByteArrayOutputStream();
			DataOutputStream dtOut = new DataOutputStream(btOut);
			dtOut.writeUTF(source);
			byte[] bt = btOut.toByteArray();
			System.arraycopy(bt, 2, bytes, writePos, bt.length-2);
			writePos += bt.length-2;
			dtOut.close();
			btOut.close();
		}catch(Exception e){
			//e.printStackTrace();
		}
	}
	public final void writePreLenStringUTF8(String source){
		try{
			ByteArrayOutputStream btOut = new ByteArrayOutputStream();
			DataOutputStream dtOut = new DataOutputStream(btOut);
			dtOut.writeUTF(source);
			byte[] bt = btOut.toByteArray();
			
			writeWord(bt.length-2);
			
			System.arraycopy(bt, 2, bytes, writePos, bt.length-2);
			writePos += bt.length-2;
			dtOut.close();
			btOut.close();
		}catch(Exception e){
			//e.printStackTrace();
		}
	}
	public final void writeByteBuffer(ByteBuffer source){
		System.arraycopy(normalizeBytes(source.bytes, source.writePos), 0, bytes, writePos, source.writePos);
		writePos += source.writePos;
	}
	public final void writeWTLD(ByteBuffer source, int TLDType){
		writeDWord(TLDType);
		writeDWord(source.writePos);
		writeByteBuffer(source);
	}
	public final void writeSTLD(ByteBuffer source, int TLDType){
		writeWord(TLDType);
		writeWord(source.writePos);
		writeByteBuffer(source);
	}
	public final void skip(int count){
		readPos += count;
	}
	public final void reset(){
		bytes = new byte[0x4000];
		readPos = 0;
		writePos = 0;
	}
	public final void reset(int size){
		bytes = new byte[size];
		readPos = 0;
		writePos = 0;
	}
	public final byte[] getBytes(){
		return normalizeBytes(bytes, writePos);
	}
	public static byte[] normalizeBytes(byte[] source, int len){
		byte[] res = new byte[len];
		System.arraycopy(source, 0, res, 0, len);
		return res;
	}
	public static byte[] normalizeBytes(byte[] source, int offset, int len){
		byte[] res = new byte[len];
		System.arraycopy(source, offset, res, 0, len);
		return res;
	}
}
