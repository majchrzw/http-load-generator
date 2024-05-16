package pl.majchrzw.loadtester.master;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.InitialConfiguration;
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
	
	private NodeRequestConfig nodeRequestConfig;
	
	
	
	private InitialConfiguration initialConfiguration;
	private NodeRequestConfig masterRequestConfig;
	private final UUID masterId;
	
	private HashMap<UUID, NodeExecutionStatistics> executionStatistics;
	
	public MasterDao() {
		this.masterId = UUID.randomUUID();
		this.nodeStatus = new HashMap<>();
		executionStatistics = new HashMap<>();
	}
	
	public void registerNewNode(UUID nodeId, Status status) {
		nodeStatus.put(nodeId, status);
	}
	
	public void addNodeExecutionStatistics(NodeExecutionStatistics statistics) {
		executionStatistics.put(statistics.nodeId(), statistics);
	}
	
	public HashMap<UUID, NodeExecutionStatistics> getAllExecutionStatistics() {
		return executionStatistics;
	}
	
	public int numberOfReadyNodes() {
		return (int) nodeStatus.values()
				.stream()
				.filter(status -> status.equals(Status.NEW))
				.count();
	}
	
	public int numberOfFinishedNodes() {
		return (int) nodeStatus.values()
				.stream()
				.filter(status -> status.equals(Status.CLOSED))
				.count();
	}
	
	@Override
	public UUID getId() {
		return masterId;
	}
	
	@Override
	public NodeRequestConfig getRequestConfig() {
		return masterRequestConfig;
	}
	
	public void setRequestConfig(NodeRequestConfig requestConfig){
		this.masterRequestConfig = requestConfig;
	}
	
	public NodeRequestConfig getNodeRequestConfig(){
		return nodeRequestConfig;
	}
	
	@Override
	public NodeExecutionStatistics getExecutionStatistics() {
		return executionStatistics.get(masterId);
	}
	
	@Override
	public void setExecutionStatistics(NodeExecutionStatistics statistics) {
		executionStatistics.put(masterId, statistics);
	}
	
	public void setNodeRequestConfig(NodeRequestConfig nodeRequestConfig) {
		this.nodeRequestConfig = nodeRequestConfig;
	}
	
	public InitialConfiguration getInitialConfiguration() {
		return initialConfiguration;
	}
	
	public void setInitialConfiguration(InitialConfiguration initialConfiguration) {
		this.initialConfiguration = initialConfiguration;
	}
	
}
