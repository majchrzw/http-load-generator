package pl.majchrzw.loadtester.dto.config;

import org.apache.commons.collections.map.MultiValueMap;

import java.util.List;
import java.util.Optional;

public record InitialConfiguration(
		List<RequestInfo> requests,
		int nodes,
		Optional<RequestOrder> requestOrder,
		Optional<Long> nextRequestDelay,
		MultiValueMap defaultHeaders
) {
}
