package net.coderace;

import com.google.android.gms.maps.model.Marker;

public class GameLocation {

	public int id;
	protected double latitude;
	protected double longitude;
	public String clue;
	public boolean visible;
	public int team;
	public Marker marker;
	
	GameLocation(int lid, double plat, double plong, String cl, int vis, int t) {
		id = lid;
		latitude = plat;
		longitude = plong;
		clue = cl;
		if (vis == 0)
			visible = false;
		else
			visible = true;
		team = t;
	}

	public Boolean isClaimed() {
		if (this.team > 0)
			return true;
		else
			return false;
	}
	
	public void setVisible(Boolean v) {
		visible = v;
	}
	
	public void setClaimed(int t, Marker m) {
    	team = t;
    	// Remove existing marker
    	if (marker != null)
    		marker.remove();
    	// Add new marker
    	marker = m;
	}
	
}
