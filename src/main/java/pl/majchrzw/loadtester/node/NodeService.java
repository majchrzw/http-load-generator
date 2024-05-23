package pl.majchrzw.loadtester.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pl.majchrzw.loadtester.dto.Status;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.shared.RequestExecutor;

@Service
@Profile("node")
public class NodeService {
	
	private final NodeMessagingService nodeMessagingService;
	
	private final NodeDao dao;
	
	private final RequestExecutor executor;
	
	private final Logger logger = LoggerFactory.getLogger(NodeService.class);
	
	public NodeService(NodeMessagingService nodeMessagingService, NodeDao dao) {
		this.nodeMessagingService = nodeMessagingService;
		this.dao = dao;
		this.executor = new RequestExecutor();
	}
	
	public void run() {
		nodeMessagingService.transmitReadiness();
		while (dao.getCurrentStatus().equals(Status.NEW)) {
			Thread.onSpinWait();
		}
		NodeExecutionStatistics statistics = executor.run(dao.getRequestConfig(), dao.getId());
		nodeMessagingService.transmitStatistics(statistics);
		logger.info("Finished requests and sent statistics from node: " + dao.getId() + ", closing node.");
	}
}
