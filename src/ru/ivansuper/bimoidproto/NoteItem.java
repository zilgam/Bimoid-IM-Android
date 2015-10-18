package ru.ivansuper.bimoidproto;

import ru.ivansuper.bimoidim.R;
import ru.ivansuper.bimoidim.resources;
import android.graphics.drawable.Drawable;

public class NoteItem extends RosterItem {
	public static final byte CL_NOTE_TYPE_TEXT = 0x00;
	public static final byte CL_NOTE_TYPE_COMMAND = 0x01;
	public static final byte CL_NOTE_TYPE_LINK = 0x02;
	public static final byte CL_NOTE_TYPE_EMAIL = 0x03;
	public static final byte CL_NOTE_TYPE_PHONE = 0x04;
	{
		this.type = RosterItem.CL_ITEM_TYPE_NOTE;
	}
	//public String name;
	public byte TYPE;
	public String TEXT;
	public long TIMESTAMP;
	public byte[] HASH;
	public Drawable getIcon(){
		switch(TYPE){
		case CL_NOTE_TYPE_COMMAND:
			return resources.ctx.getResources().getDrawable(R.drawable.note_prog);
		case CL_NOTE_TYPE_LINK:
			return resources.ctx.getResources().getDrawable(R.drawable.note_web);
		case CL_NOTE_TYPE_EMAIL:
			return resources.ctx.getResources().getDrawable(R.drawable.note_mail);
		case CL_NOTE_TYPE_PHONE:
			return resources.ctx.getResources().getDrawable(R.drawable.note_phone);
		}
		return resources.ctx.getResources().getDrawable(R.drawable.note);
	}
}
