package ru.ivansuper.bimoidproto.roster_tools;

import java.util.Vector;

import android.util.Log;

import ru.ivansuper.bimoidproto.RosterItem;

public class RosterComparator {
	public static synchronized RosterComparator getInstance(){
		return new RosterComparator();
	}
	private RosterComparator(){}
	public final Vector<RosterItem> compare(Vector<RosterItem> source, Vector<RosterItem> updated){
		final Vector<RosterItem> compared = new Vector<RosterItem>();
		final Vector<RosterItem> protector = (Vector<RosterItem>)source.clone();
		for(RosterItem r: updated){
			final RosterItem i = getByHash(protector, r);
			if(i != null){
				i.update(r);
				compared.add(i);
				//Log.e(getClass().getSimpleName(), "Updating");
			}else{
				compared.add(r);
				//Log.e(getClass().getSimpleName(), "Adding");
			}
		}
		return compared;
	}
	private final RosterItem getByHash(Vector<RosterItem> from, RosterItem item){
		final int hash = item.getHash();
		final int count = from.size();
		for(int i=0; i<count; i++){
			final RosterItem r = from.get(i);
			if(r.getHash() == hash){
				from.remove(i);
				return r;
			}
		}
		return null;
	}
}
