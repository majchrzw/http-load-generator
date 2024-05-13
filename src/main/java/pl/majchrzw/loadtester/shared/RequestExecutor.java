package pl.majchrzw.loadtester.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.majchrzw.loadtester.dto.NodeExecutionStatistics;

import java.net.http.HttpClient;
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
				.build();
	}
	
	public void run() {
		logger.info("Running http requests load");
		// TODO-to powinno czekać z zakończeniem wszystkich request-ów, tworzyć obiekt ze statystyki zapisywać do DAO
		NodeExecutionStatistics statistics = null;
		dao.setExecutionStatistics(statistics);
	}
	
}
