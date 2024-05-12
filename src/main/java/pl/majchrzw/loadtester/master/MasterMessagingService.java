package pl.majchrzw.loadtester.master;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.NodeStatusChange;

import java.util.ArrayList;
import java.util.UUID;

@Component
@Profile("master")
public class MasterMessagingService {
	
	private final JmsTemplate template;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private final String configurationTopic = "configuration";
	private final String readinessTopic = "readiness";
	
	private final ArrayList<UUID> readinessList;
	Logger logger = LoggerFactory.getLogger(MasterMessagingService.class);
	
	public MasterMessagingService(JmsTemplate template) {
		this.template = template;
		template.setPubSubDomain(true);
		readinessList = new ArrayList<>();
	}
	
	public void transmit(NodeRequestConfig config) {
		String msg;
		try {
			msg = objectMapper.writeValueAsString(config);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		template.convertAndSend(configurationTopic, msg);
	}
	
	@JmsListener(destination = readinessTopic)
	public void receive(String msg) {
		logger.info("Received status change from node: " + msg);
		NodeStatusChange nodeStatusChange;
		try {
			nodeStatusChange = objectMapper.readValue(msg, NodeStatusChange.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		switch (nodeStatusChange.action()) {
			case START -> readinessList.add(nodeStatusChange.id());
			case STOP -> readinessList.remove(nodeStatusChange.id());
		}
		logger.info(readinessList.size() + " nodes are ready.");
	}
	
	public ArrayList<UUID> getReadinessList() {
		return readinessList;
	}
}
