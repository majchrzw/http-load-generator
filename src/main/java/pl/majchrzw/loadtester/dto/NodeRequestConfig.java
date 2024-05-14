package pl.majchrzw.loadtester.dto;

import java.util.List;

public record NodeRequestConfig(
		List<RequestInfo> requests,
		Long timeoutInMs
) {
}
