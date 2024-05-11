package pl.majchrzw.loadtester.shared.messaging;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;

@Component
public class MessagingService {
	
	private final JmsTemplate template;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private final String topicName = "configuration";
	
	Logger logger = LoggerFactory.getLogger(MessagingService.class);
	
	public MessagingService(JmsTemplate template) {
		this.template = template;
		logger.info("Initialized");
	}
	
	public void transmit(NodeRequestConfig config) {
		template.setPubSubDomain(true);
		try {
			String msg = objectMapper.writeValueAsString(config);
			template.convertAndSend(topicName, msg);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	@JmsListener(destination = topicName)
	public void receive(String config) {
		System.out.println("Receiving: " + config);
	}
}
