package pl.majchrzw.loadtester.dto.statistics;

import java.util.UUID;

public record NodeRequestStatistics(
		long averageRequestExecutionTime,
		long minimalRequestExecutionTime,
		long maximalRequestExecutionTime,
		long requestCount,
		double successRate,
		double executionRate,
		long requestTimeout,
		String requestName
) {
}
