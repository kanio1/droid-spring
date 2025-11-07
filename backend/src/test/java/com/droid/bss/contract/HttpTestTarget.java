package com.droid.bss.contract;

import au.com.dius.pact.core.model.BrokerUrlSource;
import au.com.dius.pact.core.model.PactSource;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.support.Json;
import au.com.dius.pact.provider.HttpRequestFactory;
import au.com.dius.pact.provider.ProviderClient;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ResponseComparison;
import au.com.dius.pact.provider.junit5.HttpTestTarget as PactHttpTestTarget;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom HTTP test target for Pact verification
 * Extends Pact's HttpTestTarget to support custom configurations
 */
public class HttpTestTarget extends PactHttpTestTarget {

    private final String host;
    private final int port;
    private final String path;

    public HttpTestTarget(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
    }

    @Override
    public URL getUrl() throws MalformedURLException {
        return new URL("http", host, port, path);
    }

    @Override
    public Map<String, String> getRequestHeaders(RequestResponsePact pact) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    @Override
    public ResponseComparison compareResponse(
            int expectedStatus,
            Map<String, Object> expectedHeaders,
            String expectedBody,
            ProviderInfo providerInfo,
            RequestResponsePact pact,
            PactSource pactSource) {
        return super.compareResponse(expectedStatus, expectedHeaders, expectedBody, providerInfo, pact, pactSource);
    }

    @Override
    public HttpRequestFactory getRequestFactory() {
        return super.getRequestFactory();
    }

    @Override
    public ProviderClient getProviderClient(ProviderInfo providerInfo) {
        return super.getProviderClient(providerInfo);
    }
}
