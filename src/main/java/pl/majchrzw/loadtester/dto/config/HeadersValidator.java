package pl.majchrzw.loadtester.dto.config;

import org.apache.commons.collections.map.MultiValueMap;

import java.text.MessageFormat;

public class HeadersValidator {
    public static ConfigValidationStatus validateHeaders(MultiValueMap headers) {
        if(headers == null)
            return new ConfigValidationStatus(true, "Empty headers are good headers");
        if(headers.keySet().stream().anyMatch(key -> key == null || key.toString().isBlank() || ((String)key).contains(" ")))
            return new ConfigValidationStatus(false, "Headers contains invalid key" );
        return new ConfigValidationStatus(true, "Headers are valid");
    }
}
