package pl.majchrzw.loadtester.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pl.majchrzw.loadtester.dto.Status;
import pl.majchrzw.loadtester.shared.RequestExecutor;
import pl.majchrzw.loadtester.shared.ServiceWorker;

@Service
@Profile("node")
public class NodeService implements ServiceWorker {
	
	private final NodeMessagingService nodeMessagingService;
	
	private final NodeDao dao;
	
	private final RequestExecutor executor;
	
	private final Logger logger = LoggerFactory.getLogger(NodeService.class);
	
	public NodeService(NodeMessagingService nodeMessagingService, NodeDao dao, RequestExecutor executor) {
		this.nodeMessagingService = nodeMessagingService;
		this.dao = dao;
		this.executor = executor;
	}
	
	
	@Override
	public void run() {
		nodeMessagingService.transmitReadiness();
		while (dao.getCurrentStatus().equals(Status.NEW)) {
			Thread.onSpinWait();
		}
		executor.run();
		nodeMessagingService.transmitStatistics();
		logger.info("Finished requests and sent statistics from node: " + dao.getId() + ", closing node.");
	}
}
