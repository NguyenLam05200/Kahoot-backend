package common;

import entity.User;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class JsonSerializer {
    public String serialize(Object object) throws Exception {
        Map<String, String> jsonElements;
        try {
            Class<?> objClass = requireNonNull(object).getClass();

            jsonElements = new HashMap<>();
            for (Field field: objClass.getDeclaredFields()) {
                field.setAccessible(true);
                // import the declared annotation and check
                // whether it existed
                if (field.isAnnotationPresent(JsonElement.class)) {
                    jsonElements.put(getKey(field), (String) field.get(object));
                } else {
                    jsonElements.put(field.getName(), (String) field.get(object));
                }
            }
        } catch (Exception e) {
            throw new Exception(e);
        }

        return "\"{" + jsonElements.entrySet().stream().map(key -> "\"" + key.getKey() + ":" + key.getValue() + "\"").collect(Collectors.joining(",")) + "}\"";
    }

    private static String getKey(Field field) {
        String annotationValue = field.getAnnotation(JsonElement.class).key();
        if (annotationValue.isEmpty()) {
            return field.getName();
        }
        return annotationValue;
    }

    public static void main(String[] args) {
        User u = new User("dinhnn", "abc");
        JsonSerializer serializer = new JsonSerializer();
        try {
            String js  = serializer.serialize(u);
            System.out.println("LOL: " + js);

        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
