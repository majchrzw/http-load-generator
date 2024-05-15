package pl.majchrzw.loadtester.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.NodeBundleExecutionStatistics;
import pl.majchrzw.loadtester.dto.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.NodeSingleExecutionStatistics;
import pl.majchrzw.loadtester.dto.RequestInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class RequestExecutor {
	
	private final ExecutorService executorService;
	
	private HttpClient httpClient;
	
	private final DataRepository dao;
	
	private final Logger logger = LoggerFactory.getLogger(RequestExecutor.class);
	
	public RequestExecutor(DataRepository dao) {
		this.dao = dao;
		executorService = Executors.newVirtualThreadPerTaskExecutor();
		httpClient = HttpClient.newBuilder()
				.executor(executorService)
				.connectTimeout(Duration.ofMillis(dao.getRequestConfig().timeoutInMs()))
				.build();
	}
	
	public void run() {
		logger.info("Running http requests load");
		List<NodeBundleExecutionStatistics> bundleExecutionStatistics = new ArrayList<>(dao.getRequestConfig().requests().size());
		for(RequestInfo request:dao.getRequestConfig().requests()){
			NodeBundleExecutionStatistics bundle =this.executeRequestsBundle(request);
			bundleExecutionStatistics.add(bundle);
		}
		NodeExecutionStatistics statistics = new NodeExecutionStatistics(dao.getId(),bundleExecutionStatistics);
		dao.setExecutionStatistics(statistics);
	}

	private NodeBundleExecutionStatistics executeRequestsBundle(RequestInfo request) {
		try{
			List<NodeSingleExecutionStatistics> executionStatistics = new ArrayList<>(request.count());
			HttpRequest httpRequest = this.prepreRequest(request);
			for(int i = 0; i < request.count(); i++){
				NodeSingleExecutionStatistics statistics = this.handleRequest(httpRequest);
				executionStatistics.add(statistics);
			}
			return new NodeBundleExecutionStatistics(executionStatistics, request);

		}catch (Exception e) {
			//TODO dodac lepsza obsluge bledow
			logger.error("Error while executing request: " + request.name(), e);
		}
		return null;
	}

	private HttpRequest prepreRequest(RequestInfo requestInfo) throws URISyntaxException {
		HttpRequest.BodyPublisher bodyPublisher = requestInfo.body() != null ? HttpRequest.BodyPublishers.ofString(requestInfo.body()) : HttpRequest.BodyPublishers.noBody();
		//TODO dodac headers
		return HttpRequest.newBuilder()
				.uri(new URI(requestInfo.uri()))
				.method(requestInfo.method().name(), bodyPublisher)
				.build();
	}

	private NodeSingleExecutionStatistics handleRequest(HttpRequest request) throws IOException, InterruptedException {
		long start = System.currentTimeMillis();
		HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		long end = System.currentTimeMillis();
		long elapsedTime = end - start;
		return new NodeSingleExecutionStatistics(elapsedTime,response.statusCode());
	}

}
