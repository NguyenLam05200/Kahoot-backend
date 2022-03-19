package config;

import java.util.HashMap;
import java.util.Map;

public class KafkaConfig {
    private static final String bootstrapServer = "KAFKA_BOOTSTRAP_SERVER";
    private static final String keySerializer = "";
    private static final String valueSerializer = "";

    public static Map<String, String> getConsumerGroupConfig() {
        return getConsumerGroupConfig("random string");
    }

    public static Map<String, String> getConsumerGroupConfig(String serviceName) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", System.getenv().getOrDefault(bootstrapServer, "localhost:9092"));
        config.put("key.deserializer", System.getenv().getOrDefault(keySerializer, "org.apache.kafka.common.serialization.StringDeserializer"));
        config.put("value.deserializer", System.getenv().getOrDefault(keySerializer, "io.vertx.kafka.client.serialization.JsonObjectDeserializer"));
        config.put("group.id", serviceName);
        config.put("auto.offset.reset", "latest");
        config.put("enable.auto.commit", "true");
        return config;
    }
}
