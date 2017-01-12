package cz.scholz.demo.livescore;

/**
 * Created by schojak on 10.1.17.
 */
public class Game {
    private final String homeTeam;
    private final String awayTeam;
    private final String startTime;
    private Integer homeTeamGoals = 0;
    private Integer awayTeamGoals = 0;
    private String gameTime = "0";

    public Game(String homeTeam, String awayTeam, String startTime) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.startTime = startTime;
    }

    public Game(String homeTeam, String awayTeam, String startTime, int homeTeamGoals, int awayTeamGoals, String gameTime) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.startTime = startTime;
        this.homeTeamGoals = homeTeamGoals;
        this.awayTeamGoals = awayTeamGoals;
        this.gameTime = gameTime;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public String getStartTime() {
        return startTime;
    }

    public int getHomeTeamGoals() {
        return homeTeamGoals;
    }

    public Game setHomeTeamGoals(int homeTeamGoals) {
        this.homeTeamGoals = homeTeamGoals;
        return this;
    }

    public int getAwayTeamGoals() {
        return awayTeamGoals;
    }

    public Game setAwayTeamGoals(int awayTeamGoals) {
        this.awayTeamGoals = awayTeamGoals;
        return this;
    }

    public String getGameTime() {
        return gameTime;
    }

    public Game setGameTime(String gameTime) {
        this.gameTime = gameTime;
        return this;
    }

    public Game setScore(int homeTeamGoals, int awayTeamGoals) {
        setHomeTeamGoals(homeTeamGoals);
        setAwayTeamGoals(awayTeamGoals);
        return this;
    }

    public Game setScore(int homeTeamGoals, int awayTeamGoals, String gameTime) {
        setScore(homeTeamGoals, awayTeamGoals);
        setGameTime(gameTime);
        return this;
    }

    public String toString()
    {
        return String.format("Game: %s : %s, Score: %d : %d, Start time: %s, Game time: %s", homeTeam, awayTeam, homeTeamGoals, awayTeamGoals, startTime, gameTime);
    }
}
