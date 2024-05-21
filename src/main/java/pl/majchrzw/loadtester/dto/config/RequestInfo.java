package pl.majchrzw.loadtester.dto.config;

import org.apache.commons.collections.map.MultiValueMap;
import pl.majchrzw.loadtester.dto.HttpMethod;

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
