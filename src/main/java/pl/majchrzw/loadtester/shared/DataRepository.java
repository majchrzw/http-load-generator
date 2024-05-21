package pl.majchrzw.loadtester.shared;

import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;

import java.util.UUID;

public interface DataRepository {
	UUID getId();
	NodeRequestConfig getRequestConfig();
	NodeExecutionStatistics getNodeExecutionStatistics();
	// sets application's own execution statistics
	void setNodeExecutionStatistics(NodeExecutionStatistics statistics);
}
