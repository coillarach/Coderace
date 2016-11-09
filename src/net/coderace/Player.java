package net.coderace;

import com.google.android.gms.maps.model.Marker;

import android.location.Location;
import android.util.Log;

public class Player {
	protected int id;
	protected int team;
	protected String username;
	protected double latitude;
	protected double longitude;
	protected String device;
	protected Marker marker = null;
	
	private static final String TAG = "Player";
	
	Player () {
		id = 0;
		team = 0;
		username = "";
		latitude = 0;
		longitude = 0;
		device = "";
	}
	
	Player(int i, int t, String u, double la, double lo) {
		id = i;
		team = t;
		username = u;
		latitude = la;
		longitude = lo;
		device = "";
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int i) {
		id = i;
	}
	
	public String getUsername() {
		Log.d(TAG,username);
		return username;
	}
	
	public void setUsername(String un) {
		username = un;
	}
	
	public void setTeam(int t) {
		team = t;
	}
	
	public int getTeam() {
		return team;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public void setDevice(String did) {
		device = did;
	}

	public String getDevice() {
		return device;
	}
	
	public void updateGPSLocation(Location l) {
		latitude = l.getLatitude() * 1e6;
		longitude = l.getLongitude() * 1e6;
	}
	
	public void show(double la, double lo, Marker m) {
		latitude = la;
		longitude = lo;
		if(marker != null)
			marker.remove();
    	marker = m;
	}
	
	public void show(Marker m) {
		if(marker != null)
			marker.remove();
    	marker = m;
	}
	
	public void hide() {
		if(marker != null)
			marker.remove();
	}
}
