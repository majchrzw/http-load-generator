package pl.majchrzw.loadtester.dto.statistics;

import pl.majchrzw.loadtester.dto.config.RequestInfo;

import java.util.List;

public record NodeBundleExecutionStatistics(
        List<NodeSingleExecutionStatistics> executionStatistics,
        RequestInfo requestInfo
) {
}
