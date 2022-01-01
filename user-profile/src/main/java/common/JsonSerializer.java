package common;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class JsonSerializer {
    public String serialize(Object object) throws Exception {
        try {
            Class<?> objClass = requireNonNull(object).getClass();

            Map<String, String> jsonElements = new HashMap<>();
            for (Field field: objClass.getDeclaredFields()) {
                field.setAccessible(true);
                // import the declared annotation and check
                // whether it existed
                if (field.isAnnotationPresent(JsonElement.class)) {
                    jsonElements.put(getKey(field), (String) field.get(object));
                }
            }
        } catch (Exception e) {
            throw new Exception(e);
        }


        return "";
    }

    private String toJsonString(Map<String, String> jsonMap) {
        String elementsString = jsonMap
                .entrySet()
                .stream()
                .map(entry -> "\"" +entry.getKey() + "\":\"" + "\"")
                .collect(Collectors.joining(","))
                ;
        return "{" + elementsString + "}";
    }

    private static String getKey(Field field) {
        String annotationValue = field.getAnnotation(JsonElement.class).key();
        if (annotationValue.isEmpty()) {
            return field.getName();
        }
        return annotationValue;
    }
}
