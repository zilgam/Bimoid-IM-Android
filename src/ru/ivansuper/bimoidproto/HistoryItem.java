package ru.ivansuper.bimoidproto;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.text.Spannable;

import ru.ivansuper.bimoidproto.filetransfer.FileTransfer;

public class HistoryItem {
	public static final int DIRECTION_INCOMING = 0;
	public static final int DIRECTION_OUTGOING = 1;
	public int direction = 0;
	public String message = "";
	public Spannable span_message;
	public int unique_id = 0;
	public long date = 0;
	public String formattedDate = "null";
	public int isAuthMessage = -1;
	private boolean confirmed = true;
	public Contact contact;
	private boolean is_offline;
	private FileTransfer transfer;
	public HistoryItem(){
		date = System.currentTimeMillis();
		Date dt = new Date(date);
		String format = "dd.MM.yy-HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		formattedDate = sdf.format(dt);
	}
	public HistoryItem(long time){
		date = time;
		Date dt = new Date(date);
		String format = "dd.MM.yy-HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		formattedDate = sdf.format(dt);
		is_offline = true;
	}
	public void resetConfirmation(){
		confirmed = false;
	}
	public void setConfirmed(){
		confirmed = true;
	}
	public boolean confirmed(){
		return confirmed;
	}
	public boolean isOffline(){
		return is_offline;
	}
	public void attachTransfer(FileTransfer transfer){
		this.transfer = transfer;
	}
	public void deattachTransfer(){
		transfer = null;
	}
	public FileTransfer getAttachedTransfer(){
		return transfer;
	}
	public boolean isReceiverAttached(){
		if(transfer != null){
			if(transfer.type == FileTransfer.RECEIVER)
				return true;
		}
		return false;
	}
	public boolean isSenderAttached(){
		if(transfer != null){
			if(transfer.type == FileTransfer.SENDER)
				return true;
		}
		return false;
	}
}
