package service.congrats;

import config.KafkaConfig;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mail.MailClient;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/*
    Implement a handler to be registered to an Event
* */
public class CongratsService extends AbstractVerticle {
  WebClient webClient;
  Map<String, String> config;
  MailClient mailClient;
  private static final Logger logger = LoggerFactory.getLogger(CongratsService.class);
  private static final String KafkaTopicName = "KAFKA_TOPIC";
  private String kafkaTopic;

  public CongratsService(Vertx vertx) {
    MailConfig mailConfig = new MailConfig().setHostname("localhost").setPort(1025);
    mailClient = MailClient.createShared(vertx, mailConfig);
    webClient = WebClient.create(vertx);
    config = KafkaConfig.getConsumerGroupConfig("congrats-service");
    kafkaTopic = System.getenv().getOrDefault(KafkaTopicName, "new.daily.steps.updates");
  }

  private Single<String> getEmail(String username) {
    return webClient
        .get(5001, "localhost", "/api/users/" + username)
        .rxSend()
        .map(HttpResponse::bodyAsJsonObject)
        .map(json -> json.getString("email"));
  }

  private Single<MailMessage> makeEmail(Integer stepsCount, String email) {
    return Single.just(
        new MailMessage()
            .setFrom("Dinhnn@test.com")
            .setTo(email)
            .setSubject("Congrats")
            .setText("congrats on achieving " + stepsCount));
  }

  private Single<MailResult> sendEmail(MailMessage mailMessage) {
    return mailClient.rxSendMail(mailMessage);
  }

  private boolean above10K(KafkaConsumerRecord<String, JsonObject> record) {
    return true;
  }

  private boolean validJson(KafkaConsumerRecord<String, JsonObject> record) {
    return StringUtils.isNotEmpty(record.value().getString("device-id"))
        && StringUtils.isNotEmpty(record.value().getString("steps"));
  }

  @Override
  public Completable rxStart() {
    KafkaConsumer<String, JsonObject> consumer = KafkaConsumer.create(this.vertx, config);

    consumer
        .rxSubscribe(kafkaTopic)
        .subscribe(
            () -> {
              logger.info("subscribed to topic: " + kafkaTopic);
            });

    consumer
        .toObservable()
        .filter(this::validJson)
        .filter(this::above10K)
        .flatMapSingle(
            record -> {
              String deviceID = record.value().getString("device-id");
              Integer steps = record.value().getInteger("steps");

              return webClient
                  .get(5001, "localhost", "/api/users/owns/" + deviceID)
                  .rxSend()
                  .map(HttpResponse::bodyAsJsonObject)
                  .map(json -> json.getString("username"))
                  .flatMap(this::getEmail)
                  .flatMap(email -> makeEmail(steps, email))
                  .flatMap(this::sendEmail);
            })
        .subscribe(
            mailResult -> {
              logger.info("Send mail OK");
            },
            err -> {
              logger.error("Failed to send email: " + err.getMessage());
            });

    return super.rxStart();
  }

  public static void main(String[] agrs) {
    Vertx vertx = Vertx.vertx();
    vertx
        .rxDeployVerticle(new CongratsService(vertx))
        .subscribe(
            ok -> {
              logger.info("Deployed Congrats Verticle: {}");
            },
            err -> {
              logger.error("Deployed Congrats failed: {}" + err);
            });
  }
}
