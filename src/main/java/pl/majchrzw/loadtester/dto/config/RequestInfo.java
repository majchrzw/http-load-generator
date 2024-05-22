package pl.majchrzw.loadtester.dto.config;

import org.apache.commons.collections.map.MultiValueMap;
import pl.majchrzw.loadtester.dto.HttpMethod;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.regex.Pattern;

public record RequestInfo(
        HttpMethod method,
        String uri,
        MultiValueMap headers,
        String body,
        String name,
        Long timeout,
        int expectedReturnStatusCode,
        int count
) {
    public static Pattern URI_Pattern = Pattern.compile("^(http|https)://[a-zA-Z0-9.-]+(:[0-9]+)?(/[a-zA-Z0-9.-]+)*$");

    public ConfigValidationStatus validate() {
        if (name == null || name.isBlank())
            return new ConfigValidationStatus(false, "Name  is empty");
        if (!Arrays.asList(HttpMethod.values()).contains(method))
            return new ConfigValidationStatus(false, MessageFormat.format("Method {0} in request {1} is not supported", method, name));
        if (!URI_Pattern.matcher(uri).matches())
            return new ConfigValidationStatus(false, MessageFormat.format("URI {0} in request {1} is not valid", uri, name));
        if (timeout < 100)
            return new ConfigValidationStatus(false, MessageFormat.format("Timeout {0} in request {1} is too short", timeout, name));
        if (count < 1 || count > Integer.MAX_VALUE)
            return new ConfigValidationStatus(false, MessageFormat.format("Count {0} in request {1} is not valid, min value = {2}, max value = {3}", count, name, 0, Integer.MAX_VALUE));
        if (expectedReturnStatusCode < 100 || expectedReturnStatusCode > 599)
            return new ConfigValidationStatus(false, MessageFormat.format("Expected return status code {0} in request {1} is not valid, min value = {2}, max value = {3}", expectedReturnStatusCode, name, 100, 599));
        if (!HeadersValidator.validateHeaders(headers).valid())
            return new ConfigValidationStatus(false, MessageFormat.format("Headers in request {0} are not valid", name));
        return new ConfigValidationStatus(true, "");
    }
}
