package pl.majchrzw.loadtester.shared;

import pl.majchrzw.loadtester.dto.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;

import java.util.UUID;

public interface DataRepository {
	UUID getId();
	NodeRequestConfig getRequestConfig();
	NodeExecutionStatistics getExecutionStatistics();
	// sets application's own execution statistics
	void setExecutionStatistics(NodeExecutionStatistics statistics);
}
