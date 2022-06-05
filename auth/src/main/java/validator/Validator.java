package validator;

import io.vertx.core.json.JsonObject;

import java.util.List;

public interface Validator {
	List<String> validate(JsonObject JsonObject);
}
