package cz.scholz.demo.livescore;

import cz.scholz.demo.vertx.LiveScore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.proton.ProtonHelper.message;

/**
 * Created by jakub on 15/01/2017.
 */
@RunWith(VertxUnitRunner.class)
public class LiveScoreIT {
    final static private Logger LOG = LoggerFactory.getLogger(LiveScoreIT.class);
    private static Vertx vertx;
    private static int dispatchPort;

    @BeforeClass
    public static void setUp(TestContext context) {
        vertx = Vertx.vertx();

        dispatchPort = Integer.getInteger("dispatch.port", 5672);

        JsonObject config = new JsonObject();
        config.put("amqp", new JsonObject().put("port", dispatchPort).put("hostname", "127.0.0.1"));

        LOG.info("Starting verticle with config " + config);

        vertx.deployVerticle(LiveScore.class.getName(), new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());
    }

    @Test
    public void testLiveScoreService(TestContext context)
    {
        final Async asyncTest = context.async();

        ProtonClient client = ProtonClient.create(vertx);
        ProtonClientOptions options = new ProtonClientOptions().setIdleTimeout(0).setConnectTimeout(5000).addEnabledSaslMechanism("ANONYMOUS").setReconnectAttempts(0);
        client.connect(options,"127.0.0.1", dispatchPort, res -> {
            if (res.succeeded())
            {
                LOG.info("Connected");

                final Async asyncAddGame = context.async();

                ProtonConnection conn = res.result();
                conn.disconnectHandler(connection -> {
                    LOG.error("Connection disconnected " + connection.getCondition().getDescription());
                });
                ProtonReceiver recv = conn.createReceiver("#").handler((delivery, msg) -> {
                    LOG.info("Received message");
                    Section body = msg.getBody();
                    if (body instanceof AmqpValue) {
                        context.assertEquals(200, msg.getApplicationProperties().getValue().get("status"));
                        JsonObject payload = new JsonObject((String)((AmqpValue) body).getValue());
                        context.assertEquals("Aston Villa", payload.getString("homeTeam"));
                        context.assertEquals("Preston North End", payload.getString("awayTeam"));
                        context.assertEquals("21th January 2017, 16:00", payload.getString("startTime"));
                        context.assertEquals(0, payload.getString("homeTeamGoals"));
                        context.assertEquals(0, payload.getString("awayTeamGoals"));
                        context.assertEquals("0", payload.getString("gameTime"));
                        asyncAddGame.complete();
                    }
                }).open().setPrefetch(1);

                conn.createSender("addGame").openHandler(openResult -> {
                    if (openResult.succeeded()) {
                        LOG.info("Sender is open");
                        ProtonSender sender = openResult.result();

                        JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");

                        Message message = message();
                        System.out.println("Source: " + recv.getRemoteSource().getAddress());
                        System.out.println("Target: " + recv.getRemoteTarget().getAddress());
                        message.setReplyTo(recv.getRemoteSource().getAddress());
                        message.setBody(new AmqpValue(payload.encodePrettily()));

                        sender.send(message);
                    }
                }).open();

                asyncAddGame.awaitSuccess(10000);

                final Async asyncSetScore = context.async();

                ProtonReceiver recv2 = conn.createReceiver("#").handler((delivery, msg) -> {
                    LOG.info("Received message");
                    Section body = msg.getBody();
                    if (body instanceof AmqpValue) {
                        context.assertEquals(200, msg.getApplicationProperties().getValue().get("status"));
                        JsonObject payload = new JsonObject((String)((AmqpValue) body).getValue());
                        context.assertEquals("Aston Villa", payload.getString("homeTeam"));
                        context.assertEquals("Preston North End", payload.getString("awayTeam"));
                        context.assertEquals("21th January 2017, 16:00", payload.getString("startTime"));
                        context.assertEquals(0, payload.getString("homeTeamGoals"));
                        context.assertEquals(0, payload.getString("awayTeamGoals"));
                        context.assertEquals("HT", payload.getString("gameTime"));
                        asyncSetScore.complete();
                    }
                }).open().setPrefetch(1);

                conn.createSender("setScore").openHandler(openResult -> {
                    if (openResult.succeeded()) {
                        LOG.info("Sender is open");
                        ProtonSender sender = openResult.result();

                        JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("gameTime", "HT").put("homeTeamGoals", 1).put("awayTeamGoals", 0);

                        Message message = message();
                        System.out.println("Source: " + recv2.getRemoteSource().getAddress());
                        System.out.println("Target: " + recv2.getRemoteTarget().getAddress());
                        message.setReplyTo(recv2.getRemoteSource().getAddress());
                        message.setBody(new AmqpValue(payload.encodePrettily()));

                        sender.send(message);
                    }
                }).open();

                asyncSetScore.awaitSuccess(10000);

                asyncTest.complete();
            }
            else
            {
                LOG.error("Connection failed", res.cause());
                context.fail("Failed to connect to the router");
            }
        });

        asyncTest.awaitSuccess(30000);
    }

    @AfterClass
    public static void tearDown(TestContext context) {
        LiveScoreIT.vertx.close(context.asyncAssertSuccess());
    }
}
