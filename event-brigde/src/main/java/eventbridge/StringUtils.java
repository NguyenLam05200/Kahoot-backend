package eventbridge;

import java.util.Random;

public final class StringUtils {
	private StringUtils() {
	}

	static final String upperAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static final String lowerAlphabet = "abcdefghijklmnopqrstuvwxyz";
	static final String numbers = "0123456789";

	/**  Returns a random string generated from source.
	 * */
	public static String random(String source, int length) {
		Random random = new Random();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			// generate random index number
			int index = random.nextInt(source.length());

			// get character specified by index
			// from the string
			char randomChar = source.charAt(index);

			// append the character to string builder
			sb.append(randomChar);
		}
		return sb.toString();
	}
	public static String random( int length) {
		return random(upperAlphabet + numbers, length);
	}
}
