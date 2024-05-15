package pl.majchrzw.loadtester.dto;

import java.util.List;

public record NodeBundleExecutionStatistics(
        List<NodeSingleExecutionStatistics> executionStatistics,
        RequestInfo requestInfo
) {
}
