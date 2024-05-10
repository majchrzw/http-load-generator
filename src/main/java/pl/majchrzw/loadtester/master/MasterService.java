package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.majchrzw.loadtester.dto.MasterRequestConfig;
import pl.majchrzw.loadtester.dto.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.RequestInfo;
import pl.majchrzw.loadtester.shared.messaging.MessagingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class MasterService {
	
	private MasterRequestConfig requestConfig;
	private final Logger logger = LoggerFactory.getLogger(MasterService.class);
	private final MessagingService messagingService;
	
	public MasterService(MessagingService messagingService) {
		this.messagingService = messagingService;
	}
	
	public void run() {
		ObjectMapper objectMapper = new ObjectMapper();
		ClassPathResource requestsResource = new ClassPathResource("requests.json");
		try {
			requestConfig = objectMapper.readValue(requestsResource.getFile(), MasterRequestConfig.class);
		} catch (Exception e) {
			// TODO - tutaj poprawić obsługę wyjątków (przejrzeć edge case-y, ew. zamykać program)
			logger.warn(e.getMessage());
		}
		
		final int nodes = requestConfig.nodes() + 1;
		List<RequestInfo> masterRequestsList = new ArrayList<>();
		List<RequestInfo> nodeRequestsList = new ArrayList<>();
		for ( RequestInfo requestInfo: requestConfig.requests()){
			// prepare headers
			HashMap<String, String> requestHeaders = new HashMap<>(requestConfig.defaultHeaders());
			requestHeaders.putAll(requestInfo.headers());
			// prepare amount
			int baseAmount = requestInfo.count() / nodes;
			int remainder = requestInfo.count() % nodes;
			
			masterRequestsList.add(new RequestInfo(requestInfo.method(), requestInfo.uri(), requestHeaders, requestInfo.body(), requestInfo.name(), baseAmount + remainder));
			nodeRequestsList.add(new RequestInfo(requestInfo.method(), requestInfo.uri(), requestHeaders, requestInfo.body(), requestInfo.name(), baseAmount));
		}
		
		try {
			Thread.sleep(1000 * 15);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		//TODO wysłać konfiguracje do wszystkich node-ów
		messagingService.transmit(new NodeRequestConfig(nodeRequestsList));
		System.out.println("Transmitted");
	}
	
	
	/*
	private void tmp(int nodes){
		NodeRequestConfig[] nodeRequestConfigs = new NodeRequestConfig[nodes];
		
		for (int i = 0; i < nodes; i++) {
			int nodeId = i;
			nodeRequestConfigs[i] = new NodeRequestConfig(
					requestConfig.requests()
							.stream()
							.map(requestInfo -> {
								int requestAmount = requestInfo.count() / nodes;
								if (requestInfo.count() % nodes != 0 && nodeId == 0) {
									requestAmount += requestInfo.count() % nodes;
								}
								
								HashMap<String, String> headers = new HashMap<>(requestConfig.defaultHeaders());
								headers.putAll(requestInfo.headers());
								
								return new RequestInfo(
										requestInfo.method(),
										requestInfo.uri(),
										headers,
										requestInfo.body(),
										requestInfo.name(),
										requestAmount
								);
							}).toList());
		}
		
		for (NodeRequestConfig config : nodeRequestConfigs) {
			System.out.println(config.requests().toString());
		}
		*/
	}

