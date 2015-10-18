package ru.ivansuper.bimoidim;

public class BufferedDialog {
	public int type;
	public String header;
	public String text;
	public BufferedDialog(int type, String header, String text){
		this.type = type;
		this.header = header;
		this.text = text;
	}
}
