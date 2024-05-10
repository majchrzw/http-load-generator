package pl.majchrzw.loadtester.shared;

import com.google.common.collect.Multimap;
import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	
	public void run(){
	
	}
	
}
