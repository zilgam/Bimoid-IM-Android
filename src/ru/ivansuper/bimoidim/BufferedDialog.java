package ru.ivansuper.bimoidim;

public class BufferedDialog {
	public int type;
	public String header;
	public String text;
	public String field1;
	public boolean is_error;
	public BufferedDialog(int type, String header, String text){
		this.type = type;
		this.header = header;
		this.text = text;
	}
	public BufferedDialog(int type, String header, String text, String error){
		this.type = type;
		this.header = header;
		this.text = text;
		this.field1 = error;
		is_error = true;
	}
}
