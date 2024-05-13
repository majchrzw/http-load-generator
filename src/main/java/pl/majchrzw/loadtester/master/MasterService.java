package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.majchrzw.loadtester.dto.MasterRequestConfig;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.RequestInfo;
import pl.majchrzw.loadtester.shared.RequestExecutor;
import pl.majchrzw.loadtester.shared.ServiceWorker;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("master")
public class MasterService implements ServiceWorker {
	private final Logger logger = LoggerFactory.getLogger(MasterService.class);
	private final MasterMessagingService messagingService;
	private final MasterDao dao;
	
	private RequestExecutor executor;
	
	public MasterService(MasterMessagingService messagingService, MasterDao dao, RequestExecutor executor) {
		this.messagingService = messagingService;
		this.dao = dao;
		this.executor = executor;
	}
	
	@Override
	public void run() {
		ObjectMapper objectMapper = new ObjectMapper();
		ClassPathResource requestsResource = new ClassPathResource("requests.json");
		MasterRequestConfig requestConfig;
		try {
			requestConfig = objectMapper.readValue(requestsResource.getFile(), MasterRequestConfig.class);
		} catch (Exception e) {
			// jak źle wczyta config to po prostu się zamyka
			logger.warn(e.getMessage());
			return;
		}
		
		final int nodes = requestConfig.nodes() + 1;
		List<RequestInfo> masterRequestsList = new ArrayList<>();
		List<RequestInfo> nodeRequestsList = new ArrayList<>();
		for (RequestInfo requestInfo : requestConfig.requests()) {
			// prepare headers
			MultiValueMap requestHeaders = new MultiValueMap();
			requestHeaders.putAll(requestConfig.defaultHeaders());
			requestHeaders.putAll(requestInfo.headers());
			// prepare amount
			int baseAmount = requestInfo.count() / nodes;
			int remainder = requestInfo.count() % nodes;
			
			masterRequestsList.add(new RequestInfo(requestInfo.method(), requestInfo.uri(), requestHeaders, requestInfo.body(), requestInfo.name(), baseAmount + remainder));
			nodeRequestsList.add(new RequestInfo(requestInfo.method(), requestInfo.uri(), requestHeaders, requestInfo.body(), requestInfo.name(), baseAmount));
		}
		dao.setRequestConfig(new NodeRequestConfig(masterRequestsList));
		
		try {
			while ( dao.numberOfReadyNodes() < requestConfig.nodes()){
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		logger.info("All nodes are ready, sending configuration");
		messagingService.transmitConfiguration(new NodeRequestConfig(nodeRequestsList));

		processStatistics();
	}
	
	private void processStatistics(){
		// TODO-tutaj może być jakaś obróbka tych danych np. zapisanie do pliku, albo wykresy
		dao.getAllExecutionStatistics().forEach( (uuid, statistics) -> System.out.println("Statistics for: " + uuid + " - " + statistics));
	}
	
}

