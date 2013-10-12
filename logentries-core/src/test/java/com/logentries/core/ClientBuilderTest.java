package com.logentries.core;

import java.util.UUID;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for verifying a correct initial state through the builder.
 */
public class ClientBuilderTest {

    private Client.Builder builder;
    private String uuid;

    @Before
    public void setup() {
        builder = Client.Builder.get();
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
        Client client = builder.withToken(UUID.randomUUID().toString()).build();

        Assert.assertNotNull(client);
    }

    /**
     * Missing account UUIDs should be rejected
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullAccount() {
        builder.withAccountKey(null);
    }

    /**
     * Empty account UUID strings should be rejected
     */
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyAccountString() {
        builder.withAccountKey("");
    }

    /**
     * Invalid account UUIDs should be rejected.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidAccount() {
        // We shouldn't have to call build;
        // invalid UUIDs should fail fast
        builder.withAccountKey("SOME-TOKEN");
    }

    /**
     * Valid account UUIDs should be accepted.
     */
    @Test
    public void testValidAccount() {
        builder.withAccountKey(uuid);
    }

    /**
     * Missing host UUIDs should be rejected
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullHost() {
        builder.withHostKey(null);
    }

    /**
     * Empty host UUID strings should be rejected
     */
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyHostString() {
        builder.withHostKey("");
    }

    /**
     * Invalid host UUIDs should be rejected.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidHost() {
        // We shouldn't have to call build;
        // invalid UUIDs should fail fast
        builder.withHostKey("SOME-TOKEN");
    }

    /**
     * Valid account UUIDs should be accepted.
     */
    @Test
    public void testValidHost() {
        builder.withHostKey(uuid);
    }

    /**
     * Disabling SSL should not throw exceptions.
     */
    @Test
    public void testDisableSSL() {
        Client client = builder.withToken(uuid)
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
            Client client = builder.withToken(uuid)
                .usingHTTP(true)
                .build();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(ex.getMessage(), "You must specify an account key to use HTTP");
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
            Client client = builder
                .withToken(uuid)
                .usingHTTP(true)
                .withAccountKey(uuid)
                .build();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(ex.getMessage(), "You must specify a host key to use HTTP");
            return;
        }
        Assert.fail("Should throw exception");
    }
}
