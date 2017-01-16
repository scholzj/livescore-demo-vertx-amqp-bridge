package cz.scholz.demo.livescore;

import cz.scholz.demo.vertx.LiveScore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static io.vertx.proton.ProtonHelper.message;

/**
 * Created by jakub on 15/01/2017.
 */
@RunWith(VertxUnitRunner.class)
public class LiveScoreIT {
    final static private Logger LOG = LoggerFactory.getLogger(LiveScoreIT.class);
    private static Vertx vertx;
    private static int dispatchPort;
    private static JsonObject config = new JsonObject();

    @BeforeClass
    public static void setUp(TestContext context) {
        vertx = Vertx.vertx();

        dispatchPort = Integer.getInteger("dispatch.port", 5672);

        config.put("amqp", new JsonObject().put("port", dispatchPort).put("hostname", "127.0.0.1"));

        //vertx.deployVerticle(LiveScore.class.getName(), new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());
    }

    private void deployVerticle(TestContext context)
    {
        final Async asyncStart = context.async();

        LOG.info("Starting verticle with config " + config);

        vertx.deployVerticle(LiveScore.class.getName(), new DeploymentOptions().setConfig(config), res -> {
            if (res.succeeded()) {
                asyncStart.complete();
            }
            else
            {
                context.fail(res.cause());
            }
        });

        asyncStart.awaitSuccess();
    }

    @Test
    public void testAddGame(TestContext context)
    {
        final Async asyncAddGame = context.async();
        final String responseAddress = UUID.randomUUID().toString();

        deployVerticle(context);

        ProtonClient client = ProtonClient.create(vertx);
        ProtonClientOptions options = new ProtonClientOptions().setIdleTimeout(0).setConnectTimeout(5000).addEnabledSaslMechanism("ANONYMOUS").setReconnectAttempts(0);
        client.connect(options,"127.0.0.1", dispatchPort, res -> {
            if (res.succeeded())
            {
                LOG.info("Connected");

                res.result().setContainer("LiveScoreClient").disconnectHandler(connection -> {
                    LOG.error("Connection disconnected " + connection.getCondition().getDescription());
                }).openHandler(openRes -> {
                    if (openRes.succeeded()) {
                        LOG.info("Connection openned");
                        ProtonConnection conn = openRes.result();

                        ProtonReceiver recv = conn.createReceiver(responseAddress).handler((delivery, msg) -> {
                            LOG.info("Received message");
                            Section body = msg.getBody();;
                            if (body instanceof AmqpValue) {
                                context.assertEquals(201, msg.getApplicationProperties().getValue().get("status"));
                                JsonObject payload = new JsonObject((String)((AmqpValue)body).getValue().toString());
                                context.assertEquals("Aston Villa", payload.getString("homeTeam"));
                                context.assertEquals("Preston North End", payload.getString("awayTeam"));
                                context.assertEquals("21th January 2017, 16:00", payload.getString("startTime"));
                                context.assertEquals(0, payload.getInteger("homeTeamGoals"));
                                context.assertEquals(0, payload.getInteger("awayTeamGoals"));
                                context.assertEquals("0", payload.getString("gameTime"));
                                asyncAddGame.complete();
                            }
                        }).open();

                        LOG.info("Receiver created");

                        conn.createSender("/addGame").openHandler(openResult -> {
                            if (openResult.succeeded()) {
                                LOG.info("Sender is open");
                                ProtonSender sender = openResult.result();

                                JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");

                                Message message = message();
                                message.setReplyTo(responseAddress);
                                message.setBody(new AmqpValue(payload.encodePrettily()));

                                sender.send(message, delivery -> {
                                    LOG.info("Message received by server: remote state=%s, remotely settled=%s",
                                            delivery.getRemoteState(), delivery.remotelySettled());
                                });
                            }
                        }).open();

                        LOG.info("Sender created");
                    }
                    else
                    {
                        LOG.error("Opening connection failed", res.cause());
                        context.fail("Failed to open the connection");
                    }
                }).open();
            }
            else
            {
                LOG.error("Connection failed", res.cause());
                context.fail("Failed to connect to the router");
            }
        });

        asyncAddGame.awaitSuccess(10000);
    }

