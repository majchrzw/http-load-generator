package pl.majchrzw.loadtester.dto.config;

import java.util.List;

public record NodeRequestConfig(
		List<RequestInfo> requests,
		long nextRequestDelay
) {
}
