package pl.majchrzw.loadtester.dto.statistics;

public record NodeSingleExecutionStatistics(
		int id,
        Long elapsedTime,
        Integer statusCode

) {
}