    @Test
    public void testSetScore(TestContext context)
    {
        final Async asyncSetScore = context.async();
        final String responseAddress = UUID.randomUUID().toString();

        deployVerticle(context);

        ProtonClient client = ProtonClient.create(vertx);
        ProtonClientOptions options = new ProtonClientOptions().setIdleTimeout(0).setConnectTimeout(5000).addEnabledSaslMechanism("ANONYMOUS").setReconnectAttempts(0);
        client.connect(options,"127.0.0.1", dispatchPort, res -> {
            if (res.succeeded())
            {
                LOG.info("Connected");

                res.result().setContainer("LiveScoreClient").disconnectHandler(connection -> {
                    LOG.error("Connection disconnected " + connection.getCondition().getDescription());
                }).openHandler(openRes -> {
                    if (openRes.succeeded()) {
                        LOG.info("Connection openned");
                        ProtonConnection conn = openRes.result();

                        conn.createSender("/addGame").openHandler(openResult -> {
                            if (openResult.succeeded()) {
                                LOG.info("Sender is open");
                                ProtonSender sender = openResult.result();

                                JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");

                                Message message = message();
                                message.setBody(new AmqpValue(payload.encodePrettily()));

                                sender.send(message, delivery -> {
                                    LOG.info("Message received by server: remote state=%s, remotely settled=%s",
                                            delivery.getRemoteState(), delivery.remotelySettled());
                                });
                            }
                        }).open();

                        ProtonReceiver recv = conn.createReceiver(responseAddress).handler((delivery, msg) -> {
                            LOG.info("Received message");
                            Section body = msg.getBody();
                            if (body instanceof AmqpValue) {
                                context.assertEquals(200, msg.getApplicationProperties().getValue().get("status"));
                                JsonObject payload = new JsonObject((String) ((AmqpValue) body).getValue().toString());
                                context.assertEquals("Aston Villa", payload.getString("homeTeam"));
                                context.assertEquals("Preston North End", payload.getString("awayTeam"));
                                context.assertEquals("21th January 2017, 16:00", payload.getString("startTime"));
                                context.assertEquals(1, payload.getInteger("homeTeamGoals"));
                                context.assertEquals(0, payload.getInteger("awayTeamGoals"));
                                context.assertEquals("HT", payload.getString("gameTime"));
                                asyncSetScore.complete();
                            }
                        }).open();

                        conn.createSender("/setScore").openHandler(openResult -> {
                            if (openResult.succeeded()) {
                                LOG.info("Sender is open");
                                ProtonSender sender = openResult.result();

                                JsonObject payloadSetScore = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("gameTime", "HT").put("homeTeamGoals", 1).put("awayTeamGoals", 0);

                                Message messageSetScore = message();
                                messageSetScore.setReplyTo(responseAddress);
                                messageSetScore.setBody(new AmqpValue(payloadSetScore.encodePrettily()));

                                sender.send(messageSetScore);
                            }
                        }).open();
                    }
                    else
                    {
                        LOG.error("Opening connection failed", res.cause());
                        context.fail("Failed to open the connection");
                    }
                }).open();
            }
            else
            {
                LOG.error("Connection failed", res.cause());
                context.fail("Failed to connect to the router");
            }
        });

        asyncSetScore.awaitSuccess(10000);
    }

