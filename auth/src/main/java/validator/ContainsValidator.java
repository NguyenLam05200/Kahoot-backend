package validator;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ContainsValidator implements Validator {
	private  String[] requiredKeys;

	@Override
	public List<String> validate(JsonObject jsonObject) {
		List<String> errors = new ArrayList<>();
		for (String key : requiredKeys) {
			if (!jsonObject.containsKey(key)) {
				errors.add("Missing key: "+key);
			}
		}
		return errors;
	}
}
