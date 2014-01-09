package com.logentries.core;

import java.util.UUID;

public class App {

    public static void main(String[] args) {
        LogentriesClient c = LogentriesClient.builder()
                .withToken(UUID.fromString("13a65478-4fae-42bb-8ce0-a01dd11d2509"))
//                .usingSSL(false)
//                .usingHTTP(true)
//                .withAccountKey("13a65478-4fae-42bb-8ce0-a01dd11d2509")
//                .withHostKey("13a65478-4fae-42bb-8ce0-a01dd11d2509")
                .build();
//        try {
            c.open();
		System.out.println("maybe connected?");
		for (int i = 0; i < 10; i++) {
			c.write("test log one with SSL!");
		}
//            c.close();
//        } catch(Exception e) {
//            System.out.println(Throwables.getStackTraceAsString(e));
//        }
    }
}
