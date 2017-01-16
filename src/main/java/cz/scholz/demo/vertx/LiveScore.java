package cz.scholz.demo.vertx;

import cz.scholz.demo.livescore.Game;
import cz.scholz.demo.livescore.InvalidGameException;
import cz.scholz.demo.livescore.LiveScoreService;
import io.vertx.amqpbridge.AmqpBridge;
import io.vertx.amqpbridge.AmqpBridgeOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by schojak on 10.1.17.
 */
public class LiveScore extends AbstractVerticle {
    final static private Logger LOG = LoggerFactory.getLogger(LiveScore.class);
    private LiveScoreService scoreService = new LiveScoreService();
    private AmqpBridge bridge;
    private MessageProducer<JsonObject> broadcaster;

    @Override
    public void start(Future<Void> fut) {
        String amqpHostname = config().getJsonObject("amqp", new JsonObject()).getString("hostname", "localhost");
        Integer amqpPort = config().getJsonObject("amqp", new JsonObject()).getInteger("port", 5672);
        Integer amqpIdleTimeout = config().getJsonObject("amqp", new JsonObject()).getInteger("idleTimeout", 60);
        Integer amqpConnectionTimeout = config().getJsonObject("amqp", new JsonObject()).getInteger("connectionTimeout", 5000);

        AmqpBridgeOptions options = new AmqpBridgeOptions().setIdleTimeout(amqpIdleTimeout).setConnectTimeout(amqpConnectionTimeout).addEnabledSaslMechanism("ANONYMOUS");

        bridge = AmqpBridge.create(vertx, options);

        bridge.start(amqpHostname, amqpPort, res -> {
            if (res.succeeded())
            {
                LOG.info("Connected to the AMQP router");

                // Broadcasting live updates as they occur
                broadcaster = bridge.createProducer("/liveUpdates");
                scoreService.setUpdateHandler(this::broadcastUpdates);

                // Updating score of a game
                bridge.createConsumer("/setScore").setMaxBufferedMessages(100).handler(this::setScore);

                // Getting all scores
                bridge.createConsumer("/getScores").setMaxBufferedMessages(100).handler(this::getScores);

                // Updating score of a game
                bridge.createConsumer("/addGame").setMaxBufferedMessages(100).handler(this::addGame);

                fut.complete();
            }
            else
            {
                LOG.error("Failed to connect to the AMQP server", res.cause());
                fut.fail(res.cause());
            }

        });
    }

    public void setScore(Message<Object> msg)
    {
        JsonObject msgBody = (JsonObject)msg.body();
        JsonObject body;

        if (msgBody.getString("body_type").equals("value")) {
            LOG.info("Received LiveScore/setScore request with AmqpValue body", msgBody.getValue("body").toString());
            body = new JsonObject(msgBody.getValue("body").toString());
        }
        else
        {
            LOG.error("Cannot decode LiveScore/setScore request with body_type ", msgBody.getString("body_type"));

            if(msg.replyAddress() != null) {
                JsonObject response = new JsonObject();
                response.put("application_properties", new JsonObject().put("status", "400"));
                response.put("body", new JsonObject().put("error", "Failed to decode message with body type " + msgBody.getString("body_type")));

                msg.reply(response);
            }

            return;
        }

        String homeTeam = body.getString("homeTeam", null);
        String awayTeam = body.getString("awayTeam", null);
        Integer homeTeamGoals = body.getInteger("homeTeamGoals", null);
        Integer awayTeamGoals = body.getInteger("awayTeamGoals", null);
        String gameTime = body.getString("gameTime", null);

        try {
            Game game = scoreService.setScore(homeTeam, awayTeam, homeTeamGoals, awayTeamGoals, gameTime);

            if(msg.replyAddress() != null) {
                JsonObject response = new JsonObject();
                response.put("application_properties", new JsonObject().put("status", 200));
                response.put("body", new JsonObject(Json.encode(game)).encode());

                msg.reply(response);
            }
        } catch (InvalidGameException e) {
            LOG.error("Failed to set new game score", e);

            if(msg.replyAddress() != null) {
                JsonObject response = new JsonObject();
                response.put("application_properties", new JsonObject().put("status", 400));
                response.put("body", new JsonObject().put("error", e.getMessage()).encode());

                msg.reply(response);
            }
        }
    }

    public void getScores(Message<Object> msg)
    {
        LOG.info("Received LiveScore/getScores request");

        if(msg.replyAddress() != null) {
            JsonObject response = new JsonObject();
            response.put("application_properties", new JsonObject().put("status", 200));
            response.put("body", new JsonArray(Json.encode(scoreService.getScores())).encode());

            msg.reply(response);
        }
        else
        {
            LOG.warn("Received LiveScore/getScores request without reply to address");
        }
    }

    public void addGame(Message<Object> msg)
    {

        JsonObject msgBody = (JsonObject)msg.body();
        JsonObject body;

        if (msgBody.getString("body_type").equals("value")) {
            LOG.info("Received LiveScore/addGame request with AmqpValue body", msgBody.getValue("body").toString());
            body = new JsonObject(msgBody.getValue("body").toString());
        }
        else
        {
            LOG.error("Cannot decode LiveScore/addGame request with body_type ", msgBody.getString("body_type"));

            if(msg.replyAddress() != null) {
                JsonObject response = new JsonObject();
                response.put("application_properties", new JsonObject().put("status", 400));
                response.put("body", new JsonObject().put("error", "Failed to decode message with body type " + msgBody.getString("body_type")).encode());

                msg.reply(response);
            }

            return;
        }

        String homeTeam = body.getString("homeTeam", null);
        String awayTeam = body.getString("awayTeam", null);
        String startTime = body.getString("startTime", null);

        try {
            Game game = scoreService.addGame(homeTeam, awayTeam, startTime);

            if(msg.replyAddress() != null) {
                JsonObject response = new JsonObject();
                response.put("application_properties", new JsonObject().put("status", 201));
                response.put("body", new JsonObject(Json.encode(game)).encode());

                msg.reply(response);
            }
        } catch (InvalidGameException e) {
            LOG.error("Failed to add new game", e);

            if(msg.replyAddress() != null) {
                JsonObject response = new JsonObject();
                response.put("application_properties", new JsonObject().put("status", 400));
                response.put("body", new JsonObject().put("error", e.getMessage()).encode());

                msg.reply(response);
            }
        }
    }

    public void broadcastUpdates(Game game)
    {
        LOG.info("Broadcasting game update " + game);
        JsonObject message = new JsonObject();
        message.put("body", new JsonObject(Json.encode(game)).encode());
        broadcaster.send(message);
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Shutting down");
        // Nothing to do
    }
}
