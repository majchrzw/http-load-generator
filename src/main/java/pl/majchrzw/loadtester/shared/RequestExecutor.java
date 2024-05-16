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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class RequestExecutor {
	
	private final ExecutorService executorService;

	private HttpClient httpClient;

	private final DataRepository dao;
	
	private final Logger logger = LoggerFactory.getLogger(RequestExecutor.class);
	
	public RequestExecutor(DataRepository dao) {
		this.dao = dao;
		executorService = Executors.newVirtualThreadPerTaskExecutor();
	}
	
	public void run() {
		logger.info("Running http requests load");
		List<NodeBundleExecutionStatistics> bundleExecutionStatistics = new ArrayList<>(dao.getRequestConfig().requests().size());
		if(httpClient == null)
			httpClient = HttpClient.newBuilder()
					.executor(executorService)
					.build();

		for(RequestInfo request:dao.getRequestConfig().requests()){
			NodeBundleExecutionStatistics bundle =this.executeRequestsBundle(request);
			bundleExecutionStatistics.add(bundle);
		}
		NodeExecutionStatistics statistics = new NodeExecutionStatistics(dao.getId(),bundleExecutionStatistics);
		dao.setExecutionStatistics(statistics);
	}

	private NodeBundleExecutionStatistics executeRequestsBundle(RequestInfo request) {
		try{
			Integer delayOfRequests=100;
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			List<CompletableFuture<NodeSingleExecutionStatistics>> executionStatisticsFuture = new ArrayList<>(request.count());
			HttpRequest httpRequest = this.prepareRequest(request);
			for(int i=0; i<request.count();i++){
				scheduler.schedule(()->{
					CompletableFuture<NodeSingleExecutionStatistics> nodeSingleExecutionStatisticsCompletableFuture= handleAsyncRequest(httpRequest);
					executionStatisticsFuture.add(nodeSingleExecutionStatisticsCompletableFuture);
				},i*delayOfRequests, TimeUnit.MICROSECONDS);
			}
			scheduler.shutdown();
			try {
				scheduler.awaitTermination(1, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			CompletableFuture<Void> allOf = CompletableFuture.allOf(executionStatisticsFuture.toArray(new CompletableFuture[0]));
			try {
				allOf.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			List<NodeSingleExecutionStatistics> executionStatistics = executionStatisticsFuture.stream().map(future ->{
				try{
					return future.get();
				} catch (ExecutionException | InterruptedException e) {
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toList());

			return new NodeBundleExecutionStatistics(executionStatistics, request);

		}catch (Exception e) {
			//TODO dodac lepsza obsluge bledow
			logger.error("Error while executing request: " + request.name(), e);
		}
		return null;
	}

	private HttpRequest prepareRequest(RequestInfo requestInfo) throws URISyntaxException {
		HttpRequest.BodyPublisher bodyPublisher = requestInfo.body() != null ? HttpRequest.BodyPublishers.ofString(requestInfo.body()) : HttpRequest.BodyPublishers.noBody();
		//TODO dodac headers
		return HttpRequest.newBuilder()
				.uri(new URI(requestInfo.uri()))
				.method(requestInfo.method().name(), bodyPublisher)
				.timeout(Duration.ofMillis(requestInfo.timeout()))
				.build();
	}

	private CompletableFuture<NodeSingleExecutionStatistics> handleAsyncRequest(HttpRequest request) {
		Instant start =Instant.now();
		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
			Instant end = Instant.now();
			Long elapsedTime = Duration.between(start,end).toMillis();
			return new NodeSingleExecutionStatistics(elapsedTime,response.statusCode()) ;
		});
	}

}
