package pl.majchrzw.loadtester.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.config.RequestInfo;
import pl.majchrzw.loadtester.dto.statistics.BundleRequestStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.OneRequestStatistics;
import pl.majchrzw.loadtester.dto.statistics.RequestIteratorData;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestExecutor {
	
	private final ExecutorService executorService;
	private final Logger logger = LoggerFactory.getLogger(RequestExecutor.class);
	private final HttpClient httpClient;
	
	public RequestExecutor() {
		executorService = Executors.newVirtualThreadPerTaskExecutor();
		httpClient = HttpClient.newBuilder()
				.executor(executorService)
				.build();
	}
	
	public NodeExecutionStatistics run(NodeRequestConfig config, UUID nodeId) {
		
		logger.info("Running http requests in node: " + nodeId);
		Map<RequestInfo, List<OneRequestStatistics>> requestsStatisticsMap = runAllRequests(config);
		List<BundleRequestStatistics> bundleExecutionStatistics = new ArrayList<>(config.requests().size());
		for (RequestInfo requestInfo : config.requests()) {
			List<OneRequestStatistics> singleExecutionStatistics = requestsStatisticsMap.get(requestInfo);
			BundleRequestStatistics bundleStatistics = new BundleRequestStatistics(singleExecutionStatistics, requestInfo);
			bundleExecutionStatistics.add(bundleStatistics);
		}
		return new NodeExecutionStatistics(nodeId, bundleExecutionStatistics);
	}
	
	private Map<RequestInfo, List<OneRequestStatistics>> runAllRequests(NodeRequestConfig requestConfig) {
		Map<RequestInfo, List<CompletableFuture<OneRequestStatistics>>> requestsStatisticsMapFuture = new HashMap<>();
		for (RequestInfo requestInfo : requestConfig.requests()) {
			requestsStatisticsMapFuture.put(requestInfo, new ArrayList<>(requestInfo.count()));
		}
		Long seed = Instant.now().getEpochSecond();
		Iterator<RequestIteratorData> iterator = new RandomRequestIterator(requestConfig, seed);
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		AtomicInteger iteration = new AtomicInteger(0);
		while (iterator.hasNext()) {
			int finalIteration = iteration.get();
			RequestIteratorData next = iterator.next();
			scheduler.schedule(() -> {
				CompletableFuture<OneRequestStatistics> nodeSingleExecutionStatisticsCompletableFuture = handleAsyncRequest(next.request(), finalIteration, next.requestInfo().expectedReturnStatusCode());
				requestsStatisticsMapFuture.get(next.requestInfo()).add(nodeSingleExecutionStatisticsCompletableFuture);
			}, finalIteration * requestConfig.nextRequestDelay(), TimeUnit.MILLISECONDS);
			iteration.incrementAndGet();
		}
		
		scheduler.shutdown();
		try {
			scheduler.awaitTermination(2, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
		}
		
		for (RequestInfo requestInfo : requestConfig.requests()) {
			List<CompletableFuture<OneRequestStatistics>> completableFutures = requestsStatisticsMapFuture.get(requestInfo);
			CompletableFuture<Void> allOf = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
			try {
				allOf.get();
			} catch (InterruptedException | ExecutionException e) {
				logger.warn(e.getMessage());
			}
		}
		
		Map<RequestInfo, List<OneRequestStatistics>> requestsStatisticsMap = new HashMap<>();
		for (RequestInfo requestInfo : requestConfig.requests()) {
			List<OneRequestStatistics> executionStatistics = requestsStatisticsMapFuture.get(requestInfo).stream().map(future -> {
				try {
					return future.get();
				} catch (ExecutionException | InterruptedException e) {
					return null;
				}
			}).filter(Objects::nonNull).toList();
			requestsStatisticsMap.put(requestInfo, executionStatistics);
		}
		
		return requestsStatisticsMap;
		
	}
	
	private CompletableFuture<OneRequestStatistics> handleAsyncRequest(HttpRequest request, int i, int expectedStatusCode) {
		Instant start = Instant.now();
		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
			Instant end = Instant.now();
			long elapsedTime = Duration.between(start, end).toMillis();
			boolean success = response.statusCode() == expectedStatusCode;
			return new OneRequestStatistics(i, elapsedTime, response.statusCode(), response.body(), start, success, true);
		}).exceptionally(exception -> {
			boolean executed;
			executed = !(exception instanceof CompletionException);
			Instant end = Instant.now();
			long elapsedTime = Duration.between(start, end).toMillis();
			return new OneRequestStatistics(i, elapsedTime, -1, "", start, false, executed);
		});
	}
	
}
