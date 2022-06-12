package api;


import io.reactivex.Flowable;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpReceiverOptions;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.amqp.AmqpClient;
import io.vertx.reactivex.amqp.AmqpMessage;
import io.vertx.reactivex.amqp.AmqpReceiver;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import utils.JsonValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IngestVerticle extends AbstractVerticle {

    static final Logger logger = LoggerFactory.getLogger(IngestVerticle.class);
    private static final String stepsEventsKafkaEventName = "steps-events";
    private static final String stepsEventsAmqpEventName = "new-steps-events";

    private KafkaProducer<String, JsonObject> updateProducer;
    private JsonValidator stepMessageJsonValidator;

    private AmqpClientOptions loadAmqpClientOptions() {
        return new AmqpClientOptions().setHost("localhost").setPort(5672).setUsername("user").setPassword("pass");
    }

    private AmqpReceiverOptions loadAmqpReceiverOptions() {
        return new AmqpReceiverOptions().setAutoAcknowledgement(true).setDurable(true)
                ;
    }

    private Map<String, String> loadKafkaProducerOptions() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
        config.put("acks", "1");

        return config;
    }

    KafkaProducerRecord<String, JsonObject> makeProducerRecord(JsonObject payload) {
        return KafkaProducerRecord.create(stepsEventsKafkaEventName, null, payload);
    }

    private Flowable<Throwable> retryLater(Flowable<Throwable> errs) {
        return errs.delay(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx));
    }

    private void handleStepCounter(AmqpMessage message) {

        JsonObject payload = new JsonObject(message.bodyAsString());
        if (payload.isEmpty()) {
            logger.error("parse message to json failed payload: " + payload);
            return;
        }

        if (!stepMessageJsonValidator.validate(payload)) {
            message.accepted();
            logger.error("invalid json");
            return;
        }

        KafkaProducerRecord<String, JsonObject> record = makeProducerRecord(payload);
        updateProducer.rxSend(record).subscribe(ok -> {
            message.accepted();
            logger.info("message pushed to kafka");
        }, err -> {
            logger.error("ingestion failed", err);
            message.rejected();
        });
    }

    private void handleStepCounter(RoutingContext rc) {
        JsonObject payload = rc.getBodyAsJson();

        if (!stepMessageJsonValidator.validate(payload) || payload.isEmpty()) {
            rc.response().rxEnd("invalid parameters");
            return;
        }

        KafkaProducerRecord<String, JsonObject> record = makeProducerRecord(payload);

        updateProducer.rxSend(record).subscribe(ok -> {
            logger.info("message pushed to kafka");
            rc.response().rxEnd("message consumed").subscribe();
        }, err -> {
            rc.response().rxEnd("internal error");
            logger.error("ingestion failed", err);
        });
    }


    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        updateProducer = KafkaProducer.create(vertx, loadKafkaProducerOptions());
        stepMessageJsonValidator = JsonValidator.create(new String[]{"device-id", "user-name"});

        AmqpClient.create(vertx, loadAmqpClientOptions())
                .rxConnect()
                .flatMap(conn -> conn.rxCreateReceiver(stepsEventsAmqpEventName, loadAmqpReceiverOptions()))
                .flatMapPublisher(AmqpReceiver::toFlowable)
                .doOnError(throwable -> {
                    logger.error(throwable.getMessage());
                }).retryWhen(this::retryLater)
                .subscribe(
                        this::handleStepCounter,
                        throwable -> {
                            logger.error(throwable);
                        })
                ;


        Router router = Router.router(vertx);
        router.post().handler(BodyHandler.create());
        router.post("/ingest").handler(this::handleStepCounter);

        vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(8081)
                .subscribe( start -> {
                    logger.info("start http server at");
                    startPromise.complete();
                }, startPromise::fail)

        ;

    }
}
