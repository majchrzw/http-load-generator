package pl.majchrzw.loadtester.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.statistics.NodeBundleExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeExecutionStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeRequestStatistics;
import pl.majchrzw.loadtester.dto.statistics.NodeSingleExecutionStatistics;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StatisticsCalculator {
	
	
	public void generateAllStatistics(HashMap<UUID, NodeExecutionStatistics> executionStatistics){
		// create statistics dir if not exists
		String statisticsDirName = "statistics";
		File statisticsDir = new File(statisticsDirName);
		if (!statisticsDir.exists()){
			System.out.println(statisticsDir.mkdir());
		}
		String currentDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
		File dateDir = new File(statisticsDir, currentDate);
		if (!dateDir.exists()){
			System.out.println(dateDir.mkdir());
		}
		executionStatistics.keySet().forEach( key -> {
			File subDir = new File(dateDir, key.toString());
			if (!subDir.exists()){
				System.out.println(subDir.mkdir());
			}
			// TODO - tutaj po stworzeniu folderów można robić statystyki
			drawResponseTimePlots(executionStatistics.get(key), subDir);
			calculateStatistics(executionStatistics.get(key), subDir);
		});
	}
	
	public void drawResponseTimePlots(NodeExecutionStatistics statistics, File dir) {
		statistics.bundleExecutionStatistics().forEach(bundle -> drawResponseTimePlot(bundle, statistics.nodeId(), dir));
	}
	
	// rysuje wykres czasu odpowiedzi na zapytania z jednego node-a i jednego request-a
	private void drawResponseTimePlot(NodeBundleExecutionStatistics nodeBundleExecutionStatistics, UUID nodeId, File dir) {
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
				.map(t -> {
					double x = (t.startTime().toEpochMilli() - startTime) / 1000d;
					double y = t.elapsedTime();
					return new PlotValue(x, y);
				}).toList();
		
		double[] xData = new double[requestCount];
		double[] yData = new double[requestCount];
		
		AtomicInteger i = new AtomicInteger(-1);
		values.forEach(val -> {
			final int k = i.incrementAndGet();
			xData[k] = val.x();
			yData[k] = val.y();
		});
		
		XYChart chart = QuickChart.getChart(requestName, "Czas od startu wykonywania [s]", "Czas odpowiedzi [ms]", "s", xData, yData);
		// TODO - dać jakąś nazwę serii
		try {
			BitmapEncoder.saveBitmap(chart, dir.getAbsolutePath() + "/" + requestName, BitmapEncoder.BitmapFormat.PNG);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void calculateStatistics(NodeExecutionStatistics statistics, File dir) {
		List<NodeRequestStatistics> nodeStatistics = statistics.bundleExecutionStatistics().stream().map(t -> calculateOneRequestStatistics(t, statistics.nodeId())).toList();
		
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			File dataFile = new File(dir.getAbsolutePath(), "data.json");
			objectMapper.writeValue(dataFile, nodeStatistics);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private NodeRequestStatistics calculateOneRequestStatistics(NodeBundleExecutionStatistics nodeBundleExecutionStatistics, UUID nodeId) {
		int requestCount = nodeBundleExecutionStatistics.executionStatistics().size();
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
		// TODO - policzyć jeszcze procent udanych requestów, musi być do tego oddzielne pole w dto
		
		return new NodeRequestStatistics(
				averageRequestTime,
				minimumRequestTime,
				maximalRequestTime,
				requestCount,
				100, // TODO - to ma być obliczane finalnie
				nodeBundleExecutionStatistics.requestInfo().timeout(),
				nodeId,
				nodeBundleExecutionStatistics.requestInfo().name()
		);
	}
	
	private record PlotValue(double x, double y) {
	}
	
}
