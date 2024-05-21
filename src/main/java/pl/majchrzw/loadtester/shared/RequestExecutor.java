package pl.majchrzw.loadtester.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.config.RequestInfo;
import pl.majchrzw.loadtester.dto.statistics.NodeBundleExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeSingleExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.RequestIteratorData;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
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
		if (httpClient == null)
			httpClient = HttpClient.newBuilder()
					.executor(executorService)
					.build();
		Map<RequestInfo,List<NodeSingleExecutionStatistics>> requestsStatisticsMap = runAllRequests(dao.getRequestConfig());
		List<NodeBundleExecutionStatistics> bundleExecutionStatistics = new ArrayList<>(dao.getRequestConfig().requests().size());
		for (RequestInfo requestInfo : dao.getRequestConfig().requests()) {
			List<NodeSingleExecutionStatistics> singleExecutionStatistics = requestsStatisticsMap.get(requestInfo);
			NodeBundleExecutionStatistics bundleStatistics = new NodeBundleExecutionStatistics(singleExecutionStatistics,requestInfo);
			bundleExecutionStatistics.add(bundleStatistics);
		}
		NodeExecutionStatistics statistics = new NodeExecutionStatistics(dao.getId(), bundleExecutionStatistics);
		dao.setExecutionStatistics(statistics);
	}

	private Map<RequestInfo,List<NodeSingleExecutionStatistics>> runAllRequests(NodeRequestConfig requestConfig) {
		Map<RequestInfo,List<CompletableFuture<NodeSingleExecutionStatistics>>> requestsStatisticsMapFuture = new HashMap<>();
		for(RequestInfo requestInfo: requestConfig.requests()){
			requestsStatisticsMapFuture.put(requestInfo, new ArrayList<>(requestInfo.count()));
		}
		Long seed = 2137L; //TODO - to trzeba zmienić na coś losowego
		Long delayInMs = 50L; //TODO - mozna to wyciągnąć do konfiguracji
		Iterator<RequestIteratorData> iterator = new RandomRequestIterator(dao.getRequestConfig(),seed);
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		Integer iteration = 0;
		while(iterator.hasNext()){
			final Integer finalIteration = iteration;
			RequestIteratorData next = iterator.next();
			scheduler.schedule(() -> {
				CompletableFuture<NodeSingleExecutionStatistics> nodeSingleExecutionStatisticsCompletableFuture = handleAsyncRequest(next.request(), finalIteration);
				requestsStatisticsMapFuture.get(next.requestInfo()).add(nodeSingleExecutionStatisticsCompletableFuture);
			}, iteration * delayInMs, TimeUnit.MILLISECONDS);
			iteration++;
		}

		scheduler.shutdown();
		try {
			scheduler.awaitTermination(2, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (RequestInfo requestInfo : requestConfig.requests()) {
			List<CompletableFuture<NodeSingleExecutionStatistics>> completableFutures = requestsStatisticsMapFuture.get(requestInfo);
			CompletableFuture<Void> allOf = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
			try {
				allOf.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		Map<RequestInfo,List<NodeSingleExecutionStatistics>> requestsStatisticsMap = new HashMap<>();
		for (RequestInfo requestInfo : requestConfig.requests()) {
			List<NodeSingleExecutionStatistics> executionStatistics = requestsStatisticsMapFuture.get(requestInfo).stream().map(future -> {
				try {
					return future.get();
				} catch (ExecutionException | InterruptedException e) {
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toList());
			requestsStatisticsMap.put(requestInfo,executionStatistics);
		}

		return requestsStatisticsMap;

	}

	private CompletableFuture<NodeSingleExecutionStatistics> handleAsyncRequest(HttpRequest request, int i) {
		Instant start = Instant.now();
		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
			System.out.println("Sending request: " + request);
			Instant end = Instant.now();
			Long elapsedTime = Duration.between(start, end).toMillis();
			return new NodeSingleExecutionStatistics(i, elapsedTime, response.statusCode(),start);
		});
	}
	
}
