package pl.majchrzw.loadtester.dto.statistics;

import java.util.List;
import java.util.UUID;

public record NodeExecutionStatistics(
		UUID nodeId,
		List<NodeBundleExecutionStatistics> bundleExecutionStatistics
) {
}