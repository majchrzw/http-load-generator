package pl.majchrzw.loadtester.shared;

import pl.majchrzw.loadtester.dto.config.RequestInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;

public class RequestBuilder {
	
	public static HttpRequest getRequest(RequestInfo requestInfo) throws URISyntaxException {
		HttpRequest.BodyPublisher bodyPublisher = requestInfo.body() != null ? HttpRequest.BodyPublishers.ofString(requestInfo.body()) : HttpRequest.BodyPublishers.noBody();
		//TODO dodac headers
		return HttpRequest.newBuilder()
				.uri(new URI(requestInfo.uri()))
				.method(requestInfo.method().name(), bodyPublisher)
				.timeout(Duration.ofMillis(requestInfo.timeout()))
				.build();
	}
}
