package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.statistics.NodeBundleExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeRequestStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeSingleExecutionStatistics;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StatisticsCalculator {
	
	
	public void generateAllStatistics(HashMap<UUID, NodeExecutionStatistics> executionStatistics) {
		ObjectMapper mapper = new ObjectMapper();
		// create statistics dir if not exists
		String statisticsDirName = "statistics";
		File statisticsDir = new File(statisticsDirName);
		if (!statisticsDir.exists()) {
			System.out.println(statisticsDir.mkdir());
		}
		String currentDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
		File dateDir = new File(statisticsDir, currentDate);
		if (!dateDir.exists()) {
			System.out.println(dateDir.mkdir());
		}
		
		HashMap<String, NodeBundleExecutionStatistics> requestExecutionStatistics = new HashMap<>();
		// to, co dzieje się poniżej jest najdziwniejszym kodem który napisałem napisałem i jest beznadziejny, ale działa xD
		HashMap<String, List<List<PlotValue>>> series = new HashMap<>();
		executionStatistics.forEach((key, value) -> {
			value.bundleExecutionStatistics().forEach(data -> {
				String requestName = data.requestInfo().name();
				if (!requestExecutionStatistics.containsKey(requestName)) {
					requestExecutionStatistics.put(requestName, new NodeBundleExecutionStatistics(new ArrayList<>(), data.requestInfo()));
					series.put(requestName, new ArrayList<>());
				}
				NodeBundleExecutionStatistics bundleExecutionStatistics = requestExecutionStatistics.get(requestName);
				bundleExecutionStatistics.executionStatistics().addAll(data.executionStatistics());
				
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
				
				series.get(requestName).add(values);
			});
		});
		requestExecutionStatistics.forEach((key, value) -> {
			//drawResponseTimePlot(value, dateDir);
			NodeRequestStatistics s = calculateOneRequestStatistics(value);
			try {
				File dataFile = new File(dateDir.getAbsolutePath(), String.format("data-%s.json", value.requestInfo().name()));
				mapper.writeValue(dataFile, s);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		});
		series.forEach((key, value) -> {
			drawResponseTimePlotInSeries(value, dateDir, key);
		});
	}
	
	public void drawResponseTimePlots(NodeExecutionStatistics statistics, File dir, String requestName) {
		statistics.bundleExecutionStatistics().forEach(bundle -> drawResponseTimePlot(bundle, dir));
	}
	
	// rysuje wykres czasu odpowiedzi na zapytania z jednego node-a i jednego request-a
	private void drawResponseTimePlotInSeries(List<List<PlotValue>> values, File dir, String requestName) {
		XYChart chart = new XYChartBuilder().width(800).height(600).title("Wykres czasu odpowiedzi od czasu wykonania zapytania").xAxisTitle("X").yAxisTitle("Y").build();
		chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
		Random random = new Random(Instant.now().getEpochSecond());
		AtomicInteger z = new AtomicInteger();
		values.forEach(plotValues -> {
			final int e = z.get();
			int requestCount = plotValues.size();
			
			double[] xData = new double[requestCount];
			double[] yData = new double[requestCount];
			
			AtomicInteger i = new AtomicInteger(-1);
			plotValues.forEach(val -> {
				final int k = i.incrementAndGet();
				xData[k] = val.x();
				yData[k] = val.y();
			});
			XYSeries series = chart.addSeries("Dane" + e, xData, yData);
			series.setMarker(SeriesMarkers.CIRCLE);
			series.setMarkerColor(new Color( e * random.nextInt() % 255, e * random.nextInt() % 255, e * random.nextInt() % 255));
			z.getAndIncrement();
		});
		
		try {
			BitmapEncoder.saveBitmap(chart, dir.getAbsolutePath() + "/" + requestName, BitmapEncoder.BitmapFormat.PNG);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void drawResponseTimePlot(NodeBundleExecutionStatistics nodeBundleExecutionStatistics, File dir) {
		int requestCount = nodeBundleExecutionStatistics.executionStatistics().size();
		String requestName = nodeBundleExecutionStatistics.requestInfo().name();
		
		
		long startTime = nodeBundleExecutionStatistics
				.executionStatistics()
				.stream()
				.min(Comparator.comparing(NodeSingleExecutionStatistics::startTime))
				.orElseThrow(() -> new IllegalStateException("No statistics to draw a plot"))
				.startTime()
				.toEpochMilli();
		
		
		List<PlotValue> values = nodeBundleExecutionStatistics.executionStatistics()
				.stream()
				.sorted(Comparator.comparing(NodeSingleExecutionStatistics::startTime))
				.filter(NodeSingleExecutionStatistics::executed)
				.map(t -> {
					double x = (t.startTime().toEpochMilli() - startTime) / 1000d;
					double y = t.elapsedTime();
					return new PlotValue(x, y);
				}).toList();
		
		if (!values.isEmpty()) {
			double[] xData = new double[requestCount];
			double[] yData = new double[requestCount];
			
			AtomicInteger i = new AtomicInteger(-1);
			values.forEach(val -> {
				final int k = i.incrementAndGet();
				xData[k] = val.x();
				yData[k] = val.y();
			});
			XYChart chart = new XYChartBuilder().width(800).height(600).title("Wykres Punktowy").xAxisTitle("X").yAxisTitle("Y").build();
			chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
			// Dodanie serii danych do wykresu
			XYSeries series = chart.addSeries("Dane", xData, yData);
			series.setMarker(SeriesMarkers.CIRCLE);  // Ustawienie markerów na kółka
			// TODO - dać jakąś nazwę serii
			try {
				BitmapEncoder.saveBitmap(chart, dir.getAbsolutePath() + "/" + requestName, BitmapEncoder.BitmapFormat.PNG);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void calculateStatistics(NodeExecutionStatistics statistics, File dir) {
		List<NodeRequestStatistics> nodeStatistics = statistics.bundleExecutionStatistics().stream().map(this::calculateOneRequestStatistics).toList();
		
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			File dataFile = new File(dir.getAbsolutePath(), "data.json");
			objectMapper.writeValue(dataFile, nodeStatistics);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private NodeRequestStatistics calculateOneRequestStatistics(NodeBundleExecutionStatistics nodeBundleExecutionStatistics) {
		int requestCount = nodeBundleExecutionStatistics.executionStatistics().size();
		// TODO usunąć streamy i zrobić wszystko naraz w pętli forEach
		long averageRequestTime = nodeBundleExecutionStatistics.executionStatistics()
				.stream()
				.mapToLong(NodeSingleExecutionStatistics::elapsedTime)
				.sum() / requestCount;
		
		long minimumRequestTime = nodeBundleExecutionStatistics.executionStatistics()
				.stream()
				.mapToLong(NodeSingleExecutionStatistics::elapsedTime)
				.min()
				.orElse(0);
		
		long maximalRequestTime = nodeBundleExecutionStatistics.executionStatistics()
				.stream()
				.mapToLong(NodeSingleExecutionStatistics::elapsedTime)
				.max()
				.orElse(0);
		
		double successRate = (double) nodeBundleExecutionStatistics.executionStatistics()
				.stream()
				.filter(NodeSingleExecutionStatistics::success)
				.count() / requestCount;
		
		double executionRate = (double) nodeBundleExecutionStatistics.executionStatistics()
				.stream()
				.filter(NodeSingleExecutionStatistics::executed)
				.count() / requestCount;
		
		return new NodeRequestStatistics(
				averageRequestTime,
				minimumRequestTime,
				maximalRequestTime,
				requestCount,
				successRate,
				executionRate,
				nodeBundleExecutionStatistics.requestInfo().timeout(),
				nodeBundleExecutionStatistics.requestInfo().name()
		);
	}
	
	private record PlotValue(double x, double y) {
	}
	
	private record Series(List<OnePlotSeries> series) {
	}
	
	private record OnePlotSeries(double[] xValues, double[] yValues) {
	}
	
}
