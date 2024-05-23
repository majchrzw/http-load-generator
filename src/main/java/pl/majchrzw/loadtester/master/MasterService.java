package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.collections.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.majchrzw.loadtester.dto.config.ConfigValidationStatus;
import pl.majchrzw.loadtester.dto.config.InitialConfiguration;
import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.config.RequestInfo;
import pl.majchrzw.loadtester.shared.RequestExecutor;
import pl.majchrzw.loadtester.shared.ServiceWorker;

import java.io.File;

@Service
@Profile("master")
public class MasterService implements ServiceWorker {
	private final Logger logger = LoggerFactory.getLogger(MasterService.class);
	private final MasterMessagingService messagingService;
	private final MasterDao dao;
	
	private final RequestExecutor executor;
	
	public MasterService(MasterMessagingService messagingService, MasterDao dao, RequestExecutor executor) {
		this.messagingService = messagingService;
		this.dao = dao;
		this.executor = executor;
	}
	
	@Override
	public void run() {
		InitialConfiguration initialConfiguration = readInitialConfiguration();
		int nodes = initialConfiguration.nodes();

		NodeRequestConfig nodeRequestConfig = prepareNodesConfiguration(initialConfiguration);
		prepareMasterConfiguration(initialConfiguration);

		while (dao.numberOfReadyNodes() < nodes) {
			Thread.onSpinWait();
		}

		logger.info("All nodes are ready, sending configuration");
		messagingService.transmitConfiguration(nodeRequestConfig);
		executor.run();
		// wait for all nodes to finish
		while (dao.numberOfFinishedNodes() < nodes) {
			Thread.onSpinWait();
		}
		// generate statistics of run
		processStatistics(); // TODO - to potem do usunięcia
		StatisticsCalculator calculator = new StatisticsCalculator();
		calculator.generateAllStatistics(dao.getAllExecutionStatistics());
	}
	
	private void processStatistics() {
		// TODO - potem to usunąć
		dao.getAllExecutionStatistics().forEach((uuid, statistics) -> System.out.println("Statistics for: " + uuid + " - " + statistics));
	}
	
	private InitialConfiguration readInitialConfiguration() {
		// TODO - dodać na pewno walidację danych wejściowych: int większe od zera, enum dobrze ustalony itd..
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
		File file = new File("requests.json");
		InitialConfiguration configuration = null;
		try {
			if (file.exists()) {
				configuration = objectMapper.readValue(file, InitialConfiguration.class);
			} else {
				ClassPathResource requestsResource = new ClassPathResource("requests.json");
				configuration = objectMapper.readValue(requestsResource.getFile(), InitialConfiguration.class);
			}
		} catch (Exception e) {
			logger.error("Cannot read configuration from requests.json file");
			System.exit(-1);
		}
		if (configuration.nodes() < 0) {
			throw new IllegalArgumentException("Number of nodes must be 0 or greater");
		}
		return configuration;
	}
	
	private NodeRequestConfig prepareNodesConfiguration(InitialConfiguration initialConfiguration) {
		return new NodeRequestConfig(initialConfiguration.requests().stream().map(request -> {
			MultiValueMap requestHeaders = new MultiValueMap();
			requestHeaders.putAll(initialConfiguration.defaultHeaders());
			requestHeaders.putAll(request.headers());
			
			int count = request.count() / (initialConfiguration.nodes() + 1);
			
			return new RequestInfo(request.method(), request.uri(), requestHeaders, request.body(), request.name(), request.timeout(),request.expectedReturnStatusCode(), count);
		}).toList(), initialConfiguration.nextRequestDelay().orElse(100L));
	}
	
	private void prepareMasterConfiguration(InitialConfiguration initialConfiguration) {
		int nodes = initialConfiguration.nodes() + 1; // all nodes (plus master)
		
		var masterRequestConfig = new NodeRequestConfig(initialConfiguration.requests().stream().map(request -> {
			MultiValueMap requestHeaders = new MultiValueMap();
			requestHeaders.putAll(initialConfiguration.defaultHeaders());
			requestHeaders.putAll(request.headers());
			
			int base = request.count() / nodes;
			int remainder = request.count() % nodes;
			
			return new RequestInfo(request.method(), request.uri(), requestHeaders, request.body(), request.name(), request.timeout(),request.expectedReturnStatusCode(), base + remainder);
		}).toList(), initialConfiguration.nextRequestDelay().orElse(100L));
		dao.setRequestConfig(masterRequestConfig);
	}
	
}

