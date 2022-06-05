import api.EventsVerticle;
import config.KafkaConfig;
import infra.ActivityRepoPqImpl;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import repo.Activity;

import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static Map<String, String> getKafkaProducerConfig() {
        HashMap<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
        config.put("acks", "1");
        return config;
    }

    private static PgPool getPgPool(Vertx vertx) {
        PgConnectOptions connectOptions =
                new PgConnectOptions()
                        .setPort(5432)
                        .setHost("localhost")
                        .setDatabase("postgres")
                        .setUser("postgres")
                        .setPassword("pass");

        PoolOptions poolOptions = new PoolOptions().setMaxSize(30);
        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();
        PgPool pgPool = getPgPool(vertx);
        Activity activityRepo = new ActivityRepoPqImpl(pgPool);

        KafkaProducer<String, JsonObject> producer =
                KafkaProducer.create(vertx, getKafkaProducerConfig());

        KafkaConsumer<String, JsonObject> consumer =
                KafkaConsumer.create(vertx, KafkaConfig.getConsumerGroupConfig("activity-service"));

        EventsVerticle eventsVerticle =
                new EventsVerticle("counter.events.new", activityRepo, consumer, producer);

        vertx
                .rxDeployVerticle(eventsVerticle)
                .subscribe(
                        ok -> {
                            logger.info("Deployed Event verticle OK");
                        });
    }
}
