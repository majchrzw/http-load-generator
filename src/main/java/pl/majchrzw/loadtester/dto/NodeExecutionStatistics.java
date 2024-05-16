package pl.majchrzw.loadtester.dto;

import java.util.List;
import java.util.UUID;

public record NodeExecutionStatistics (
		UUID nodeId,
		List<NodeBundleExecutionStatistics> bundleExecutionStatistics
){
}