package com.logentries.core;

import com.google.common.base.Throwables;

/**
 *
 */
public class App {

    public static void main(String[] args) {
        LogentriesClient c = LogentriesClient.Builder.get()
                .withToken("13a65478-4fae-42bb-8ce0-a01dd11d2509")
                .usingSSL(false)
//                .usingHTTP(true)
//                .withAccountKey("13a65478-4fae-42bb-8ce0-a01dd11d2509")
//                .withHostKey("13a65478-4fae-42bb-8ce0-a01dd11d2509")
                .build();
        try {
            c.open();
            Thread.sleep(1000);
            c.write("Hello, world");
            c.close();
        } catch(Exception e) {
            System.out.println(Throwables.getStackTraceAsString(e));
        }
    }
}
