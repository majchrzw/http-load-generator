package pl.majchrzw.loadtester.shared;

import pl.majchrzw.loadtester.dto.config.RequestInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;

public class RequestBuilder {
	
	public static HttpRequest getRequest(RequestInfo requestInfo) throws URISyntaxException {
		HttpRequest.BodyPublisher bodyPublisher = requestInfo.body() != null ? HttpRequest.BodyPublishers.ofString(requestInfo.body()) : HttpRequest.BodyPublishers.noBody();
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(new URI(requestInfo.uri()))
				.method(requestInfo.method().name(), bodyPublisher)
				.timeout(Duration.ofMillis(requestInfo.timeout()));
		
		requestInfo.headers().forEach((key, value) ->
				((ArrayList<?>) value).forEach(val ->
				{
					String headerValue = "";
					if (val instanceof String) {
						headerValue = String.valueOf(val);
					} else if (val instanceof ArrayList) {
						headerValue = String.valueOf(val)
								.replace("[", "")
								.replace("]", "");
					}
					builder.header(key.toString(), headerValue);
				})
		
		);
		
		return builder.build();
	}
}
