package pl.majchrzw.loadtester.shared;

import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RequestExecutor {
	
	private ExecutorService executorService;
	
	private HttpClient httpClient;
	
	public RequestExecutor() {
		executorService = Executors.newFixedThreadPool(5);
		httpClient = HttpClient.newBuilder()
				.executor(executorService)
				.build();
	}
	
	public void run() {
	
	}
	
}
