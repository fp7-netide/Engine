package eu.netide.core.api;

/**
 * Created by arne on 04.07.16.
 */
public interface OFRoutingRequest {
    long getBackendXid();

    long getShimXid();

    String getBackendID();

    String getReqTypeString();

    long getLastTimeActive();

    int getResponses();
}
