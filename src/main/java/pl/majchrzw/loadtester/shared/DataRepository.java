package pl.majchrzw.loadtester.shared;

import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;

import java.util.UUID;

public interface DataRepository {
	UUID getId();
	
	NodeRequestConfig getRequestConfig();
	
	void setRequestConfig(NodeRequestConfig config);
}
