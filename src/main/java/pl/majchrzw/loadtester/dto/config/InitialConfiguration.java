package pl.majchrzw.loadtester.dto.config;

import org.apache.commons.collections.map.MultiValueMap;

import java.util.List;

public record InitialConfiguration(
		List<RequestInfo> requests,
		int nodes,
		MultiValueMap defaultHeaders
) {
}
