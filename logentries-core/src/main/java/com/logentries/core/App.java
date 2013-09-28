package com.logentries.core;

/**
 *
 * @author chris
 */
public class App {

    public static void main(String[] args) {
        Client c = Client.Builder.get()
                .withToken("13a65478-4fae-42bb-8ce0-a01dd11d2509")
                .usingSSL(false)
//                .usingHTTP(true)
                .withAccountKey("13a65478-4fae-42bb-8ce0-a01dd11d2509")
                .withHostKey("13a65478-4fae-42bb-8ce0-a01dd11d2509")
                .build();
        try {
            c.open();
//            c.write("Hello, world");
//            c.close();
//            Thread.sleep(1000);
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}
