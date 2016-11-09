package net.coderace;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class Game {
	Context ctx;
	int id=0;
	String name="";
	double startLat = 55933415;
	double startLong = -3213243;
	int startZoom = 14;
	Long startTime=(long) 0;
	Long endTime=(long) 0;
	Long timeLeft=(long) 0;
	Long timeOffset=(long) 0; // Difference between local time and server time
	protected int state = 0;
	
	private static final String TAG = "Game";
	
	public static final int PENDING  = 0;
	public static final int STARTING = 1;
	public static final int STARTED  = 2;
	public static final int ENDING 	 = 3;
	public static final int OVER 	 = 4;
	
	Player[] players;
	GameLocation[] locations;
	Team[] teams;

	Game(CodeRace c, int pid, String pname, double plat, double plong, int pzoom, Long pstime, Long petime, Long offset, int s) {
		ctx			= c;
		id			= pid;
		name		= pname;
		startLat	= plat;
		startLong	= plong;
		startZoom	= pzoom;
		startTime	= pstime;
		endTime		= petime;
		timeOffset	= offset;
		state		= s;
		
		timeLeft = endTime - timeOffset - System.currentTimeMillis()/1000;
	}
	


	public void populate(JSONObject jGame) throws JSONException {
		if (jGame != null) {
            Log.i(TAG, "Game state retrieved");
            
            // Remove any current data
            teams = null;
            locations = null;
            players = null;

            // Teams
			Log.d(TAG,"Reading team data");
			int teamNumber = jGame.getInt("teamNumber");
			teams = new Team[teamNumber];
				
			JSONArray jTeams = jGame.getJSONArray("teams");
			for (int i=0; i<teamNumber; i++) {
				int tid  = jTeams.getJSONObject(i).getInt("id");
				String n = jTeams.getJSONObject(i).getString("name");
				int s    = jTeams.getJSONObject(i).getInt("score");
				
				teams[i] = new Team(tid, n, s);
			}

			// Locations
			Log.d(TAG,"Reading location data");
			int locationNumber = jGame.getInt("locationNumber");
			locations = new GameLocation[locationNumber];
				
			JSONArray jLocations = jGame.getJSONArray("locations");
			for (int i=0; i<locationNumber; i++) {
				int lid      = jLocations.getJSONObject(i).getInt("id");
				double jLat  = jLocations.getJSONObject(i).getDouble("latitude");
				double jLong = jLocations.getJSONObject(i).getDouble("longitude");
				String jClue = jLocations.getJSONObject(i).getString("clue");
				int jVisible = jLocations.getJSONObject(i).getInt("visible");
				int jTeam    = jLocations.getJSONObject(i).getInt("team");
				
				locations[i] = new GameLocation(lid, jLat, jLong, jClue, jVisible, jTeam);
			}

			// Players
			JSONArray jPlayers = jGame.getJSONArray("players");
			updatePlayers(jPlayers);
			
			// Update state
//			state = STARTED;
		}
	}
	
	public void updatePlayers(JSONArray jPlayers) throws JSONException {
		int jId  = 0;
		int jTeam = 0;
		String jName = "";
		double jLat = 90;
		double jLong = 0;

		if (jPlayers != null) {
            // Remove any current data
            players = null;
			Log.d(TAG,"Reading player data");
			int playerNumber = jPlayers.length();
			players = new Player[playerNumber];
				
			Log.d(TAG,"Players: " + playerNumber);
			for (int i=0; i<playerNumber; i++) {
				jId   = jPlayers.getJSONObject(i).getInt("id");
				jLat  = jPlayers.getJSONObject(i).getDouble("latitude");
				jLong = jPlayers.getJSONObject(i).getDouble("longitude");
				jTeam = jPlayers.getJSONObject(i).getInt("team");
				jName = jPlayers.getJSONObject(i).getString("username");
				players[i] = new Player(jId, jTeam, jName, jLat, jLong);
			}
		}
	}
	
	public void start() {
		state = STARTED;
	}
	
	public String end() {
		int i = 0;
		int topScore = 0;
		ArrayList<Integer> winners = new ArrayList<Integer>(0); 
		String returnVal = "";
		
		state = OVER;
		for(Team t:teams) {
			if (t.id != Team.FREE && t.id != Team.TOTAL) {
				if (t.score > topScore) {
					topScore = t.score;
					winners.clear();
					winners.add(t.id);
				}
				else if (t.score == topScore)
					winners.add(t.id);
			}
		}
		
		if (winners.size() == 1) {
			returnVal = getTeam(winners.get(0)).getName() + " are the winners!";
		}
		else {
			for(i=0;i<winners.size();i++) {
				returnVal += getTeam(winners.get(i)).getName();
				if (i == winners.size()-2) 
					returnVal += " and ";
				else if (i < winners.size()-2)
					returnVal += ", ";
			}
			returnVal += " draw!";
		}
		
		return returnVal;
	}
	
	public int getState() {
		return state;
	}
	
	public int duration() {
		return (int)(endTime - startTime);
	}
	
	public Player getPlayer(int p) {
		for (int i=0;i<players.length;i++) {
			if (players[i].id == p) {
				return players[i];
			}
		}
		return null;
	}
	
	public Team getTeam(int t) {
		for (int i=0;i<teams.length;i++) {
			if (teams[i].id == t) {
				return teams[i];
			}
		}
		return null;
	}

	public GameLocation getLocation(int l) {
		for (int i=0;i<locations.length;i++) {
			if (locations[i].id == l) {
				return locations[i];
			}
		}
		return null;
	}

	public int visibleLocations() {
		int count = 0;
		for (int i=0;i<locations.length;i++) {
			if (locations[i].visible) {
				count++;
			}
		}
		return count;
	}
}
