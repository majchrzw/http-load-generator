package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.majchrzw.loadtester.dto.config.InitialConfiguration;
import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.config.RequestInfo;
import pl.majchrzw.loadtester.shared.RequestExecutor;
import pl.majchrzw.loadtester.shared.ServiceWorker;

@Service
@Profile("master")
public class MasterService implements ServiceWorker {
	private final Logger logger = LoggerFactory.getLogger(MasterService.class);
	private final MasterMessagingService messagingService;
	private final MasterDao dao;
	
	private final RequestExecutor executor;
	private final StatisticsCalculator statisticsCalculator;
	
	public MasterService(MasterMessagingService messagingService, MasterDao dao, RequestExecutor executor, StatisticsCalculator statisticsCalculator) {
		this.messagingService = messagingService;
		this.dao = dao;
		this.executor = executor;
		this.statisticsCalculator = statisticsCalculator;
	}
	
	@Override
	public void run() {
		readInitialConfiguration();
		
		prepareNodesConfiguration();
		prepareMasterConfiguration();
		
		try {
			while (dao.numberOfReadyNodes() < dao.getInitialConfiguration().nodes()) {
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		logger.info("All nodes are ready, sending configuration");
		messagingService.transmitConfiguration();
		executor.run();
		
		try {
			while (dao.numberOfFinishedNodes() < dao.getInitialConfiguration().nodes()) {
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		processStatistics();
	}
	
	private void processStatistics() {
		// TODO-tutaj może być jakaś obróbka tych danych np. zapisanie do pliku, albo wykresy
		// TODO-aktualnie dane z requestów są oddzielne dla każego node-a, trzeba je połączyć z powrotem albo rysować oddzielnie dla każdej maszyny
		dao.getAllExecutionStatistics().forEach((uuid, statistics) -> System.out.println("Statistics for: " + uuid + " - " + statistics));
		statisticsCalculator.drawAllPlots(dao.getExecutionStatistics());
		statisticsCalculator.calculateAllStatistics(dao.getExecutionStatistics());
	}
	
	private void readInitialConfiguration() {
		ObjectMapper objectMapper = new ObjectMapper();
		ClassPathResource requestsResource = new ClassPathResource("requests.json");
		InitialConfiguration initialConfiguration;
		try {
			initialConfiguration = objectMapper.readValue(requestsResource.getFile(), InitialConfiguration.class);
			dao.setInitialConfiguration(initialConfiguration);
		} catch (Exception e) {
			// jak źle wczyta config to po prostu się zamyka
			logger.warn(e.getMessage());
		}
	}
	
	private void prepareNodesConfiguration() {
		InitialConfiguration configuration = dao.getInitialConfiguration();
		var nodeRequestConfig = new NodeRequestConfig(configuration.requests().stream().map(request -> {
			MultiValueMap requestHeaders = new MultiValueMap();
			requestHeaders.putAll(configuration.defaultHeaders());
			requestHeaders.putAll(request.headers());
			
			int count = request.count() / (configuration.nodes() + 1);
			
			return new RequestInfo(request.method(), request.uri(), requestHeaders, request.body(), request.name(), request.timeout(), count);
		}).toList());
		dao.setNodeRequestConfig(nodeRequestConfig);
	}
	
	private void prepareMasterConfiguration() {
		InitialConfiguration configuration = dao.getInitialConfiguration();
		int nodes = configuration.nodes() + 1; // all nodes (plus master)
		
		var masterRequestConfig = new NodeRequestConfig(configuration.requests().stream().map(request -> {
			MultiValueMap requestHeaders = new MultiValueMap();
			requestHeaders.putAll(configuration.defaultHeaders());
			requestHeaders.putAll(request.headers());
			
			int base = request.count() / nodes;
			int remainder = request.count() % nodes;
			
			return new RequestInfo(request.method(), request.uri(), requestHeaders, request.body(), request.name(), request.timeout(), base + remainder);
		}).toList());
		dao.setRequestConfig(masterRequestConfig);
	}
	
}

