package pl.majchrzw.loadtester.dto;

import java.util.HashMap;

public record RequestInfo(
		HttpMethod method,
		String uri,
		HashMap<String, String> headers,
		String body,
		String name,
		int count
) {
}
