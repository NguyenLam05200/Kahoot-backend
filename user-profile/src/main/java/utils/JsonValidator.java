package utils;

import io.vertx.core.json.JsonObject;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsonValidator {

    public class InvalidJsonException extends Exception {
        public InvalidJsonException(String message) {
            super(message);
        }
    }

    private String[] requiredKeys;
    private String[] optionalKeys;
    public static  JsonValidator create(String[] requiredKeys, String... optionalKeys) {
        JsonValidator self = new JsonValidator();
        self.requiredKeys = requiredKeys;
        self.optionalKeys = optionalKeys;

        return self;
    }

    public boolean validate(JsonObject payload) {
        if (payload == null)
            return false;

        List<String> missingKeys = Arrays.stream(requiredKeys).filter(key ->
            !payload.containsKey(key)
        ).collect(Collectors.toList());
        if (missingKeys.isEmpty()) {
            return true;
        }

    System.out.println("missing keys: " + missingKeys);
        return false;
    }

    public static void main(String[] args){
    }
}
