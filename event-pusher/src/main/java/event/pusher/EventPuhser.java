package event.pusher;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EventPuhser extends AbstractVerticle {
  private static final Logger logger =
      LoggerFactory.getLogger(EventPuhser.class);
  KafkaProducer<JsonObject, JsonObject> producer;
Vertx vertx;

    public EventPuhser(Vertx vertx) {
        this.vertx = vertx;
    }

    private static Map<String, String> getKafkaConfig() {
    HashMap<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    config.put("acks", "1");
    return config;
  }

  private KafkaProducerRecord<JsonObject, JsonObject> makeRecord(String content) {
      JsonObject payload = new JsonObject().put("steps", 1).put("device-id", "dinhnn").put("content", content).put("timestamp", Instant.now());
      KafkaProducerRecord<JsonObject, JsonObject> record =  KafkaProducerRecord.create("counter.events", null, payload);
      return record;
  }
  @Override
  public Completable rxStart() {
    producer = KafkaProducer.create(vertx, getKafkaConfig());

    Observable.interval(10, TimeUnit.SECONDS)
        .map(
            l -> {
              logger.info("start worker: {}", LocalTime.now());
              producer
                  .rxSend(makeRecord("my-content"))
                  .subscribe();
              return Single.just("ok");
            })
        .subscribe(ok -> logger.info("timer end OK"));
      return super.rxStart();
  }

  @Override
  public Completable rxStop() {
    return super.rxStop();
  }

  public static void main(String[] args) {
      Vertx vertx = Vertx.vertx();
      vertx.rxDeployVerticle(new EventPuhser(vertx)).subscribe(ok -> {
          logger.info("Deployed Verticle: {}", EventPuhser.class);
      }, err -> {
          logger.error("Deployed failed: {}", err);
      });
  }
}
