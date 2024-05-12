package pl.majchrzw.loadtester.node;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pl.majchrzw.loadtester.shared.ServiceWorker;

import java.util.UUID;

@Service
@Profile("node")
public class NodeService implements ServiceWorker {
	
	private final NodeMessagingService nodeMessagingService;
	
	UUID nodeId;
	
	public NodeService(NodeMessagingService nodeMessagingService) {
		this.nodeMessagingService = nodeMessagingService;
		nodeId = UUID.randomUUID();
	}
	
	
	@Override
	public void run() {
		nodeMessagingService.transmit(nodeId);
	}
}
