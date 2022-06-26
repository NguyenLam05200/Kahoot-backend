package eventbridge;

import java.time.Duration;
import java.time.Instant;

public class Utils {
	public long calculatePoints(int basePoint, Instant startTime, Instant answerTime, Duration limit) {
		Duration neededTime = Duration.between(startTime, answerTime);
		if (neededTime.isNegative()) {
			return 0;
		}
		return (1 -  neededTime.getSeconds() / limit.getSeconds() / 2 ) * basePoint;
	}
}
