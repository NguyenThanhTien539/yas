// package com.yas.order;

// import com.yas.order.config.ServiceUrlConfig;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.context.properties.EnableConfigurationProperties;

// @SpringBootApplication
// @EnableConfigurationProperties(ServiceUrlConfig.class)
// public class OrderApplication {

//     public static void main(String[] args) {
//         SpringApplication.run(OrderApplication.class, args);
//     }


// }


package com.yas.order;

import com.yas.order.config.ServiceUrlConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties(ServiceUrlConfig.class)
public class OrderApplication {

    // Sonar issue: hardcoded credentials (security vulnerability)
    private static final String PASSWORD = "123456";

    // Sonar issue: unused field
    private static String unusedField = "I am not used";

    public static void main(String[] args) {

        // Sonar issue: empty catch block
        try {
            SpringApplication.run(OrderApplication.class, args);
        } catch (Exception e) {

        }

        // Sonar issue: resource not closed (if it were real resource)
        List<String> list = new ArrayList<>();

        // Sonar issue: useless assignment
        int x = 10;
        x = 20;

        // Sonar issue: duplicated string literal
        System.out.println("HELLO");
        System.out.println("HELLO");

        // Sonar issue: condition always true
        if (true) {
            System.out.println("Always true");
        }

        // Sonar issue: possible NullPointerException
        String str = null;
        if (str.equals("test")) {
            System.out.println("NPE risk");
        }

        // Sonar issue: too complex method (code smell)
        complexMethod();
    }

    // Sonar issue: method too complex
    private static void complexMethod() {
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                if (i > 5) {
                    if (i < 8) {
                        System.out.println(i);
                    }
                }
            }
        }
    }
}