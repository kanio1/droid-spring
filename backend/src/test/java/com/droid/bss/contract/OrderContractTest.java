package com.droid.bss.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChangeMethod;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;

/**
 * Contract tests for Order API using Pact
 * Verifies that the backend provider adheres to contracts defined by frontend consumers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("BSS Backend API")
@PactFolder("tests/contract/pacts")
@ExtendWith(SpringJUnitExtension.class)
public class OrderContractTest {

    @LocalServerPort
    private int port;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void beforeEach(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port, "/"));
    }

    @State("order exists")
    @IgnoreMissingStateChangeMethod
    void orderExists() {
        // Setup state for order exists
    }

    @State("orders exist")
    @IgnoreMissingStateChangeMethod
    void ordersExist() {
        // Setup state for orders list
    }

    @State("order created")
    @IgnoreMissingStateChangeMethod
    void orderCreated() {
        // Setup state for order creation
    }
}
