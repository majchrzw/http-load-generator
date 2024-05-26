package pl.majchrzw.loadtester.dto.statistics;

public record CalculatedRequestStatistics(
		long averageRequestExecutionTime,
		long minimalRequestExecutionTime,
		long maximalRequestExecutionTime,
		long requestCount,
		long successfulRequestCount,
		long executedRequestsCount,
		double successRate,
		double executionRate,
		long requestTimeout,
		int expectedResponseStatus,
		String requestName
) {
}
