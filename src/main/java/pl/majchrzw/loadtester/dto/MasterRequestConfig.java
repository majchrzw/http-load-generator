package pl.majchrzw.loadtester.dto;

import org.apache.commons.collections.map.MultiValueMap;

import java.util.HashMap;
import java.util.List;

public record MasterRequestConfig(
		List<RequestInfo> requests,
		int nodes,
		MultiValueMap defaultHeaders,
		Long timeoutInMs
) {
}
