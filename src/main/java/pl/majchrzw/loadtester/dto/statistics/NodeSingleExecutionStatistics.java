package pl.majchrzw.loadtester.dto.statistics;

import java.time.Instant;

public record NodeSingleExecutionStatistics(
		int id,
		Long elapsedTime,
		Integer statusCode,
		Instant startTime

) {
}
