package pl.majchrzw.loadtester.master;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.Status;
import pl.majchrzw.loadtester.shared.DataRepository;

import java.util.HashMap;
import java.util.UUID;

@Component
@Profile("master")
public class MasterDao implements DataRepository {
	
	private final HashMap<UUID, Status> nodeStatus;
	
	private NodeRequestConfig requestConfig;
	
	private final UUID masterId;
	
	private HashMap<UUID, NodeExecutionStatistics> executionStatistics;
	
	public MasterDao() {
		this.masterId = UUID.randomUUID();
		this.nodeStatus = new HashMap<>();
		executionStatistics = new HashMap<>();
	}
	
	public void registerNewNode(UUID nodeId) {
		nodeStatus.put(nodeId, Status.NEW);
	}
	
	public void deleteNodeFromList(UUID nodeId) {
		nodeStatus.put(nodeId, Status.CLOSED);
	}
	
	public void setNodeStatusAsSendingRequests(UUID nodeId) {
		nodeStatus.put(nodeId, Status.SENDING_REQUESTS);
	}
	
	public void setNodeStatusAsFinishedSending(UUID nodeId) {
		nodeStatus.put(nodeId, Status.FINISHED_SENDING);
	}
	
	public void addNodeExecutionStatistics(NodeExecutionStatistics statistics) {
		executionStatistics.put(statistics.nodeId(), statistics);
	}
	
	public HashMap<UUID, NodeExecutionStatistics> getAllExecutionStatistics() {
		return executionStatistics;
	}
	
	public int numberOfReadyNodes() {
		return nodeStatus.size();
	}
	
	@Override
	public UUID getId() {
		return masterId;
	}
	
	public NodeRequestConfig getRequestConfig() {
		return requestConfig;
	}
	
	@Override
	public NodeExecutionStatistics getExecutionStatistics() {
		return executionStatistics.get(masterId);
	}
	
	@Override
	public void setExecutionStatistics(NodeExecutionStatistics statistics) {
		executionStatistics.put(masterId, statistics);
	}
	
	public void setRequestConfig(NodeRequestConfig requestConfig) {
		this.requestConfig = requestConfig;
	}
	
}
