package api;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import repo.Activity;

import java.sql.Timestamp;
import java.time.Instant;

public class EventsVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(EventsVerticle.class);

  private static final String KafkaTopicName = "KAFKA_TOPIC";
  private final String stepsUpdateKafkaTopic = "todo2";
  private final String kafkaTopic;
  private final Activity activityRepo;
  private final Integer hourForAccumSteps = 1;
  private final KafkaConsumer<String, JsonObject> consumer;
  private final KafkaProducer<String, JsonObject> producer;

  private Observable<KafkaConsumerRecord<String, JsonObject>> writeToDB(
      KafkaConsumerRecord<String, JsonObject> record) {
    String deviceId = record.value().getString("device-id");
    String deviceSync = record.value().getString("device-sync");
    Integer counter = record.value().getInteger("steps");
    logger.debug("[] got record" + deviceId + counter);
    return activityRepo.write(deviceId, deviceSync, counter).map(row -> record);
  }

  private Observable<KafkaConsumerRecord<String, JsonObject>> generateUpdateEvents(
      KafkaConsumerRecord<String, JsonObject> record) {

    String deviceId = record.value().getString("device-id");

    return activityRepo
        .countStepsInHours(deviceId, hourForAccumSteps)
        .map(
            steps ->
                new JsonObject()
                    .put("device-id", deviceId)
                    .put("steps", steps)
                    .put("timestamp", Timestamp.from(Instant.now())))
        .map(
            payload ->
                KafkaProducerRecord.<String, JsonObject>create(
                    stepsUpdateKafkaTopic, null, payload))
        .flatMap(kafkaRecord -> producer.rxSend(kafkaRecord))
        .map(rs -> record)
        .toObservable();
  }

  public EventsVerticle(
      String kafkaTopic,
      Activity activityRepo,
      KafkaConsumer<String, JsonObject> consumer,
      KafkaProducer<String, JsonObject> producer) {
    this.kafkaTopic = kafkaTopic;
    this.activityRepo = activityRepo;
    this.consumer = consumer;
    this.producer = producer;
  }

  @Override
  public Completable rxStart() {

    consumer
        .subscribe(kafkaTopic)
        .toObservable()
        .flatMap(this::writeToDB)
        .flatMap(this::generateUpdateEvents)
        .map(
            record -> {
              return consumer.rxCommit();
            })
        .onErrorResumeNext(
            error -> {
              if (error instanceof SerializationException) {
                logger.warn("parsing message failed, skipped");
                String s =
                    ((SerializationException) error)
                        .getMessage()
                        .split("Error deserializing key/value for partition ")[1]
                        .split(". If needed, please seek past the record to continue consumption.")[
                        0];
                String topics = s.split("-")[0];
                int offset = Integer.valueOf(s.split("offset ")[1]);
                int partition = Integer.valueOf(s.split("-")[1].split(" at")[0]);

                TopicPartition topicPartition = new TopicPartition(topics, partition);
                consumer.seek(topicPartition, offset + 1);
                consumer.commit();

                return consumer.toObservable();
              }
              return Observable.error(error);
            })
        .retry()
        .subscribe(logger::info, logger::warn);

    return Completable.complete();
  }
}
