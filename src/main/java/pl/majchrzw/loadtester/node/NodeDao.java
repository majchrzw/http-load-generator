package pl.majchrzw.loadtester.node;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.Status;
import pl.majchrzw.loadtester.shared.DataRepository;

import java.util.UUID;

@Component
@Profile("node")
public class NodeDao implements DataRepository {
	
	private Status currentStatus;
	private final UUID nodeId;
	private NodeRequestConfig requestConfig;
	
	private NodeExecutionStatistics statistics;
	
	public NodeDao() {
		this.currentStatus = Status.NEW;
		this.nodeId = UUID.randomUUID();
	}
	public Status getCurrentStatus() {
		return currentStatus;
	}
	
	public void setReceivedConfigurationStatus() {
		if (currentStatus.equals(Status.NEW)) {
			currentStatus = Status.RECEIVED_CONFIGURATION;
		} else {
			throw new IllegalStateException("Tried to again change status to received configuration");
		}
	}
	
	public void setRequestConfig(NodeRequestConfig config){
		requestConfig = config;
	}
	
	@Override
	public UUID getId() {
		return nodeId;
	}
	
	public NodeRequestConfig getRequestConfig(){
		return requestConfig;
	}
	
	@Override
	public NodeExecutionStatistics getExecutionStatistics() {
		return statistics;
	}
	
	@Override
	public void setExecutionStatistics(NodeExecutionStatistics statistics) {
		this.statistics = statistics;
	}
	
	public NodeExecutionStatistics getStatistics() {
		return statistics;
	}
	
	public void setStatistics(NodeExecutionStatistics statistics) {
		this.statistics = statistics;
	}
}
