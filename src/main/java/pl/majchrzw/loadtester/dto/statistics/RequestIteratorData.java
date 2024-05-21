package pl.majchrzw.loadtester.dto.statistics;

import pl.majchrzw.loadtester.dto.config.RequestInfo;

import java.net.http.HttpRequest;

public record RequestIteratorData(
        RequestInfo requestInfo,
        HttpRequest request
) {
}
