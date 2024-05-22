package pl.majchrzw.loadtester.shared;

import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;

import java.util.UUID;

public interface DataRepository {
	UUID getId();
	
	NodeRequestConfig getRequestConfig();
	
	NodeExecutionStatistics getNodeExecutionStatistics();
	
	// sets application's own execution statistics
	void setNodeExecutionStatistics(NodeExecutionStatistics statistics);
}
