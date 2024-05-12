package pl.majchrzw.loadtester.shared;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RequestExecutor {
	
	private ExecutorService executorService;
	
	private HttpClient httpClient;
	
	public RequestExecutor() {
		executorService = Executors.newVirtualThreadPerTaskExecutor();
		//httpClient = HttpClient.newBuilder()
		//		.executor(executorService)
		//		.build();
	}
	
	public void run() {
		//System.setProperty("jdk.virtualThreadScheduler.parallelism", String.valueOf(10));
		//System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", String.valueOf(10));
		
		//System.out.println(System.getProperty("jdk.virtualThreadScheduler.parallelism"));
		//System.out.println(System.getProperty("jdk.virtualThreadScheduler.maxPoolSize"));
		//System.out.println(System.getProperty("jdk.virtualThreadScheduler.minRunnable"));
		for (int i = 0; i < 10; i++) {
			var test = executorService.submit(() -> {
				System.out.println("Task of thread: " + Thread.currentThread().toString());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				System.out.println("Ended thread: " + Thread.currentThread().toString());
			});
		}
	}
	
}