    @Test
    public void testGetScore(TestContext context)
    {
        final Async asyncGetScore = context.async();
        final String responseAddress = UUID.randomUUID().toString();

        deployVerticle(context);

        ProtonClient client = ProtonClient.create(vertx);
        ProtonClientOptions options = new ProtonClientOptions().setIdleTimeout(0).setConnectTimeout(5000).addEnabledSaslMechanism("ANONYMOUS").setReconnectAttempts(0);
        client.connect(options,"127.0.0.1", dispatchPort, res -> {
            if (res.succeeded())
            {
                LOG.info("Connected");

                res.result().setContainer("LiveScoreClient").disconnectHandler(connection -> {
                    LOG.error("Connection disconnected " + connection.getCondition().getDescription());
                }).openHandler(openRes -> {
                    if (openRes.succeeded()) {
                        LOG.info("Connection openned");
                        ProtonConnection conn = openRes.result();

                        conn.createSender("/addGame").openHandler(openResult -> {
                            if (openResult.succeeded()) {
                                LOG.info("Sender is open");
                                ProtonSender sender = openResult.result();

                                JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");

                                Message message = message();
                                message.setBody(new AmqpValue(payload.encodePrettily()));

                                sender.send(message, delivery -> {
                                    LOG.info("Message received by server: remote state=%s, remotely settled=%s",
                                            delivery.getRemoteState(), delivery.remotelySettled());
                                });
                            }
                        }).open();

                        ProtonReceiver recv = conn.createReceiver(responseAddress).handler((delivery, msg) -> {
                            LOG.info("Received message");
                            Section body = msg.getBody();
                            if (body instanceof AmqpValue) {
                                context.assertEquals(200, msg.getApplicationProperties().getValue().get("status"));
                                JsonArray payload = new JsonArray((String) ((AmqpValue) body).getValue().toString());
                                context.assertEquals("Aston Villa", payload.getJsonObject(0).getString("homeTeam"));
                                context.assertEquals("Preston North End", payload.getJsonObject(0).getString("awayTeam"));
                                context.assertEquals("21th January 2017, 16:00", payload.getJsonObject(0).getString("startTime"));
                                context.assertEquals(0, payload.getJsonObject(0).getInteger("homeTeamGoals"));
                                context.assertEquals(0, payload.getJsonObject(0).getInteger("awayTeamGoals"));
                                context.assertEquals("0", payload.getJsonObject(0).getString("gameTime"));
                                asyncGetScore.complete();
                            }
                        }).open();

                        conn.createSender("/getScores").openHandler(openResult -> {
                            if (openResult.succeeded()) {
                                LOG.info("Sender is open");
                                ProtonSender sender = openResult.result();

                                Message messageSetScore = message();
                                messageSetScore.setReplyTo(responseAddress);

                                sender.send(messageSetScore);
                            }
                        }).open();
                    }
                    else
                    {
                        LOG.error("Opening connection failed", res.cause());
                        context.fail("Failed to open the connection");
                    }
                }).open();
            }
            else
            {
                LOG.error("Connection failed", res.cause());
                context.fail("Failed to connect to the router");
            }
        });

        asyncGetScore.awaitSuccess(10000);
    }

    @Test
    public void testLiveUpdates(TestContext context)
    {
        final Async asyncLiveUpdate = context.async();

        deployVerticle(context);

        ProtonClient client = ProtonClient.create(vertx);
        ProtonClientOptions options = new ProtonClientOptions().setIdleTimeout(0).setConnectTimeout(5000).addEnabledSaslMechanism("ANONYMOUS").setReconnectAttempts(0);
        client.connect(options,"127.0.0.1", dispatchPort, res -> {
            if (res.succeeded())
            {
                LOG.info("Connected");

                res.result().setContainer("LiveScoreClient").disconnectHandler(connection -> {
                    LOG.error("Connection disconnected " + connection.getCondition().getDescription());
                }).openHandler(openRes -> {
                    if (openRes.succeeded()) {
                        LOG.info("Connection openned");
                        ProtonConnection conn = openRes.result();



                        ProtonReceiver recv = conn.createReceiver("/liveUpdates").handler((delivery, msg) -> {
                            LOG.info("Received message");
                            Section body = msg.getBody();
                            if (body instanceof AmqpValue) {
                                JsonObject payload = new JsonObject((String) ((AmqpValue) body).getValue().toString());
                                context.assertEquals("Aston Villa", payload.getString("homeTeam"));
                                context.assertEquals("Preston North End", payload.getString("awayTeam"));
                                context.assertEquals("21th January 2017, 16:00", payload.getString("startTime"));
                                context.assertEquals(0, payload.getInteger("homeTeamGoals"));
                                context.assertEquals(0, payload.getInteger("awayTeamGoals"));
                                context.assertEquals("0", payload.getString("gameTime"));
                                asyncLiveUpdate.complete();
                            }
                        }).open();

                        conn.createSender("/addGame").openHandler(openResult -> {
                            if (openResult.succeeded()) {
                                LOG.info("Sender is open");
                                ProtonSender sender = openResult.result();

                                JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");

                                Message message = message();
                                message.setBody(new AmqpValue(payload.encodePrettily()));

                                sender.send(message, delivery -> {
                                    LOG.info("Message received by server: remote state=%s, remotely settled=%s",
                                            delivery.getRemoteState(), delivery.remotelySettled());
                                });
                            }
                        }).open();
                    }
                    else
                    {
                        LOG.error("Opening connection failed", res.cause());
                        context.fail("Failed to open the connection");
                    }
                }).open();
            }
            else
            {
                LOG.error("Connection failed", res.cause());
                context.fail("Failed to connect to the router");
            }
        });

        asyncLiveUpdate.awaitSuccess(10000);
    }

    @After
    public void cleanup(TestContext context)
    {
        vertx.deploymentIDs().forEach(id -> {
            vertx.undeploy(id, context.asyncAssertSuccess());
        });
    }

    @AfterClass
    public static void tearDown(TestContext context) {
        LiveScoreIT.vertx.close(context.asyncAssertSuccess());
    }
}
