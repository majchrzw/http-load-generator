package pl.majchrzw.loadtester.dto;

import org.apache.commons.collections.map.MultiValueMap;

import java.util.HashMap;

public record RequestInfo(
		HttpMethod method,
		String uri,
		MultiValueMap headers,
		String body,
		String name,
		Long timeout,
		int count
) {
}
