package com.logentries.core;

import com.google.common.base.Throwables;
import rx.Observer;
import rx.util.functions.Func1;

/**
 *
 * @author chris
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
            for (int i = 0; i < 1000; i++) {
                c.write("Hello, world", "and again")
                        .then(new Func1() {
                            @Override
                            public Object call(Object o) {
                                return null;  //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });
            }

            c.close();
        } catch(Exception e) {
            System.out.println(Throwables.getStackTraceAsString(e));
        }
    }
}
