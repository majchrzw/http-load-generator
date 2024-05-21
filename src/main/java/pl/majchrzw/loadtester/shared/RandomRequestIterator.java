package pl.majchrzw.loadtester.shared;

import pl.majchrzw.loadtester.dto.config.NodeRequestConfig;
import pl.majchrzw.loadtester.dto.config.RequestInfo;
import pl.majchrzw.loadtester.dto.statistics.RequestIteratorData;

import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.*;

public class RandomRequestIterator implements Iterator<RequestIteratorData> {

    Map<RequestInfo, HttpRequest> requestMap = new HashMap<>();
    Map<RequestInfo,Integer> remainingRequestsMap = new HashMap<>();
    List<RequestInfo> requests;
    Random random;
    Integer remainingRequestsCount =0;

    public RandomRequestIterator(NodeRequestConfig nodeRequestConfig,Long seed) {
        this.random = new Random(seed);
        this.requests=nodeRequestConfig.requests();
        prepareRequestsMap();
        prepareRemainingRequestsMap();
    }
    @Override
    public boolean hasNext() {
        return remainingRequestsCount > 0;
    }

    @Override
    public RequestIteratorData next() {
        if(!hasNext())
            throw new NoSuchElementException("No more requests to iterate");
        RequestInfo request = requests.get(random.nextInt(requests.size()));
        HttpRequest httpRequest = requestMap.get(request);
        remainingRequestsCount--;
        remainingRequestsMap.put(request,remainingRequestsMap.get(request)-1);
        if(remainingRequestsMap.get(request)==0){
            remainingRequestsMap.remove(request);
            requests = remainingRequestsMap.keySet().stream().toList();
        }
        return new RequestIteratorData(request,httpRequest);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation is not supported");
    }

    private void prepareRequestsMap() {
        for (RequestInfo request : requests) {
            try {
            HttpRequest httpRequest = RequestBuilder.getRequest(request);
            requestMap.put(request, httpRequest);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void prepareRemainingRequestsMap() {
        for (RequestInfo request : requests) {
            this.remainingRequestsCount+=request.count();
            remainingRequestsMap.put(request, request.count());
        }
    }
}
