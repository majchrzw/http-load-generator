package pl.majchrzw.loadtester.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.Action;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.NodeStatusChange;


@Component
@Profile("node")
public class NodeMessagingService implements DisposableBean {
	
	private final String configurationTopic = "configuration";
	private final String readinessTopic = "readiness";
	private final String statisticsTopic = "statistics";
	
	private final NodeDao dao;
	private final JmsTemplate template;
	
	Logger logger = LoggerFactory.getLogger(NodeMessagingService.class);
	
	public NodeMessagingService(JmsTemplate template, NodeDao dao) {
		this.template = template;
		this.dao = dao;
		template.setPubSubDomain(true);
	}
	
	public void transmitReadiness() {
		NodeStatusChange startup = new NodeStatusChange(dao.getId(), Action.START);
		template.convertAndSend(readinessTopic, startup);
		logger.info("Transmitted id to master from node: " + startup);
	}
	
	public void transmitStatistics() {
		template.convertAndSend(statisticsTopic, dao.getStatistics());
		logger.info("Transmitted execution statistics to master from node: " + dao.getId());
	}
	
	@JmsListener(destination = configurationTopic)
	public void receiveConfiguration(NodeRequestConfig config) {
		logger.info("Received request config from master: " + config);
		dao.setRequestConfig(config);
		dao.setReceivedConfigurationStatus();
	}
	
	
	@Override
	public void destroy() {
		NodeStatusChange shutdown = new NodeStatusChange(dao.getId(), Action.STOP);
		template.convertAndSend(readinessTopic, shutdown);
		logger.info("Transmitted shutdown to master from node: " + dao.getId());
	}
}
