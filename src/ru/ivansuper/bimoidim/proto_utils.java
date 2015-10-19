package ru.ivansuper.bimoidim;

import ru.ivansuper.bimoidproto.Contact;

public class proto_utils {
	public static final String getSchema(Contact contact){
		if(contact == null) return "";
		StringBuffer buf = new StringBuffer("CHAT");
		buf.append(contact.getID());
		buf.append("***$$$SEPARATOR$$$***");
		buf.append(contact.getTransportId());
		buf.append("***$$$SEPARATOR$$$***");
		buf.append(contact.getProfile().ID);
		return buf.toString();
	}
}
