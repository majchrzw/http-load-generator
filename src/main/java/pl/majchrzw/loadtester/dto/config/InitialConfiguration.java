package pl.majchrzw.loadtester.dto.config;

import org.apache.commons.collections.map.MultiValueMap;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

public record InitialConfiguration(
        List<RequestInfo> requests,
        int nodes,
        Optional<Long> nextRequestDelay,
        MultiValueMap defaultHeaders
) {
    public static Long MAXIMUM_DELAY = 10_000L;
    public ConfigValidationStatus validate() {
        if(requests == null || requests.isEmpty()){
            return new ConfigValidationStatus(false, "Requests list is empty");
        }

        for (RequestInfo requestInfo : requests) {
           ConfigValidationStatus status = requestInfo.validate();
           if(!status.valid()){
               return status;
           }
        }
        if(nodes < 0){
            return new ConfigValidationStatus(false, "Number of nodes must be greater or equall than 0");
        }
        if(nextRequestDelay.isPresent() && (nextRequestDelay.get() < 0 || nextRequestDelay.get() > MAXIMUM_DELAY)){
            return new ConfigValidationStatus(false,  MessageFormat.format("Next request delay must be greater  than 0 and less or equal than {0}", MAXIMUM_DELAY));
        }
        if(!HeadersValidator.validateHeaders(defaultHeaders).valid())
            return new ConfigValidationStatus(false, "Default headers are not valid");

        return new ConfigValidationStatus(true, "");
    }
}
