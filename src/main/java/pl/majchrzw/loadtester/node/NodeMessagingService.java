package pl.majchrzw.loadtester.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.Action;
import pl.majchrzw.loadtester.dto.NodeStatusChange;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;
import pl.majchrzw.loadtester.master.MasterMessagingService;

import java.util.UUID;


@Component
@Profile("node")
public class NodeMessagingService implements DisposableBean {
	
	private final JmsTemplate template;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private final String configurationTopic = "configuration";
	private final String readinessTopic = "readiness";
	private NodeRequestConfig requestConfig;
	
	private UUID nodeId;
	
	Logger logger = LoggerFactory.getLogger(MasterMessagingService.class);
	
	public NodeMessagingService(JmsTemplate template) {
		this.template = template;
		template.setPubSubDomain(true);
		
	}
	
	public void transmit(UUID nodeId) {
		this.nodeId = nodeId;
		String msg;
		NodeStatusChange startup = new NodeStatusChange(nodeId, Action.START);
		try {
			msg = objectMapper.writeValueAsString(startup);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		template.convertAndSend(readinessTopic, msg);
		logger.info("Transmitted id to master from node: " + msg);
	}
	
	@JmsListener(destination = configurationTopic)
	public void receive(String config) {
		logger.info("Received request config from master: " + config);
		try {
			requestConfig = objectMapper.readValue(config, NodeRequestConfig.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public void destroy() {
		String msg;
		NodeStatusChange startup = new NodeStatusChange(nodeId, Action.STOP);
		try {
			msg = objectMapper.writeValueAsString(startup);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		template.convertAndSend(readinessTopic, msg);
		logger.info("Transmitted unexpected shutdown to master from node: " + msg);
	}
}
