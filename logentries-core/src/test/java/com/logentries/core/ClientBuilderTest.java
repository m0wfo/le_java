package com.logentries.core;

import java.util.UUID;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for verifying a correct initial state through the builder.
 */
public class ClientBuilderTest {

    private LogentriesClient.Builder builder;
    private String uuid;

    @Before
    public void setup() {
        builder = LogentriesClient.Builder.get();
        uuid = UUID.randomUUID().toString();
    }

    /**
     * A log token is always mandatory.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testMissingToken() {
        builder.build();
    }

    /**
     * Invalid token UUIDs should be rejected.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidToken() {
        // We shouldn't have to call build;
        // invalid UUIDs should fail fast
        builder.withToken("SOME-TOKEN");
    }

    /**
     * Valid token UUIDs should be accepted.
     */
    @Test
    public void testValidToken() {
        LogentriesClient client = builder.withToken(UUID.randomUUID().toString()).build();

        Assert.assertNotNull(client);
    }

    /**
     * Disabling SSL should not throw exceptions.
     */
    @Test
    public void testDisableSSL() {
        LogentriesClient client = builder.withToken(uuid)
                .usingSSL(false)
                .build();

        Assert.assertNotNull(client);
    }

    /**
     * Using HTTP without specifying account token should throw an exception.
     */
    @Test
    public void testUseHTTPWithoutAccount() {
        try {
            LogentriesClient client = builder.withToken(uuid)
                .usingHTTP(true)
                .build();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(ex.getMessage(), "You must specify an account key to use the HTTP input.");
            return;
        }
        Assert.fail("Should throw exception");
    }

    /**
     * Using HTTP without specifying host token should throw an exception.
     */
    @Test
    public void testUseHTTPWithoutHost() {
        try {
            LogentriesClient client = builder
                .withToken(uuid)
                .usingHTTP(true)
                .build();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(ex.getMessage(), "You must specify a host key to use the HTTP input.");
            return;
        }
        Assert.fail("Should throw exception");
    }
}
