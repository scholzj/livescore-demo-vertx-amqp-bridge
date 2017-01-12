package cz.scholz.demo.livescore;

import io.vertx.core.Handler;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by schojak on 12.1.17.
 */
public class LiveScoreService {
    private ConcurrentHashMap<String, Game> games = new ConcurrentHashMap();
    private Handler<Game> updateHandler = null;

    public LiveScoreService setUpdateHandler(Handler<Game> handler)
    {
        updateHandler = handler;
        return this;
    }

    public List<Game> getScores() {
        return new LinkedList<Game>(games.values());
    }

    public Game getScore(String homeTeam, String awayTeam) throws InvalidGameException {
        if (!gameExists(homeTeam, awayTeam))
        {
            throw new InvalidGameException("Game between " + homeTeam + " and " + awayTeam + " was not found!");
        }
        else
        {
            return getGame(homeTeam, awayTeam);
        }
    }

    public Game addGame(String homeTeam, String awayTeam, String startTime) throws InvalidGameException {
        if (homeTeam == null || awayTeam == null || startTime == null)
        {
            throw new InvalidGameException("Some of the mandatory fields (homeTeam, awayTeam or startTime) is invalid");
        }
        else if (!gameExists(homeTeam, awayTeam))
        {
            Game game = new Game(homeTeam, awayTeam, startTime);
            games.put(gameId(homeTeam, awayTeam), game);
            pushUpdate(game);
            return game;
        }
        else
        {
            throw new InvalidGameException("Game between " + homeTeam + " and " + awayTeam + " already exists!");
        }
    }

    public Game setScore(String homeTeam, String awayTeam, Integer homeTeamGoals, Integer awayTeamGoals, String gameTime) throws InvalidGameException {

        if (homeTeam == null || awayTeam == null || homeTeamGoals == null || awayTeamGoals == null || gameTime == null)
        {
            throw new InvalidGameException("Some of the mandatory fields (homeTeam, awayTeam, homeTeamGoals, awayTeamGoals or gameTime) is invalid");
        }
        else if (homeTeamGoals < 0 || awayTeamGoals < 0)
        {
            throw new InvalidGameException("The home and away team goals have to be => 0!");
        }
        else if (!gameExists(homeTeam, awayTeam))
        {
            throw new InvalidGameException("Game between " + homeTeam + " and " + awayTeam + " was not found! Maybe you forgot to create the game first?");
        }
        else
        {
            Game game = getGame(homeTeam, awayTeam).setScore(homeTeamGoals, awayTeamGoals, gameTime);
            pushUpdate(game);
            return game;
        }
    }

    private String gameId(String homeTeam, String awayTeam)
    {
        return homeTeam + awayTeam;
    }

    private Boolean gameExists(String homeTeam, String awayTeam)
    {
        return games.containsKey(gameId(homeTeam, awayTeam));
    }

    private Game getGame(String homeTeam, String awayTeam)
    {
        return games.getOrDefault(gameId(homeTeam, awayTeam), null);
    }

    private void pushUpdate(Game game)
    {
        if (updateHandler != null)
        {
            updateHandler.handle(game);
        }
    }
}
