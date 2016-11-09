package net.coderace;

public class Team {
	public static int  LOCATION = 0;
	public static int  PLAYER = 1;
	
	public static int	FREE  = 1000;
	public static int	TOTAL = 2000;
	
	int id;
	String name;
	int score;
	
	Team(int i, String n, int s) {
		id = i;
		name = n;
		score = s;
	}
	
	public int getScore() {
		return this.score;
	}
	
	public void incrementScore() {
		score++;
	}
	
	public void decrementScore() {
		score--;
	}
	
	public String getName() {
		return this.name;
	}
	
	// TODO: Change team test to >=0
	//       Admin values (free and total) should be -1 and -2
	
	public Boolean isTeam() {
		if (id < 1000)
			return true;
		else
			return false;
	}
}
