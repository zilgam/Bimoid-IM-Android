package ru.ivansuper.locale;

public class Language {
	public String NAME;
	public String LANGUAGE;
	public String AUTHOR;
	public String path;
	public boolean internal;
	public Language(String name, String language, String author, String path, boolean internal){
		NAME = name;
		LANGUAGE = language;
		AUTHOR = author;
		this.path = path;
		this.internal = internal;
	}
}
