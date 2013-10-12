package com.logentries.core;

import javax.net.ssl.SSLContext;

/**
 * A client-mode only SSL context provider.
 */
class SSLContextProvider {

    public static final String PROTOCOL = "TLS";

    public static SSLContext getContext() {
        try {
            SSLContext context = SSLContext.getInstance(PROTOCOL);
            context.init(null, TrustManagerFactory.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            return null;
        }
    }
}
