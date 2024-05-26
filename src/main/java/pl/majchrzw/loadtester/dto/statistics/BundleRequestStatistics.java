package pl.majchrzw.loadtester.dto.statistics;

import pl.majchrzw.loadtester.dto.config.RequestInfo;

import java.util.List;

public record BundleRequestStatistics(
		List<OneRequestStatistics> executionStatistics,
		RequestInfo requestInfo
) {
}
