package pl.majchrzw.loadtester.dto.statistics;

import java.time.Instant;

public record OneRequestStatistics(
		int id,
		long elapsedTime,
		int statusCode,
		String responseBody,
		Instant startTime,
		boolean success,
		boolean executed
) {
}
