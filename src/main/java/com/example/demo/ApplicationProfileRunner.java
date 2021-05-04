package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class  ApplicationProfileRunner implements ApplicationRunner {

    @Value("${my.message}")
    private String myMessage;

    @Autowired
    Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println(myMessage +
                " " + Arrays.toString(environment.getActiveProfiles()) +
                " " + Arrays.toString(environment.getDefaultProfiles()));
    }
}
