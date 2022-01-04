package api;

import io.vertx.amqp.AmqpClientOptions;
import io.vertx.rxjava3.amqp.AmqpMessage;
import io.vertx.amqp.AmqpReceiverOptions;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.amqp.AmqpClient;
import io.vertx.rxjava3.amqp.AmqpReceiver;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;

import java.util.HashMap;
import java.util.Map;

public class IngestVerticle extends AbstractVerticle {
    static final Logger logger = LoggerFactory.getLogger(IngestVerticle.class);
    private  KafkaProducer<String, JsonObject>  updateProducer;
    private static final String updateEventName = "incoming.steps";
    private AmqpClientOptions loadOptions() {
        return new AmqpClientOptions()
                .setHost("localhost")
                .setPort(5672)
                .setUsername("admin")
                .setPassword("pass");
    }

    private AmqpReceiverOptions loadReceiverOptions() {
        return new AmqpReceiverOptions()
                .setAutoAcknowledgement(false)
                .setDurable(true);
    }

    private Boolean validateEntries(JsonObject payload) {
        return payload.containsKey("device-id") && payload.containsKey("user-name");
    }

    private void handleStepCounter(AmqpMessage message) {
        if (!message.contentType().equals("application/json")) {
            logger.error("message is not json" +  message.bodyAsByte());
            message.accepted();
        }
        JsonObject payload = message.bodyAsJsonObject();

        KafkaProducerRecord<String, JsonObject> record = makeProducerRecord(payload);
        // publish message
        updateProducer
                .rxSend(record)
                .subscribe(
                ok -> message.accepted(),
                err -> {
                    logger.error("ingestion failed", err);
                    message.rejected();
                }
        );

    }

    KafkaProducerRecord<String, JsonObject> makeProducerRecord(JsonObject payload) {
        JsonObject record = new JsonObject();
        record.put("device-id", payload.getString("device-id"));

        return KafkaProducerRecord.create(updateEventName,"a", record );
    }

    Map<String, String> loadKafkaConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092"); config.put("key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer"); config.put("value.serializer",
                "io.vertx.kafka.client.serialization.JsonObjectSerializer"); config.put("acks", "1");
        return config;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);
        String eventName = "step-events";
        updateProducer = KafkaProducer.create(vertx, loadKafkaConfig());

        AmqpClient.create(vertx, loadOptions())
                .rxConnect()
                .flatMap(conn -> conn.rxCreateReceiver("steps-events", loadReceiverOptions()))
                .flatMapPublisher(AmqpReceiver::toFlowable)
                .doOnError(throwable ->  {
                    logger.error(throwable.getMessage());
                })
                .subscribe(this::handleStepCounter);


        Router router = Router.router(vertx);
        router.post().handler(BodyHandler.create());
//        router.post("/ingest").handler(this::handleStepCounter);

        vertx
                .createHttpServer()
                .requestHandler(router)
                .rxListen(82)
                .ignoreElement();

    }
}
