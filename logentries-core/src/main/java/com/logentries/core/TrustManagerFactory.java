package com.logentries.core;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 *
 */
class TrustManagerFactory extends TrustManagerFactorySpi {

    private static final TrustManager DEFAULT = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            // no-op
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            // no-op
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    public static TrustManager[] getTrustManagers() {
        return new TrustManager[] {DEFAULT};
    }

    @Override
    protected void engineInit(KeyStore ks) throws KeyStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void engineInit(ManagerFactoryParameters mfp) throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[] {DEFAULT};
    }

}
