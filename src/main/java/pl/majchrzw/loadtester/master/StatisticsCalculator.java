package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;
import pl.majchrzw.loadtester.dto.statistics.NodeBundleExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeRequestStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeSingleExecutionStatistics;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class StatisticsCalculator {
	// TODO - sprawdzić czy jest generowane wszystko co chcemy, code cleanup, test działania
	// TODO - ewentualnie dodać logi też, i sprawdzić wyjątki
	public void generateAllStatistics(HashMap<UUID, NodeExecutionStatistics> executionStatistics) {
		// create statistics dir if not exists
		File statisticsDirectory = new File("statistics");
		if (!statisticsDirectory.exists()) {
			if (!statisticsDirectory.mkdir()){
				throw new RuntimeException("Cannot create directory for storing statistics");
			}
		}
		// create dir for storing statistics of current run
		String currentDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
		File currentRunDirectory = new File(statisticsDirectory, currentDate);
		if (!currentRunDirectory.exists()) {
			if (!currentRunDirectory.mkdir()){
				throw new RuntimeException("Cannot create directory for current run statistics");
			}
		}
		// generate data series for each request
		HashMap<String, List<DataSeries>> dataSeries = generateDataSeriesForAllRequests(executionStatistics);
		// draw and save plots for each request
		dataSeries.forEach((key, value) -> drawResponseTimePlotInSeries(value, currentRunDirectory, key));
		// generate statistics in JSON format
		calculateAndSaveRequestStatisticsToFile(executionStatistics, currentRunDirectory);
	}
	
	// rysuje wykres czasu odpowiedzi na zapytania z jednego node-a i jednego request-a
	private void drawResponseTimePlotInSeries(List<DataSeries> values, File dir, String requestName) {
		XYChart chart = new XYChartBuilder()
				.width(800)
				.height(600)
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
			// TODO - poprawić wybór koloru dla kolejnych serii - może jakoś uzależnić od ilości node-ów?
			series.setMarkerColor(new Color( Math.abs(e * random.nextInt()) % 255, Math.abs(e * random.nextInt()) % 255, Math.abs(e * random.nextInt()) % 255));
			z.getAndIncrement();
		});
		
		try {
			BitmapEncoder.saveBitmap(chart, dir.getAbsolutePath() + "/" + requestName, BitmapEncoder.BitmapFormat.PNG);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private HashMap<String, List<DataSeries>> generateDataSeriesForAllRequests(HashMap<UUID, NodeExecutionStatistics> executionStatistics){
		HashMap<String, List<DataSeries>> series = new HashMap<>();
		executionStatistics.forEach((key, value) -> value.bundleExecutionStatistics().forEach(data -> {
			String requestName = data.requestInfo().name();
			if (!series.containsKey(requestName)) {
				series.put(requestName, new ArrayList<>());
			}
			
			long startTime = data
					.executionStatistics()
					.stream()
					.min(Comparator.comparing(NodeSingleExecutionStatistics::startTime))
					.orElseThrow(() -> new IllegalStateException("No statistics to draw a plot"))
					.startTime()
					.toEpochMilli();
			
			List<PlotValue> values = data.executionStatistics()
					.stream()
					.sorted(Comparator.comparing(NodeSingleExecutionStatistics::startTime))
					.filter(NodeSingleExecutionStatistics::executed)
					.map(t -> {
						double x = (t.startTime().toEpochMilli() - startTime) / 1000d;
						double y = t.elapsedTime();
						return new PlotValue(x, y);
					}).toList();
			
			series.get(requestName).add(new DataSeries(values, key));
		}));
		return series;
	}
	
	private void calculateAndSaveRequestStatisticsToFile(HashMap<UUID, NodeExecutionStatistics> executionStatistics, File directory){
		HashMap<String, NodeBundleExecutionStatistics> requestExecutionStatistics = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		executionStatistics.forEach( (key, value) -> value.bundleExecutionStatistics().forEach(data -> {
			String requestName = data.requestInfo().name();
			if(!requestExecutionStatistics.containsKey(requestName)){
				requestExecutionStatistics.put(requestName, new NodeBundleExecutionStatistics( new ArrayList<>(), data.requestInfo()));
			}
			NodeBundleExecutionStatistics bundleExecutionStatistics = requestExecutionStatistics.get(requestName);
			bundleExecutionStatistics.executionStatistics().addAll(data.executionStatistics());
		}));
		List<NodeRequestStatistics> allRequestsStatistics = new ArrayList<>(requestExecutionStatistics.size());
		requestExecutionStatistics.forEach( (key, value) -> {
			allRequestsStatistics.add(calculateOneRequestStatistics(value));
			try {
				File dataFile = new File(directory.getAbsolutePath(), "run-statistics.json");
				mapper.writeValue(dataFile, allRequestsStatistics);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		});
	}
	private NodeRequestStatistics calculateOneRequestStatistics(NodeBundleExecutionStatistics nodeBundleExecutionStatistics) {
		long requestCount = nodeBundleExecutionStatistics.executionStatistics().size();
		
		long minimalResponseTime = Long.MAX_VALUE;
		long maximalResponseTime = 0;
		long totalResponseTime = 0;
		double successCount = 0, executionCount = 0;
		
		for (var statistic : nodeBundleExecutionStatistics.executionStatistics()){
			if ( statistic.success()){
				successCount++;

				totalResponseTime += statistic.elapsedTime();
				if ( statistic.elapsedTime() < minimalResponseTime){
					minimalResponseTime = statistic.elapsedTime();
				}
				if ( statistic.elapsedTime() > maximalResponseTime){
					maximalResponseTime = statistic.elapsedTime();
				}
			}
			if ( statistic.executed()){
				executionCount++;
			}
			
		}
		
		long averageResponseTime = totalResponseTime / requestCount;
		double successRate = successCount / requestCount;
		double executionRate = executionCount / requestCount;
		
		return new NodeRequestStatistics(
				averageResponseTime,
				minimalResponseTime,
				maximalResponseTime,
				requestCount,
				successRate,
				executionRate,
				nodeBundleExecutionStatistics.requestInfo().timeout(),
				nodeBundleExecutionStatistics.requestInfo().name()
		);
	}
	
	private record PlotValue(double x, double y) {
	}
	
	private record DataSeries(List<PlotValue> values, UUID nodeId){}
}
