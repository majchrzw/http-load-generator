package pl.majchrzw.loadtester.dto;

import java.util.HashMap;
import java.util.List;

public record MasterRequestConfig(
		List<RequestInfo> requests,
		int nodes,
		HashMap<String, String> defaultHeaders
) {
}
