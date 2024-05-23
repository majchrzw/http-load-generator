package pl.majchrzw.loadtester.node;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.Status;
import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.shared.DataRepository;

import java.util.UUID;

@Component
@Profile("node")
public class NodeDao implements DataRepository {
	
	private final UUID nodeId;
	private Status currentStatus;
	private NodeRequestConfig requestConfig;
	
	public NodeDao() {
		this.currentStatus = Status.NEW;
		this.nodeId = UUID.randomUUID();
	}
	
	public Status getCurrentStatus() {
		return currentStatus;
	}
	
	public void setCurrentStatus(Status status) {
		currentStatus = status;
	}
	
	@Override
	public UUID getId() {
		return nodeId;
	}
	
	@Override
	public NodeRequestConfig getRequestConfig() {
		return requestConfig;
	}
	
	@Override
	public void setRequestConfig(NodeRequestConfig config) {
		requestConfig = config;
	}
}
