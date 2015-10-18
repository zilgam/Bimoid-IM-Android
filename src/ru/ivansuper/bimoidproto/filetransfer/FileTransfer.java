package ru.ivansuper.bimoidproto.filetransfer;

public abstract class FileTransfer {
	public static final int FT_REPLY_CODE_ACCEPT = 0x0001;
	public static final int FT_REPLY_CODE_DECLINE = 0x0002;
	public static final int FT_REPLY_CODE_DISABLED = 0x0003;
	public static final int FT_REPLY_CODE_NOT_ALLOWED = 0x0004;
	public static final int FT_CONTROL_CODE_CANCEL = 0x0001;
	public static final int FT_CONTROL_CODE_DIRECT_FAILED = 0x0002;
	public static final int FT_CONTROL_CODE_DIRECT_FAILED_TRY_REVERSE = 0x0003;
	public static final int FT_CONTROL_CODE_DIRECT_FAILED_TRY_PROXY = 0x0004;
	public static final int FT_CONTROL_CODE_PROXY_FAILED = 0x0005;
	public static final int FT_CONTROL_CODE_READY = 0x0006;
	public static final int OBIMP_MAX_FILE_DATA_WTDL_LEN = 0x00000800;
	public static final int RECEIVER = 0;
	public static final int SENDER = 1;
	public int type = -1;
	protected byte[] unique_id;
	protected int server_port = 0;
	public byte[] getUniqueId(){
		return unique_id;
	}
	public int getLocalPort(){
		return server_port;
	}
	public abstract void runTransfer();
	public abstract void reverseConnection();//Used only in sender
	public abstract void cancel();
	public abstract void runProxyTransfer();
}
