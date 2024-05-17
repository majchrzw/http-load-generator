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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StatisticsCalculator {
	
	public void drawAllPlots(NodeExecutionStatistics statistics) {
		statistics.bundleExecutionStatistics().forEach(bundle -> drawOneRequestPlot(bundle, statistics.nodeId()));
	}
	
	// rysuje wykres czasu odpowiedzi na zapytania z jednego node-a i jednego request-a
	private void drawOneRequestPlot(NodeBundleExecutionStatistics nodeBundleExecutionStatistics, UUID nodeid) {
		int requestCount = nodeBundleExecutionStatistics.executionStatistics().size();
		String requestName = nodeBundleExecutionStatistics.requestInfo().name();
		
		double[] xData = new double[requestCount];
		double[] yData = new double[requestCount];
		
		nodeBundleExecutionStatistics.executionStatistics().forEach(executionStatistics -> {
			xData[executionStatistics.id()] = executionStatistics.id();
			yData[executionStatistics.id()] = executionStatistics.elapsedTime();
		});
		
		XYChart chart = QuickChart.getChart(requestName, "Zapytanie " + requestName, "Czas odpowiedzi [ms]", "y(x)", xData, yData);
		
		try {
			BitmapEncoder.saveBitmap(chart, "./statistics/chart-" + requestName + "-" + nodeid, BitmapEncoder.BitmapFormat.PNG);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void calculateAllStatistics(NodeExecutionStatistics statistics) {
		List<NodeRequestStatistics> nodeStatistics = statistics.bundleExecutionStatistics().stream().map(t -> calculateOneRequestStatistics(t, statistics.nodeId())).toList();
		
		ObjectMapper objectMapper = new ObjectMapper();
		try{
			File dataFile = new File("./statistics/data-" + statistics.nodeId() + ".json");
			if (!dataFile.exists()){
				dataFile.createNewFile();
			}
			objectMapper.writeValue( dataFile, nodeStatistics);
		} catch (Exception e){
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
				100,
				nodeBundleExecutionStatistics.requestInfo().timeout(),
				nodeId,
				nodeBundleExecutionStatistics.requestInfo().name()
		);
	}
	
}
