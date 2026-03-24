package org.com.story;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StoryApplication {

    public static void main(String[] args) {

        SpringApplication.run(StoryApplication.class, args);
    }

}
