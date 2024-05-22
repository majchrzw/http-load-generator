package pl.majchrzw.loadtester.dto.statistics;

import java.time.Instant;

public record NodeSingleExecutionStatistics(
		int id,
		long elapsedTime,
		int statusCode,
		Instant startTime,
		boolean success
) {
}
