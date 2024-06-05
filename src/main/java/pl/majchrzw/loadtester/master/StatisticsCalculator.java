package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.majchrzw.loadtester.dto.statistics.BundleRequestStatistics;
import pl.majchrzw.loadtester.dto.statistics.CalculatedRequestStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.OneRequestStatistics;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StatisticsCalculator {
	
	Logger logger = LoggerFactory.getLogger(StatisticsCalculator.class);
	
	public void generateAllStatistics(HashMap<UUID, NodeExecutionStatistics> executionStatistics) {
		File statisticsDirectory = new File("statistics");
		if (!statisticsDirectory.exists()) {
			if (!statisticsDirectory.mkdir()) {
				throw new RuntimeException("Cannot create directory for storing statistics");
			}
		}
		
		String currentDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
		File currentRunDirectory = new File(statisticsDirectory, currentDate);
		if (!currentRunDirectory.exists()) {
			if (!currentRunDirectory.mkdir()) {
				throw new RuntimeException("Cannot create directory for current run statistics");
			}
		}
		
		HashMap<String, List<DataSeries>> dataSeries = generateDataSeriesForAllRequests(executionStatistics);
		dataSeries.forEach((key, value) -> drawResponseTimePlotInSeries(value, currentRunDirectory, key));
		calculateAndSaveRequestStatisticsToFile(executionStatistics, currentRunDirectory);
	}
	
	private void drawResponseTimePlotInSeries(List<DataSeries> values, File dir, String requestName) {
		XYChart chart = new XYChartBuilder()
				.width(1280)
				.height(720)
				.title("Response time")
				.xAxisTitle("Time of request execution [s]")
				.yAxisTitle("Response time [ms]")
				.build();
		chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
		Random random = new Random(Instant.now().getEpochSecond());
		AtomicInteger z = new AtomicInteger();
		values.forEach(plotValues -> {
			final int e = z.get();
			int requestCount = plotValues.values.size();
			
			double[] xData = new double[requestCount];
			double[] yData = new double[requestCount];
			
			AtomicInteger i = new AtomicInteger(-1);
			plotValues.values.forEach(val -> {
				final int k = i.incrementAndGet();
				xData[k] = val.x();
				yData[k] = val.y();
			});
			XYSeries series = chart.addSeries(plotValues.nodeId.toString(), xData, yData);
			series.setMarker(SeriesMarkers.CIRCLE);
			series.setMarkerColor(new Color(Math.abs(e * random.nextInt()) % 255, Math.abs(e * random.nextInt()) % 255, Math.abs(e * random.nextInt()) % 255));
			z.getAndIncrement();
		});
		
		try {
			BitmapEncoder.saveBitmap(chart, dir.getAbsolutePath() + "/" + requestName, BitmapEncoder.BitmapFormat.PNG);
		} catch (IOException e) {
			logger.warn("Cannot save response time plot to file");
			throw new RuntimeException(e);
		}
	}
	
	private HashMap<String, List<DataSeries>> generateDataSeriesForAllRequests(HashMap<UUID, NodeExecutionStatistics> executionStatistics) {
		HashMap<String, List<DataSeries>> series = new HashMap<>();
		executionStatistics.forEach((key, value) -> value.bundleExecutionStatistics().forEach(data -> {
			String requestName = data.requestInfo().name();
			if (!series.containsKey(requestName)) {
				series.put(requestName, new ArrayList<>());
			}
			
			long startTime = data
					.executionStatistics()
					.stream()
					.min(Comparator.comparing(OneRequestStatistics::startTime))
					.orElseThrow(() -> new IllegalStateException("No statistics to draw a plot"))
					.startTime()
					.toEpochMilli();
			
			List<PlotValue> values = data.executionStatistics()
					.stream()
					.sorted(Comparator.comparing(OneRequestStatistics::startTime))
					.filter(OneRequestStatistics::executed)
					.map(t -> {
						double x = (t.startTime().toEpochMilli() - startTime) / 1000d;
						double y = t.elapsedTime();
						return new PlotValue(x, y);
					}).toList();
			
			series.get(requestName).add(new DataSeries(values, key));
		}));
		return series;
	}
	
	private void calculateAndSaveRequestStatisticsToFile(HashMap<UUID, NodeExecutionStatistics> executionStatistics, File directory) {
		HashMap<String, BundleRequestStatistics> requestExecutionStatistics = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		executionStatistics.forEach((key, value) -> value.bundleExecutionStatistics().forEach(data -> {
			String requestName = data.requestInfo().name();
			if (!requestExecutionStatistics.containsKey(requestName)) {
				requestExecutionStatistics.put(requestName, new BundleRequestStatistics(new ArrayList<>(), data.requestInfo()));
			}
			BundleRequestStatistics bundleExecutionStatistics = requestExecutionStatistics.get(requestName);
			bundleExecutionStatistics.executionStatistics().addAll(data.executionStatistics());
		}));
		List<CalculatedRequestStatistics> allRequestsStatistics = new ArrayList<>(requestExecutionStatistics.size());
		requestExecutionStatistics.forEach((key, value) -> allRequestsStatistics.add(calculateOneRequestStatistics(value)));
		try {
			File calculatedDataFile = new File(directory.getAbsolutePath(), "run-statistics.json");
			mapper.writeValue(calculatedDataFile, allRequestsStatistics);
			File requestsDataFile = new File(directory.getAbsoluteFile(), "responses.json");
			mapper.writeValue(requestsDataFile, executionStatistics);
		} catch (Exception e) {
			logger.warn("Cannot save statistics to one of .json files");
			logger.warn(e.getMessage());
		}
	}
	
	private CalculatedRequestStatistics calculateOneRequestStatistics(BundleRequestStatistics bundleRequestStatistics) {
		long requestCount = bundleRequestStatistics.executionStatistics().size();
		
		long minimalResponseTime = Long.MAX_VALUE;
		long maximalResponseTime = 0;
		long totalResponseTime = 0;
		long successCount = 0, executionCount = 0;
		
		for (var statistic : bundleRequestStatistics.executionStatistics()) {
			if (statistic.success()) {
				successCount++;
				
				totalResponseTime += statistic.elapsedTime();
				if (statistic.elapsedTime() < minimalResponseTime) {
					minimalResponseTime = statistic.elapsedTime();
				}
				if (statistic.elapsedTime() > maximalResponseTime) {
					maximalResponseTime = statistic.elapsedTime();
				}
			}
			if (statistic.executed()) {
				executionCount++;
			}
		}
		
		long averageResponseTime = totalResponseTime / successCount;
		double successRate = (double) successCount / requestCount;
		double executionRate = (double) executionCount / requestCount;
		
		return new CalculatedRequestStatistics(
				averageResponseTime,
				minimalResponseTime,
				maximalResponseTime,
				requestCount,
				successCount,
				executionCount,
				successRate,
				executionRate,
				bundleRequestStatistics.requestInfo().timeout(),
				bundleRequestStatistics.requestInfo().expectedReturnStatusCode(),
				bundleRequestStatistics.requestInfo().name()
		);
	}
	
	private record PlotValue(double x, double y) {
	}
	
	private record DataSeries(List<PlotValue> values, UUID nodeId) {
	}
}
